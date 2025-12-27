package me.vzhilin.bstreamer.server.streaming.picture;

import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avutil.AVRational;

import static org.bytedeco.ffmpeg.global.avutil.av_opt_set;

public class H264CodecParameters {
    private int bitrate = 400000;
    private int width = 640;
    private int height = 480;
    private final AVRational timebase;
    private int gopSize = 10;
    private int maxBFrames = 1;
    private int fps;
    private String profile;

    public H264CodecParameters() {
        timebase = new AVRational();
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

    public AVRational getTimebase() {
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

    public void setParameters(AVCodecContext c) {
        c.bit_rate(bitrate);
        c.width(width);
        c.height(height);
        c.time_base(timebase);
        c.gop_size(gopSize);
        c.max_b_frames(maxBFrames);
        if (profile != null && !profile.isEmpty()) {
            if ("libopenh264".equals(c.codec().name().getString())) {
                String libopenh264_profileId;
                switch (profile) {
                    case "baseline":
                        libopenh264_profileId = "66";
                        break;
                    case "main":
                        libopenh264_profileId = "77";
                        break;
                    case "high":
                        libopenh264_profileId = "100";
                        break;
                    default: libopenh264_profileId = "66";
                }
                av_opt_set(c.priv_data(), "profile", libopenh264_profileId, 0);
            } else {
                av_opt_set(c.priv_data(), "profile", profile, 0);
            }
        }
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public int getFps() {
        return fps;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}
