package me.vzhilin.mediaserver.conf;

import io.netty.channel.WriteBufferWaterMark;
import me.vzhilin.mediaserver.media.file.MediaPaketSourceConfig;
import me.vzhilin.mediaserver.media.picture.H264CodecParameters;

import java.io.File;
import java.io.IOException;

public class Config {
    public static final String NETWORK_PORT                        = "network.port";
    public static final String NETWORK_SNDBUF                      = "network.sndbuf";
    public static final String NETWORK_WATERMARKS_LOW              = "network.watermarks.low";
    public static final String NETWORK_WATERMARKS_HIGH             = "network.watermarks.high";
    public static final String STRATEGY_SYNC_LIMITS_PACKETS        = "strategy.sync.limits.packets";
    public static final String STRATEGY_SYNC_LIMITS_SIZE           = "strategy.sync.limits.size";
    public static final String STRATEGY_SYNC_LIMITS_TIME           = "strategy.sync.limits.time";
    public static final String SOURCE_FILE_DIR                     = "source.file.dir";
    public static final String SOURCE_PICTURE_WIDTH                = "source.picture.width";
    public static final String SOURCE_PICTURE_HEIGHT               = "source.picture.height";
    public static final String SOURCE_PICTURE_ENCODER_BITRATE      = "source.picture.encoder.bitrate";
    public static final String SOURCE_PICTURE_ENCODER_FPS          = "source.picture.encoder.fps";
    public static final String SOURCE_PICTURE_ENCODER_GOP_SIZE     = "source.picture.encoder.gop_size";
    public static final String SOURCE_PICTURE_ENCODER_MAX_B_FRAMES = "source.picture.encoder.max_b_frames";
    private final PropertyMap properties;

    public Config(PropertyMap properties) throws IOException {
        this.properties = properties;
//        NetworkConfig networkConfig = new NetworkConfig(properties.getMap("network"));
//        PropertyMap sourcesMap = properties.getMap("source");
//        SourcesConfig sourcesConfig = new SourcesConfig(properties.getMap("source"));
//        StrategyConfig strategyConfig = new StrategyConfig(properties.getMap("strategy"));
//        mapping = Yaml.createYamlInput(is).readYamlMapping();
//        YamlMapping network = mapping.yamlMapping("network");
//        String port = network.string("port");
//        if (port != null) {
//            this.port = Integer.parseInt(port);
//        } else {
//            this.port = 5000;
//        }
//        String sndbuf = network.string("sndbuf");
//        if (sndbuf == null || "default".equals(sndbuf)) {
//            networkSndbuf = Optional.empty();
//        } else {
//            networkSndbuf = Optional.of(Integer.parseInt(sndbuf));
//        }
//        YamlMapping watermarks = network.yamlMapping("watermarks");
//        if (watermarks == null) {
//            networkWatermarks = WriteBufferWaterMark.DEFAULT;
//        } else {
//            int lowWatermark = Integer.parseInt(watermarks.string("low"));
//            int highWatermark = Integer.parseInt(watermarks.string("high"));
//            networkWatermarks = new WriteBufferWaterMark(lowWatermark, highWatermark);
//        }
//        YamlMapping strategies = mapping.yamlMapping("strategy");
//        YamlMapping syncStrategy = strategies.yamlMapping("sync");
//        YamlMapping syncStrategyLimits = syncStrategy.yamlMapping("limits");
//        String size = syncStrategyLimits.string("size");
//        String packets = syncStrategyLimits.string("packets");
//        String time = syncStrategyLimits.string("time");
//        this.syncStrategyLimits = new SyncStrategyLimits(
//            Integer.parseInt(size),
//            Integer.parseInt(packets),
//            Integer.parseInt(time)
//        );
//        YamlMapping sourceMapping = mapping.yamlMapping("source");
//        YamlMapping fileMapping = sourceMapping.yamlMapping("file");
//        filesDir = new File(fileMapping.string("dir"));
//        YamlMapping pictureMapping = sourceMapping.yamlMapping("picture");
//        YamlMapping encoderMapping = pictureMapping.yamlMapping("encoder");
//        h264Params = new H264CodecParameters();
//        h264Params.setBitrate(Integer.parseInt(encoderMapping.string("bitrate")));
//        h264Params.setFps(Integer.parseInt(encoderMapping.string("fps")));
//        h264Params.setGopSize(Integer.parseInt(encoderMapping.string("gop_size")));
//        h264Params.setMaxBFrames(Integer.parseInt(encoderMapping.string("max_b_frames")));
//        h264Params.setWidth(Integer.parseInt(pictureMapping.string("width")));
//        h264Params.setHeight(Integer.parseInt(pictureMapping.string("height")));
    }

    public int getPort() {
        return properties.getInt(NETWORK_PORT);
    }

    public Integer getNetworkSndbuf() {
        return properties.getInt(NETWORK_SNDBUF);
    }

    public WriteBufferWaterMark getNetworkWatermarks() {
        int lowWatermark = properties.getInt(NETWORK_WATERMARKS_LOW);
        int highWatermark = properties.getInt(NETWORK_WATERMARKS_HIGH);
        return new WriteBufferWaterMark(lowWatermark, highWatermark);
    }

    public SyncStrategyLimits getSyncStrategyLimits() {
        int packets = properties.getInt(STRATEGY_SYNC_LIMITS_PACKETS);
        int size = properties.getInt(STRATEGY_SYNC_LIMITS_SIZE);
        int time = properties.getInt(STRATEGY_SYNC_LIMITS_TIME);
        return new SyncStrategyLimits(packets, size, time);
    }

    public File getFilesDir() {
        return new File(properties.getValue(SOURCE_FILE_DIR));
    }

    public H264CodecParameters getH264Params() {
        int width = properties.getInt(SOURCE_PICTURE_WIDTH);
        int height = properties.getInt(SOURCE_PICTURE_HEIGHT);
        int bitrate = properties.getInt(SOURCE_PICTURE_ENCODER_BITRATE);
        int fps = properties.getInt(SOURCE_PICTURE_ENCODER_FPS);
        int gopSize = properties.getInt(SOURCE_PICTURE_ENCODER_GOP_SIZE);
        int maxBFrames = properties.getInt(SOURCE_PICTURE_ENCODER_MAX_B_FRAMES);
        H264CodecParameters parameters = new H264CodecParameters();
        parameters.setWidth(width);
        parameters.setHeight(height);
        parameters.setBitrate(bitrate);
        parameters.setFps(fps);
        parameters.setGopSize(gopSize);
        parameters.setMaxBFrames(maxBFrames);
        return parameters;
    }

    public MediaPaketSourceConfig getSourceConfig(String configName) {
        PropertyMap props = properties.getMap("source").getMap(configName);
        return new MediaPaketSourceConfig(new PropertyMap(props));
    }

    @Override
    public String toString() {
        return properties.toString();
    }
}
