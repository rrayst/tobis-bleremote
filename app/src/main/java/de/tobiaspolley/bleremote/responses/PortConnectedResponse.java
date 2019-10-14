package de.tobiaspolley.bleremote.responses;

public class PortConnectedResponse extends HubResponse {
    public static final int IOTYPE_CONTROLPLUS_MOTOR_L = 46;
    public static final int IOTYPE_CONTROLPLUS_MOTOR_XL = 47;

    private byte port;
    private int ioType;

    protected PortConnectedResponse(String text, byte port, int ioType) {
        super(text);
        this.port = port;
        this.ioType = ioType;
    }

    public byte getPort() {
        return port;
    }

    public int getIoType() {
        return ioType;
    }
}
