package com.nook.widgets;

import android.content.*;
import android.net.Uri;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/** Optional, user-selected public feeds and authenticated quote data. Never called while drawing a widget. */
public final class FeedData {
    private FeedData(){}
    public static String key(String kind,JSONObject config){return kind+":"+(kind.equals("news")?config.optString("feedUrl",""):config.optString("symbol","")+":"+Integer.toHexString(config.optString("marketKey","").hashCode()));}
    public static JSONObject cached(Context c,String kind,JSONObject config){try{return new JSONObject(c.getSharedPreferences("nook_feeds",0).getString(key(kind,config),"{}"));}catch(Exception e){return new JSONObject();}}
    public static boolean configured(String kind,JSONObject config){return kind.equals("news")?!config.optString("feedUrl","").isEmpty():!config.optString("marketKey","").isEmpty()&&!config.optString("symbol","").isEmpty();}
    public static String request(String address)throws Exception{
        URL url=new URL(address);if(!url.getProtocol().equals("https"))throw new IOException("Use an HTTPS source");HttpURLConnection connection=(HttpURLConnection)url.openConnection();connection.setConnectTimeout(5000);connection.setReadTimeout(6000);connection.setInstanceFollowRedirects(false);connection.setRequestProperty("User-Agent","Nook/0.9");try{if(connection.getResponseCode()!=200)throw new IOException("Source unavailable");try(InputStream input=connection.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] block=new byte[8192];int n;while((n=input.read(block))!=-1){if(bytes.size()+n>1024*1024)throw new IOException("Source too large");bytes.write(block,0,n);}return bytes.toString(StandardCharsets.UTF_8.name());}}finally{connection.disconnect();}
    }
    public static JSONObject parseFeed(String xml)throws Exception{
        if(xml.toUpperCase(java.util.Locale.ROOT).contains("<!DOCTYPE"))throw new IOException("Unsupported feed document");XmlPullParser parser=Xml.newPullParser();parser.setInput(new StringReader(xml));JSONArray items=new JSONArray();JSONObject item=null;String field="";
        for(int event=parser.getEventType();event!=XmlPullParser.END_DOCUMENT;event=parser.next()){if(event==XmlPullParser.START_TAG){String tag=parser.getName();if(tag.equals("item")||tag.equals("entry"))item=new JSONObject();else if(item!=null){field=tag;if(tag.equals("link")&&parser.getAttributeValue(null,"href")!=null)item.put("url",parser.getAttributeValue(null,"href"));}}else if(event==XmlPullParser.TEXT&&item!=null){String value=parser.getText().trim();if(!value.isEmpty()){if(field.equals("title"))item.put("title",item.optString("title","")+value);if(field.equals("link")&&!item.has("url"))item.put("url",value);if(field.equals("pubDate")||field.equals("published"))item.put("date",value);}}else if(event==XmlPullParser.END_TAG){String tag=parser.getName();if((tag.equals("item")||tag.equals("entry"))&&item!=null){if(!item.optString("title").isEmpty()&&safeUrl(item.optString("url")))items.put(item);item=null;if(items.length()>=12)break;}field="";}}
        return new JSONObject().put("items",items).put("fetchedAt",System.currentTimeMillis());
    }
    public static boolean safeUrl(String value){try{Uri uri=Uri.parse(value);return ("https".equals(uri.getScheme())||"http".equals(uri.getScheme()))&&uri.getHost()!=null;}catch(Exception e){return false;}}
    public static void refresh(Context c,String kind,JSONObject config){
        if(!configured(kind,config))return;JSONObject previous=cached(c,kind,config);long now=System.currentTimeMillis();if(now-previous.optLong("attemptAt",0)<30000)return;
        try{JSONObject data;if(kind.equals("news"))data=parseFeed(request(config.optString("feedUrl")));else{String symbol=config.optString("symbol","");if(!symbol.matches("[A-Za-z0-9.^:_-]{1,24}"))throw new IOException("Check the symbol");String address="https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol="+URLEncoder.encode(symbol,"UTF-8")+"&apikey="+URLEncoder.encode(config.optString("marketKey"),"UTF-8");JSONObject quote=new JSONObject(request(address)).optJSONObject("Global Quote");if(quote==null||!quote.has("05. price"))throw new IOException("Check your provider key, symbol and plan");data=new JSONObject().put("quote",quote).put("fetchedAt",now);}data.put("attemptAt",now);c.getSharedPreferences("nook_feeds",0).edit().putString(key(kind,config),data.toString()).commit();}
        catch(Exception e){try{previous.put("attemptAt",now).put("offline",true).put("error","Source unavailable. Check the connection and source settings.");c.getSharedPreferences("nook_feeds",0).edit().putString(key(kind,config),previous.toString()).commit();}catch(Exception ignored){}}
    }
}
