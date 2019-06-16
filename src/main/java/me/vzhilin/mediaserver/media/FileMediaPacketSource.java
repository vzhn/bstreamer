package me.vzhilin.mediaserver.media;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import me.vzhilin.mediaserver.util.AVCCExtradataParser;
import org.bridj.Pointer;
import org.ffmpeg.avcodec.AVCodecParameters;
import org.ffmpeg.avcodec.AVPacket;
import org.ffmpeg.avcodec.AvcodecLibrary;
import org.ffmpeg.avformat.AVFormatContext;
import org.ffmpeg.avformat.AVStream;
import org.ffmpeg.avutil.AVRational;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.ffmpeg.avcodec.AvcodecLibrary.AV_PKT_FLAG_KEY;
import static org.ffmpeg.avcodec.AvcodecLibrary.av_packet_unref;
import static org.ffmpeg.avformat.AvformatLibrary.*;

public class FileMediaPacketSource implements MediaPacketSource {
    private boolean hasNext;
    private final File file;
    private Pointer<AVPacket> pktPtr;
    private Pointer<Pointer<AVFormatContext>> pAvfmtCtx;
    private Pointer<AVFormatContext> ifmtCtx;
    private boolean wasClosed;
    private byte[] sps;
    private byte[] pps;
    private AVRational streamTimebase;
    private AVRational avgFrameRate;
    private int videoStreamId;

    public FileMediaPacketSource(File file) throws IOException {
        this.file = file;

        if (file.exists()) {
            open(file);
        } else {
            throw new FileNotFoundException("file not found: " + file);
        }
    }

    private void open(File file) throws IOException {
        int r;
        pktPtr = Pointer.allocate(AVPacket.class);
        pAvfmtCtx = Pointer.allocatePointer(AVFormatContext.class);

        Pointer<Byte> name = Pointer.pointerToCString(file.getAbsolutePath());
        r = avformat_open_input(pAvfmtCtx, name, null, null);
        name.release();

        if (r < 0) {
            pAvfmtCtx.release();
            pktPtr.release();
            wasClosed = true;
            throw new IOException("avformat_open_input error: " + r);
        }

        ifmtCtx = pAvfmtCtx.get();
        r = avformat_find_stream_info(ifmtCtx, null);
        if (r < 0) {
            wasClosed = true;
            avformat_close_input(pAvfmtCtx);
            pAvfmtCtx.release();
            pktPtr.release();
            throw new IOException("error: " + r);
        }

        AVStream avStream = getVideoStream();
        if (avStream == null) {
            close();
            throw new IOException("h264 stream not found");
        }

        videoStreamId = avStream.index();
        streamTimebase = avStream.time_base();
        avgFrameRate = avStream.avg_frame_rate();

        Pointer<AVCodecParameters> cp = avStream.codecpar();
        AVCodecParameters codecpar = cp.get();
        byte[] extradataBytes = codecpar.extradata().getBytes(codecpar.extradata_size());

        AVCCExtradataParser extradata = new AVCCExtradataParser(extradataBytes);
        this.sps = extradata.getSps();
        this.pps = extradata.getPps();

        hasNext = av_read_frame(ifmtCtx, pktPtr) >= 0;
        if (hasNext && pktPtr.get().stream_index() != videoStreamId) {
            hasNext = nextPacket();
        }
    }

    private AVStream getVideoStream() {
        int ns = ifmtCtx.get().nb_streams();
        for (int i = 0; i < ns; i++) {
            Pointer<AVStream> pstream = ifmtCtx.get().streams().get(i);
            AVStream stream = pstream.get();
            AVCodecParameters cp = stream.codecpar().get();
            if (cp.codec_id().value() == AvcodecLibrary.AVCodecID.AV_CODEC_ID_H264.value()) {
                return stream;
            }
        }

        return null;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public MediaPacket next() {
        AVPacket avpacket = pktPtr.get();
        int sz = avpacket.size();
        boolean isKey = (avpacket.flags() & AV_PKT_FLAG_KEY) != 0;
        long pts = avpacket.pts();
        long dts = avpacket.dts();
        byte[] data = avpacket.data().getBytes(sz);

        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(data.length, data.length);
        buffer.writeBytes(data);
        MediaPacket pkt = new MediaPacket(pts, dts, isKey, buffer);

        hasNext = nextPacket();
        return pkt;
    }

    private boolean nextPacket() {
        boolean eof = false;
        while (!eof) {
            av_packet_unref(pktPtr);
            if (av_read_frame(ifmtCtx, pktPtr) < 0) {
                eof = true;
            } else
            if (pktPtr.get().stream_index() == videoStreamId) {
                break;
            }
        }

        return !eof;
    }

    @Override
    public void close() {
        if (!wasClosed) {
            wasClosed = true;
            if (hasNext) {
                av_packet_unref(pktPtr);
            }
            avformat_close_input(pAvfmtCtx);
            pAvfmtCtx.release();
            pktPtr.release();
        }
    }

    public byte[] getSps() {
        return sps;
    }

    public byte[] getPps() {
        return pps;
    }

    public AVRational getStreamTimebase() {
        return streamTimebase;
    }

    public AVRational getAvgFrameRate() {
        return avgFrameRate;
    }
}
