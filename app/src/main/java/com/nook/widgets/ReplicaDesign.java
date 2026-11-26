package com.nook.widgets;

import android.content.Context;
import android.graphics.*;
import android.media.session.*;
import android.util.TypedValue;
import android.view.*;
import android.widget.RemoteViews;
import org.json.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Original, data-driven compositions based on each platform's widget hierarchy. */
public final class ReplicaDesign {
    private ReplicaDesign(){}
    public static boolean enabled(JSONObject config){int d=WidgetDesign.id(config);return d==1||d==2||d==3||d==4||d==7;}
    private static boolean apple(int d){return d==2||d==3;}
    private static final int[] ACTIONS={R.id.widget_action0,R.id.widget_action1,R.id.widget_action2};
    private static final int[] ANALOG={R.id.replica_clock_apple,R.id.replica_clock_dot,R.id.replica_clock_pixel,R.id.replica_clock_samsung,R.id.replica_clock_glassdark,R.id.replica_clock_glasslight};
    private static int contrast(int color){return Color.luminance(color)>.43?0xff191919:Color.WHITE;}
    private static void hide(RemoteViews v,int... ids){for(int id:ids)v.setViewVisibility(id,View.GONE);}
    private static void text(RemoteViews v,int id,float sp,JSONObject config){v.setTextViewTextSize(id,TypedValue.COMPLEX_UNIT_SP,sp*WidgetDesign.scale(config));}
    public static void colors(WidgetDesign.Colors c,int d,String kind,boolean dark){
        if(d==1){c.surface=dark?0xff171717:0xfff5f5f1;c.text=dark?0xfff5f5f1:0xff191919;c.muted=dark?0xffbcbcb7:0xff62625d;c.chip=dark?0xff2c2c2c:0xffe7e7e2;c.accent=dark?0xffff544b:0xffcc3029;}
        if(apple(d)){
            c.surface=dark?0xff202022:d==3?0xffe9e9e9:0xffffffff;c.chip=dark?0xff363638:0xfff0f0f3;
            c.accent=switch(kind){case "notes"->dark?0xffffd34b:0xffa77400;case "calendar","agenda"->dark?0xffff6965:0xffd92c30;case "battery","habits"->dark?0xff6ee685:0xff247b38;case "timers","alarms"->dark?0xffffb04d:0xffa25d00;default->dark?0xff83baff:0xff0874df;};
            if(kind.equals("weather")){c.surface=dark?0xff173150:d==3?0xff527794:0xff2872b8;c.text=Color.WHITE;c.muted=0xffe2efff;c.chip=dark?0xff355472:0xff518fc5;c.accent=Color.WHITE;}
        }
        if(d==7){c.surface=dark?0xff171719:0xffffffff;c.chip=dark?0xff333338:0xfff0f0f2;c.accent=dark?0xff88b5ff:0xff2574ed;if(kind.equals("battery")||kind.equals("habits"))c.accent=dark?0xff73e082:0xff278838;if(kind.equals("notes")&&!dark)c.surface=0xfffff3cf;}
    }
    public static Bitmap button(Context c,JSONObject config,String kind,int width,int height,int index){
        WidgetDesign.Colors colors=WidgetDesign.colors(config,kind);int d=WidgetDesign.id(config);Bitmap b=Bitmap.createBitmap(width*2,height*2,Bitmap.Config.ARGB_8888);b.setDensity(Bitmap.DENSITY_NONE);Canvas canvas=new Canvas(b);canvas.scale(2,2);Paint p=new Paint(3);
        boolean transport=kind.equals("music")&&index==1,avatar=kind.equals("contacts"),control=kind.equals("controls");
        if(kind.equals("search"))return b;
        if(avatar||control){float x=width/2f,y=16,r=14;p.setColor(colors.chip);canvas.drawCircle(x,y,r,p);p.setColor(colors.accent);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif-medium",0));p.setTextSize(12);if(avatar){String label=LibraryWidgets.contactLabel(config,index);canvas.drawText(label.startsWith("+")?"+":label.substring(0,1).toUpperCase(Locale.ROOT),x,20,p);}else{int slot=WidgetDesign.clamp(config.optInt("control"+index,config.optInt("variant",0)==0?index:new int[]{6,7,5}[index]),0,7);drawControl(canvas,p,x,y,slot);}return b;}
        p.setColor(transport?colors.accent:colors.chip);float r=d==1?24:d==4?24:apple(d)?12:24;
        if(kind.equals("notes")&&apple(d))p.setColor(colors.chip);
        canvas.drawRoundRect(1,1,width-1,height-1,r,r,p);
        if(d==1&&!transport){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.7f);p.setColor(colors.muted);canvas.drawRoundRect(1,1,width-1,height-1,r,r,p);}
        return b;
    }
    private static void drawControl(Canvas canvas,Paint p,float x,float y,int slot){
        canvas.save();canvas.translate(x,y);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.6f);p.setStrokeCap(Paint.Cap.ROUND);
        if(slot==0){for(int n=0;n<3;n++){float r=4+n*4;canvas.drawArc(-r,-r+5,r,r+5,220,100,false,p);}p.setStyle(Paint.Style.FILL);canvas.drawCircle(0,7,1.5f,p);}
        else if(slot==1){Path a=new Path();a.moveTo(0,-10);a.lineTo(6,-5);a.lineTo(-6,5);a.moveTo(-6,-5);a.lineTo(6,5);a.lineTo(0,10);a.close();canvas.drawPath(a,p);canvas.drawLine(0,-10,0,10,p);}
        else if(slot==2){canvas.drawCircle(0,0,4,p);for(int n=0;n<8;n++){double a=n*Math.PI/4;canvas.drawLine((float)Math.cos(a)*7,(float)Math.sin(a)*7,(float)Math.cos(a)*10,(float)Math.sin(a)*10,p);}}
        else if(slot==5){canvas.drawRoundRect(-6,-9,6,9,2,2,p);canvas.drawLine(-2,-12,2,-12,p);}
        else if(slot==7){canvas.drawRoundRect(-10,-7,10,8,3,3,p);canvas.drawCircle(0,0,4,p);}
        else{canvas.drawRoundRect(-8,-8,8,8,3,3,p);canvas.drawLine(-4,0,4,0,p);canvas.drawLine(0,-4,0,4,p);}
        canvas.restore();p.setStyle(Paint.Style.FILL);
    }
    public static void apply(Context c,RemoteViews v,int widgetId,String kind,JSONObject config,boolean compact){
        int d=WidgetDesign.id(config);WidgetDesign.Colors colors=WidgetDesign.colors(config,kind);boolean embedded=config.optBoolean("_embedded",false),quiet=Set.of("clock","weather","calendar","battery","photos","search","controls","contacts","music").contains(kind);
        for(int id:ANALOG)v.setViewVisibility(id,View.GONE);
        v.setViewVisibility(R.id.replica_dial,View.GONE);
        v.setViewVisibility(R.id.replica_spacer,Widgets.index(kind)>=7?View.GONE:View.VISIBLE);
        v.setViewVisibility(R.id.widget_settings,embedded?View.GONE:View.VISIBLE);v.setTextColor(R.id.widget_settings,colors.muted);
        boolean icon=config.optBoolean("showIcon",false),label=config.optBoolean("showLabel",!quiet);
        v.setViewVisibility(R.id.widget_icon,icon?View.VISIBLE:View.GONE);v.setViewVisibility(R.id.widget_label,label?View.VISIBLE:View.INVISIBLE);
        v.setViewVisibility(R.id.widget_header,(!label&&!icon)||embedded?View.GONE:View.VISIBLE);
        v.setViewPadding(R.id.widget_root,Ui.dp(c,compact?14:d==3?16:18),Ui.dp(c,compact?12:16),Ui.dp(c,compact?14:d==3?16:18),Ui.dp(c,compact?12:16));
        if(config.has("padding")){int p=WidgetDesign.padding(config,compact);v.setViewPadding(R.id.widget_root,Ui.dp(c,p),Ui.dp(c,compact?8:14),Ui.dp(c,p),Ui.dp(c,compact?8:14));}
        String labelText=config.optString("title","").trim();if(labelText.isEmpty())labelText=switch(kind){case "notes"->"Notes";case "checklist"->"Tasks";case "reminders"->"Reminders";case "timers"->"Timers";case "alarms"->"Alarms";case "habits"->"Today";case "contacts"->"Favorites";case "worldclock"->"World clock";case "controls"->"Quick controls";case "countdown"->"Countdown";case "music"->"Now playing";default->Widgets.title(kind);};
        v.setTextViewText(R.id.widget_label,d==1?labelText.toUpperCase(Locale.ROOT):labelText);v.setFloat(R.id.widget_label,"setLetterSpacing",d==1?.06f:0f);text(v,R.id.widget_label,d==1?11:13,config);v.setTextColor(R.id.widget_label,config.has("accent")?colors.accent:colors.text);
        text(v,R.id.widget_title,compact?19:d==7?26:apple(d)?23:26,config);text(v,R.id.widget_body,compact?12:14,config);text(v,R.id.widget_meta,10,config);
        for(int action:ACTIONS){v.setTextColor(action,colors.text);v.setInt(action,"setGravity",Gravity.CENTER);v.setInt(action,"setMaxLines",1);v.setViewPadding(action,2,0,2,0);}
        if(apple(d)&&kind.equals("notes")&&!GlassMaterial.enabled(config)){int headerInk=config.has("accent")?contrast(colors.accent):0xff29251b;v.setTextColor(R.id.widget_label,headerInk);v.setInt(R.id.widget_icon,"setColorFilter",headerInk);v.setTextColor(R.id.widget_settings,headerInk);}
        if(kind.equals("notes")){hide(v,R.id.replica_spacer);text(v,R.id.widget_title,compact?18:21,config);boolean shortCard=compact||WidgetDesign.size(c,widgetId,kind,config,compact)[1]<235;v.setInt(R.id.widget_body,"setMaxLines",shortCard?1:2);if(shortCard&&NoteLife.selected(c,config)!=null)v.setInt(R.id.widget_title,"setMaxLines",1);if(compact&&NoteLife.selected(c,config)==null){hide(v,R.id.widget_title,R.id.widget_body,R.id.widget_meta);v.setViewVisibility(R.id.widget_actions,View.VISIBLE);}}
        if(kind.equals("clock")){
            hide(v,R.id.widget_body);int face=config.optInt("clockFace",0);if(face==0)face=apple(d)||d==4?2:1;
            boolean analog=face==2;
            v.setViewVisibility(R.id.widget_time,analog?View.GONE:View.VISIBLE);v.setViewVisibility(R.id.widget_date,View.VISIBLE);v.setViewVisibility(R.id.replica_spacer,View.GONE);
            v.setInt(R.id.widget_root,"setGravity",Gravity.CENTER_VERTICAL);v.setInt(R.id.widget_date,"setGravity",Gravity.CENTER);text(v,R.id.widget_date,compact?11:13,config);
            String dateFormat=!analog&&config.optInt("variant",0)==0?"a · EEEE, d MMM":"EEEE, d MMM";v.setCharSequence(R.id.widget_date,"setFormat12Hour",dateFormat);v.setCharSequence(R.id.widget_date,"setFormat24Hour",dateFormat);
            if(analog){v.setViewVisibility(GlassMaterial.enabled(config)?config.optInt("tone",0)==1?R.id.replica_clock_glasslight:R.id.replica_clock_glassdark:ANALOG[apple(d)?0:d==1?1:d==4?2:3],View.VISIBLE);}
            else{v.setInt(R.id.widget_time,"setGravity",Gravity.CENTER);v.setInt(R.id.widget_time,"setMaxLines",face==3?2:1);String format=face==3?(config.optInt("variant",0)==1?"HH\nmm":"hh\nmm"):config.optInt("variant",0)==1?"HH:mm":"h:mm";v.setCharSequence(R.id.widget_time,"setFormat12Hour",format);v.setCharSequence(R.id.widget_time,"setFormat24Hour",format);float sp=(face==3?(compact?42:68):(compact?48:d==7?70:62))*WidgetDesign.scale(config);Paint measure=new Paint(3);measure.setTypeface(WidgetDesign.typeface(c,config,true));measure.setTextSize(sp);float available=WidgetDesign.size(c,widgetId,kind,config,compact)[0]-WidgetDesign.padding(config,compact)*2-8;sp=Math.min(sp,sp*available/Math.max(1,measure.measureText(face==3?"88":"23:58")*c.getResources().getConfiguration().fontScale));v.setTextViewTextSize(R.id.widget_time,TypedValue.COMPLEX_UNIT_SP,sp);}
        }
        if(kind.equals("timers")||kind.equals("alarms")){
            v.setTextViewText(R.id.widget_title,kind.equals("timers")?"Set a timer":"Alarm shortcuts");text(v,R.id.widget_title,d==1?28:compact?19:25,config);
            if(kind.equals("timers")&&d==1){int[] durations=Widgets.shortcuts(config,true,config.optInt("variant",0));for(int n=0;n<3;n++){v.setTextViewText(ACTIONS[n],durations[n]+"′");text(v,ACTIONS[n],22,config);}if(!compact){hide(v,R.id.widget_title,R.id.replica_spacer);v.setViewVisibility(R.id.replica_dial,View.VISIBLE);v.setImageViewBitmap(R.id.replica_dial,timer(c,config,durations[0]));v.setContentDescription(R.id.replica_dial,"First timer preset: "+durations[0]+" minutes; use the buttons to start a timer");}}
        }
        if(kind.equals("controls")||kind.equals("contacts")&&config.optInt("variant",0)==0){hide(v,R.id.widget_title,R.id.widget_body);for(int action:ACTIONS){v.setInt(action,"setGravity",Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);v.setViewPadding(action,0,0,0,1);text(v,action,11,config);}}
        if(kind.equals("worldclock"))hide(v,R.id.widget_title);
        if(kind.equals("search")){hide(v,R.id.widget_title,R.id.widget_body);v.setTextViewText(R.id.widget_label,"Search");v.setInt(R.id.widget_root,"setGravity",Gravity.CENTER_VERTICAL);v.setTextViewText(ACTIONS[0],"⌕  Search…");v.setInt(ACTIONS[0],"setGravity",Gravity.START|Gravity.CENTER_VERTICAL);text(v,ACTIONS[0],16,config);v.setTextViewText(ACTIONS[2],embedded?"···":"");}
        if(kind.equals("music")){if(config.optInt("align",0)==0){v.setInt(R.id.widget_title,"setGravity",Gravity.START);v.setInt(R.id.widget_body,"setGravity",Gravity.START);}MediaController player=MediaAccess.controller(c);if(player!=null){PlaybackState state=player.getPlaybackState();boolean playing=state!=null&&state.getState()==PlaybackState.STATE_PLAYING;String[] glyphs={"‹‹",playing?"Ⅱ":"▶","››"},descriptions={"Previous track",playing?"Pause playback":"Play","Next track"};for(int n=0;n<3;n++){v.setTextViewText(ACTIONS[n],glyphs[n]);v.setContentDescription(ACTIONS[n],descriptions[n]);text(v,ACTIONS[n],22,config);}v.setTextColor(ACTIONS[1],contrast(colors.accent));}}
    }
    private static Bitmap bitmap(int w,int h){Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);b.setDensity(Bitmap.DENSITY_NONE);return b;}
    private static void label(Canvas canvas,Paint p,String text,float x,float y,float size,int color,Typeface face){p.setStyle(Paint.Style.FILL);p.setShader(null);p.setTextSize(size);p.setColor(color);p.setTypeface(face);canvas.drawText(text,x,y,p);}
    public static Bitmap battery(Context c,JSONObject config,WidgetData.Battery battery){
        Bitmap b=bitmap(600,270);Canvas canvas=new Canvas(b);Paint p=new Paint(3);int d=WidgetDesign.id(config);WidgetDesign.Colors colors=WidgetDesign.colors(config,"battery");Typeface face=WidgetDesign.typeface(c,config,true);float x=apple(d)?100:d==7?110:150,y=120,r=apple(d)?72:d==7?82:90;
        if(d==4){p.setColor(colors.chip);canvas.drawRoundRect(20,40,580,214,80,80,p);p.setColor(colors.accent);canvas.drawRoundRect(20,40,20+560*Math.max(0,battery.percent)/100f,214,80,80,p);p.setTextAlign(Paint.Align.RIGHT);label(canvas,p,battery.percent<0?"—":battery.percent+"%",540,159,96,contrast(colors.accent),face);p.setTextAlign(Paint.Align.LEFT);phone(canvas,p,72,95,contrast(colors.accent));return b;}
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d==1?13:16);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(colors.chip);canvas.drawCircle(x,y,r,p);p.setColor(colors.accent);
        if(d==1){for(int n=0;n<40;n++){p.setColor(n<Math.max(0,battery.percent)*.4f?colors.text:colors.chip);canvas.drawArc(x-r,y-r,x+r,y+r,-90+n*9,4,false,p);}}
        else canvas.drawArc(x-r,y-r,x+r,y+r,-90,Math.max(0,battery.percent)*3.6f,false,p);
        p.setStyle(Paint.Style.FILL);phone(canvas,p,x-17,y-29,colors.text);
        p.setTextAlign(Paint.Align.LEFT);label(canvas,p,battery.percent<0?"—":battery.percent+"%",d==1?285:230,145,d==1?78:94,colors.text,face);label(canvas,p,"This phone",d==1?285:234,192,24,colors.muted,Typeface.create("sans-serif",0));
        return b;
    }
    private static void phone(Canvas canvas,Paint p,float x,float y,int color){p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);canvas.drawRoundRect(x,y,x+34,y+58,7,7,p);canvas.drawLine(x+13,y+7,x+21,y+7,p);canvas.drawLine(x+12,y+50,x+22,y+50,p);p.setStyle(Paint.Style.FILL);}
    public static Bitmap calendar(Context c,JSONObject config,boolean week){
        int d=WidgetDesign.id(config);WidgetDesign.Colors colors=WidgetDesign.colors(config,"calendar");Bitmap b=bitmap(600,week?155:350);Canvas canvas=new Canvas(b);Paint p=new Paint(3);LocalDate today=LocalDate.now();DayOfWeek start=config.optBoolean("sunday",false)?DayOfWeek.SUNDAY:DayOfWeek.MONDAY;LocalDate first=(week?today:today.withDayOfMonth(1)).with(TemporalAdjusters.previousOrSame(start));boolean split=!week&&(apple(d)||d==7||d==1);float left=split?185:0,gridWidth=600-left;int headingColor=apple(d)?colors.accent:colors.text;Typeface normal=WidgetDesign.typeface(c,config,false),display=WidgetDesign.typeface(c,config,true);
        p.setTextAlign(Paint.Align.LEFT);
        if(split){label(canvas,p,today.format(DateTimeFormatter.ofPattern("EEEE")).toUpperCase(Locale.getDefault()),4,30,18,headingColor,normal);label(canvas,p,String.valueOf(today.getDayOfMonth()),0,144,d==1?108:124,colors.text,display);label(canvas,p,today.format(DateTimeFormatter.ofPattern("MMM yyyy")),4,186,21,colors.muted,normal);}
        label(canvas,p,today.format(DateTimeFormatter.ofPattern("MMMM")).toUpperCase(Locale.getDefault()),left+8,30,22,headingColor,normal);
        Set<LocalDate> events=new HashSet<>();for(WidgetData.Event event:WidgetData.events(c,first.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),first.plusDays(42).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()))events.add(WidgetData.eventDay(event));
        p.setTextAlign(Paint.Align.CENTER);for(int col=0;col<7;col++){int day=(col+(start==DayOfWeek.SUNDAY?6:0))%7;float x=left+(col+.5f)*gridWidth/7;label(canvas,p,new String[]{"M","T","W","T","F","S","S"}[day],x,week?60:74,19,d==7&&day==6?0xffd33a40:d==7&&day==5?0xff3779cb:colors.muted,normal);}
        for(int n=0;n<(week?7:42);n++){LocalDate date=first.plusDays(n);float x=left+(n%7+.5f)*gridWidth/7,y=(week?110:112)+(n/7)*40;boolean selected=date.equals(today);if(selected){p.setColor(colors.accent);canvas.drawCircle(x,y-8,week?25:17,p);}int color=selected?contrast(colors.accent):date.getMonth()!=today.getMonth()?colors.muted:d==7&&date.getDayOfWeek()==DayOfWeek.SUNDAY?0xffd33a40:d==7&&date.getDayOfWeek()==DayOfWeek.SATURDAY?0xff3779cb:colors.text;label(canvas,p,String.valueOf(date.getDayOfMonth()),x,y,week?30:22,color,normal);if(events.contains(date)){p.setColor(selected?contrast(colors.accent):colors.accent);canvas.drawCircle(x,y+9,2,p);}}
        return b;
    }
    public static void library(Context c,RemoteViews v,String kind,JSONObject config,boolean compact){
        if(!enabled(config))return;int d=WidgetDesign.id(config);
        if(kind.equals("calendar"))hide(v,R.id.widget_title);
        if(kind.equals("weather")){JSONObject data=WeatherData.cached(c,config),current=data.optJSONObject("current");if(current!=null&&WeatherData.configured(config)){hide(v,R.id.widget_title,R.id.widget_body);v.setViewVisibility(R.id.tool_visual,View.VISIBLE);v.setImageViewBitmap(R.id.tool_visual,weather(c,config,data,compact));v.setContentDescription(R.id.tool_visual,config.optString("city","Weather")+", "+WeatherData.degrees(current.optDouble("temperature_2m"),config)+", "+WeatherData.condition(current.optInt("weather_code",-1)));}}
        if(kind.equals("photos")){Bitmap photo=WidgetData.photo(c,config.optString("photoUri",""));v.setViewVisibility(R.id.replica_photo_space,photo==null?View.GONE:View.VISIBLE);if(photo!=null){v.setViewPadding(R.id.widget_root,Ui.dp(c,18),Ui.dp(c,16),Ui.dp(c,18),Ui.dp(c,18));v.setTextColor(R.id.widget_title,Color.WHITE);v.setTextColor(R.id.widget_settings,Color.WHITE);text(v,R.id.widget_title,18,config);hide(v,R.id.replica_spacer);}}
        if(kind.equals("countdown")){WidgetDesign.Colors colors=WidgetDesign.colors(config,kind);v.setTextColor(R.id.widget_title,d==7?colors.accent:colors.text);}
    }
    public static Bitmap photo(Context c,int widgetId,JSONObject config,Bitmap source){
        int[] dimensions=WidgetDesign.size(c,widgetId,"photos",config,config.optInt("density",0)==1);int width=Math.min(720,Math.max(300,Math.round(dimensions[0]*c.getResources().getDisplayMetrics().density))),height=Math.max(150,Math.min(880,Math.round(width*dimensions[1]/(float)dimensions[0])));Bitmap b=bitmap(width,height);Canvas canvas=new Canvas(b);Paint p=new Paint(3);float radius=WidgetDesign.radius(config)*width/(float)dimensions[0];Path outline=new Path();outline.addRoundRect(new RectF(0,0,width,height),radius,radius,Path.Direction.CW);canvas.clipPath(outline);p.setColor(WidgetDesign.colors(config,"photos").surface);canvas.drawRect(0,0,width,height,p);float factor=config.optInt("variant",0)==1?Math.min(width/(float)source.getWidth(),height/(float)source.getHeight()):Math.max(width/(float)source.getWidth(),height/(float)source.getHeight());float w=source.getWidth()*factor,h=source.getHeight()*factor;canvas.drawBitmap(source,null,new RectF((width-w)/2,(height-h)/2,(width+w)/2,(height+h)/2),p);p.setShader(new LinearGradient(0,height*.45f,0,height,0,0x99000000,Shader.TileMode.CLAMP));canvas.drawRect(0,height*.45f,width,height,p);return b;
    }
    private static Bitmap timer(Context c,JSONObject config,int minutes){Bitmap b=bitmap(600,270);Canvas canvas=new Canvas(b);Paint p=new Paint(3);WidgetDesign.Colors colors=WidgetDesign.colors(config,"timers");p.setStrokeWidth(3);p.setStrokeCap(Paint.Cap.ROUND);for(int n=0;n<40;n++){double a=(n*6+153)*Math.PI/180;float inner=n%5==0?104:115;p.setColor(n<7?colors.accent:colors.muted);canvas.drawLine(300+(float)Math.cos(a)*inner,146+(float)Math.sin(a)*inner,300+(float)Math.cos(a)*128,146+(float)Math.sin(a)*128,p);}p.setTextAlign(Paint.Align.CENTER);Typeface display=WidgetDesign.typeface(c,config,true);String value=String.format(Locale.ROOT,"%02d:00",minutes);p.setTypeface(display);p.setTextSize(60);float size=Math.min(60,60*195/Math.max(1,p.measureText(value)));label(canvas,p,value,300,163,size,colors.text,display);label(canvas,p,"PRESET",300,213,18,colors.muted,Typeface.create("monospace",0));return b;}
    private static Bitmap weather(Context c,JSONObject config,JSONObject data,boolean compact){
        int d=WidgetDesign.id(config);boolean forecast=config.optInt("variant",0)==1&&!compact;Bitmap b=bitmap(600,forecast?360:300);Canvas canvas=new Canvas(b);Paint p=new Paint(3);WidgetDesign.Colors colors=WidgetDesign.colors(config,"weather");JSONObject current=data.optJSONObject("current"),daily=data.optJSONObject("daily");Typeface normal=WidgetDesign.typeface(c,config,false),display=WidgetDesign.typeface(c,config,true);String city=config.optString("city","Weather");p.setTextAlign(Paint.Align.LEFT);label(canvas,p,ellipsize(city,p,400),0,31,27,colors.text,normal);label(canvas,p,WeatherData.degrees(current.optDouble("temperature_2m"),config),-3,154,d==1?108:132,colors.text,display);icon(canvas,350,40,155,current.optInt("weather_code",-1),current.optInt("is_day",1)==1,d,colors);
        String condition=WeatherData.condition(current.optInt("weather_code",-1));label(canvas,p,condition,0,200,25,colors.text,normal);
        JSONArray high=daily==null?null:daily.optJSONArray("temperature_2m_max"),low=daily==null?null:daily.optJSONArray("temperature_2m_min"),dates=daily==null?null:daily.optJSONArray("time"),codes=daily==null?null:daily.optJSONArray("weather_code");
        if(high!=null&&low!=null)label(canvas,p,"H:"+WeatherData.degrees(high.optDouble(0),config)+"  L:"+WeatherData.degrees(low.optDouble(0),config),0,239,23,colors.muted,normal);
        if(forecast&&dates!=null&&high!=null&&low!=null){p.setColor(colors.muted);p.setAlpha(55);p.setStrokeWidth(1);canvas.drawLine(0,260,600,260,p);p.setAlpha(255);for(int n=0;n<Math.min(3,dates.length());n++){float x=n*200+10;String day;try{day=LocalDate.parse(dates.optString(n)).format(DateTimeFormatter.ofPattern("EEE"));}catch(Exception e){day="";}label(canvas,p,day,x,296,21,colors.muted,normal);icon(canvas,x+60,274,43,codes==null?-1:codes.optInt(n,-1),true,d,colors);label(canvas,p,WeatherData.degrees(high.optDouble(n),config)+" / "+WeatherData.degrees(low.optDouble(n),config),x,341,24,colors.text,normal);}}
        if(compact){boolean cached=WeatherData.failed(c,config)||System.currentTimeMillis()-data.optLong("fetchedAt")>7200000;label(canvas,p,(cached?"Cached · ":"")+"Open-Meteo",0,282,20,colors.muted,normal);}
        return b;
    }
    private static String ellipsize(String s,Paint p,float width){p.setTextSize(27);if(p.measureText(s)<=width)return s;while(!s.isEmpty()&&p.measureText(s+"…")>width)s=s.substring(0,s.length()-1);return s+"…";}
    private static void icon(Canvas canvas,float x,float y,float size,int code,boolean day,int d,WidgetDesign.Colors colors){
        if(code<0){Paint unknown=new Paint(3);unknown.setColor(colors.muted);unknown.setStrokeWidth(Math.max(1,size/22));unknown.setStrokeCap(Paint.Cap.ROUND);canvas.drawLine(x+size*.3f,y+size*.45f,x+size*.7f,y+size*.45f,unknown);return;}
        Bitmap b=bitmap(180,160);Canvas art=new Canvas(b);Paint p=new Paint(3);boolean rain=code>=51&&code<=67||code>=80,snow=code>=71&&code<=77;int sun=d==1||colors.monochrome?colors.text:0xffffd04a,cloud=d==1||colors.monochrome?colors.text:0xffeaf3ff;
        if(code<=2){p.setColor(sun);art.drawCircle(68,58,34,p);if(!day){p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));art.drawCircle(81,45,31,p);p.setXfermode(null);}else if(d==7||d==4){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);for(int n=0;n<8;n++){double a=n*Math.PI/4;art.drawLine(68+(float)Math.cos(a)*44,58+(float)Math.sin(a)*44,68+(float)Math.cos(a)*51,58+(float)Math.sin(a)*51,p);}p.setStyle(Paint.Style.FILL);}}
        if(code>0){p.setShader(d==1||colors.monochrome?null:new LinearGradient(0,55,0,125,cloud,0xffb6cfe8,Shader.TileMode.CLAMP));p.setColor(cloud);art.drawCircle(92,83,35,p);art.drawCircle(127,99,25,p);art.drawCircle(59,104,23,p);art.drawRoundRect(42,91,147,124,20,20,p);p.setShader(null);}
        if(rain||snow){p.setColor(d==1||colors.monochrome?colors.text:apple(d)?0xffb8dcff:0xff478ee7);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(5);for(int n=0;n<3;n++){if(snow)art.drawCircle(66+n*29,142,4,p);else art.drawLine(70+n*29,134,65+n*29,147,p);}}
        if(d==1){p.setColor(colors.text);for(int row=0;row<160;row+=7)for(int col=0;col<180;col+=7)if(Color.alpha(b.getPixel(col,row))>100)canvas.drawCircle(x+col*size/180,y+row*size/180,size/180*2.3f,p);}
        else canvas.drawBitmap(b,null,new RectF(x,y,x+size,y+size*160/180),p);b.recycle();
    }
}
