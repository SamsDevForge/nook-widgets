package com.nook.widgets;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.media.*;
import android.net.Uri;
import android.os.Bundle;
import android.util.Rational;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;

/** Full video playback is an activity; PiP is the system's floating video surface. */
public final class VideoPlayerActivity extends Activity {
    private VideoView video;private MediaPlayer player;private MediaController controls;private LinearLayout top,bottom;private TextView sound,status;private boolean muted,repeat,prepared,requestFloat;private int widgetId,position;private JSONObject settings;private boolean changingToPip;
    @Override public void onCreate(Bundle saved){super.onCreate(saved);widgetId=getIntent().getIntExtra("widget",-1);AppWidgetProviderInfo info=AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId);if(info==null){finish();return;}String provider=Widgets.kindOf(info.provider);JSONObject root=Store.config(this,widgetId);String kind=provider.equals("universal")?UniversalWidget.currentPage(root):provider;if(!kind.equals("video")){finish();return;}settings=MediaData.active(provider.equals("universal")?UniversalWidget.pageSettings(root,"video"):root);String uri=settings.optString("videoUri");if(!MediaData.document(uri)){finish();return;}muted=saved==null?settings.optBoolean("videoMuted",true):saved.getBoolean("muted",true);repeat=settings.optBoolean("videoRepeat",true);position=saved==null?0:saved.getInt("position",0);requestFloat=getIntent().getBooleanExtra("floating",false);
        LinearLayout layout=Ui.column(this);layout.setBackgroundColor(0xff080b10);setContentView(layout);Ui.insets(this,layout);top=Ui.row(this);Ui.pad(top,16,8);top.addView(Ui.iconButton(this,"back","Close video",this::finish));Ui.weight(top,Ui.bold(this,settings.optString("videoName","Your video"),17,Ui.TEXT));layout.addView(top);
        FrameLayout stage=new FrameLayout(this);layout.addView(stage,new LinearLayout.LayoutParams(-1,0,1));video=new VideoView(this);video.setId(R.id.video_player);stage.addView(video,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));controls=new MediaController(this);controls.setAnchorView(video);video.setMediaController(controls);
        bottom=Ui.column(this);Ui.pad(bottom,20,14);status=Ui.text(this,"Loading your video…",13,Ui.MUTED);bottom.addView(status);LinearLayout buttons=Ui.row(this);sound=Ui.button(this,muted?"Unmute":"Mute",Ui.SURFACE,Ui.ACCENT,()->{muted=!muted;if(player!=null)player.setVolume(muted?0:1,muted?0:1);sound.setText(muted?"Unmute":"Mute");status.setText("Local video · "+(muted?"Muted":"Sound on"));});Ui.weight(buttons,sound);Ui.weight(buttons,Ui.button(this,"Float over home",Ui.SURFACE,Ui.ACCENT,this::floatVideo));bottom.addView(buttons);layout.addView(bottom);
        video.setOnPreparedListener(media->{player=media;prepared=true;player.setLooping(repeat);player.setVolume(muted?0:1,muted?0:1);video.seekTo(position);video.start();status.setText("Local video · "+(muted?"Muted":"Sound on"));if(requestFloat){requestFloat=false;video.post(this::floatVideo);}});video.setOnCompletionListener(media->{if(!repeat)status.setText("Finished · use Play to watch again");});video.setOnErrorListener((media,what,extra)->{prepared=false;player=null;status.setText("This video is unavailable or unsupported. Choose it again in widget options.");return true;});try{video.setVideoURI(Uri.parse(uri));}catch(RuntimeException e){status.setText("Video access has expired. Choose the file again in widget options.");}
    }
    public void floatVideo(){
        if(!prepared){Toast.makeText(this,"Wait for the video to load.",Toast.LENGTH_SHORT).show();return;}if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)){Toast.makeText(this,"Floating video is unavailable on this device.",Toast.LENGTH_LONG).show();return;}
        int width=Math.max(1,player.getVideoWidth()),height=Math.max(1,player.getVideoHeight());float ratio=Math.max(1/2.39f,Math.min(2.39f,width/(float)height));Rect bounds=new Rect();video.getGlobalVisibleRect(bounds);PictureInPictureParams params=new PictureInPictureParams.Builder().setAspectRatio(new Rational(Math.round(ratio*1000),1000)).setSourceRectHint(bounds).build();
        try{changingToPip=true;controls.hide();if(!enterPictureInPictureMode(params)){changingToPip=false;Toast.makeText(this,"Allow picture-in-picture for Nook in Android settings.",Toast.LENGTH_LONG).show();}else video.start();}catch(IllegalStateException|IllegalArgumentException e){changingToPip=false;Toast.makeText(this,"Picture-in-picture could not open. Check Nook's PiP permission.",Toast.LENGTH_LONG).show();}
    }
    @Override public void onPictureInPictureModeChanged(boolean inPip,Configuration config){super.onPictureInPictureModeChanged(inPip,config);changingToPip=false;top.setVisibility(inPip?View.GONE:View.VISIBLE);bottom.setVisibility(inPip?View.GONE:View.VISIBLE);video.setMediaController(inPip?null:controls);}
    @Override protected void onPause(){super.onPause();if(video!=null&&!isInPictureInPictureMode()&&!changingToPip)video.pause();}
    @Override protected void onStop(){if(video!=null&&!isInPictureInPictureMode())video.pause();super.onStop();}
    @Override public void onSaveInstanceState(Bundle state){if(video!=null)state.putInt("position",video.getCurrentPosition());state.putBoolean("muted",muted);super.onSaveInstanceState(state);}
    @Override protected void onDestroy(){if(video!=null)video.stopPlayback();player=null;super.onDestroy();}
}
