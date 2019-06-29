package me.vzhilin.mediaserver.media.picture;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avutil;

public class H264CodecParameters {
    private int bitrate = 400000;
    private int width = 640;
    private int height = 480;
    private avutil.AVRational timebase;
    private int gopSize = 10;
    private int maxBFrames = 1;
    private int fps;

    public H264CodecParameters() {
        timebase = new avutil.AVRational();
        timebase.num(1);
        timebase.den(25);
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public avutil.AVRational getTimebase() {
        return timebase;
    }

    public void setTimebase(int num, int den) {
        this.timebase.num(num);
        this.timebase.den(den);
    }

    public int getGopSize() {
        return gopSize;
    }

    public void setGopSize(int gopSize) {
        this.gopSize = gopSize;
    }

    public int getMaxBFrames() {
        return maxBFrames;
    }

    public void setMaxBFrames(int maxBFrames) {
        this.maxBFrames = maxBFrames;
    }

    public void setParameters(avcodec.AVCodecContext c) {
        c.bit_rate(bitrate);
        c.width(width);
        c.height(height);
        c.time_base(timebase);
        c.gop_size(gopSize);
        c.max_b_frames(maxBFrames);
    }

    public void setFps(int fps) {
        this.fps = fps;
    }
}
