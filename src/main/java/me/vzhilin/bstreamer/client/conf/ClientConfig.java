package me.vzhilin.bstreamer.client.conf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientConfig {
    private final static Logger LOG = LogManager.getLogger(ClientConfig.class);

    private NetworkOptions network;
    private final List<ConnectionSettings> connections = new ArrayList<>();
    public ClientConfig() { }

    public List<ConnectionSettings> getConnections() {
        return connections;
    }

    public NetworkOptions getNetwork() {
        return network;
    }

    public static ClientConfig read(File file) throws IOException {
        ClientConfig clientConfig = new ObjectMapper(new YAMLFactory()).readValue(file, ClientConfig.class);
        LOG.info("config loaded: " + file.getAbsolutePath());
        return clientConfig;
    }
}
