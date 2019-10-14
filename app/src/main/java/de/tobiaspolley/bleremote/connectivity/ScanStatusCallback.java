package de.tobiaspolley.bleremote.connectivity;

public interface ScanStatusCallback {
    void onScanStart();
    void onScanEnd(boolean success);
}
