package com.nook.widgets;

import android.appwidget.*;
import android.content.*;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.widget.*;
import org.json.JSONObject;
import java.util.concurrent.ConcurrentHashMap;

/** A lazy frame collection: the launcher requests only a bounded window of the full video. */
public final class InlineVideoService extends RemoteViewsService {
    private static final ConcurrentHashMap<Integer,Long> DECODED=new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer,Bitmap> LATEST=new ConcurrentHashMap<>();
    public static long lastDecodedTime(int id){return DECODED.getOrDefault(id,-1L);}
    public static void forget(int id){DECODED.remove(id);LATEST.remove(id);}
    public static Bitmap snapshot(int id){Bitmap frame=LATEST.get(id);return frame==null?null:frame.copy(Bitmap.Config.ARGB_8888,false);}
    @Override public RemoteViewsFactory onGetViewFactory(Intent intent){return new Frames(getApplicationContext(),intent);}
    public static final class Frames implements RemoteViewsFactory {
        private final Context c;private final int id;private final String session;private final JSONObject config;private final long offset;private MediaMetadataRetriever reader;private VideoFrameDecoder decoder;private JSONObject info=new JSONObject();private int count=1,interval;private long duration=120,begin;private boolean available;
        public Frames(Context c,Intent intent){this.c=c;id=intent.getIntExtra("widget",-1);session=intent.getStringExtra("session");offset=intent.getLongExtra("offset",0);JSONObject parsed;try{parsed=new JSONObject(intent.getStringExtra("config"));}catch(Exception e){parsed=new JSONObject();}config=parsed;interval=InlineVideoState.interval(config);}
        private boolean valid(){AppWidgetProviderInfo provider=AppWidgetManager.getInstance(c).getAppWidgetInfo(id);if(provider==null||!InlineVideoState.current(c,id,session))return false;String kind=Widgets.kindOf(provider.provider);return kind.equals("video")||kind.equals("universal")&&UniversalWidget.currentPage(Store.config(c,id)).equals("video");}
        @Override public synchronized void onCreate(){onDataSetChanged();}
        @Override public synchronized void onDataSetChanged(){release();available=false;count=1;if(!valid()||!MediaData.document(config.optString("videoUri")))return;try{reader=new MediaMetadataRetriever();reader.setDataSource(c,android.net.Uri.parse(config.optString("videoUri")));info=MediaData.dimensions(reader);long length=Long.parseLong(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));ExtraData.put(info,"duration",length);begin=config.optInt("variant",0)==2?Math.min(length-1,MediaData.start(config)*1000L):0;duration=config.optInt("variant",0)==2?Math.min(MediaData.length(config)*1000L,length-begin):length;duration=Math.max(1,duration);count=(int)Math.min(Integer.MAX_VALUE,Math.max(1,(duration+interval-1)/interval));try{decoder=new VideoFrameDecoder(c,config.optString("videoUri"));}catch(Exception ignored){}available=true;}catch(Exception e){android.util.Log.w("NookInline","Open failed: "+e.getClass().getSimpleName());release();}}
        @Override public synchronized RemoteViews getViewAt(int position){if(!available||!valid())return getLoadingView();long time=begin+Math.floorMod(offset+position*(long)interval,duration);try{Bitmap bitmap=decoder==null?MediaData.decode(reader,info,time,320):decoder.frame(time,320);if(bitmap==null&&decoder!=null)bitmap=MediaData.decode(reader,info,time,320);if(bitmap==null)return getLoadingView();RemoteViews frame=new RemoteViews(c.getPackageName(),config.optBoolean("videoCrop",false)?R.layout.video_frame_crop:R.layout.video_frame_fit);frame.setImageViewBitmap(R.id.video_frame_image,bitmap);frame.setContentDescription(R.id.video_frame_image,"Video frame at "+time+" milliseconds");frame.setOnClickFillInIntent(R.id.video_frame_image,new Intent());DECODED.put(id,time);LATEST.put(id,bitmap);return frame;}catch(Exception e){if(decoder!=null){decoder.close();decoder=null;return getViewAt(position);}return getLoadingView();}}
        @Override public synchronized RemoteViews getLoadingView(){RemoteViews view=new RemoteViews(c.getPackageName(),config.optBoolean("videoCrop",false)?R.layout.video_frame_crop:R.layout.video_frame_fit);JSONObject cache=MediaData.cached(c,config);Bitmap poster=MediaData.frame(c,config,cache,0);if(poster!=null)view.setImageViewBitmap(R.id.video_frame_image,poster);return view;}
        @Override public synchronized int getCount(){return count;}
        @Override public int getViewTypeCount(){return 1;}
        @Override public long getItemId(int position){return position;}
        @Override public boolean hasStableIds(){return true;}
        @Override public synchronized void onDestroy(){release();if(InlineVideoState.current(c,id,session)){DECODED.remove(id);LATEST.remove(id);}}
        private void release(){if(decoder!=null){decoder.close();decoder=null;}if(reader!=null){try{reader.release();}catch(Exception ignored){}reader=null;}}
    }
}
