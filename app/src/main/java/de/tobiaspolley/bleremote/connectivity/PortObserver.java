package de.tobiaspolley.bleremote.connectivity;

public interface PortObserver {

    void onPortConnected(int index, int port);
    void onPortDisconnected(int index, int port);


}
