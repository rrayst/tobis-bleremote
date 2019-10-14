package de.tobiaspolley.bleremote.structs;

public enum Action {
    SWITCH_OFF(0x01),
    DISCONNECT(0x02),
    VCC_PORT_ON(0x03),
    VCC_PORT_OFF(0x04),
    ACTIVATE_BUSY_INDICATION(0x05),
    RESET_BUSY_INDICATION(0x06)
    ;

    private byte action;

    private Action(int action) {
        this.action = (byte) action;
    }

    public byte getAction() {
        return action;
    }
}
