package com.nook.widgets;

import android.app.*;
import android.graphics.Color;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;

/** A focused appearance editor with a live native widget preview. */
public final class AppearanceEditor {
    private AppearanceEditor(){}
    private interface Choice{void set(int value);}
    private static void put(JSONObject config,String key,Object value){try{config.put(key,value);}catch(Exception e){throw new IllegalStateException(e);}}
    private static void label(Activity a,LinearLayout parent,String text){Ui.gap(parent,16);parent.addView(Ui.bold(a,text,12,Ui.MUTED));Ui.gap(parent,8);}
    private static void choices(Activity a,LinearLayout parent,String[] labels,int value,Choice changed){
        Spinner spinner=new Spinner(a);ArrayAdapter<String> adapter=new ArrayAdapter<>(a,android.R.layout.simple_spinner_item,labels);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);spinner.setAdapter(adapter);spinner.setSelection(WidgetDesign.clamp(value,0,labels.length-1));
        Ui.surface(spinner,Ui.SURFACE,12);spinner.setPopupBackgroundDrawable(Ui.shape(Ui.SURFACE,12,a));parent.addView(spinner,new LinearLayout.LayoutParams(-1,Ui.dp(a,52)));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){int previous=value;public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View view,int n,long id){if(view instanceof TextView t){t.setTextColor(Ui.TEXT);t.setTextSize(14);}if(n!=previous){previous=n;changed.set(n);}}});
    }
    private static SeekBar slider(Activity a,LinearLayout parent,String title,int value,int min,int max,String suffix,Choice changed){
        TextView label=Ui.bold(a,title+" · "+value+suffix,13,Ui.TEXT);parent.addView(label);
        SeekBar slider=new SeekBar(a);slider.setMin(min);slider.setMax(max);slider.setProgress(value);slider.setContentDescription(title);slider.setProgressTintList(android.content.res.ColorStateList.valueOf(Ui.ACCENT));slider.setThumbTintList(android.content.res.ColorStateList.valueOf(Ui.ACCENT));parent.addView(slider,new LinearLayout.LayoutParams(-1,Ui.dp(a,44)));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int n,boolean user){label.setText(title+" · "+n+suffix);changed.set(n);}});return slider;
    }
    private static void toggle(Activity a,LinearLayout parent,String title,boolean value,java.util.function.Consumer<Boolean> change){Switch button=new Switch(a);button.setText(title);button.setTextColor(Ui.TEXT);button.setTextSize(14);button.setChecked(value);button.setMinHeight(Ui.dp(a,48));parent.addView(button);button.setOnCheckedChangeListener((v,on)->change.accept(on));}
    public static AlertDialog show(Activity a,String kind,JSONObject config,Runnable refresh){
        LinearLayout root=Ui.column(a);Ui.pad(root,16,4);root.setBackgroundColor(Ui.BG);
        FrameLayout preview=new FrameLayout(a);int height=kind.equals("universal")?218:182;root.addView(preview,new LinearLayout.LayoutParams(-1,Ui.dp(a,height)));
        Runnable redraw=()->{
            preview.removeAllViews();boolean glass=config.optInt("style",0)!=0;int inset=glass?10:0;preview.setPadding(Ui.dp(a,inset),Ui.dp(a,inset),Ui.dp(a,inset),Ui.dp(a,inset));preview.setBackgroundResource(glass?R.drawable.preview_wallpaper:0);
            try{JSONObject sample=new JSONObject(config.toString());int width=Math.round(a.getResources().getDisplayMetrics().widthPixels/a.getResources().getDisplayMetrics().density)-96-inset*2;sample.put("_width",width).put("_height",height-inset*2);View widget=Widgets.render(a,-1,kind,sample,true,true).apply(a,preview);preview.addView(widget,new FrameLayout.LayoutParams(-1,-1));}catch(Exception e){throw new IllegalStateException(e);}
            refresh.run();
        };
        ScrollView scroll=new ScrollView(a);LinearLayout controls=Ui.column(a);Ui.pad(controls,2,6);scroll.addView(controls);root.addView(scroll,new LinearLayout.LayoutParams(-1,Ui.dp(a,300)));
        label(a,controls,"TYPE & SIZE");
        choices(a,controls,new String[]{"Design default","System sans","Soft sans","Monospaced","Editorial serif","Dot display"},config.optInt("font",0),n->{put(config,"font",n);redraw.run();});
        Ui.gap(controls,12);slider(a,controls,"Text size",Math.round(WidgetDesign.scale(config)*100),85,115,"%",n->{put(config,"textScale",n);redraw.run();});
        label(a,controls,"SHAPE & BORDER");
        SeekBar corner=slider(a,controls,"Corner radius",WidgetDesign.radius(config),0,40," dp",n->{put(config,"corner",n);redraw.run();});
        corner.setEnabled(config.has("corner"));toggle(a,controls,"Use design's corner shape",!config.has("corner"),on->{corner.setEnabled(!on);if(on)config.remove("corner");else put(config,"corner",corner.getProgress());redraw.run();});
        choices(a,controls,new String[]{"Design border","No border","Fine · 1 dp","Medium · 2 dp","Bold · 3 dp"},config.has("border")?config.optInt("border")+1:0,n->{if(n==0)config.remove("border");else put(config,"border",n-1);redraw.run();});
        label(a,controls,"COLORS");
        choices(a,controls,new String[]{"Design tone","Light","Dark"},config.optInt("tone",0),n->{put(config,"tone",n);redraw.run();});
        if(WidgetDesign.id(config)!=0)toggle(a,controls,"Tint with selected palette",config.optBoolean("paletteCustom",false),on->{put(config,"paletteCustom",on);redraw.run();});
        Ui.gap(controls,12);controls.addView(Ui.text(a,"Accent color · blank uses the design's accent",12,Ui.MUTED));
        EditText accent=new EditText(a);accent.setSingleLine(true);accent.setTextColor(Ui.TEXT);accent.setHintTextColor(Ui.MUTED);accent.setHint("#RRGGBB");accent.setText(config.optString("accent",""));accent.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});controls.addView(accent);
        accent.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int before,int count){String value=s.toString().trim();if(WidgetDesign.validAccent(value)){if(value.isEmpty())config.remove("accent");else put(config,"accent",value);accent.setError(null);redraw.run();}}});
        LinearLayout swatches=Ui.row(a);for(String color:new String[]{"#FF5148","#007AFF","#8B72E8","#65A34B","#D39B42","#F0F0ED"}){TextView dot=Ui.text(a,"●",28,Color.parseColor(color));dot.setGravity(Gravity.CENTER);dot.setContentDescription("Accent "+color);swatches.addView(dot,new LinearLayout.LayoutParams(0,Ui.dp(a,48),1));Ui.click(dot,()->accent.setText(color));}controls.addView(swatches);
        label(a,controls,"SPACING & DETAILS");
        choices(a,controls,new String[]{"Design spacing","Tight","Balanced","Airy"},config.optInt("padding",0),n->{put(config,"padding",n);redraw.run();});
        Ui.gap(controls,8);choices(a,controls,new String[]{"Design alignment","Left aligned","Centered"},config.optInt("align",0),n->{put(config,"align",n);redraw.run();});
        toggle(a,controls,"Show widget icon",config.optBoolean("showIcon",true),on->{put(config,"showIcon",on);redraw.run();});
        toggle(a,controls,"Show widget label",config.optBoolean("showLabel",true),on->{put(config,"showLabel",on);redraw.run();});
        controls.addView(Ui.text(a,"Tool controls stay available. In Universal, these choices apply to every view.",12,Ui.MUTED));Ui.gap(controls,12);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Fine-tune appearance").setView(root).setPositiveButton("Done",null).setNeutralButton("Preset defaults",(d,n)->{WidgetDesign.resetOverrides(config);refresh.run();}).create();
        dialog.setOnShowListener(d->{dialog.getButton(-1).setOnClickListener(v->{if(!WidgetDesign.validAccent(accent.getText().toString().trim())){accent.setError("Use a color like #007AFF, or leave blank");return;}dialog.dismiss();});redraw.run();});dialog.show();return dialog;
    }
}
