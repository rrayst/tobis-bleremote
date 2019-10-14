package de.tobiaspolley.bleremote.responses;

public class PortDisconnectedResponse extends HubResponse {
    private byte port;

    protected PortDisconnectedResponse(String text, byte port) {
        super(text);
        this.port = port;
    }

    public byte getPort() {
        return port;
    }
}
