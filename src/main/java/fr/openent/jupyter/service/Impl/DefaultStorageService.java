package fr.openent.jupyter.service.Impl;

import fr.openent.jupyter.Jupyter;
import fr.openent.jupyter.service.StorageService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.entcore.common.storage.Storage;

import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class DefaultStorageService implements StorageService {

    private final Storage storage;

    public DefaultStorageService(Storage storage) {
        this.storage = storage;
    }

    @Override
    public void add(JsonObject body, Handler<Either<String, JsonObject>> handler) {
        String content;
        byte[] byteContent;
        String contentType;

        switch (body.getString("format")) {
            case "json": // Case notebook created from Jupyter
                content = body.getJsonObject("content").toString();
                byteContent = content.getBytes(StandardCharsets.UTF_8);
                contentType = null;
                break;
            case "text": // Case text created from Jupyter
                byteContent = body.getString("content").getBytes(StandardCharsets.UTF_8);
                contentType = "text/plain";
                break;
            case "base64": // Case file imported in Jupyter from local computer
                String docName = body.getString("name");
                if (docName.contains(".")) {
                    String fileExtension = docName.substring(docName.lastIndexOf("."));

                    if (fileExtension.equals(Jupyter.EXTENSION_NOTEBOOK)) { // Case notebook
                        content = new String(Base64.getDecoder().decode(body.getString("content")));
                        byteContent = content.getBytes(StandardCharsets.UTF_8);
                        contentType = null;
                    }
                    else if (Jupyter.EXTENSIONS_TEXT.contains(fileExtension)) { // Case text
                        content = new String(Base64.getDecoder().decode(body.getString("content")));
                        byteContent = content.getBytes(StandardCharsets.UTF_8);
                        contentType = URLConnection.getFileNameMap().getContentTypeFor(docName);
                    }
                    else { // Case other type files
                        byteContent = Base64.getDecoder().decode(body.getString("content").getBytes(StandardCharsets.UTF_8));
                        contentType = URLConnection.getFileNameMap().getContentTypeFor(docName);
                    }
                }
                else {
                    handler.handle(new Either.Left<>("[DefaultFileService@getFile] Filename does not contains extension : " + docName));
                    return;
                }
                break;
            default:
                handler.handle(new Either.Left<>("[DefaultFileService@getFile] File type unknown : " + body.getString("format")));
                return;
        }

        Buffer buffer = Buffer.buffer(byteContent);
        storage.writeBuffer(UUID.randomUUID().toString(), buffer, contentType, body.getString("name"), file -> {
            if ("ok".equals(file.getString("status"))) {
                handler.handle(new Either.Right<>(file));
            }
            else {
                handler.handle(new Either.Left<>("[DefaultFileService@add] Failed to upload file from http request"));
            }
        });
    }

    @Override
    public void remove(String storageId, Handler<Either<String, JsonObject>> handler) {
        storage.removeFile(storageId, removeEvent -> {
            if ("ok".equals(removeEvent.getString("status"))) {
                handler.handle(new Either.Right<>(removeEvent));
            }
            else {
                handler.handle(new Either.Left<>("[DefaultFileService@remove] Failed to remove file from storage : " + removeEvent.getString("message")));
            }
        });
    }
}
