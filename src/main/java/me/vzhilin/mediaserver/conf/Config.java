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

    public PropertyMap getStrategyConfig(String strategyName) {
        return new PropertyMap(properties.getMap("strategy").getMap(strategyName));
    }

    public PropertyMap getSourceConfig(String configName) {
        PropertyMap props = properties.getMap("source").getMap(configName);
        return new PropertyMap(props);
    }

    @Override
    public String toString() {
        return properties.toString();
    }

    public BufferingLimits getBufferingLimits() {
        PropertyMap syncProperties = properties.getMap("strategy").getMap("sync");
        int sizeLimit = syncProperties.getInt(SyncStrategyAttributes.LIMIT_SIZE);
        int packetLimit = syncProperties.getInt(SyncStrategyAttributes.LIMIT_PACKETS);
        int timeLimit = syncProperties.getInt(SyncStrategyAttributes.LIMIT_TIME);
        return new BufferingLimits(sizeLimit, packetLimit, timeLimit);
    }
}
