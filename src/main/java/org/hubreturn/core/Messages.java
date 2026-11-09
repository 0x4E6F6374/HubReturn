package org.hubreturn.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public final class Messages {

    private final YamlConfig root;

    private Messages(YamlConfig root) {
        this.root = root;
    }

    public static Messages load(Path dataDir) throws IOException {
        return new Messages(YamlConfig.load(dataDir, "messages.yml"));
    }

    public String raw(String key) {
        return root.string(key);
    }

    public String text(String key) {
        return raw(key).replace("{prefix}", root.string("prefix"));
    }

    public String format(String key, Map<String, String> vars) {
        String out = text(key);
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            out = out.replace("{" + entry.getKey() + "}", value);
        }
        return out;
    }
}
