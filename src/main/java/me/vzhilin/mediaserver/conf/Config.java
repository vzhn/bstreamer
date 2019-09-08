package me.vzhilin.mediaserver.conf;

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
        PropertyMap syncProperties = properties.getMap("network").getMap("limits");
        int sizeLimit = syncProperties.getInt("size");
        int packetLimit = syncProperties.getInt("packets");
        int timeLimit = syncProperties.getInt("time");
        return new BufferingLimits(sizeLimit, packetLimit, timeLimit);
    }

    @Override
    public String toString() {
        return properties.toString();
    }
}
