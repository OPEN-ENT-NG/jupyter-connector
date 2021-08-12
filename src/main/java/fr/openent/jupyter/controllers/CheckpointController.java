package fr.openent.jupyter.controllers;

import fr.wseduc.rs.*;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;

// TODO Implement the manual checkpoints system (return default values for now)
public class CheckpointController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(CheckpointController.class);

    public CheckpointController(EventBus eb, Storage storage) {
        super();
    }

    @Get("/checkpoints/all")
    @ApiDoc("List the checkpoints of a specific file")
    public void listCheckpoints(HttpServerRequest request) {
        String entId = request.getParam("ent_id");
    }

    @Get("/checkpoints")
    @ApiDoc("Get a specific checkpoint's file")
    public void getCheckpoint(HttpServerRequest request) {
        String entId = request.getParam("ent_id");

        JsonObject checkpoint = new JsonObject()
                .put("id", "")
                .put("content", "")
                .put("last_modified", "");
        renderJson(request, checkpoint);
    }

    // Automatiquement géré à chaque création / update de document
    @Post("/checkpoints")
    @ApiDoc("Create a checkpoint's file")
    public void createCheckpoint(HttpServerRequest request) {
        UserInfos user = new UserInfos();
        user.setUserId(request.headers().get("User-Id"));
        user.setUsername(request.headers().get("User-Name"));

        JsonObject checkpoint = new JsonObject()
                .put("id", "")
                .put("content", "")
                .put("last_modified", "");
        renderJson(request, checkpoint);
    }

    // Automatiquement géré à chaque delete / update de document
    @Delete("/checkpoints")
    @ApiDoc("Delete a specific checkpoint's file")
    public void deleteCheckpoint(HttpServerRequest request) {
        String entId = request.getParam("ent_id");
        String userId = request.headers().get("User-Id");
    }

    @Put("/checkpoints/rename")
    @ApiDoc("Rename a specific checkpoint's file")
    public void renameCheckpoint(HttpServerRequest request) {
    }
}
