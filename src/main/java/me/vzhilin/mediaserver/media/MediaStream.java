package me.vzhilin.mediaserver.media;

import io.netty.buffer.Unpooled;
import org.bridj.Pointer;
import org.ffmpeg.avcodec.AVPacket;
import org.ffmpeg.avformat.AVFormatContext;
import org.ffmpeg.avformat.AVInputFormat;
import org.ffmpeg.avformat.AVStream;
import org.ffmpeg.avformat.AvformatLibrary;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import static org.ffmpeg.avcodec.AvcodecLibrary.AV_PKT_FLAG_KEY;
import static org.ffmpeg.avcodec.AvcodecLibrary.av_packet_unref;
import static org.ffmpeg.avformat.AvformatLibrary.avformat_close_input;

public class MediaStream implements Closeable {
    private final AvformatLibrary avformat = new AvformatLibrary();
    private final Pointer<AVFormatContext> ifmtCtx;
    private final Pointer<AVPacket> pktPtr;
    private final AVStream videoStream;
    private final Pointer<Pointer<AVFormatContext>> pAvfmtCtx;

    public MediaStream() {
        pktPtr = Pointer.allocate(AVPacket.class);

        pAvfmtCtx = Pointer.allocatePointer(AVFormatContext.class);
        Pointer<Byte> name = Pointer.pointerToCString("/home/vzhilin/misc/video_samples/simpsons_video.mkv");
        Pointer<AVInputFormat> fmt = (Pointer<AVInputFormat>) Pointer.NULL;

        avformat.avformat_open_input(pAvfmtCtx, name, fmt, Pointer.NULL);
        ifmtCtx = pAvfmtCtx.get();

        int r = avformat.avformat_find_stream_info(ifmtCtx, Pointer.NULL);
        videoStream = ifmtCtx.get().streams().get().get();
    }

    public List<MediaPacket> next() {
        int ret = avformat.av_read_frame(ifmtCtx, pktPtr);
        if (ret < 0) {
            return null;
        }

//        av_packet_rescale_ts(pktPtr, videoStream.time_base(), videoStream.time_base());
        AVPacket avpacket = pktPtr.get();
        int sz = avpacket.size();
        boolean isKey = (avpacket.flags() & AV_PKT_FLAG_KEY) != 0;
        long pts = avpacket.pts();
        long dts = avpacket.dts();

        byte[] data = avpacket.data().getBytes(sz);
        av_packet_unref(pktPtr);

        int offset = 0;
        List<MediaPacket> ls = new ArrayList<>();
        while (offset < data.length) {

            int avccLen = ((data[offset] & 0xff) << 24) +
                    ((data[offset + 1] & 0xff) << 16) +
                    ((data[offset + 2] & 0xff) << 8) +
                    (data[offset + 3] & 0xff);

            ls.add(new MediaPacket(pts, dts, isKey, Unpooled.wrappedBuffer(data, offset + 4, avccLen)));
            offset += (avccLen + 4);
        }

        return ls;
    }

    @Override
    public void close()  {
        avformat_close_input(pAvfmtCtx);
    }
}
