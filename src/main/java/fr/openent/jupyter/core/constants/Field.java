package fr.openent.jupyter.core.constants;

public class Field {

    private Field() {
        throw new IllegalStateException("Utility class");
    }

    public static final String ID = "id";
    public static final String DISPLAYNAME = "displayName";
    public static final String USER = "user";
    public static final String JUPYTERHUB_USER = "JUPYTERHUB_USER";

}
