package me.vzhilin.bstreamer.util;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public final class PropertyMap {
    private Map<String, Object> properties = new LinkedHashMap<>();

    /**
     * ctor
     */
    public PropertyMap() { }

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


    public void put(String key, Object value) {
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

    public Object getObject(String key) {
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
            return mp.properties.get(parts[parts.length-1]);
        } else {
            return mp.properties.get(parts[0]);
        }
    }

    public String getString(String key) {
        return (String) getObject(key);
    }

    public Boolean getBoolean(String key) {
        return Boolean.valueOf(getString(key, "false"));
    }

    public List<String> getStringArray(String key) {
        return (List<String>) getObject(key);
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

    public String getString(String key, String defaultValue) {
        String v = getString(key);
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
        JsonParser parser = factory.createParser(is);
        Deque<Object> stack = new LinkedList<>();
        String label = "";
        JsonToken token;
        while ((token = parser.nextToken()) != null) {
            Object top = stack.peekFirst();
            switch (token) {
                case START_OBJECT: {
                    PropertyMap nested = new PropertyMap();
                    if (!stack.isEmpty()) {
                        if (top instanceof PropertyMap) {
                            PropertyMap first = (PropertyMap) top;
                            first.putPropertyMap(label, nested);
                        } else {
                            ((List) top).add(nested);
                        }
                    }
                    stack.push(nested);
                    break;
                }
                case END_OBJECT: {
                    Object obj = stack.pop();
                    if (stack.isEmpty()) {
                        return (PropertyMap) obj;
                    }
                    break;
                }
                case PROPERTY_NAME:
                    label = parser.getText();
                    break;
                case VALUE_STRING:
                case VALUE_NUMBER_INT:
                case VALUE_NUMBER_FLOAT:
                case VALUE_TRUE:
                case VALUE_FALSE:
                    if (top instanceof PropertyMap) {
                        PropertyMap first = (PropertyMap) top;
                        first.put(label, parser.getText());
                    } else {
                        ((List<Object>) top).add(parser.getText());
                    }
                    break;
                case START_ARRAY:
                    ArrayList<Object> array = new ArrayList<>();
                    if (top instanceof PropertyMap) {
                        PropertyMap first = (PropertyMap) top;
                        first.put(label, array);
                    } else {
                        ((List<Object>) top).add(array);
                    }
                    stack.push(array);
                    break;
                case END_ARRAY:
                    stack.pop();
                    break;
                case NOT_AVAILABLE:
                case VALUE_EMBEDDED_OBJECT:
                case VALUE_NULL:
                    break;
            }
        }
        return (PropertyMap) stack.pop();
    }

    public int getInt(String key, int defaultValue) {
        return hasKey(key) ? getInt(key) : defaultValue;
    }
    public int getInt(String key) {
        return Integer.parseInt(getString(key));
    }

    public void putAll(Map<String, String> params) {
        params.forEach(this::put);
    }

    public List<PropertyMap> getArray(String name) {
        return (List<PropertyMap>) getObject(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyMap that = (PropertyMap) o;
        return properties.equals(that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(properties);
    }
}