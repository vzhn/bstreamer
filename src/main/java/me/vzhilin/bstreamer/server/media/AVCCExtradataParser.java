package me.vzhilin.bstreamer.server.media;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public final class AVCCExtradataParser {
    private final byte[] sps;
    private final byte[] pps;

    public AVCCExtradataParser(byte[] extradata) {
        ByteBuf is = Unpooled.wrappedBuffer(extradata);
        int v = is.readByte();

        int profile = is.readByte();
        int compatibility = is.readByte();
        int level = is.readByte();

        int naluLengthMinusOne = (is.readByte() & 0xff) & 0b11;
        if (naluLengthMinusOne != 3) {
            throw new RuntimeException("not supported: naluLengthMinusOne != 3");
        }

        int spsNumber = is.readByte() & 0b11111;
        if (spsNumber != 1) {
            throw new RuntimeException("not supported: spsNumber != 1");
        }

        int spsLen = ((is.readByte() & 0xff) << 8) + is.readByte() & 0xff;
        sps = new byte[spsLen];
        is.readBytes(sps);

        int numPps = is.readByte() & 0xff;
        if (numPps != 1) {
            throw new RuntimeException();
        }

        int ppsLen = ((is.readByte() & 0xff) << 8) + is.readByte() & 0xff;
        pps = new byte[ppsLen];
        is.readBytes(pps);
    }

    public byte[] getSps() {
        return sps;
    }

    public byte[] getPps() {
        return pps;
    }
}
