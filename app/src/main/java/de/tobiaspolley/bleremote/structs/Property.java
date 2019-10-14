package de.tobiaspolley.bleremote.structs;

public enum Property {
    FIRMWARE_VERSION(0x03),
    LEGO_WIRELESS_PROTOCOL_VERSION(0x0A);

    private int property;

    private Property(int property) {
        this.property = property;
    }

    public int getProperty() {
        return property;
    }
}
