package com.nook.widgets;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONObject;

/** Shared visual tokens for dedicated widgets and the Universal toolbox. */
public final class WidgetDesign {
    private WidgetDesign(){}
    public static final String[] NAMES={"Nook · Original","Nothing-inspired · Dot","Apple-inspired · iOS & iPadOS","Apple-inspired · macOS","Pixel-inspired · Material","Paper · Notebook","Terminal · Retro","Samsung-inspired · One UI"};
    public static final String[] DESCRIPTIONS={"Calm colors and soft, familiar cards.","Clean monochrome, dot displays, segmented dials and red controls.","Apple widget layouts in clear Liquid Glass or classic content colors.","Desktop widget layouts with clear, tinted or classic materials.","Sculpted clock faces, large numerals and playful tonal shapes.","Warm paper, serif headings and notebook rules.","Monospaced type, phosphor accents and precise outlines.","Large clear type, green battery circles, weekend colors and rounded Galaxy-style cards."};
    public static final String[] KEYS={"design","tone","paletteCustom","corner","border","font","textScale","padding","align","showIcon","showLabel","accent","glassMode","glassUri","glassBlur","glassRefraction","glassCropX","glassCropY","glassZoom","glassTint"};
    private static final int[] RADII={28,28,32,18,36,12,6,32},FONTS={0,4,0,0,1,3,2,1};
    private static final int[][] LIGHT={
        {0xfff3e9d9,0xff3c372d,0xff7d7567,0xffe2d4bd,0xff556b41},
        {0xfff1f0ec,0xff171717,0xff5b5b57,0xffdfded9,0xffc52d25},
        {0xfff7f7fa,0xff1c1c1e,0xff626269,0xffe7e8ef,0xff006fe6},
        {0xffe5eaf1,0xff243144,0xff5d6c80,0xffd0d9e7,0xff0068bc},
        {0xffe5edc7,0xff273810,0xff586b37,0xffcfdda5,0xff42651b},
        {0xfffbf2df,0xff43362a,0xff806b53,0xffebdec4,0xffa64a32},
        {0xffedf6e9,0xff20391b,0xff526d47,0xffd7e7d0,0xff2e6e24},
        {0xfff7f7fb,0xff17171c,0xff5f6472,0xffe8ecf6,0xff175ad7}
    };
    private static final int[][] DARK={
        {0xff243128,0xffe9f0e4,0xffaebfae,0xff394d3d,0xffc4d7b2},
        {0xff151515,0xfff5f2ec,0xffb2b1ad,0xff2b2b29,0xffff5148},
        {0xff1c1c20,0xfff5f5f9,0xffb4b4c0,0xff34343c,0xff7ab4ff},
        {0xff263342,0xfff0f4fa,0xffbccbdc,0xff3a4b60,0xff8dc6ff},
        {0xff2a351d,0xffe1edc7,0xffb6c698,0xff43552e,0xffc1e88f},
        {0xff332b24,0xfff1e6cf,0xffc4b59b,0xff514334,0xfff4ba89},
        {0xff0c1510,0xffe2f6dc,0xff9abe91,0xff1d3023,0xffa5ef7c},
        {0xff191a20,0xfff3f5fc,0xffb6becf,0xff303542,0xff8bb4ff}
    };
    public static int clamp(int value,int min,int max){return Math.max(min,Math.min(max,value));}
    public static int id(JSONObject config){return clamp(config.optInt("design",0),0,NAMES.length-1);}
    public static int defaultSurface(int design){return design==2||design==3?3:0;}
    public static int font(JSONObject config){int font=clamp(config.optInt("font",0),0,5);return font==0?FONTS[id(config)]:font-1;}
    public static int layout(Context c,String base,JSONObject config){if(ReplicaDesign.enabled(config)&&!base.startsWith("widget_universal"))base=base.equals("widget")?"replica_widget":base.replace("widget_tools","replica_tools").replace("widget_photo","replica_photo");int font=font(config);if(font==0)return c.getResources().getIdentifier(base,"layout",c.getPackageName());return c.getResources().getIdentifier(base+"_font"+font,"layout",c.getPackageName());}
    public static float scale(JSONObject config){return clamp(config.optInt("textScale",100),85,115)/100f;}
    public static int radius(JSONObject config){return clamp(config.optInt("corner",RADII[id(config)]),0,40);}
    public static int padding(JSONObject config,boolean compact){return switch(config.optInt("padding",0)){case 1->10;case 2->16;case 3->22;default->compact?12:id(config)==3?16:18;};}
    public static int alignment(JSONObject config){int align=config.optInt("align",0);return align==2||align==0&&id(config)==4?Gravity.CENTER:Gravity.START|Gravity.CENTER_VERTICAL;}
    public static boolean customSurface(JSONObject config){return GlassMaterial.enabled(config)||id(config)!=0||config.has("corner")||config.has("border")||config.has("tone")||config.has("accent");}
    public static final class Colors {
        public int surface,text,muted,chip,accent;
        public String kind;
        public boolean monochrome;
        Colors(int[] colors){surface=colors[0];text=colors[1];muted=colors[2];chip=colors[3];accent=colors[4];}
    }
    private static int mix(int a,int b,float amount){return Color.rgb(Math.round(Color.red(a)*(1-amount)+Color.red(b)*amount),Math.round(Color.green(a)*(1-amount)+Color.green(b)*amount),Math.round(Color.blue(a)*(1-amount)+Color.blue(b)*amount));}
    private static boolean light(int color){return .2126*Color.red(color)+.7152*Color.green(color)+.0722*Color.blue(color)>155;}
    public static boolean validAccent(String value){return value.isEmpty()||value.matches("#[0-9a-fA-F]{6}");}
    public static Colors colors(JSONObject config,String kind){
        int design=id(config),tone=clamp(config.optInt("tone",0),0,2),style=clamp(config.optInt("style",0),0,3);
        int palette=clamp(config.optInt("palette",Widgets.defaultPalette(kind)),0,5);
        boolean dark=tone==2||style==2||style==3&&tone!=1||tone==0&&(design==6||design==0&&!light(Widgets.BACKGROUNDS[palette]));
        Colors colors=new Colors((dark?DARK:LIGHT)[design]);
        colors.kind=kind.equals("universal")&&!config.optString("page","menu").equals("menu")?config.optString("page"):kind;
        if(!config.optBoolean("paletteCustom",false))ReplicaDesign.colors(colors,design,colors.kind,dark);
        if(design==0&&tone==0){colors.surface=Widgets.BACKGROUNDS[palette];colors.text=Widgets.legacyForeground(config,kind);colors.muted=Widgets.legacySecondary(config,kind);colors.chip=mix(colors.surface,colors.text,.12f);colors.accent=colors.text;if(style==2)colors.surface=mix(colors.surface,Color.BLACK,.6f);}
        else if(config.optBoolean("paletteCustom",false)){
            colors.surface=mix(dark?0xff161b20:0xfffafaf7,Widgets.BACKGROUNDS[palette],.18f);
            colors.chip=mix(colors.surface,dark?Color.WHITE:Color.BLACK,.12f);
            colors.muted=mix(colors.text,colors.surface,.22f);
        }
        GlassMaterial.colors(colors,config);
        String accent=config.optString("accent","");if(!accent.isEmpty()&&validAccent(accent))colors.accent=Color.parseColor(accent);
        return colors;
    }
    public static void select(JSONObject config,int design){
        try{config.put("design",clamp(design,0,NAMES.length-1));resetOverrides(config);}catch(Exception e){throw new IllegalStateException(e);}
    }
    public static void resetOverrides(JSONObject config){
        for(String key:KEYS)if(!key.equals("design")&&!key.equals("glassUri"))config.remove(key);
        config.remove("transparency");try{config.put("style",defaultSurface(id(config)));}catch(Exception e){throw new IllegalStateException(e);}
    }
    static int[] size(Context c,int widgetId,String kind,JSONObject config,boolean compact){
        int width=Math.round(c.getResources().getDisplayMetrics().widthPixels/c.getResources().getDisplayMetrics().density)-48;
        int height=kind.equals("universal")?(compact?280:340):(compact?182:250);
        if(widgetId>0){
            Bundle options=AppWidgetManager.getInstance(c).getAppWidgetOptions(widgetId);boolean landscape=c.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
            width=options.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH:AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,width);
            height=options.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT:AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,height);
        }
        return new int[]{clamp(config.optInt("_width",width),180,520),clamp(config.optInt("_height",height),110,600)};
    }
    private static Bitmap surface(Context c,JSONObject config,Colors colors,int width,int height,boolean chip){
        if(GlassMaterial.enabled(config))return GlassMaterial.render(c,config,width,height,chip);
        float resolution=Math.min(2f,(float)Math.sqrt(500000f/(width*height)));
        Bitmap bitmap=Bitmap.createBitmap(Math.round(width*resolution),Math.round(height*resolution),Bitmap.Config.ARGB_8888);bitmap.setDensity(Bitmap.DENSITY_NONE);
        Canvas canvas=new Canvas(bitmap);canvas.scale(resolution,resolution);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);int design=id(config),style=clamp(config.optInt("style",0),0,3);
        float radius=chip?Math.min(radius(config),design==4?28:16):radius(config);radius=Math.min(radius,Math.min(width,height)/2f);
        boolean search=!chip&&"search".equals(colors.kind)&&!config.has("page");float inset=search?Math.max(1,(height-64)/2f):1;if(search&&!config.has("corner"))radius=32;RectF rect=new RectF(1,inset,width-1,height-inset);Path path=new Path();path.addRoundRect(rect,radius,radius,Path.Direction.CW);canvas.save();canvas.clipPath(path);
        int fill=chip?colors.chip:colors.surface;
        if(chip&&design==4)fill=mix(colors.chip,colors.accent,.15f);
        if(style!=0||!chip&&(design==2||design==3)&&"weather".equals(colors.kind)){paint.setShader(new LinearGradient(0,0,width,height,new int[]{mix(fill,Color.WHITE,style==1?.18f:.09f),fill,mix(fill,Color.BLACK,.045f)},null,Shader.TileMode.CLAMP));}else paint.setColor(fill);
        canvas.drawRect(rect,paint);paint.setShader(null);
        if(!chip){
            if((design==2||design==3)&&"notes".equals(colors.kind)&&(config.optBoolean("showLabel",true)||config.optBoolean("showIcon",false))){paint.setColor(config.has("accent")?colors.accent:0xffffd54e);canvas.drawRect(0,0,width,48,paint);}
            if(design==5){paint.setStrokeWidth(.6f);paint.setColor((colors.accent&0xffffff)|0x22000000);for(int y=54;y<height;y+=24)canvas.drawLine(12,y,width-12,y,paint);canvas.drawLine(22,0,22,height,paint);}
            if(design==6){paint.setColor((colors.accent&0xffffff)|0x12000000);paint.setStrokeWidth(.5f);for(int y=3;y<height;y+=4)canvas.drawLine(0,y,width,y,paint);}
        }
        canvas.restore();int border=config.optInt("border",-1);float stroke=border<0?(style==0?(design==6?1.4f:0):.7f):clamp(border,0,3);
        if(stroke>0){paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke);paint.setColor((colors.accent&0xffffff)|(style!=0?0x66000000:0x70000000));canvas.drawRoundRect(new RectF(rect.left+stroke/2,rect.top+stroke/2,rect.right-stroke/2,rect.bottom-stroke/2),radius,radius,paint);}
        return bitmap;
    }
    public static void surfaces(Context c,RemoteViews view,int widgetId,String kind,JSONObject config,boolean compact,int[] chips){
        if(!customSurface(config))return;Colors colors=colors(config,kind);int[] size=size(c,widgetId,kind,config,compact);
        if(!config.optBoolean("_embedded",false))view.setImageViewBitmap(R.id.widget_surface,surface(c,config,colors,size[0],size[1],false));
        int chipWidth=Math.max(40,(size[0]-padding(config,compact)*2-16)/3);
        for(int n=0;n<chips.length;n++)view.setImageViewBitmap(chips[n],ReplicaDesign.enabled(config)&&!GlassMaterial.enabled(config)?ReplicaDesign.button(c,config,kind,chipWidth,48,n):surface(c,config,colors,chipWidth,48,true));
    }
    public static void universalSurfaces(Context c,RemoteViews view,int id,JSONObject config,boolean compact,int[] tiles){
        if(!customSurface(config))return;int[] size=size(c,id,"universal",config,compact);Colors colors=colors(config,"universal");
        view.setImageViewBitmap(R.id.widget_surface,surface(c,config,colors,size[0],size[1],false));
        int width=Math.max(40,(size[0]-padding(config,compact)*2-16)/3),height=Math.max(32,(size[1]-(compact?60:140)-16)/3);
        Bitmap tile=surface(c,config,colors,width,height,true);for(int image:tiles)view.setImageViewBitmap(image,tile);
        Bitmap nav=surface(c,config,colors,width,compact?32:40,true);for(int image:new int[]{R.id.hub_prev_bg,R.id.hub_count_bg,R.id.hub_next_bg})view.setImageViewBitmap(image,nav);
        view.setImageViewBitmap(R.id.hub_menu_bg,surface(c,config,colors,96,compact?32:40,true));
    }
    private static void text(RemoteViews view,int id,float size,JSONObject config){view.setTextViewTextSize(id,TypedValue.COMPLEX_UNIT_SP,size*scale(config));}
    public static Typeface typeface(Context c,JSONObject config,boolean display){int font=font(config);return font==4&&display?c.getResources().getFont(R.font.nook_dots):Typeface.create(font==2||font==4?"monospace":font==3&&display?"serif":font==1?"sans-serif-medium":display?"sans-serif-light":"sans-serif-medium",0);}
    public static void dedicated(Context c,RemoteViews view,int widgetId,String kind,JSONObject config,boolean compact){
        int design=id(config);Colors colors=colors(config,kind);boolean styled=customSurface(config);int gravity=alignment(config);
        if(styled){
            for(int id:new int[]{R.id.widget_title,R.id.widget_time,R.id.widget_row0,R.id.widget_row1,R.id.widget_row2,R.id.widget_settings})view.setTextColor(id,colors.text);
            for(int id:new int[]{R.id.widget_body,R.id.widget_meta,R.id.widget_date})view.setTextColor(id,colors.muted);
            view.setTextColor(R.id.widget_label,colors.accent);view.setInt(R.id.widget_icon,"setColorFilter",colors.accent);
            for(int id:new int[]{R.id.widget_action0,R.id.widget_action1,R.id.widget_action2})view.setTextColor(id,design==2||design==3||design==6?colors.accent:colors.text);
        }
        for(int id:new int[]{R.id.widget_title,R.id.widget_body,R.id.widget_meta,R.id.widget_time,R.id.widget_date,R.id.widget_row0,R.id.widget_row1,R.id.widget_row2})view.setInt(id,"setGravity",gravity);
        view.setViewVisibility(R.id.widget_icon,config.optBoolean("showIcon",true)?View.VISIBLE:View.GONE);
        view.setViewVisibility(R.id.widget_label,config.optBoolean("showLabel",true)?View.VISIBLE:View.INVISIBLE);
        if(design!=0){String label=config.optString("title","").trim();if(label.isEmpty())label=Widgets.title(kind);view.setTextViewText(R.id.widget_label,design==1||design==6?label.toUpperCase(java.util.Locale.ROOT):label);view.setFloat(R.id.widget_label,"setLetterSpacing",design==1||design==6?.1f:0);}
        int padding=padding(config,compact);view.setViewPadding(R.id.widget_root,Ui.dp(c,padding),Ui.dp(c,compact?8:14),Ui.dp(c,padding),Ui.dp(c,compact?8:14));
        text(view,R.id.widget_title,compact?17:design==2?24:design==4?25:design==5?24:22,config);
        text(view,R.id.widget_body,14,config);text(view,R.id.widget_meta,11,config);
        float clockSize=(compact?36:design==2?56:design==4?58:52)*scale(config);
        if(kind.equals("clock")){
            Paint measure=new Paint();measure.setTypeface(typeface(c,config,true));measure.setTextSize(clockSize);
            String period="PM";for(String value:java.text.DateFormatSymbols.getInstance().getAmPmStrings())if(value.length()>period.length())period=value;
            String longest=config.optInt("variant",0)==1?"23:58":"12:58 "+period;int width=size(c,widgetId,kind,config,compact)[0]-padding*2;
            clockSize=Math.min(clockSize,width*clockSize/Math.max(1,measure.measureText(longest)*c.getResources().getConfiguration().fontScale));
            if(config.optInt("align",0)==0&&(design==1||design==4)){view.setInt(R.id.widget_time,"setGravity",Gravity.CENTER);view.setInt(R.id.widget_date,"setGravity",Gravity.CENTER);}
        }
        view.setTextViewTextSize(R.id.widget_time,TypedValue.COMPLEX_UNIT_SP,clockSize);view.setInt(R.id.widget_time,"setMaxLines",1);text(view,R.id.widget_date,14,config);
        for(int id:new int[]{R.id.widget_row0,R.id.widget_row1,R.id.widget_row2})text(view,id,14,config);
        String[] labels=null;
        if(kind.equals("notes")){
            Store.Item note=NoteLife.selected(c,config);
            labels=config.optInt("variant",0)==1||note==null?new String[]{"Temporary","Permanent","Library"}:note.archived?new String[]{"Restore","Temporary","Permanent"}:note.expiresAt>0?new String[]{"+1 hour","Keep forever","Archive"}:new String[]{"Edit","Archive","+ New"};
        }else if(kind.equals("timers")){int[] values=Widgets.shortcuts(config,true,config.optInt("variant",0));labels=new String[]{values[0]+" min",values[1]+" min",values[2]+" min"};}
        else if(kind.equals("alarms"))labels=new String[]{"07:00","07:30","08:00"};
        else if(kind.equals("habits"))labels=new String[]{"−","999 / 999","+"};
        else if(kind.equals("search"))labels=new String[]{"Search","Voice","Options"};
        else if(kind.equals("music"))labels=new String[]{"‹ Prev","Pause","Next ›"};
        else if(kind.equals("contacts"))labels=new String[]{LibraryWidgets.contactLabel(config,0),LibraryWidgets.contactLabel(config,1),LibraryWidgets.contactLabel(config,2)};
        else if(kind.equals("controls")){labels=new String[3];for(int n=0;n<3;n++)labels[n]=LibraryWidgets.TOOL_NAMES[clamp(config.optInt("control"+n,config.optInt("variant",0)==0?n:new int[]{6,7,5}[n]),0,7)];}
        int[] actions={R.id.widget_action0,R.id.widget_action1,R.id.widget_action2};float buttonSize=14*scale(config);
        if(labels!=null){Paint measure=new Paint();measure.setTypeface(typeface(c,config,false));measure.setTextSize(buttonSize);float available=(size(c,widgetId,kind,config,compact)[0]-padding*2-16)/3f-4;for(String label:labels)buttonSize=Math.min(buttonSize,14*scale(config)*available/Math.max(1,measure.measureText(label)*c.getResources().getConfiguration().fontScale));}
        for(int action:actions)view.setTextViewTextSize(action,TypedValue.COMPLEX_UNIT_SP,Math.max(8,buttonSize*(ReplicaDesign.enabled(config)?.9f:1f)));
        if(ReplicaDesign.enabled(config))ReplicaDesign.apply(c,view,widgetId,kind,config,compact);
    }
    public static void universal(RemoteViews view,JSONObject config,boolean compact){
        text(view,R.id.hub_heading,20,config);view.setViewVisibility(R.id.hub_label,config.optBoolean("showLabel",true)?View.VISIBLE:View.INVISIBLE);
        Colors colors=colors(config,"universal");view.setTextColor(R.id.hub_menu,colors.accent);
    }
}
