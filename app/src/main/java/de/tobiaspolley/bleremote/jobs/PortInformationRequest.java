package de.tobiaspolley.bleremote.jobs;

public class PortInformationRequest extends Job {
    private int portId;
    private int informationType;

    public PortInformationRequest(int portId, int informationType) {
        super(true);
        this.portId = portId;
        this.informationType = informationType;
    }

    public int getPortId() {
        return portId;
    }

    public int getInformationType() {
        return informationType;
    }
}
