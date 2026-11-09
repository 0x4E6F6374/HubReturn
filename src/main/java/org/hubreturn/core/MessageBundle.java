package org.hubreturn.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class MessageBundle {

    private final Messages fallback;
    private final Map<String, Messages> byCode;

    private MessageBundle(Messages fallback, Map<String, Messages> byCode) {
        this.fallback = fallback;
        this.byCode = byCode;
    }

    public static MessageBundle load(Path dataDir, String defaultCode, Map<String, String> files) throws IOException {
        String defaultFile = files.get(defaultCode);
        if (defaultFile == null) {
            throw new YamlConfig.ConfigProblem("language.default");
        }
        YamlConfig fallbackRoot = YamlConfig.load(dataDir, defaultFile);
        Messages defaultMessages = Messages.of(fallbackRoot, fallbackRoot);
        Map<String, Messages> loaded = new LinkedHashMap<>();
        String normalizedDefault = defaultCode.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : files.entrySet()) {
            String code = entry.getKey().toLowerCase(Locale.ROOT);
            if (code.equals(normalizedDefault)) {
                loaded.put(code, defaultMessages);
                continue;
            }
            YamlConfig own = YamlConfig.load(dataDir, entry.getValue());
            loaded.put(code, Messages.of(own, fallbackRoot));
        }
        return new MessageBundle(defaultMessages, loaded);
    }

    static MessageBundle of(Messages fallback, Map<String, Messages> byCode) {
        return new MessageBundle(fallback, byCode);
    }

    public Messages defaultMessages() {
        return fallback;
    }

    public Messages forLocale(Locale locale) {
        if (locale == null) {
            return fallback;
        }
        Messages exact = byCode.get(locale.toString().toLowerCase(Locale.ROOT));
        if (exact != null) {
            return exact;
        }
        Messages byLanguage = byCode.get(locale.getLanguage().toLowerCase(Locale.ROOT));
        return byLanguage != null ? byLanguage : fallback;
    }
}
