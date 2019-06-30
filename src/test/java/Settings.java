import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import me.vzhilin.mediaserver.conf.PropertyMap;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;

public class Settings {
    @Test
    public void test() throws IOException {
        String path = "/home/vzhilin/IdeaProjects/mediaserver/src/main/resources/settings.yaml";

        PropertyMap pm = PropertyMap.parseYaml(new FileInputStream(path));
//
//
//        YamlMapping pictureNode = mapping.yamlMapping("source").yamlMapping("picture");
//
//        YamlMapping wrapped = Yaml.createYamlMappingBuilder().add("_wrapped", pictureNode).build();
//        System.err.println(wrapped.yamlMapping("_wrapped"));

        PropertyMap mp = new PropertyMap();
        mp.put("network.port", "5000");
        mp.put("network.sndbuf", "default");
        mp.put("network.watermarks.high", "524288");
        mp.put("network.watermarks.low", "524288");
        mp.put("source.file.dir", "/home/vzhilin/misc/video_samples/");
        mp.put("source.picture.width", "640");
        mp.put("source.picture.height", "480");
        mp.put("source.picture.encoder.bitrate", "400000");
        mp.put("source.picture.encoder.fps", "25");
        mp.put("source.picture.encoder.gop_size", "10");
        mp.put("source.picture.encoder.max_b_frames", "1");

        /**
         *   picture:
         *     width: 640
         *     height: 480
         *     encoder:
         *       bitrate: 400000
         *       fps: 25
         *       gop_size: 10
         *       max_b_frames: 1
         * strategy:
         */

//
//                picture:
//        width: 640
//        height: 480
//        encoder:
//        bitrate: 400000
//        fps: 25
//        gop_size: 10
//        max_b_frames: 1
//        strategy:
//        sync:
//        limits:
//        packets: 20
//        size: 256000
//        time: 200
//        mp.put("a.b.c.d", "123");

        System.err.println(mp);

    }
}
