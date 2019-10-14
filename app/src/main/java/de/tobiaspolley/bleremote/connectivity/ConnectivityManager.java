package de.tobiaspolley.bleremote.connectivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

import androidx.room.Room;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import de.tobiaspolley.bleremote.db.AppDatabase;
import de.tobiaspolley.bleremote.db.Hub;
import de.tobiaspolley.bleremote.jobs.Job;

public class ConnectivityManager {
    private final Context context;
    private final AppDatabase db;
    private final Handler handler;

    private List<Runnable> connectivityChangeListeners = new ArrayList<>();
    private List<HubObserver> hubObservers = new ArrayList<>();
    private List<PortObserver> portObservers = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private HashMap<Integer, HubInfo> hubInfos = new HashMap<>();

    private class DeviceInfo {
        int ioType;

        public DeviceInfo(int ioType) {
            this.ioType = ioType;
        }
    }

    private class HubInfo {
        Queue<Job> jobsToTransmit = new ArrayBlockingQueue<Job>(1000);
        boolean isConnected = false;
        private Hub hub;
        HashMap<Integer, DeviceInfo> ports = new HashMap<>();

        public HubInfo(Hub hub) {
            this.hub = hub;
        }
    }

    private ConnectivityManager(final Context context) {
        this.context = context;
        this.handler = new Handler(context.getMainLooper());

        db = Room.databaseBuilder(context, AppDatabase.class, "hubs").build();

        new HubLoader().execute();
    }

    private static ConnectivityManager instance;

    public static synchronized ConnectivityManager getInstance(Context context) {
        if (instance == null)
            instance = new ConnectivityManager(context);
        return instance;
    }

    public void addConnectivityChangeListener(Runnable runnable) {
        connectivityChangeListeners.add(runnable);
    }

    public void removeConnectivityChangeListener(Runnable runnable) {
        connectivityChangeListeners.remove(runnable);
    }

    public void addHubObserver(HubObserver hubObserver) {
        hubObservers.add(hubObserver);
    }

    public void removeHubObserver(HubObserver hubObserver) {
        hubObservers.remove(hubObserver);
    }

    public void addPortObserver(PortObserver portObserver) {
        portObservers.add(portObserver);
    }

    public void removePortObserver(PortObserver portObserver) {
        portObservers.remove(portObserver);
    }

    private void fireConnectivityChanged() {
        List<Runnable> runnables;
        synchronized (this) {
            runnables = new ArrayList<>(connectivityChangeListeners);
        }
        for (Runnable runnable : runnables) {
            runnable.run();
        }
    }

    public void connect(final int index, ScanStatusCallback statusCallback) throws BluetoothNotAvailableException {
        HubInfo hubInfo = hubInfos.get(index);
        if (hubInfo == null)
            throw new IllegalArgumentException("Hub with index " + index + " not found.");

        final Hub hub = hubInfo.hub;

        Function<BluetoothDevice, Boolean> bluetoothDeviceFinder = new Function<BluetoothDevice, Boolean>() {

            @NullableDecl
            @Override
            public Boolean apply(@NullableDecl BluetoothDevice device) {
                if (hub.name.equals(device.getName()) && (hub.mac == null || device.getAddress().equals(hub.mac))) {
                    return true;
                }
                return false;
            }
        };
        Function<BluetoothDevice, Void> bluetoothDeviceHandler = new Function<BluetoothDevice, Void>() {
            @NullableDecl
            @Override
            public Void apply(@NullableDecl BluetoothDevice device) {
                device.connectGatt(context, true, new TheBluetoothGattCallback(context, index));
                return null;
            }
        };

        BluetoothDevice device = locateBondedDevice(bluetoothDeviceFinder, bluetoothDeviceHandler);
        if (device != null) {
            return;
        }

        System.out.println("device is not bonded.");

        startScan(bluetoothDeviceFinder, bluetoothDeviceHandler, statusCallback);
    }

    private String stateToString(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_ON:
                return "ON";
            case BluetoothAdapter.STATE_OFF:
                return "OFF";
            case BluetoothAdapter.STATE_CONNECTED:
                return "CONNECTED";
            case BluetoothAdapter.STATE_CONNECTING:
                return "CONNECTING";
            default:
                return "" + state;
        }
    }

    public void enqueue(int index, Job job) {
        Queue<Job> jobsToTransmit = hubInfos.get(index).jobsToTransmit;

        if (job.canReplaceOtherJob()) {
            Iterator<Job> i = jobsToTransmit.iterator();
            while (i.hasNext()) {
                Job j = i.next();
                if (job.canReplaceOtherJob(j))
                    i.remove();
            }
        }

        jobsToTransmit.add(job);
    }

    public boolean isConnected(int index) {
        return hubInfos.get(index).isConnected;
    }

    public void setConnected(int index, boolean isConnected) {
        hubInfos.get(index).isConnected = isConnected;
        fireConnectivityChanged();
    }

    public Job poll(int index) {
        return hubInfos.get(index).jobsToTransmit.poll();
    }

    public Set<Integer> getIndexes() {
        return hubInfos.keySet();
    }

    public void scanForNewHub(ScanStatusCallback statusCallback) throws BluetoothNotAvailableException {
        startScan(new Function<BluetoothDevice, Boolean>() {
            @NullableDecl
            @Override
            public Boolean apply(@NullableDecl BluetoothDevice device) {
                for (HubInfo value : hubInfos.values()) {
                    if (value.hub.name.equals(device.getName()) && (value.hub.mac == null || device.getAddress().equals(value.hub.mac))) {
                        return false;
                    }
                }

                if ("Technic Hub".equals(device.getName())) {

                    return true;
                }

                return false;
            }
        }, new Function<BluetoothDevice, Void>() {
            @NullableDecl
            @Override
            public Void apply(@NullableDecl BluetoothDevice device) {
                final Hub hub = new Hub();
                hub.mac = device.getAddress();
                hub.name = device.getName();

                new HubSaver().execute(hub);

                return null;
            }
        }, statusCallback);
    }

    private void startScan(final Function<BluetoothDevice, Boolean> scanSuccessful, final Function<BluetoothDevice, Void> bluetoothDeviceHandler, final ScanStatusCallback statusCallback) throws BluetoothNotAvailableException {
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
            throw new BluetoothNotAvailableException();

        if (bluetoothAdapter.isDiscovering()) {
            if (!bluetoothAdapter.cancelDiscovery())
                System.out.println("cancelling discovery failed.");
        }

        final BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        final ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                if (scanSuccessful.apply(result.getDevice())) {
                    bluetoothAdapter.getBluetoothLeScanner().stopScan(this);
                    statusCallback.onScanEnd(true);
                    bluetoothDeviceHandler.apply(result.getDevice());
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                for (ScanResult result : results) {
                    onScanResult(0, result);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                System.out.println("scan failed " + errorCode);
                statusCallback.onScanEnd(false);
                super.onScanFailed(errorCode);
            }
        };
        scanner.startScan(new ArrayList<ScanFilter>(), new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setReportDelay(0)
                .build(), scanCallback);
        statusCallback.onScanStart();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                statusCallback.onScanEnd(false);
                scanner.stopScan(scanCallback);
            }
        }, 30 * 1000);
    }

    class HubLoader extends AsyncTask<Void, Void, List<Hub>> {
        @Override
        protected List<Hub> doInBackground(Void... voids) {
            return db.hubDao().getAll();
        }

        @Override
        protected void onPostExecute(List<Hub> hubs) {
            for (Hub hub : hubs) {
                hubInfos.put(hub.uid, new HubInfo(hub));
                for (HubObserver hubObserver : hubObservers)
                    hubObserver.onHubAdded(hub.uid);
            }
        }
    }

    class HubForgetter extends AsyncTask<Integer, Void, Set<Integer>> {
        @Override
        protected Set<Integer> doInBackground(Integer... ids) {
            HashSet<Integer> ids2 = Sets.newHashSet(ids);
            for (Hub hub : db.hubDao().getAll()) {
                if (ids2.contains(hub.uid))
                    db.hubDao().delete(hub);
            }
            return ids2;
        }

        @Override
        protected void onPostExecute(Set<Integer> hubIds) {
            for (Integer hubId : hubIds) {
                hubInfos.remove(hubId);
                for (HubObserver hubObserver : hubObservers)
                    hubObserver.onHubRemoved(hubId);
            }
        }
    }

    class HubSaver extends AsyncTask<Hub, Void, List<Hub>> {
        @Override
        protected List<Hub> doInBackground(Hub... hubs) {
            List<Long> ids = db.hubDao().insertAll(hubs);
            for (int i = 0; i < hubs.length; i++)
                hubs[i].uid = ids.get(i).intValue();
            return Lists.newArrayList(hubs);
        }

        @Override
        protected void onPostExecute(List<Hub> hubs) {
            for (Hub hub : hubs) {
                hubInfos.put(hub.uid, new HubInfo(hub));
                for (HubObserver hubObserver : hubObservers)
                    hubObserver.onHubAdded(hub.uid);
            }
        }
    }

    private BluetoothDevice locateBondedDevice(Function<BluetoothDevice, Boolean> matchCriterium, Function<BluetoothDevice, Void> bluetoothDeviceHandler) throws BluetoothNotAvailableException {
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null)
            throw new IllegalStateException("No Bluetooth Manager found.");
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
            throw new BluetoothNotAvailableException();

        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            if (matchCriterium.apply(device)) {
                bluetoothDeviceHandler.apply(device);
                return device;
            }
        }

        return null;
    }

    public void setAttached(int index, byte port, int ioType) {
        if (ioType >= 0) {
            hubInfos.get(index).ports.put((int) port, new DeviceInfo(ioType));

            for (PortObserver portObserver : portObservers) {
                portObserver.onPortConnected(index, port);
            }
        } else {
            hubInfos.get(index).ports.remove((int) port);

            for (PortObserver portObserver : portObservers) {
                portObserver.onPortDisconnected(index, port);
            }
        }
    }

    public int getPortIOType(int index, int port) {
        DeviceInfo deviceInfo = hubInfos.get(index).ports.get(port);
        if (deviceInfo == null)
            return -1;
        return deviceInfo.ioType;
    }

    public Hub getHub(int index) {
        return hubInfos.get(index).hub;
    }

    public void forgetHub(int index) {
        new HubForgetter().execute(index);
    }

}