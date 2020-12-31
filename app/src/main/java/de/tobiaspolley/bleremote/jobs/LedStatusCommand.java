package de.tobiaspolley.bleremote.jobs;

public class LedStatusCommand extends PortOutputCommand {
    private int motor;
    private boolean on;

    public LedStatusCommand(int motor, boolean on) {
        this.motor = motor;
        this.on = on;
    }

    public int getMotor() {
        return motor;
    }

    public boolean isOn() {
        return on;
    }

    @Override
    public boolean canReplaceOtherJob() {
        return true;
    }

    @Override
    public boolean canReplaceOtherJob(Job j) {
        return j instanceof LedStatusCommand && ((LedStatusCommand) j).getMotor() == motor;
    }
}
