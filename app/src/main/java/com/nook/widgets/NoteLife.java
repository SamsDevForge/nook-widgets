package com.nook.widgets;

import android.app.*;
import android.content.*;
import android.os.SystemClock;
import android.provider.Settings;
import java.time.*;
import java.util.*;

/** Quiet note expiry. Expired and archived content is kept for recovery. */
public final class NoteLife {
    private NoteLife(){}
    public static final long HOUR=3600000L;
    public static int boot(Context c){return Settings.Global.getInt(c.getContentResolver(),Settings.Global.BOOT_COUNT,-1);}
    public static long remaining(Context c,Store.Item i){
        if(i.expiresAt==0)return Long.MAX_VALUE;
        if(i.expiryElapsed>0&&i.expiryBoot>=0&&i.expiryBoot==boot(c))return i.expiryElapsed-SystemClock.elapsedRealtime();
        return i.expiresAt-System.currentTimeMillis();
    }
    public static boolean active(Context c,Store.Item i){return !i.archived&&!i.expired&&(i.expiresAt==0||remaining(c,i)>0);}
    public static List<Store.Item> activeNotes(Context c){List<Store.Item> items=new ArrayList<>();for(Store.Item i:Store.list(c,"notes"))if(active(c,i))items.add(i);return items;}
    public static Store.Item selected(Context c,org.json.JSONObject config){return selected(c,config,activeNotes(c));}
    static Store.Item selected(Context c,org.json.JSONObject config,List<Store.Item> active){
        String entity=config.optString("entity","");Store.Item note=entity.isEmpty()?(active.isEmpty()?null:active.get(0)):Store.find(c,"notes",entity);
        if(!entity.equals(Widgets.NEW_NOTE)&&(note==null||!active(c,note))&&config.optInt("fallback",0)==1)note=active.isEmpty()?null:active.get(0);
        return note;
    }
    public static void permanent(Store.Item i){i.expiresAt=0;i.expiryElapsed=0;i.expiryBoot=-1;i.expired=false;i.archived=false;}
    public static void timer(Context c,Store.Item i,long milliseconds){
        long duration=Math.max(1,Math.min(365L*24*HOUR,milliseconds));
        i.expiresAt=System.currentTimeMillis()+duration;i.expiryElapsed=SystemClock.elapsedRealtime()+duration;i.expiryBoot=boot(c);i.archived=false;i.expired=false;
    }
    public static void deadline(Store.Item i,long timestamp){i.expiresAt=timestamp;i.expiryElapsed=0;i.expiryBoot=-1;i.archived=false;i.expired=false;}
    public static long endToday(){return LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();}
    public static long displayDeadline(Context c,Store.Item i){return i.expiryElapsed>0&&i.expiryBoot==boot(c)?System.currentTimeMillis()+remaining(c,i):i.expiresAt;}
    public static String status(Context c,Store.Item i){
        if(i.archived)return "Archived · Saved for later";
        if(!active(c,i))return "Expired · Saved for later";
        return i.expiresAt==0?"Permanent · Until you remove it":"Temporary · Until "+Widgets.when(displayDeadline(c,i));
    }
    public static void extend(Context c,String id){Store.Item i=Store.find(c,"notes",id);if(i==null)return;long left=active(c,i)&&i.expiresAt>0?Math.max(0,remaining(c,i)):0;timer(c,i,left+HOUR);Store.save(c,"notes",i);}
    public static void keep(Context c,String id){Store.Item i=Store.find(c,"notes",id);if(i!=null){permanent(i);Store.save(c,"notes",i);}}
    public static void archive(Context c,String id){Store.Item i=Store.find(c,"notes",id);if(i!=null){i.archived=true;Store.save(c,"notes",i);}}
    private static PendingIntent alarm(Context c){return PendingIntent.getBroadcast(c,0,new Intent(c,NoteExpiryReceiver.class).setAction("com.nook.widgets.EXPIRE_NOTES"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    public static void refresh(Context c){
        synchronized(Store.class){
            List<Store.Item> items=Store.list(c,"notes");boolean changed=false;long nearest=Long.MAX_VALUE;
            long now=System.currentTimeMillis();int currentBoot=boot(c);
            for(Store.Item i:items){
                if(i.archived||i.expired||i.expiresAt==0)continue;
                long left=remaining(c,i);
                if(left<=0){i.expired=true;changed=true;continue;}
                if(i.expiryElapsed>0&&currentBoot>=0&&i.expiryBoot!=currentBoot){i.expiryElapsed=SystemClock.elapsedRealtime()+left;i.expiryBoot=currentBoot;changed=true;}
                // Timers use elapsed time within a boot. Keep their wall-clock
                // fallback current for recovery if the phone restarts.
                if(i.expiryElapsed>0&&i.expiryBoot==currentBoot&&Math.abs(i.expiresAt-(now+left))>2000){i.expiresAt=now+left;changed=true;}
                nearest=Math.min(nearest,left);
            }
            if(changed)Store.write(c,"notes",items);
            AlarmManager manager=c.getSystemService(AlarmManager.class);PendingIntent pending=alarm(c);manager.cancel(pending);
            if(nearest==Long.MAX_VALUE)return;
            long trigger=SystemClock.elapsedRealtime()+Math.max(1,nearest);
            try{if(Reminders.precise(c))manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,trigger,pending);else manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,trigger,pending);}
            catch(SecurityException e){manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,trigger,pending);}
        }
    }
}
