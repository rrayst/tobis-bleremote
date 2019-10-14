package de.tobiaspolley.bleremote.jobs;

public class StartPowerCommand extends PortOutputCommand {
    private int motor;
    private int speed;

    public StartPowerCommand(int motor, int speed) {
        this.motor = motor;
        this.speed = speed;
    }

    public int getMotor() {
        return motor;
    }

    public int getSpeed() {
        return speed;
    }

    @Override
    public boolean canReplaceOtherJob() {
        return true;
    }

    @Override
    public boolean canReplaceOtherJob(Job j) {
        return j instanceof StartPowerCommand && ((StartPowerCommand) j).getMotor() == motor;
    }
}
