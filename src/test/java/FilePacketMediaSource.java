import me.vzhilin.mediaserver.media.FileMediaPacketSource;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class FilePacketMediaSource {
    @Test
    public void test() throws IOException {
        FileMediaPacketSource src = new FileMediaPacketSource(new File("/home/vzhilin/misc/video_samples/simpsons_video.mkv"));
        src.next();

        src.close();
    }
}
