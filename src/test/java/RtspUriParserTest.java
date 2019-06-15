import me.vzhilin.mediaserver.util.RtspUriParser;
import org.junit.Test;

import java.text.ParseException;

public class RtspUriParserTest {
    @Test
    public void test() throws ParseException {
        RtspUriParser parser = new RtspUriParser("rtsp://localhost:5000/simpsons_video.mkv/TrackID=0");

        System.err.println(parser);
    }
}
