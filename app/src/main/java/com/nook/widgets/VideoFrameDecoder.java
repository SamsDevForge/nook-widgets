package com.nook.widgets;

import android.content.Context;
import android.graphics.*;
import android.media.*;
import android.net.Uri;
import android.os.SystemClock;
import java.nio.ByteBuffer;

/** Sequential hardware decoding, with a bounded bitmap per requested widget frame. */
public final class VideoFrameDecoder implements AutoCloseable {
    private final MediaExtractor extractor=new MediaExtractor();
    private MediaCodec codec;
    private long last=-1;
    private boolean ended;
    private int rotation;
    public VideoFrameDecoder(Context c,String uri)throws Exception{
        try{
            extractor.setDataSource(c,Uri.parse(uri),null);
            MediaFormat format=null;
            for(int n=0;n<extractor.getTrackCount();n++){MediaFormat candidate=extractor.getTrackFormat(n);if(candidate.getString(MediaFormat.KEY_MIME).startsWith("video/")){format=candidate;extractor.selectTrack(n);break;}}
            if(format==null)throw new IllegalArgumentException("No video track");
            rotation=format.containsKey(MediaFormat.KEY_ROTATION)?format.getInteger(MediaFormat.KEY_ROTATION):0;
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            codec=MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));codec.configure(format,null,null,0);codec.start();
        }catch(Exception e){close();throw e;}
    }
    public Bitmap frame(long milliseconds,int edge)throws Exception{
        long target=milliseconds*1000;
        if(last>=0&&(target<last-100000||target>last+3000000)){extractor.seekTo(target,MediaExtractor.SEEK_TO_PREVIOUS_SYNC);codec.flush();ended=false;last=-1;}
        MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();long deadline=SystemClock.elapsedRealtime()+2500;
        while(SystemClock.elapsedRealtime()<deadline){
            if(!ended){int input=codec.dequeueInputBuffer(0);if(input>=0){ByteBuffer bytes=codec.getInputBuffer(input);int size=extractor.readSampleData(bytes,0);if(size<0){codec.queueInputBuffer(input,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);ended=true;}else{codec.queueInputBuffer(input,0,size,extractor.getSampleTime(),0);extractor.advance();}}}
            int output=codec.dequeueOutputBuffer(info,10000);
            if(output>=0){try{last=info.presentationTimeUs;if(info.size>0&&last>=target){try(Image image=codec.getOutputImage(output)){if(image!=null)return bitmap(image,edge,rotation);}}if((info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0)return null;}finally{codec.releaseOutputBuffer(output,false);}}
        }
        return null;
    }
    private static Bitmap bitmap(Image image,int edge,int rotation){
        Rect crop=image.getCropRect();int width=crop.width(),height=crop.height();float scale=Math.min(1,edge/(float)Math.max(width,height));int w=Math.max(2,Math.round(width*scale)),h=Math.max(2,Math.round(height*scale));
        Image.Plane[] planes=image.getPlanes();ByteBuffer y=planes[0].getBuffer(),u=planes[1].getBuffer(),v=planes[2].getBuffer();int yp=y.position(),up=u.position(),vp=v.position();int[] pixels=new int[w*h];
        for(int row=0;row<h;row++){int sy=crop.top+row*height/h;for(int col=0;col<w;col++){int sx=crop.left+col*width/w;int yy=(y.get(yp+sy*planes[0].getRowStride()+sx*planes[0].getPixelStride())&255)-16,uu=(u.get(up+(sy/2)*planes[1].getRowStride()+(sx/2)*planes[1].getPixelStride())&255)-128,vv=(v.get(vp+(sy/2)*planes[2].getRowStride()+(sx/2)*planes[2].getPixelStride())&255)-128;int base=298*Math.max(0,yy);pixels[row*w+col]=Color.rgb(clamp((base+409*vv+128)>>8),clamp((base-100*uu-208*vv+128)>>8),clamp((base+516*uu+128)>>8));}}
        Bitmap result=Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888);result.setDensity(Bitmap.DENSITY_NONE);if(rotation!=0){Matrix matrix=new Matrix();matrix.postRotate(rotation);Bitmap turned=Bitmap.createBitmap(result,0,0,w,h,matrix,true);result.recycle();result=turned;}return result;
    }
    private static int clamp(int n){return Math.max(0,Math.min(255,n));}
    @Override public void close(){if(codec!=null){try{codec.stop();}catch(Exception ignored){}codec.release();codec=null;}extractor.release();}
}
