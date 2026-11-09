package org.hubreturn.core;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class MessageBundleTest {

    @Test
    void matchesLanguageThenFallsBackToDefault() {
        Messages fallback = Messages.of(config("[en] ", "EN"), config("[en] ", "EN"));
        Messages chinese = Messages.of(config("[zh] ", "ZH"), config("[zh] ", "ZH"));
        MessageBundle bundle = MessageBundle.of(fallback, Map.of("en", fallback, "zh", chinese));

        assertSame(chinese, bundle.forLocale(Locale.SIMPLIFIED_CHINESE));
        assertSame(fallback, bundle.forLocale(Locale.forLanguageTag("fr")));
        assertSame(fallback, bundle.forLocale(null));
        assertSame(fallback, bundle.defaultMessages());
    }

    @Test
    void missingKeyFallsBackToDefault() {
        YamlConfig fallbackRoot = config("[en] ", "{prefix}EN");
        Map<String, Object> partial = new LinkedHashMap<>();
        partial.put("prefix", "[zh] ");
        Messages texts = Messages.of(YamlConfig.of(partial), fallbackRoot);

        assertEquals("[zh] EN", texts.text("command.reloaded"));
    }

    private static YamlConfig config(String prefix, String reloaded) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("prefix", prefix);
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("reloaded", reloaded);
        values.put("command", command);
        return YamlConfig.of(values);
    }
}
