package com.nook.widgets;

import android.content.*;
import android.media.session.*;
import android.app.NotificationManager;
import android.os.SystemClock;
import org.json.JSONObject;
import java.util.*;

public final class MediaAccess {
    private MediaAccess(){}
    public static ComponentName listener(Context c){return new ComponentName(c,MediaListener.class);}
    public static boolean allowed(Context c){return c.getSystemService(NotificationManager.class).isNotificationListenerAccessGranted(listener(c));}
    public static List<MediaController> sessions(Context c){
        if(!allowed(c))return new ArrayList<>();
        try{List<MediaController> list=new ArrayList<>(c.getSystemService(MediaSessionManager.class).getActiveSessions(listener(c)));list.removeIf(player->player.getPlaybackState()==null&&player.getMetadata()==null);list.sort(Comparator.comparingLong(MediaAccess::score).reversed());return list;}catch(SecurityException|IllegalStateException e){return new ArrayList<>();}
    }
    private static long score(MediaController player){PlaybackState state=player.getPlaybackState();if(state==null)return 0;int rank=switch(state.getState()){case PlaybackState.STATE_PLAYING->5;case PlaybackState.STATE_BUFFERING,PlaybackState.STATE_CONNECTING->4;case PlaybackState.STATE_PAUSED->3;case PlaybackState.STATE_STOPPED->2;default->1;};return rank*1000000000000L+Math.max(0,state.getLastPositionUpdateTime());}
    public static MediaController controller(Context c){return controller(c,new JSONObject());}
    public static MediaController controller(Context c,JSONObject config){String wanted=config.optString("musicPackage","");for(MediaController player:sessions(c))if(wanted.isEmpty()||wanted.equals(player.getPackageName()))return player;return null;}
    public static MediaController find(Context c,MediaSession.Token token){if(token==null)return null;for(MediaController player:sessions(c))if(token.equals(player.getSessionToken()))return player;return null;}
    public static boolean ongoing(PlaybackState state){if(state==null)return false;return switch(state.getState()){case PlaybackState.STATE_PLAYING,PlaybackState.STATE_BUFFERING,PlaybackState.STATE_CONNECTING,PlaybackState.STATE_FAST_FORWARDING,PlaybackState.STATE_REWINDING->true;default->false;};}
    public static boolean supports(PlaybackState state,long action){return state!=null&&(state.getActions()&action)!=0;}
    public static long position(PlaybackState state,long duration){if(state==null||state.getPosition()<0)return 0;double position=state.getPosition();if(state.getState()==PlaybackState.STATE_PLAYING&&state.getLastPositionUpdateTime()>0)position+=Math.max(0,SystemClock.elapsedRealtime()-state.getLastPositionUpdateTime())*state.getPlaybackSpeed();return Math.max(0,Math.min(duration>0?duration:Long.MAX_VALUE,(long)position));}
    public static boolean enabled(PlaybackState state,String operation){if(operation==null)return false;return switch(operation){case "previous"->supports(state,PlaybackState.ACTION_SKIP_TO_PREVIOUS);case "next"->supports(state,PlaybackState.ACTION_SKIP_TO_NEXT);case "back","forward","seek"->supports(state,PlaybackState.ACTION_SEEK_TO);case "queue"->supports(state,PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM);case "toggle"->supports(state,PlaybackState.ACTION_PLAY_PAUSE|(ongoing(state)?PlaybackState.ACTION_PAUSE:PlaybackState.ACTION_PLAY));default->false;};}
    public static boolean transport(MediaController player,String action,long value){
        if(player==null)return false;PlaybackState state=player.getPlaybackState();if(!enabled(state,action))return false;
        try{MediaController.TransportControls controls=player.getTransportControls();switch(action){case "previous"->controls.skipToPrevious();case "next"->controls.skipToNext();case "toggle"->{if(ongoing(state))controls.pause();else controls.play();}case "queue"->controls.skipToQueueItem(value);case "seek"->controls.seekTo(Math.max(0,value));case "back","forward"->{long duration=player.getMetadata()==null?0:player.getMetadata().getLong(android.media.MediaMetadata.METADATA_KEY_DURATION);long target=position(state,duration)+(action.equals("back")?-15000:30000);controls.seekTo(Math.max(0,duration>0?Math.min(duration,target):target));}default->{return false;}}return true;}catch(IllegalStateException|SecurityException e){return false;}
    }
    public static void transport(Context c,String action){transport(controller(c),action,0);}
    public static String appName(Context c,String name){try{return c.getPackageManager().getApplicationLabel(c.getPackageManager().getApplicationInfo(name,0)).toString();}catch(Exception e){return name;}}
}
