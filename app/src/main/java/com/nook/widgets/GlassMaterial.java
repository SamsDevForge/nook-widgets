package com.nook.widgets;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.widget.*;
import org.json.JSONObject;

/** Locally rendered clear glass, with optional user-granted wallpaper optics. */
public final class GlassMaterial {
    private GlassMaterial(){}
    public static boolean enabled(JSONObject c){return c.optInt("style",0)==3;}
    public static boolean clear(JSONObject c){return enabled(c)&&c.optInt("glassMode",0)==0;}
    public static void colors(WidgetDesign.Colors colors,JSONObject config){
        if(!enabled(config))return;
        boolean dark=config.optInt("tone",0)!=1;
        if(clear(config)){colors.monochrome=true;colors.surface=dark?0xff263446:0xffedf1f6;colors.chip=dark?0xff38495d:0xfff5f8fc;colors.text=dark?Color.WHITE:0xff151c27;colors.muted=dark?0xffe4eaf2:0xff394454;colors.accent=colors.text;}
    }
    public static Bitmap render(Context context,JSONObject config,int width,int height,boolean chip){
        float scale=Math.min(2f,(float)Math.sqrt(360000f/(width*height)));int w=Math.max(2,Math.round(width*scale)),h=Math.max(2,Math.round(height*scale));Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);bitmap.setDensity(Bitmap.DENSITY_NONE);Canvas canvas=new Canvas(bitmap);Paint paint=new Paint(3);WidgetDesign.Colors colors=WidgetDesign.colors(config,config.optString("kind","notes"));
        float radius=Math.min((chip?Math.min(24,WidgetDesign.radius(config)):WidgetDesign.radius(config))*scale,Math.min(w,h)/2f);RectF bounds=new RectF(scale,scale,w-scale,h-scale);Path path=new Path();path.addRoundRect(bounds,radius,radius,Path.Direction.CW);canvas.save();canvas.clipPath(path);
        paint.setColor(colors.surface);canvas.drawRect(bounds,paint);
        Bitmap source=chip?null:WidgetData.photo(context,config.optString("glassUri",""));boolean sample=source==null&&!chip&&config.optBoolean("_preview",false);
        if(source!=null||sample){Bitmap backdrop=backdrop(context,config,source,w,h);int columns=24,rows=18;float[] vertices=new float[(columns+1)*(rows+1)*2];float strength=WidgetDesign.clamp(config.optInt("glassRefraction",60),0,100)/100f*10*scale;int i=0;for(int y=0;y<=rows;y++)for(int x=0;x<=columns;x++){float nx=2*x/(float)columns-1,ny=2*y/(float)rows-1,edge=(float)Math.pow(Math.max(Math.abs(nx),Math.abs(ny)),5);vertices[i++]=x*w/(float)columns+nx*edge*strength;vertices[i++]=y*h/(float)rows+ny*edge*strength;}canvas.drawBitmapMesh(backdrop,columns,rows,vertices,0,null,0,paint);backdrop.recycle();}
        int tint=WidgetDesign.clamp(config.optInt("glassTint",28),0,100);paint.setColor((Math.round(tint*1.8f)<<24)|(colors.surface&0xffffff));canvas.drawRect(bounds,paint);paint.setAlpha(255);
        paint.setShader(new LinearGradient(0,0,w,h,new int[]{0x8cffffff,0x12ffffff,0x03ffffff,0x380a172a},new float[]{0,.24f,.62f,1},Shader.TileMode.CLAMP));canvas.drawRect(bounds,paint);paint.setShader(new RadialGradient(w*.16f,-h*.25f,Math.max(w,h)*.85f,new int[]{0x80ffffff,0x16ffffff,0x00ffffff},null,Shader.TileMode.CLAMP));canvas.drawRect(bounds,paint);paint.setShader(null);
        // A broad inner bevel and a crisp, direction-dependent outer reflection.
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(5*scale);paint.setShader(new LinearGradient(0,0,w,h,new int[]{0x80ffffff,0x05ffffff,0x500f2138},null,Shader.TileMode.CLAMP));canvas.drawRoundRect(new RectF(3*scale,3*scale,w-3*scale,h-3*scale),Math.max(0,radius-2*scale),Math.max(0,radius-2*scale),paint);paint.setStrokeWidth(1.05f*scale);paint.setShader(new LinearGradient(0,0,w,h,new int[]{0xf5ffffff,0x55ffffff,0x30d9e9ff,0xc8ffffff},null,Shader.TileMode.CLAMP));canvas.drawRoundRect(bounds,radius,radius,paint);paint.setShader(null);canvas.restore();return bitmap;
    }
    private static Bitmap backdrop(Context c,JSONObject config,Bitmap source,int width,int height){
        int w=240,h=Math.max(80,Math.min(320,Math.round(240f*height/width)));Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);Paint paint=new Paint(3);
        if(source==null){android.graphics.drawable.Drawable sample=c.getDrawable(R.drawable.preview_wallpaper);sample.setBounds(0,0,w,h);sample.draw(canvas);}else{float zoom=WidgetDesign.clamp(config.optInt("glassZoom",100),100,300)/100f,factor=Math.max(w/(float)source.getWidth(),h/(float)source.getHeight())*zoom,sw=source.getWidth()*factor,sh=source.getHeight()*factor,x=-(sw-w)*WidgetDesign.clamp(config.optInt("glassCropX",50),0,100)/100f,y=-(sh-h)*WidgetDesign.clamp(config.optInt("glassCropY",50),0,100)/100f;canvas.drawBitmap(source,null,new RectF(x,y,x+sw,y+sh),paint);}
        int radius=WidgetDesign.clamp(config.optInt("glassBlur",14),0,30);if(radius>0){int[] pixels=new int[w*h],out=new int[w*h];b.getPixels(pixels,0,w,0,0,w,h);for(int pass=0;pass<2;pass++){for(int y=0;y<h;y++)for(int x=0;x<w;x++){int r=0,g=0,blue=0,count=0;for(int offset=-radius;offset<=radius;offset++){int xx=pass==0?Math.max(0,Math.min(w-1,x+offset)):x,yy=pass==1?Math.max(0,Math.min(h-1,y+offset)):y,color=pixels[yy*w+xx];r+=Color.red(color);g+=Color.green(color);blue+=Color.blue(color);count++;}out[y*w+x]=Color.rgb(r/count,g/count,blue/count);}int[] swap=pixels;pixels=out;out=swap;}b.setPixels(pixels,0,w,0,0,w,h);}return b;
    }
    private static void put(JSONObject c,String key,Object value){try{c.put(key,value);}catch(Exception e){throw new IllegalStateException(e);}}
    public static void editor(Activity a,JSONObject config,Runnable refresh){
        JSONObject draft;try{draft=new JSONObject(config.toString());}catch(Exception e){throw new IllegalStateException(e);}LinearLayout panel=Ui.column(a);Ui.pad(panel,24,16);panel.addView(Ui.text(a,"Clear uses monochrome content; Tinted retains the design colors. A chosen wallpaper is blurred and refracted locally. Match the crop to the widget's position on your home screen.",13,Ui.MUTED));Spinner mode=new Spinner(a);mode.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,new String[]{"Clear · monochrome","Tinted · design colors"}));mode.setSelection(config.optInt("glassMode",0));panel.addView(mode);
        slider(a,panel,draft,"glassBlur","Blur",0,30,14);slider(a,panel,draft,"glassRefraction","Refraction",0,100,60);slider(a,panel,draft,"glassTint","Tint strength",0,100,28);slider(a,panel,draft,"glassCropX","Wallpaper crop · horizontal",0,100,50);slider(a,panel,draft,"glassCropY","Wallpaper crop · vertical",0,100,50);slider(a,panel,draft,"glassZoom","Wallpaper zoom",100,300,100);ScrollView scroll=new ScrollView(a);scroll.addView(panel);new AlertDialog.Builder(a).setTitle("Liquid Glass optics").setView(scroll).setPositiveButton("Apply",(d,n)->{put(draft,"glassMode",mode.getSelectedItemPosition());for(String key:new String[]{"glassMode","glassBlur","glassRefraction","glassTint","glassCropX","glassCropY","glassZoom"})put(config,key,draft.opt(key));refresh.run();}).setNegativeButton("Cancel",null).show();
    }
    private static void slider(Activity a,LinearLayout panel,JSONObject config,String key,String title,int min,int max,int fallback){TextView label=Ui.text(a,title+" · "+config.optInt(key,fallback),12,Ui.MUTED);panel.addView(label);SeekBar bar=new SeekBar(a);bar.setContentDescription(title);bar.setMax(max-min);bar.setProgress(config.optInt(key,fallback)-min);panel.addView(bar);bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int value,boolean user){put(config,key,value+min);label.setText(title+" · "+(value+min));}});}
}
