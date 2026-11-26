package com.nook.widgets;

import android.app.*;
import android.app.usage.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import java.time.*;
import java.util.*;
import org.json.JSONObject;

public final class ExtraData {
    private ExtraData(){}
    public static void put(JSONObject json,String key,Object value){try{json.put(key,value);}catch(Exception e){throw new IllegalStateException(e);}}
    public static boolean usageAllowed(Context c){return c.getSystemService(AppOpsManager.class).checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),c.getPackageName())==AppOpsManager.MODE_ALLOWED;}
    public static final class Usage {public long total;public final List<Map.Entry<String,Long>> apps=new ArrayList<>();}
    public static Usage usage(Context c){Usage result=new Usage();if(!usageAllowed(c))return result;long start=LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();try{Map<String,UsageStats> values=c.getSystemService(UsageStatsManager.class).queryAndAggregateUsageStats(start,System.currentTimeMillis());for(Map.Entry<String,UsageStats> app:values.entrySet()){long time=app.getValue().getTotalTimeInForeground();if(time>0){result.total+=time;result.apps.add(new AbstractMap.SimpleEntry<>(appName(c,app.getKey()),time));}}result.apps.sort((a,b)->Long.compare(b.getValue(),a.getValue()));}catch(RuntimeException ignored){}return result;}
    public static String appName(Context c,String packageName){try{return c.getPackageManager().getApplicationLabel(c.getPackageManager().getApplicationInfo(packageName,0)).toString();}catch(Exception e){return packageName;}}
    public static String duration(long ms){long seconds=Math.max(0,ms/1000);return seconds>=3600?String.format(Locale.getDefault(),"%dh %02dm",seconds/3600,seconds/60%60):String.format(Locale.getDefault(),"%dm",seconds/60);}
    public static long boot(Context c){try{return Settings.Global.getInt(c.getContentResolver(),Settings.Global.BOOT_COUNT);}catch(Exception e){return -1;}}
    public static long stopwatch(Context c,JSONObject config){long saved=config.optLong("elapsed",0);return config.optBoolean("running",false)&&config.optLong("boot",-2)==boot(c)?saved+Math.max(0,SystemClock.elapsedRealtime()-config.optLong("started",SystemClock.elapsedRealtime())):saved;}
    public static String stopwatchText(long elapsed){long s=Math.max(0,elapsed/1000);return String.format(Locale.ROOT,"%02d:%02d:%02d",s/3600,s/60%60,s%60);}
    public static String fileName(Context c,String uri){if(uri.isEmpty())return "Choose a file";try(Cursor cursor=c.getContentResolver().query(Uri.parse(uri),new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())return cursor.getString(0);}catch(Exception ignored){}return "File unavailable";}
    public static int trackerValue(JSONObject config,int slot){if(config.optBoolean("daily",true)&&!config.optString("trackedDay",LocalDate.now().toString()).equals(LocalDate.now().toString()))return 0;return Math.max(0,config.optInt("amount"+slot,0));}
    public static String trackerName(JSONObject config){return config.optString("trackerName",switch(config.optString("trackerType","activity")){case "water"->"Water log";case "reading"->"Reading progress";case "sleep"->"Sleep record";case "steps"->"Steps log";case "medications"->"Medication check-in";default->"Activity rings";});}
    public static final String[] TIPS={"Resize a widget to give its content more room.","Keep one note permanent and another temporary.","Use a separate widget copy for each routine.","Choose a wallpaper match for the Liquid Glass optics.","Your Universal widget remembers each tool's options.","Expired notes stay in Saved until you delete them.","Connect only the optional data sources you use."};
}
