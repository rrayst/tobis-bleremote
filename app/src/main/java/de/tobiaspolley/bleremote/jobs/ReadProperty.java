package de.tobiaspolley.bleremote.jobs;

public class ReadProperty extends Job {
    private byte property;

    public ReadProperty(int property) {
        super(true);
        this.property = (byte)property;
    }

    public byte getProperty() {
        return property;
    }
}
