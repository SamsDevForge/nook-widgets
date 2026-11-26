package com.nook.widgets;

import android.content.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Optional city-based Open-Meteo forecasts; no location permission is used. */
public final class WeatherData {
    private WeatherData(){}
    private static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("nook_weather",0);}
    private static final java.util.concurrent.ConcurrentHashMap<String,Boolean> inFlight=new java.util.concurrent.ConcurrentHashMap<>();
    public static String key(JSONObject settings){return String.format(Locale.ROOT,"%.4f,%.4f",settings.optDouble("latitude"),settings.optDouble("longitude"));}
    public static boolean configured(JSONObject settings){return settings.has("latitude")&&settings.has("longitude")&&Math.abs(settings.optDouble("latitude",999))<=90&&Math.abs(settings.optDouble("longitude",999))<=180;}
    public static JSONObject cached(Context c,JSONObject settings){try{return new JSONObject(prefs(c).getString(key(settings),"{}"));}catch(Exception e){return new JSONObject();}}
    public static JSONObject json(String url) throws Exception {
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();connection.setConnectTimeout(3000);connection.setReadTimeout(4000);connection.setRequestProperty("User-Agent","Nook/0.7 (Android widget)");
        try{if(connection.getResponseCode()!=200)throw new IOException("Weather service unavailable");try(InputStream input=connection.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int count;while((count=input.read(buffer))!=-1){bytes.write(buffer,0,count);if(bytes.size()>1024*1024)throw new IOException("Response too large");}return new JSONObject(bytes.toString("UTF-8"));}}finally{connection.disconnect();}
    }
    public static JSONArray cities(String name) throws Exception {JSONObject json=json("https://geocoding-api.open-meteo.com/v1/search?count=6&language=en&format=json&name="+URLEncoder.encode(name,"UTF-8"));JSONArray result=json.optJSONArray("results");return result==null?new JSONArray():result;}
    public static JSONObject fetch(JSONObject settings) throws Exception {
        if(!configured(settings))throw new IllegalArgumentException("Choose a city first");
        JSONObject data=json("https://api.open-meteo.com/v1/forecast?latitude="+settings.getDouble("latitude")+"&longitude="+settings.getDouble("longitude")+"&current=temperature_2m,apparent_temperature,weather_code,is_day&daily=temperature_2m_max,temperature_2m_min,weather_code&forecast_days=3&timezone=auto");
        JSONObject current=data.getJSONObject("current");if(!current.has("temperature_2m")||current.isNull("temperature_2m"))throw new IOException("No forecast available");data.put("fetchedAt",System.currentTimeMillis());return data;
    }
    public static void refresh(Context c,JSONObject settings,boolean force){
        if(!configured(settings))return;String key=key(settings);JSONObject cache=cached(c,settings);long now=System.currentTimeMillis();
        if(!force&&(now-cache.optLong("fetchedAt")<3600000||now-prefs(c).getLong("failed_"+key,0)<300000))return;
        if(force&&now-prefs(c).getLong("attempt_"+key,0)<30000||inFlight.putIfAbsent(key,true)!=null)return;
        prefs(c).edit().putLong("attempt_"+key,now).commit();try{JSONObject data=fetch(settings);prefs(c).edit().putString(key,data.toString()).remove("failed_"+key).commit();}catch(Exception e){prefs(c).edit().putLong("failed_"+key,now).commit();}finally{inFlight.remove(key);}
    }
    public static boolean failed(Context c,JSONObject settings){return prefs(c).getLong("failed_"+key(settings),0)>cached(c,settings).optLong("fetchedAt");}
    public static int temperature(double celsius,JSONObject settings){return (int)Math.round(settings.optBoolean("fahrenheit",false)?celsius*9/5+32:celsius);}
    public static String degrees(double value,JSONObject settings){return Double.isNaN(value)||Double.isInfinite(value)?"—":temperature(value,settings)+"°";}
    public static String condition(int code){return switch(code){case 0->"Clear";case 1,2,3->"Partly cloudy";case 45,48->"Fog";case 51,53,55,56,57->"Drizzle";case 61,63,65,66,67->"Rain";case 71,73,75,77->"Snow";case 80,81,82->"Rain showers";case 85,86->"Snow showers";case 95,96,99->"Thunderstorms";default->"Unavailable";};}
}
