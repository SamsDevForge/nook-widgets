package com.nook.widgets;

import android.Manifest;
import android.app.*;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.text.*;
import android.widget.*;
import org.json.JSONObject;
import java.util.*;

public final class BudsOptions {
    private BudsOptions(){}
    private static void choose(Activity a,JSONObject config,Runnable refresh){
        if(!BudsData.allowed(a)){if(Build.VERSION.SDK_INT>=31)a.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},84);return;}
        List<BluetoothDevice> devices=BudsData.paired(a);if(devices.isEmpty()){new AlertDialog.Builder(a).setTitle("Pair your earbuds first").setMessage("Pair in Android Bluetooth settings, then return here to choose them.").setPositiveButton("Bluetooth settings",(d,n)->a.startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))).setNegativeButton("Close",null).show();return;}
        String[] names=new String[devices.size()];for(int n=0;n<names.length;n++)names[n]=BudsData.name(devices.get(n))+" · "+devices.get(n).getAddress().substring(12);
        new AlertDialog.Builder(a).setTitle("Choose a paired device").setItems(names,(dialog,n)->{ExtraData.put(config,"budsAddress",devices.get(n).getAddress());ExtraData.put(config,"budsName",BudsData.name(devices.get(n)));BudsData.refresh(a);refresh.run();}).setNegativeButton("Cancel",null).show();
    }
    public static void add(Activity a,LinearLayout page,JSONObject config,Runnable refresh){
        page.addView(Ui.button(a,"Choose paired earbuds",Ui.SURFACE,Ui.ACCENT,()->choose(a,config,refresh)));
        if(!BudsData.allowed(a))page.addView(Ui.text(a,"Choosing a device asks for Nearby devices permission. Only paired devices are read; no scanning or location permission is used. Tap Choose again after granting access.",12,Ui.MUTED));
        page.addView(Ui.button(a,"Bluetooth settings",Ui.SURFACE,Ui.ACCENT,()->a.startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))));
        page.addView(Ui.button(a,"Refresh connection",Ui.SURFACE,Ui.ACCENT,()->{BudsData.refresh(a);refresh.run();}));
        page.addView(Ui.text(a,"Android may share a combined battery report for some earbuds. Separate left, right and case readings are manufacturer-specific and are shown as unavailable here. You can enter your own readings below. The widget never substitutes your phone's battery.",12,Ui.MUTED));
        Switch manual=new Switch(a);manual.setText("Use clearly labeled manual readings");manual.setTextColor(Ui.TEXT);manual.setChecked(config.optBoolean("budsManual",false));page.addView(manual);manual.setOnCheckedChangeListener((b,on)->{ExtraData.put(config,"budsManual",on);refresh.run();});
        String[] keys={"budsBattery","budsLeft","budsRight","budsCase"},labels={"Combined","Left","Right","Case"};for(int n=0;n<keys.length;n++){String key=keys[n];EditText field=new EditText(a);field.setSingleLine();field.setTextColor(Ui.TEXT);field.setHintTextColor(Ui.MUTED);field.setInputType(InputType.TYPE_CLASS_NUMBER);field.setHint(labels[n]+" battery % · blank = unknown");field.setText(config.has(key)?config.optString(key):"");page.addView(field);field.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int start,int before,int count){int value=-1;try{value=BudsData.percent(Integer.parseInt(s.toString()));}catch(NumberFormatException ignored){}if(value<0)config.remove(key);else ExtraData.put(config,key,value);ExtraData.put(config,"budsManualAt",System.currentTimeMillis());refresh.run();}});}
        Switch hide=new Switch(a);hide.setText("Hide titles · keep status and reading source");hide.setTextColor(Ui.TEXT);hide.setChecked(config.optBoolean("budsHideTitle",false));page.addView(hide);hide.setOnCheckedChangeListener((b,on)->{ExtraData.put(config,"budsHideTitle",on);refresh.run();});
    }
}
