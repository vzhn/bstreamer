import me.vzhilin.mediaserver.media.MediaPacket;
import me.vzhilin.mediaserver.media.MediaStream;
import org.junit.Test;

import java.io.IOException;

import static org.ffmpeg.avcodec.AvcodecLibrary.avcodec_register_all;

public class MediaStreamTest {
    @Test
    public void test()  {
        avcodec_register_all();

        MediaStream ms = new MediaStream();
        MediaPacket mp;
//        while ((mp = ms.next()) != null) {
//            System.err.println(mp);
//        }

        ms.close();
    }
}
