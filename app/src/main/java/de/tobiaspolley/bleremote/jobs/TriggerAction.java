package de.tobiaspolley.bleremote.jobs;

import de.tobiaspolley.bleremote.structs.Action;

public class TriggerAction extends Job {
    private Action action;

    public TriggerAction(Action action) {
        super(false);
        this.action = action;
    }

    public Action getAction() {
        return action;
    }
}
