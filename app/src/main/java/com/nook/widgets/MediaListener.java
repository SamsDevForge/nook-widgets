package com.nook.widgets;

import android.media.MediaMetadata;
import android.media.session.*;
import android.service.notification.NotificationListenerService;
import java.util.*;

/** Notification-listener access is used only to discover active media sessions. */
public final class MediaListener extends NotificationListenerService {
    private final List<MediaController> controllers=new ArrayList<>();
    private final MediaController.Callback callback=new MediaController.Callback(){public void onMetadataChanged(MediaMetadata m){Widgets.updateAll(MediaListener.this);}public void onPlaybackStateChanged(PlaybackState p){Widgets.updateAll(MediaListener.this);}public void onSessionDestroyed(){Widgets.updateAll(MediaListener.this);}};
    private final MediaSessionManager.OnActiveSessionsChangedListener listener=list->{attach(list);Widgets.updateAll(this);};
    private void attach(List<MediaController> list){for(MediaController c:controllers)c.unregisterCallback(callback);controllers.clear();if(list!=null){controllers.addAll(list);for(MediaController c:controllers)c.registerCallback(callback);}}
    @Override public void onListenerConnected(){super.onListenerConnected();try{MediaSessionManager manager=getSystemService(MediaSessionManager.class);manager.addOnActiveSessionsChangedListener(listener,MediaAccess.listener(this));attach(manager.getActiveSessions(MediaAccess.listener(this)));Widgets.updateAll(this);}catch(SecurityException ignored){}}
    @Override public void onListenerDisconnected(){getSystemService(MediaSessionManager.class).removeOnActiveSessionsChangedListener(listener);attach(null);Widgets.updateAll(this);super.onListenerDisconnected();}
    @Override public void onDestroy(){getSystemService(MediaSessionManager.class).removeOnActiveSessionsChangedListener(listener);attach(null);super.onDestroy();}
}
