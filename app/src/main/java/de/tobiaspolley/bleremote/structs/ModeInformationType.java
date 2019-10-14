package de.tobiaspolley.bleremote.structs;

public enum ModeInformationType {
    NAME(0x0),
    RAW(0x1),
    PCT(0x2),
    SI(0x3),
    SYMBOL(0x4),
    MAPPING(0x5),
    USED_INTERNALLY(0x6),
    MOTOR_BIAS(0x7),
    CAPABILITY_BITS(0x8),
    VALUE_ENCODING(0x80),
    ;

    private byte value;

    private ModeInformationType(int value) {
        this.value = (byte)value;
    }

    public byte getValue() {
        return value;
    }
}
