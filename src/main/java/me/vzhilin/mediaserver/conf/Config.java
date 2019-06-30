package me.vzhilin.mediaserver.conf;

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
}
