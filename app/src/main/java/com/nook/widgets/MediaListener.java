package com.nook.widgets;

import android.media.MediaMetadata;
import android.media.session.*;
import android.service.notification.NotificationListenerService;
import java.util.*;
import android.os.*;
import java.util.concurrent.*;

/** Notification-listener access is used only to discover active media sessions. */
public final class MediaListener extends NotificationListenerService {
    private final Map<MediaController,MediaController.Callback> controllers=new LinkedHashMap<>();
    private static final ExecutorService REFRESH=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean queued;private long lastRefresh;
    private final Runnable refresh=()->{queued=false;lastRefresh=SystemClock.elapsedRealtime();REFRESH.execute(()->MusicWidgets.updateAll(getApplicationContext()));};
    private void refresh(){refresh(true);}
    private void refresh(boolean urgent){if(queued||!urgent&&SystemClock.elapsedRealtime()-lastRefresh<15000)return;queued=true;handler.postDelayed(refresh,150);}
    private final MediaSessionManager.OnActiveSessionsChangedListener listener=list->{attach(list);refresh();};
    private static String key(PlaybackState state){return state==null?"":state.getState()+"|"+state.getActions()+"|"+state.getPlaybackSpeed()+"|"+state.getActiveQueueItemId()+"|"+state.getCustomActions().stream().map(action->action.getAction()+action.getName()).collect(java.util.stream.Collectors.joining("|"));}
    private void attach(List<MediaController> list){
        for(Map.Entry<MediaController,MediaController.Callback> entry:controllers.entrySet())entry.getKey().unregisterCallback(entry.getValue());controllers.clear();if(list==null)return;
        for(MediaController player:list){MediaController.Callback callback=new MediaController.Callback(){
            private PlaybackState previous=player.getPlaybackState();
            public void onMetadataChanged(MediaMetadata metadata){refresh();}
            public void onPlaybackStateChanged(PlaybackState state){boolean changed=!key(state).equals(key(previous))||state!=null&&Math.abs(state.getPosition()-MediaAccess.position(previous,0))>2000;previous=state;refresh(changed);}
            public void onQueueChanged(List<MediaSession.QueueItem> queue){refresh();}
            public void onAudioInfoChanged(MediaController.PlaybackInfo info){refresh();}
            public void onSessionDestroyed(){refresh();}
        };controllers.put(player,callback);player.registerCallback(callback,handler);}
    }
    @Override public void onListenerConnected(){super.onListenerConnected();try{MediaSessionManager manager=getSystemService(MediaSessionManager.class);manager.removeOnActiveSessionsChangedListener(listener);manager.addOnActiveSessionsChangedListener(listener,MediaAccess.listener(this),handler);attach(manager.getActiveSessions(MediaAccess.listener(this)));refresh();}catch(SecurityException ignored){}}
    private void detach(){try{getSystemService(MediaSessionManager.class).removeOnActiveSessionsChangedListener(listener);}catch(SecurityException ignored){}attach(null);handler.removeCallbacks(refresh);queued=false;}
    @Override public void onListenerDisconnected(){detach();REFRESH.execute(()->MusicWidgets.updateAll(getApplicationContext()));super.onListenerDisconnected();}
    @Override public void onDestroy(){detach();super.onDestroy();}
}
