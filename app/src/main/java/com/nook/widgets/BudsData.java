package com.nook.widgets;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Paired audio devices only. No discovery, location, reflection or vendor account access. */
public final class BudsData {
    private BudsData(){}
    static final String BATTERY_ACTION="android.bluetooth.device.action.BATTERY_LEVEL_CHANGED";
    static final String BATTERY_EXTRA="android.bluetooth.device.extra.BATTERY_LEVEL";
    private static final Map<Integer,Set<String>> connected=new ConcurrentHashMap<>();
    private static long requested;
    public static boolean allowed(Context c){return Build.VERSION.SDK_INT<31||c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;}
    private static BluetoothAdapter adapter(Context c){BluetoothManager m=c.getSystemService(BluetoothManager.class);return m==null?null:m.getAdapter();}
    public static boolean enabled(Context c){try{BluetoothAdapter a=adapter(c);return allowed(c)&&a!=null&&a.isEnabled();}catch(SecurityException e){return false;}}
    public static List<BluetoothDevice> paired(Context c){List<BluetoothDevice> list=new ArrayList<>();try{BluetoothAdapter a=adapter(c);if(allowed(c)&&a!=null)list.addAll(a.getBondedDevices());list.sort(Comparator.comparing(BudsData::name,String.CASE_INSENSITIVE_ORDER));}catch(SecurityException ignored){}return list;}
    public static String name(BluetoothDevice device){try{String n=device.getName();return n==null||n.trim().isEmpty()?"Bluetooth device":n;}catch(SecurityException e){return "Bluetooth device";}}
    public static int percent(int n){return n>=0&&n<=100?n:-1;}
    public static String value(int n){return percent(n)<0?"—":n+"%";}
    private static android.content.SharedPreferences cache(Context c){return c.getSharedPreferences("buds_reports",Context.MODE_PRIVATE);}
    public static void refresh(Context context){
        Context c=context.getApplicationContext();if(!enabled(c)){connected.clear();return;}
        synchronized(BudsData.class){long now=SystemClock.elapsedRealtime();if(now-requested<5000)return;requested=now;}
        BluetoothAdapter a=adapter(c);for(int kind:Build.VERSION.SDK_INT>=33?new int[]{BluetoothProfile.A2DP,BluetoothProfile.HEADSET,BluetoothProfile.LE_AUDIO}:new int[]{BluetoothProfile.A2DP,BluetoothProfile.HEADSET})try{a.getProfileProxy(c,new BluetoothProfile.ServiceListener(){public void onServiceConnected(int profile,BluetoothProfile proxy){try{Set<String> addresses=new HashSet<>();if(allowed(c))for(BluetoothDevice d:proxy.getConnectedDevices())addresses.add(d.getAddress());connected.put(profile,addresses);}catch(SecurityException ignored){}finally{a.closeProfileProxy(profile,proxy);}update(c);}public void onServiceDisconnected(int profile){connected.remove(profile);update(c);}},kind);}catch(SecurityException ignored){}
    }
    public static final class State {
        public String name,status,source;public int battery=-1,left=-1,right=-1,caseLevel=-1;public boolean manual,online,stale;
    }
    public static State read(Context c,JSONObject config){
        State s=new State();String address=config.optString("budsAddress","");s.name=config.optString("budsName","Your earbuds");s.manual=config.optBoolean("budsManual",false);
        if(!allowed(c))s.status="Allow Nearby devices";else if(!enabled(c))s.status="Bluetooth is off";else if(address.isEmpty())s.status="Choose your earbuds";else{for(Set<String> devices:connected.values())if(devices.contains(address))s.online=true;s.status=s.online?"Connected":connected.size()<2?"Checking connection":"Not connected";}
        if(s.manual){s.left=percent(config.optInt("budsLeft",-1));s.right=percent(config.optInt("budsRight",-1));s.caseLevel=percent(config.optInt("budsCase",-1));s.battery=percent(config.optInt("budsBattery",-1));long at=config.optLong("budsManualAt",0);s.stale=at>0&&System.currentTimeMillis()-at>24*60*60*1000L;s.source=s.stale?"Manual · older than 24h":"Manual readings";}
        else if(allowed(c)&&!address.isEmpty()){long at=cache(c).getLong(address+".at",0);long age=System.currentTimeMillis()-at;if(at>0&&age>=0&&age<24*60*60*1000L){s.battery=percent(cache(c).getInt(address+".battery",-1));s.stale=!s.online||age>15*60*1000;s.source="Last reported · "+android.text.format.DateFormat.getTimeFormat(c).format(new Date(at));}else s.source="Battery not shared by Android";}
        else s.source="Battery unavailable";
        return s;
    }
    static void event(Context c,Intent i){
        if(!allowed(c))return;String action=i.getAction();BluetoothDevice d=i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if(BATTERY_ACTION.equals(action)&&d!=null){int level=percent(i.getIntExtra(BATTERY_EXTRA,-1));try{if(d.getBondState()==BluetoothDevice.BOND_BONDED){android.content.SharedPreferences.Editor e=cache(c).edit();if(level<0)e.remove(d.getAddress()+".battery").remove(d.getAddress()+".at");else e.putInt(d.getAddress()+".battery",level).putLong(d.getAddress()+".at",System.currentTimeMillis());e.apply();}}catch(SecurityException ignored){}}
        int profile=BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)?BluetoothProfile.A2DP:BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)?BluetoothProfile.HEADSET:Build.VERSION.SDK_INT>=33&&BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED.equals(action)?BluetoothProfile.LE_AUDIO:-1;
        if(profile>=0&&d!=null)try{Set<String> copy=new HashSet<>(connected.getOrDefault(profile,Collections.emptySet()));if(i.getIntExtra(BluetoothProfile.EXTRA_STATE,-1)==BluetoothProfile.STATE_CONNECTED)copy.add(d.getAddress());else copy.remove(d.getAddress());connected.put(profile,copy);}catch(SecurityException ignored){}
        if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)&&i.getIntExtra(BluetoothAdapter.EXTRA_STATE,-1)!=BluetoothAdapter.STATE_ON)connected.clear();
        update(c);refresh(c);
    }
    public static void update(Context c){c.sendBroadcast(new Intent("com.nook.widgets.BUDS_REFRESH").setPackage(c.getPackageName()));AppWidgetManager m=AppWidgetManager.getInstance(c);for(String kind:new String[]{"buds","universal"})for(int id:m.getAppWidgetIds(new ComponentName(c,Widgets.provider(kind))))if(kind.equals("buds")||UniversalWidget.currentPage(Store.config(c,id)).equals("buds"))Widgets.update(c,id,kind);}
}
