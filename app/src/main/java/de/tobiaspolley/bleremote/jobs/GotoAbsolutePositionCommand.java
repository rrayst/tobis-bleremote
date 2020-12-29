package de.tobiaspolley.bleremote.jobs;

public class GotoAbsolutePositionCommand extends PortOutputCommand {
    private int motor;
    private int speed;
    private int maxPower;
    private int position;
    private int endState;

    public static final int ENDSTATE_FLOAT = 0;
    public static final int ENDSTATE_HOLD = 126;
    public static final int ENDSTATE_BREAK = 127;

    public GotoAbsolutePositionCommand(int motor, int position, int endState) {
        this.motor = motor;
        this.speed = 100;
        this.maxPower = 100;
        this.position = position;
        this.endState = endState;
    }

    public int getMotor() {
        return motor;
    }

    public int getSpeed() {
        return speed;
    }

    public int getMaxPower() {
        return maxPower;
    }

    public int getPosition() {
        return position;
    }

    public int getEndState() {
        return endState;
    }

    @Override
    public boolean canReplaceOtherJob() {
        return true;
    }

    @Override
    public boolean canReplaceOtherJob(Job j) {
        return j instanceof GotoAbsolutePositionCommand && ((GotoAbsolutePositionCommand) j).getMotor() == motor;
    }
}
