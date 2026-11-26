package com.nook.widgets;

import android.app.*;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.time.*;

/** Lifetime choices shared by new, edited and recovered notes. */
public final class NoteLifetimeEditor extends LinearLayout {
    private final Activity activity;
    private final Store.Item original;
    private boolean temporary,changed;
    private long duration,until;
    private final LinearLayout options;
    private final TextView permanentButton,temporaryButton,summary,permission;
    public NoteLifetimeEditor(Activity a,Store.Item item,String initialMode){
        super(a);activity=a;original=item;setOrientation(VERTICAL);
        temporary=item.expiresAt>0||"temporary".equals(initialMode);until=item.expiresAt;
        if(temporary&&(until==0||NoteLife.remaining(a,item)<=0)){duration=NoteLife.HOUR;changed=true;}
        TextView title=Ui.bold(a,"NOTE LIFETIME",11,Ui.MUTED);title.setLetterSpacing(.12f);addView(title);Ui.gap(this,10);
        LinearLayout mode=Ui.row(a);
        permanentButton=Ui.button(a,"Permanent",Ui.SURFACE,Ui.TEXT,()->{temporary=false;changed=true;duration=0;until=0;refresh();});
        temporaryButton=Ui.button(a,"Temporary",Ui.SURFACE,Ui.TEXT,()->{temporary=true;if(until==0&&duration==0){duration=NoteLife.HOUR;changed=true;}refresh();});
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,-2,1);left.rightMargin=Ui.dp(a,8);mode.addView(permanentButton,left);mode.addView(temporaryButton,new LinearLayout.LayoutParams(0,-2,1));addView(mode);Ui.gap(this,12);
        options=Ui.column(a);
        String[] labels={"15 min","1 hour","6 hours","Today","Duration…","Date & time…"};
        for(int row=0;row<2;row++){
            LinearLayout line=Ui.row(a);
            for(int col=0;col<3;col++){
                final int n=row*3+col;
                TextView button=Ui.button(a,labels[n],0xff29332b,Ui.ACCENT,()->{
                    if(n<3){duration=new long[]{15*60000L,NoteLife.HOUR,6*NoteLife.HOUR}[n];until=0;changed=true;refresh();}
                    if(n==3){until=NoteLife.endToday();duration=0;changed=true;refresh();}
                    if(n==4)customDuration();if(n==5)pickDate();
                });button.setTextSize(12);Ui.pad(button,5,12);
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);if(col<2)p.rightMargin=Ui.dp(a,6);line.addView(button,p);
            }
            options.addView(line);Ui.gap(options,8);
        }
        addView(options);summary=Ui.text(a,"",13,Ui.ACCENT);addView(summary);Ui.gap(this,8);
        addView(Ui.text(a,"Expired and archived notes stay in Library → Notes → Saved. You can restore them anytime.",12,Ui.MUTED));Ui.gap(this,10);
        permission=Ui.button(a,"Allow precise expiry",0xff3d3927,0xffead9a1,()->Reminders.exactPermission(a));addView(permission);Ui.gap(this,18);refresh();
    }
    public void refresh(){
        Ui.surface(permanentButton,temporary?0xff29332b:Ui.ACCENT,16);permanentButton.setTextColor(temporary?Ui.TEXT:Ui.BG);permanentButton.setSelected(!temporary);
        Ui.surface(temporaryButton,temporary?Ui.ACCENT:0xff29332b,16);temporaryButton.setTextColor(temporary?Ui.BG:Ui.TEXT);temporaryButton.setSelected(temporary);
        options.setVisibility(temporary?VISIBLE:GONE);
        long displayed=duration>0?System.currentTimeMillis()+duration:changed?until:NoteLife.displayDeadline(activity,original);
        summary.setText(temporary?(duration>0?"For "+(duration/60000)+" minutes after saving · ":"Until ")+Widgets.when(displayed):"Stays on your home screen until you archive or delete it.");
        permission.setVisibility(temporary&&!Reminders.precise(activity)?VISIBLE:GONE);
        if(temporary&&!Reminders.precise(activity))summary.append("\nAndroid may delay widget updates until precise timing is allowed.");
    }
    private void customDuration(){
        EditText input=new EditText(activity);input.setTextColor(Ui.TEXT);input.setInputType(InputType.TYPE_CLASS_NUMBER);input.setHint("Minutes, 1–525600");input.setText("60");
        LinearLayout panel=Ui.column(activity);Ui.pad(panel,24,12);panel.addView(input);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("How long should it stay?").setView(panel).setNegativeButton("Cancel",null).setPositiveButton("Use duration",null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{long minutes;try{minutes=Long.parseLong(input.getText().toString());}catch(Exception e){minutes=0;}if(minutes<1||minutes>525600){input.setError("Choose 1–525600 minutes");return;}duration=minutes*60000;until=0;changed=true;refresh();dialog.dismiss();}));dialog.show();
    }
    private void pickDate(){
        ZonedDateTime current=Instant.ofEpochMilli(until>System.currentTimeMillis()?until:System.currentTimeMillis()+NoteLife.HOUR).atZone(ZoneId.systemDefault());
        DatePickerDialog date=new DatePickerDialog(activity,(picker,y,m,d)->new TimePickerDialog(activity,(time,h,min)->{
            long selected=LocalDateTime.of(y,m+1,d,h,min).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            if(selected<=System.currentTimeMillis()){Toast.makeText(activity,"Choose a future date and time",Toast.LENGTH_LONG).show();return;}
            until=selected;duration=0;changed=true;refresh();
        },current.getHour(),current.getMinute(),false).show(),current.getYear(),current.getMonthValue()-1,current.getDayOfMonth());date.getDatePicker().setMinDate(System.currentTimeMillis()-1000);date.show();
    }
    public boolean apply(Store.Item item){
        if(!temporary){NoteLife.permanent(item);return true;}
        if(duration>0){NoteLife.timer(activity,item,duration);return true;}
        if((changed&&until<=System.currentTimeMillis())||(!changed&&NoteLife.remaining(activity,original)<=0)){
            summary.setText("This note has expired. Pick a new duration or date, or choose Permanent.");Toast.makeText(activity,"Choose a future expiry",Toast.LENGTH_LONG).show();return false;
        }
        if(changed)NoteLife.deadline(item,until);else{item.archived=false;item.expired=false;}
        return true;
    }
    public void extendHour(){long left=temporary?(duration>0?duration:changed?Math.max(0,until-System.currentTimeMillis()):Math.max(0,NoteLife.remaining(activity,original))):0;temporary=true;duration=left+NoteLife.HOUR;until=0;changed=true;refresh();}
    public void keepPermanently(){permanentButton.performClick();}
    public void saveState(Bundle out){out.putBoolean("noteTemporary",temporary);out.putBoolean("noteChanged",changed);out.putLong("noteDuration",duration);out.putLong("noteUntil",until);}
    public void restoreState(Bundle state){temporary=state.getBoolean("noteTemporary");changed=state.getBoolean("noteChanged");duration=state.getLong("noteDuration");until=state.getLong("noteUntil");refresh();}
}
