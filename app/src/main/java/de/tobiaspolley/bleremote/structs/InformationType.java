package de.tobiaspolley.bleremote.structs;

public enum InformationType {
    PORT_VALUE(0x0),
    MODE_INFO(0x1),
    MODE_COMBINATIONS(0x2);

    private int value;

    private InformationType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
