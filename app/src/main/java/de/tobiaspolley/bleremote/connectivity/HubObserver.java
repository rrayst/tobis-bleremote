package de.tobiaspolley.bleremote.connectivity;

public interface HubObserver {
    void onHubAdded(int index);
    void onHubRemoved(int index);
}
