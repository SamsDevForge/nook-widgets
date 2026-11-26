package com.nook.widgets;

import android.content.*;
import android.os.SystemClock;
import org.json.*;
import java.util.UUID;

/** Per-widget transport state; appearance updates retain the current video position. */
public final class InlineVideoState {
    private InlineVideoState(){}
    private static JSONObject read(Context c,int id){try{return new JSONObject(c.getSharedPreferences("nook_inline_video",0).getString("widget_"+id,"{}"));}catch(Exception e){return new JSONObject();}}
    private static void save(Context c,int id,JSONObject state){c.getSharedPreferences("nook_inline_video",0).edit().putString("widget_"+id,state.toString()).commit();}
    public static int interval(JSONObject config){return new int[]{333,200,125}[WidgetDesign.clamp(config.optInt("videoPace",1),0,2)];}
    public static long duration(JSONObject config,JSONObject info){return config.optInt("variant",0)==2?Math.max(120,info.optLong("span",3000)):Math.max(120,info.optLong("duration",3000));}
    public static long begin(JSONObject config,JSONObject info){return config.optInt("variant",0)==2?info.optLong("begin",0):0;}
    public static long position(Context c,JSONObject state,long duration){long position=state.optLong("position",0);if(state.optBoolean("playing",true)&&state.optLong("boot",-2)==ExtraData.boot(c))position+=Math.max(0,SystemClock.elapsedRealtime()-state.optLong("epoch",SystemClock.elapsedRealtime()));return Math.floorMod(position,Math.max(1,duration));}
    public static synchronized JSONObject state(Context c,int id,JSONObject config,JSONObject info){String key=MediaData.key(config)+"|"+config.optInt("variant",0)+"|"+interval(config);JSONObject state=read(c,id);if(!key.equals(state.optString("key"))){state=new JSONObject();ExtraData.put(state,"key",key);ExtraData.put(state,"playing",!config.optBoolean("videoPaused",false));ExtraData.put(state,"position",0);ExtraData.put(state,"epoch",SystemClock.elapsedRealtime());ExtraData.put(state,"boot",ExtraData.boot(c));ExtraData.put(state,"session",UUID.randomUUID().toString());save(c,id,state);}else if(state.optLong("boot",-2)!=ExtraData.boot(c)){ExtraData.put(state,"playing",false);ExtraData.put(state,"epoch",SystemClock.elapsedRealtime());ExtraData.put(state,"boot",ExtraData.boot(c));ExtraData.put(state,"session",UUID.randomUUID().toString());save(c,id,state);}return state;}
    public static synchronized void toggle(Context c,int id,JSONObject config,JSONObject info){JSONObject state=state(c,id,config,info);ExtraData.put(state,"position",position(c,state,duration(config,info)));ExtraData.put(state,"playing",!state.optBoolean("playing",true));ExtraData.put(state,"epoch",SystemClock.elapsedRealtime());ExtraData.put(state,"session",UUID.randomUUID().toString());save(c,id,state);}
    public static synchronized void reset(Context c,int id){InlineVideoService.forget(id);c.getSharedPreferences("nook_inline_video",0).edit().remove("widget_"+id).commit();}
    public static boolean current(Context c,int id,String session){return session!=null&&session.equals(read(c,id).optString("session"));}
}
