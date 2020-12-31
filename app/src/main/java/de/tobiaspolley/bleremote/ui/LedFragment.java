package de.tobiaspolley.bleremote.ui;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.function.Function;

import de.tobiaspolley.bleremote.R;
import de.tobiaspolley.bleremote.connectivity.ConnectivityManager;
import de.tobiaspolley.bleremote.jobs.GotoAbsolutePositionCommand;
import de.tobiaspolley.bleremote.jobs.LedStatusCommand;
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

public class LedFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {
    private static final String ARG_INDEX = "index";
    private static final String ARG_PORT = "port";

    private int index;
    private int port;

    private CheckBox radioButton;
    private TextView captionText, infoText;
    private ConnectivityManager connectivityManager;

    public LedFragment() {
    }

    public static LedFragment newInstance(int index, int port) {
        LedFragment fragment = new LedFragment();
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
        View inflate = inflater.inflate(R.layout.fragment_port_led, container, false);

        radioButton = inflate.findViewById(R.id.radio);
        radioButton.setOnCheckedChangeListener(this);

        captionText = inflate.findViewById(R.id.text_led_caption);
        infoText = inflate.findViewById(R.id.text_led_info);
        captionText.setText(String.format(getString(R.string.motor_caption_p), ""+(char)('A' + port), "LED"));

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
        radioButton.setChecked(false);
        onCheckedChanged(null, false);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        connectivityManager.enqueue(index, new LedStatusCommand(port, isChecked));
    }
}
