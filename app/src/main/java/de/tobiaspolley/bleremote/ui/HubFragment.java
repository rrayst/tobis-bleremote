package de.tobiaspolley.bleremote.ui;


import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.function.Function;

import de.tobiaspolley.bleremote.MainActivity;
import de.tobiaspolley.bleremote.connectivity.BluetoothNotAvailableException;
import de.tobiaspolley.bleremote.connectivity.ConnectivityManager;
import de.tobiaspolley.bleremote.R;
import de.tobiaspolley.bleremote.connectivity.PortObserver;
import de.tobiaspolley.bleremote.connectivity.ScanStatusCallback;
import de.tobiaspolley.bleremote.db.Hub;
import de.tobiaspolley.bleremote.jobs.Disconnect;
import de.tobiaspolley.bleremote.jobs.ReadProperty;
import de.tobiaspolley.bleremote.jobs.TriggerAction;
import de.tobiaspolley.bleremote.responses.PortConnectedResponse;
import de.tobiaspolley.bleremote.structs.Action;
import de.tobiaspolley.bleremote.structs.Property;

import static de.tobiaspolley.bleremote.Util.runEnumChooser;
import static de.tobiaspolley.bleremote.responses.PortConnectedResponse.IOTYPE_CONTROLPLUS_MOTOR_L;
import static de.tobiaspolley.bleremote.responses.PortConnectedResponse.IOTYPE_CONTROLPLUS_MOTOR_XL;

public class HubFragment extends Fragment implements PortObserver {
    private static final String ARG_INDEX = "index";

    private Button toggleConnectButton;
    private ImageButton overflowImageButton;
    private TextView captionText;
    private TextView infoText;

    private Runnable connectivityChangeListener;
    private ConnectivityManager connectivityManager;

    private int index;

    public HubFragment() {
    }

    public static HubFragment newInstance(int index) {
        HubFragment fragment = new HubFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_INDEX, index);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_INDEX);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View inflate = inflater.inflate(R.layout.fragment_hub, container, false);
        toggleConnectButton = inflate.findViewById(R.id.button_toggleConnection);
        overflowImageButton = inflate.findViewById(R.id.imageButton_overflow);
        captionText = inflate.findViewById(R.id.text_hub_caption);
        infoText = inflate.findViewById(R.id.text_hub_info);

        toggleConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleConnection(view);
            }
        });
        overflowImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(getActivity(), view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.hub_overflow, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.menu_hub_action:
                                triggerAction(menuItem.getActionView());
                                return true;
                            case R.id.menu_hub_info:
                                getInfo(menuItem.getActionView());
                                return true;
                            case R.id.menu_hub_forget:
                                if (connectivityManager.isConnected(index))
                                    Toast.makeText(getActivity(), R.string.forget_hub_while_connected, Toast.LENGTH_LONG).show();
                                else
                                    connectivityManager.forgetHub(index);
                                return true;
                        }
                        return false;
                    }
                });
                popup.show();
            }
        });

        connectivityManager = ConnectivityManager.getInstance(getActivity().getApplicationContext());
        connectivityChangeListener = new Runnable() {
            @Override
            public void run() {
                updateConnectedColors();
            }
        };
        connectivityManager.addConnectivityChangeListener(connectivityChangeListener);
        connectivityManager.addPortObserver(this);
        updateConnectedColors();

        captionText.setText(String.format(getString(R.string.hub_caption_p), ""+index));
        Hub hub = connectivityManager.getHub(index);
        infoText.setText(hub.name + " " + hub.mac);

        return inflate;
    }

    @Override
    public void onDestroyView() {
        connectivityManager.addPortObserver(this);
        connectivityManager.removeConnectivityChangeListener(connectivityChangeListener);

        super.onDestroyView();
    }

    private void updateConnectedColors() {
        toggleConnectButton.getBackground().setColorFilter(getResources().getColor(
                connectivityManager.isConnected(index) ?
                        R.color.connected :
                        R.color.disconnected, getActivity().getTheme()), PorterDuff.Mode.MULTIPLY);
    }

    public void toggleConnection(View view) {
        if (connectivityManager.isConnected(index)) {
            disconnect(index);
        } else {
            try {
                connectivityManager.connect(index, new ScanStatusCallback() {
                    @Override
                    public void onScanStart() {
                        toggleConnectButton.getBackground().setColorFilter(getResources().getColor(R.color.scanning, getActivity().getTheme()), PorterDuff.Mode.MULTIPLY);
                    }

                    @Override
                    public void onScanEnd(boolean success) {
                        updateConnectedColors();
                    }
                });
            } catch (BluetoothNotAvailableException e) {
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), MainActivity.REQUEST_ENABLE_BT);
            }
        }
    }

    public void disconnect(int index) {
        connectivityManager.enqueue(index, new Disconnect());
    }

    public void getInfo(View view) {
        runEnumChooser(getActivity(), Property.class, "Choose Property", "Retrieve", new Function<Property, Void>() {
            @Override
            public Void apply(Property i) {
                connectivityManager.enqueue(index, new ReadProperty(i.getProperty()));
                return null;
            }
        });
    }

    public void triggerAction(View view) {
        runEnumChooser(getActivity(), Action.class, "Choose Property", "Retrieve", new Function<Action, Void>() {
            @Override
            public Void apply(Action i) {
                connectivityManager.enqueue(index, new TriggerAction(i));
                return null;
            }
        });
    }

    public void halt(View view) {
        for (Fragment fragment : getChildFragmentManager().getFragments()) {
            if (fragment instanceof MotorFragment)
                ((MotorFragment)fragment).halt(view);
        }
    }

    @Override
    public void onPortConnected(int index, int port) {
        if (index != this.index)
            return;

        int ioType = connectivityManager.getPortIOType(index, port);
        if (ioType == IOTYPE_CONTROLPLUS_MOTOR_L || ioType == IOTYPE_CONTROLPLUS_MOTOR_XL) {
            Fragment fragment = getChildFragmentManager().findFragmentByTag("port" + port);
            if (fragment == null)
                getChildFragmentManager().beginTransaction().add(R.id.list_ports,
                        MotorFragment.newInstance(index, port), "port" + port).commit();
        }

    }

    @Override
    public void onPortDisconnected(int index, int port) {
        Fragment fragment = getChildFragmentManager().findFragmentByTag("port" + port);
        if (fragment != null)
            getChildFragmentManager().beginTransaction().remove(fragment).commit();
    }
}
