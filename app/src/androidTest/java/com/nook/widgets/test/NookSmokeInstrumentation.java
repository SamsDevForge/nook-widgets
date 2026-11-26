package com.nook.widgets.test;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.nook.widgets.*;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.*;

/** Device integration checks, without a third-party test runtime. */
public class NookSmokeInstrumentation extends Instrumentation {
    private final List<String[]> created=new ArrayList<>();
    private int assertions;
    private boolean libraryOnly,stockOnly,sourceOnly,mediaOnly;
    @Override public void onCreate(Bundle args){super.onCreate(args);libraryOnly=args!=null&&Boolean.parseBoolean(args.getString("libraryOnly","false"));stockOnly=args!=null&&Boolean.parseBoolean(args.getString("stockOnly","false"));sourceOnly=args!=null&&Boolean.parseBoolean(args.getString("sourceOnly","false"));mediaOnly=args!=null&&Boolean.parseBoolean(args.getString("mediaOnly","false"));start();}
    private void check(boolean condition,String message){assertions++;if(!condition)throw new AssertionError(message);}
    private Store.Item add(String type,String title,String body) {
        Store.Item i=new Store.Item();i.title=title;i.body=body;Store.save(getTargetContext(),type,i);created.add(new String[]{type,i.id});return i;
    }
    private void screenshot(String name) throws Exception {
        Thread.sleep(700);Bitmap bitmap=getUiAutomation().takeScreenshot();
        if(bitmap==null)throw new AssertionError("Screenshot unavailable: "+name);
        File dir=new File(getTargetContext().getFilesDir(),"screenshots");dir.mkdirs();
        try(FileOutputStream output=new FileOutputStream(new File(dir,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,output);}bitmap.recycle();
    }
    private void onMain(Runnable action){runOnMainSync(action);waitForIdleSync();}
    private void glassComparison() {
        onMain(()->{
            Context c=getTargetContext();
            Bitmap bitmap=Bitmap.createBitmap(1280,820,Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);canvas.drawColor(Ui.BG);
            android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Ui.TEXT);paint.setTextSize(30);paint.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));
            for(int n=0;n<2;n++) {
                int left=32+n*624;canvas.drawText(n==0?"FROSTED GLASS":"SMOKED GLASS",left+8,58,paint);
                FrameLayout frame=new FrameLayout(c);frame.setBackgroundResource(com.nook.widgets.R.drawable.preview_wallpaper);frame.setPadding(28,28,28,28);
                JSONObject config=Widgets.defaults("notes");
                try{config.put("style",n+1).put("palette",3);}catch(Exception e){throw new RuntimeException(e);}
                View widget=Widgets.render(c,-1,"notes",config,false,true).apply(c,frame);frame.addView(widget,new FrameLayout.LayoutParams(-1,-1));
                frame.measure(View.MeasureSpec.makeMeasureSpec(592,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(650,View.MeasureSpec.EXACTLY));frame.layout(0,0,592,650);
                canvas.save();canvas.translate(left,90);frame.draw(canvas);canvas.restore();
            }
            paint.setTextSize(24);paint.setColor(Ui.MUTED);canvas.drawText("Native widget previews on a sample wallpaper · Translucent tints, without live blur",40,790,paint);
            File dir=new File(c.getFilesDir(),"screenshots");dir.mkdirs();
            try(FileOutputStream output=new FileOutputStream(new File(dir,"glass-comparison.png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,output);}catch(Exception e){throw new RuntimeException(e);}finally{bitmap.recycle();}
        });
    }
    private static TextView findText(View view,String label) {
        if(view instanceof TextView t && t.getText().toString().equals(label))return t;
        if(view instanceof ViewGroup g)for(int n=0;n<g.getChildCount();n++){TextView found=findText(g.getChildAt(n),label);if(found!=null)return found;}
        return null;
    }
    private static SeekBar findSlider(View view) {
        if(view instanceof SeekBar bar)return bar;
        if(view instanceof ViewGroup group)for(int n=0;n<group.getChildCount();n++){SeekBar found=findSlider(group.getChildAt(n));if(found!=null)return found;}
        return null;
    }
    private static int surfacePixelAlpha(View view,Bitmap bitmap) {
        bitmap.eraseColor(android.graphics.Color.TRANSPARENT);
        ImageView image=view.findViewById(com.nook.widgets.R.id.widget_surface);
        android.graphics.drawable.Drawable surface=image.getDrawable();surface.setBounds(0,0,bitmap.getWidth(),bitmap.getHeight());surface.draw(new android.graphics.Canvas(bitmap));
        return android.graphics.Color.alpha(bitmap.getPixel(bitmap.getWidth()/2,bitmap.getHeight()/2));
    }
    private static SeekBar sliderNamed(View view,String name){
        if(view instanceof SeekBar slider&&name.contentEquals(slider.getContentDescription()))return slider;
        if(view instanceof ViewGroup group)for(int n=0;n<group.getChildCount();n++){SeekBar found=sliderNamed(group.getChildAt(n),name);if(found!=null)return found;}return null;
    }
    private static AlertDialog appearanceDialog(Activity activity){try{java.lang.reflect.Field field=WidgetConfigActivity.class.getDeclaredField("appearance");field.setAccessible(true);return (AlertDialog)field.get(activity);}catch(Exception e){throw new RuntimeException(e);}}
    private void designComparison(){
        onMain(()->{
            Context c=getTargetContext();Bitmap bitmap=Bitmap.createBitmap(1520,4000,Bitmap.Config.ARGB_8888);android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);canvas.drawColor(Ui.BG);android.graphics.Paint paint=new android.graphics.Paint(3);paint.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));paint.setTextSize(28);paint.setColor(Ui.TEXT);
            for(int n=0;n<=WidgetDesign.NAMES.length;n++)try{
                int left=32+(n%2)*752,top=24+(n/2)*794;boolean universal=n==WidgetDesign.NAMES.length;String kind=universal?"universal":n==7?"battery":n==1||n==4?"clock":n==3||n==6?"timers":"notes";
                JSONObject config=Widgets.defaults(kind);WidgetDesign.select(config,universal?7:n);config.put("_width",Math.round(696/c.getResources().getDisplayMetrics().density)).put("_height",Math.round(690/c.getResources().getDisplayMetrics().density));
                canvas.drawText(universal?"Universal · shared design":WidgetDesign.NAMES[n],left,top+30,paint);FrameLayout frame=new FrameLayout(c);if(config.optInt("style")!=0)frame.setBackgroundResource(com.nook.widgets.R.drawable.preview_wallpaper);
                frame.addView(Widgets.render(c,-1,kind,config,universal,true).apply(c,frame),new FrameLayout.LayoutParams(-1,-1));frame.measure(View.MeasureSpec.makeMeasureSpec(696,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(690,View.MeasureSpec.EXACTLY));frame.layout(0,0,696,690);canvas.save();canvas.translate(left,top+62);frame.draw(canvas);canvas.restore();
            }catch(Exception e){throw new RuntimeException(e);}
            File dir=new File(c.getFilesDir(),"screenshots");dir.mkdirs();try(FileOutputStream output=new FileOutputStream(new File(dir,"design-languages.png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,output);}catch(Exception e){throw new RuntimeException(e);}finally{bitmap.recycle();}
        });
    }
    private void designLifecycle(Context c,AppWidgetHost host,List<Activity> activities) throws Exception {
        onMain(()->{
            check(c.getResources().getFont(com.nook.widgets.R.font.nook_dots)!=null,"Original dot display font loads natively");
            Bitmap sample=Bitmap.createBitmap(280,200,Bitmap.Config.ARGB_8888);
            for(int design=0;design<WidgetDesign.NAMES.length;design++)for(String kind:Widgets.CATALOG)for(int surface=0;surface<3;surface++)for(int density=0;density<2;density++)try{
                JSONObject config=Widgets.defaults(kind);WidgetDesign.select(config,design);config.put("style",surface).put("density",density).put("transparency",50);
                View view=Widgets.render(c,990,kind,config,false,true).apply(c,new FrameLayout(c));int height=Ui.dp(c,kind.equals("universal")?(density==0?340:180):(density==0?250:182));view.measure(View.MeasureSpec.makeMeasureSpec(Ui.dp(c,320),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,view.getMeasuredWidth(),height);
                check(Math.abs(surfacePixelAlpha(view,sample)-(surface==0?255:128))<=1,"Design preserves exact surface transparency");
                TextView text=view.findViewById(kind.equals("universal")?com.nook.widgets.R.id.hub_label:com.nook.widgets.R.id.widget_title);check(text.getAlpha()==1,"Design typography stays fully visible");
            }catch(Exception e){throw new RuntimeException("Design="+design+" kind="+kind+" surface="+surface+" density="+density,e);}
            for(int design=1;design<WidgetDesign.NAMES.length;design++)for(String page:Widgets.KINDS)try{
                JSONObject config=Widgets.defaults("universal");WidgetDesign.select(config,design);config.put("page",page).put("textScale",115);View view=Widgets.render(c,990,"universal",config,true,true).apply(c,new FrameLayout(c));int height=Ui.dp(c,180);view.measure(View.MeasureSpec.makeMeasureSpec(Ui.dp(c,320),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,view.getMeasuredWidth(),height);
                View nav=view.findViewById(com.nook.widgets.R.id.hub_next);check(nav.getVisibility()==View.VISIBLE&&nav.getHeight()>0,"Styled Universal navigation remains usable");
                View content=view.findViewById(com.nook.widgets.R.id.hub_content);check(content.getHeight()>=Ui.dp(c,90),"Minimum Universal view has room for large text and controls");
            }catch(Exception e){throw new RuntimeException(e);}
            for(int font=0;font<=5;font++)for(String kind:Widgets.CATALOG)try{
                JSONObject config=Widgets.defaults(kind);WidgetDesign.select(config,4);config.put("font",font).put("corner",0).put("border",3).put("tone",2).put("accent","#33CC99").put("textScale",115).put("align",2).put("showIcon",false).put("showLabel",false);
                View view=Widgets.render(c,990,kind,config,false,true).apply(c,new FrameLayout(c));View settings=view.findViewById(kind.equals("universal")?com.nook.widgets.R.id.hub_settings:com.nook.widgets.R.id.widget_settings);check(settings.getVisibility()==View.VISIBLE,"Fine tuning preserves customization access");
                View label=view.findViewById(kind.equals("universal")?com.nook.widgets.R.id.hub_label:com.nook.widgets.R.id.widget_label);check(label.getVisibility()!=View.VISIBLE,"Header label can be hidden");
            }catch(Exception e){throw new RuntimeException(e);}
            check(WidgetDesign.validAccent("#A1B2C3")&&!WidgetDesign.validAccent("#ABC")&&!WidgetDesign.validAccent("#GGGGGG"),"Accent validation rejects malformed colors");sample.recycle();
            for(int design=0;design<WidgetDesign.NAMES.length;design++)for(int palette=0;palette<6;palette++)for(int tone=1;tone<=2;tone++)try{
                JSONObject config=Widgets.defaults("notes");WidgetDesign.select(config,design);config.put("paletteCustom",true).put("palette",palette).put("tone",tone);WidgetDesign.Colors colors=WidgetDesign.colors(config,"notes");
                double bg=android.graphics.Color.luminance(colors.surface),fg=android.graphics.Color.luminance(colors.text),muted=android.graphics.Color.luminance(colors.muted);
                check((Math.max(bg,fg)+.05)/(Math.min(bg,fg)+.05)>=4.5,"Tinted design preserves readable primary text");check((Math.max(bg,muted)+.05)/(Math.min(bg,muted)+.05)>=4.5,"Tinted design preserves readable secondary text");
            }catch(Exception e){throw new RuntimeException(e);}
        });
        designComparison();
        AppWidgetManager manager=AppWidgetManager.getInstance(c);int id=host.allocateAppWidgetId();check(manager.bindAppWidgetIdIfAllowed(id,new ComponentName(c,Widgets.Notes.class)),"Design test uses a native bound widget");JSONObject original=Widgets.defaults("notes");original.put("entity","");Store.config(c,id,original);
        Activity first=startActivitySync(new Intent(c,WidgetConfigActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));activities.add(first);waitForIdleSync();ActivityMonitor recreation=addMonitor(WidgetConfigActivity.class.getName(),null,false);
        onMain(()->((Spinner)findText(first.getWindow().getDecorView(),WidgetDesign.NAMES[0]).getParent()).setSelection(1));Activity editor=recreation.waitForActivityWithTimeout(5000);removeMonitor(recreation);check(editor!=null,"Selecting a design restores the screen with its defaults");activities.remove(first);activities.add(editor);waitForIdleSync();screenshot("design-editor");
        onMain(()->findText(editor.getWindow().getDecorView(),"Fine-tune appearance").performClick());AlertDialog dialog=appearanceDialog(editor);check(dialog!=null&&dialog.isShowing(),"Fine-tuning opens a live native editor");
        onMain(()->{
            View controls=dialog.getWindow().getDecorView();sliderNamed(controls,"Text size").setProgress(115);findText(controls,"Use design's corner shape").performClick();sliderNamed(controls,"Corner radius").setProgress(0);
            ((Spinner)findText(controls,"Design default").getParent()).setSelection(3);findText(controls,"Show widget icon").performClick();findText(controls,"Show widget label").performClick();
            editWithHint(controls,"#RRGGBB").setText("#GGGGGG");dialog.getButton(-1).performClick();check(dialog.isShowing(),"Invalid accent keeps the editor open");editWithHint(controls,"#RRGGBB").setText("#3366CC");
        });waitForIdleSync();screenshot("design-fine-tune");onMain(()->dialog.getButton(-1).performClick());
        onMain(()->{findText(editor.getWindow().getDecorView(),"Frosted").performClick();findSlider(editor.getWindow().getDecorView()).setProgress(63);findText(editor.getWindow().getDecorView(),"Save widget").performClick();});activities.remove(editor);
        JSONObject saved=Store.config(c,id);check(WidgetDesign.id(saved)==1&&saved.optInt("font")==3&&saved.optInt("corner")==0&&saved.optInt("textScale")==115,"Design, font, corners and scale persist together");check(saved.optString("accent").equals("#3366CC")&&!saved.optBoolean("showIcon",true)&&!saved.optBoolean("showLabel",true),"Accent and header choices persist");check(saved.optInt("transparency")==63&&saved.optString("entity").isEmpty(),"Styling preserves glass controls and content selection");
        onMain(()->{AppWidgetHostView view=host.createView(c,id,manager.getAppWidgetInfo(id));check(((TextView)view.findViewById(com.nook.widgets.R.id.widget_label)).getCurrentTextColor()==0xff3366cc,"Saved accent reaches the native launcher widget");});
        Activity reopened=startActivitySync(new Intent(c,WidgetConfigActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));activities.add(reopened);waitForIdleSync();onMain(()->findText(reopened.getWindow().getDecorView(),"Fine-tune appearance").performClick());AlertDialog again=appearanceDialog(reopened);
        onMain(()->{View controls=again.getWindow().getDecorView();check(sliderNamed(controls,"Text size").getProgress()==115&&sliderNamed(controls,"Corner radius").getProgress()==0,"Fine-tuning restores saved slider values");again.getButton(-3).performClick();});
        onMain(()->findText(reopened.getWindow().getDecorView(),"Save widget").performClick());activities.remove(reopened);
        saved=Store.config(c,id);check(WidgetDesign.id(saved)==1&&!saved.has("font")&&!saved.has("corner")&&!saved.has("accent")&&saved.optString("entity").isEmpty(),"Preset defaults clear appearance overrides and preserve note selection");
        JSONObject universal=Widgets.defaults("universal");UniversalWidget.pageSettings(universal,"timers").put("value0",17);WidgetDesign.select(universal,4);universal.put("font",4).put("corner",8).put("accent","#4477DD");JSONObject page=UniversalWidget.viewConfig(universal,"timers");check(WidgetDesign.id(page)==4&&page.optInt("corner")==8&&page.optInt("value0")==17&&page.optInt("font")==4,"Universal views share appearance while retaining tool options");
        host.deleteAppWidgetId(id);Store.removeConfig(c,id);
    }
    private static Dialog editorDialog(Activity activity){
        try{java.lang.reflect.Field field=MainActivity.class.getDeclaredField("editor");field.setAccessible(true);return (Dialog)field.get(activity);}catch(Exception e){throw new RuntimeException(e);}
    }
    private static EditText editWithHint(View view,String hint){
        if(view instanceof EditText edit&&edit.getHint()!=null&&edit.getHint().toString().equals(hint))return edit;
        if(view instanceof ViewGroup group)for(int n=0;n<group.getChildCount();n++){EditText found=editWithHint(group.getChildAt(n),hint);if(found!=null)return found;}return null;
    }
    private Activity openNoteFromWidget(Context c,AppWidgetHost host,int widget,String choice,Activity fallback,List<Activity> activities) throws Exception {
        ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);
        try{
            onMain(()->{AppWidgetHostView view=host.createView(c,widget,AppWidgetManager.getInstance(c).getAppWidgetInfo(widget));TextView button=findText(view,choice);check(button!=null,"Widget offers "+choice);button.performClick();});
            // A PendingIntent crosses system_server before the activity message
            // reaches the app. One idle queue does not imply launch completion.
            long limit=SystemClock.elapsedRealtime()+5000;
            while(SystemClock.elapsedRealtime()<limit){
                Activity observed=monitor.getLastActivity();Activity candidate=observed==null?fallback:observed;boolean[] visible={false};
                onMain(()->{Dialog dialog=editorDialog(candidate);visible[0]=dialog!=null&&dialog.isShowing();});
                if(visible[0]){if(candidate!=fallback&&!activities.contains(candidate))activities.add(candidate);check(true,"Widget opens note editor");return candidate;}
                Thread.sleep(100);
            }
            throw new AssertionError("Widget did not open the "+choice+" note editor within 5 seconds");
        }finally{removeMonitor(monitor);}
    }
    private void noteLifecycle(Context c,AppWidgetHost host,Activity main,List<Activity> activities) throws Exception {
        String legacyId=UUID.randomUUID().toString();
        synchronized(Store.class){android.content.SharedPreferences preferences=c.getSharedPreferences("nook",Context.MODE_PRIVATE);org.json.JSONArray legacy=new org.json.JSONArray(preferences.getString("notes","[]"));legacy.put(new JSONObject().put("id",legacyId).put("title","Legacy permanent note").put("body","Created before lifetimes existed.").put("created",System.currentTimeMillis()-86400000));preferences.edit().putString("notes",legacy.toString()).commit();}
        created.add(new String[]{"notes",legacyId});check(Store.find(c,"notes",legacyId).expiresAt==0&&NoteLife.active(c,Store.find(c,"notes",legacyId)),"Existing JSON notes remain permanent");NoteLife.archive(c,legacyId);
        Store.Item permanent=add("notes","A note to keep","Keep this until I remove it.");
        check(permanent.expiresAt==0&&NoteLife.active(c,Store.find(c,"notes",permanent.id)),"New and legacy-shaped notes are permanent");
        Store.Item timer=add("notes","A passing thought","Recover this after expiry.");NoteLife.timer(c,timer,NoteLife.HOUR);Store.save(c,"notes",timer);
        long before=NoteLife.remaining(c,Store.find(c,"notes",timer.id));NoteLife.extend(c,timer.id);
        check(NoteLife.remaining(c,Store.find(c,"notes",timer.id))>before+NoteLife.HOUR-1000,"Extend adds to remaining duration");
        timer=Store.find(c,"notes",timer.id);timer.expiresAt=System.currentTimeMillis()-10000;Store.save(c,"notes",timer);
        check(NoteLife.active(c,Store.find(c,"notes",timer.id)),"Countdown ignores wall-clock jump within same boot");
        check(Store.find(c,"notes",timer.id).expiresAt>System.currentTimeMillis(),"Countdown updates its restart fallback");
        timer=Store.find(c,"notes",timer.id);timer.expiryBoot=NoteLife.boot(c)-1;timer.expiresAt=System.currentTimeMillis()+NoteLife.HOUR;Store.save(c,"notes",timer);
        check(Store.find(c,"notes",timer.id).expiryBoot==NoteLife.boot(c),"Countdown is rebased after simulated restart");
        Store.Item expired=add("notes","An expired note","Still safely saved.");NoteLife.deadline(expired,System.currentTimeMillis()-1000);Store.save(c,"notes",expired);
        check(!NoteLife.active(c,Store.find(c,"notes",expired.id))&&Store.find(c,"notes",expired.id).expired,"Past deadline expires immediately");
        check(Store.find(c,"notes",expired.id).body.equals("Still safely saved."),"Expiry preserves note content");
        expired=Store.find(c,"notes",expired.id);expired.expiresAt=System.currentTimeMillis()+NoteLife.HOUR;Store.save(c,"notes",expired);
        check(!NoteLife.active(c,Store.find(c,"notes",expired.id)),"Expired notes do not return after clock rollback");
        new WidgetActionReceiver().onReceive(c,new Intent().setAction("note_keep").putExtra("id",expired.id));
        check(NoteLife.active(c,Store.find(c,"notes",expired.id))&&Store.find(c,"notes",expired.id).expiresAt==0,"Keep restores as permanent");
        new WidgetActionReceiver().onReceive(c,new Intent().setAction("note_archive").putExtra("id",permanent.id));
        check(!NoteLife.active(c,Store.find(c,"notes",permanent.id))&&Store.find(c,"notes",permanent.id).archived,"Archive removes from active notes");
        NoteLife.keep(c,permanent.id);check(NoteLife.active(c,Store.find(c,"notes",permanent.id)),"Archived permanent note can be restored");
        check(NoteLife.endToday()>System.currentTimeMillis(),"Today ends at the next local midnight");
        int widget=host.allocateAppWidgetId();AppWidgetManager manager=AppWidgetManager.getInstance(c);
        check(manager.bindAppWidgetIdIfAllowed(widget,new ComponentName(c,Widgets.provider("notes"))),"Note capture widget binds");
        JSONObject config=Widgets.defaults("notes");config.put("style",1).put("transparency",55);check(config.optString("entity").equals(Widgets.NEW_NOTE),"New widgets start with their own note choices");Store.config(c,widget,config);Widgets.update(c,widget,"notes");
        onMain(()->{View legacy=Widgets.render(c,widget,"notes",new JSONObject(),false,false).apply(c,new FrameLayout(c));check(findText(legacy,NoteLife.activeNotes(c).get(0).title)!=null,"Existing auto-latest widget selection is preserved");});
        Activity capture=openNoteFromWidget(c,host,widget,"Temporary",main,activities);
        onMain(()->{
            View dialog=editorDialog(capture).getWindow().getDecorView();check(findText(dialog,"Temporary").isSelected(),"Temporary capture selects its lifetime");
            findText(dialog,"15 min").performClick();editWithHint(dialog,"Give it a title").setText("Tea and a short break");editWithHint(dialog,"What’s on your mind?").setText("Just for the next fifteen minutes.");
        });screenshot("temporary-note-editor");
        onMain(()->findText(editorDialog(capture).getWindow().getDecorView(),"Save note").performClick());
        String capturedId=Store.config(c,widget).optString("entity");created.add(new String[]{"notes",capturedId});Store.Item captured=Store.find(c,"notes",capturedId);
        check(captured!=null&&captured.title.equals("Tea and a short break"),"Widget capture saves and pins the note");
        check(NoteLife.remaining(c,captured)>14*60000L&&NoteLife.remaining(c,captured)<=15*60000L,"Preset timer starts on save");
        check(Store.config(c,widget).optInt("transparency")==55&&Store.config(c,widget).optInt("style")==1,"Capture preserves widget glass settings");
        config=Store.config(c,widget);config.put("entity","missing-note");Store.config(c,widget,config);Widgets.update(c,widget,"notes");
        Activity keepCapture=openNoteFromWidget(c,host,widget,"Permanent",capture,activities);
        onMain(()->{View dialog=editorDialog(keepCapture).getWindow().getDecorView();check(findText(dialog,"Permanent").isSelected(),"Permanent capture selects its lifetime");editWithHint(dialog,"Give it a title").setText("My permanent note");findText(dialog,"Save note").performClick();});
        String keepId=Store.config(c,widget).optString("entity");created.add(new String[]{"notes",keepId});check(Store.find(c,"notes",keepId).expiresAt==0,"Permanent capture has no expiry");
        Store.Item shortNote=add("notes","A short-lived note","This should leave the widget quietly.");NoteLife.timer(c,shortNote,1800);Store.save(c,"notes",shortNote);
        config=Store.config(c,widget);config.put("entity",shortNote.id);Store.config(c,widget,config);Widgets.update(c,widget,"notes");
        // Android may enforce a minimum alarm lead time even for an exact alarm.
        long deliveryLimit=SystemClock.elapsedRealtime()+12000;
        while(!Store.find(c,"notes",shortNote.id).expired&&SystemClock.elapsedRealtime()<deliveryLimit)Thread.sleep(250);
        waitForIdleSync();
        check(Store.find(c,"notes",shortNote.id).expired,"Alarm receiver expires a note without user interaction");
        onMain(()->{AppWidgetHostView view=host.createView(c,widget,manager.getAppWidgetInfo(widget));check(findText(view,"Note expired.")!=null,"Expired content is removed from the native widget");check(findText(view,"A short-lived note")==null,"Expired title is not leaked by the widget");});
        onMain(()->{try{JSONObject moreText=Store.config(c,widget);moreText.put("entity",keepId).put("noteActions",false);View view=Widgets.render(c,widget,"notes",moreText,false,false).apply(c,new FrameLayout(c));check(view.findViewById(com.nook.widgets.R.id.widget_actions).getVisibility()==View.GONE,"More-text mode hides quick actions");check(((TextView)view.findViewById(com.nook.widgets.R.id.widget_body)).getMaxLines()==4,"More-text mode allocates four lines");}catch(Exception e){throw new RuntimeException(e);}});
        JSONObject fallback=Store.config(c,widget);fallback.put("fallback",1);
        onMain(()->{View fallbackView=Widgets.render(c,widget,"notes",fallback,false,false).apply(c,new FrameLayout(c));check(findText(fallbackView,"Note expired.")==null,"Optional fallback shows an active note instead");});
        new WidgetActionReceiver().onReceive(c,new Intent().setAction("note_extend").putExtra("id",shortNote.id));
        check(NoteLife.active(c,Store.find(c,"notes",shortNote.id))&&NoteLife.remaining(c,Store.find(c,"notes",shortNote.id))>59*60000L,"Extend restores an expired note for an hour");
        NoteLife.archive(c,shortNote.id);check(Store.find(c,"notes",shortNote.id).body.equals(shortNote.body),"Archiving keeps the entire note");
        // Exercise state restoration of lifetime choices without saving a draft.
        onMain(()->{
            NoteLifetimeEditor choices=new NoteLifetimeEditor((Activity)main,new Store.Item(),"temporary");findText(choices,"6 hours").performClick();Bundle state=new Bundle();choices.saveState(state);
            NoteLifetimeEditor restored=new NoteLifetimeEditor((Activity)main,new Store.Item(),null);restored.restoreState(state);Store.Item restoredItem=new Store.Item();check(restored.apply(restoredItem),"Lifetime draft restores");check(NoteLife.remaining(c,restoredItem)>5*NoteLife.HOUR,"Restored draft preserves duration");
            findText(restored,"Permanent").performClick();check(restored.apply(restoredItem)&&restoredItem.expiresAt==0,"Temporary draft converts to permanent");
            findText(restored,"Temporary").performClick();findText(restored,"Today").performClick();check(restored.apply(restoredItem)&&restoredItem.expiresAt==NoteLife.endToday()&&restoredItem.expiryElapsed==0,"Today uses a fixed local-midnight deadline");
            Store.Item ticking=new Store.Item();NoteLife.timer(c,ticking,NoteLife.HOUR);NoteLifetimeEditor late=new NoteLifetimeEditor((Activity)main,ticking,null);ticking.expiryElapsed=SystemClock.elapsedRealtime()-1;check(!late.apply(new Store.Item()),"A note expiring while edited requires a new lifetime");
        });
        // Leave test notes available only in Saved, then verify the library filter.
        for(String id:new String[]{permanent.id,timer.id,expired.id,capturedId,keepId})NoteLife.archive(c,id);
        onMain(()->{View navigation=keepCapture.getWindow().getDecorView();TextView library=findText(navigation,"Library");((View)library.getParent()).performClick();findText(keepCapture.getWindow().getDecorView(),"Saved").performClick();});screenshot("saved-notes");
        check(findText(keepCapture.getWindow().getDecorView(),"Tea and a short break")!=null,"Archived notes appear in Saved");
        host.deleteAppWidgetId(widget);Store.removeConfig(c,widget);
    }
    private void waitForPage(Context c,int widget,String page) throws Exception {
        long limit=SystemClock.elapsedRealtime()+5000;
        while(!UniversalWidget.currentPage(Store.config(c,widget)).equals(page)&&SystemClock.elapsedRealtime()<limit)Thread.sleep(50);
        waitForIdleSync();check(UniversalWidget.currentPage(Store.config(c,widget)).equals(page),"Universal switch reaches "+page);
    }
    private void universalLifecycle(Context c,AppWidgetHost host,Activity main,List<Activity> activities) throws Exception {
        onMain(()->{
            Bitmap background=Bitmap.createBitmap(320,220,Bitmap.Config.ARGB_8888);
            String[] pages=Widgets.CATALOG.clone();pages[0]="menu";
            for(String page:pages)for(int palette=0;palette<6;palette++)for(int style=0;style<3;style++)for(int density=0;density<2;density++){
                try{
                    JSONObject config=Widgets.defaults("universal");config.put("page",page).put("palette",palette).put("style",style).put("density",density);
                    View view=Widgets.render(c,992,"universal",config,false,false).apply(c,new FrameLayout(c));
                    int height=Ui.dp(c,density==0?340:180);view.measure(View.MeasureSpec.makeMeasureSpec(Ui.dp(c,360),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,view.getMeasuredWidth(),height);
                    check(view.findViewById(com.nook.widgets.R.id.hub_menu)!=null,"Universal has an in-widget picker");
                    check(Math.abs(surfacePixelAlpha(view,background)-(style==0?255:style==1?191:204))<=1,"Universal shared surface alpha");
                    if(page.equals("menu")){
                        View last=view.findViewById(density==0?com.nook.widgets.R.id.hub_tile8:com.nook.widgets.R.id.hub_tile5);android.graphics.Rect bounds=new android.graphics.Rect(0,0,last.getWidth(),last.getHeight());((ViewGroup)view).offsetDescendantRectToMyCoords(last,bounds);
                        check(bounds.bottom<=height&&last.getHeight()>=Ui.dp(c,density==0?48:32),"Chooser remains usable when compact");
                    }else{
                        ViewGroup content=view.findViewById(com.nook.widgets.R.id.hub_content);
                        check(content.findViewById(com.nook.widgets.R.id.widget_header).getVisibility()==View.GONE,"Embedded view has one header");
                        check(content.findViewById(com.nook.widgets.R.id.widget_surface).getVisibility()==View.GONE,"Embedded glass does not double its opacity");
                        if(page.equals("notes")&&density==1)check(content.findViewById(com.nook.widgets.R.id.widget_actions).getVisibility()==View.VISIBLE,"Compact note view retains capture choices");
                    }
                }catch(Exception e){throw new RuntimeException("Universal page="+page+" palette="+palette+" style="+style+" density="+density,e);}
            }
            background.recycle();
        });
        AppWidgetManager manager=AppWidgetManager.getInstance(c);int first=host.allocateAppWidgetId(),second=host.allocateAppWidgetId();
        check(manager.bindAppWidgetIdIfAllowed(first,new ComponentName(c,Widgets.provider("universal"))),"First universal widget binds");
        check(manager.bindAppWidgetIdIfAllowed(second,new ComponentName(c,Widgets.provider("universal"))),"Second universal widget binds");
        JSONObject config=Widgets.defaults("universal");config.put("style",1).put("transparency",50);Store.config(c,first,config);
        JSONObject other=Widgets.defaults("universal");other.put("page","clock");Store.config(c,second,other);Widgets.update(c,first,"universal");Widgets.update(c,second,"universal");
        ActivityMonitor appLaunches=addMonitor(MainActivity.class.getName(),null,false);
        try{
            for(String page:Widgets.KINDS){
                UniversalWidget.switchMenu(c,first,Widgets.index(page)/UniversalWidget.TOOLS_PER_PAGE);
                onMain(()->{AppWidgetHostView view=host.createView(c,first,manager.getAppWidgetInfo(first));TextView tile=findText(view,Widgets.shortTitle(page));check(tile!=null,"Picker contains "+page);((View)tile.getParent()).performClick();});
                waitForPage(c,first,page);
                onMain(()->{AppWidgetHostView view=host.createView(c,first,manager.getAppWidgetInfo(first));check(((TextView)view.findViewById(com.nook.widgets.R.id.hub_label)).getText().toString().equals(Widgets.shortTitle(page)),"Native universal widget displays "+page);});
                check(UniversalWidget.currentPage(Store.config(c,second)).equals("clock"),"A second widget keeps its own current view");
            }
            String last=Widgets.KINDS[Widgets.KINDS.length-1];UniversalWidget.switchPage(c,first,last);
            onMain(()->host.createView(c,first,manager.getAppWidgetInfo(first)).findViewById(com.nook.widgets.R.id.hub_next).performClick());waitForPage(c,first,"notes");
            onMain(()->host.createView(c,first,manager.getAppWidgetInfo(first)).findViewById(com.nook.widgets.R.id.hub_prev).performClick());waitForPage(c,first,last);
            onMain(()->host.createView(c,first,manager.getAppWidgetInfo(first)).findViewById(com.nook.widgets.R.id.hub_menu).performClick());waitForPage(c,first,"menu");
            check(appLaunches.getHits()==0,"Switching views does not open the app");
        }finally{removeMonitor(appLaunches);}
        check(!UniversalWidget.switchPage(c,first,"unsupported"),"Unknown views cannot be selected");
        check(UniversalWidget.currentPage(Store.config(c,first)).equals("menu"),"Invalid switching preserves the last view");
        config=Store.config(c,first);UniversalWidget.pageSettings(config,"timers").put("value0",7).put("value1",9).put("value2",11);Store.config(c,first,config);
        UniversalWidget.switchPage(c,first,"notes");Activity capture=openNoteFromWidget(c,host,first,"Temporary",main,activities);
        onMain(()->{View dialog=editorDialog(capture).getWindow().getDecorView();editWithHint(dialog,"Give it a title").setText("A note in my toolbox");findText(dialog,"Save note").performClick();});
        JSONObject notes=UniversalWidget.pageSettings(Store.config(c,first),"notes");String noteId=notes.optString("entity");created.add(new String[]{"notes",noteId});
        check(Store.find(c,"notes",noteId)!=null,"Note capture pins within the universal widget");
        check(Store.config(c,first).optInt("transparency")==50,"Note capture retains universal glass settings");
        check(UniversalWidget.pageSettings(Store.config(c,first),"timers").optInt("value0")==7,"Note capture does not overwrite timer shortcuts");
        NoteLife.archive(c,noteId);
        for(String page:new String[]{"timers","clock"}){
            UniversalWidget.switchPage(c,first,page);
            Activity editor=startActivitySync(new Intent(c,WidgetConfigActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,first).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));activities.add(editor);waitForIdleSync();
            onMain(()->{
                String label=page.equals("timers")?"Everyday · 1 / 5 / 10 min":"12-hour clock";TextView selected=findText(editor.getWindow().getDecorView(),label);check(selected!=null,"Universal editor exposes "+page+" settings");
                ((Spinner)selected.getParent()).setSelection(1);
            });waitForIdleSync();if(page.equals("timers"))screenshot("universal-timers");
            onMain(()->findText(editor.getWindow().getDecorView(),"Save widget").performClick());activities.remove(editor);
        }
        config=Store.config(c,first);
        check(UniversalWidget.pageSettings(config,"timers").optInt("variant")==1&&UniversalWidget.pageSettings(config,"clock").optInt("variant")==1,"Views retain independent preset selections");
        check(UniversalWidget.pageSettings(config,"notes").optString("entity").equals(noteId),"Other settings do not replace the selected note");
        check(UniversalWidget.viewConfig(config,"timers").optInt("palette")==config.optInt("palette"),"All views share the widget palette");
        UniversalWidget.switchPage(c,first,"menu");
        Activity chooser=startActivitySync(new Intent(c,WidgetConfigActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,first).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));activities.add(chooser);waitForIdleSync();screenshot("universal-menu");
        onMain(()->{findText(chooser.getWindow().getDecorView(),"Smoked").performClick();findSlider(chooser.getWindow().getDecorView()).setProgress(100);
            ImageView menuBackground=chooser.findViewById(com.nook.widgets.R.id.hub_bg0);check(menuBackground.getImageAlpha()==0,"Live slider updates chooser tile surfaces");findSlider(chooser.getWindow().getDecorView()).setProgress(50);
        });
        ActivityMonitor recreation=addMonitor(WidgetConfigActivity.class.getName(),null,false);
        onMain(()->{TextView selection=findText(chooser.getWindow().getDecorView(),"All tools · chooser");((Spinner)selection.getParent()).setSelection(Widgets.index("alarms")+1);});
        Activity rebuilt=recreation.waitForActivityWithTimeout(5000);removeMonitor(recreation);check(rebuilt!=null&&rebuilt!=chooser,"Changing the displayed view rebuilds its options");activities.remove(chooser);activities.add(rebuilt);waitForIdleSync();
        onMain(()->{check(findText(rebuilt.getWindow().getDecorView(),"Morning · 07:00 / 07:30 / 08:00")!=null,"The selected view exposes its own presets");check(findSlider(rebuilt.getWindow().getDecorView()).getProgress()==50,"Changing views preserves unsaved appearance choices");rebuilt.finish();});activities.remove(rebuilt);
        check(UniversalWidget.currentPage(Store.config(c,first)).equals("menu")&&Store.config(c,first).optInt("style")==1,"Canceling configuration leaves the widget unchanged");
        host.deleteAppWidgetId(first);host.deleteAppWidgetId(second);Store.removeConfig(c,first);Store.removeConfig(c,second);
    }
    @Override public void onStart() {
        if(sourceOnly){Bundle results=new Bundle();int code=Activity.RESULT_CANCELED;try{JSONObject feed=FeedData.parseFeed(FeedData.request("https://feeds.bbci.co.uk/news/rss.xml"));int count=feed.getJSONArray("items").length();check(count>0,"Live HTTPS RSS source returns usable stories");results.putString("stream","\nPASS: "+assertions+" checks. Live BBC HTTPS source returned "+count+" usable stories.\n");code=Activity.RESULT_OK;}catch(Throwable e){results.putString("stream","\nFAIL: "+android.util.Log.getStackTraceString(e));}finish(code,results);return;}
        if(libraryOnly||stockOnly||mediaOnly){Bundle results=new Bundle();AppWidgetHost libraryHost=new AppWidgetHost(getTargetContext(),8818);int code=Activity.RESULT_CANCELED;try{if(mediaOnly)MediaSmoke.run(this,getTargetContext(),libraryHost,this::check);else if(stockOnly)StockSmoke.run(this,getTargetContext(),libraryHost,this::check);else LibrarySmoke.run(this,getTargetContext(),libraryHost,this::check);results.putString("stream","\nPASS: "+assertions+" checks. Device-backed widget library integrations.\n");code=Activity.RESULT_OK;}catch(Throwable e){results.putString("stream","\nFAIL: "+android.util.Log.getStackTraceString(e));}finally{libraryHost.deleteHost();}finish(code,results);return;}
        Bundle result=new Bundle();List<Activity> activities=new ArrayList<>();int resultCode=Activity.RESULT_CANCELED;
        AppWidgetHost host=new AppWidgetHost(getTargetContext(),8818);
        try {
            Context c=getTargetContext();
            Store.Item note=add("notes","A little inspiration","More walks without a destination.\nMore ideas scribbled down.\nMore room for the little things.");
            check(Store.find(c,"notes",note.id).title.equals(note.title),"Note persistence");
            Store.Item task=add("checklist","Take a walk outside","");
            Store.toggle(c,"checklist",task.id);check(Store.find(c,"checklist",task.id).done,"Task completion");
            Store.toggle(c,"checklist",task.id);check(!Store.find(c,"checklist",task.id).done,"Task reopening");
            add("checklist","Read a few pages","");add("checklist","Make time for yourself","");
            Store.Item habit=add("habits","Drink more water","");habit.target=8;habit.day=LocalDate.now().minusDays(1).toString();habit.count=8;Store.save(c,"habits",habit);
            check(Store.find(c,"habits",habit.id).todayCount()==0,"Habits reset at date change");
            Store.increment(c,habit.id,1);check(Store.find(c,"habits",habit.id).todayCount()==1,"First check-in after reset");
            Store.increment(c,habit.id,-10);check(Store.find(c,"habits",habit.id).todayCount()==0,"Habit lower bound");
            Store.increment(c,habit.id,100);check(Store.find(c,"habits",habit.id).todayCount()==8,"Habit target bound");
            Store.increment(c,habit.id,-5);
            Store.Item reminder=add("reminders","Stretch & take a proper break","A small reset goes a long way.");reminder.due=System.currentTimeMillis()+3600000;Store.save(c,"reminders",reminder);Reminders.schedule(c,reminder);
            glassComparison();
            onMain(()->{
                Bitmap background=Bitmap.createBitmap(320,220,Bitmap.Config.ARGB_8888);
                for(String kind:Widgets.KINDS)for(int p=0;p<6;p++)for(int d=0;d<2;d++)for(int style=0;style<3;style++)for(int variant=0;variant<(kind.equals("timers")||kind.equals("alarms")?3:2);variant++) {
                    try {
                        JSONObject config=Widgets.defaults(kind);config.put("palette",p).put("density",d).put("variant",variant).put("style",style);
                        FrameLayout parent=new FrameLayout(c);
                        View view=Widgets.render(c,991,kind,config,false,false).apply(c,parent);
                        view.measure(View.MeasureSpec.makeMeasureSpec(Ui.dp(c,360),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Ui.dp(c,d==1?182:250),View.MeasureSpec.EXACTLY));
                        view.layout(0,0,view.getMeasuredWidth(),view.getMeasuredHeight());
                        check(view.findViewById(com.nook.widgets.R.id.widget_settings)!=null,"Widget settings: "+kind);
                        int alpha=surfacePixelAlpha(view,background);
                        int expected=style==0?255:style==1?191:204;
                        check(Math.abs(alpha-expected)<=1,"Default surface transparency: "+kind+" style="+style+" got="+alpha);
                    }catch(Exception e){throw new RuntimeException(kind+" palette="+p+" density="+d+" variant="+variant+" style="+style,e);}
                }
                int[] percentages={0,25,50,75,100},expectedAlpha={255,191,128,64,0};
                for(int style=1;style<=2;style++)for(int palette=0;palette<6;palette++)for(int n=0;n<percentages.length;n++) {
                    try {
                        JSONObject config=Widgets.defaults("timers");config.put("style",style).put("palette",palette).put("transparency",percentages[n]);
                        View view=Widgets.render(c,991,"timers",config,false,false).apply(c,new FrameLayout(c));
                        check(Math.abs(surfacePixelAlpha(view,background)-expectedAlpha[n])<=1,"Transparency pixels "+percentages[n]+"% palette="+palette+" style="+style);
                        for(int image:Widgets.BACKGROUND_IMAGES)check(((ImageView)view.findViewById(image)).getImageAlpha()==expectedAlpha[n],"Button and surface alpha "+percentages[n]+"%");
                        TextView title=view.findViewById(com.nook.widgets.R.id.widget_title);
                        check(title.getAlpha()==1f&&android.graphics.Color.alpha(title.getCurrentTextColor())==255,"Text remains visible at "+percentages[n]+"%");
                    }catch(Exception e){throw new RuntimeException(e);}
                }
                try {
                    JSONObject bounded=Widgets.defaults("notes");bounded.put("style",1).put("transparency",-25);check(Widgets.transparency(bounded)==0,"Negative transparency is clamped");
                    bounded.put("transparency",200);check(Widgets.transparency(bounded)==100,"High transparency is clamped");
                    bounded.put("style",0);check(Widgets.transparency(bounded)==0,"Solid ignores saved glass transparency");
                }catch(Exception e){throw new RuntimeException(e);}
                background.recycle();
            });
            JSONObject timerConfig=Widgets.defaults("timers");timerConfig.put("value0",0).put("value1",99999);
            int[] shortcuts=Widgets.shortcuts(timerConfig,true,0);check(shortcuts[0]==1&&shortcuts[1]==1440,"Timer shortcut bounds");
            Intent completion=new Intent().setAction("toggle").putExtra("type","checklist").putExtra("id",task.id);
            new WidgetActionReceiver().onReceive(c,completion);check(Store.find(c,"checklist",task.id).done,"Widget task completion");
            Store.toggle(c,"checklist",task.id);
            Activity main=startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));activities.add(main);waitForIdleSync();screenshot("today");
            onMain(()->{TextView widgets=findText(main.getWindow().getDecorView(),"Widgets");check(widgets!=null,"Widgets navigation");((View)widgets.getParent()).performClick();});screenshot("gallery");
            onMain(()->{TextView glass=findText(main.getWindow().getDecorView(),"Glass");check(glass!=null,"Glass collection navigation");glass.performClick();});screenshot("glass-gallery");
            universalLifecycle(c,host,main,activities);
            designLifecycle(c,host,activities);
            LibrarySmoke.run(this,c,host,this::check);
            StockSmoke.run(this,c,host,this::check);
            MediaSmoke.run(this,c,host,this::check);
            noteLifecycle(c,host,main,activities);
            for(String kind:Widgets.CATALOG) {
                Activity config=startActivitySync(new Intent(c,WidgetConfigActivity.class).putExtra("kind",kind).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));activities.add(config);waitForIdleSync();
                check(findText(config.getWindow().getDecorView(),"Make it yours")!=null,"Configuration opens: "+kind);
                check(findText(config.getWindow().getDecorView(),"Add to home screen  +")!=null,"New widget uses pin flow: "+kind);
                if(kind.equals("notes")||kind.equals("timers"))screenshot("configure-"+kind);
                if(kind.equals("notes")||kind.equals("timers")) {
                    onMain(()->findText(config.getWindow().getDecorView(),"Frosted").performClick());screenshot("glass-frosted-"+kind);
                    onMain(()->findText(config.getWindow().getDecorView(),"Smoked").performClick());screenshot("glass-smoked-"+kind);
                    if(kind.equals("timers")) {
                        onMain(()->{findText(config.getWindow().getDecorView(),"Frosted").performClick();SeekBar slider=findSlider(config.getWindow().getDecorView());check(slider!=null&&slider.getMax()==100,"Transparency slider range");slider.setProgress(50);
                            check(findText(config.getWindow().getDecorView(),"50%")!=null,"Live percentage label");
                            check(((ImageView)config.findViewById(com.nook.widgets.R.id.widget_surface)).getImageAlpha()==128,"Live preview updates to 50 percent");
                        });screenshot("transparency-slider");
                    }
                }
                onMain(config::finish);activities.remove(config);
            }
            // Exercise registered providers through Android's real widget service.
            AppWidgetManager manager=AppWidgetManager.getInstance(c);
            for(String kind:Widgets.CATALOG) {
                int id=host.allocateAppWidgetId();
                check(manager.bindAppWidgetIdIfAllowed(id,new ComponentName(c,Widgets.provider(kind))),"System binds provider: "+kind);
                onMain(()->{
                    Store.config(c,id,Widgets.defaults(kind));Widgets.update(c,id,kind);
                    AppWidgetHostView view=host.createView(c,id,manager.getAppWidgetInfo(id));
                    check(view.getAppWidgetId()==id,"Native widget host: "+kind);
                });
                Activity config=startActivitySync(new Intent(c,WidgetConfigActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));activities.add(config);waitForIdleSync();
                onMain(()->{
                    TextView swatch=findText(config.getWindow().getDecorView(),"Lilac");((View)swatch.getParent()).performClick();
                    findText(config.getWindow().getDecorView(),"Smoked").performClick();
                    SeekBar slider=findSlider(config.getWindow().getDecorView());slider.setProgress(0);
                    check(((ImageView)config.findViewById(com.nook.widgets.R.id.widget_surface)).getImageAlpha()==255,"Zero transparency is opaque");
                    slider.setProgress(100);check(((ImageView)config.findViewById(com.nook.widgets.R.id.widget_surface)).getImageAlpha()==0,"Full transparency hides the surface");
                    slider.setProgress(63);check(((ImageView)config.findViewById(com.nook.widgets.R.id.widget_surface)).getImageAlpha()==94,"Exact slider preview percentage");
                    TextView save=findText(config.getWindow().getDecorView(),"Save widget");check(save!=null,"Placed widget can be reconfigured: "+kind);save.performClick();
                });
                check(Store.config(c,id).optInt("palette",-1)==3,"Per-widget color persisted: "+kind);
                check(Store.config(c,id).optInt("style",-1)==2,"Per-widget glass style persisted: "+kind);
                check(Store.config(c,id).optInt("transparency",-1)==63,"Per-widget transparency persisted: "+kind);
                Activity reopened=startActivitySync(new Intent(c,WidgetConfigActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));activities.add(reopened);waitForIdleSync();
                onMain(()->{check(findSlider(reopened.getWindow().getDecorView()).getProgress()==63,"Transparency restored when reopening: "+kind);reopened.finish();});activities.remove(reopened);
                activities.remove(config);host.deleteAppWidgetId(id);Store.removeConfig(c,id);
            }
            // Verify actual notification delivery, rather than only scheduling calls.
            Reminders.channel(c);
            check(Reminders.notificationsAllowed(c),"Test device notification permission");
            new ReminderReceiver().onReceive(c,new Intent().putExtra("id",reminder.id));
            check(Store.find(c,"reminders",reminder.id).notified,"Reminder delivery recorded");
            boolean active=false;for(android.service.notification.StatusBarNotification notification:c.getSystemService(NotificationManager.class).getActiveNotifications())if(reminder.id.equals(notification.getTag()))active=true;
            check(active,"Reminder notification posted");
            Store.toggle(c,"reminders",reminder.id);check(Store.find(c,"reminders",reminder.id).done,"Complete delivered reminder");
            for(android.service.notification.StatusBarNotification notification:c.getSystemService(NotificationManager.class).getActiveNotifications())check(!reminder.id.equals(notification.getTag()),"Completion cancels notification");
            result.putString("stream","\nPASS: "+assertions+" checks. Dedicated and Universal widgets; eight design languages, typography, appearance persistence/reset, in-widget navigation, note capture, glass transparency, lifetimes, native bindings, reminders and device-backed library integrations.\n");
            result.putInt("assertions",assertions);
            resultCode=Activity.RESULT_OK;
        }catch(Throwable e){result.putString("stream","\nFAIL: "+android.util.Log.getStackTraceString(e));}
        finally {
            for(String[] item:created)Store.delete(getTargetContext(),item[0],item[1]);
            host.deleteHost();
            for(Activity activity:activities)onMain(activity::finish);
        }
        finish(resultCode,result);
    }
}
