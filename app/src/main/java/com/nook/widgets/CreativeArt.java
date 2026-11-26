package com.nook.widgets;

import android.graphics.*;
import android.media.session.*;
import org.json.JSONObject;
import java.util.*;

/** Original code-drawn music objects inspired by physical media and sculpted interfaces. */
public final class CreativeArt {
    private CreativeArt(){}
    public static int[] palette(JSONObject config,WidgetDesign.Colors design,Bitmap art,int variant){
        if(config.optInt("style",0)>0||config.optInt("musicMood",0)==1)return new int[]{design.surface,design.text,design.muted,design.accent};
        int[][] defaults={{0xffded1b7,0xff29261f,0xff665f53,0xffb34830},{0xff860811,0xffffe45b,0xffe6b59e,0xffffd829},{0xff251c35,0xfffff4e9,0xffd2b8cf,0xffeebc79},{0xff080b09,0xfff5fff4,0xff9ea99f,0xff5fe26d},{0xffc8cbcc,0xff171c20,0xff515960,0xff315e63},{0xfff1ebd9,0xff1b2926,0xff58645c,0xffbd3831},{0xff1c1833,0xfff1f5df,0xffb8b2d0,0xffbdff72}};
        int[] colors=defaults[Math.max(0,Math.min(6,variant-4))].clone();int mood=config.optInt("musicMood",0);if(mood==2)colors=new int[]{0xff10151d,0xfff4f7ff,0xffaab8cc,0xff8ed5ff};if(mood==3)colors=new int[]{0xfff1ebd9,0xff1c2724,0xff59665b,0xffad4639};if(mood==4)colors=new int[]{0xff860811,0xffffe45b,0xfff0c3b6,0xffffd829};if(mood==5){colors[0]=MusicArt.mix(MusicArt.dominant(art,design.accent),Color.BLACK,.65f);colors[1]=Color.WHITE;colors[2]=0xffd5dce0;colors[3]=MusicArt.mix(MusicArt.dominant(art,design.accent),Color.WHITE,.5f);}return colors;
    }
    public static Bitmap panel(int color,int width,int height,float radius,boolean texture){Bitmap b=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);b.setDensity(Bitmap.DENSITY_NONE);Canvas canvas=new Canvas(b);Paint p=new Paint(3);p.setColor(color);canvas.drawRoundRect(0,0,width,height,radius,radius,p);if(texture){canvas.save();Path clip=new Path();clip.addRoundRect(new RectF(0,0,width,height),radius,radius,Path.Direction.CW);canvas.clipPath(clip);p.setColor(0x14000000);for(int y=3;y<height;y+=7)for(int x=(y%11);x<width;x+=13)canvas.drawPoint(x,y,p);canvas.restore();}return b;}
    private static void rounded(Canvas c,Paint p,RectF box,float radius,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(box,radius,radius,p);}
    private static void photo(Canvas c,Bitmap source,RectF box,float radius,int accent){JSONObject cfg=new JSONObject();ExtraData.put(cfg,"musicArtShape",radius>box.width()*.4?1:3);Bitmap art=MusicArt.cover(source,cfg,(int)box.width(),(int)box.height(),accent,radius,false);c.drawBitmap(art,null,box,new Paint(3));art.recycle();}
    public static Bitmap scene(JSONObject config,MusicState state,int variant,int width,int height,int[] colors){
        Bitmap b=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);b.setDensity(Bitmap.DENSITY_NONE);Canvas c=new Canvas(b);c.scale(width/480f,height/(variant==5?300f:200f));Paint p=new Paint(3);int accent=colors[3];boolean hide=config.optBoolean("musicHideText",false);Bitmap artwork=config.optBoolean("musicShowArt",true)?state.art:null;float depth=WidgetDesign.clamp(config.optInt("musicDepth",65),0,100)/100f;
        if(variant==4){
            p.setShadowLayer(9*depth,4,6,0x80000000);rounded(c,p,new RectF(218,19,469,182),18,0xff303633);p.clearShadowLayer();rounded(c,p,new RectF(228,29,459,158),10,0xffefe2c2);rounded(c,p,new RectF(237,36,450,66),4,accent);p.setColor(0xff18211f);p.setTextSize(15);p.setTypeface(Typeface.create("monospace",Typeface.BOLD));if(!hide)c.drawText("NOOK / SIDE A",247,56,p);rounded(c,p,new RectF(244,77,442,132),18,0xff3d4541);
            for(int x:new int[]{280,407}){p.setColor(0xffc7c7b9);c.drawCircle(x,105,23,p);p.setColor(0xff202a25);c.drawCircle(x,105,8,p);p.setStrokeWidth(3);for(int n=0;n<6;n++){double a=n*Math.PI/3;c.drawLine(x+(float)Math.cos(a)*12,105+(float)Math.sin(a)*12,x+(float)Math.cos(a)*20,105+(float)Math.sin(a)*20,p);}}
            rounded(c,p,new RectF(276,143,408,181),8,0xff1c231f);for(int x:new int[]{246,434}){p.setColor(0xffa0a99b);c.drawCircle(x,160,3,p);}p.setColor(colors[2]);p.setTextSize(11);if(!hide)c.drawText("STEREO  •  C60",280,173,p);
        }else if(variant==5){
            p.setShadowLayer(13*depth,-5,5,0xa0000000);p.setColor(MusicArt.mix(colors[0],Color.BLACK,.35f));c.drawCircle(425,150,176,p);p.clearShadowLayer();photo(c,artwork,new RectF(279,4,571,296),146,accent);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);p.setColor(0x40ffffff);c.drawArc(249,-26,601,326,205,230,false,p);p.setColor(accent);float progress=state.duration>0?230f*state.position/state.duration:0;c.drawArc(245,-63,551,243,205,progress,false,p);p.setStyle(Paint.Style.FILL);
        }else if(variant==6){
            java.util.List<MediaSession.QueueItem> queue=state.player==null?null:state.player.getQueue();Bitmap behind=queue!=null&&!queue.isEmpty()?queue.get(0).getDescription().getIconBitmap():null;
            for(int n:new int[]{0,2,1}){c.save();float x=244+n*53;c.rotate((n-1)*13,x+64,100);p.setShadowLayer(7*depth,2,5,0x99000000);rounded(c,p,new RectF(x,20,x+121,183),12,MusicArt.mix(colors[0],Color.WHITE,.18f));p.clearShadowLayer();photo(c,config.optBoolean("musicShowArt",true)?(n==1?artwork:behind):null,new RectF(x+6,26,x+115,139),10,accent);p.setColor(colors[1]);p.setTextSize(10);if(!hide)c.drawText(n==1?"NOW PLAYING":"QUEUE",x+9,160,p);c.restore();}
        }else if(variant==7){
            int percent=-1;MediaController.PlaybackInfo info=state.player==null?null:state.player.getPlaybackInfo();if(info!=null&&info.getMaxVolume()>0)percent=Math.round(info.getCurrentVolume()*100f/info.getMaxVolume());
            for(int n=0;n<9;n++){float x=240+n*24,r=8+(4-Math.abs(4-n))*3.5f;boolean lit=percent>=0&&n<=percent*8/100;p.setShader(new RadialGradient(x-r*.35f,87-r*.4f,r*1.5f,new int[]{lit?MusicArt.mix(accent,Color.WHITE,.35f):0xff404642,lit?accent:0xff101512},null,Shader.TileMode.CLAMP));c.drawCircle(x,87,r,p);}p.setShader(null);p.setColor(colors[1]);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(28);if(!hide)c.drawText(percent<0?"—":percent+"%",336,43,p);p.setTextSize(11);p.setColor(colors[2]);if(!hide)c.drawText("OUTPUT VOLUME",336,146,p);p.setTextAlign(Paint.Align.LEFT);
        }else if(variant==8){
            p.setShadowLayer(10*depth,6,6,0x60616a70);rounded(c,p,new RectF(258,14,444,190),90,colors[0]);p.setShadowLayer(9*depth,-5,-5,0xb0ffffff);rounded(c,p,new RectF(258,14,444,190),90,colors[0]);p.clearShadowLayer();photo(c,artwork,new RectF(272,27,430,185),80,accent);
        }else if(variant==9){
            c.save();c.rotate(-8,350,105);p.setShadowLayer(6*depth,3,5,0x65000000);rounded(c,p,new RectF(253,15,449,192),2,0xfffffcf0);p.clearShadowLayer();photo(c,artwork,new RectF(263,24,439,157),2,accent);p.setColor(0xff605c50);p.setTypeface(Typeface.create("serif",Typeface.ITALIC));p.setTextSize(14);if(!hide)c.drawText("Listen a little closer",267,179,p);p.setColor(0x90d2b68a);c.save();c.rotate(-9,300,19);c.drawRect(273,5,343,28,p);c.restore();c.restore();
        }else{
            rounded(c,p,new RectF(239,9,458,190),8,0xff0b1017);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(accent);c.drawRect(250,20,447,153,p);p.setStyle(Paint.Style.FILL);photo(c,artwork,new RectF(260,27,437,147),0,accent);p.setColor(accent);for(int x=265;x<430;x+=18)c.drawRect(x,165,x+11,173,p);p.setColor(0xffe77ab7);c.drawRect(426,163,439,178,p);
        }return b;
    }
    public static Bitmap button(int variant,int color,int background,int width,int height,float depth){Bitmap b=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);b.setDensity(Bitmap.DENSITY_NONE);Canvas c=new Canvas(b);Paint p=new Paint(3);float radius=variant==10?5:height/2f;
        if(variant==8){p.setShadowLayer(5*depth,3,3,0x70616a70);rounded(c,p,new RectF(5,4,width-5,height-5),radius,background);p.setShadowLayer(4*depth,-2,-2,0xc0ffffff);rounded(c,p,new RectF(5,4,width-5,height-5),radius,background);p.clearShadowLayer();}else rounded(c,p,new RectF(2,2,width-2,height-2),radius,color);return b;}
}
