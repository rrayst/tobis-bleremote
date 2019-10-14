package de.tobiaspolley.bleremote.jobs;

public class PortModeInformationRequest extends Job {
    private int portId;
    private int mode;
    private int informationType;

    public PortModeInformationRequest(int portId, int mode, int informationType) {
        super(true);
        this.portId = portId;
        this.mode = mode;
        this.informationType = informationType;
    }

    public int getPortId() {
        return portId;
    }

    public int getMode() {
        return mode;
    }

    public int getInformationType() {
        return informationType;
    }
}
