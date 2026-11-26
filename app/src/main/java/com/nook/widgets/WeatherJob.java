package com.nook.widgets;

import android.app.job.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import org.json.JSONObject;
import java.util.*;

/** System-managed network work; long refresh batches never run in a receiver. */
public final class WeatherJob extends JobService {
    private volatile Thread worker;
    public static List<JSONObject> locations(Context c){List<JSONObject> result=new ArrayList<>();Set<String> seen=new HashSet<>();AppWidgetManager manager=AppWidgetManager.getInstance(c);for(String kind:new String[]{"weather","universal","clock"})for(int id:manager.getAppWidgetIds(new ComponentName(c,Widgets.provider(kind)))){JSONObject root=Store.config(c,id);List<JSONObject> settings=new ArrayList<>();if(kind.equals("universal")){settings.add(UniversalWidget.viewConfig(root,"weather"));JSONObject quick=UniversalWidget.viewConfig(root,"clock");if(quick.optBoolean("quickLook",false))settings.add(quick);}else if(kind.equals("weather")||root.optBoolean("quickLook",false))settings.add(root);for(JSONObject location:settings)if(WeatherData.configured(location)&&seen.add(WeatherData.key(location)))result.add(location);}return result;}
    public static void schedule(Context c){if(locations(c).isEmpty())return;JobScheduler scheduler=c.getSystemService(JobScheduler.class);if(scheduler.getPendingJob(701)!=null)return;scheduler.schedule(new JobInfo.Builder(701,new ComponentName(c,WeatherJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setMinimumLatency(1000).build());}
    @Override public boolean onStartJob(JobParameters params){worker=new Thread(()->{for(JSONObject location:locations(this)){if(Thread.currentThread().isInterrupted())return;WeatherData.refresh(this,location,false);}if(!Thread.currentThread().isInterrupted()){Widgets.updateAll(this);jobFinished(params,false);}},"Nook scheduled weather");worker.start();return true;}
    @Override public boolean onStopJob(JobParameters params){Thread task=worker;if(task!=null)task.interrupt();return true;}
}
