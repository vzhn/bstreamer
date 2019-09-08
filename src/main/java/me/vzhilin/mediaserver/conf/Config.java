package me.vzhilin.mediaserver.conf;

import me.vzhilin.mediaserver.server.strategy.sync.SyncStrategyAttributes;
import me.vzhilin.mediaserver.util.scheduler.BufferingLimits;

public class Config {
    private final PropertyMap properties;

    public Config(PropertyMap properties) {
        this.properties = properties;
    }

    public PropertyMap getNetwork() {
        return properties.getMap("network");
    }

    public PropertyMap getStreamingConfig(String configName) {
        PropertyMap props = properties.getMap("streaming").getMap(configName);
        return new PropertyMap(props);
    }

    public BufferingLimits getBufferingLimits() {
        PropertyMap syncProperties = properties.getMap("strategy").getMap("sync");
        int sizeLimit = syncProperties.getInt(SyncStrategyAttributes.LIMIT_SIZE);
        int packetLimit = syncProperties.getInt(SyncStrategyAttributes.LIMIT_PACKETS);
        int timeLimit = syncProperties.getInt(SyncStrategyAttributes.LIMIT_TIME);
        return new BufferingLimits(sizeLimit, packetLimit, timeLimit);
    }

    @Override
    public String toString() {
        return properties.toString();
    }
}
