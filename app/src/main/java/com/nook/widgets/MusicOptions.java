package com.nook.widgets;

import android.app.*;
import android.content.*;
import android.media.session.MediaController;
import android.widget.*;
import org.json.JSONObject;
import java.util.*;

public final class MusicOptions {
    private MusicOptions(){}
    public static void choosePlayer(Activity a,JSONObject config,Runnable changed){
        List<String> packages=new ArrayList<>(),labels=new ArrayList<>();packages.add("");labels.add("Automatic · follow the active player");for(MediaController player:MediaAccess.sessions(a))if(!packages.contains(player.getPackageName())){packages.add(player.getPackageName());labels.add(MediaAccess.appName(a,player.getPackageName()));}String saved=config.optString("musicPackage","");if(!packages.contains(saved)){packages.add(saved);labels.add(MediaAccess.appName(a,saved)+" · currently inactive");}new AlertDialog.Builder(a).setTitle("Music player").setSingleChoiceItems(labels.toArray(new String[0]),packages.indexOf(saved),(dialog,n)->{ExtraData.put(config,"musicPackage",packages.get(n));dialog.dismiss();changed.run();}).setNegativeButton("Cancel",null).show();
    }
    private static void toggle(Activity a,LinearLayout page,JSONObject config,String key,String name,boolean fallback,Runnable refresh){Switch control=new Switch(a);control.setText(name);control.setTextColor(Ui.TEXT);control.setChecked(config.optBoolean(key,fallback));control.setMinHeight(Ui.dp(a,48));page.addView(control);control.setOnCheckedChangeListener((button,on)->{ExtraData.put(config,key,on);refresh.run();});}
    private static void choice(Activity a,LinearLayout page,JSONObject config,String key,String[] names,Runnable refresh){Spinner control=new Spinner(a);control.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,names));control.setSelection(WidgetDesign.clamp(config.optInt(key,0),0,names.length-1));page.addView(control);control.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> parent){}public void onItemSelected(AdapterView<?> parent,android.view.View view,int n,long id){if(view instanceof TextView text)text.setTextColor(Ui.TEXT);if(n!=config.optInt(key,0)){ExtraData.put(config,key,n);refresh.run();}}});}
    public static void add(Activity a,LinearLayout page,JSONObject config,Runnable refresh){
        page.addView(Ui.button(a,"Explore creative players",Ui.SURFACE,Ui.ACCENT,()->new AlertDialog.Builder(a).setTitle("A different way to play").setItems(CreativeMusic.NAMES,(dialog,n)->{ExtraData.put(config,"variant",n+4);a.recreate();}).setNegativeButton("Close",null).show()));
        choice(a,page,config,"musicMood",new String[]{"Mood · creative collection","Mood · follow design language","Mood · midnight","Mood · warm paper","Mood · cherry red","Mood · album glow"},refresh);
        toggle(a,page,config,"musicTexture","Subtle texture on creative objects",true,refresh);
        page.addView(Ui.text(a,"Object depth",12,Ui.MUTED));SeekBar depth=new SeekBar(a);depth.setMax(100);depth.setProgress(config.optInt("musicDepth",65));page.addView(depth);depth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar bar){}public void onStopTrackingTouch(SeekBar bar){}public void onProgressChanged(SeekBar bar,int value,boolean user){if(user){ExtraData.put(config,"musicDepth",value);refresh.run();}}});
        page.addView(Ui.text(a,"Creative moods color solid cards. Follow design uses your existing theme; glass keeps its glass material. Compact spaces adapt to a simple player. Cassette reels and arcade details are decorative; Bubble Lab shows reported output volume, and Cover Stack uses queue art only when shared.",12,Ui.MUTED));
        page.addView(Ui.button(a,MediaAccess.allowed(a)?"Media access is connected":"Connect media access",Ui.SURFACE,Ui.ACCENT,()->a.startActivity(WidgetUtilityActivity.intent(a,"media_access",-1,""))));
        page.addView(Ui.button(a,"Choose music or podcast player",Ui.SURFACE,Ui.ACCENT,()->choosePlayer(a,config,()->a.recreate())));String selected=config.optString("musicPackage","");page.addView(Ui.text(a,selected.isEmpty()?"Automatically follows the active player.":"This widget follows "+MediaAccess.appName(a,selected)+" only.",12,Ui.MUTED));
        page.addView(Ui.text(a,"Start your music app once to make it available here. Each widget can follow a different app. Notification access supplies the player's public media session; Nook does not read or store notification messages.",12,Ui.MUTED));
        toggle(a,page,config,"musicShowArt","Show album artwork",true,refresh);toggle(a,page,config,"musicTint","Tint solid cards from album artwork",false,refresh);toggle(a,page,config,"musicProgress","Show track timeline and elapsed time",true,refresh);toggle(a,page,config,"musicShowAlbum","Show album name",true,refresh);toggle(a,page,config,"musicShowApp","Show player name",true,refresh);toggle(a,page,config,"musicHideText","Hide all text · artwork and controls",false,refresh);
        choice(a,page,config,"musicArtShape",new String[]{"Artwork shape · match the design","Artwork shape · circle","Artwork shape · square","Artwork shape · rounded"},refresh);
        choice(a,page,config,"musicTransport",new String[]{"Track controls · Previous / Next","Podcast controls · Back 15s / Forward 30s"},refresh);
        choice(a,page,config,"musicTap",new String[]{"Tap artwork · open Nook Now Playing","Tap artwork · open the music app"},refresh);
        page.addView(Ui.text(a,"The expand icon opens Now Playing with a seek slider, available queue, volume and any custom actions shared by your player. Unavailable controls are dimmed. The widget's elapsed clock runs natively; its progress bar refreshes when the player reports an update.",12,Ui.MUTED));
    }
}
