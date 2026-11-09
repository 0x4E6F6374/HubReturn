package org.hubreturn.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Settings {

    private final boolean debug;
    private final YamlConfig language;
    private final YamlConfig command;
    private final YamlConfig lobbies;
    private final YamlConfig kick;
    private final YamlConfig join;
    private final YamlConfig messaging;
    private final YamlConfig confirm;
    private final YamlConfig applyTo;
    private final YamlConfig subcommand;
    private final YamlConfig chat;
    private final YamlConfig click;
    private final YamlConfig result;

    private Settings(YamlConfig root) {
        this.debug = root.bool("debug");
        this.language = root.section("language");
        this.command = root.section("command");
        this.lobbies = root.section("lobbies");
        this.kick = root.section("kick");
        this.join = root.section("join");
        this.messaging = root.section("messaging");
        this.confirm = root.section("confirm");
        this.applyTo = confirm.section("apply-to");
        this.subcommand = confirm.section("subcommand");
        this.chat = confirm.section("chat");
        this.click = confirm.section("click");
        this.result = confirm.section("result");
    }

    public static Settings load(Path dataDir) throws IOException {
        return new Settings(YamlConfig.load(dataDir, "config.yml"));
    }

    public void validate() {
        String mode = confirmMode();
        if (!oneOf(mode, "none", "repeat-command", "subcommand", "chat", "click")) {
            throw new YamlConfig.ConfigProblem("confirm.mode");
        }
        if (!oneOf(confirmOnTimeout(), "cancel", "execute")) {
            throw new YamlConfig.ConfigProblem("confirm.on-timeout");
        }
        if (!oneOf(resultMode(), "callback", "event", "poll")) {
            throw new YamlConfig.ConfigProblem("confirm.result.mode");
        }
        if (confirmSubcommand().isEmpty() || confirmCancelSubcommand().isEmpty()) {
            throw new YamlConfig.ConfigProblem("confirm.subcommand");
        }
        if (mode.equals("chat") && chatKeyword().isEmpty()) {
            throw new YamlConfig.ConfigProblem("confirm.chat.keyword");
        }
        if (mode.equals("click") && (clickPlaceholder().isEmpty()
                || clickLabel().isEmpty() || clickCommandTemplate().isEmpty())) {
            throw new YamlConfig.ConfigProblem("confirm.click");
        }
        Map<String, String> files = languageFiles();
        if (languageDefault().isEmpty() || !files.containsKey(languageDefault())) {
            throw new YamlConfig.ConfigProblem("language.default");
        }
        if (files.isEmpty()) {
            throw new YamlConfig.ConfigProblem("language.files");
        }
        for (Map.Entry<String, String> entry : files.entrySet()) {
            if (entry.getKey().isEmpty() || entry.getValue().isEmpty()) {
                throw new YamlConfig.ConfigProblem("language.files");
            }
        }
        nonNegative("confirm.timeout-millis", confirmTimeoutMillis());
        nonNegative("confirm.result.wait-millis", resultWaitMillis());
        nonNegative("confirm.result.poll-delay-millis", resultPollDelayMillis());
    }

    public String languageDefault() {
        return language.string("default");
    }

    public Map<String, String> languageFiles() {
        return language.stringMap("files");
    }

    public boolean debug() {
        return debug;
    }

    public String commandName() {
        return command.string("name");
    }

    public List<String> commandAliases() {
        return command.strings("aliases");
    }

    public String commandPermission() {
        return command.string("permission");
    }

    public String bypassCooldownPermission() {
        return command.string("bypass-cooldown-permission");
    }

    public long commandCooldownMillis() {
        return command.longValue("cooldown-millis");
    }

    public boolean alreadyInLobby() {
        return command.bool("already-in-lobby");
    }

    public String reloadSubcommand() {
        return command.string("reload-subcommand");
    }

    public String reloadPermission() {
        return command.string("reload-permission");
    }

    public String lobbyStrategy() {
        return lobbies.string("strategy");
    }

    public List<LobbyEntry> lobbyEntries() {
        List<LobbyEntry> out = new ArrayList<>();
        for (Map<String, Object> item : lobbies.maps("servers")) {
            Object name = item.get("name");
            if (name == null) {
                throw new YamlConfig.ConfigProblem("lobbies.servers[].name");
            }
            Object weight = item.get("weight");
            int value = weight instanceof Number ? ((Number) weight).intValue() : 1;
            out.add(new LobbyEntry(String.valueOf(name), Math.max(value, 1)));
        }
        return out;
    }

    public List<String> lobbyNames() {
        List<String> out = new ArrayList<>();
        for (LobbyEntry entry : lobbyEntries()) {
            out.add(entry.name());
        }
        return out;
    }

    public boolean skipCurrent() {
        return lobbies.bool("skip-current");
    }

    public int retryCount() {
        return lobbies.integer("retry-count");
    }

    public long retryDelayMillis() {
        return lobbies.longValue("retry-delay-millis");
    }

    public boolean kickEnabled() {
        return kick.bool("enabled");
    }

    public String kickMode() {
        return kick.string("mode");
    }

    public List<String> kickServers() {
        return kick.strings("servers");
    }

    public List<String> kickReasonContains() {
        return kick.strings("reason-contains");
    }

    public String kickBypassPermission() {
        return kick.string("bypass-permission");
    }

    public boolean kickShowCustomReason() {
        return kick.bool("show-custom-reason");
    }

    public boolean kickServerAllowed(String server) {
        String mode = kickMode();
        if (mode.equals("blacklist")) {
            return !contains(kickServers(), server);
        }
        if (mode.equals("whitelist")) {
            return contains(kickServers(), server);
        }
        return true;
    }

    public boolean kickReasonAllowed(String reason) {
        List<String> needles = kickReasonContains();
        if (needles.isEmpty()) {
            return true;
        }
        String lower = reason.toLowerCase(Locale.ROOT);
        for (String needle : needles) {
            if (lower.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public boolean joinEnabled() {
        return join.bool("enabled");
    }

    public String joinBypassPermission() {
        return join.string("bypass-permission");
    }

    public boolean messagingEnabled() {
        return messaging.bool("enabled");
    }

    public String channel() {
        return messaging.string("channel");
    }

    public String requestAction() {
        return messaging.string("request-action");
    }

    public String replyTag() {
        return messaging.string("reply-tag");
    }

    public boolean replyEnabled() {
        return messaging.bool("reply-enabled");
    }

    public List<String> allowedSources() {
        return messaging.strings("allowed-source-servers");
    }

    public boolean allowPlayerSource() {
        return messaging.bool("allow-player-source");
    }

    public long messageCooldownMillis() {
        return messaging.longValue("cooldown-millis");
    }

    public boolean sourceAllowed(String server) {
        List<String> allowed = allowedSources();
        return allowed.isEmpty() || contains(allowed, server);
    }

    public String confirmMode() {
        return confirm.string("mode");
    }

    public long confirmTimeoutMillis() {
        return confirm.longValue("timeout-millis");
    }

    public String confirmOnTimeout() {
        return confirm.string("on-timeout");
    }

    public boolean confirmOnCommand() {
        return applyTo.bool("command");
    }

    public boolean confirmOnMessaging() {
        return applyTo.bool("messaging");
    }

    public String confirmSubcommand() {
        return subcommand.string("confirm");
    }

    public String confirmCancelSubcommand() {
        return subcommand.string("cancel");
    }

    public String chatKeyword() {
        return chat.string("keyword");
    }

    public String chatCancelKeyword() {
        return chat.string("cancel-keyword");
    }

    public boolean chatIgnoreCase() {
        return chat.bool("ignore-case");
    }

    public String chatReplacement() {
        return chat.string("replacement");
    }

    public String clickLabel() {
        return click.string("label");
    }

    public String clickCommandTemplate() {
        return click.string("command");
    }

    public String clickPlaceholder() {
        return click.string("placeholder");
    }

    public String resultMode() {
        return result.string("mode");
    }

    public long resultWaitMillis() {
        return result.longValue("wait-millis");
    }

    public long resultPollDelayMillis() {
        return result.longValue("poll-delay-millis");
    }

    private static boolean oneOf(String value, String... allowed) {
        for (String item : allowed) {
            if (item.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static void nonNegative(String path, long value) {
        if (value < 0) {
            throw new YamlConfig.ConfigProblem(path);
        }
    }

    private static boolean contains(List<String> list, String value) {
        for (String item : list) {
            if (item.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    public static final class LobbyEntry {
        private final String name;
        private final int weight;

        LobbyEntry(String name, int weight) {
            this.name = name;
            this.weight = weight;
        }

        public String name() {
            return name;
        }

        public int weight() {
            return weight;
        }
    }
}
