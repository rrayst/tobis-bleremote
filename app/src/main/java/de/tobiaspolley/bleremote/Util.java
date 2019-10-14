package de.tobiaspolley.bleremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.widget.EditText;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class Util {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null)
            return "null";

        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static void runInputDialog(Activity activity, String title, final Function<String, Void> onInput) {
        AlertDialog.Builder adb = new AlertDialog.Builder(activity);

        adb.setTitle(title);
        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        adb.setView(input);

        adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onInput.apply(input.getText().toString());
            }
        });
        adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        adb.show();
    }

    public static <T extends Enum> void runEnumChooser(Activity activity, final Class<T> clazz, String title, String actionText, final Function<T, Void> onSelect) {
        AlertDialog.Builder adb = new AlertDialog.Builder(activity);
        CharSequence items[] = new CharSequence[clazz.getEnumConstants().length];
        for (int i = 0; i < items.length; i++)
            items[i] = clazz.getEnumConstants()[i].name();
        final AtomicInteger selected = new AtomicInteger(0);
        adb.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                selected.set(i);
            }
        });
        adb.setNegativeButton("Cancel", null);
        adb.setPositiveButton(actionText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                onSelect.apply(clazz.getEnumConstants()[selected.get()]);
            }
        });
        adb.setTitle(title);
        adb.show();
    }


}
