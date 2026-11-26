package com.nook.widgets;

import android.app.*;
import android.content.*;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainActivity extends Activity {
    private LinearLayout root, body, nav;
    private ScrollView scroll;
    private int tab;
    private String category="notes", galleryFilter="All";
    private static final String[] LIBRARY={"notes","reminders","checklist","habits"};
    private Dialog editor;
    private EditText editingTitle, editingBody, editingTarget;
    private String editingType, editingId;
    private long editingDue;
    private NoteLifetimeEditor noteLifetime;
    private int editingNoteWidget=-1;
    private String notesFilter="Active";

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);Reminders.channel(this);
        NoteLife.refresh(this);
        if(saved!=null){tab=saved.getInt("tab");category=saved.getString("category","notes");galleryFilter=saved.getString("filter","All");notesFilter=saved.getString("notesFilter","Active");}
        render();route(getIntent());
        if(saved!=null && saved.containsKey("editorType")) {
            String type=saved.getString("editorType");
            Store.Item draft=Store.find(this,type,saved.getString("editorId",""));
            if(draft==null)draft=new Store.Item();draft.id=saved.getString("editorId",draft.id);
            draft.title=saved.getString("editorTitle","");draft.body=saved.getString("editorBody","");draft.target=saved.getInt("editorTarget",1);draft.due=saved.getLong("editorDue");
            showEditor(type,draft,null,saved.getInt("noteWidget",-1));if(noteLifetime!=null&&saved.containsKey("noteTemporary"))noteLifetime.restoreState(saved);
        }
    }
    @Override protected void onResume(){super.onResume();NoteLife.refresh(this);if(root!=null && (editor==null||!editor.isShowing()))renderPreservingScroll();if(noteLifetime!=null)noteLifetime.refresh();Reminders.restore(this);Widgets.updateAll(this);WeatherJob.schedule(this);}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);route(intent);}
    private void route(Intent i) {
        String section=i.getStringExtra("section");if(section==null)return;
        if(Arrays.asList(LIBRARY).contains(section)){tab=2;category=section;}else tab=0;
        render();
        String id=i.getStringExtra("edit");boolean create=i.getBooleanExtra("create",false);
        String noteMode=i.getStringExtra("noteMode");int noteWidget=i.getIntExtra("noteWidget",-1);
        i.removeExtra("noteMode");i.removeExtra("noteWidget");
        i.removeExtra("section");i.removeExtra("edit");i.removeExtra("create");
        if(Arrays.asList(LIBRARY).contains(section) && (create || (id!=null&&!id.isEmpty()))) {
            Store.Item item=id==null?null:Store.find(this,section,id);showEditor(section,item,noteMode,noteWidget);
        }
    }
    private void renderPreservingScroll(){int y=scroll==null?0:scroll.getScrollY();render();scroll.post(()->scroll.scrollTo(0,y));}
    private void render() {
        root=Ui.column(this);root.setBackgroundColor(Ui.BG);setContentView(root);Ui.insets(this,root);
        LinearLayout top=Ui.row(this);Ui.pad(top,24,8);
        LinearLayout brand=Ui.row(this);Ui.Icon logo=Ui.icon(this,"grid",Ui.ACCENT);Ui.add(brand,logo,32,42);
        TextView name=Ui.bold(this,"nook",25,Ui.TEXT);name.setLetterSpacing(-.04f);brand.addView(name);Ui.weight(top,brand);
        top.addView(Ui.iconButton(this,"settings","Settings and help",this::settings));root.addView(top);
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        body=Ui.column(this);Ui.pad(body,24,16);scroll.addView(body);
        if(tab==0)today();else if(tab==1)gallery();else library();
        Ui.gap(body,18);
        nav=Ui.row(this);Ui.pad(nav,8,5);Ui.surface(nav,Ui.SURFACE,26);
        String[] labels={"Today","Widgets","Library"},icons={"sun","grid","notes"};
        for(int n=0;n<3;n++) {
            final int selected=n;
            LinearLayout item=Ui.column(this);item.setGravity(Gravity.CENTER);
            Ui.Icon icon=Ui.icon(this,icons[n],tab==n?Ui.ACCENT:Ui.MUTED);Ui.add(item,icon,44,33);
            TextView text=Ui.bold(this,labels[n],11,tab==n?Ui.ACCENT:Ui.MUTED);text.setGravity(Gravity.CENTER);item.addView(text);
            if(tab==n)Ui.surface(item,0xff2a352c,20);Ui.click(item,()->{tab=selected;render();});
            nav.addView(item,new LinearLayout.LayoutParams(0,Ui.dp(this,58),1));
        }
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(Ui.dp(this,20),Ui.dp(this,5),Ui.dp(this,20),Ui.dp(this,12));root.addView(nav,p);
    }
    private void eyebrow(String text) {TextView t=Ui.bold(this,text,11,Ui.MUTED);t.setLetterSpacing(.13f);body.addView(t);Ui.gap(body,12);}
    private void pageTitle(String title,String subtitle) {
        TextView t=Ui.text(this,title,38,Ui.TEXT);t.setTypeface(android.graphics.Typeface.create("sans-serif-light",0));t.setLetterSpacing(-.035f);body.addView(t);Ui.gap(body,12);
        body.addView(Ui.text(this,subtitle,15,Ui.MUTED));Ui.gap(body,28);
    }
    private void section(String name,String action,Runnable run) {
        LinearLayout row=Ui.row(this);Ui.weight(row,Ui.bold(this,name,19,Ui.TEXT));
        if(action!=null){TextView button=Ui.bold(this,action,12,Ui.ACCENT);Ui.pad(button,10,14);Ui.click(button,run);row.addView(button);}
        body.addView(row);Ui.gap(body,12);
    }
    private void today() {
        eyebrow(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM",Locale.getDefault())).toUpperCase(Locale.getDefault()));
        pageTitle("Your day,\na little lighter.","The little things, all in one place.");
        LinearLayout hero=card(Ui.ACCENT,24);
        LinearLayout heading=Ui.row(this);Ui.weight(heading,Ui.bold(this,"MAKE YOURSELF AT HOME",10,0xff3e523c));Ui.add(heading,Ui.icon(this,"habits",0xff344b31),30,30);hero.addView(heading);Ui.gap(hero,12);
        hero.addView(Ui.text(this,"Good things come\nin little widgets.",28,Ui.BG));Ui.gap(hero,12);
        hero.addView(Ui.text(this,"Your notes, your routines, your kind of day.\nPick a widget and make it yours.",14,0xff3e523c));Ui.gap(hero,20);
        hero.addView(Ui.button(this,"Explore your widgets   →",0xff253527,0xffedf2e5,()->{tab=1;render();}));body.addView(hero);Ui.gap(body,28);
        section("A thought to keep","+ New",()->showEditor("notes",null));
        List<Store.Item> notes=NoteLife.activeNotes(this);Store.Item latest=notes.isEmpty()?null:notes.get(0);
        LinearLayout note=card(0xffe9dfc9,22);
        smallCardHeader(note,"notes","QUICK NOTE",0xff7b715c);
        note.addView(Ui.text(this,latest==null?"Catch that thought.":latest.title,23,0xff37372a));Ui.gap(note,10);
        TextView excerpt=Ui.text(this,latest==null?"An idea, a little reminder to yourself, or something you don’t want to forget.":latest.body,14,0xff78715e);excerpt.setMaxLines(3);excerpt.setEllipsize(android.text.TextUtils.TruncateAt.END);note.addView(excerpt);
        Ui.click(note,()->showEditor("notes",latest));body.addView(note);Ui.gap(body,28);
        section("A moment for you","Custom",this::customTimer);
        LinearLayout timer=card(Ui.SURFACE,20);smallCardHeader(timer,"timers","QUICK TIMERS",Ui.MUTED);
        LinearLayout timerButtons=Ui.row(this);
        int[] minutes={5,15,25};String[] captions={"Little break","Reset","Focus"};
        for(int n=0;n<3;n++) {
            final int value=minutes[n];LinearLayout option=Ui.column(this);option.setGravity(Gravity.CENTER);Ui.pad(option,4,16);Ui.surface(option,0xff2b372e,20);
            TextView number=Ui.text(this,String.valueOf(value),30,Ui.ACCENT);number.setGravity(Gravity.CENTER);option.addView(number);
            TextView desc=Ui.text(this,"min · "+captions[n],10,Ui.MUTED);desc.setGravity(Gravity.CENTER);option.addView(desc);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);if(n<2)p.rightMargin=Ui.dp(this,8);timerButtons.addView(option,p);Ui.click(option,()->QuickActionActivity.launch(this,"timer",value));
        }
        timer.addView(timerButtons);Ui.gap(timer,12);timer.addView(Ui.text(this,"Rings through your phone’s Clock app.",11,Ui.MUTED));body.addView(timer);Ui.gap(body,28);
        section("Coming up","+ Reminder",()->showEditor("reminders",null));
        boolean any=false;
        for(Store.Item item:Store.list(this,"reminders"))if(!item.done){body.addView(itemCard("reminders",item));any=true;break;}
        if(!any)emptyCard("reminders","A little breathing room.","No upcoming reminders. Add one when you need a nudge.",()->showEditor("reminders",null));
        Ui.gap(body,28);section("Small steps","View habits",()->{tab=2;category="habits";render();});
        List<Store.Item> habits=Store.list(this,"habits");
        if(habits.isEmpty())emptyCard("habits","Build a tiny ritual.","Drink water. Read a page. Take a walk. Start small.",()->showEditor("habits",null));
        else body.addView(itemCard("habits",habits.get(0)));
    }
    private LinearLayout card(int color,int padding) {LinearLayout l=Ui.column(this);Ui.pad(l,padding,padding);Ui.surface(l,color,26);l.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));return l;}
    private void smallCardHeader(LinearLayout parent,String icon,String label,int color) {
        LinearLayout h=Ui.row(this);Ui.add(h,Ui.icon(this,icon,color),26,26);TextView t=Ui.bold(this,label,10,color);t.setLetterSpacing(.12f);t.setPadding(Ui.dp(this,7),0,0,0);h.addView(t);parent.addView(h);Ui.gap(parent,14);
    }
    private void gallery() {
        eyebrow("LITTLE TOOLS. LOTS OF POSSIBILITY.");pageTitle("Meet your\nhome screen.","Pick a dedicated widget or your whole toolbox.");
        HorizontalScrollView filters=new HorizontalScrollView(this);filters.setHorizontalScrollBarEnabled(false);LinearLayout chips=Ui.row(this);
        for(String label:new String[]{"All","Motion","Video","Liquid Glass","Samsung","Nothing","Apple","macOS","Pixel","Designs","Universal","Dedicated","Glass","Essentials","Focus","Everyday"}) {TextView chip=Ui.button(this,label,galleryFilter.equals(label)?Ui.ACCENT:Ui.SURFACE,galleryFilter.equals(label)?Ui.BG:Ui.MUTED,()->{galleryFilter=label;render();});chips.addView(chip);LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)chip.getLayoutParams();p.rightMargin=Ui.dp(this,8);}
        filters.addView(chips);body.addView(filters);Ui.gap(body,26);
        if(galleryFilter.equals("Designs")){designGallery();return;}
        if(collectionDesign()>=0||galleryFilter.equals("Liquid Glass")){String brand=galleryFilter.equals("Liquid Glass")?"Apple":galleryFilter;body.addView(Ui.text(this,"Named stock families and size/layout variants. Each family identifies whether it uses Android data, your own log, a connected source, or an app shortcut.",14,Ui.MUTED));Ui.gap(body,18);int count=StockCatalog.families(brand).size();LinearLayout hero=Ui.column(this);hero.setBackgroundResource(R.drawable.preview_wallpaper);Ui.pad(hero,14,14);org.json.JSONObject glass=Widgets.defaults("clock");WidgetDesign.select(glass,StockCatalog.design(brand));ExtraData.put(glass,"style",3);ExtraData.put(glass,"_width",300);ExtraData.put(glass,"_height",220);hero.addView(Widgets.render(this,-1,"clock",glass,false,true).apply(this,hero),new LinearLayout.LayoutParams(-1,Ui.dp(this,220)));body.addView(hero);Ui.gap(body,20);body.addView(Ui.button(this,"Browse "+brand+" · "+count+" families",Ui.ACCENT,Ui.BG,()->startActivity(new Intent(this,StockLibraryActivity.class).putExtra("brand",brand).putExtra("liquid",galleryFilter.equals("Liquid Glass")||brand.equals("Apple")||brand.equals("macOS")))));Ui.gap(body,20);body.addView(Ui.text(this,"Liquid Glass is a separate clear/tinted material with optical edges and optional wallpaper blur/refraction. It is available on all widget families.",13,Ui.MUTED));return;}
        for(String kind:Widgets.CATALOG) {
            boolean universal=kind.equals("universal"),focus=kind.equals("timers")||kind.equals("alarms"),essentials=kind.equals("notes")||kind.equals("reminders")||kind.equals("checklist");
            if(galleryFilter.equals("Motion")&&!MediaWidgets.supports(kind)||galleryFilter.equals("Video")&&!kind.equals("video"))continue;
            if(galleryFilter.equals("Universal")&&!universal||galleryFilter.equals("Dedicated")&&universal||galleryFilter.equals("Focus")&&!focus || galleryFilter.equals("Essentials")&&!essentials || galleryFilter.equals("Everyday")&&(focus||essentials||universal))continue;
            int index=Widgets.index(kind);LinearLayout item=Ui.column(this);
            LinearLayout preview=Ui.column(this);int height=universal?330:kind.equals("timers")||kind.equals("alarms")?184:kind.equals("clock")?248:236;
            org.json.JSONObject sampleConfig=Widgets.defaults(kind);
            int collection=collectionDesign();if(collection>=0)WidgetDesign.select(sampleConfig,collection);
            if(galleryFilter.equals("Glass")) {
                try{sampleConfig.put("style",index%2==0?1:2);}catch(org.json.JSONException e){throw new IllegalStateException(e);}
                preview.setBackgroundResource(R.drawable.preview_wallpaper);Ui.pad(preview,14,14);preview.setClipToOutline(true);
                preview.setContentDescription("Glass widget preview over a sample wallpaper");
            }
            View sample=Widgets.render(this,-1,kind,sampleConfig,false,true).apply(this,preview);
            preview.addView(sample,new LinearLayout.LayoutParams(-1,Ui.dp(this,height)));item.addView(preview);Ui.gap(item,16);
            LinearLayout label=Ui.row(this);LinearLayout copy=Ui.column(this);copy.addView(Ui.bold(this,Widgets.title(kind),19,Ui.TEXT));Ui.gap(copy,5);copy.addView(Ui.text(this,Widgets.description(kind),12,Ui.MUTED));Ui.weight(label,copy);
            label.addView(Ui.iconButton(this,"plus","Customize and add "+Widgets.title(kind),()->configure(kind)));item.addView(label);Ui.gap(item,10);
            LinearLayout options=Ui.row(this);
            for(int p=0;p<6;p++){TextView swatch=Ui.text(this,"●",16,Widgets.BACKGROUNDS[p]);swatch.setPadding(0,0,Ui.dp(this,5),0);options.addView(swatch);}
            options.addView(Ui.text(this,"  8 designs · 6 colors · 4 surfaces",11,Ui.MUTED));item.addView(options);
            Ui.click(preview,()->configure(kind));body.addView(item);Ui.gap(body,30);
        }
        LinearLayout tip=card(Ui.SURFACE,20);tip.addView(Ui.bold(this,"A home screen that feels like you.",16,Ui.ACCENT));Ui.gap(tip,8);tip.addView(Ui.text(this,"Add several copies of any widget and style each one differently. Pin different notes or habits, and mix your favorite timer presets.",13,Ui.MUTED));body.addView(tip);
    }
    private void designGallery(){
        body.addView(Ui.text(this,"Choose a starting point. Every design works with all "+Widgets.KINDS.length+" tools and the Universal widget.",14,Ui.MUTED));Ui.gap(body,22);
        for(int n=0;n<WidgetDesign.NAMES.length;n++){
            final int design=n;String kind=n==7?"battery":n==1||n==4?"clock":n==6?"timers":"notes";LinearLayout item=Ui.column(this);org.json.JSONObject config=Widgets.defaults(kind);WidgetDesign.select(config,n);
            int height=kind.equals("timers")?184:250;try{config.put("_height",height-28).put("_width",Math.round(getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density)-76);}catch(Exception e){throw new IllegalStateException(e);}
            FrameLayout preview=new FrameLayout(this);if(config.optInt("style")!=0){preview.setBackgroundResource(R.drawable.preview_wallpaper);Ui.pad(preview,14,14);}
            preview.addView(Widgets.render(this,-1,kind,config,false,true).apply(this,preview),new FrameLayout.LayoutParams(-1,-1));item.addView(preview,new LinearLayout.LayoutParams(-1,Ui.dp(this,height)));Ui.gap(item,14);
            item.addView(Ui.bold(this,WidgetDesign.NAMES[n],19,Ui.TEXT));Ui.gap(item,6);item.addView(Ui.text(this,WidgetDesign.DESCRIPTIONS[n],13,Ui.MUTED));Ui.gap(item,12);
            Runnable choose=()->startActivity(new Intent(this,WidgetConfigActivity.class).putExtra("kind",kind).putExtra("design",design));
            item.addView(Ui.button(this,"Try this design  +",Ui.SURFACE,Ui.ACCENT,choose));Ui.click(preview,choose);body.addView(item);Ui.gap(body,30);
        }
    }
    private int collectionDesign(){return switch(galleryFilter){case "Samsung"->7;case "Nothing"->1;case "Apple"->2;case "macOS"->3;case "Pixel"->4;default->-1;};}
    private void configure(String kind){Intent intent=new Intent(this,WidgetConfigActivity.class).putExtra("kind",kind).putExtra("style",galleryFilter.equals("Glass")?(Widgets.index(kind)%2==0?1:2):0);if(collectionDesign()>=0)intent.putExtra("design",collectionDesign());startActivity(intent);}
    private void library() {
        eyebrow("YOUR LITTLE COLLECTION");pageTitle("Room for\nwhat matters.","Keep your thoughts and everyday rituals close.");
        HorizontalScrollView filters=new HorizontalScrollView(this);filters.setHorizontalScrollBarEnabled(false);LinearLayout tabs=Ui.row(this);
        for(String type:LIBRARY) {
            String title=switch(type){case "notes"->"Notes";case "reminders"->"Reminders";case "checklist"->"Checklist";default->"Habits";};
            TextView chip=Ui.button(this,title,category.equals(type)?Ui.ACCENT:Ui.SURFACE,category.equals(type)?Ui.BG:Ui.MUTED,()->{category=type;render();});tabs.addView(chip);((LinearLayout.LayoutParams)chip.getLayoutParams()).rightMargin=Ui.dp(this,8);
        }
        filters.addView(tabs);body.addView(filters);Ui.gap(body,20);
        if(category.equals("notes")){
            LinearLayout states=Ui.row(this);
            for(String state:new String[]{"Active","Saved","All"}){
                TextView option=Ui.button(this,state,notesFilter.equals(state)?0xff344331:Ui.SURFACE,notesFilter.equals(state)?Ui.ACCENT:Ui.MUTED,()->{notesFilter=state;render();});
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.rightMargin=Ui.dp(this,6);states.addView(option,p);
            }
            body.addView(states);Ui.gap(body,12);
            if(notesFilter.equals("Saved")){body.addView(Ui.text(this,"Expired and archived notes. Open one to restore it with a new lifetime.",13,Ui.MUTED));Ui.gap(body,14);}
        }
        String singular=singular(category);
        body.addView(Ui.button(this,"+  New "+singular,Ui.SURFACE,Ui.ACCENT,()->showEditor(category,null)));Ui.gap(body,20);
        if(category.equals("reminders")) {
            if(!Reminders.notificationsAllowed(this)||!Reminders.precise(this)) {
                LinearLayout permissions=card(0xff302e22,18);permissions.addView(Ui.bold(this,"Help your reminders reach you",15,0xffead9a1));Ui.gap(permissions,8);
                permissions.addView(Ui.text(this,!Reminders.notificationsAllowed(this)?"Allow notifications to receive your reminder alerts.":"Allow precise timing so reminders arrive when you expect them. Otherwise Android may delay them.",12,0xffc1b68e));
                Ui.gap(permissions,10);permissions.addView(Ui.button(this,"Set up reminders",0xff47412c,0xfff3e4b6,this::reminderSettings));body.addView(permissions);Ui.gap(body,18);
            }
        }
        List<Store.Item> items=Store.list(this,category);
        if(category.equals("notes")&&!notesFilter.equals("All"))items.removeIf(item->notesFilter.equals("Active")?!NoteLife.active(this,item):NoteLife.active(this,item));
        if(category.equals("checklist"))items.sort(Comparator.comparing((Store.Item i)->i.done).thenComparingLong(i->i.created));
        if(items.isEmpty()) {
            Ui.gap(body,28);Ui.Icon icon=Ui.icon(this,category,Ui.ACCENT);LinearLayout frame=Ui.column(this);frame.setGravity(Gravity.CENTER);Ui.add(frame,icon,86,86);body.addView(frame);Ui.gap(body,20);
            boolean savedNotes=category.equals("notes")&&notesFilter.equals("Saved");
            TextView title=Ui.text(this,savedNotes?"Nothing saved away yet.":"Your first "+singular+" starts here.",23,Ui.TEXT);title.setGravity(Gravity.CENTER);body.addView(title);Ui.gap(body,12);
            TextView desc=Ui.text(this,savedNotes?"Expired and archived notes will appear here.":"Add something small. It’ll be ready for your home screen, too.",14,Ui.MUTED);desc.setGravity(Gravity.CENTER);body.addView(desc);
        } else for(Store.Item item:items){body.addView(itemCard(category,item));Ui.gap(body,12);}
    }
    private View itemCard(String type,Store.Item item) {
        LinearLayout card=card(Ui.SURFACE,18);LinearLayout row=Ui.row(this);
        if(type.equals("checklist")||type.equals("reminders")) {
            TextView done=Ui.text(this,item.done?"✓":"○",26,item.done?Ui.ACCENT:Ui.MUTED);done.setGravity(Gravity.CENTER);done.setContentDescription((item.done?"Reopen ":"Mark complete: ")+item.title);
            Ui.add(row,done,48,48);Ui.click(done,()->{Store.toggle(this,type,item.id);renderPreservingScroll();});
        }
        LinearLayout copy=Ui.column(this);TextView title=Ui.bold(this,item.title,17,item.done?Ui.MUTED:Ui.TEXT);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if(item.done)title.setPaintFlags(title.getPaintFlags()|Paint.STRIKE_THRU_TEXT_FLAG);copy.addView(title);
        if(type.equals("notes")){Ui.gap(copy,7);copy.addView(Ui.text(this,NoteLife.status(this,item),12,NoteLife.active(this,item)?Ui.ACCENT:Ui.MUTED));if(!item.body.isEmpty()){Ui.gap(copy,7);TextView excerpt=Ui.text(this,item.body,13,Ui.MUTED);excerpt.setMaxLines(2);excerpt.setEllipsize(android.text.TextUtils.TruncateAt.END);copy.addView(excerpt);}}
        else if(type.equals("reminders")){Ui.gap(copy,7);copy.addView(Ui.text(this,Widgets.when(item.due),12,item.due<System.currentTimeMillis()&&!item.done?0xffe6b49c:Ui.MUTED));}
        else if(type.equals("habits")){Ui.gap(copy,7);copy.addView(Ui.text(this,item.todayCount()+" of "+item.target+" today"+(item.todayCount()>=item.target?" · Done!":""),12,Ui.MUTED));}
        else if(!item.body.trim().isEmpty()){Ui.gap(copy,7);TextView excerpt=Ui.text(this,item.body,13,Ui.MUTED);excerpt.setMaxLines(3);excerpt.setEllipsize(android.text.TextUtils.TruncateAt.END);copy.addView(excerpt);}
        Ui.weight(row,copy);Ui.click(copy,()->showEditor(type,item));
        if(type.equals("habits"))row.addView(Ui.iconButton(this,"plus","Increase "+item.title,()->{Store.increment(this,item.id,1);renderPreservingScroll();}));
        else {TextView more=Ui.text(this,"···",24,Ui.MUTED);more.setGravity(Gravity.CENTER);more.setContentDescription("Edit "+item.title);Ui.add(row,more,44,48);Ui.click(more,()->showEditor(type,item));}
        card.addView(row);
        if(type.equals("habits")){Ui.gap(card,14);ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(item.target);bar.setProgress(item.todayCount());bar.setProgressTintList(android.content.res.ColorStateList.valueOf(Ui.ACCENT));card.addView(bar,new LinearLayout.LayoutParams(-1,Ui.dp(this,5)));}
        return card;
    }
    private void emptyCard(String icon,String title,String subtitle,Runnable action) {
        LinearLayout l=card(Ui.SURFACE,20);smallCardHeader(l,icon,"A LITTLE SPACE",Ui.MUTED);l.addView(Ui.bold(this,title,19,Ui.TEXT));Ui.gap(l,8);l.addView(Ui.text(this,subtitle,13,Ui.MUTED));Ui.click(l,action);body.addView(l);
    }
    private static String singular(String type){return switch(type){case "notes"->"note";case "reminders"->"reminder";case "checklist"->"task";default->"habit";};}
    private EditText input(String hint,String value,boolean multiline) {
        EditText e=new EditText(this);e.setTextColor(Ui.TEXT);e.setHintTextColor(Ui.MUTED);e.setTextSize(16);e.setHint(hint);e.setText(value);e.setGravity(Gravity.TOP);
        e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|(multiline?InputType.TYPE_TEXT_FLAG_MULTI_LINE:0));e.setSingleLine(!multiline);
        e.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(multiline?10000:150)});
        Ui.pad(e,16,15);Ui.surface(e,0xff29332b,16);if(multiline)e.setMinLines(4);return e;
    }
    private void showEditor(String type,Store.Item original) {
        showEditor(type,original,null,-1);
    }
    private void showEditor(String type,Store.Item original,String noteMode,int noteWidget) {
        if(editor!=null&&editor.isShowing())editor.dismiss();
        Store.Item item=original==null?new Store.Item():original;
        editingType=type;editingId=item.id;editingDue=item.due>0?item.due:System.currentTimeMillis()+3600000;
        editingNoteWidget=noteWidget;noteLifetime=null;
        Dialog dialog=new Dialog(this);editor=dialog;
        LinearLayout panel=Ui.column(this);Ui.pad(panel,24,24);Ui.surface(panel,Ui.SURFACE,28);
        ScrollView sheet=new ScrollView(this);sheet.setFillViewport(false);sheet.addView(panel);dialog.setContentView(sheet);
        LinearLayout header=Ui.row(this);Ui.weight(header,Ui.bold(this,(original==null?"A new ":"Your ")+singular(type),25,Ui.TEXT));
        TextView close=Ui.button(this,"Close",Ui.SURFACE,Ui.MUTED,dialog::dismiss);header.addView(close);panel.addView(header);Ui.gap(panel,20);
        if(type.equals("notes")){noteLifetime=new NoteLifetimeEditor(this,item,noteMode);panel.addView(noteLifetime);}
        editingTitle=input(type.equals("habits")?"e.g. Drink water":"Give it a title",item.title,false);panel.addView(editingTitle);Ui.gap(panel,12);
        editingBody=null;editingTarget=null;
        if(type.equals("notes")||type.equals("reminders")) {editingBody=input(type.equals("notes")?"What’s on your mind?":"A little extra detail (optional)",item.body,true);panel.addView(editingBody);Ui.gap(panel,16);}
        if(type.equals("reminders")) {
            TextView time=Ui.button(this,Widgets.when(editingDue),0xff303c32,Ui.ACCENT,()->{});
            Ui.click(time,()->{
                ZonedDateTime current=Instant.ofEpochMilli(editingDue).atZone(ZoneId.systemDefault());
                DatePickerDialog datePicker=new DatePickerDialog(this,(date,y,m,d)->new TimePickerDialog(this,(picker,h,min)->{
                    editingDue=LocalDateTime.of(y,m+1,d,h,min).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();time.setText(Widgets.when(editingDue));
                },current.getHour(),current.getMinute(),false).show(),current.getYear(),current.getMonthValue()-1,current.getDayOfMonth());
                datePicker.getDatePicker().setMinDate(System.currentTimeMillis()-1000);datePicker.show();
            });panel.addView(time);Ui.gap(panel,12);
            panel.addView(Ui.text(this,"Your reminder will appear as a notification.",12,Ui.MUTED));Ui.gap(panel,16);
        }
        if(type.equals("habits")) {
            panel.addView(Ui.text(this,"Daily target",12,Ui.MUTED));Ui.gap(panel,8);
            editingTarget=input("e.g. 8",String.valueOf(item.target),false);editingTarget.setInputType(InputType.TYPE_CLASS_NUMBER);panel.addView(editingTarget);Ui.gap(panel,12);
            panel.addView(Ui.text(this,"Your count starts fresh each day. Tap + in the widget to make progress.",12,Ui.MUTED));Ui.gap(panel,12);
            if(original!=null) {panel.addView(Ui.button(this,"Undo one check-in today",0xff29332b,Ui.MUTED,()->{Store.increment(this,item.id,-1);dialog.dismiss();renderPreservingScroll();}));Ui.gap(panel,12);}
        }
        panel.addView(Ui.button(this,type.equals("notes")&&original!=null&&!NoteLife.active(this,item)?"Restore note":"Save "+singular(type),Ui.ACCENT,Ui.BG,()->{
            String title=editingTitle.getText().toString().trim();if(title.isEmpty()){editingTitle.setError("Add a title");return;}
            if(type.equals("reminders")&&editingDue<=System.currentTimeMillis()){Toast.makeText(this,"Choose a time in the future",Toast.LENGTH_LONG).show();return;}
            if(type.equals("notes")&&!noteLifetime.apply(item))return;
            if(type.equals("habits")) {
                int target;try{target=Integer.parseInt(editingTarget.getText().toString());}catch(Exception e){target=0;}
                if(target<1||target>999){editingTarget.setError("Choose 1–999");return;}item.target=target;item.count=Math.min(item.count,target);
            }
            item.title=title;if(editingBody!=null)item.body=editingBody.getText().toString().trim();
            if(type.equals("reminders")){Reminders.cancel(this,item.id);item.due=editingDue;item.notified=false;}
            try {Store.save(this,type,item);if(type.equals("reminders")&&!item.done)Reminders.schedule(this,item);
                if(type.equals("notes")&&editingNoteWidget>0)UniversalWidget.attachNote(this,editingNoteWidget,item.id);
            }
            catch(IllegalStateException e){Toast.makeText(this,"Couldn’t save. Check your phone’s free storage and try again.",Toast.LENGTH_LONG).show();return;}
            if(type.equals("notes"))notesFilter="Active";
            dialog.dismiss();renderPreservingScroll();Toast.makeText(this,"Saved to your little collection",Toast.LENGTH_SHORT).show();
            if(type.equals("reminders")&&!Reminders.notificationsAllowed(this))Reminders.permissions(this);
        }));
        if(original!=null && Store.find(this,type,item.id)!=null) {
            if(type.equals("notes")&&NoteLife.active(this,item)){
                Ui.gap(panel,12);LinearLayout actions=Ui.row(this);
                if(item.expiresAt>0){Ui.weight(actions,Ui.button(this,"+1 hour",0xff29332b,Ui.ACCENT,()->noteLifetime.extendHour()));Ui.weight(actions,Ui.button(this,"Keep forever",0xff29332b,Ui.ACCENT,()->noteLifetime.keepPermanently()));}
                Ui.weight(actions,Ui.button(this,"Archive",0xff29332b,Ui.MUTED,()->{String title=editingTitle.getText().toString().trim();if(title.isEmpty()){editingTitle.setError("Add a title");return;}item.title=title;item.body=editingBody.getText().toString().trim();item.archived=true;Store.save(this,"notes",item);notesFilter="Saved";dialog.dismiss();renderPreservingScroll();}));panel.addView(actions);
            }
            Ui.gap(panel,12);panel.addView(Ui.button(this,"Delete "+singular(type),Ui.SURFACE,0xffdcae9e,()->new AlertDialog.Builder(this).setTitle("Delete this "+singular(type)+"?")
                .setMessage("This will also remove it from your widgets.").setNegativeButton("Keep",null).setPositiveButton("Delete",(d,w)->{Store.delete(this,type,item.id);dialog.dismiss();renderPreservingScroll();}).show()));
        }
        dialog.setOnDismissListener(d->{editingType=null;editingTitle=null;editingBody=null;editingTarget=null;noteLifetime=null;editingNoteWidget=-1;});
        Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.setLayout(-1,-2);window.setGravity(Gravity.BOTTOM);window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}
        dialog.show();if(window!=null)window.setLayout(-1,-2);
    }
    private void customTimer() {
        EditText minutes=input("Minutes (1–1440)","10",false);minutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        LinearLayout content=Ui.column(this);Ui.pad(content,24,12);content.addView(minutes);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("A timer for your moment").setView(content).setNegativeButton("Cancel",null).setPositiveButton("Start timer",null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{int value;try{value=Integer.parseInt(minutes.getText().toString());}catch(Exception e){value=0;}if(value<1||value>1440){minutes.setError("Use 1–1440 minutes");return;}QuickActionActivity.launch(this,"timer",value);dialog.dismiss();}));dialog.show();
    }
    private void reminderSettings(){new AlertDialog.Builder(this).setTitle("Your reminder settings").setItems(new String[]{(Reminders.notificationsAllowed(this)?"✓ ":"○ ")+"Notifications",(Reminders.precise(this)?"✓ ":"○ ")+"Precise timing"},(d,n)->{if(n==0)Reminders.permissions(this);else Reminders.exactPermission(this);}).setNegativeButton("Done",null).show();}
    private void settings() {
        new AlertDialog.Builder(this).setTitle("Your Nook").setItems(new String[]{"Set an alarm","Reminder permissions","How to add widgets","About & privacy"},(d,n)->{
            if(n==0)new TimePickerDialog(this,(picker,h,m)->QuickActionActivity.launch(this,"alarm",h*60+m),7,0,false).show();
            if(n==1)reminderSettings();
            if(n==2)new AlertDialog.Builder(this).setTitle("A little home screen magic").setMessage("1. Open the Widgets tab in Nook.\n\n2. Pick a widget, choose its colors and options, then tap Add to home screen.\n\n3. Confirm placement with your launcher.\n\nYou can also long-press an empty spot on your home screen → Widgets → Nook.\n\nTap ··· on any widget to customize it again. Add more than one copy for different notes, habits, or shortcuts.").setPositiveButton("Got it",null).show();
            if(n==3)new AlertDialog.Builder(this).setTitle("Little tools. A lighter day.").setMessage("Nook 0.12.0\n\nEight designs. Thirty-seven dedicated tool families. Redesigned music controls. Native animations and full-length silent inline video. Stock-library presets. Liquid Glass. One switchable toolbox.\n\nNotes, settings, selected photos, calendar data and media metadata stay on this phone. No ads or analytics. Weather uses Open-Meteo with a city you select. Optional RSS and market connections contact their chosen providers. Web search and translation open their providers. Calendar, media, usage and microphone access are optional. Private vendor services are represented only as explicitly labeled manual cards or app shortcuts.\n\nTimers and alarms ring through your Clock app. Favorite contacts open the dialer without placing calls. Uninstalling clears Nook's local data.").setPositiveButton("Lovely",null).show();
        }).setNegativeButton("Close",null).show();
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code==10){Reminders.restore(this);renderPreservingScroll();}}
    @Override public void onSaveInstanceState(Bundle out) {
        out.putInt("tab",tab);out.putString("category",category);out.putString("filter",galleryFilter);
        out.putString("notesFilter",notesFilter);out.putInt("noteWidget",editingNoteWidget);if(noteLifetime!=null)noteLifetime.saveState(out);
        if(editingType!=null&&editingTitle!=null){out.putString("editorType",editingType);out.putString("editorId",editingId);out.putString("editorTitle",editingTitle.getText().toString());out.putString("editorBody",editingBody==null?"":editingBody.getText().toString());out.putLong("editorDue",editingDue);try{out.putInt("editorTarget",editingTarget==null?1:Integer.parseInt(editingTarget.getText().toString()));}catch(Exception e){out.putInt("editorTarget",1);}}
        super.onSaveInstanceState(out);
    }
}
