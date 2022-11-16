package fr.openent.jupyter.models;

import fr.openent.jupyter.helper.DateHelper;
import fr.openent.jupyter.utils.JupyterType;
import io.vertx.core.json.JsonObject;

public class File {

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

        this.id = document.getString("_id");
        this.name = document.getString("name");

        String mimetype = document.getJsonObject("metadata").getString("content-type");
        String fileExtension = this.name.contains(".") ? this.name.substring(this.name.lastIndexOf(".")) : "";

        this.type = mimetype == null && fileExtension.equals("ipynb") ? JupyterType.NOTEBOOK.getName() : JupyterType.FILE.getName();
        if(mimetype == null && (fileExtension.equals("ipynb") || fileExtension.equals("json")))
            this.format = "json";
        else
            this.format = mimetype != null && mimetype.equals("text/plain") ? "text" : "base64";
        this.mimetype = mimetype;
        this.writable = true;
        this.last_modified = DateHelper.tryFormat(document.getString("modified"));
        this.created = DateHelper.tryFormat(document.getString("created"));
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
