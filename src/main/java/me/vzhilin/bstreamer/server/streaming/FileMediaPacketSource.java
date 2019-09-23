package me.vzhilin.bstreamer.server.streaming;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import me.vzhilin.bstreamer.server.ServerContext;
import me.vzhilin.bstreamer.server.media.AVCCExtradataParser;
import me.vzhilin.bstreamer.server.streaming.base.PullSource;
import me.vzhilin.bstreamer.server.streaming.file.FileSourceAttributes;
import me.vzhilin.bstreamer.server.streaming.file.MediaPacket;
import me.vzhilin.bstreamer.server.streaming.file.SourceDescription;
import me.vzhilin.bstreamer.util.AppRuntime;
import me.vzhilin.bstreamer.util.PropertyMap;
import org.bytedeco.javacpp.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avformat.*;

public class FileMediaPacketSource implements PullSource {
    private static final String WORKDIR_VIDEO = "video";
    private static final String WORKDIR_SRC_DEPLOY_VIDEO = "src/deploy/video";

    private boolean wasClosed;
    private SourceDescription desc;
    private Queue<MediaPacket> packetQueue = new LinkedList<>();
    private AVPacket pk;
    private AVFormatContext pAvfmtCtx;

    public FileMediaPacketSource(ServerContext context, PropertyMap sourceProperties) throws IOException {
        String dirPath = sourceProperties.getString(FileSourceAttributes.DIR);
        File dir = probeDirectories(dirPath);

        File videoFile = new File(dir, sourceProperties.getString(FileSourceAttributes.FILE));
        if (videoFile.exists()) {
            open(videoFile);
        } else {
            throw new FileNotFoundException(videoFile.getAbsolutePath());
        }
    }

    private File probeDirectories(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.isAbsolute()) {
            dir = new File(AppRuntime.APP_PATH, dirPath);
        }

        if (!dir.exists()) {
            dir = new File(WORKDIR_VIDEO);
        }

        if (!dir.exists()) {
            dir = new File(WORKDIR_SRC_DEPLOY_VIDEO);
        }

        return dir;
    }

    private void open(File file) throws IOException {
        pAvfmtCtx = new avformat.AVFormatContext(null);
        int r = avformat_open_input(pAvfmtCtx, new BytePointer(file.getAbsolutePath()), null, null);
        if (r < 0) {
            pAvfmtCtx.close();
            wasClosed = true;
            throw new IOException("avformat_open_input error: " + r);
        }
        r = avformat_find_stream_info(pAvfmtCtx, (PointerPointer) null);
        if (r < 0) {
            wasClosed = true;
            avformat_close_input(pAvfmtCtx);
            pAvfmtCtx.close();
            throw new IOException("error: " + r);
        }
        pk = new avcodec.AVPacket();
        AVStream avStream = getVideoStream();
        if (avStream == null) {
            close();
            throw new IOException("h264 stream not found");
        }
        int videoStreamId = avStream.index();
        avutil.AVRational streamTimebase = avStream.time_base();
        avutil.AVRational avgFrameRate = avStream.avg_frame_rate();
        avcodec.AVCodecParameters cp = avStream.codecpar();
        byte[] extradataBytes = new byte[cp.extradata_size()];
        cp.extradata().get(extradataBytes);
        AVCCExtradataParser extradata = new AVCCExtradataParser(extradataBytes);
        byte[] sps = extradata.getSps();
        byte[] pps = extradata.getPps();
        desc = new SourceDescription();
        desc.setSps(sps);
        desc.setPps(pps);
        desc.setTimebase(streamTimebase);
        desc.setAvgFrameRate(avgFrameRate);
        desc.setVideoStreamId(videoStreamId);
        fillQueue();
    }

    private AVStream getVideoStream() {
        int ns = pAvfmtCtx.nb_streams();
        for (int i = 0; i < ns; i++) {
            AVStream pstream = pAvfmtCtx.streams(i);
            avcodec.AVCodecParameters cp = pstream.codecpar();
            if (cp.codec_id() == AV_CODEC_ID_H264) {
                return pstream;
            }
        }
        return null;
    }

    @Override
    public SourceDescription getDesc() {
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
            fillQueue();
        }
        return pkt;
    }

    private void fillQueue() {
        while (packetQueue.size() < 10) {
            if (av_read_frame(pAvfmtCtx, pk) < 0) {
                return;
            }

            if (pk.stream_index() == desc.getVideoStreamId()) {
                long pts = pk.pts();
                long dts = pk.dts();
                boolean isKey = (pk.flags() & AV_PKT_FLAG_KEY) != 0;
                int sz = pk.size();
                byte[] data = new byte[sz];
                pk.data().get(data);
                int offset = 0;
                while (offset < data.length) {
                    int avccLen =
                            ((data[offset] & 0xff) << 24) +
                            ((data[offset + 1] & 0xff) << 16) +
                            ((data[offset + 2] & 0xff) << 8) +
                            (data[offset + 3] & 0xff);
                    ByteBuf payload = PooledByteBufAllocator.DEFAULT.buffer(avccLen, avccLen);
                    payload.writeBytes(data, offset + 4, avccLen);
                    packetQueue.offer(new MediaPacket(pts, dts, isKey, payload));
                    offset += (avccLen + 4);
                }
            }

            av_packet_unref(pk);
        }
    }

    @Override
    public void close() {
        if (!wasClosed) {
            wasClosed = true;
            avformat_close_input(pAvfmtCtx);
            packetQueue.forEach(mediaPacket -> mediaPacket.getPayload().release());
            packetQueue.clear();
            pAvfmtCtx.close();
            pk.close();
        }
    }
}
