package org.hubreturn.core;

import java.util.Map;

public final class Messages {

    private final YamlConfig root;
    private final YamlConfig fallback;

    private Messages(YamlConfig root, YamlConfig fallback) {
        this.root = root;
        this.fallback = fallback;
    }

    static Messages of(YamlConfig root, YamlConfig fallback) {
        return new Messages(root, fallback);
    }

    public String raw(String key) {
        return lookup(key);
    }

    public String text(String key) {
        return raw(key).replace("{prefix}", lookup("prefix"));
    }

    public String format(String key, Map<String, String> vars) {
        String out = text(key);
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            out = out.replace("{" + entry.getKey() + "}", value);
        }
        return out;
    }

    private String lookup(String key) {
        Object own = root.opt(key);
        return String.valueOf(own != null ? own : fallback.req(key));
    }
}
