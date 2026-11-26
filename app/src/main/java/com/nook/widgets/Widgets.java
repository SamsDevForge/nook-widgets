package com.nook.widgets;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONObject;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class Widgets {
    private Widgets() {}
    public static final String NEW_NOTE="__new_note__";
    private static final String[] ORIGINAL_KINDS={"notes","reminders","timers","alarms","checklist","habits","clock","battery","calendar","agenda","weather","photos","worldclock","countdown","contacts","search","controls","music"};
    public static final String[] KINDS;
    public static final String[] CATALOG;
    static{KINDS=new String[ORIGINAL_KINDS.length+ExtraKinds.KEYS.length];System.arraycopy(ORIGINAL_KINDS,0,KINDS,0,ORIGINAL_KINDS.length);System.arraycopy(ExtraKinds.KEYS,0,KINDS,ORIGINAL_KINDS.length,ExtraKinds.KEYS.length);CATALOG=new String[KINDS.length+1];CATALOG[0]="universal";System.arraycopy(KINDS,0,CATALOG,1,KINDS.length);}
    public static final String[] SHORT_TITLES={"Notes","Reminders","Timers","Alarms","Checklist","Habits","Clock","Battery","Calendar","Agenda","Weather","Photos","World clock","Countdown","Favorites","Search","Controls","Music"};
    public static final String[] TITLES={"Quick notes","Gentle reminders","Quick timers","Quick alarms","Daily checklist","Daily habits","Day clock","Phone battery","Month calendar","Upcoming agenda","Local weather","Photo frame","World clocks","Event countdown","Favorite contacts","Web search","Quick controls","Now playing"};
    public static final String[] DESCRIPTIONS={"Catch a thought before it goes.","A little nudge, right on time.","A moment for whatever matters.","Your usual times. One tap away.","Less to remember. More to do.","Small steps make good days.","Give your day a little space.","Your phone's charge, at a glance.","A month of possibilities. Or just this week.","Your real calendar events, close at hand.","Current conditions and a three-day forecast.","A favorite photo, framed your way.","Keep your cities a little closer.","Count the days to something good.","Your people, one tap from the dialer.","A small doorway to the web.","Useful phone settings and app shortcuts.","Your active player's artwork and controls."};
    public static final String[] PALETTES = {"Moss", "Ink", "Sand", "Lilac", "Rose", "Sky"};
    public static final int[] BACKGROUNDS = {0xff243128,0xff20252e,0xfff3e9d9,0xffddd9f0,0xfff0dcd6,0xffd9e8ef};
    public static final int[] FOREGROUNDS = {0xffe9f0e4,0xfff0f2f7,0xff3c372d,0xff353047,0xff4c3432,0xff253e4a};
    public static final int[] MUTED = {0xffaebfae,0xffa6b0c1,0xff7d7567,0xff746b90,0xff916e66,0xff5f7e8c};
    private static final int[] SURFACES = {R.drawable.moss_surface,R.drawable.ink_surface,R.drawable.sand_surface,R.drawable.lilac_surface,R.drawable.rose_surface,R.drawable.sky_surface};
    private static final int[] CHIPS = {R.drawable.moss_chip,R.drawable.ink_chip,R.drawable.sand_chip,R.drawable.lilac_chip,R.drawable.rose_chip,R.drawable.sky_chip};
    public static final String[] STYLES = {"Solid", "Frosted", "Smoked", "Liquid Glass"};
    private static final int[][] GLASS_SURFACES = {
        {R.drawable.moss_frosted,R.drawable.ink_frosted,R.drawable.sand_frosted,R.drawable.lilac_frosted,R.drawable.rose_frosted,R.drawable.sky_frosted},
        {R.drawable.moss_smoked,R.drawable.ink_smoked,R.drawable.sand_smoked,R.drawable.lilac_smoked,R.drawable.rose_smoked,R.drawable.sky_smoked}
    };
    private static final int[][] GLASS_CHIPS = {
        {R.drawable.moss_frosted_chip,R.drawable.ink_frosted_chip,R.drawable.sand_frosted_chip,R.drawable.lilac_frosted_chip,R.drawable.rose_frosted_chip,R.drawable.sky_frosted_chip},
        {R.drawable.moss_smoked_chip,R.drawable.ink_smoked_chip,R.drawable.sand_smoked_chip,R.drawable.lilac_smoked_chip,R.drawable.rose_smoked_chip,R.drawable.sky_smoked_chip}
    };
    private static final int[] FROSTED_MUTED = {0xffd0dbd2,0xffd0d8e6,0xff554b3c,0xff534b67,0xff654b47,0xff3f5e6c};
    private static final int[] ICONS={R.drawable.ic_notes,R.drawable.ic_reminders,R.drawable.ic_timers,R.drawable.ic_alarms,R.drawable.ic_checklist,R.drawable.ic_habits,R.drawable.ic_clock,R.drawable.ic_battery,R.drawable.ic_calendar,R.drawable.ic_agenda,R.drawable.ic_weather,R.drawable.ic_photos,R.drawable.ic_worldclock,R.drawable.ic_countdown,R.drawable.ic_contacts,R.drawable.ic_search,R.drawable.ic_controls,R.drawable.ic_music};
    private static final int[] ROWS = {R.id.widget_row0,R.id.widget_row1,R.id.widget_row2};
    private static final int[] ACTIONS = {R.id.widget_action0,R.id.widget_action1,R.id.widget_action2};
    public static final int[] BACKGROUND_IMAGES = {R.id.widget_surface,R.id.widget_action_bg0,R.id.widget_action_bg1,R.id.widget_action_bg2};
    public static int transparency(JSONObject config) {
        int style=Math.max(0,Math.min(3,config.optInt("style",0)));
        if(style==0)return 0;
        return Math.max(0,Math.min(100,config.optInt("transparency",style==3?45:style==1?25:20)));
    }
    public static int surfaceAlpha(JSONObject config) { return Math.round(255f*(100-transparency(config))/100f); }
    private static int palette(JSONObject config,String kind){return Math.max(0,Math.min(5,config.optInt("palette",defaultPalette(kind))));}
    private static int style(JSONObject config){return Math.max(0,Math.min(3,config.optInt("style",0)));}
    static int surfaceResource(JSONObject config,String kind){int p=palette(config,kind),s=style(config);return s==0?SURFACES[p]:GLASS_SURFACES[Math.min(s,2)-1][p];}
    static int chipResource(JSONObject config,String kind){int p=palette(config,kind),s=style(config);return s==0?CHIPS[p]:GLASS_CHIPS[Math.min(s,2)-1][p];}
    static int legacyForeground(JSONObject config,String kind){return style(config)==2?0xfff3f5f9:FOREGROUNDS[palette(config,kind)];}
    static int legacySecondary(JSONObject config,String kind){int s=style(config),p=palette(config,kind);return s==2?0xffcfd6de:s==1?FROSTED_MUTED[p]:MUTED[p];}
    static int foreground(JSONObject config,String kind){return WidgetDesign.customSurface(config)?WidgetDesign.colors(config,kind).text:legacyForeground(config,kind);}
    static int secondary(JSONObject config,String kind){return WidgetDesign.customSurface(config)?WidgetDesign.colors(config,kind).muted:legacySecondary(config,kind);}
    static int iconResource(String kind){return kind.equals("universal")?R.drawable.ic_universal:ExtraKinds.index(kind)>=0?ExtraKinds.ICONS[ExtraKinds.index(kind)]:ICONS[index(kind)];}
    public static void applyPreviewTransparency(View view,JSONObject config){
        int id=view.getId();boolean background=UniversalWidget.isBackground(id);for(int common:BACKGROUND_IMAGES)if(id==common)background=true;
        if(background&&view instanceof android.widget.ImageView image)image.setImageAlpha(surfaceAlpha(config));
        if(view instanceof android.view.ViewGroup group)for(int n=0;n<group.getChildCount();n++)applyPreviewTransparency(group.getChildAt(n),config);
    }
    public static int index(String kind) { for(int i=0;i<KINDS.length;i++) if(KINDS[i].equals(kind)) return i; return 0; }
    public static String shortTitle(String kind){return ExtraKinds.index(kind)>=0?ExtraKinds.TITLES[ExtraKinds.index(kind)]:SHORT_TITLES[index(kind)];}
    public static String title(String kind){return kind.equals("universal")?"Universal widget":ExtraKinds.index(kind)>=0?ExtraKinds.TITLES[ExtraKinds.index(kind)]:TITLES[index(kind)];}
    public static String description(String kind){return kind.equals("universal")?"Your whole toolbox, in one home screen slot.":ExtraKinds.index(kind)>=0?ExtraKinds.DESCRIPTIONS[ExtraKinds.index(kind)]:DESCRIPTIONS[index(kind)];}
    public static int defaultPalette(String kind){return kind.equals("universal")?0:ExtraKinds.index(kind)>=0?ExtraKinds.index(kind)%6:new int[]{2,3,0,1,5,4,0,0,3,5,5,2,1,4,4,2,1,3}[index(kind)];}
    public static Class<?> provider(String kind) {
        if(kind.equals("universal"))return Universal.class;
        if(ExtraKinds.index(kind)>=0)return new Class<?>[]{Stopwatch.class,Compass.class,Recorder.class,ScreenTime.class,DeviceCare.class,Tracker.class,Files.class,Bookmarks.class,Shortcuts.class,Maps.class,Translate.class,Breathing.class,News.class,Stocks.class,Service.class,Tips.class,Animation.class,Slideshow.class,Video.class,Buds.class}[ExtraKinds.index(kind)];
        return new Class<?>[]{Notes.class,Reminders.class,Timers.class,Alarms.class,Checklist.class,Habits.class,Clock.class,Battery.class,Calendar.class,Agenda.class,Weather.class,Photos.class,WorldClock.class,Countdown.class,Contacts.class,Search.class,Controls.class,Music.class}[index(kind)];
    }
    public static String kindOf(ComponentName provider) {
        if(provider!=null&&provider.getClassName().equals(MusicCompact.class.getName()))return "music";
        if(provider.getClassName().equals(VideoPortrait.class.getName())||provider.getClassName().equals(VideoSquare.class.getName()))return "video";
        for (String k : CATALOG) if (provider(k).getName().equals(provider.getClassName())) return k;
        return "notes";
    }
    public static JSONObject defaults(String kind) {
        JSONObject j = new JSONObject();
        try { j.put("palette", defaultPalette(kind)).put("kind", kind).put("density", 0).put("variant", 0).put("style", 0).put("title", "");if(kind.equals("notes"))j.put("entity",NEW_NOTE);if(kind.equals("universal"))j.put("page","menu").put("pages",new JSONObject()); }
        catch (org.json.JSONException e) { throw new IllegalStateException(e); }
        return j;
    }
    public static Class<?>[] providers(String kind){return kind.equals("video")?new Class<?>[]{Video.class,VideoPortrait.class,VideoSquare.class}:kind.equals("music")?new Class<?>[]{Music.class,MusicCompact.class}:new Class<?>[]{provider(kind)};}
    public static void updateAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        for (String kind : CATALOG) for(Class<?> type:providers(kind))for (int id : m.getAppWidgetIds(new ComponentName(c,type))) update(c,id,kind);
    }
    public static void update(Context c, int id, String kind) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        Bundle options = m.getAppWidgetOptions(id);
        boolean landscape=c.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        int height=options.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT:AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,kind.equals("universal")?340:180);
        int width=options.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH:AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,280);
        boolean small=height<(kind.equals("universal")?300:170)||width<230;
        m.updateAppWidget(id, render(c, id, kind, Store.config(c,id), small, false));
    }
    public static RemoteViews render(Context c, int id, String kind, JSONObject config, boolean small, boolean preview) {
        if(preview){try{config=new JSONObject(config.toString()).put("_preview",true);}catch(Exception e){throw new IllegalStateException(e);}}
        if(kind.equals("universal"))return UniversalWidget.render(c,id,config,small,preview);
        if(kind.equals("clock")&&config.optBoolean("quickLook",false))return QuickLookWidget.render(c,id,config,small,preview);
        if(kind.equals("music"))return MusicWidgets.render(c,id,config,small,preview);
        if(kind.equals("buds"))return BudsWidgets.render(c,id,config,small,preview);
        if(kind.equals("video"))return VideoWidgets.render(c,id,config,small,preview);
        if(MediaWidgets.supports(kind))return MediaWidgets.render(c,id,kind,config,small,preview);
        if(ExtraKinds.index(kind)>=0)return ExtraWidgets.render(c,id,kind,config,small,preview);
        if(index(kind)>=7)return LibraryWidgets.render(c,id,kind,config,small,preview);
        RemoteViews v = new RemoteViews(c.getPackageName(), WidgetDesign.layout(c,kind.equals("notes")&&ReplicaDesign.enabled(config)?"replica_notes":"widget",config));
        hide(v,R.id.widget_meta,R.id.widget_actions,R.id.widget_progress,R.id.widget_time,R.id.widget_date,R.id.widget_row0,R.id.widget_row1,R.id.widget_row2);
        v.setViewVisibility(R.id.widget_title,View.VISIBLE);v.setViewVisibility(R.id.widget_body,View.VISIBLE);
        int palette = Math.max(0, Math.min(5, config.optInt("palette", defaultPalette(kind))));
        int variant = config.optInt("variant", 0);
        int style = Math.max(0,Math.min(3,config.optInt("style",0)));
        boolean compact = small || config.optInt("density", 0) == 1 || kind.equals("notes")&&ReplicaDesign.enabled(config)&&WidgetDesign.size(c,id,kind,config,false)[1]<200;
        int text = style==2?0xfff3f5f9:FOREGROUNDS[palette];
        int muted = style==2?0xffcfd6de:style==1?FROSTED_MUTED[palette]:MUTED[palette];
        v.setImageViewResource(R.id.widget_surface,style==0?SURFACES[palette]:GLASS_SURFACES[Math.min(style,2)-1][palette]);
        int surfaceAlpha=surfaceAlpha(config);
        for(int image:BACKGROUND_IMAGES)v.setInt(image,"setImageAlpha",surfaceAlpha);
        v.setViewPadding(R.id.widget_root, Ui.dp(c,compact?12:18), Ui.dp(c,compact?8:14), Ui.dp(c,compact?12:18), Ui.dp(c,compact?8:14));
        v.setTextColor(R.id.widget_label,text); v.setTextColor(R.id.widget_settings,text);
        v.setTextColor(R.id.widget_title,text); v.setTextColor(R.id.widget_body,muted);
        v.setTextColor(R.id.widget_meta,muted);
        v.setTextColor(R.id.widget_time,text); v.setTextColor(R.id.widget_date,muted);
        v.setTextViewTextSize(R.id.widget_title, android.util.TypedValue.COMPLEX_UNIT_SP, compact?17:22);
        v.setInt(R.id.widget_title, "setMaxLines", compact?1:2);
        v.setInt(R.id.widget_body, "setMaxLines", compact?1:4);
        v.setImageViewResource(R.id.widget_icon, iconResource(kind));
        v.setInt(R.id.widget_icon, "setColorFilter", text);
        v.setTextViewText(R.id.widget_label, kind.equals("clock") ? "A LITTLE PERSPECTIVE" : TITLES[index(kind)].toUpperCase(Locale.getDefault()));
        v.setTextViewText(R.id.widget_title, ""); v.setTextViewText(R.id.widget_body, "");
        for (int row : ROWS) v.setTextColor(row, text);
        for(int n=0;n<ACTIONS.length;n++) {
            v.setTextColor(ACTIONS[n],text);
            v.setImageViewResource(BACKGROUND_IMAGES[n+1],style==0?CHIPS[palette]:GLASS_CHIPS[Math.min(style,2)-1][palette]);
        }
        String entity = config.optString("entity", "");
        PendingIntent open = open(c,kind,"",false);
        if (!preview) {
            v.setOnClickPendingIntent(R.id.widget_root,open);
            v.setOnClickPendingIntent(R.id.widget_label,open);
            Intent customize = new Intent(c, WidgetConfigActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra("kind",kind).setData(Uri.parse("nook://configure/"+id));
            v.setOnClickPendingIntent(R.id.widget_settings, PendingIntent.getActivity(c,0,customize,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        }
        if (kind.equals("notes")) {
            List<Store.Item> items = NoteLife.activeNotes(c);
            Store.Item note = NoteLife.selected(c,config,items);
            if (variant == 1 && !items.isEmpty()) {
                hide(v,R.id.widget_title,R.id.widget_body);
                for(int n=0;n<Math.min(compact?1:2,items.size());n++)row(v,n,items.get(n).title,preview?null:open(c,kind,items.get(n).id,false));
                noteActions(v,preview?null:noteCreate(c,id,"temporary"),preview?null:noteCreate(c,id,"permanent"),preview?null:open,
                    "Temporary","Permanent","Library");
            } else if(note!=null&&!NoteLife.active(c,note)){
                v.setTextViewText(R.id.widget_title,note.archived?"Note archived.":"Note expired.");
                v.setTextViewText(R.id.widget_body,"Kept safely in Library → Notes → Saved.");
                noteActions(v,preview?null:(note.archived?open(c,kind,note.id,false):broadcast(c,"note_extend",kind,note.id,1)),
                    preview?null:(note.archived?noteCreate(c,id,"temporary"):broadcast(c,"note_keep",kind,note.id,1)),
                    preview?null:noteCreate(c,id,"permanent"),note.archived?"Restore":"+1 hour",note.archived?"Temporary":"Keep forever","Permanent");
                if(!preview){v.setOnClickPendingIntent(R.id.widget_title,open(c,kind,note.id,false));v.setOnClickPendingIntent(R.id.widget_body,open(c,kind,note.id,false));}
            } else {
                v.setTextViewText(R.id.widget_title,note==null?(preview?"Small ideas,\nbig possibilities.":"A thought worth keeping?"):note.title);
                v.setTextViewText(R.id.widget_body,note==null?(preview?"A place for the things you don’t want to forget.":"Choose Temporary or Permanent to begin."):note.body);
                if(note==null){
                    noteActions(v,preview?null:noteCreate(c,id,"temporary"),preview?null:noteCreate(c,id,"permanent"),preview?null:open,"Temporary","Permanent","Library");
                }else{
                    v.setViewVisibility(R.id.widget_meta,View.VISIBLE);v.setTextViewText(R.id.widget_meta,NoteLife.status(c,note));
                    boolean timer=note.expiresAt>0;
                    noteActions(v,preview?null:(timer?broadcast(c,"note_extend",kind,note.id,1):open(c,kind,note.id,false)),
                        preview?null:(timer?broadcast(c,"note_keep",kind,note.id,1):broadcast(c,"note_archive",kind,note.id,1)),
                        preview?null:(timer?broadcast(c,"note_archive",kind,note.id,1):noteCreate(c,id,"permanent")),timer?"+1 hour":"Edit",timer?"Keep forever":"Archive",timer?"Archive":"+ New");
                    if(!config.optBoolean("noteActions",true))hide(v,R.id.widget_actions);
                }
                v.setInt(R.id.widget_body,"setMaxLines",compact?1:config.optBoolean("noteActions",true)?2:4);
                if(!preview) { PendingIntent edit=open(c,kind,note==null?"":note.id,note==null); v.setOnClickPendingIntent(R.id.widget_title,edit); v.setOnClickPendingIntent(R.id.widget_body,edit); }
            }
            if(compact)hide(v,R.id.widget_meta,R.id.widget_actions);
        } else if (kind.equals("checklist") || kind.equals("reminders")) {
            boolean reminders=kind.equals("reminders");
            List<Store.Item> items = new ArrayList<>();
            long tomorrow=LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            for(Store.Item item:Store.list(c,kind)) {
                if(reminders ? (!item.done && (variant!=1 || item.due<tomorrow)) : (!item.done || variant==1)) items.add(item);
            }
            if (!reminders) items.sort(Comparator.comparing((Store.Item i)->i.done).thenComparingLong(i->i.created));
            if (items.isEmpty()) {
                v.setTextViewText(R.id.widget_title, preview?(reminders?"A little nudge.":"Make room for today."):(reminders?"Nothing on your mind.":"A fresh little list."));
                v.setTextViewText(R.id.widget_body,preview?(reminders?"Stretch & take a break · 4:00 PM":"○  A walk outside\n○  Read a few pages"):(reminders?"Tap to add a reminder.":"Tap to add your first task."));
                if(!preview) v.setOnClickPendingIntent(R.id.widget_title,open(c,kind,"",true));
            } else {
                hide(v,R.id.widget_title,R.id.widget_body);
                for(int n=0;n<Math.min(compact?1:3,items.size());n++) {
                    Store.Item item=items.get(n);
                    String label=(item.done?"✓  ":"○  ")+item.title+(reminders?"  ·  "+when(item.due):"");
                    row(v,n,label,preview?null:broadcast(c,"toggle",kind,item.id,1));
                    v.setContentDescription(ROWS[n], (item.done?"Reopen ":"Mark complete: ")+item.title);
                }
            }
        } else if (kind.equals("timers") || kind.equals("alarms")) {
            boolean timer=kind.equals("timers");
            v.setTextViewText(R.id.widget_title,timer?new String[]{"A little time for you.","Find your focus.","Something’s cooking."}[Math.min(2,variant)]:"A good day starts here.");
            hide(v,R.id.widget_body);
            if(compact) hide(v,R.id.widget_title);
            v.setViewVisibility(R.id.widget_actions,View.VISIBLE);
            int[] values = shortcuts(config,timer,variant);
            for(int n=0;n<3;n++) {
                v.setTextViewText(ACTIONS[n],timer?values[n]+" min":alarmTime(values[n]));
                v.setContentDescription(ACTIONS[n],timer?"Start a "+values[n]+" minute timer":"Set alarm for "+alarmTime(values[n]));
                if(!preview) v.setOnClickPendingIntent(ACTIONS[n],quick(c,timer?"timer":"alarm",values[n]));
            }
        } else if (kind.equals("habits")) {
            List<Store.Item> items=Store.list(c,"habits");
            Store.Item habit=entity.isEmpty()?(items.isEmpty()?null:items.get(0)):Store.find(c,"habits",entity);
            int count=habit==null?(preview?3:0):habit.todayCount(), target=habit==null?8:habit.target;
            v.setTextViewText(R.id.widget_title,habit==null?(preview?"Drink more water":"One small step."):habit.title);
            v.setTextViewText(R.id.widget_body,habit==null&&!preview?"Tap to add a daily habit.":count+" of "+target+" today"+(count>=target?" · You did it!":" · Keep going"));
            if(habit!=null || preview) {
                v.setViewVisibility(R.id.widget_progress,variant==1?View.GONE:View.VISIBLE); v.setProgressBar(R.id.widget_progress,target,count,false);
                v.setViewVisibility(R.id.widget_actions,View.VISIBLE);
                v.setTextViewText(ACTIONS[0],"−"); v.setTextViewText(ACTIONS[1],count+" / "+target); v.setTextViewText(ACTIONS[2],"+");
                v.setContentDescription(ACTIONS[0],"Decrease today’s count"); v.setContentDescription(ACTIONS[2],"Increase today’s count");
                if(!preview && habit!=null) { v.setOnClickPendingIntent(ACTIONS[0],broadcast(c,"habit",kind,habit.id,-1)); v.setOnClickPendingIntent(ACTIONS[2],broadcast(c,"habit",kind,habit.id,1)); v.setOnClickPendingIntent(ACTIONS[1],open(c,kind,habit.id,false)); }
                if(compact) hide(v,R.id.widget_progress,R.id.widget_body);
            } else if(!preview) v.setOnClickPendingIntent(R.id.widget_title,open(c,kind,"",true));
        } else if (kind.equals("clock")) {
            hide(v,R.id.widget_title); v.setViewVisibility(R.id.widget_time,View.VISIBLE); v.setViewVisibility(R.id.widget_date,View.VISIBLE);
            v.setTextViewTextSize(R.id.widget_time,android.util.TypedValue.COMPLEX_UNIT_SP,compact?36:52);
            if(variant==1) { v.setCharSequence(R.id.widget_time,"setFormat12Hour","HH:mm"); v.setCharSequence(R.id.widget_time,"setFormat24Hour","HH:mm"); }
            else { v.setCharSequence(R.id.widget_time,"setFormat12Hour","h:mm a"); v.setCharSequence(R.id.widget_time,"setFormat24Hour","h:mm a"); }
            v.setTextViewText(R.id.widget_body,"Make room for what matters."); if(compact) hide(v,R.id.widget_body);
        }
        String title=config.optString("title","").trim();
        if(!title.isEmpty()) v.setTextViewText(R.id.widget_label,title.toUpperCase(Locale.getDefault()));
        WidgetDesign.surfaces(c,v,id,kind,config,compact,new int[]{R.id.widget_action_bg0,R.id.widget_action_bg1,R.id.widget_action_bg2});
        WidgetDesign.dedicated(c,v,id,kind,config,compact);
        return v;
    }
    private static void hide(RemoteViews v,int... ids) { for(int id:ids) v.setViewVisibility(id,View.GONE); }
    private static void noteActions(RemoteViews v,PendingIntent first,PendingIntent second,PendingIntent third,String a,String b,String c){
        v.setViewVisibility(R.id.widget_actions,View.VISIBLE);String[] labels={a,b,c};PendingIntent[] intents={first,second,third};
        for(int n=0;n<3;n++){v.setTextViewText(ACTIONS[n],labels[n]);if(intents[n]!=null)v.setOnClickPendingIntent(ACTIONS[n],intents[n]);}
    }
    private static PendingIntent noteCreate(Context c,int widgetId,String mode){
        Intent i=new Intent(c,MainActivity.class).putExtra("section","notes").putExtra("create",true).putExtra("noteMode",mode).putExtra("noteWidget",widgetId)
            .setData(Uri.parse("nook://create-note/"+widgetId+"/"+mode));
        return PendingIntent.getActivity(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private static void row(RemoteViews v,int n,String text,PendingIntent action) {
        v.setViewVisibility(ROWS[n],View.VISIBLE); v.setTextViewText(ROWS[n],text); if(action!=null) v.setOnClickPendingIntent(ROWS[n],action);
    }
    private static PendingIntent open(Context c,String kind,String id,boolean create) {
        Intent i=new Intent(c,MainActivity.class).putExtra("section",kind).putExtra("edit",id).putExtra("create",create)
            .setData(Uri.parse("nook://open/"+kind+"/"+id+"/"+create));
        return PendingIntent.getActivity(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private static PendingIntent broadcast(Context c,String action,String type,String id,int delta) {
        Intent i=new Intent(c,WidgetActionReceiver.class).setAction(action).putExtra("type",type).putExtra("id",id).putExtra("delta",delta)
            .setData(Uri.parse("nook://action/"+action+"/"+type+"/"+id+"/"+delta));
        return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private static PendingIntent quick(Context c,String action,int value) {
        Intent i=new Intent(c,QuickActionActivity.class).putExtra("action",action).putExtra("value",value).setData(Uri.parse("nook://quick/"+action+"/"+value));
        return PendingIntent.getActivity(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    public static int[] shortcuts(JSONObject config,boolean timer,int variant) {
        int[][] defaults=timer?new int[][]{{1,5,10},{25,5,50},{3,7,15}}:new int[][]{{420,450,480},{360,390,420},{480,510,540}};
        int[] values=defaults[Math.max(0,Math.min(2,variant))].clone();
        for(int n=0;n<3;n++) values[n]=Math.max(timer?1:0,Math.min(timer?1440:1439,config.optInt("value"+n,values[n])));
        return values;
    }
    public static String alarmTime(int minute) { return LocalTime.of(minute/60,minute%60).format(DateTimeFormatter.ofPattern("HH:mm")); }
    public static String when(long due) {
        ZonedDateTime time=Instant.ofEpochMilli(due).atZone(ZoneId.systemDefault());
        String day=time.toLocalDate().equals(LocalDate.now())?"Today":time.toLocalDate().equals(LocalDate.now().plusDays(1))?"Tomorrow":time.format(DateTimeFormatter.ofPattern("d MMM"));
        return day+", "+time.format(DateTimeFormatter.ofPattern("h:mm a"));
    }
    public abstract static class Base extends AppWidgetProvider {
        protected abstract String kind();
        @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){if(kind().equals("notes")||kind().equals("universal"))NoteLife.refresh(c);for(int id:ids)update(c,id,kind());if(kind().equals("weather")||kind().equals("universal")||kind().equals("clock"))c.sendBroadcast(new Intent(c,WeatherRefreshReceiver.class));}
        @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options) { update(c,id,kind()); }
        @Override public void onDeleted(Context c,int[] ids) { for(int id:ids){Store.removeConfig(c,id);InlineVideoState.reset(c,id);} }
    }
    public static class Notes extends Base { protected String kind(){return "notes";} }
    public static class Reminders extends Base { protected String kind(){return "reminders";} }
    public static class Timers extends Base { protected String kind(){return "timers";} }
    public static class Alarms extends Base { protected String kind(){return "alarms";} }
    public static class Checklist extends Base { protected String kind(){return "checklist";} }
    public static class Habits extends Base { protected String kind(){return "habits";} }
    public static class Clock extends Base { protected String kind(){return "clock";} }
    public static class Universal extends Base { protected String kind(){return "universal";} }
    public static class Battery extends Base{protected String kind(){return "battery";}}
    public static class Calendar extends Base{protected String kind(){return "calendar";}}
    public static class Agenda extends Base{protected String kind(){return "agenda";}}
    public static class Weather extends Base{protected String kind(){return "weather";}}
    public static class Photos extends Base{protected String kind(){return "photos";}}
    public static class WorldClock extends Base{protected String kind(){return "worldclock";}}
    public static class Countdown extends Base{protected String kind(){return "countdown";}}
    public static class Contacts extends Base{protected String kind(){return "contacts";}}
    public static class Search extends Base{protected String kind(){return "search";}}
    public static class Controls extends Base{protected String kind(){return "controls";}}
    public static class Music extends Base{protected String kind(){return "music";}}
    public static class Stopwatch extends Base{protected String kind(){return "stopwatch";}}
    public static class Compass extends Base{protected String kind(){return "compass";}}
    public static class Recorder extends Base{protected String kind(){return "recorder";}}
    public static class ScreenTime extends Base{protected String kind(){return "screentime";}}
    public static class DeviceCare extends Base{protected String kind(){return "devicecare";}}
    public static class Tracker extends Base{protected String kind(){return "tracker";}}
    public static class Files extends Base{protected String kind(){return "files";}}
    public static class Bookmarks extends Base{protected String kind(){return "bookmarks";}}
    public static class Shortcuts extends Base{protected String kind(){return "shortcuts";}}
    public static class Maps extends Base{protected String kind(){return "maps";}}
    public static class Translate extends Base{protected String kind(){return "translate";}}
    public static class Breathing extends Base{protected String kind(){return "breathing";}}
    public static class News extends Base{protected String kind(){return "news";}}
    public static class Stocks extends Base{protected String kind(){return "stocks";}}
    public static class Service extends Base{protected String kind(){return "service";}}
    public static class Tips extends Base{protected String kind(){return "tips";}}
    public static class Animation extends Base{protected String kind(){return "animation";}}
    public static class Slideshow extends Base{protected String kind(){return "slideshow";}}
    public static class Buds extends Base{protected String kind(){return "buds";}}
    public static class Video extends Base{protected String kind(){return "video";}}
    public static class MusicCompact extends Base{protected String kind(){return "music";}}
    public static class VideoPortrait extends Base{protected String kind(){return "video";}}
    public static class VideoSquare extends Base{protected String kind(){return "video";}}
}
