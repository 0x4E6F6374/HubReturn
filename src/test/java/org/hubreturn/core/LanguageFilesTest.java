package org.hubreturn.core;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LanguageFilesTest {

    @Test
    void everyLanguageHasTheSameKeys() {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Map<String, Object> config = load(yaml, "config.yml");
        Map<String, Object> language = section(config.get("language"));
        Map<String, Object> files = section(language.get("files"));
        String defaultFile = String.valueOf(files.get(String.valueOf(language.get("default"))));
        Set<String> expected = flatten(load(yaml, defaultFile), "");

        for (Map.Entry<String, Object> entry : files.entrySet()) {
            Set<String> actual = flatten(load(yaml, String.valueOf(entry.getValue())), "");
            assertEquals(expected, actual, "语言 " + entry.getKey() + " 的文案键与默认语言不一致");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> load(Yaml yaml, String resource) {
        try (InputStream in = LanguageFilesTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, resource);
            return (Map<String, Object>) yaml.load(in);
        } catch (IOException bad) {
            throw new IllegalStateException(bad);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> section(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> flatten(Map<String, Object> values, String prefix) {
        Set<String> out = new TreeSet<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map) {
                out.addAll(flatten((Map<String, Object>) entry.getValue(), path));
            } else {
                out.add(path);
            }
        }
        return out;
    }
}
