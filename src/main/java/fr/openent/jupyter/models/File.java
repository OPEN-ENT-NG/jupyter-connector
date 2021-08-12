package fr.openent.jupyter.models;

import fr.openent.jupyter.utils.JupyterType;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class File {
    private static final Logger log = LoggerFactory.getLogger(File.class);
    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm.s.S");

    public String id;
    public String name;
    public String path;
    public String last_modified;
    public String created;
    public String type;
    public String format;
    public String mimetype;
    public boolean writable;

    public File(JsonObject document) {
        String mimetype = document.getJsonObject("metadata").getString("content-type");

        try {
            this.id = document.getString("_id");
            this.name = document.getString("name");
            this.last_modified = formatter.parse(document.getString("modified")).toInstant().toString();
            this.created = formatter.parse(document.getString("created")).toInstant().toString();
            this.type = mimetype == null ? JupyterType.NOTEBOOK.getName() : JupyterType.FILE.getName();
            this.format = mimetype == null ? "json" : mimetype.equals("text/plain") ? "text" : "base64";
            this.mimetype = mimetype;
            this.writable = true;
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public File() {}

    public JsonObject toJson() {
        return new JsonObject()
                .put("id", this.id)
                .put("name", this.name)
                .put("last_modified", this.last_modified)
                .put("created", this.created)
                .put("type", this.type)
                .put("format", this.format)
                .put("mimetype", this.mimetype)
                .put("writable", this.writable);
    }
}
