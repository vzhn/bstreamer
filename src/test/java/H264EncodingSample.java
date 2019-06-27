import org.bytedeco.javacpp.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import static java.lang.System.exit;
import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.*;
import static org.bytedeco.javacpp.presets.avutil.AVERROR_EAGAIN;
import static org.bytedeco.javacpp.swscale.*;

public class H264EncodingSample {
    public static void main(String... argv) {
        new H264EncodingSample().start();
    }

    private void start() {
        BufferedImage image = new BufferedImage(352, 288, BufferedImage.TYPE_3BYTE_BGR);
        Graphics gc = image.getGraphics();
        gc.setColor(Color.ORANGE);
        gc.fillRect(0, 0, 352, 288);

        gc.setColor(Color.BLACK);
        gc.drawString("Hello, world!", 100, 100);


        avcodec.AVCodec codec = avcodec_find_encoder(AV_CODEC_ID_H264);
        avcodec.AVCodecContext c = initCodecContext(codec);

        avformat.AVFormatContext oc = new avformat.AVFormatContext(null);
        avformat.avformat_alloc_output_context2(oc, null, "matroska", (String) null);
        if (oc.isNull()) {
            exit(1);
        }

        avformat.AVStream st = avformat_new_stream(oc, codec);
        if (st.isNull()) {
            System.err.println("Could not allocate stream");
            exit(1);
        }

        String filename = "output.mkv";
        /* open the output file, if needed */
        if ((oc.flags() & AVFMT_NOFILE) == 0) {
            AVIOContext avioContext = new AVIOContext(null);
            if (avio_open(avioContext, filename, AVIO_FLAG_WRITE) < 0) {
                System.err.printf("Could not open '%s'\n", filename);
                exit(1);
            }
            oc.pb(avioContext);
        }

        if (avcodec_parameters_from_context(st.codecpar(), c) < 0) {
            System.err.println("Could not copy parameters");
            exit(1);
        }
        av_dump_format(oc, 0, filename, 1);

        /* Write the stream header, if any. */
        if (avformat_write_header(oc, (PointerPointer) null) < 0) {
            System.err.println("Error occurred when opening output file\n");
            exit(1);
        }

        avutil.AVFrame frame = av_frame_alloc();
        frame.format(c.pix_fmt());
        frame.width(c.width());
        frame.height(c.height());

        avutil.AVFrame rgbFrame = av_frame_alloc();
        rgbFrame.format(AV_PIX_FMT_BGR24);
        rgbFrame.width(frame.width());
        rgbFrame.height(frame.height());

        SwsContext swsContext = sws_getContext(c.width(), c.height(), rgbFrame.format(),
                c.width(), c.height(), frame.format(), SWS_BICUBIC,
                null, null, (DoublePointer) null);

        if (swsContext.isNull()) {
            System.err.println("Could not init sws context!");
            exit(1);
        }


        int ret = av_frame_get_buffer(frame, 32);
        if (ret < 0) {
            System.err.println("Could not allocate the video frame data");
            exit(1);
        }

        ret = av_frame_get_buffer(rgbFrame, 32);
        if (ret < 0) {
            System.err.println("Could not allocate the video frame data");
            exit(1);
        }

        avcodec.AVCodecParameters cp = new avcodec.AVCodecParameters();
        avcodec_parameters_from_context(cp, c);
        avcodec.AVPacket pkt = av_packet_alloc();

        /* encode 1 second of video */
        for (int i = 0; i < 25; i++) {
            /* make sure the frame data is writable */
            ret = av_frame_make_writable(frame);
            if (ret < 0) {
                exit(1);
            }

            ret = av_frame_make_writable(rgbFrame);
            if (ret < 0) {
                exit(1);
            }

            rgbFrame.data().put(((DataBufferByte) image.getRaster().getDataBuffer()).getData());
            sws_scale(swsContext, rgbFrame.data(),rgbFrame.linesize(), 0, c.height(), frame.data(), frame.linesize());

            frame.pts(i);
            encode(c, frame, pkt, oc, st);
        }

        sws_freeContext(swsContext);


        encode(c, null, pkt, oc, st);
        av_write_trailer(oc);
        if (!oc.isNull() && (oc.flags() & AVFMT_NOFILE) == 0) {
            avio_closep(oc.pb());
        }
        avcodec_free_context(c);
        avformat_free_context(oc);
        av_frame_free(frame);
        av_frame_free(rgbFrame);
    }

    private avcodec.AVCodecContext initCodecContext(avcodec.AVCodec codec) {
        avcodec.AVCodecContext c = avcodec_alloc_context3(codec);
        avutil.AVRational timebase = new avutil.AVRational();
        timebase.num(1);
        timebase.den(25);
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
        return c;
    }

    private void encode(AVCodecContext c, AVFrame frame, AVPacket pkt, AVFormatContext fmt, AVStream st) {
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
            pkt.stream_index(st.index());
            av_packet_rescale_ts(pkt, c.time_base(), st.time_base());
            if (av_interleaved_write_frame(fmt, pkt) < 0) {
                System.err.println("Error write frame");
                exit(1);
            }
            av_packet_unref(pkt);
        }
    }
}
