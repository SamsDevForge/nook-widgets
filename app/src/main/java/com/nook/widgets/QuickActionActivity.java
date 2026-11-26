package com.nook.widgets;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.widget.Toast;

/** A widget must launch an Activity directly; background receiver trampolines are blocked. */
public class QuickActionActivity extends Activity {
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        String action = getIntent().getStringExtra("action");
        int value = getIntent().getIntExtra("value", 5);
        launch(this, action, value);
        finish();
    }
    public static void launch(Activity a, String action, int value) {
        Intent intent;
        if ("timer".equals(action)) {
            intent = new Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, value * 60)
                .putExtra(AlarmClock.EXTRA_MESSAGE, "Nook · " + value + " min").putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        } else if ("alarm".equals(action)) {
            intent = new Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR, value / 60)
                .putExtra(AlarmClock.EXTRA_MINUTES, value % 60).putExtra(AlarmClock.EXTRA_MESSAGE, "Nook alarm")
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        } else { intent = new Intent(AlarmClock.ACTION_SHOW_ALARMS); }
        try { a.startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(a, "No compatible Clock app found. Install or enable an Android Clock app.", Toast.LENGTH_LONG).show();
        }
    }
}
