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
import java.util.LinkedList;
import java.util.Queue;

import static org.ffmpeg.avcodec.AvcodecLibrary.AV_PKT_FLAG_KEY;
import static org.ffmpeg.avcodec.AvcodecLibrary.av_packet_unref;
import static org.ffmpeg.avformat.AvformatLibrary.*;

public class FileMediaPacketSource implements MediaPacketSource {
    private Pointer<AVPacket> pktPtr;
    private Pointer<Pointer<AVFormatContext>> pAvfmtCtx;
    private Pointer<AVFormatContext> ifmtCtx;
    private boolean wasClosed;
    private MediaPacketSourceDescription desc;

    private Queue<MediaPacket> packetQueue = new LinkedList<>();

    public FileMediaPacketSource(File file) throws IOException {
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

        int videoStreamId = avStream.index();
        AVRational streamTimebase = avStream.time_base();
        AVRational avgFrameRate = avStream.avg_frame_rate();

        Pointer<AVCodecParameters> cp = avStream.codecpar();
        AVCodecParameters codecpar = cp.get();
        byte[] extradataBytes = codecpar.extradata().getBytes(codecpar.extradata_size());

        AVCCExtradataParser extradata = new AVCCExtradataParser(extradataBytes);
        byte[] sps = extradata.getSps();
        byte[] pps = extradata.getPps();

        desc = new MediaPacketSourceDescription();
        desc.setSps(sps);
        desc.setPps(pps);
        desc.setTimebase(streamTimebase);
        desc.setAvgFrameRate(avgFrameRate);
        desc.setVideoStreamId(videoStreamId);
        nextAvPacket();
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
    public MediaPacketSourceDescription getDesc() {
        return desc;
    }

    @Override
    public boolean hasNext() {
        return !packetQueue.isEmpty();
    }

    @Override
    public MediaPacket next() {
        MediaPacket pkt = packetQueue.poll();
        if (packetQueue.isEmpty()) {
            boolean eof = nextAvPacket();
            if (eof) {
                return null;
            }
        }

        return pkt;
    }

    private boolean nextAvPacket() {
        while (true) {
            av_packet_unref(pktPtr);
            if (av_read_frame(ifmtCtx, pktPtr) < 0) {
                return true;
            }

            AVPacket pkt = pktPtr.get();
            if (pkt.stream_index() == desc.getVideoStreamId()) {
                long pts = pkt.pts();
                long dts = pkt.dts();
                boolean isKey = (pkt.flags() & AV_PKT_FLAG_KEY) != 0;

                int sz = pkt.size();
                byte[] data = pkt.data().getBytes(sz);
                int offset = 0;
                while (offset < data.length) {
                    int avccLen = ((data[offset] & 0xff) << 24) +
                            ((data[offset + 1] & 0xff) << 16) +
                            ((data[offset + 2] & 0xff) << 8) +
                            (data[offset + 3] & 0xff);

                    ByteBuf payload = PooledByteBufAllocator.DEFAULT.buffer(avccLen, avccLen);
                    payload.writeBytes(data, offset + 4, avccLen);

                    packetQueue.offer(new MediaPacket(pts, dts, isKey, payload));
                    offset += (avccLen + 4);
                }

                return false;
            }
        }
    }

    @Override
    public void close() {
        if (!wasClosed) {
            wasClosed = true;
            if (hasNext()) {
                av_packet_unref(pktPtr);
            }
            avformat_close_input(pAvfmtCtx);
            pAvfmtCtx.release();
            pktPtr.release();
        }
    }
}
