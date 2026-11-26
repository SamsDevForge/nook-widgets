package com.nook.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.*;
import org.json.JSONObject;
import java.util.Locale;

/** A fresh, immutable view of the selected player's public MediaSession. */
public final class MusicState {
    public final MediaController player;
    public final PlaybackState playback;
    public final String title,artist,album,app,status;
    public final Bitmap art;
    public final long duration,position;
    public final boolean playing,connected;
    public MusicState(Context c,JSONObject config){this(c,MediaAccess.controller(c,config));}
    public MusicState(Context c,MediaController controller){
        player=controller;connected=controller!=null;playback=connected?controller.getPlaybackState():null;playing=MediaAccess.ongoing(playback);
        MediaMetadata metadata=connected?controller.getMetadata():null;
        title=metadata==null?connected?"Now playing":"Ready when you are":first(metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),metadata.getString(MediaMetadata.METADATA_KEY_TITLE),"Now playing");
        artist=metadata==null?"":first(metadata.getString(MediaMetadata.METADATA_KEY_ARTIST),metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE),"");
        album=metadata==null?"":first(metadata.getString(MediaMetadata.METADATA_KEY_ALBUM),"");app=connected?MediaAccess.appName(c,controller.getPackageName()):"";
        Bitmap image=metadata==null?null:metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);if(image==null&&metadata!=null)image=metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);if(image==null&&metadata!=null)image=metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);art=image;
        duration=metadata==null?0:Math.max(0,metadata.getLong(MediaMetadata.METADATA_KEY_DURATION));position=MediaAccess.position(playback,duration);
        status=playback==null?"No playback":switch(playback.getState()){case PlaybackState.STATE_PLAYING->"Playing";case PlaybackState.STATE_BUFFERING->"Buffering";case PlaybackState.STATE_CONNECTING->"Connecting";case PlaybackState.STATE_ERROR->"Player error";case PlaybackState.STATE_PAUSED->"Paused";case PlaybackState.STATE_STOPPED->"Stopped";default->"Ready";};
    }
    private static String first(String... values){for(String value:values)if(value!=null&&!value.trim().isEmpty())return value;return "";}
    public static String time(long milliseconds){long seconds=Math.max(0,milliseconds)/1000;return seconds>=3600?String.format(Locale.ROOT,"%d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60):String.format(Locale.ROOT,"%d:%02d",seconds/60,seconds%60);}
    public boolean can(String operation){return MediaAccess.enabled(playback,operation);}
}
