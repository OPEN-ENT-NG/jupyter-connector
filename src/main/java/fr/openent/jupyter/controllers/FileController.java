package fr.openent.jupyter.controllers;

import fr.openent.jupyter.Jupyter;
import fr.openent.jupyter.helper.ParametersHelper;
import fr.openent.jupyter.models.File;
import fr.openent.jupyter.security.AccessRight;
import fr.openent.jupyter.service.DocumentService;
import fr.openent.jupyter.service.Impl.DefaultDocumentService;
import fr.openent.jupyter.service.Impl.DefaultStorageService;
import fr.openent.jupyter.service.StorageService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.bus.WorkspaceHelper;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;

import java.util.Base64;

import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class FileController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(FileController.class);
    private final DocumentService documentService;
    private final StorageService storageService;
    private final WorkspaceHelper workspaceHelper;
    private final Storage storage;

    public FileController(EventBus eb, Storage storage) {
        super();
        documentService = new DefaultDocumentService(eb, storage);
        storageService = new DefaultStorageService(storage);
        workspaceHelper = new WorkspaceHelper(eb, storage);
        this.storage = storage;
    }

    @Get("/file")
    @ApiDoc("Get a specific file")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getFile(HttpServerRequest request) {
        String entId = request.getParam("ent_id");

        ParametersHelper.hasMissingOrEmptyParameters(new String[] {entId}, handler -> {
            if (handler.isRight()) {
                workspaceHelper.getDocument(entId, getDocumentEvent -> {
                    if (getDocumentEvent.succeeded()) {
                        JsonObject document = getDocumentEvent.result().body().getJsonObject("result");

                        if (document != null) {
                            String id = document.getString("file");
                            workspaceHelper.readFile(id, content -> {
                                if (content != null) {
                                    JsonObject file = new File(document).toJson();
                                    String docName = document.getString("name");
                                    String fileExtension = docName.contains(".") ? docName.substring(docName.lastIndexOf(".")) : "";
                                    if (fileExtension.equals(Jupyter.EXTENSION_NOTEBOOK)) { // Case notebook
                                        file.put("content", content.toString());
                                        file.put("format", "json");
                                        file.put("mimetype", (String) null);
                                        file.put("type", "notebook");
                                    }
                                    switch (file.getString("format")) {
                                        case "json": // Case notebook created from Jupyter
                                        case "text": // Case file text created from Jupyter
                                            file.put("content", content.toString());
                                            break;
                                        case "base64": // Case file imported from Jupyter or Workspace
                                            String finalContent = Base64.getEncoder().encodeToString(content.getBytes());
                                            if (fileExtension.isEmpty()) { //if no file extension, we don't change the content
                                                finalContent = content.toString();
                                            } else if (Jupyter.EXTENSIONS_TEXT.contains(fileExtension)) { // Case text en base64
                                                finalContent = new String(Base64.getDecoder().decode(content.toString()));
                                                file.put("format", "json");
                                            }
                                            file.put("content", finalContent);
                                            break;
                                        default:
                                            log.error("[Jupyter@getFile] File format unknown : " + file.getString("format"));
                                            break;
                                    }
                                    renderJson(request, file);
                                } else {
                                    badRequest(request, "[Jupyter@getFile] No file found in storage for id : " + id);
                                }
                            });
                        }
                        else {
                            badRequest(request, "[Jupyter@getFile] No document found for entId : " + entId);
                        }
                    }
                    else {
                        badRequest(request, "[Jupyter@getFile] Fail to get document from workspace : " + getDocumentEvent.cause().getMessage());
                    }
                });
            }
            else {
                badRequest(request, "[Jupyter@getFile] " + handler.left().getValue());
            }
        });
    }

    @Post("/file")
    @ApiDoc("Create a new file")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void createFile(HttpServerRequest request) {
        String userId = request.headers().get("User-Id");
        String userName = request.headers().get("User-Name");
        String format = request.getParam("format");
        String name = request.getParam("name");
        String parent_id = request.getParam("parent_id");

        ParametersHelper.hasMissingOrEmptyParameters(new String[] {userId, userName, format, name}, handler -> {
            if (handler.isRight()) {
                JsonObject body = new JsonObject()
                        .put("name",name)
                        .put("format", format)
                        .put("parent_id", parent_id);
                UserInfos user = new UserInfos();
                user.setUserId(userId);
                user.setUsername(userName);

                request.setExpectMultipart(true);
                final Buffer buff = Buffer.buffer();
                request.uploadHandler(upload -> {
                    upload.handler(buff::appendBuffer);
                    upload.endHandler(end -> {
                        addNewDocument(request, name, body, user, buff,null);
                    });
                });
            }
            else {
                badRequest(request, "[Jupyter@createFile] " + handler.left().getValue());
            }
        });
    }

    private void addNewDocument(HttpServerRequest request, String name, JsonObject body, UserInfos user, Buffer buff, Buffer contentToAdd) {
        storageService.add(body, buff, contentToAdd, addFileEvent -> {
            if (addFileEvent.isRight()) {
                JsonObject storageEntries = addFileEvent.right().getValue();

                String application = config.getString("app-name");
                workspaceHelper.addDocument(storageEntries, user, name, application, false, null, createEvent -> {
                    if (createEvent.succeeded()) {
                        JsonObject doc = createEvent.result().body();

                        JsonObject file = new File(doc).toJson();

                        String parentId = body.getString("parent_id");
                        if (parentId == null || parentId.isEmpty()) { // If parent is base directory there's no need to move the document
                            renderJson(request, file, 200);
                        } else {
                            workspaceHelper.moveDocument(file.getString("id"), parentId, user, moveEvent -> {
                                if (moveEvent.succeeded()) {
                                    renderJson(request, file, 200);
                                } else {
                                    badRequest(request, "[Jupyter@createFile] Failed to move a workspace document : " +
                                            moveEvent.cause().getMessage());
                                }
                            });
                        }
                    } else {
                        badRequest(request, "[Jupyter@createFile] Failed to create a workspace document : " +
                                createEvent.cause().getMessage());
                    }
                });
            } else {
                badRequest(request, "[Jupyter@createFile] Failed to create a new entry in the storage");
            }
        });
    }

    @Put("/file")
    @ApiDoc("Update a specific file")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void updateFile(HttpServerRequest request) {
        String entId = request.getParam("ent_id");
        String format = request.getParam("format");
        String name = request.getParam("name");
        String userId = request.headers().get("User-Id");
        String userName = request.headers().get("User-Name");

        ParametersHelper.hasMissingOrEmptyParameters(new String[] {entId, format, name}, handler -> {
            if (handler.isRight()) {
                JsonObject body = new JsonObject()
                        .put("name",name)
                        .put("format", format);
                UserInfos user = new UserInfos();
                user.setUserId(userId);
                user.setUsername(userName);
                request.setExpectMultipart(true);
                final Buffer buff = Buffer.buffer();
                request.uploadHandler(upload -> {
                    upload.handler(buff::appendBuffer);
                    upload.endHandler(end -> {
                        if(request.params().contains("chunk")){
                            workspaceHelper.getDocument(entId, getDocumentEvent -> {
                                if (getDocumentEvent.succeeded()) {
                                    JsonObject document = getDocumentEvent.result().body().getJsonObject("result");
                                    if (document != null) {
                                        String id = document.getString("file");
                                        workspaceHelper.readFile(id, content -> {
                                            if (content != null) {
                                                if (request.getParam("chunk").equals("-1")) { //last chunk
                                                    documentService.delete(entId, userId, deleteEvent -> {
                                                        if (deleteEvent.isRight()) {
                                                            body.put("parent_id",document.getString("eParent"));
                                                            addNewDocument(request, name, body, user, buff,content);
                                                        }else{
                                                            badRequest(request, "[Jupyter@updateFile] Fail to delete document in workspace : " +
                                                                    deleteEvent.left().getValue());
                                                        }
                                                    });
                                                } else {
                                                    updateFile(request, entId, body, buff, content);
                                                }
                                            } else {
                                                badRequest(request, "[Jupyter@updateFile] No file found in storage for id : " + id);
                                            }
                                        });
                                    } else {
                                        badRequest(request, "[Jupyter@updateFile] No document found for entId : " + entId);
                                    }
                                } else {
                                    badRequest(request, "[Jupyter@updateFile] Fail to get document from workspace : " +
                                            getDocumentEvent.cause().getMessage());
                                }
                            });
                        } else {
                            updateFile(request, entId, body, buff, null);
                        }
                    });
                });
            }
            else {
                badRequest(request, "[Jupyter@updateFile] " + handler.left().getValue());
            }
        });

    }

    private void updateFile(HttpServerRequest request, String entId, JsonObject body, Buffer buff, Buffer contentToAdd) {
        storageService.add(body, buff, contentToAdd, addFileEvent -> {
            if (addFileEvent.isRight()) {
                JsonObject uploaded = addFileEvent.right().getValue();

                documentService.update(entId, uploaded, updateEvent -> {
                    if (updateEvent.isRight()) {

                        workspaceHelper.getDocument(entId, getDocumentEvent -> {
                            if (getDocumentEvent.succeeded()) {
                                JsonObject document = getDocumentEvent.result().body().getJsonObject("result");
                                if (document != null) {
                                    JsonObject file = new File(document).toJson();
                                    renderJson(request, file);
                                } else {
                                    badRequest(request, "[Jupyter@getFile] No document found for entId : " + entId);
                                }
                            } else {
                                badRequest(request, "[Jupyter@updateFile] Fail to get document from workspace : " +
                                        getDocumentEvent.cause().getMessage());
                            }
                        });
                    } else {
                        log.error("[Jupyter@updateFile] Failed to update document with new storage id : " +
                                updateEvent.left().getValue());
                        storage.removeFile(uploaded.getString("_id"), event -> {
                            if (!"ok".equals(event.getString("status"))) {
                                log.error("[Jupyter@updateFile] Error removing file " + uploaded.getString("_id") + " : " +
                                        event.getString("message"));
                            }
                            badRequest(request);
                        });
                    }
                });
            } else {
                badRequest(request, addFileEvent.left().getValue());
            }
        });
    }

    @Delete("/file")
    @ApiDoc("Delete a specific file")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void deleteFile(HttpServerRequest request) {
        String entId = request.getParam("ent_id");
        String userId = request.headers().get("User-Id");

        ParametersHelper.hasMissingOrEmptyParameters(new String[] {entId, userId}, handler -> {
            if (handler.isRight()) {
                documentService.delete(entId, userId, defaultResponseHandler(request));
            }
            else {
                badRequest(request, "[Jupyter@deleteFile] " + handler.left().getValue());
            }
        });
    }
}
