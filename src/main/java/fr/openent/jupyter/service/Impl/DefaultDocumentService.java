package fr.openent.jupyter.service.Impl;

import fr.openent.jupyter.Jupyter;
import fr.openent.jupyter.service.DocumentService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.storage.Storage;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class DefaultDocumentService implements DocumentService {
    Logger log = LoggerFactory.getLogger(DefaultDocumentService.class);
    private final String WORKSPACE_BUS_ADDRESS = Jupyter.WORKSPACE_BUS_ADDRESS;
    private final EventBus eb;
    private final Storage storage;

    public DefaultDocumentService(EventBus eb, Storage storage) {
        this.eb = eb;
        this.storage = storage;
    }

    @Override
    public void list(String folderId, String userId, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "list")
                .put("userId", userId)
                .put("filter", "owner")
                .put("parentId", folderId);
        eb.request(WORKSPACE_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if (!"ok".equals(body.getString("status"))) {
                String errorMessage = "[DefaultDocumentService@list] An error occurred when calling document by event bus : ";
                String errorEb = message.body().getString("message");
                handler.handle(new Either.Left<>(errorMessage + errorEb));
            } else {
                handler.handle(new Either.Right<>(message.body().getJsonArray("result")));
            }
        }));
    }

    @Override
    public void createFolder(JsonObject folder, String userId, String userName, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "addFolder")
                .put("name", folder.getString("name"))
                .put("owner", userId)
                .put("ownerName", userName)
                .put("parentFolderId", folder.getString("parent_id"));
        ebHandling(action, "create", handler);
    }

    @Override
    public void update(String documentId, JsonObject uploaded, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "updateDocument")
                .put("id", documentId)
                .put("uploaded", uploaded);
        ebHandling(action, "update", handler);
    }

    @Override
    public void delete(String itemId, String userId, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "delete")
                .put("id", itemId)
                .put("userId", userId);
        ebHandling(action, "delete", handler);
    }

    @Override
    public void rename(String itemId, String userId, String name, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "rename")
                .put("id", itemId)
                .put("userId", userId)
                .put("name", name);
        ebHandling(action, "rename", handler);
    }

    private void ebHandling(JsonObject action, String functionName, Handler<Either<String, JsonObject>> handler) {
        eb.request(WORKSPACE_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if (!"ok".equals(body.getString("status"))) {
                String errorMessage = "[DefaultDocumentService@" + functionName + "] An error occurred when calling document by event bus : ";
                String errorEb = message.body().getString("message");
                handler.handle(new Either.Left<>(errorMessage + errorEb));
            } else {
                handler.handle(new Either.Right<>(message.body()));
            }
        }));
    }
}
