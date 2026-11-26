package com.nook.widgets;

import android.content.*;
import android.media.session.*;
import android.app.NotificationManager;
import java.util.*;

public final class MediaAccess {
    private MediaAccess(){}
    public static ComponentName listener(Context c){return new ComponentName(c,MediaListener.class);}
    public static boolean allowed(Context c){return c.getSystemService(NotificationManager.class).isNotificationListenerAccessGranted(listener(c));}
    public static MediaController controller(Context c){
        if(!allowed(c))return null;try{List<MediaController> list=c.getSystemService(MediaSessionManager.class).getActiveSessions(listener(c));for(MediaController controller:list){PlaybackState state=controller.getPlaybackState();if(state!=null&&state.getState()==PlaybackState.STATE_PLAYING)return controller;}return list.isEmpty()?null:list.get(0);}catch(SecurityException e){return null;}
    }
    public static void transport(Context c,String action){
        MediaController controller=controller(c);if(controller==null)return;MediaController.TransportControls controls=controller.getTransportControls();
        if("previous".equals(action))controls.skipToPrevious();else if("next".equals(action))controls.skipToNext();else {PlaybackState state=controller.getPlaybackState();if(state!=null&&state.getState()==PlaybackState.STATE_PLAYING)controls.pause();else controls.play();}
    }
}
