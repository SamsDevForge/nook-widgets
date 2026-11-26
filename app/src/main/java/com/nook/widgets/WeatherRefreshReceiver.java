package com.nook.widgets;

import android.appwidget.AppWidgetManager;
import android.content.*;
import org.json.JSONObject;
import java.util.*;

public final class WeatherRefreshReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent){
        int id=intent.getIntExtra("widget",-1);if(id<=0){WeatherJob.schedule(c);return;}
        android.appwidget.AppWidgetProviderInfo info=AppWidgetManager.getInstance(c).getAppWidgetInfo(id);if(info==null)return;String kind=Widgets.kindOf(info.provider);if(!kind.equals("weather")&&!kind.equals("universal"))return;
        JSONObject root=Store.config(c,id),settings=kind.equals("universal")?UniversalWidget.viewConfig(root,"weather"):root;PendingResult result=goAsync();new Thread(()->{try{WeatherData.refresh(c,settings,true);Widgets.updateAll(c);}finally{result.finish();}},"Nook weather").start();
    }
}
