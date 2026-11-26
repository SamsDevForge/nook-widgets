package com.nook.widgets;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.function.*;

/** Local video selection and presentation controls shared by dedicated and Universal widgets. */
public final class VideoOptions {
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
    private VideoOptions(){}
    private static Intent picker(){return new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("video/*").putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);}
    private static void toggle(Activity a,LinearLayout panel,JSONObject config,String key,String label,boolean fallback,Runnable refresh){Switch control=new Switch(a);control.setText(label);control.setTextColor(Ui.TEXT);control.setChecked(config.optBoolean(key,fallback));panel.addView(control);control.setOnCheckedChangeListener((v,on)->{ExtraData.put(config,key,on);refresh.run();});}
    private static void choice(Activity a,LinearLayout panel,JSONObject config,String key,String label,String[] names,Runnable refresh){panel.addView(Ui.text(a,label,12,Ui.MUTED));Spinner control=new Spinner(a);control.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,names));control.setSelection(WidgetDesign.clamp(config.optInt(key,0),0,names.length-1));panel.addView(control);control.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> parent){}public void onItemSelected(AdapterView<?> parent,View view,int position,long id){if(view instanceof TextView text)text.setTextColor(Ui.TEXT);if(config.optInt(key,0)!=position){ExtraData.put(config,key,position);refresh.run();}}});}
    public static void add(Activity a,LinearLayout panel,JSONObject config,Runnable refresh,BiConsumer<Integer,Intent> pick){
        Ui.gap(panel,14);panel.addView(Ui.text(a,"Full-length video plays silently inside the widget, using a lightweight stream of frames. Previous and Next switch between your videos without opening a player.",13,Ui.MUTED));Ui.gap(panel,10);
        panel.addView(Ui.button(a,"Choose videos · up to 12",Ui.SURFACE,Ui.ACCENT,()->pick.accept(70,picker())));
        List<String> videos=MediaData.videos(config);if(!videos.isEmpty())panel.addView(Ui.button(a,"Add more videos",Ui.SURFACE,Ui.ACCENT,()->pick.accept(72,picker())));
        panel.addView(Ui.text(a,videos.size()+" videos selected",12,Ui.MUTED));
        for(int n=0;n<videos.size();n++){int index=n;JSONObject current=MediaData.at(config,n);String name=current.optString("videoName",ExtraData.fileName(a,videos.get(n)));panel.addView(Ui.button(a,(n==MediaData.videoIndex(config)?"● ":"")+(n+1)+" · "+name,Ui.SURFACE,Ui.TEXT,()->new AlertDialog.Builder(a).setTitle(name).setItems(new String[]{"Show this video first","Remove from carousel"},(dialog,item)->{if(item==0)ExtraData.put(config,"videoIndex",index);else{List<String> kept=MediaData.videos(config);kept.remove(index);setVideos(a,config,kept);ExtraData.put(config,"prepareVideo",true);}a.recreate();}).show()));}
        toggle(a,panel,config,"videoHideText","Hide all text · icons only",false,refresh);
        toggle(a,panel,config,"videoBackground","Show styled background",false,refresh);
        toggle(a,panel,config,"videoCrop","Fill the widget · crop edges",false,refresh);
        toggle(a,panel,config,"videoPaused","Start paused",false,refresh);
        choice(a,panel,config,"videoShape","Initial widget shape",new String[]{"Match the selected video","Landscape","Square","Portrait"},refresh);
        if(!config.has("videoPace"))ExtraData.put(config,"videoPace",1);
        choice(a,panel,config,"videoPace","Playback smoothness",new String[]{"Battery saver · 3 frames / second","Balanced · 5 frames / second","Smoother · 8 frames / second"},refresh);
        panel.addView(Ui.text(a,"Match video chooses a portrait, square or landscape widget when adding it. Your launcher controls the outer grid size; long-press to resize it later. Mixed video shapes fit inside that space. Playback is silent and lower resolution; speed depends on the phone and launcher.",12,Ui.MUTED));
        {MediaOptions.number(a,panel,config,"clipStart","Short loop only · start in seconds",0,86400,refresh);MediaOptions.number(a,panel,config,"clipLength","Short loop length · 1–6 seconds",3,6,refresh);}
        TextView status=Ui.text(a,videos.isEmpty()?"Select videos to begin.":"Local videos stay on your phone.",12,Ui.MUTED);panel.addView(status);ProgressBar progress=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);progress.setVisibility(View.GONE);panel.addView(progress);boolean[] busy={false};TextView[] button={null};
        Runnable prepare=()->{if(busy[0]||MediaData.videos(config).isEmpty())return;busy[0]=true;button[0].setEnabled(false);progress.setVisibility(View.VISIBLE);status.setText("Reading video sizes and preparing posters…");JSONObject snapshot;try{snapshot=new JSONObject(config.toString());}catch(Exception e){throw new IllegalStateException(e);}Context app=a.getApplicationContext();WORK.execute(()->{int failed=0;List<String> selected=MediaData.videos(snapshot);for(int n=0;n<selected.size();n++){int index=n;try{MediaData.prepare(app,MediaData.at(snapshot,n),percent->a.runOnUiThread(()->{if(!a.isDestroyed())progress.setProgress((index*100+percent)/selected.size());}));}catch(Exception e){failed++;}}int errors=failed;a.runOnUiThread(()->{if(a.isFinishing()||a.isDestroyed())return;busy[0]=false;button[0].setEnabled(true);progress.setVisibility(View.GONE);status.setText(errors==0?"Ready · full-length silent playback":"Could not read "+errors+" video(s). Check file access and format.");refresh.run();});});};
        button[0]=Ui.button(a,"Refresh video sizes & posters",Ui.SURFACE,Ui.ACCENT,prepare);panel.addView(button[0]);
        int placed=a.getIntent().getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,-1);if(placed>0&&!videos.isEmpty()){panel.addView(Ui.button(a,"Open saved video in full-screen player",Ui.SURFACE,Ui.MUTED,()->a.startActivity(new Intent(a,VideoPlayerActivity.class).putExtra("widget",placed))));}
        if(config.optBoolean("prepareVideo",false)||!videos.isEmpty()&&!MediaData.cached(a,config).has("width")){config.remove("prepareVideo");panel.post(prepare);}
    }
    public static void setVideos(Context c,JSONObject config,List<String> values){JSONArray uris=new JSONArray(),names=new JSONArray();for(String value:values){uris.put(value);names.put(ExtraData.fileName(c,value));}ExtraData.put(config,"videoUris",uris);ExtraData.put(config,"videoNames",names);ExtraData.put(config,"videoIndex",0);config.remove("videoUri");config.remove("videoName");}
    public static boolean picked(Activity a,JSONObject config,int request,int result,Intent data){if(result!=Activity.RESULT_OK||data==null)return false;LinkedHashSet<String> selected=new LinkedHashSet<>();if(request==72)selected.addAll(MediaData.videos(config));List<Uri> incoming=new ArrayList<>();if(data.getData()!=null)incoming.add(data.getData());if(data.getClipData()!=null)for(int n=0;n<data.getClipData().getItemCount();n++)incoming.add(data.getClipData().getItemAt(n).getUri());int accepted=0;for(Uri uri:incoming){if(selected.size()>=12)break;if(!MediaData.document(uri.toString()))continue;try{a.getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);selected.add(uri.toString());accepted++;}catch(SecurityException ignored){}}if(accepted==0){Toast.makeText(a,selected.size()>=12?"The carousel holds up to 12 videos.":"Choose a file provider that allows lasting read access.",Toast.LENGTH_LONG).show();return false;}setVideos(a,config,new ArrayList<>(selected));ExtraData.put(config,"prepareVideo",true);return true;}
}
