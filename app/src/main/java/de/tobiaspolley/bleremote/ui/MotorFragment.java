package de.tobiaspolley.bleremote.ui;


import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.function.Function;

import de.tobiaspolley.bleremote.connectivity.ConnectivityManager;
import de.tobiaspolley.bleremote.R;
import de.tobiaspolley.bleremote.jobs.PortInformationRequest;
import de.tobiaspolley.bleremote.jobs.PortModeInformationRequest;
import de.tobiaspolley.bleremote.jobs.StartPowerCommand;
import de.tobiaspolley.bleremote.structs.InformationType;
import de.tobiaspolley.bleremote.structs.ModeInformationType;

import static de.tobiaspolley.bleremote.Util.runEnumChooser;
import static de.tobiaspolley.bleremote.Util.runInputDialog;
import static de.tobiaspolley.bleremote.responses.PortConnectedResponse.IOTYPE_CONTROLPLUS_MOTOR_L;
import static de.tobiaspolley.bleremote.responses.PortConnectedResponse.IOTYPE_CONTROLPLUS_MOTOR_SERVO;
import static de.tobiaspolley.bleremote.responses.PortConnectedResponse.IOTYPE_CONTROLPLUS_MOTOR_XL;

public class MotorFragment extends Fragment implements SeekBar.OnSeekBarChangeListener {
    public static final int STOP = 256;

    private static final String ARG_INDEX = "index";
    private static final String ARG_PORT = "port";

    private int index;
    private int port;

    private SeekBar seekBar;
    private TextView captionText;
    private ConnectivityManager connectivityManager;

    public MotorFragment() {
    }

    public static MotorFragment newInstance(int index, int port) {
        MotorFragment fragment = new MotorFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_INDEX, index);
        args.putInt(ARG_PORT, port);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_INDEX);
            port = getArguments().getInt(ARG_PORT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        connectivityManager = ConnectivityManager.getInstance(getActivity().getApplicationContext());

        // Inflate the layout for this fragment
        View inflate = inflater.inflate(R.layout.fragment_port_motor, container, false);

        seekBar = inflate.findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);

        captionText = inflate.findViewById(R.id.text_motor_caption);
        String description;
        switch (connectivityManager.getPortIOType(index, port)) {
            case IOTYPE_CONTROLPLUS_MOTOR_L:
                description = getString(R.string.motor_description_l);
                break;
            case IOTYPE_CONTROLPLUS_MOTOR_XL:
                description = getString(R.string.motor_description_xl);
                break;
            case IOTYPE_CONTROLPLUS_MOTOR_SERVO:
                description = getString(R.string.motor_description_servo);
                break;
            default:
                description = getString(R.string.motor_description_unknown);
                break;
        }
        captionText.setText(String.format(getString(R.string.motor_caption_p), ""+(char)('A' + port), description));

        ImageButton overflowImageButton = inflate.findViewById(R.id.imageButton_overflow);
        overflowImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(getActivity(), view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.motor_overflow, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.menu_motor_pir:
                                pir(menuItem.getActionView());
                                return true;
                            case R.id.menu_motor_pmir:
                                pmir(menuItem.getActionView());
                                return true;
                        }
                        return false;
                    }
                });

                popup.show();
            }
        });

        return inflate;
    }

    public void pir(View view) {
        runEnumChooser(getActivity(), InformationType.class, "Information Type", "Select", new Function<InformationType, Void>() {
            @Override
            public Void apply(InformationType informationType) {
                connectivityManager.enqueue(index, new PortInformationRequest(port, informationType.getValue()));
                return null;
            }
        });
    }

    public void pmir(View view) {
        runInputDialog(getActivity(), "Mode", new Function<String, Void>() {
            @Override
            public Void apply(final String mode) {
                runEnumChooser(getActivity(), ModeInformationType.class, "Information Type", "Select", new Function<ModeInformationType, Void>() {
                    @Override
                    public Void apply(ModeInformationType informationType) {
                        connectivityManager.enqueue(index, new PortModeInformationRequest(port, Integer.parseInt(mode), informationType.getValue()));
                        return null;
                    }
                });
                return null;
            }
        });
    }

    public void halt(View view) {
        seekBar.setProgress(STOP);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        connectivityManager.enqueue(index, new StartPowerCommand(port, progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

}
