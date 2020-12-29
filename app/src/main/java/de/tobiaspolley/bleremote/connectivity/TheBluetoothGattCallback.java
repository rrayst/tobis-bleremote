package de.tobiaspolley.bleremote.connectivity;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.UUID;

import de.tobiaspolley.bleremote.jobs.Disconnect;
import de.tobiaspolley.bleremote.jobs.GotoAbsolutePositionCommand;
import de.tobiaspolley.bleremote.jobs.Job;
import de.tobiaspolley.bleremote.jobs.PortInformationRequest;
import de.tobiaspolley.bleremote.jobs.PortModeInformationRequest;
import de.tobiaspolley.bleremote.jobs.ReadProperty;
import de.tobiaspolley.bleremote.jobs.StartPowerCommand;
import de.tobiaspolley.bleremote.jobs.TriggerAction;
import de.tobiaspolley.bleremote.responses.HubResponse;
import de.tobiaspolley.bleremote.responses.PortConnectedResponse;
import de.tobiaspolley.bleremote.responses.PortDisconnectedResponse;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
import static de.tobiaspolley.bleremote.Util.bytesToHex;
import static de.tobiaspolley.bleremote.responses.PortConnectedResponse.IOTYPE_CONTROLPLUS_MOTOR_L;
import static de.tobiaspolley.bleremote.responses.PortConnectedResponse.IOTYPE_CONTROLPLUS_MOTOR_XL;
import static de.tobiaspolley.bleremote.ui.MotorFragment.STOP;

public class TheBluetoothGattCallback extends BluetoothGattCallback {
    private UUID SERVICE_REMOTE_CONTROL = UUID.fromString("00001623-1212-efde-1623-785feabcd123");
    private UUID CHARACTERISTIC_REMOTE_CONTROL_COMMANDS = UUID.fromString("00001624-1212-efde-1623-785feabcd123");

    private final ConnectivityManager connectivityManager;
    private final Handler handler;
    private final int index;

    public TheBluetoothGattCallback(Context context, int index) {
        this.connectivityManager = ConnectivityManager.getInstance(context);
        this.index = index;
        this.handler = new Handler(context.getMainLooper());
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == STATE_CONNECTED) {
            connectivityManager.setConnected(index, true);
            System.out.println("discover services");
            gatt.discoverServices();
        } else if (newState == STATE_DISCONNECTED) {
            connectivityManager.setConnected(index, false);
            System.out.println("status: disconnected");
        } else {
            System.out.println("connection status: " + status + "  new status:" + newState);
        }
    }

    int state = 0;

    private void handle(final BluetoothGatt gatt) {
        if (state != 7)
            handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                handle(gatt);
            }
        }, 1000);

        if (state == 0) {
            state = 5;
            BluetoothGattCharacteristic characteristic =
                    gatt.getService(SERVICE_REMOTE_CONTROL)
                            .getCharacteristic(CHARACTERISTIC_REMOTE_CONTROL_COMMANDS);

            UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
            gatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(uuid);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);

            System.out.println("written enable notifications");

            //characteristic.setValue(new byte[]{0x00 /* break */, 0x00 /* channel */});

            return;
        }
        if (state == 1) {
            state = 5;
            BluetoothGattCharacteristic characteristic =
                    gatt.getService(SERVICE_REMOTE_CONTROL)
                            .getCharacteristic(CHARACTERISTIC_REMOTE_CONTROL_COMMANDS);

            characteristic.setValue(new byte[]{
                    0x00 /* hub id */,
                    0x01 /* message type: Hub Properties */,
                    0x03 /* FW version */,
                    0x06 /* property operation: update */
            });
            characteristic.setWriteType(1);
            check(gatt.writeCharacteristic(characteristic));
            System.out.println("issued write (of read)");
            return;
        }
        if (state == 2) {
            state = 3;
            BluetoothGattCharacteristic characteristic =
                    gatt.getService(SERVICE_REMOTE_CONTROL)
                            .getCharacteristic(CHARACTERISTIC_REMOTE_CONTROL_COMMANDS);
            check(gatt.readCharacteristic(characteristic));
            System.out.println("issued read");
            return;
        }
        if (state == 3) {
            state = 4;
            BluetoothGattCharacteristic characteristic =
                    gatt.getService(SERVICE_REMOTE_CONTROL)
                            .getCharacteristic(CHARACTERISTIC_REMOTE_CONTROL_COMMANDS);

            characteristic.setValue(new byte[]{0x0E});
            characteristic.setWriteType(WRITE_TYPE_NO_RESPONSE);
            check(gatt.writeCharacteristic(characteristic));
            System.out.println("issued write (of read)");
            return;
        }
        if (state == 4) {
            state = 8;
            BluetoothGattCharacteristic characteristic =
                    gatt.getService(SERVICE_REMOTE_CONTROL)
                            .getCharacteristic(CHARACTERISTIC_REMOTE_CONTROL_COMMANDS);
            check(gatt.readCharacteristic(characteristic));
            System.out.println("issued read");
            return;
        }
        if (state == 5) {

            Job job = connectivityManager.poll(index);
            if (job == null)
                return;

            BluetoothGattService service = gatt.getService(SERVICE_REMOTE_CONTROL);
            if (service == null) {
                System.out.println("service " + SERVICE_REMOTE_CONTROL + " not found.");
                return;
            }

            BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_REMOTE_CONTROL_COMMANDS);
            if (characteristic == null) {
                System.out.println("characteristic not found.");
                return;
            }


            if (job instanceof StartPowerCommand) {

                StartPowerCommand mj = (StartPowerCommand) job;
                byte channel = (byte) (mj.getMotor());
                byte power = (byte) (Math.abs(mj.getSpeed() - STOP) * 100 / 256);
                if (mj.getSpeed() < STOP)
                    power = (byte) - power;

                characteristic.setValue(new byte[]{
                        0x07 /* message length */,
                        0x00 /* hub id */,
                        (byte)0x81 /* message type: Port Output Command */,
                        channel,
                        ((StartPowerCommand) job).getStartupCompletion(),
                        (byte)0x01 /* subcommand: StartPower(Power) */,
                        power
                });
                characteristic.setWriteType(1);
                check(gatt.writeCharacteristic(characteristic));
                System.out.println("issued start-power");
                return;
            }

            if (job instanceof GotoAbsolutePositionCommand) {

                GotoAbsolutePositionCommand mj = (GotoAbsolutePositionCommand) job;
                byte channel = (byte) (mj.getMotor());
                int value =  (Math.abs(mj.getPosition() - STOP) * 100 / 256);
                if (mj.getPosition() < STOP)
                    value = -value;

                characteristic.setValue(new byte[]{
                        0x0e /* message length */,
                        0x00 /* hub id */,
                        (byte)0x81 /* message type: Port Output Command */,
                        channel,
                        ((GotoAbsolutePositionCommand) job).getStartupCompletion(),
                        (byte)0x0D /* subcommand: StartPower(Power) */,
                        // int32 absPos
                        (byte)value,
                        (byte)(value >>> 8),
                        (byte)(value >>> 16),
                        (byte)(value >>> 24),
                        // int8 speed (0..100)
                        (byte)mj.getSpeed(),
                        // int8 maxPower (0..100)
                        (byte)mj.getMaxPower(),
                        // int8 endState
                        (byte)mj.getEndState(),
                        // use profile [0 = DO NOT USE, 1 = USE PROFILE] / (0x0000000? acc profile 0x000000?0 dec profile)
                        0x11
                });
                characteristic.setWriteType(1);
                check(gatt.writeCharacteristic(characteristic));
                System.out.println("issued goto-absolute-position");
                return;
            }

            if (job instanceof ReadProperty) {
                characteristic.setValue(new byte[]{
                        0x05 /* message length */,
                        0x00 /* hub id */,
                        0x01 /* message type: Hub Properties */,
                        ((ReadProperty) job).getProperty(),
                        0x05 /* property operation: request update */
                });
                characteristic.setWriteType(1);
                check(gatt.writeCharacteristic(characteristic));
                System.out.println("issued property read");
                state = 6;
                return;
            }

            if (job instanceof TriggerAction) {
                characteristic.setValue(new byte[]{
                        0x04 /* message length */,
                        0x00 /* hub id */,
                        0x02 /* message type: Hub Actions */,
                        ((TriggerAction) job).getAction().getAction()
                });
                characteristic.setWriteType(1);
                check(gatt.writeCharacteristic(characteristic));
                System.out.println("issued action");
                state = 6;
                return;
            }

            if (job instanceof PortInformationRequest) {
                characteristic.setValue(new byte[]{
                        0x05 /* message length */,
                        0x00 /* hub id */,
                        0x21 /* message type: Port Information Request */,
                        (byte) ((PortInformationRequest) job).getPortId(),
                        (byte) ((PortInformationRequest) job).getInformationType()
                });
                characteristic.setWriteType(1);
                check(gatt.writeCharacteristic(characteristic));
                System.out.println("issued port information request");
                state = 6;
                return;
            }

            if (job instanceof PortModeInformationRequest) {
                characteristic.setValue(new byte[]{
                        0x06 /* message length */,
                        0x00 /* hub id */,
                        0x22 /* message type: Port Information Request */,
                        (byte) ((PortModeInformationRequest) job).getPortId(),
                        (byte) ((PortModeInformationRequest) job).getMode(),
                        (byte) ((PortModeInformationRequest) job).getInformationType()
                });
                characteristic.setWriteType(1);
                check(gatt.writeCharacteristic(characteristic));
                System.out.println("issued port mode information request");
                state = 6;
                return;
            }

            if (job instanceof Disconnect) {
                gatt.disconnect();
                System.out.println("disconnect");
                state = 7;
                return;
            }
        }

        System.out.println("default write handler");
    }

    private void check(boolean result) {
        if (!result)
            throw new RuntimeException("error invoking");
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        System.out.println("onDescriptorWrite status=" + status + " value=" + bytesToHex(descriptor.getValue()));

        handle(gatt);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        byte[] data = characteristic.getValue();

        HubResponse response = HubResponse.decodeMessage(data);

        System.out.println("characteristic changed: " + characteristic.getUuid() + " value=" + (data == null ? "null" : bytesToHex(data)) + " decoded=" + response.getText());

        if (response instanceof PortConnectedResponse) {
            connectivityManager.setAttached(index, ((PortConnectedResponse)response).getPort(), ((PortConnectedResponse)response).getIoType());
        }
        if (response instanceof PortDisconnectedResponse) {
            connectivityManager.setAttached(index, ((PortDisconnectedResponse)response).getPort(), -1);
        }

        if (state == 6)
            state = 5;
        handle(gatt);
    }


    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        System.out.println("read characteristic " + characteristic.getUuid() + " value=" + (characteristic.getValue() == null ? "null" : bytesToHex(characteristic.getValue())));
        if (status == GATT_SUCCESS) {
            handle(gatt);
        }
    }

    @Override
    public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {


        for (BluetoothGattService service : gatt.getServices()) {
            System.out.println("service discovered: " + service.getUuid());

            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                System.out.println("  characteristic:" + characteristic.getUuid());
            }
        }

        BluetoothGattService service = gatt.getService(SERVICE_REMOTE_CONTROL);
        if (service != null) {
            for (BluetoothGattDescriptor descriptor : service.getCharacteristic(CHARACTERISTIC_REMOTE_CONTROL_COMMANDS).getDescriptors()) {
                System.out.println("descr = " + descriptor.getUuid() + " perm" + descriptor.getPermissions() + " " + bytesToHex(descriptor.getValue()));
            }
        }

        handle(gatt);

    }

    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        System.out.println("onReliab");
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        System.out.println("onCharWrite status=" + status + " value=" + bytesToHex(characteristic.getValue()));
        if (status == GATT_SUCCESS)
            handle(gatt);
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        System.out.println("onDescRead " + status);
    }
}
