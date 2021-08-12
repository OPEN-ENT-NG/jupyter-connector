package fr.openent.jupyter.models;

import fr.openent.jupyter.Jupyter;
import fr.openent.jupyter.utils.JupyterType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Directory {
    private static final Logger log = LoggerFactory.getLogger(Directory.class);
    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm.s.S");

    public String id;
    public String name;
    public String last_modified;
    public String created;
    public String type;
    public String format;
    public boolean writable;
    public String mimetype;

    public Directory(JsonObject folder) {
        try {
            this.id = folder.getString("_id");
            this.name = folder.getString("name");
            this.last_modified = formatter.parse(folder.getString("modified")).toInstant().toString();
            this.created = formatter.parse(folder.getString("created")).toInstant().toString();
            this.type = JupyterType.DIRECTORY.getName();
            this.format = "json";
            this.mimetype = null;
            this.writable = true;
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public Directory() {
        this.type = JupyterType.DIRECTORY.getName();
        this.format = "json";
        this.mimetype = null;
        this.writable = true;
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("id", this.id)
                .put("name", this.name)
                .put("last_modified", this.last_modified)
                .put("created", this.created)
                .put("type", this.type)
                .put("format", this.format)
                .put("mimetype", this.mimetype)
                .put("writable", this.writable)
                .put("content", new JsonArray());
    }

    public JsonObject getSourceDirectoryInfos () {
        return this.toJson()
                .put("parent_id", "")
                .put("id", "")
                .put("name", Jupyter.WORKSPACE_SRC_DIRECTORY_NAME)
                .put("last_modified", new Date().toString())
                .put("created", new Date().toString())
                .put("writable", true);
    }
}
