package com.nook.widgets;

import android.content.*;

/** System Bluetooth stack only; the manifest requires a signature-level sender permission. */
public final class BudsReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){BudsData.event(c,i);}
}
