package me.vzhilin.mediaserver.conf;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public final class PropertyMap {
    private Map<String, Object> properties = new LinkedHashMap<>();

    /**
     * ctor
     */
    public PropertyMap() {
    }

    /**
     * Copy ctor
     * @param map
     */
    public PropertyMap(PropertyMap map) {
        for (Map.Entry<String, Object> e: map.properties.entrySet()) {
            String label = e.getKey();
            Object v = e.getValue();
            if (v instanceof PropertyMap) {
                this.properties.put(label, new PropertyMap((PropertyMap) v));
            } else {
                this.properties.put(label, v);
            }
        }
    }

    public Collection<Map.Entry<String, Object>> entries() {
        return properties.entrySet();
    }

    public void put(String key, String value) {
        String[] parts = key.split("\\.");
        PropertyMap mp = this;
        if (parts.length > 1) {
            for (int i = 0; i < parts.length - 1; i++) {
                String k = parts[i];
                if (!mp.hasKey(k)) {
                    mp.putPropertyMap(k, new PropertyMap());
                }
                mp = (PropertyMap) mp.properties.get(k);
            }
            mp.properties.put(parts[parts.length-1], value);
        } else {
            mp.properties.put(parts[0], value);
        }
    }

    private boolean hasKey(String key) {
        return properties.containsKey(key);
    }

    public String getValue(String key) {
        String[] parts = key.split("\\.");
        PropertyMap mp = this;
        if (parts.length > 1) {
            for (int i = 0; i < parts.length - 1; i++) {
                String k = parts[i];
                if (!mp.hasKey(k)) {
                    return null;
                }
                mp = (PropertyMap) mp.properties.get(k);
            }
            return (String) mp.properties.get(parts[parts.length-1]);
        } else {
            return (String) mp.properties.get(parts[0]);
        }
    }

    public PropertyMap getMap(String key) {
        String[] parts = key.split("\\.");
        PropertyMap mp = this;
        if (parts.length > 1) {
            for (int i = 0; i < parts.length - 1; i++) {
                String k = parts[i];
                if (!mp.hasKey(k)) {
                    return null;
                }
                mp = (PropertyMap) properties.get(k);
            }
            return (PropertyMap) properties.get(parts[parts.length-1]);
        } else {
            return (PropertyMap) properties.get(parts[0]);
        }
    }

    public String getValue(String key, String defaultValue) {
        String v = getValue(key);
        if (v == null) {
            return defaultValue;
        } else {
            return v;
        }
    }

    private void putPropertyMap(String k, PropertyMap propertyMap) {
        properties.put(k, propertyMap);
    }

    public static PropertyMap parseYaml(InputStream is) throws IOException {
        YAMLFactory factory = new YAMLFactory();
        YAMLParser parser = factory.createParser(is);
        Deque<PropertyMap> stack = new LinkedList<>();
        String label = "";
        JsonToken token;
        while ((token = parser.nextToken()) != null) {
            switch (token) {
                case START_OBJECT: {
                    PropertyMap nested = new PropertyMap();
                    if (!stack.isEmpty()) {
                        PropertyMap first = stack.getFirst();
                        first.putPropertyMap(label, nested);
                    }
                    stack.push(nested);
                    break;
                }
                case END_OBJECT: {
                    PropertyMap obj = stack.pop();
                    if (stack.isEmpty()) {
                        return obj;
                    }
                    break;
                }
                case FIELD_NAME:
                    label = parser.getText();
                    break;
                case VALUE_STRING:
                case VALUE_NUMBER_INT:
                case VALUE_NUMBER_FLOAT:
                case VALUE_TRUE:
                case VALUE_FALSE:
                    stack.getFirst().put(label, parser.getText());
                    break;
                case NOT_AVAILABLE:
                case VALUE_EMBEDDED_OBJECT:
                case START_ARRAY:
                case END_ARRAY:
                case VALUE_NULL:
                    break;
            }
        }

        return stack.pop();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Deque<Entry> stack = new LinkedList<Entry>();
        stack.push(new Entry("",this, -1));

        while (!stack.isEmpty()) {
            Entry e = stack.pop();
            if (!e.label.isEmpty()) {
                for (int i = 0; i < e.depth; i++) {
                    sb.append("  ");
                }
                sb.append(e.label + ":\n");
            }
            for (Map.Entry<String, Object> entry: e.map.properties.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof PropertyMap) {
                    stack.push(new Entry(entry.getKey(), (PropertyMap) value, e.depth + 1));
                } else {
                    for (int i = 0; i < e.depth + 1; i++) {
                        sb.append("  ");
                    }
                    sb.append(entry.getKey()).append(": ").append(value).append("\n");
                }
            }
        }
        return sb.toString();
    }

    public int getInt(String key) {
        return Integer.parseInt(getValue(key));
    }

    public void putAll(Map<String, String> params) {
        params.forEach(this::put);
    }

    private final static class Entry {
        private final PropertyMap map;
        private final int depth;
        private final String label;
        private Entry(String label, PropertyMap map, int depth) {
            this.label = label;
            this.map = map;
            this.depth = depth;
        }
    }
}
