package fr.openent.jupyter.controllers;

import fr.openent.jupyter.Jupyter;
import fr.openent.jupyter.helper.ParametersHelper;
import fr.openent.jupyter.models.Directory;
import fr.openent.jupyter.models.File;
import fr.openent.jupyter.security.AccessRight;
import fr.openent.jupyter.service.DocumentService;
import fr.openent.jupyter.service.Impl.DefaultDocumentService;
import fr.openent.jupyter.utils.WorkspaceType;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.bus.WorkspaceHelper;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;

public class ManagerController extends ControllerHelper {
    private final DocumentService documentService;
    private final WorkspaceHelper workspaceHelper;


    public ManagerController(EventBus eb, Storage storage) {
        super();
        documentService = new DefaultDocumentService(eb, storage);
        workspaceHelper = new WorkspaceHelper(eb, storage);

    }

    @Get("")
    @ApiDoc("Render view")
    @SecuredAction(Jupyter.ACCESS_RIGHT)
    public void render(HttpServerRequest request) {
        renderView(request, null, "jupyter-connector.html", null);
    }

    @Get("/userinfos")
    @ApiDoc("Get user infos from Neo4j")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getUserInfos(HttpServerRequest request) {
        String user = request.getParam("user");

        ParametersHelper.hasMissingOrEmptyParameters(new String[] {user}, handler -> {
           if (handler.isRight()) {
               JsonObject params = new JsonObject().put("JUPYTERHUB_USER", request.getParam("user"));
               String queryUsersNeo4j = "MATCH (u:User) WHERE u.login={JUPYTERHUB_USER} RETURN u.displayName, u.id";
               Neo4j.getInstance().execute(queryUsersNeo4j, params, Neo4jResult.validUniqueResultHandler(getNeoEvent -> {
                   if (getNeoEvent.isRight()) {
                       JsonObject userinfos = getNeoEvent.right().getValue();
                       if (userinfos != null && !userinfos.isEmpty()) {
                           renderJson(request, new JsonObject().put("id", userinfos.getString("u.id")).put("displayName", userinfos.getString("u.displayName")));
                       }
                       else {
                           badRequest(request, "[Jupyter@getUserInfos] Incorrect user");
                       }
                   } else {
                       badRequest(request, "[Jupyter@getUserInfos] Fail to get users' infos from Neo4j : " + getNeoEvent.left().getValue());
                   }
               }));
           }
           else {
               badRequest(request, "[Jupyter@getUserInfos] " + handler.left().getValue());
           }
        });
    }

    @Put("/rename")
    @ApiDoc("Rename a specific file or directory")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void rename(HttpServerRequest request) {
        String entId = request.getParam("ent_id");
        String name = request.getParam("name");
        String userId = request.headers().get("User-Id");
        String parentId = request.getParam("parent_id");

        ParametersHelper.hasMissingOrEmptyParameters(new String[] {entId, name, userId}, handler -> {
            if (handler.isRight()) {
                UserInfos user = new UserInfos();
                user.setUserId(userId);
                documentService.rename(entId, userId, name, renameItem -> {
                    if (renameItem.isRight()) {
                        JsonObject item = renameItem.right().getValue();

                        if (parentId == null) { // If parent not change
                            render(request, item);
                        } else {
                            workspaceHelper.moveDocument(entId, parentId.equals("") ? null : parentId, user, moveEvent -> {
                                if (moveEvent.succeeded()) {
                                    render(request, item);
                                } else {
                                    badRequest(request, "[Jupyter@createFile] Failed to move a workspace document : " +
                                            moveEvent.cause().getMessage());
                                }
                            });
                        }
                    }
                    else {
                        badRequest(request, renameItem.left().getValue());
                    }
                });
            }
            else {
                badRequest(request, "[Jupyter@rename] " + handler.left().getValue());
            }
        });
    }

    private void render(HttpServerRequest request, JsonObject item) {
        if (item.getString("eType").equals(WorkspaceType.FOLDER.getName())) {
            renderJson(request, new Directory(item).toJson());
        }
        else {
            renderJson(request, new File(item).toJson().put("content", ""));
        }
    }
}
