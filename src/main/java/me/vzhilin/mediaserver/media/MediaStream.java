package me.vzhilin.mediaserver.media;

import io.netty.buffer.Unpooled;
import me.vzhilin.mediaserver.InterleavedFrame;
import me.vzhilin.mediaserver.MediaPacketEncoder;
import org.bridj.Pointer;
import org.ffmpeg.avcodec.AVPacket;
import org.ffmpeg.avformat.AVFormatContext;
import org.ffmpeg.avformat.AVInputFormat;
import org.ffmpeg.avformat.AVStream;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import static org.ffmpeg.avcodec.AvcodecLibrary.AV_PKT_FLAG_KEY;
import static org.ffmpeg.avcodec.AvcodecLibrary.av_packet_unref;
import static org.ffmpeg.avformat.AvformatLibrary.*;

public class MediaStream implements Closeable {
    public static List<InterleavedFrame> readAllPackets() {
        Pointer<AVPacket> pktPtr = Pointer.allocate(AVPacket.class);
        Pointer<Pointer<AVFormatContext>> pAvfmtCtx = Pointer.allocatePointer(AVFormatContext.class);
        Pointer<Byte> name = Pointer.pointerToCString("/home/vzhilin/misc/video_samples/simpsons_video.mkv");
        Pointer<AVInputFormat> fmt = (Pointer<AVInputFormat>) Pointer.NULL;

        avformat_open_input(pAvfmtCtx, name, fmt, Pointer.NULL);
        Pointer<AVFormatContext> ifmtCtx = pAvfmtCtx.get();

        int r = avformat_find_stream_info(ifmtCtx, Pointer.NULL);
        AVStream videoStream = ifmtCtx.get().streams().get().get();
        MediaPacketEncoder encoder = new MediaPacketEncoder();

        List<InterleavedFrame> rr = new ArrayList<>();
        while (true) {
            int ret = av_read_frame(ifmtCtx, pktPtr);
            if (ret < 0) {
                break;
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

            while (offset < data.length) {
                int avccLen = ((data[offset] & 0xff) << 24) +
                        ((data[offset + 1] & 0xff) << 16) +
                        ((data[offset + 2] & 0xff) << 8) +
                        (data[offset + 3] & 0xff);

                encoder.encode(rr, isKey, pts, dts, Unpooled.wrappedBuffer(data, offset + 4, avccLen));
                offset += (avccLen + 4);
            }
        }

        avformat_close_input(pAvfmtCtx);
        return rr;
    }

    @Override
    public void close()  {

    }
}
