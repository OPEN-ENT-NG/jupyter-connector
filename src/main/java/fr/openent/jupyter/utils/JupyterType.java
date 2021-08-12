package fr.openent.jupyter.utils;

import java.util.HashMap;
import java.util.Map;

public enum JupyterType {
    DIRECTORY("directory"),
    FILE("file"),
    NOTEBOOK("notebook"),
    ERROR("error");

    private final String name;

    private static final Map<String, JupyterType> lookup = new HashMap<>();

    static {
        for (JupyterType type : JupyterType.values()) {
            lookup.put(type.getName(), type);
        }
    }

    JupyterType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static JupyterType get(String name) {
        return lookup.getOrDefault(name, JupyterType.ERROR);
    }

}
