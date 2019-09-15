package me.vzhilin.bstreamer.util;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConfigLocator {
    private final static Logger LOG = Logger.getLogger(ConfigLocator.class);
    private final List<File> defaults = new ArrayList<>();

    public ConfigLocator(String configFile) {
        this.defaults.add(new File(configFile));
        this.defaults.add(new File("src/conf", configFile));
        this.defaults.add(new File("conf", configFile));
        this.defaults.add(new File(new File(AppRuntime.APP_PATH, "conf"), configFile));
    }

    public Optional<File> locate(String configPath) {
        if (configPath != null) {
            File file = new File(configPath);
            if (LOG.isDebugEnabled()) {
                LOG.debug("probe: " + file);
            }
            if (file.exists()) {
                return Optional.of(file);
            }
        }
        for (File file : defaults) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("probe: " + file);
            }
            if (file.exists()) {
                return Optional.of(file);
            }
        }
        return Optional.empty();
    }
}
