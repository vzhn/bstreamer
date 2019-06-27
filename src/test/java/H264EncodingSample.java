import static java.lang.System.exit;
import static org.bytedeco.javacpp.avcodec.AV_CODEC_FLAG_GLOBAL_HEADER;
import static org.bytedeco.javacpp.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.javacpp.avcodec.av_packet_alloc;
import static org.bytedeco.javacpp.avcodec.av_packet_unref;
import static org.bytedeco.javacpp.avcodec.avcodec_alloc_context3;
import static org.bytedeco.javacpp.avcodec.avcodec_find_encoder;
import static org.bytedeco.javacpp.avcodec.avcodec_open2;
import static org.bytedeco.javacpp.avcodec.avcodec_parameters_from_context;
import static org.bytedeco.javacpp.avcodec.avcodec_parameters_to_context;
import static org.bytedeco.javacpp.avcodec.avcodec_receive_packet;
import static org.bytedeco.javacpp.avcodec.avcodec_send_frame;
import static org.bytedeco.javacpp.avutil.AVERROR_EOF;
import static org.bytedeco.javacpp.avutil.av_frame_alloc;
import static org.bytedeco.javacpp.avutil.av_frame_get_buffer;
import static org.bytedeco.javacpp.avutil.av_frame_make_writable;
import static org.bytedeco.javacpp.presets.avutil.AVERROR_EAGAIN;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avutil;

public class H264EncodingSample {
    public static void main(String... argv) {
        new H264EncodingSample().start();
    }

    private void start() {
        avcodec.AVCodec codec = avcodec_find_encoder(AV_CODEC_ID_H264);
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
//                    frame->data[0][y * frame->linesize[0] + x] = x + y + i * 3;
                }
            }

            /* Cb and Cr */
            for (int y = 0; y < c.height()/2; y++) {
                for (int x = 0; x < c.width()/2; x++) {
//                    frame->data[1][y * frame->linesize[1] + x] = 128 + y + i * 2;
//                    frame->data[2][y * frame->linesize[2] + x] = 64 + x + i * 5;
                }
            }
            frame.pts(i);

            encode(c, frame, pkt);
        }

        encode(c, null, pkt);

        System.err.println(c.extradata_size());
    }

    private void encode(avcodec.AVCodecContext c, avutil.AVFrame frame, avcodec.AVPacket pkt) {
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

            av_packet_unref(pkt);
        }
    }
}
