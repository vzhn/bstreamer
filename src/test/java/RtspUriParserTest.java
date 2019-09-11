import me.vzhilin.bstreamer.server.RtspUriParser;
import org.junit.Test;

import java.text.ParseException;

public class RtspUriParserTest {
    @Test
    public void test() throws ParseException {
        RtspUriParser parser = new RtspUriParser("rtsp://localhost:5000/simpsons_video.mkv/TrackID=0?mode=sync&repeat=true");

        System.err.println(parser);
    }
}
