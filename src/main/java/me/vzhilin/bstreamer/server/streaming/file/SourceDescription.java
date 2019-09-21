package me.vzhilin.bstreamer.server.streaming.file;


import org.bytedeco.javacpp.avutil;

public class SourceDescription {
    private byte[] sps;
    private byte[] pps;
    private avutil.AVRational timebase;
    private avutil.AVRational avgFrameRate;
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

    public void setTimebase(avutil.AVRational timebase) {
        this.timebase = timebase;
    }

    public avutil.AVRational getTimebase() {
        return timebase;
    }

    public void setAvgFrameRate(avutil.AVRational avgFrameRate) {
        this.avgFrameRate = avgFrameRate;
    }

    public avutil.AVRational getAvgFrameRate() {
        return avgFrameRate;
    }

    public void setVideoStreamId(int videoStreamId) {
        this.videoStreamId = videoStreamId;
    }

    public int getVideoStreamId() {
        return videoStreamId;
    }
}
