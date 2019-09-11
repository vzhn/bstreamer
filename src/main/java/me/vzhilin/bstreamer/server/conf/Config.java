package me.vzhilin.bstreamer.server.conf;

import me.vzhilin.bstreamer.server.scheduler.BufferingLimits;
import me.vzhilin.bstreamer.util.PropertyMap;

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
        int sizeLimit = syncProperties.getInt("size", 0);
        int packetLimit = syncProperties.getInt("packets", 0);
        int timeLimit = syncProperties.getInt("time", 0);
        return new BufferingLimits(sizeLimit, packetLimit, timeLimit);
    }

    @Override
    public String toString() {
        return properties.toString();
    }
}
