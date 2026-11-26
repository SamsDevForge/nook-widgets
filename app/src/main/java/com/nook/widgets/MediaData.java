package com.nook.widgets;

import android.content.Context;
import android.graphics.*;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.IntConsumer;

/** Local, bounded media previews. Video decoding happens only after an explicit Prepare action. */
public final class MediaData {
    private MediaData(){}
    public static final int MAX_FRAMES=16,FRAME_WIDTH=200,FRAME_HEIGHT=112;
    public static boolean document(String value){try{return "content".equals(Uri.parse(value).getScheme());}catch(Exception e){return false;}}
    public static String key(JSONObject config){
        try{String input=config.optString("videoUri")+"|"+start(config)+"|"+length(config);byte[] digest=MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(int n=0;n<12;n++)out.append(String.format(Locale.ROOT,"%02x",digest[n]));return out.toString();}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    public static int start(JSONObject c){return WidgetDesign.clamp(c.optInt("clipStart",0),0,86400);}
    public static int length(JSONObject c){return WidgetDesign.clamp(c.optInt("clipLength",3),1,6);}
    private static File directory(Context c,JSONObject config){return new File(new File(c.getFilesDir(),"video_previews"),key(config));}
    public static JSONObject cached(Context c,JSONObject config){try{return new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(directory(c,config),"info.json").toPath()),StandardCharsets.UTF_8));}catch(Exception e){return new JSONObject();}}
    public static boolean ready(Context c,JSONObject config){return cached(c,config).optInt("count",0)>0;}
    public static Bitmap frame(Context c,JSONObject config,JSONObject info,int index){String generation=info.optString("generation");if(!generation.matches("[a-f0-9]{32}")||index<0||index>=MAX_FRAMES)return null;return BitmapFactory.decodeFile(new File(directory(c,config),generation+"-"+index+".jpg").getAbsolutePath());}
    public static synchronized JSONObject prepare(Context c,JSONObject config,IntConsumer progress)throws Exception{
        String uri=config.optString("videoUri");if(!document(uri))throw new IOException("Choose a video from the document picker.");
        File folder=directory(c,config);if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("Preview storage is unavailable.");
        String generation=UUID.randomUUID().toString().replace("-","");List<File> created=new ArrayList<>();MediaMetadataRetriever retriever=new MediaMetadataRetriever();
        try{
            retriever.setDataSource(c,Uri.parse(uri));long duration=Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            if(duration<120)throw new IOException("This video is too short for a preview.");if(start(config)*1000L>=duration)throw new IOException("Choose a start time before the video ends.");long begin=start(config)*1000L,span=Math.min(length(config)*1000L,duration-begin);
            int count=Math.max(1,Math.min(MAX_FRAMES,(int)(span/120)));int interval=Math.max(120,(int)(span/count));
            for(int n=0;n<count;n++){
                Bitmap raw=retriever.getScaledFrameAtTime((begin+n*span/count)*1000,MediaMetadataRetriever.OPTION_CLOSEST,FRAME_WIDTH,FRAME_HEIGHT);
                if(raw==null)throw new IOException("This video format does not expose preview frames.");Bitmap image=fit(raw,FRAME_WIDTH,FRAME_HEIGHT,false,0xff10151e);raw.recycle();File file=new File(folder,generation+"-"+n+".jpg");created.add(file);try(FileOutputStream out=new FileOutputStream(file)){image.compress(Bitmap.CompressFormat.JPEG,88,out);}finally{image.recycle();}if(progress!=null)progress.accept((n+1)*100/count);
            }
            JSONObject info=new JSONObject().put("generation",generation).put("count",count).put("interval",interval).put("duration",duration).put("begin",begin).put("span",span).put("name",ExtraData.fileName(c,uri));
            File temporary=new File(folder,generation+".json");created.add(temporary);java.nio.file.Files.write(temporary.toPath(),info.toString().getBytes(StandardCharsets.UTF_8));
            java.nio.file.Files.move(temporary.toPath(),new File(folder,"info.json").toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
            File[] obsolete=folder.listFiles((dir,name)->name.matches("[a-f0-9]{32}-\\d+\\.jpg")&&!name.startsWith(generation));if(obsolete!=null)for(File old:obsolete)old.delete();return info;
        }catch(Exception e){for(File file:created)file.delete();throw e;}finally{try{retriever.release();}catch(Exception ignored){}}
    }
    public static Bitmap fit(Bitmap source,int width,int height,boolean crop,int background){
        Bitmap result=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);result.setDensity(Bitmap.DENSITY_NONE);Canvas canvas=new Canvas(result);canvas.drawColor(background);float scale=crop?Math.max(width/(float)source.getWidth(),height/(float)source.getHeight()):Math.min(width/(float)source.getWidth(),height/(float)source.getHeight());float w=source.getWidth()*scale,h=source.getHeight()*scale;canvas.drawBitmap(source,null,new RectF((width-w)/2,(height-h)/2,(width+w)/2,(height+h)/2),new Paint(3));return result;
    }
    public static Bitmap widgetFrame(Context c,Bitmap source,int count){
        long budget=Math.min(1600000L,(long)c.getResources().getDisplayMetrics().widthPixels*c.getResources().getDisplayMetrics().heightPixels*6/4);float pixels=budget/(float)(Math.max(1,count)*4);int w=Math.min(FRAME_WIDTH,Math.max(48,(int)Math.sqrt(pixels*16/9)));return fit(source,w,Math.max(27,w*9/16),false,Color.TRANSPARENT);
    }
    public static List<String> photos(JSONObject config){List<String> values=new ArrayList<>();JSONArray list=config.optJSONArray("slideUris");if(list!=null)for(int n=0;n<Math.min(6,list.length());n++)if(document(list.optString(n)))values.add(list.optString(n));return values;}
    public static Bitmap scene(JSONObject config,int frame,int count){
        int w=FRAME_WIDTH,h=FRAME_HEIGHT;Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);b.setDensity(Bitmap.DENSITY_NONE);Canvas canvas=new Canvas(b);Paint p=new Paint(3);WidgetDesign.Colors colors=WidgetDesign.colors(config,"animation");float phase=(float)(frame*Math.PI*2/count),cx=w/2f,cy=h/2f;int variant=WidgetDesign.clamp(config.optInt("variant",0),0,3);
        int accent=colors.accent,soft=colors.monochrome?colors.text:Color.rgb(Math.min(255,Color.red(accent)+45),Math.min(255,Color.green(accent)+65),Math.min(255,Color.blue(accent)+55));
        if(variant==0){for(int n=0;n<3;n++){float angle=phase+n*(float)(Math.PI*2/3),x=cx+(float)Math.cos(angle)*43,y=cy+(float)Math.sin(angle)*22,r=25+(float)Math.sin(phase+n)*5;p.setShader(new RadialGradient(x-r*.3f,y-r*.4f,r,new int[]{Color.WHITE,accent,accent&0xffffff},new float[]{0,.35f,1},Shader.TileMode.CLAMP));canvas.drawCircle(x,y,r,p);}p.setShader(null);}
        else if(variant==1){float r=24+10*(float)(.5-.5*Math.cos(phase));p.setShader(new RadialGradient(cx,cy,r*1.65f,new int[]{soft,accent,accent&0xffffff},null,Shader.TileMode.CLAMP));canvas.drawCircle(cx,cy,r*1.65f,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(colors.text);p.setAlpha(150);canvas.drawCircle(cx,cy,r,p);}
        else if(variant==2){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);for(int n=0;n<3;n++){float t=(frame/(float)count+n/3f)%1;p.setColor(accent);p.setAlpha(Math.round((1-t)*230));canvas.drawOval(cx-65*t,cy-38*t,cx+65*t,cy+38*t,p);}}
        else{for(int x=0;x<15;x++)for(int y=0;y<5;y++){float wave=(float)(.5+.5*Math.sin(phase+x*.55f+y*.6f));p.setColor(colors.monochrome?colors.text:accent);p.setAlpha(Math.round(50+wave*205));canvas.drawCircle(30+x*10,34+y*11,1+wave*2,p);}}return b;
    }
}
