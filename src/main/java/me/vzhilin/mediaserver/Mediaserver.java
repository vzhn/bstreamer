package me.vzhilin.mediaserver;

import me.vzhilin.mediaserver.server.RtspServer;
import org.bridj.Pointer;
import org.ffmpeg.avcodec.AVPacket;
import org.ffmpeg.avcodec.AvcodecLibrary;
import org.ffmpeg.avformat.AVFormatContext;
import org.ffmpeg.avformat.AVInputFormat;
import org.ffmpeg.avformat.AVStream;
import org.ffmpeg.avformat.AvformatLibrary;

public class Mediaserver {
    public static void main(String... argv) {
        new Mediaserver().start();
    }

    private void start() {
        new RtspServer().start();
    }

    private void test() {

        AvformatLibrary avformat = new AvformatLibrary();
        AvcodecLibrary avcodec = new AvcodecLibrary();
        avcodec.avcodec_register_all();
        Pointer<AVPacket> pktPtr = Pointer.allocate(AVPacket.class);

        Pointer<Pointer<AVFormatContext>> pAvfmtCtx = Pointer.allocatePointer(AVFormatContext.class);
        Pointer<Byte> name = Pointer.pointerToCString("/home/vzhilin/misc/video_samples/simpsons_video.mkv");

        Pointer<AVInputFormat> fmt = (Pointer<AVInputFormat>) Pointer.NULL;
//        Pointer<Pointer<AvdeviceLibrary.AVDictionary> > options = (Pointer<Pointer<AvdeviceLibrary.AVDictionary>>) Pointer.NULL;

        avformat.avformat_open_input(pAvfmtCtx, name, fmt, Pointer.NULL);
        Pointer<AVFormatContext> ifmtCtx = pAvfmtCtx.get();

        int r = avformat.avformat_find_stream_info(ifmtCtx, Pointer.NULL);
        avformat.av_dump_format(ifmtCtx, 0, name, 0);

        Pointer<AVStream> p = ifmtCtx.get().streams().get();
        System.err.println(p.getLong());

        long t1 = System.currentTimeMillis();
        long size = 0;
        while (true) {
            int ret = avformat.av_read_frame(ifmtCtx, pktPtr);
            if (ret < 0) {
                break;
            }

            AVPacket avpacket = pktPtr.get();
            int sz = avpacket.size();

            byte[] dataArray = avpacket.data().getBytes(sz);
            size += dataArray.length;
            System.err.println(avpacket.pts() + " " + avpacket.dts() + " " + sz);

            avcodec.av_packet_unref(pktPtr);
        }
        avcodec.av_free_packet(pktPtr);
        avformat.avformat_close_input(pAvfmtCtx);

        long delta = System.currentTimeMillis() - t1;
        System.err.println(delta);
        System.err.println(size);

        System.err.println((float) size / delta * 1000 / 1e9);
        name.release();
        pAvfmtCtx.release();
    }
}
