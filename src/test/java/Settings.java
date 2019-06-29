import me.vzhilin.mediaserver.conf.Config;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;

public class Settings {
    @Test
    public void test() throws IOException {
        String path = "/home/vzhilin/IdeaProjects/mediaserver/src/main/resources/settings.yaml";
        Config conf = new Config(new FileInputStream(path));
        System.err.println(conf);


    }
}
