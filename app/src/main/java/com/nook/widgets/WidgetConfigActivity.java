package com.nook.widgets;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.time.LocalTime;
import java.util.*;

public class WidgetConfigActivity extends Activity {
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private String kind,contentKind;
    private JSONObject config;
    private LinearLayout page, preview;
    private EditText label;
    private TextView surfaceDescription;
    private LinearLayout transparencyControls,glassControls;
    private TextView transparencyValue;
    private SeekBar transparencySlider;
    private boolean syncingTransparency;
    private final List<TextView> surfaceButtons = new ArrayList<>();
    private final List<String> entityIds = new ArrayList<>();
    private final List<TextView> paletteDots = new ArrayList<>();
    private boolean ready;
    private AlertDialog appearance;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        setResult(RESULT_CANCELED);
        widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
        AppWidgetProviderInfo info=widgetId<=0?null:AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId);
        kind=info==null?getIntent().getStringExtra("kind"):Widgets.kindOf(info.provider);
        if(kind==null) { finish(); return; }
        config=saved==null?(widgetId<=0?Widgets.defaults(kind):Store.config(this,widgetId)):json(saved.getString("config","{}"));
        if(saved==null&&config.length()==0)config=Widgets.defaults(kind);
        if(saved==null && widgetId<=0)put("style",Math.max(0,Math.min(3,getIntent().getIntExtra("style",0))));
        if(saved==null&&widgetId<=0&&getIntent().hasExtra("design"))WidgetDesign.select(config,getIntent().getIntExtra("design",0));
        if(saved==null&&widgetId<=0&&getIntent().hasExtra("preset")){StockCatalog.Preset preset=StockCatalog.find(getIntent().getStringExtra("preset"));if(preset!=null){config=StockCatalog.configuration(preset);if(getIntent().hasExtra("surface"))put("style",getIntent().getIntExtra("surface",3));}}
        contentKind=kind.equals("universal")?UniversalWidget.currentPage(config):kind;
        LinearLayout root=Ui.column(this); root.setBackgroundColor(Ui.BG); setContentView(root); Ui.insets(this,root);
        LinearLayout header=Ui.row(this); Ui.pad(header,16,10);
        header.addView(Ui.iconButton(this,"back","Go back",this::finish));
        TextView title=Ui.bold(this,"Make it yours",22,Ui.TEXT); title.setPadding(Ui.dp(this,14),0,0,0); Ui.weight(header,title);
        root.addView(header);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        page=Ui.column(this); Ui.pad(page,24,16); scroll.addView(page);
        page.addView(Ui.text(this,Widgets.title(kind),34,Ui.TEXT)); Ui.gap(page,8);
        page.addView(Ui.text(this,"A little widget. Entirely your style.",15,Ui.MUTED)); Ui.gap(page,24);
        section("LIVE PREVIEW");
        preview=Ui.column(this); preview.setLayoutParams(new LinearLayout.LayoutParams(-1,Ui.dp(this,250))); page.addView(preview); Ui.gap(page,26);
        section("DESIGN LANGUAGE");
        spinner(WidgetDesign.NAMES,WidgetDesign.id(config),n->{if(n!=WidgetDesign.id(config)){WidgetDesign.select(config,n);recreate();}});
        Ui.gap(page,10);page.addView(Ui.text(this,WidgetDesign.DESCRIPTIONS[WidgetDesign.id(config)],12,Ui.MUTED));Ui.gap(page,12);
        page.addView(Ui.button(this,"Fine-tune appearance",Ui.SURFACE,Ui.ACCENT,()->appearance=AppearanceEditor.show(this,kind,config,this::refreshPreview)));Ui.gap(page,24);
        section("SURFACE");
        LinearLayout surfaces=Ui.column(this);
        LinearLayout surfaceRow=null;
        for(int n=0;n<Widgets.STYLES.length;n++) {
            final int selected=n;
            if(n%2==0){surfaceRow=Ui.row(this);surfaces.addView(surfaceRow);Ui.gap(surfaces,8);}
            TextView button=Ui.button(this,Widgets.STYLES[n],Ui.SURFACE,Ui.TEXT,()->{put("style",selected);refreshPreview();});
            surfaceButtons.add(button);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1);if(n%2==0)params.rightMargin=Ui.dp(this,8);surfaceRow.addView(button,params);
        }
        page.addView(surfaces);Ui.gap(page,10);
        surfaceDescription=Ui.text(this,"",12,Ui.MUTED);page.addView(surfaceDescription);Ui.gap(page,24);
        glassControls=Ui.column(this);glassControls.addView(Ui.button(this,"Liquid Glass optics",Ui.SURFACE,Ui.ACCENT,()->GlassMaterial.editor(this,config,this::refreshPreview)));Ui.gap(glassControls,8);glassControls.addView(Ui.button(this,"Choose wallpaper for blur & refraction",Ui.SURFACE,Ui.ACCENT,()->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),62)));glassControls.addView(Ui.button(this,"Use transparent glass without a wallpaper match",Ui.SURFACE,Ui.MUTED,()->{config.remove("glassUri");refreshPreview();}));Ui.gap(glassControls,12);page.addView(glassControls);
        transparencyControls=Ui.column(this);
        LinearLayout transparencyHeader=Ui.row(this);
        TextView transparencyTitle=Ui.bold(this,"TRANSPARENCY",11,Ui.MUTED);transparencyTitle.setLetterSpacing(.14f);Ui.weight(transparencyHeader,transparencyTitle);
        transparencyValue=Ui.bold(this,"",17,Ui.ACCENT);transparencyHeader.addView(transparencyValue);transparencyControls.addView(transparencyHeader);Ui.gap(transparencyControls,8);
        transparencySlider=new SeekBar(this);transparencySlider.setMax(100);transparencySlider.setKeyProgressIncrement(1);
        transparencySlider.setProgressTintList(android.content.res.ColorStateList.valueOf(Ui.ACCENT));transparencySlider.setThumbTintList(android.content.res.ColorStateList.valueOf(Ui.ACCENT));
        transparencyControls.addView(transparencySlider,new LinearLayout.LayoutParams(-1,Ui.dp(this,48)));
        LinearLayout ends=Ui.row(this);Ui.weight(ends,Ui.text(this,"0% · Opaque",11,Ui.MUTED));ends.addView(Ui.text(this,"100% · Clear",11,Ui.MUTED));transparencyControls.addView(ends);Ui.gap(transparencyControls,8);
        transparencyControls.addView(Ui.text(this,"Changes the background and button surfaces. Text and icons stay clear.",12,Ui.MUTED));Ui.gap(transparencyControls,24);page.addView(transparencyControls);
        transparencySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int progress,boolean fromUser){if(syncingTransparency)return;put("transparency",progress);updateTransparencyPreview();}
            public void onStartTrackingTouch(SeekBar bar){}
            public void onStopTrackingTouch(SeekBar bar){}
        });
        section("COLOR PALETTE");
        LinearLayout swatches=Ui.row(this);
        for(int n=0;n<Widgets.PALETTES.length;n++) {
            final int p=n;
            LinearLayout option=Ui.column(this); option.setGravity(Gravity.CENTER); Ui.pad(option,2,6);
            TextView dot=Ui.bold(this,"",20,Widgets.FOREGROUNDS[n]); dot.setGravity(Gravity.CENTER);Ui.surface(dot,Widgets.BACKGROUNDS[n],14);
            paletteDots.add(dot);Ui.add(option,dot,40,40);Ui.gap(option,8);
            TextView name=Ui.text(this,Widgets.PALETTES[n],10,Ui.MUTED); name.setGravity(Gravity.CENTER); option.addView(name);
            Ui.click(option,()->{put("palette",p);put("paletteCustom",true);refreshPreview();});
            swatches.addView(option,new LinearLayout.LayoutParams(0,-2,1));
        }
        page.addView(swatches); Ui.gap(page,22);
        section("LAYOUT");
        spinner(new String[]{"Spacious · more room to breathe","Compact · just the essentials"},config.optInt("density",0),n->{put("density",n);refreshPreview();});
        Ui.gap(page,22); section("WIDGET OPTIONS");
        if(kind.equals("universal")){
            String[] views=new String[Widgets.KINDS.length+1];views[0]="All tools · chooser";for(int n=0;n<Widgets.KINDS.length;n++)views[n+1]=Widgets.title(Widgets.KINDS[n]);
            int selection=contentKind.equals("menu")?0:Widgets.index(contentKind)+1;
            spinner(views,selection,n->{String selected=n==0?"menu":Widgets.KINDS[n-1];if(!selected.equals(UniversalWidget.currentPage(config))){put("page",selected);recreate();}});
            Ui.gap(page,10);page.addView(Ui.text(this,"All tools opens the picker inside the widget. Previous and Next switch views. Each view keeps its own settings.",12,Ui.MUTED));Ui.gap(page,16);
        }
        if(!contentKind.equals("menu")){
        JSONObject settings=contentOptions();
        String[] variants=switch(contentKind) {
            case "notes" -> new String[]{"One pinned note","Recent notes"};
            case "reminders" -> new String[]{"All upcoming reminders","Due today, including overdue"};
            case "timers" -> new String[]{"Everyday · 1 / 5 / 10 min","Focus · 25 / 5 / 50 min","Kitchen · 3 / 7 / 15 min"};
            case "alarms" -> new String[]{"Morning · 07:00 / 07:30 / 08:00","Early start · 06:00 / 06:30 / 07:00","Slow morning · 08:00 / 08:30 / 09:00"};
            case "checklist" -> new String[]{"Only unfinished tasks","All tasks"};
            case "habits" -> new String[]{"Progress bar & counter","Simple counter"};
            default -> MediaWidgets.supports(contentKind)?MediaWidgets.variants(contentKind):ExtraKinds.index(contentKind)>=0?(contentKind.equals("breathing")?new String[]{"Box rhythm","4–7–8 rhythm","Steady rhythm"}:new String[]{"Detailed","Summary"}):contentKind.equals("clock")?new String[]{"12-hour clock","24-hour clock"}:LibraryWidgets.variants(contentKind);
        };
        spinner(variants,settings.optInt("variant",0),n->{
            if(settings.optInt("variant",0)!=n){putOption("variant",n);for(int k=0;k<3;k++){settings.remove("value"+k);if(contentKind.equals("controls"))settings.remove("control"+k);}}
            refreshPreview();
        });
        if(MediaWidgets.supports(contentKind))MediaOptions.add(this,page,contentKind,settings,this::refreshPreview,(request,intent)->startActivityForResult(intent,request));
        else if(ExtraKinds.index(contentKind)>=0)ExtraOptions.add(this,page,contentKind,settings,this::refreshPreview,(slot,intent)->startActivityForResult(intent,63+slot));
        if(contentKind.equals("clock")&&settings.optBoolean("quickLook",false)){LibraryOptions.add(this,page,"weather",settings,this::refreshPreview,intent->startActivityForResult(intent,61));LibraryOptions.add(this,page,"agenda",settings,this::refreshPreview,intent->startActivityForResult(intent,61));}
        if(Widgets.index(contentKind)>=7&&ExtraKinds.index(contentKind)<0)LibraryOptions.add(this,page,contentKind,settings,this::refreshPreview,intent->startActivityForResult(intent,61));
        if(contentKind.equals("clock")&&ReplicaDesign.enabled(config)){Ui.gap(page,12);spinner(new String[]{"Clock face · Design default","Clock face · Digital","Clock face · Analog dial","Clock face · Stacked digits"},settings.optInt("clockFace",0),n->{putOption("clockFace",n);refreshPreview();});}
        if(contentKind.equals("notes")||contentKind.equals("habits")) {
            Ui.gap(page,14);
            List<Store.Item> items=Store.list(this,contentKind);
            boolean note=contentKind.equals("notes");int offset=note?2:1;
            String[] labels=new String[items.size()+offset];labels[0]=note?"Start with a new note":"Use the first habit";
            entityIds.add(note?Widgets.NEW_NOTE:"");
            if(note){labels[1]="Always use the latest active note";entityIds.add("");}
            int selected=note&&settings.optString("entity","").isEmpty()?1:0;
            for(int n=0;n<items.size();n++) { labels[n+offset]=items.get(n).title+(note&&!NoteLife.active(this,items.get(n))?" · Saved":""); entityIds.add(items.get(n).id); if(items.get(n).id.equals(settings.optString("entity"))) selected=n+offset; }
            spinner(labels,selected,n->{putOption("entity",entityIds.get(n));refreshPreview();});
            if(items.isEmpty()) { Ui.gap(page,10); page.addView(Ui.text(this,"Create a "+(note?"note":"habit")+" in Nook, then pick it here. Your widget will show it automatically.",12,Ui.MUTED)); }
        }
        if(contentKind.equals("notes")){
            Ui.gap(page,14);spinner(new String[]{"When inactive: show the saved-note slot","When inactive: show the next active note"},settings.optInt("fallback",0),n->{putOption("fallback",n);refreshPreview();});
            Ui.gap(page,14);spinner(new String[]{"Show quick note actions","Use space for more note text"},settings.optBoolean("noteActions",true)?0:1,n->{putOption("noteActions",n==0);refreshPreview();});
            Ui.gap(page,10);page.addView(Ui.text(this,"Temporary or Permanent is chosen when you create a note. Expired and archived notes stay in Library → Notes → Saved.",12,Ui.MUTED));
        }
        if(contentKind.equals("timers")||contentKind.equals("alarms")) {
            Ui.gap(page,14);
            page.addView(Ui.button(this,"Edit the three shortcuts",Ui.SURFACE,Ui.ACCENT,this::editShortcuts));
        }
        }
        Ui.gap(page,22); section("YOUR LABEL");
        label=new EditText(this); label.setTextColor(Ui.TEXT); label.setHintTextColor(Ui.MUTED); label.setSingleLine(true);
        label.setHint("Optional · e.g. Morning rituals"); label.setText(config.optString("title","")); label.setTextSize(15);
        label.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(32)});
        Ui.pad(label,14,14); Ui.surface(label,Ui.SURFACE,14); page.addView(label);
        label.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){} public void onTextChanged(CharSequence s,int a,int b,int count){put("title",s.toString());refreshPreview();} public void afterTextChanged(android.text.Editable e){}});
        Ui.gap(page,14); page.addView(Ui.text(this,"Each widget has its own settings. Long-press a widget to resize it; tap ··· to change its look.",12,Ui.MUTED)); Ui.gap(page,24);
        LinearLayout footer=Ui.column(this); Ui.pad(footer,24,12);
        footer.addView(Ui.button(this,widgetId<=0?"Add to home screen  +":"Save widget",Ui.ACCENT,Ui.BG,this::save)); root.addView(footer);
        ready=true; refreshPreview();
    }
    private static JSONObject json(String value) { try{return new JSONObject(value);}catch(Exception e){return new JSONObject();} }
    private void put(String key,Object value){try{config.put(key,value);}catch(Exception e){throw new IllegalStateException(e);}}
    private JSONObject contentOptions(){return kind.equals("universal")?UniversalWidget.pageSettings(config,contentKind):config;}
    private void putOption(String key,Object value){try{contentOptions().put(key,value);}catch(Exception e){throw new IllegalStateException(e);}}
    private void section(String name) { TextView t=Ui.bold(this,name,11,Ui.MUTED); t.setLetterSpacing(.14f); page.addView(t); Ui.gap(page,12); }
    private interface Selected { void accept(int n); }
    private void spinner(String[] values,int selected,Selected change) {
        Spinner spinner=new Spinner(this,Spinner.MODE_DROPDOWN);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinner.setAdapter(adapter);
        spinner.setSelection(Math.min(Math.max(0,selected),values.length-1)); Ui.surface(spinner,Ui.SURFACE,14);
        spinner.setPopupBackgroundDrawable(Ui.shape(Ui.SURFACE,14,this));
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int n,long id){if(v instanceof TextView t){t.setTextColor(Ui.TEXT);t.setTextSize(14);}change.accept(n);}
            public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        page.addView(spinner,new LinearLayout.LayoutParams(-1,Ui.dp(this,56)));
    }
    private void refreshPreview() {
        if(!ready||preview==null)return;
        int style=Math.max(0,Math.min(3,config.optInt("style",0)));
        for(int n=0;n<surfaceButtons.size();n++) {
            TextView button=surfaceButtons.get(n);Ui.surface(button,n==style?Ui.ACCENT:Ui.SURFACE,18);button.setTextColor(n==style?Ui.BG:Ui.TEXT);button.setSelected(n==style);
            button.setContentDescription(Widgets.STYLES[n]+(n==style?", selected":" surface"));
        }
        surfaceDescription.setText(new String[]{"A soft, opaque color that works with any wallpaper.","Light-catching edges and translucent color. Preview uses a sample wallpaper; live wallpaper blur is not applied.","A deeper glass tint with luminous edges and bright text. Preview uses a sample wallpaper; live wallpaper blur is not applied.","Clear or tinted optical glass with reflective rims. For a blurred, refracted wallpaper match, choose your wallpaper and adjust its crop. Android widgets cannot sample the launcher wallpaper live."}[style]);
        glassControls.setVisibility(style==3?View.VISIBLE:View.GONE);transparencyControls.setVisibility(style==0?View.GONE:View.VISIBLE);
        syncingTransparency=true;transparencySlider.setProgress(Widgets.transparency(config));syncingTransparency=false;
        int selected=Math.max(0,Math.min(5,config.optInt("palette",Widgets.defaultPalette(kind))));
        for(int n=0;n<paletteDots.size();n++){boolean active=n==selected&&(WidgetDesign.id(config)==0||config.optBoolean("paletteCustom",false));paletteDots.get(n).setText(active?"✓":"");((View)paletteDots.get(n).getParent()).setContentDescription(Widgets.PALETTES[n]+(active?", selected":" palette"));}
        preview.removeAllViews();
        preview.setBackgroundResource(style==0?0:R.drawable.preview_wallpaper);int inset=Ui.dp(this,style==0?0:14);preview.setPadding(inset,inset,inset,inset);preview.setClipToOutline(true);
        boolean compact=config.optInt("density",0)==1;int preferredWidth=config.optInt("preferredWidth",0),preferredHeight=config.optInt("preferredHeight",0);
        int previewHeight=config.optInt("preferredHeight",kind.equals("universal")?(compact?280:340):(compact?182:250));
        preview.getLayoutParams().height=Ui.dp(this,previewHeight+(style==0?0:28)); preview.requestLayout();
        JSONObject sample=json(config.toString());try{sample.put("_width",config.optInt("preferredWidth",Math.round(getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density)-48)-(style==0?0:28));sample.put("_height",previewHeight);}catch(Exception e){throw new IllegalStateException(e);}
        int innerWidth=-1;if(preferredWidth>0&&!kind.equals("universal")){int width=Math.min(preferredWidth,Math.round(getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density)-48-(style==0?0:28));int height=preferredHeight;ExtraData.put(sample,"_width",width);ExtraData.put(sample,"_height",height);innerWidth=Ui.dp(this,width);preview.getLayoutParams().height=Ui.dp(this,height+(style==0?0:28));preview.setGravity(Gravity.CENTER);}
        View view=Widgets.render(this,-1,kind,sample,false,true).apply(this,preview);
        preview.addView(view,new LinearLayout.LayoutParams(innerWidth,-1));
        updateTransparencyPreview();
        preview.setContentDescription("Preview, "+WidgetDesign.NAMES[WidgetDesign.id(config)]+", "+Widgets.STYLES[style]+", "+Widgets.PALETTES[selected]+" palette"+(style==0?"":", sample wallpaper"));
    }
    private void updateTransparencyPreview() {
        int percentage=Widgets.transparency(config);
        transparencyValue.setText(percentage+"%");transparencySlider.setContentDescription("Transparency "+percentage+" percent");
        Widgets.applyPreviewTransparency(preview,config);
    }
    private void editShortcuts() {
        int[] values=Widgets.shortcuts(contentOptions(),contentKind.equals("timers"),contentOptions().optInt("variant",0));
        LinearLayout content=Ui.column(this); Ui.pad(content,24,12);
        if(contentKind.equals("timers")) {
            EditText[] inputs=new EditText[3];
            content.addView(Ui.text(this,"Timer lengths in minutes (1–1440)",14,Ui.MUTED));
            for(int n=0;n<3;n++){inputs[n]=new EditText(this);inputs[n].setInputType(android.text.InputType.TYPE_CLASS_NUMBER);inputs[n].setText(String.valueOf(values[n]));inputs[n].setTextColor(Ui.TEXT);inputs[n].setHint("Shortcut "+(n+1));content.addView(inputs[n]);}
            AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Your timer shortcuts").setView(content).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
            dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{
                int[] parsed=new int[3];
                for(int n=0;n<3;n++){try{parsed[n]=Integer.parseInt(inputs[n].getText().toString());}catch(Exception e){parsed[n]=0;}if(parsed[n]<1||parsed[n]>1440){inputs[n].setError("Use 1–1440 minutes");return;}}
                for(int n=0;n<3;n++)putOption("value"+n,parsed[n]); refreshPreview();dialog.dismiss();
            }));dialog.show();
        } else {
            for(int n=0;n<3;n++) {
                final int slot=n;
                TextView button=Ui.button(this,"Shortcut "+(n+1)+" · "+Widgets.alarmTime(values[n]),Ui.SURFACE,Ui.TEXT,()->{});
                Ui.click(button,()->new TimePickerDialog(this,(picker,h,m)->{values[slot]=h*60+m;putOption("value"+slot,values[slot]);button.setText("Shortcut "+(slot+1)+" · "+Widgets.alarmTime(values[slot]));refreshPreview();},values[slot]/60,values[slot]%60,true).show());
                content.addView(button);Ui.gap(content,10);
            }
            new AlertDialog.Builder(this).setTitle("Your alarm shortcuts").setView(content).setPositiveButton("Done",null).show();
        }
    }
    private void save() {
        put("title",label.getText().toString().trim()); put("kind",kind);
        if(widgetId>0) {
            Store.config(this,widgetId,config);Widgets.update(this,widgetId,kind);WeatherJob.schedule(this);
            setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId));finish();return;
        }
        AppWidgetManager manager=AppWidgetManager.getInstance(this);
        if(!manager.isRequestPinAppWidgetSupported()) {
            new AlertDialog.Builder(this).setTitle("Add from your home screen")
                .setMessage("Touch and hold an empty spot on your home screen, choose Widgets, then find Nook. Drag a widget into place and customize it there.")
                .setPositiveButton("Got it",null).show();return;
        }
        String token=UUID.randomUUID().toString();Store.pending(this,token,config);
        Intent callback=new Intent(this,WidgetActionReceiver.class).setAction("pinned").putExtra("token",token)
            .setData(android.net.Uri.parse("nook://pin/"+token));
        PendingIntent success=PendingIntent.getBroadcast(this,0,callback,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);
        Bundle extras=new Bundle();extras.putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW,Widgets.render(this,-1,kind,config,false,true));
        if(manager.requestPinAppWidget(new ComponentName(this,Widgets.provider(kind)),extras,success)) {
            Toast.makeText(this,"Confirm placement with your home screen launcher",Toast.LENGTH_LONG).show();finish();
        } else Toast.makeText(this,"Your launcher couldn’t add this widget. Try its Widgets menu.",Toast.LENGTH_LONG).show();
    }
    @Override public void onSaveInstanceState(Bundle out){put("title",label.getText().toString());out.putString("config",config.toString());super.onSaveInstanceState(out);}
    @Override protected void onResume(){super.onResume();if(ready)refreshPreview();}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==70||request==71){if(MediaOptions.picked(this,contentOptions(),request,result,data))recreate();return;}if((request>=61&&request<=65)&&result==RESULT_OK&&data!=null&&data.getData()!=null){try{getContentResolver().takePersistableUriPermission(data.getData(),Intent.FLAG_GRANT_READ_URI_PERMISSION);if(request==62)put("glassUri",data.getData().toString());else if(request>=63)putOption("file"+(request-63),data.getData().toString());else putOption("photoUri",data.getData().toString());refreshPreview();}catch(SecurityException e){Toast.makeText(this,"Choose a local photo that allows lasting access",Toast.LENGTH_LONG).show();}}}
}
