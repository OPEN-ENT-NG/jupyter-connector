package fr.openent.jupyter.controllers;

import fr.openent.jupyter.models.File;
import fr.openent.jupyter.service.DocumentService;
import fr.openent.jupyter.service.Impl.DefaultDocumentService;
import fr.openent.jupyter.service.Impl.DefaultStorageService;
import fr.openent.jupyter.service.StorageService;
import fr.wseduc.rs.*;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.bus.WorkspaceHelper;
import org.entcore.common.controller.ControllerHelper;
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
    public void getFile(HttpServerRequest request) {
        String entId = request.getParam("ent_id");

        workspaceHelper.getDocument(entId, getDocumentEvent -> {
            if (getDocumentEvent.succeeded()) {
                JsonObject document = getDocumentEvent.result().body().getJsonObject("result");
                String id = document.getString("file");

                workspaceHelper.readFile(id, content -> {
                    JsonObject file = new File(document).toJson();

                    switch (file.getString("format")) {
                        case "json": // Case notebook
                            file.put("content", content.toJsonObject());
                            break;
                        case "text": // Case file text
                            file.put("content", content.toString());
                            break;
                        case "base64": // Case file not text
                            file.put("content", Base64.getEncoder().encodeToString(content.getBytes()));
                            break;
                        default:
                            log.error("[Jupyter@getFile] File format unknown : " + file.getString("format"));
                            break;
                    }

                    renderJson(request, file);
                });
            }
            else {
                log.error("[Jupyter@getFile] Fail to get document from workspace : " + getDocumentEvent.cause().getMessage());
                badRequest(request);
            }
        });
    }

    @Post("/file")
    @ApiDoc("Create a new file")
    public void createFile(HttpServerRequest request) {
        UserInfos user = new UserInfos();
        user.setUserId(request.headers().get("User-Id"));
        user.setUsername(request.headers().get("User-Name"));

        RequestUtils.bodyToJson(request, body -> {
            storageService.add(body, addFileEvent -> {
                if (addFileEvent.isRight()) {
                    JsonObject storageEntries = addFileEvent.right().getValue();

                    String name = body.getString("name");
                    String application = config.getString("app-name");
                    workspaceHelper.addDocument(storageEntries, user, name, application, false, null, createEvent -> {
                        if (createEvent.succeeded()) {
                            JsonObject doc = createEvent.result().body();

                            JsonObject file = new File(doc).toJson();
                            log.info("[Jupyter@createFile] File created for path '" + body.getString("path") + "' with body " + body);

                            String parentId = body.getString("parent_id");
                            if (parentId == null || parentId.isEmpty()) { // If parent is base directory there's no need to move the document
                                renderJson(request, file, 200);
                            }
                            else {
                                workspaceHelper.moveDocument(file.getString("id"), parentId, user, moveEvent -> {
                                    if (moveEvent.succeeded()) {
                                        renderJson(request, file, 200);
                                    }
                                    else {
                                        log.error("[Jupyter@createFile] Failed to move a workspace document : " + moveEvent.cause().getMessage());
                                        badRequest(request);
                                    }
                                });
                            }
                        }
                        else {
                            log.error("[Jupyter@createFile] Failed to create a workspace document : " + createEvent.cause().getMessage());
                            badRequest(request);
                        }
                    });
                }
                else {
                    log.error("[Jupyter@createFile] Failed to create a new entry in the storage");
                    badRequest(request);
                }
            });
        });
    }

    @Put("/file")
    @ApiDoc("Update a specific file")
    public void updateFile(HttpServerRequest request) {
        String entId = request.getParam("ent_id");

        RequestUtils.bodyToJson(request, body -> {
            storageService.add(body, addFileEvent -> {
                if (addFileEvent.isRight()) {
                    JsonObject uploaded = addFileEvent.right().getValue();

                    documentService.update(entId, uploaded, updateEvent -> {
                        if (updateEvent.isRight()) {
                            log.info("[Jupyter@updateFile] File updated for path '" + body.getString("path") + "' with body " + body);

                            workspaceHelper.getDocument(entId, getDocumentEvent -> {
                                if (getDocumentEvent.succeeded()) {
                                    JsonObject document = getDocumentEvent.result().body().getJsonObject("result");
                                    JsonObject file = new File(document).toJson();
                                    renderJson(request, file);
                                }
                                else {
                                    log.error("[Jupyter@updateFile] Fail to get document from workspace : " + getDocumentEvent.cause().getMessage());
                                    badRequest(request);
                                }
                            });
                        }
                        else {
                            log.error("[Jupyter@updateFile] Failed to update document with new storage id : " + updateEvent.left().getValue());
                            storage.removeFile(uploaded.getString("_id"), event -> {
                                if (!"ok".equals(event.getString("status"))) {
                                    log.error("[Jupyter@updateFile] Error removing file " + uploaded.getString("_id") + " : " + event.getString("message"));
                                }
                                badRequest(request);
                            });
                        }
                    });
                }
                else {
                    log.error(addFileEvent.left().getValue());
                    badRequest(request);
                }
            });
        });
    }

    @Delete("/file")
    @ApiDoc("Delete a specific file")
    public void deleteFile(HttpServerRequest request) {
        String entId = request.getParam("ent_id");
        String userId = request.headers().get("User-Id");
        documentService.delete(entId, userId, defaultResponseHandler(request));
    }
}
