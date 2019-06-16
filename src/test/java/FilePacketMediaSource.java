import me.vzhilin.mediaserver.media.FileMediaPacketSource;
import org.bridj.Pointer;
import org.ffmpeg.avutil.AVRational;
import org.ffmpeg.avutil.AvutilLibrary;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class FilePacketMediaSource {
    @Test
    public void test() throws IOException {
        FileMediaPacketSource src = new FileMediaPacketSource(new File("/home/vzhilin/misc/video_samples/simpsons_video.mkv"));
        while (src.hasNext()) {
            Pointer<AVRational> r1 = Pointer.allocate(AVRational.class);
            Pointer<AVRational> r2 = Pointer.allocate(AVRational.class);


            AVRational s1 = r1.get();
            AVRational s2 = r2.get();
            s1.den(1);
            s1.num(1);

            s2.den(1);
            s2.num(1);
            AvutilLibrary.av_rescale_q(123, s1, s2);
            System.err.println(src.next());
        }
        src.close();
    }
}
