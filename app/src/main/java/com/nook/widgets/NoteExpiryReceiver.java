package com.nook.widgets;
import android.content.*;

public class NoteExpiryReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent){NoteLife.refresh(c);Widgets.updateAll(c);}
}
