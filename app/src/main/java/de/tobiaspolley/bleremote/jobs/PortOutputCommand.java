package de.tobiaspolley.bleremote.jobs;

public class PortOutputCommand extends Job {
    private byte startupCompletion;

    public PortOutputCommand() {
        super(false);
    }

    public byte getStartupCompletion() {
        return startupCompletion;
    }
}
