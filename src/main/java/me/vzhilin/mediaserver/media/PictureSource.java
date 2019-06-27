package me.vzhilin.mediaserver.media;

import io.netty.buffer.Unpooled;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avutil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

import static java.lang.System.exit;
import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avutil.*;
import static org.bytedeco.javacpp.swscale.*;

public class PictureSource implements MediaPacketSource {
    private final MediaPacketSourceDescription desc;
    private final AVPacket pkt;
    private final AVCodecContext c;
    private final AVRational rtpTimebase;
    private byte[] sps;
    private byte[] pps;
    private AVFrame frame;
    private AVFrame rgbFrame;
    private BufferedImage image;
    private SwsContext swsContext;
    private long frameNumber = 0;
    private Deque<MediaPacket> queue = new LinkedList<MediaPacket>();

    public PictureSource() {
        initImage();

        avcodec.AVCodec codec = avcodec_find_encoder(AV_CODEC_ID_H264);
        c = avcodec_alloc_context3(codec);
        avutil.AVRational timebase = new avutil.AVRational();
        timebase.num(1);
        timebase.den(25);

        avutil.AVRational framerate = new avutil.AVRational();
        framerate.num(25);
        framerate.den(1);

        rtpTimebase = new AVRational();
        rtpTimebase.num(1);
        rtpTimebase.den(1000);

        c.bit_rate(400000);
        c.width(352);
        c.height(288);
        c.time_base(timebase);
        c.gop_size(10);
        c.max_b_frames(1);
        c.pix_fmt(avutil.AV_PIX_FMT_YUV420P);
        c.flags(c.flags() | AV_CODEC_FLAG_GLOBAL_HEADER);
        if (avcodec_open2(c, codec, (avutil.AVDictionary) null) < 0) {
            System.err.println("could not open codec");
            exit(1);
        }
        byte[] extradata = new byte[c.extradata_size()];
        c.extradata().get(extradata);
        
        parseSpsPps(extradata);
        desc = new MediaPacketSourceDescription();
        desc.setSps(sps);
        desc.setPps(pps);
        desc.setTimebase(timebase);
        desc.setAvgFrameRate(framerate);
        desc.setVideoStreamId(0);

        System.err.println(extradata);

        pkt = av_packet_alloc();
        initFrames(c);
    }

    private void initImage() {
        image = new BufferedImage(352, 288, BufferedImage.TYPE_3BYTE_BGR);
    }

    private void initFrames(AVCodecContext c) {
        frame = av_frame_alloc();
        frame.format(c.pix_fmt());
        frame.width(c.width());
        frame.height(c.height());
        int ret = av_frame_get_buffer(frame, 32);
        if (ret < 0) {
            System.err.println("Could not allocate the video frame data");
            exit(1);
        }
        rgbFrame = av_frame_alloc();
        rgbFrame.format(AV_PIX_FMT_BGR24);
        rgbFrame.width(c.width());
        rgbFrame.height(c.height());
        ret = av_frame_get_buffer(rgbFrame, 32);
        if (ret < 0) {
            System.err.println("Could not allocate the video frame data");
            exit(1);
        }
        swsContext = sws_getContext(rgbFrame.width(), rgbFrame.height(), rgbFrame.format(),
                frame.width(), frame.height(), frame.format(), SWS_BICUBIC,
                null, null, (DoublePointer) null);

        if (swsContext.isNull()) {
            System.err.println("Could not init sws context!");
            exit(1);
        }
    }

    private void parseSpsPps(byte[] extradata) {
        int[] seps = new int[2];
        int sep = 0;
        for (int i = 0; i < extradata.length - 4; /* */) {
            if (extradata[i] == 0 && 
                extradata[i + 1] == 0 && 
                extradata[i + 2] == 0 &&                
                extradata[i + 3] == 1) {
                
                seps[sep++] = i;
                
                i += 4;
            } else {
                ++i;
            }
        }

        sps = Arrays.copyOfRange(extradata, 4, seps[1]);
        pps = Arrays.copyOfRange(extradata, seps[1] + 4, extradata.length);
    }

    @Override
    public MediaPacketSourceDescription getDesc() {
        return desc;
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public MediaPacket next() {
        while (queue.isEmpty()) {
            refreshPicture();

            /* make sure the frame data is writable */
            int ret = av_frame_make_writable(frame);
            if (ret < 0) {
                exit(1);
            }
            ret = av_frame_make_writable(rgbFrame);
            if (ret < 0) {
                exit(1);
            }

            PointerPointer rgbData = rgbFrame.data();
            rgbData.put(((DataBufferByte) image.getRaster().getDataBuffer()).getData());
            sws_scale(swsContext, rgbData,rgbFrame.linesize(), 0, c.height(), frame.data(), frame.linesize());

            frame.pts(frameNumber++);
            encode(c, frame, pkt);
        }

        return queue.poll();
    }

    private void refreshPicture() {
        Graphics gc = image.getGraphics();
        gc.setColor(Color.ORANGE);
        gc.fillRect(0, 0, 352, 288);
        gc.setColor(Color.BLACK);
        gc.drawString("Hello, world! " + frameNumber, 100, 100);
        gc.dispose();
    }

    private void encode(AVCodecContext c, AVFrame frame, AVPacket pkt) {
        int ret = avcodec_send_frame(c, frame);
        if (ret < 0) {
            System.err.println("Error sending a frame for encoding");
            exit(1);
        }

        while (ret >= 0) {
            ret = avcodec_receive_packet(c, pkt);
            if (ret == AVERROR_EAGAIN() || ret == AVERROR_EOF)
                return;
            else if (ret < 0) {
                System.err.println("Error during encoding");
                exit(1);
            }
            av_packet_rescale_ts(pkt, c.time_base(), rtpTimebase);
            byte[] data = new byte[pkt.size()];
            pkt.data().get(data);

            queue.offer(new MediaPacket(pkt.pts(), pkt.dts(), (pkt.flags() & AV_PKT_FLAG_KEY) != 0, Unpooled.wrappedBuffer(data)));
            av_packet_unref(pkt);
        }
    }

    @Override
    public void close() throws IOException {
        sws_freeContext(swsContext);
        avcodec_free_context(c);
        av_frame_free(rgbFrame);
        av_frame_free(frame);
    }
}
