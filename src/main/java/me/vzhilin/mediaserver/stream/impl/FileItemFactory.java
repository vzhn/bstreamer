package me.vzhilin.mediaserver.stream.impl;

import me.vzhilin.mediaserver.media.MediaPacket;
import me.vzhilin.mediaserver.stream.ItemFactory;
import org.bridj.Pointer;
import org.ffmpeg.avcodec.AVPacket;
import org.ffmpeg.avformat.AVFormatContext;
import org.ffmpeg.avformat.AVInputFormat;
import org.ffmpeg.avformat.AVStream;

import static org.ffmpeg.avformat.AvformatLibrary.avformat_find_stream_info;
import static org.ffmpeg.avformat.AvformatLibrary.avformat_open_input;

public class FileItemFactory implements ItemFactory<MediaPacket> {
    private final Pointer<AVPacket> pktPtr;
    private final Pointer<Pointer<AVFormatContext>> pAvfmtCtx;
    private final Pointer<AVFormatContext> ifmtCtx;
    private final AVStream videoStream;

    public FileItemFactory(String fname) {
        pktPtr = Pointer.allocate(AVPacket.class);

        pAvfmtCtx = Pointer.allocatePointer(AVFormatContext.class);
        Pointer<Byte> name = Pointer.pointerToCString(fname);
        Pointer<AVInputFormat> fmt = (Pointer<AVInputFormat>) Pointer.NULL;

        avformat_open_input(pAvfmtCtx, name, fmt, Pointer.NULL);
        ifmtCtx = pAvfmtCtx.get();

        int r = avformat_find_stream_info(ifmtCtx, Pointer.NULL);
        videoStream = ifmtCtx.get().streams().get().get();
    }

    @Override
    public MediaPacket next() {
        return null;
    }

    @Override
    public void free(MediaPacket item) {

    }
}
