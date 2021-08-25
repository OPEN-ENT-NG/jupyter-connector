package fr.openent.jupyter.controllers;

import fr.openent.jupyter.models.Directory;
import fr.openent.jupyter.models.File;
import fr.openent.jupyter.security.AccessRight;
import fr.openent.jupyter.service.DocumentService;
import fr.openent.jupyter.service.Impl.DefaultDocumentService;
import fr.openent.jupyter.utils.WorkspaceType;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.bus.WorkspaceHelper;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;

import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class DirectoryController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(DirectoryController.class);
    private final DocumentService documentService;
    private final WorkspaceHelper workspaceHelper;

    public DirectoryController(EventBus eb, Storage storage) {
        super();
        documentService = new DefaultDocumentService(eb, storage);
        workspaceHelper = new WorkspaceHelper(eb, storage);
    }

    @Get("/directory/base")
    @ApiDoc("Get src directory")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getBaseDirectory(HttpServerRequest request) {
        String entId = request.getParam("ent_id");
        String userId = request.headers().get("User-Id");

        if (!entId.trim().isEmpty()) {
            log.error("[Jupyter@getDirectoryBase] Ent id must be empty, not '" + entId + "'");
            badRequest(request);
        }

        // Get documents from src Workspace
        JsonObject baseDrc = new Directory().getSourceDirectoryInfos();
        renderMeAndMyChildren(request, baseDrc, userId);

//       // TODO keep this commented part (originally for the idea that all Jupyter files would into a specific folder called 'Jupyter' on the workspace)
//
//        documentService.list(entId, userId, listSrcDocuments -> {
//            if (listSrcDocuments.isRight()) {
//                JsonArray srcDocuments = listSrcDocuments.right().getValue();
//
//                JsonObject baseDrc = new Directory().getSourceDirectoryInfos();
//                baseDrc.put("content", srcDocuments);
//                renderJson(request, baseDrc, 200);
//
////                        // Get Jupyter folder (if exists) from workspace list
////                        JsonObject jupyterFolder = new JsonObject();
////                        int i = 0;
////                        while (jupyterFolder.size() <= 0 && i < srcDocuments.size()) {
////                            JsonObject document = srcDocuments.getJsonObject(i);
////                            if (document.getString("name").equals(Jupyter.WORKSPACE_SRC_DIRECTORY_NAME) &&
////                                    document.getString("eType").equals("folder")) {
////                                jupyterFolder = document;
////                            }
////                            i++;
////                        }
////
////                        // Create folder if Jupyter does not exists, list content if it does
////                        if (jupyterFolder.size() <= 0) {
////                            JsonObject jupyterInfos = new Directory().getSourceDirectoryInfos();
////                            createFolder(request, jupyterInfos, user);
////                        }
////                        else {
////                            JsonObject jupyter = new Directory(jupyterFolder).toJson();
////                            renderMeAndMyChildren(request, jupyter, user);
////                        }
//            }
//            else {
//                log.error(listSrcDocuments.left().getValue());
//                badRequest(request);
//            }
//        });
    }

    @Get("/directory")
    @ApiDoc("Get a specific directory")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getDirectory(HttpServerRequest request) {
        String entId = request.getParam("ent_id");
        String userId = request.headers().get("User-Id");

        workspaceHelper.getDocument(entId, getFolder -> {
            if (getFolder.succeeded()) {
                JsonObject directory = new Directory(getFolder.result().body().getJsonObject("result")).toJson();
                renderMeAndMyChildren(request, directory, userId);
            }
            else {
                log.error(getFolder.cause().getMessage());
                badRequest(request);
            }
        });
    }

    @Post("/directory")
    @ApiDoc("Create a new directory")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void createDirectory(HttpServerRequest request) {
        String userId = request.headers().get("User-Id");
        String userName = request.headers().get("User-Name");
        RequestUtils.bodyToJson(request, body -> createFolder(request, body, userId, userName));
    }

    @Delete("/directory")
    @ApiDoc("Delete a specific directory")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void deleteDirectory(HttpServerRequest request) {
        String entId = request.getParam("ent_id");
        String userId = request.headers().get("User-Id");
        documentService.delete(entId, userId, defaultResponseHandler(request));
    }

    private void renderMeAndMyChildren(HttpServerRequest request, JsonObject directory, String userId) {
        documentService.list(directory.getString("id"), userId, listChildren -> {
            if (listChildren.isRight()) {
                JsonArray children = listChildren.right().getValue();

                // Fill content of directory
                for (Object child : children) {
                    JsonObject doc = (JsonObject) child;
                    if (doc.getString("eType").equals(WorkspaceType.FOLDER.getName())) {
                        directory.getJsonArray("content").add(new Directory(doc).toJson());
                    }
                    else {
                        directory.getJsonArray("content").add(new File(doc).toJson().put("content", ""));
                    }
                }

                renderJson(request, directory, 200);
            }
            else {
                log.error(listChildren.left().getValue());
                badRequest(request);
            }
        });
    }

    private void createFolder(HttpServerRequest request, JsonObject directoryInfos, String userId, String userName) {
        documentService.createFolder(directoryInfos, userId, userName, createFolder -> {
            if (createFolder.isRight()) {
                JsonObject directory = new Directory(createFolder.right().getValue()).toJson();
                renderJson(request, directory, 200);
            }
            else {
                log.error(createFolder.left().getValue());
                badRequest(request);
            }
        });
    }
}