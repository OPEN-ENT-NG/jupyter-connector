package fr.openent.jupyter.utils;

import java.util.HashMap;
import java.util.Map;

public enum WorkspaceType {
    FOLDER("folder"),
    FILE("file"),
    ERROR("error");

    private final String name;

    private static final Map<String, WorkspaceType> lookup = new HashMap<>();

    static {
        for (WorkspaceType type : WorkspaceType.values()) {
            lookup.put(type.getName(), type);
        }
    }

    WorkspaceType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static WorkspaceType get(String name) {
        return lookup.getOrDefault(name, WorkspaceType.ERROR);
    }

}
