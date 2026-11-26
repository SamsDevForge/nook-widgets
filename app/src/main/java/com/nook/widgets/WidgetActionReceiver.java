package com.nook.widgets;

import android.appwidget.AppWidgetManager;
import android.content.*;

public class WidgetActionReceiver extends BroadcastReceiver {
    private static final java.util.concurrent.ExecutorService VIDEO=java.util.concurrent.Executors.newSingleThreadExecutor();
    @Override public void onReceive(Context c, Intent i) {
        String action = i.getAction();
        if("motion_action".equals(action)){String op=i.getStringExtra("op");if(op!=null&&op.startsWith("video_")){PendingResult pending=goAsync();VIDEO.execute(()->{try{MediaWidgets.receive(c,i.getIntExtra("widget",-1),op);}finally{pending.finish();}});}else MediaWidgets.receive(c,i.getIntExtra("widget",-1),op);return;}
        if("extra_action".equals(action)){ExtraWidgets.action(c,i.getIntExtra("widget",-1),i.getStringExtra("op"));return;}
        if("universal_switch".equals(action))UniversalWidget.switchPage(c,i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1),i.getStringExtra("page"));
        if("universal_menu".equals(action))UniversalWidget.switchMenu(c,i.getIntExtra("widget",-1),i.getIntExtra("menu",0));
        if("media".equals(action)||"music_action".equals(action)){PendingResult pending=goAsync();VIDEO.execute(()->{try{MusicWidgets.receive(c,i);}finally{pending.finish();}});return;}
        if("library_refresh".equals(action)){int id=i.getIntExtra("widget",-1);android.appwidget.AppWidgetProviderInfo info=AppWidgetManager.getInstance(c).getAppWidgetInfo(id);if(info!=null){String kind=Widgets.kindOf(info.provider);if(kind.equals("weather")||kind.equals("universal")&&UniversalWidget.currentPage(Store.config(c,id)).equals("weather"))c.sendBroadcast(new Intent(c,WeatherRefreshReceiver.class).putExtra("widget",id));else Widgets.update(c,id,kind);}}
        if ("toggle".equals(action)) Store.toggle(c, i.getStringExtra("type"), i.getStringExtra("id"));
        if ("habit".equals(action)) Store.increment(c, i.getStringExtra("id"), i.getIntExtra("delta", 1));
        if("note_extend".equals(action))NoteLife.extend(c,i.getStringExtra("id"));
        if("note_keep".equals(action))NoteLife.keep(c,i.getStringExtra("id"));
        if("note_archive".equals(action))NoteLife.archive(c,i.getStringExtra("id"));
        if ("pinned".equals(action)) {
            int id = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (id != -1) {
                Store.config(c, id, Store.takePending(c, i.getStringExtra("token")));
                Widgets.updateAll(c);
                WeatherJob.schedule(c);
            }
        }
    }
}
