package com.nook.widgets;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.text.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

public final class MediaOptions {
    private MediaOptions(){}
    private static void choice(Activity a,LinearLayout panel,String[] names,int selected,IntConsumer change){Spinner spinner=new Spinner(a);spinner.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,names));spinner.setSelection(WidgetDesign.clamp(selected,0,names.length-1));panel.addView(spinner);spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int n,long id){if(v instanceof TextView t)t.setTextColor(Ui.TEXT);change.accept(n);}});}
    public static void number(Activity a,LinearLayout panel,JSONObject config,String key,String label,int fallback,int max,Runnable refresh){panel.addView(Ui.text(a,label,12,Ui.MUTED));EditText field=new EditText(a);field.setTextColor(Ui.TEXT);field.setSingleLine(true);field.setInputType(InputType.TYPE_CLASS_NUMBER);field.setText(String.valueOf(config.optInt(key,fallback)));field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});panel.addView(field);field.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int x,int y,int z){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int x,int y,int z){try{ExtraData.put(config,key,WidgetDesign.clamp(Integer.parseInt(s.toString()),key.equals("clipStart")?0:1,max));refresh.run();}catch(NumberFormatException ignored){}}});}
    private static Intent picker(String mime,boolean multiple){return new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(mime).putExtra(Intent.EXTRA_ALLOW_MULTIPLE,multiple).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);}
    public static void add(Activity a,LinearLayout panel,String kind,JSONObject config,Runnable refresh,BiConsumer<Integer,Intent> pick){
        Ui.gap(panel,14);
        if(kind.equals("animation")){panel.addView(Ui.text(a,"Original looping scenes animate directly inside the home screen widget. Choose a style above and a pace below.",13,Ui.MUTED));choice(a,panel,new String[]{"Lively","Gentle","Slow"},config.optInt("motionSpeed",1),n->{ExtraData.put(config,"motionSpeed",n);refresh.run();});}
        else if(kind.equals("slideshow")){panel.addView(Ui.text(a,"Choose up to six photos. The slideshow changes inside the widget, with a soft fade or direct cut. Pause it whenever you want a still frame.",13,Ui.MUTED));Ui.gap(panel,10);panel.addView(Ui.button(a,"Choose slideshow photos",Ui.SURFACE,Ui.ACCENT,()->pick.accept(71,picker("image/*",true))));panel.addView(Ui.text(a,MediaData.photos(config).size()+" photos selected",12,Ui.MUTED));number(a,panel,config,"slideSeconds","Seconds per photo · 2–60",5,60,refresh);choice(a,panel,new String[]{"Fill the frame","Fit the whole photo"},config.optBoolean("slideCrop",true)?0:1,n->{ExtraData.put(config,"slideCrop",n==0);refresh.run();});panel.addView(Ui.button(a,"Clear photos",Ui.SURFACE,Ui.MUTED,()->{config.remove("slideUris");ExtraData.put(config,"slideIndex",0);a.recreate();}));}
        else VideoOptions.add(a,panel,config,refresh,pick);
        if(!kind.equals("video")){Switch pause=new Switch(a);pause.setText("Pause motion");pause.setTextColor(Ui.TEXT);pause.setChecked(config.optBoolean("motionPaused",false));panel.addView(pause);pause.setOnCheckedChangeListener((v,on)->{ExtraData.put(config,"motionPaused",on);refresh.run();});panel.addView(Ui.text(a,"Motion runs in the launcher's native view while visible. Launcher behavior, battery modes and accessibility settings can affect animation.",12,Ui.MUTED));}
    }
    public static boolean picked(Activity a,JSONObject config,int request,int result,Intent data){
        if(request==70||request==72)return VideoOptions.picked(a,config,request,result,data);
        if(result!=Activity.RESULT_OK||data==null)return false;LinkedHashSet<String> values=new LinkedHashSet<>();if(data.getData()!=null)values.add(data.getData().toString());if(data.getClipData()!=null)for(int n=0;n<Math.min(6,data.getClipData().getItemCount());n++)values.add(data.getClipData().getItemAt(n).getUri().toString());JSONArray selected=new JSONArray();for(String value:values){if(!MediaData.document(value))continue;try{a.getContentResolver().takePersistableUriPermission(Uri.parse(value),Intent.FLAG_GRANT_READ_URI_PERMISSION);selected.put(value);}catch(SecurityException ignored){}}
        if(selected.length()==0){Toast.makeText(a,"Choose a file provider that allows lasting read access.",Toast.LENGTH_LONG).show();return false;}ExtraData.put(config,"slideUris",selected);ExtraData.put(config,"slideIndex",0);return true;
    }
}
