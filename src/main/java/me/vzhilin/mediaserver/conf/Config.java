package me.vzhilin.mediaserver.conf;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import io.netty.channel.WriteBufferWaterMark;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class Config {
    private final YamlMapping mapping;
    private int port;
    private final Optional<Integer> networkSndbuf; //so_sndbuf
    private final WriteBufferWaterMark networkWatermarks;
    private final SyncStrategyLimits syncStrategyLimits;

    public Config(InputStream is) throws IOException {
        mapping = Yaml.createYamlInput(is).readYamlMapping();
        YamlMapping network = mapping.yamlMapping("network");
        String port = network.string("port");
        if (port == null) {
            this.port = Integer.parseInt(port);
        }
        String sndbuf = network.string("sndbuf");
        if (sndbuf == null || "default".equals(sndbuf)) {
            networkSndbuf = Optional.empty();
        } else {
            networkSndbuf = Optional.of(Integer.parseInt(sndbuf));
        }
        YamlMapping watermarks = network.yamlMapping("watermarks");
        if (watermarks == null) {
            networkWatermarks = WriteBufferWaterMark.DEFAULT;
        } else {
            int lowWatermark = Integer.parseInt(watermarks.string("low"));
            int highWatermark = Integer.parseInt(watermarks.string("high"));
            networkWatermarks = new WriteBufferWaterMark(lowWatermark, highWatermark);
        }
        YamlMapping strategies = mapping.yamlMapping("strategy");
        YamlMapping syncStrategy = strategies.yamlMapping("sync");
        YamlMapping syncStrategyLimits = syncStrategy.yamlMapping("limits");
        String size = syncStrategyLimits.string("size");
        String packets = syncStrategyLimits.string("packets");
        String time = syncStrategyLimits.string("time");
        this.syncStrategyLimits = new SyncStrategyLimits(
            Integer.parseInt(size),
            Integer.parseInt(packets),
            Integer.parseInt(time)
        );
    }

    public int getPort() {
        return port;
    }

    public Optional<Integer> getNetworkSndbuf() {
        return networkSndbuf;
    }

    public WriteBufferWaterMark getNetworkWatermarks() {
        return networkWatermarks;
    }

    public SyncStrategyLimits getSyncStrategyLimits() {
        return syncStrategyLimits;
    }

    @Override
    public String toString() {
        return mapping.toString();
    }
}
