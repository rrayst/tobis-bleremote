package de.tobiaspolley.bleremote.structs;

public enum MappingOptions {
    DISCRETE(2),
    RELATIVE(3),
    ABSOLUTE(4),
    SUPPORT_FUNCTIONAL_MAPPING(6),
    SUPPORTS_NULL(7);

    private final int bit;

    private MappingOptions(int bit) {
        this.bit = bit;
    }

    public int getBit() {
        return bit;
    }
}
