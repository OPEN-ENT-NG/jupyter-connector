package fr.openent.jupyter.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface DocumentService {

    /**
     * List ??
     *
     * @param folderId      Folder id
     * @param userId        User id
     * @param handler       Function handler returning data
     */
    void list(String folderId, String userId, Handler<Either<String, JsonArray>> handler);

    /**
     * Create a new folder in workspace
     *
     * @param folder    Folder to create
     * @param userId    User id
     * @param userName  User name
     * @param handler   Function handler returning data
     */
    void createFolder(JsonObject folder, String userId, String userName, Handler<Either<String, JsonObject>> handler);

    /**
     * Update file in workspace
     *
     * @param documentId    document id to update
     * @param uploaded      new file uploaded
     * @param handler       Function handler returning data
     */
    void update(String documentId, JsonObject uploaded, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete item from workspace
     *
     * @param itemId    Item id
     * @param userId        User id
     * @param handler   Function handler returning data
     */
    void delete(String itemId, String userId, Handler<Either<String, JsonObject>> handler);

    /**
     * Rename item from workspace
     *
     * @param itemId    Item id
     * @param userId    User id
     * @param name      New name of the item
     * @param handler   Function handler returning data
     */
    void rename(String itemId, String userId, String name, Handler<Either<String, JsonObject>> handler);
}
