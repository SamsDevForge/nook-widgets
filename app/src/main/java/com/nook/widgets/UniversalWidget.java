package com.nook.widgets;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONObject;
import java.util.*;

/** Native switching within a single launcher widget; tool actions are reused. */
public final class UniversalWidget {
    private UniversalWidget(){}
    public static final int TOOLS_PER_PAGE=6;
    public static int menuPages(){return (Widgets.KINDS.length+TOOLS_PER_PAGE-1)/TOOLS_PER_PAGE;}
    private static final int[] TILES={R.id.hub_tile0,R.id.hub_tile1,R.id.hub_tile2,R.id.hub_tile3,R.id.hub_tile4,R.id.hub_tile5,R.id.hub_tile6,R.id.hub_tile7,R.id.hub_tile8};
    private static final int[] TILE_TEXT={R.id.hub_text0,R.id.hub_text1,R.id.hub_text2,R.id.hub_text3,R.id.hub_text4,R.id.hub_text5,R.id.hub_text6,R.id.hub_text7,R.id.hub_text8};
    private static final int[] TILE_ICONS={R.id.hub_icon0,R.id.hub_icon1,R.id.hub_icon2,R.id.hub_icon3,R.id.hub_icon4,R.id.hub_icon5,R.id.hub_icon6,R.id.hub_icon7,R.id.hub_icon8};
    private static final int[] BACKGROUNDS={R.id.hub_bg0,R.id.hub_bg1,R.id.hub_bg2,R.id.hub_bg3,R.id.hub_bg4,R.id.hub_bg5,R.id.hub_bg6,R.id.hub_bg7,R.id.hub_bg8,R.id.hub_menu_bg,R.id.hub_prev_bg,R.id.hub_count_bg,R.id.hub_next_bg};
    public static boolean isBackground(int id){for(int background:BACKGROUNDS)if(id==background)return true;return false;}
    public static boolean validPage(String page){return "menu".equals(page)||Arrays.asList(Widgets.KINDS).contains(page);}
    public static String currentPage(JSONObject config){String page=config.optString("page","menu");return validPage(page)?page:"menu";}
    public static JSONObject pageSettings(JSONObject config,String page){
        if(!Arrays.asList(Widgets.KINDS).contains(page))throw new IllegalArgumentException("Unknown widget view");
        try{
            JSONObject pages=config.optJSONObject("pages");if(pages==null){pages=new JSONObject();config.put("pages",pages);}
            JSONObject settings=pages.optJSONObject(page);
            if(settings==null){settings=new JSONObject();if(page.equals("notes"))settings.put("entity",Widgets.NEW_NOTE);pages.put(page,settings);}
            return settings;
        }catch(org.json.JSONException e){throw new IllegalStateException(e);}
    }
    public static JSONObject viewConfig(JSONObject config,String page){
        JSONObject result=Widgets.defaults(page);
        try{
            JSONObject pages=config.optJSONObject("pages"),settings=pages==null?null:pages.optJSONObject(page);
            if(settings!=null)for(Iterator<String> keys=settings.keys();keys.hasNext();){String key=keys.next();result.put(key,settings.get(key));}
            JSONObject appearance=Widgets.defaults("universal");
            for(String key:new String[]{"palette","style","density","title"})result.put(key,config.has(key)?config.get(key):appearance.get(key));
            if(config.has("transparency"))result.put("transparency",config.get("transparency"));
            for(String key:WidgetDesign.KEYS)if(config.has(key))result.put(key,config.get(key));
            for(String key:new String[]{"_width","_height"})if(config.has(key))result.put(key,config.get(key));
            result.put("_embedded",true);
        }catch(org.json.JSONException e){throw new IllegalStateException(e);}
        return result;
    }
    public static boolean switchPage(Context c,int widgetId,String page){
        if(!validPage(page)||widgetId<=0)return false;
        AppWidgetProviderInfo info=AppWidgetManager.getInstance(c).getAppWidgetInfo(widgetId);
        if(info==null||!info.provider.equals(new ComponentName(c,Widgets.Universal.class)))return false;
        JSONObject config=Store.config(c,widgetId);
        try{if(page.equals("menu")&&!currentPage(config).equals("menu"))config.put("menuPage",Widgets.index(currentPage(config))/TOOLS_PER_PAGE);config.put("page",page);}catch(org.json.JSONException e){throw new IllegalStateException(e);}
        Store.config(c,widgetId,config);Widgets.update(c,widgetId,"universal");return true;
    }
    public static boolean switchMenu(Context c,int id,int menu){
        AppWidgetProviderInfo info=AppWidgetManager.getInstance(c).getAppWidgetInfo(id);if(info==null||!info.provider.equals(new ComponentName(c,Widgets.Universal.class)))return false;
        JSONObject config=Store.config(c,id);try{config.put("page","menu").put("menuPage",Math.floorMod(menu,menuPages()));}catch(Exception e){throw new IllegalStateException(e);}Store.config(c,id,config);Widgets.update(c,id,"universal");return true;
    }
    public static boolean attachNote(Context c,int widgetId,String noteId){
        AppWidgetProviderInfo info=AppWidgetManager.getInstance(c).getAppWidgetInfo(widgetId);if(info==null)return false;
        boolean universal=info.provider.equals(new ComponentName(c,Widgets.Universal.class));
        if(!universal&&!info.provider.equals(new ComponentName(c,Widgets.Notes.class)))return false;
        JSONObject config=Store.config(c,widgetId);
        try{if(universal){pageSettings(config,"notes").put("entity",noteId);config.put("page","notes");}else config.put("entity",noteId);}
        catch(org.json.JSONException e){throw new IllegalStateException(e);}
        Store.config(c,widgetId,config);Widgets.update(c,widgetId,universal?"universal":"notes");return true;
    }
    private static PendingIntent switchIntent(Context c,int widgetId,String page){
        Intent i=new Intent(c,WidgetActionReceiver.class).setAction("universal_switch").putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId).putExtra("page",page)
            .setData(Uri.parse("nook://switch/"+widgetId+"/"+page));
        return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private static PendingIntent configure(Context c,int id){return PendingIntent.getActivity(c,0,new Intent(c,WidgetConfigActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).putExtra("kind","universal").setData(Uri.parse("nook://configure/"+id)),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private static PendingIntent menuIntent(Context c,int id,int page){int target=Math.floorMod(page,menuPages());return PendingIntent.getBroadcast(c,0,new Intent(c,WidgetActionReceiver.class).setAction("universal_menu").putExtra("widget",id).putExtra("menu",target).setData(Uri.parse("nook://menu/"+id+"/"+target)),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    public static RemoteViews render(Context c,int id,JSONObject config,boolean small,boolean preview){
        boolean compact=small||config.optInt("density",0)==1;
        RemoteViews view=new RemoteViews(c.getPackageName(),WidgetDesign.layout(c,compact?"widget_universal_compact":"widget_universal",config));
        int text=Widgets.foreground(config,"universal"),muted=Widgets.secondary(config,"universal"),chip=Widgets.chipResource(config,"universal"),alpha=Widgets.surfaceAlpha(config);
        String page=currentPage(config);boolean menu=page.equals("menu");
        view.setImageViewResource(R.id.widget_surface,Widgets.surfaceResource(config,"universal"));view.setInt(R.id.widget_surface,"setImageAlpha",alpha);
        for(int background:BACKGROUNDS){view.setImageViewResource(background,chip);view.setInt(background,"setImageAlpha",alpha);}
        WidgetDesign.universalSurfaces(c,view,id,config,compact,new int[]{R.id.hub_bg0,R.id.hub_bg1,R.id.hub_bg2,R.id.hub_bg3,R.id.hub_bg4,R.id.hub_bg5,R.id.hub_bg6,R.id.hub_bg7,R.id.hub_bg8});
        int padding=Math.min(WidgetDesign.padding(config,compact),compact?16:24);
        view.setViewPadding(R.id.universal_root,Ui.dp(c,padding),Ui.dp(c,compact?6:14),Ui.dp(c,padding),Ui.dp(c,compact?6:14));
        for(int textId:new int[]{R.id.hub_menu,R.id.hub_label,R.id.hub_settings,R.id.hub_heading,R.id.hub_prev,R.id.hub_count,R.id.hub_next})view.setTextColor(textId,text);
        view.setTextColor(R.id.hub_description,muted);
        String label=menu?"Nook toolbox":Widgets.shortTitle(page);String custom=config.optString("title","").trim();
        view.setTextViewText(R.id.hub_label,custom.isEmpty()?label:menu?custom:custom+" · "+label);
        view.setViewVisibility(R.id.hub_grid,menu?View.VISIBLE:View.GONE);view.setViewVisibility(R.id.hub_content,menu?View.GONE:View.VISIBLE);view.setViewVisibility(R.id.hub_nav,View.VISIBLE);
        view.setViewVisibility(R.id.hub_heading,View.GONE);view.setViewVisibility(R.id.hub_description,View.GONE);view.setViewVisibility(R.id.hub_row2,compact?View.GONE:View.VISIBLE);
        if(!preview){view.setOnClickPendingIntent(R.id.hub_menu,switchIntent(c,id,"menu"));view.setOnClickPendingIntent(R.id.hub_settings,configure(c,id));}
        int menuPage=Math.floorMod(config.optInt("menuPage",0),menuPages());
        for(int n=0;n<TILES.length;n++){
            int tool=menuPage*TOOLS_PER_PAGE+n;boolean isTool=n<TOOLS_PER_PAGE&&tool<Widgets.KINDS.length;
            String name=isTool?Widgets.shortTitle(Widgets.KINDS[tool]):n==6?"Library":n==7?"Customize":"More tools";
            view.setTextViewText(TILE_TEXT[n],name);view.setTextColor(TILE_TEXT[n],text);
            view.setImageViewResource(TILE_ICONS[n],isTool?Widgets.iconResource(Widgets.KINDS[tool]):n==6?R.drawable.ic_notes:n==7?R.drawable.ic_controls:R.drawable.ic_universal);view.setInt(TILE_ICONS[n],"setColorFilter",text);
            if(WidgetDesign.customSurface(config))view.setInt(TILE_ICONS[n],"setColorFilter",WidgetDesign.colors(config,"universal").accent);
            view.setViewVisibility(TILE_ICONS[n],config.optBoolean("showIcon",true)?View.VISIBLE:View.GONE);
            view.setContentDescription(TILES[n],isTool?"Show "+name+" in this widget":name);
            if(!preview){PendingIntent action=isTool?switchIntent(c,id,Widgets.KINDS[tool]):n==6?PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class).putExtra("section","notes").setData(Uri.parse("nook://hub-library/"+id)),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE):n==7?configure(c,id):menuIntent(c,id,menuPage+1);view.setOnClickPendingIntent(TILES[n],action);}
        }
        if(menu){view.setTextViewText(R.id.hub_count,(menuPage+1)+" / "+menuPages());view.setContentDescription(R.id.hub_count,"Tool page "+(menuPage+1)+" of "+menuPages());if(!preview){view.setOnClickPendingIntent(R.id.hub_prev,menuIntent(c,id,menuPage-1));view.setOnClickPendingIntent(R.id.hub_next,menuIntent(c,id,menuPage+1));view.setOnClickPendingIntent(R.id.hub_count,menuIntent(c,id,0));}}
        view.removeAllViews(R.id.hub_content);
        if(!menu){
            JSONObject options=viewConfig(config,page);RemoteViews content=Widgets.render(c,id,page,options,compact,preview);
            content.setViewVisibility(R.id.widget_header,View.GONE);content.setViewVisibility(R.id.widget_surface,View.GONE);content.setViewPadding(R.id.widget_root,0,0,0,0);
            content.setInt(R.id.widget_root,"setGravity",android.view.Gravity.CENTER_VERTICAL);
            content.setInt(R.id.widget_title,"setMaxLines",1);if(Widgets.index(page)<7)content.setTextViewTextSize(R.id.widget_title,android.util.TypedValue.COMPLEX_UNIT_SP,(compact?17:20)*WidgetDesign.scale(config));
            if(compact&&page.equals("notes")&&options.optInt("variant",0)==0){
                Store.Item note=NoteLife.selected(c,options);
                if(note==null||!NoteLife.active(c,note)){
                    content.setViewVisibility(R.id.widget_title,View.GONE);content.setViewVisibility(R.id.widget_body,View.GONE);content.setViewVisibility(R.id.widget_meta,View.GONE);content.setViewVisibility(R.id.widget_actions,View.VISIBLE);
                    String state=note==null?"New":note.archived?"Saved":"Expired";view.setTextViewText(R.id.hub_label,(custom.isEmpty()?"Notes":custom+" · Notes")+" · "+state);
                }
            }
            view.addView(R.id.hub_content,content);
            int index=Widgets.index(page);view.setTextViewText(R.id.hub_count,(index+1)+" / "+Widgets.KINDS.length);view.setContentDescription(R.id.hub_count,"All tools, currently "+label);
            if(!preview){view.setOnClickPendingIntent(R.id.hub_prev,switchIntent(c,id,Widgets.KINDS[(index+Widgets.KINDS.length-1)%Widgets.KINDS.length]));view.setOnClickPendingIntent(R.id.hub_next,switchIntent(c,id,Widgets.KINDS[(index+1)%Widgets.KINDS.length]));view.setOnClickPendingIntent(R.id.hub_count,switchIntent(c,id,"menu"));}
        }
        WidgetDesign.universal(view,config,compact);if(page.equals("video")&&viewConfig(config,page).optBoolean("videoHideText",false)||page.equals("music")&&viewConfig(config,page).optBoolean("musicHideText",false)){view.setViewVisibility(R.id.hub_label,View.GONE);view.setTextViewText(R.id.hub_count,"");}return view;
    }
}
