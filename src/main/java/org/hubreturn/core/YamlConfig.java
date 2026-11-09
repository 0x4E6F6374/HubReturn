package org.hubreturn.core;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class YamlConfig {

    private final Map<String, Object> values;

    private YamlConfig(Map<String, Object> values) {
        this.values = values;
    }

    public static YamlConfig load(Path dataDir, String resource) throws IOException {
        // 模板先落地，再拿数据目录的文件按键覆盖，Java 代码里不留任何缺省值
        Files.createDirectories(dataDir);
        Path file = dataDir.resolve(resource);
        if (!Files.exists(file)) {
            try (InputStream in = open(resource)) {
                Files.copy(in, file);
            }
        }

        Map<String, Object> merged;
        try (InputStream in = open(resource)) {
            merged = read(in);
        }
        try (InputStream in = Files.newInputStream(file)) {
            merge(merged, read(in));
        }
        return new YamlConfig(merged);
    }

    private static InputStream open(String resource) throws IOException {
        InputStream in = YamlConfig.class.getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            throw new IOException("jar 里找不到 " + resource);
        }
        return in;
    }

    private static Map<String, Object> read(InputStream in) {
        Object loaded = new Yaml(new SafeConstructor(new LoaderOptions())).load(in);
        if (loaded instanceof Map) {
            return copy(loaded);
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> copy(Object raw) {
        return new LinkedHashMap<>((Map<String, Object>) raw);
    }

    @SuppressWarnings("unchecked")
    private static void merge(Map<String, Object> base, Map<String, Object> over) {
        for (Map.Entry<String, Object> entry : over.entrySet()) {
            Object old = base.get(entry.getKey());
            Object value = entry.getValue();
            if (old instanceof Map && value instanceof Map) {
                merge((Map<String, Object>) old, (Map<String, Object>) value);
            } else {
                base.put(entry.getKey(), value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Object req(String path) {
        Map<String, Object> cursor = values;
        String[] parts = path.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            Object value = cursor.get(parts[i]);
            if (value == null) {
                throw new ConfigProblem(path);
            }
            if (i == parts.length - 1) {
                return value;
            }
            if (!(value instanceof Map)) {
                throw new ConfigProblem(path);
            }
            cursor = (Map<String, Object>) value;
        }
        throw new ConfigProblem(path);
    }

    public Object opt(String path) {
        try {
            return req(path);
        } catch (ConfigProblem ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public YamlConfig section(String path) {
        Object value = req(path);
        if (!(value instanceof Map)) {
            throw new ConfigProblem(path);
        }
        return new YamlConfig((Map<String, Object>) value);
    }

    public String string(String path) {
        return String.valueOf(req(path));
    }

    public int integer(String path) {
        Object value = req(path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(String.valueOf(value).trim());
    }

    public long longValue(String path) {
        Object value = req(path);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(String.valueOf(value).trim());
    }

    public boolean bool(String path) {
        return Boolean.parseBoolean(String.valueOf(req(path)).trim());
    }

    public List<String> strings(String path) {
        List<String> out = new ArrayList<>();
        for (Object item : list(path)) {
            out.add(String.valueOf(item));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> maps(String path) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list(path)) {
            if (!(item instanceof Map)) {
                throw new ConfigProblem(path);
            }
            out.add((Map<String, Object>) item);
        }
        return out;
    }

    private List<?> list(String path) {
        Object value = req(path);
        if (!(value instanceof List)) {
            throw new ConfigProblem(path);
        }
        return (List<?>) value;
    }

    public static final class ConfigProblem extends RuntimeException {
        public ConfigProblem(String path) {
            super("配置项缺失或类型不对: " + path);
        }
    }
}
