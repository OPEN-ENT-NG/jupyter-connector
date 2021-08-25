package fr.openent.jupyter.security;

import fr.openent.jupyter.Jupyter;

public enum WorkflowActions {
    ACCESS_RIGHT (Jupyter.ACCESS_RIGHT);

    private final String actionName;

    WorkflowActions(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String toString () {
        return this.actionName;
    }
}
