import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avutil;

import static java.lang.System.exit;
import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.*;
import static org.bytedeco.javacpp.presets.avutil.AVERROR_EAGAIN;

public class H264EncodingSample {
    public static void main(String... argv) {
        new H264EncodingSample().start();
    }

    private void start() {
        avcodec.AVCodec codec = avcodec_find_encoder(AV_CODEC_ID_H264);
        avcodec.AVCodecContext c = initCodecContext(codec);

        avformat.AVFormatContext oc = new avformat.AVFormatContext(null);
        String filename = "output.mkv";
        avformat.avformat_alloc_output_context2(oc, null, null, filename);
        if (oc.isNull()) {
            exit(1);
        }

        avformat.AVStream st = avformat_new_stream(oc, codec);
        if (st.isNull()) {
            System.err.println("Could not allocate stream");
            exit(1);
        }



        /* open the output file, if needed */
        if ((oc.flags() & AVFMT_NOFILE) == 0) {
            AVIOContext avioContext = new AVIOContext(null);
            if (avio_open(avioContext, filename, AVIO_FLAG_WRITE) < 0) {
                System.err.printf("Could not open '%s'\n", filename);
                exit(1);
            }
            oc.pb(avioContext);
        }



        avcodec_parameters_from_context(st.codecpar(), c);
        av_dump_format(oc, 0, filename, 1);

        AVDictionary dict = new AVDictionary();
        /* Write the stream header, if any. */
        if (avformat_write_header(oc, dict) < 0) {
            System.err.println("Error occurred when opening output file\n");
            exit(1);
        }

        avutil.AVFrame frame = av_frame_alloc();
        frame.format(c.pix_fmt());
        frame.width(c.width());
        frame.height(c.height());

        int ret = av_frame_get_buffer(frame, 32);
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
            if (ret < 0)
                exit(1);

            /* prepare a dummy image */
            /* Y */
            for (int y = 0; y < c.height(); y++) {
                for (int x = 0; x < c.width(); x++) {
                    frame.data(0).put(y * frame.linesize(0) + x, (byte) (x + y + i * 3));
                }
            }

            /* Cb and Cr */
            for (int y = 0; y < c.height()/2; y++) {
                for (int x = 0; x < c.width()/2; x++) {
                    frame.data(1).put(y * frame.linesize(1) + x, (byte) (128 + y + i * 2));
                    frame.data(2).put(y * frame.linesize(2) + x, (byte) (64 + x + i * 5));
                }
            }
            frame.pts(i);

            encode(c, frame, pkt, oc, st);
        }

        encode(c, null, pkt, oc, st);


        av_write_trailer(oc);
        if (!oc.isNull() && (oc.flags() & AVFMT_NOFILE) == 0) {
            avio_closep(oc.pb());
        }
        avformat_free_context(oc);
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
            int sz = pkt.size();
            byte[] data = new byte[sz];
            pkt.data().get(data);

            pkt.stream_index(st.index());
            av_packet_rescale_ts(pkt, c.time_base(), st.time_base());
            if (av_interleaved_write_frame(fmt, pkt) != 0) {
                System.err.println("Error write frame");
                exit(1);
            }

            av_packet_unref(pkt);
        }
    }
}
