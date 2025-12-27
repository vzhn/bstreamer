package me.vzhilin.bstreamer.server.streaming.file;


import org.bytedeco.ffmpeg.avutil.AVRational;

public class SourceDescription {
    private byte[] sps;
    private byte[] pps;
    private AVRational timebase;
    private AVRational avgFrameRate;
    private int videoStreamId;

    public void setSps(byte[] sps) {
        this.sps = sps;
    }

    public byte[] getSps() {
        return sps;
    }

    public void setPps(byte[] pps) {
        this.pps = pps;
    }

    public byte[] getPps() {
        return pps;
    }

    public void setTimebase(AVRational timebase) {
        this.timebase = timebase;
    }

    public AVRational getTimebase() {
        return timebase;
    }

    public void setAvgFrameRate(AVRational avgFrameRate) {
        this.avgFrameRate = avgFrameRate;
    }

    public AVRational getAvgFrameRate() {
        return avgFrameRate;
    }

    public void setVideoStreamId(int videoStreamId) {
        this.videoStreamId = videoStreamId;
    }

    public int getVideoStreamId() {
        return videoStreamId;
    }
}
