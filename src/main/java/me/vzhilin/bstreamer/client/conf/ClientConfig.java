package me.vzhilin.bstreamer.client.conf;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientConfig {
    private final static Logger LOG = LogManager.getLogger(ClientConfig.class);

    private NetworkOptions network;
    private List<ConnectionSettings> connections = new ArrayList<>();

    public ClientConfig() { }

    public List<ConnectionSettings> getConnections() {
        return connections;
    }

    public void setConnections(List<ConnectionSettings> connections) {
        this.connections = connections;
    }

    public NetworkOptions getNetwork() {
        return network;
    }

    public void setNetwork(NetworkOptions network) {
        this.network = network;
    }

    public static ClientConfig read(File file) throws IOException {
        ClientConfig clientConfig = new ObjectMapper(new YAMLFactory()).readValue(file, ClientConfig.class);
        LOG.info("config loaded: {}", file.getAbsolutePath());
        return clientConfig;
    }
}