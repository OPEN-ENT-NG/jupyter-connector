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
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.storage.Storage;

public class ManagerController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(ManagerController.class);
    private final DocumentService documentService;

    public ManagerController(EventBus eb, Storage storage) {
        super();
        documentService = new DefaultDocumentService(eb, storage);
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
               String queryUsersNeo4j = "MATCH (u:User) WHERE u.login={JUPYTERHUB_USER} RETURN u";
               Neo4j.getInstance().execute(queryUsersNeo4j, params, Neo4jResult.validUniqueResultHandler(getNeoEvent -> {
                   if (getNeoEvent.isRight()) {
                       JsonObject userinfos = getNeoEvent.right().getValue();
                       if (userinfos != null && !userinfos.isEmpty()) {
                           renderJson(request, userinfos.getJsonObject("u").getJsonObject("data"));
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

        ParametersHelper.hasMissingOrEmptyParameters(new String[] {entId, name, userId}, handler -> {
            if (handler.isRight()) {
                documentService.rename(entId, userId, name, renameItem -> {
                    if (renameItem.isRight()) {
                        JsonObject item = renameItem.right().getValue();

                        if (item.getString("eType").equals(WorkspaceType.FOLDER.getName())) {
                            renderJson(request, new Directory(item).toJson());
                        }
                        else {
                            renderJson(request, new File(item).toJson().put("content", ""));
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
}
