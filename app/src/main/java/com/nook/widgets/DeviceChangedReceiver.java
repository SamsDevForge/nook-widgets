package com.nook.widgets;
import android.content.*;
public final class DeviceChangedReceiver extends BroadcastReceiver{public void onReceive(Context c,Intent i){Widgets.updateAll(c);}}
