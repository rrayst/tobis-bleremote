package de.tobiaspolley.bleremote;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.room.Room;

import de.tobiaspolley.bleremote.connectivity.BluetoothNotAvailableException;
import de.tobiaspolley.bleremote.connectivity.ConnectivityManager;
import de.tobiaspolley.bleremote.connectivity.HubObserver;
import de.tobiaspolley.bleremote.connectivity.ScanStatusCallback;
import de.tobiaspolley.bleremote.db.AppDatabase;
import de.tobiaspolley.bleremote.db.Option;
import de.tobiaspolley.bleremote.ui.HubFragment;

public class MainActivity extends AppCompatActivity implements HubObserver {

    public static final int REQUEST_ENABLE_BT = 0;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private ConnectivityManager connectivityManager;
    private ImageButton overflowImageButton;
    private View mainView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainView = findViewById(R.id.main);

        overflowImageButton = findViewById(R.id.imageButton_overflow);
        overflowImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(MainActivity.this, view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.main_overflow, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.menu_scan:
                                addNewHub(menuItem.getActionView());
                                return true;
                            case R.id.menu_about:
                                final TextView message = new TextView(MainActivity.this);
                                int p = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
                                message.setPadding(p, p, p, p);
                                final SpannableString s =
                                        new SpannableString(getText(R.string.about_text));
                                Linkify.addLinks(s, Linkify.WEB_URLS);
                                message.setText(s);
                                message.setMovementMethod(LinkMovementMethod.getInstance());

                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle(getString(R.string.about_title))
                                        .setView(message)
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                new WarningAcknowledger().execute();
                                                checkPermissions();
                                            }
                                        })
                                        .setIcon(android.R.drawable.ic_dialog_info)
                                        .show();
                                return true;
                        }
                        return false;
                    }
                });
                popup.show();
            }
        });


        connectivityManager = ConnectivityManager.getInstance(getApplicationContext());

        for (Integer index : connectivityManager.getIndexes()) {
            onHubAdded(index);
        }
        connectivityManager.addHubObserver(this);

        new WarningChecker().execute();
    }

    @Override
    protected void onDestroy() {
        connectivityManager.removeHubObserver(this);
        super.onDestroy();
    }

    public void halt(View view) {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof HubFragment)
                ((HubFragment)fragment).halt(view);
        }
    }

    public void addNewHub(View view) {
        try {
            connectivityManager.scanForNewHub(new ScanStatusCallback() {
                @Override
                public void onScanStart() {
                    mainView.setBackgroundColor(getResources().getColor(R.color.scanning, getTheme()));
                }

                @Override
                public void onScanEnd(boolean success) {
                    mainView.setBackground(null);
                }
            });
        } catch (BluetoothNotAvailableException e) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onHubAdded(int index) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("hub" + index);
        if (fragment == null)
            getSupportFragmentManager().beginTransaction().add(R.id.list_hubs, HubFragment.newInstance(index), "hub" + index).commit();
        updateScanButtonVisibility();
    }

    @Override
    public void onHubRemoved(int index) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("hub" + index);

        if (fragment != null)
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();

        updateScanButtonVisibility();
    }

    private void updateScanButtonVisibility() {
        findViewById(R.id.button_scan).setVisibility(((LinearLayout)findViewById(R.id.list_hubs)).getChildCount() == 0 ? View.GONE : View.VISIBLE);
    }

    private class WarningChecker extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            AppDatabase db = Room.databaseBuilder(MainActivity.this, AppDatabase.class, "hubs").build();
            Option option = db.optionDao().getOption("warning");
            return option == null;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(getString(R.string.warning_title))
                        .setMessage(getString(R.string.warning_text))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                new WarningAcknowledger().execute();
                                checkPermissions();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            } else {
                checkPermissions();
            }
        }
    }

    private class WarningAcknowledger extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            AppDatabase db = Room.databaseBuilder(MainActivity.this, AppDatabase.class, "hubs").build();
            Option option = new Option();
            option.name = "warning";
            option.value = "" + System.currentTimeMillis();
            db.optionDao().insertAll(option);
            return null;
        }
    }

    private void checkPermissions() {
        if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, PERMISSION_REQUEST_COARSE_LOCATION);
        }
    }
}
