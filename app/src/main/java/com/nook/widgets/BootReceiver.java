package com.nook.widgets;

import android.content.*;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        Reminders.restore(c);
        NoteLife.refresh(c);
        Widgets.updateAll(c);
    }
}
