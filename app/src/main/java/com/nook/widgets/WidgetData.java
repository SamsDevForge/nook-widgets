package com.nook.widgets;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.*;
import android.net.Uri;
import android.os.BatteryManager;
import android.provider.CalendarContract;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Local device data. Calendar access is optional and read-only. */
public final class WidgetData {
    private WidgetData(){}
    public static final class Battery {
        public int percent=-1;public boolean charging;public String source="On battery";public float temperature=Float.NaN;
    }
    public static Battery battery(Context c){
        Battery b=new Battery();Intent i=c.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));if(i==null)return b;
        int level=i.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),scale=i.getIntExtra(BatteryManager.EXTRA_SCALE,100),status=i.getIntExtra(BatteryManager.EXTRA_STATUS,-1),plug=i.getIntExtra(BatteryManager.EXTRA_PLUGGED,0);
        if(level>=0&&scale>0)b.percent=WidgetDesign.clamp(Math.round(100f*level/scale),0,100);b.charging=status==BatteryManager.BATTERY_STATUS_CHARGING||status==BatteryManager.BATTERY_STATUS_FULL;
        b.source=status==BatteryManager.BATTERY_STATUS_FULL?"Fully charged":plug==BatteryManager.BATTERY_PLUGGED_USB?"USB charging":plug==BatteryManager.BATTERY_PLUGGED_AC?"Plugged in":plug==BatteryManager.BATTERY_PLUGGED_WIRELESS?"Wireless charging":"On battery";
        int temperature=i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,Integer.MIN_VALUE);if(temperature!=Integer.MIN_VALUE)b.temperature=temperature/10f;return b;
    }
    public static boolean calendarAllowed(Context c){return c.checkSelfPermission(Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED;}
    public static final class Event {public long id,begin,end;public String title="";public boolean allDay;}
    public static List<Event> events(Context c,long start,long end){
        List<Event> result=new ArrayList<>();if(!calendarAllowed(c))return result;
        Uri.Builder uri=CalendarContract.Instances.CONTENT_URI.buildUpon();ContentUris.appendId(uri,start-86400000);ContentUris.appendId(uri,end+86400000);
        String status=CalendarContract.Instances.STATUS,rsvp=CalendarContract.Instances.SELF_ATTENDEE_STATUS;
        String selection=CalendarContract.Instances.VISIBLE+"=1 AND ("+status+" IS NULL OR "+status+"!="+CalendarContract.Events.STATUS_CANCELED+") AND ("+rsvp+" IS NULL OR "+rsvp+"!="+CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED+")";
        try(Cursor cursor=c.getContentResolver().query(uri.build(),new String[]{CalendarContract.Instances.EVENT_ID,CalendarContract.Instances.TITLE,CalendarContract.Instances.BEGIN,CalendarContract.Instances.END,CalendarContract.Instances.ALL_DAY},selection,null,CalendarContract.Instances.BEGIN+" ASC")){
            while(cursor!=null&&cursor.moveToNext()&&result.size()<100){Event e=new Event();e.id=cursor.getLong(0);e.title=cursor.getString(1);if(e.title==null||e.title.trim().isEmpty())e.title="Untitled event";e.begin=cursor.getLong(2);e.end=cursor.getLong(3);e.allDay=cursor.getInt(4)==1;if(overlaps(e,start,end))result.add(e);}
        }catch(RuntimeException e){android.util.Log.w("Nook","Calendar provider unavailable",e);}return result;
    }
    public static LocalDate eventDay(Event event){return Instant.ofEpochMilli(event.begin).atZone(event.allDay?ZoneOffset.UTC:ZoneId.systemDefault()).toLocalDate();}
    public static boolean overlaps(Event event,long start,long end){if(!event.allDay)return event.end>start&&event.begin<end;LocalDate from=Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDate(),until=Instant.ofEpochMilli(end-1).atZone(ZoneId.systemDefault()).toLocalDate(),last=Instant.ofEpochMilli(event.end).atZone(ZoneOffset.UTC).toLocalDate();return last.isAfter(from)&&!eventDay(event).isAfter(until);}
    public static long daysUntil(long target){return ChronoUnit.DAYS.between(LocalDate.now(),Instant.ofEpochMilli(target).atZone(ZoneId.systemDefault()).toLocalDate());}
    public static LocalDate targetDay(org.json.JSONObject config){try{String day=config.optString("targetDay","");if(!day.isEmpty())return LocalDate.parse(day);}catch(DateTimeException ignored){}long legacy=config.optLong("targetDate",0);return legacy==0?null:Instant.ofEpochMilli(legacy).atZone(ZoneId.systemDefault()).toLocalDate();}
    private static final android.util.LruCache<String,Bitmap> photos=new android.util.LruCache<>(3*1024*1024){protected int sizeOf(String key,Bitmap bitmap){return bitmap.getAllocationByteCount();}};
    public static Bitmap photo(Context c,String uri){
        if(uri.isEmpty())return null;Bitmap cached=photos.get(uri);if(cached!=null)return cached;
        try{Bitmap bitmap=ImageDecoder.decodeBitmap(ImageDecoder.createSource(c.getContentResolver(),Uri.parse(uri)),(decoder,info,source)->{decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);decoder.setTargetSampleSize(Math.max(1,Math.max(info.getSize().getWidth(),info.getSize().getHeight())/800));});photos.put(uri,bitmap);return bitmap;}catch(Exception e){return null;}
    }
}
