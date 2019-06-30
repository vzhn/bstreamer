package me.vzhilin.mediaserver.conf;

import io.netty.channel.WriteBufferWaterMark;

import java.io.IOException;

public class Config {
    public static final String STRATEGY_SYNC_LIMITS_PACKETS        = "strategy.sync.limits.packets";
    public static final String STRATEGY_SYNC_LIMITS_SIZE           = "strategy.sync.limits.size";
    public static final String STRATEGY_SYNC_LIMITS_TIME           = "strategy.sync.limits.time";
    private final PropertyMap properties;

    public Config(PropertyMap properties) throws IOException {
        this.properties = properties;
    }

    public SyncStrategyLimits getSyncStrategyLimits() {
        int packets = properties.getInt(STRATEGY_SYNC_LIMITS_PACKETS);
        int size = properties.getInt(STRATEGY_SYNC_LIMITS_SIZE);
        int time = properties.getInt(STRATEGY_SYNC_LIMITS_TIME);
        return new SyncStrategyLimits(packets, size, time);
    }

    public PropertyMap getNetwork() {
        return properties.getMap("network");
    }

    public PropertyMap getSourceConfig(String configName) {
        PropertyMap props = properties.getMap("source").getMap(configName);
        return new PropertyMap(props);
    }

    @Override
    public String toString() {
        return properties.toString();
    }
}
