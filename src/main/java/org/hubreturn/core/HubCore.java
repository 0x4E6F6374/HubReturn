package org.hubreturn.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class HubCore {

    private final Platform platform;
    private final Cooldowns cooldowns = new Cooldowns();
    private final Confirmations confirmations = new Confirmations();
    private final DeliveryWatch delivery = new DeliveryWatch();
    private final Map<UUID, CancelToken> inFlight = new ConcurrentHashMap<>();

    private volatile Settings settings;
    private volatile MessageBundle bundle;
    private volatile LobbySelector selector;
    private volatile String activeChannel;

    private HubCore(Platform platform) {
        this.platform = platform;
    }

    public static HubCore start(Platform platform) throws IOException {
        HubCore core = new HubCore(platform);
        core.reload();
        return core;
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return bundle.defaultMessages();
    }

    private Messages logs() {
        return bundle.defaultMessages();
    }

    private Messages textsFor(Locale locale) {
        return bundle.forLocale(locale);
    }

    private Messages textsFor(CommandActor actor) {
        return bundle.forLocale(actor.locale());
    }

    public void reload() throws IOException {
        Settings next = Settings.load(platform.dataDirectory());
        next.validate();
        MessageBundle texts = MessageBundle.load(platform.dataDirectory(),
                next.languageDefault(), next.languageFiles());
        LobbySelector picker = new LobbySelector(next, platform);
        Messages logs = texts.defaultMessages();

        settings = next;
        bundle = texts;
        selector = picker;
        cooldowns.configure(next);
        cooldowns.clear();
        cancelPending(logs);
        for (CancelToken token : inFlight.values()) {
            token.cancel();
        }
        inFlight.clear();
        delivery.clear();

        String channel = next.channel();
        boolean wanted = next.messagingEnabled() && !channel.isEmpty();
        if (wanted && !channel.equals(activeChannel)) {
            if (activeChannel != null) {
                platform.unregisterChannel(activeChannel);
            }
            try {
                platform.registerChannel(channel);
                activeChannel = channel;
                platform.info(logs.format("log.channel", Map.of("channel", channel)));
            } catch (RuntimeException bad) {
                activeChannel = null;
                platform.warn(logs.format("log.bad-channel", Map.of(
                        "channel", channel, "error", describe(bad))));
            }
        } else if (!wanted && activeChannel != null) {
            platform.unregisterChannel(activeChannel);
            activeChannel = null;
        }

        platform.registerCommands(this);
        if (next.lobbyEntries().isEmpty()) {
            platform.warn(logs.text("log.no-lobbies"));
        }
        platform.info(logs.format("log.loaded", Map.of(
                "lobbies", String.valueOf(next.lobbyEntries().size()),
                "strategy", next.lobbyStrategy(),
                "platform", platform.platformName())));
    }

    public void shutdown() {
        if (activeChannel != null) {
            platform.unregisterChannel(activeChannel);
            activeChannel = null;
        }
    }

    public void onCommand(CommandActor actor, String[] args) {
        Settings current = settings;
        Messages logs = logs();
        Messages texts = textsFor(actor);
        String first = args.length > 0 ? args[0] : "";

        if (first.equalsIgnoreCase(current.reloadSubcommand())) {
            if (!allowed(actor, current.reloadPermission())) {
                actor.sendMessage(texts.text("command.no-permission"));
                return;
            }
            try {
                reload();
                actor.sendMessage(texts.text("command.reloaded"));
                platform.info(logs.text("log.reload"));
            } catch (Exception bad) {
                actor.sendMessage(texts.format("command.reload-failed", Map.of("error", describe(bad))));
                platform.error(logs.format("log.reload-failed", Map.of("error", describe(bad))), bad);
            }
            return;
        }

        if (!actor.isPlayer()) {
            actor.sendMessage(texts.text("command.console-only"));
            return;
        }
        PlayerRef player = (PlayerRef) actor;

        if (first.equalsIgnoreCase(current.confirmSubcommand())) {
            confirm(player, texts);
            return;
        }
        if (first.equalsIgnoreCase(current.confirmCancelSubcommand())) {
            cancel(player, texts);
            return;
        }
        if (current.confirmMode().equals("repeat-command") && confirmations.has(player.uuid())) {
            confirm(player, texts);
            return;
        }

        if (!allowed(actor, current.commandPermission())) {
            actor.sendMessage(texts.text("command.no-permission"));
            return;
        }
        if (inLobby(player.currentServer(), current)) {
            if (current.alreadyInLobby()) {
                player.sendMessage(texts.text("command.already-there"));
            }
            return;
        }
        if (!bypassed(current.bypassCooldownPermission(), actor::hasPermission)) {
            long left = cooldowns.remaining(player.uuid(), Cooldowns.Kind.COMMAND);
            if (left > 0) {
                player.sendMessage(texts.format("command.cooldown", Map.of(
                        "seconds", String.valueOf((left + 999) / 1000))));
                return;
            }
            cooldowns.mark(player.uuid(), Cooldowns.Kind.COMMAND);
        }

        if (current.confirmOnCommand() && !current.confirmMode().equals("none")) {
            if (!startConfirmation(player, Confirmations.Source.COMMAND, "", current, texts)) {
                cooldowns.clear(player.uuid(), Cooldowns.Kind.COMMAND);
            }
            return;
        }
        commandTransfer(player, player.currentServer(), texts);
    }

    public void onKick(KickView view) {
        Settings current = settings;
        Messages logs = logs();
        if (!current.kickEnabled()) {
            return;
        }
        if (bypassed(current.kickBypassPermission(), view::hasPermission)
                || !current.kickServerAllowed(view.fromServer())
                || !current.kickReasonAllowed(view.reason())) {
            if (current.debug()) {
                platform.info(logs.format("log.kick-dropped", Map.of(
                        "player", view.playerName(), "server", view.fromServer())));
            }
            return;
        }

        Optional<ServerRef> target = selector.pick(view.fromServer());
        if (target.isEmpty()) {
            platform.warn(logs.format("log.drop-no-lobby", Map.of("player", view.playerName())));
            return;
        }
        String shown = current.kickShowCustomReason()
                ? textsFor(view.locale()).format("kick.redirected", Map.of("server", view.fromServer()))
                : view.reason();
        view.redirectTo(target.get(), shown);
        platform.info(logs.format("log.kick-redirected", Map.of(
                "player", view.playerName(), "server", view.fromServer(), "lobby", target.get().name())));
    }

    public Optional<ServerRef> pickInitial(PlayerRef player) {
        Settings current = settings;
        if (!current.joinEnabled() || bypassed(current.joinBypassPermission(), player::hasPermission)) {
            return Optional.empty();
        }
        return selector.pick(null);
    }

    public void onPluginMessage(String channel, String source, boolean fromPlayer, byte[] data) {
        Settings current = settings;
        Messages logs = logs();
        if (!current.messagingEnabled()) {
            return;
        }
        if (!channel.equals(current.channel())) {
            drop(logs, "log.drop-channel", source, "");
            return;
        }
        if (fromPlayer && !current.allowPlayerSource()) {
            drop(logs, "log.drop-player-source", source, "");
            return;
        }
        if (!current.sourceAllowed(source) || (source.isEmpty() && !fromPlayer)) {
            drop(logs, "log.drop-source-server", source, "");
            return;
        }

        MessageCodec.Request request = MessageCodec.parse(data);
        if (request == null) {
            drop(logs, "log.drop-frame", source, "");
            return;
        }
        if (!request.action().equals(current.requestAction())) {
            drop(logs, "log.drop-action", source, "");
            return;
        }

        Optional<PlayerRef> found = platform.player(request.player());
        if (found.isEmpty()) {
            drop(logs, "log.drop-offline", source, request.player());
            reply(current, source, request.player(), false, "");
            return;
        }

        PlayerRef player = found.get();
        if (cooldowns.remaining(player.uuid(), Cooldowns.Kind.MESSAGE) > 0) {
            drop(logs, "log.drop-cooldown", source, player.name());
            reply(current, source, player.name(), false, "");
            return;
        }
        cooldowns.mark(player.uuid(), Cooldowns.Kind.MESSAGE);

        if (current.confirmOnMessaging() && !current.confirmMode().equals("none")) {
            if (!startConfirmation(player, Confirmations.Source.MESSAGE, source, current, textsFor(player))) {
                reply(current, source, player.name(), false, logs.text("confirm.pending-reason"));
                cooldowns.clear(player.uuid(), Cooldowns.Kind.MESSAGE);
            }
            return;
        }
        messageTransfer(player, source, current, logs);
    }

    public String onChat(UUID id, String name, String message) {
        Settings current = settings;
        Messages logs = logs();
        if (!current.confirmMode().equals("chat") || !confirmations.has(id)) {
            return null;
        }
        String typed = message.trim();
        boolean confirmWord = matches(typed, current.chatKeyword(), current.chatIgnoreCase());
        boolean cancelWord = !confirmWord && !current.chatCancelKeyword().isEmpty()
                && matches(typed, current.chatCancelKeyword(), current.chatIgnoreCase());
        if (!confirmWord && !cancelWord) {
            return null;
        }
        PlayerRef player = resolve(id, name);
        if (player == null) {
            platform.info(logs.format("log.confirm-offline", Map.of("player", name)));
            return null;
        }
        Messages texts = textsFor(player);
        if (confirmWord) {
            confirm(player, texts);
        } else {
            cancel(player, texts);
        }
        String replacement = current.chatReplacement();
        return replacement.isEmpty() ? texts.text("confirm.chat-echo") : replacement;
    }

    public void onServerConnected(UUID id, String server) {
        delivery.arrived(id, server);
    }

    private void cancelPending(Messages logs) {
        for (Confirmations.Pending pending : confirmations.drain().values()) {
            PlayerRef live = resolve(pending.id(), pending.playerName());
            if (live != null) {
                live.sendMessage(textsFor(pending.locale()).text("confirm.reload-cancelled"));
            }
            if (pending.source() == Confirmations.Source.MESSAGE) {
                reply(settings, pending.replySource(), pending.playerName(), false,
                        logs.text("confirm.reload-reason"));
            }
            cooldowns.clear(pending.id(), kind(pending));
        }
    }

    private boolean startConfirmation(PlayerRef player, Confirmations.Source source, String replySource,
                                      Settings current, Messages texts) {
        long window = current.confirmTimeoutMillis();
        long now = System.currentTimeMillis();
        long deadline = now + window;
        if (!confirmations.put(player.uuid(), new Confirmations.Pending(
                player.uuid(), player.name(), source, replySource, deadline, player.locale()))) {
            long left = confirmations.remaining(player.uuid(), now);
            player.sendMessage(texts.format("confirm.pending", Map.of(
                    "seconds", String.valueOf((left + 999) / 1000))));
            return false;
        }
        sendPrompt(player, current, texts, window);
        platform.schedule(() -> onConfirmTimeout(player.uuid()), window);
        return true;
    }

    private void sendPrompt(PlayerRef player, Settings current, Messages texts, long window) {
        String seconds = String.valueOf((window + 999) / 1000);
        String main = "/" + current.commandName();
        switch (current.confirmMode()) {
            case "repeat-command":
                player.sendMessage(texts.format("confirm.prompt-repeat",
                        Map.of("command", main, "seconds", seconds)));
                return;
            case "subcommand":
                player.sendMessage(texts.format("confirm.prompt-subcommand", Map.of(
                        "command", main,
                        "confirm", current.confirmSubcommand(),
                        "cancel", current.confirmCancelSubcommand(),
                        "seconds", seconds)));
                return;
            case "chat":
                player.sendMessage(texts.format("confirm.prompt-chat", Map.of(
                        "keyword", current.chatKeyword(),
                        "cancel-keyword", current.chatCancelKeyword(),
                        "seconds", seconds)));
                return;
            case "click":
                String template = texts.format("confirm.prompt-click", Map.of("seconds", seconds));
                String command = current.clickCommandTemplate()
                        .replace("{command}", current.commandName())
                        .replace("{confirm}", current.confirmSubcommand());
                player.sendActionMessage(template, current.clickPlaceholder(), current.clickLabel(), command);
                return;
            default:
                return;
        }
    }

    private void confirm(PlayerRef player, Messages texts) {
        Optional<Confirmations.Pending> found = confirmations.take(player.uuid());
        if (found.isEmpty()) {
            player.sendMessage(texts.text("confirm.no-pending"));
            return;
        }
        Confirmations.Pending pending = found.get();
        if (pending.deadline() <= System.currentTimeMillis()) {
            player.sendMessage(texts.text("confirm.expired"));
            cooldowns.clear(pending.id(), kind(pending));
            return;
        }
        PlayerRef live = resolve(pending.id(), pending.playerName());
        if (live == null) {
            platform.info(logs().format("log.confirm-offline", Map.of("player", pending.playerName())));
            cooldowns.clear(pending.id(), kind(pending));
            return;
        }
        if (pending.source() == Confirmations.Source.COMMAND
                && !allowed(live, settings.commandPermission())) {
            live.sendMessage(texts.text("command.no-permission"));
            cooldowns.clear(pending.id(), kind(pending));
            return;
        }
        execute(live, pending, texts);
    }

    private void cancel(PlayerRef player, Messages texts) {
        Optional<Confirmations.Pending> found = confirmations.take(player.uuid());
        if (found.isEmpty()) {
            player.sendMessage(texts.text("confirm.no-pending"));
            return;
        }
        Confirmations.Pending pending = found.get();
        cooldowns.clear(pending.id(), kind(pending));
        player.sendMessage(texts.text("confirm.cancelled"));
        if (pending.source() == Confirmations.Source.MESSAGE) {
            reply(settings, pending.replySource(), pending.playerName(), false,
                    logs().text("confirm.cancel-reason"));
        }
        platform.info(logs().format("log.confirm-cancelled", Map.of("player", pending.playerName())));
    }

    private void onConfirmTimeout(UUID id) {
        Settings current = settings;
        Messages logs = logs();
        Optional<Confirmations.Pending> found = confirmations.takeExpired(id, System.currentTimeMillis());
        if (found.isEmpty()) {
            return;
        }
        Confirmations.Pending pending = found.get();
        Messages texts = textsFor(pending.locale());
        if (current.confirmOnTimeout().equals("execute")) {
            PlayerRef live = resolve(pending.id(), pending.playerName());
            if (live == null) {
                platform.info(logs.format("log.confirm-offline", Map.of("player", pending.playerName())));
                cooldowns.clear(id, kind(pending));
                return;
            }
            if (pending.source() == Confirmations.Source.COMMAND
                    && !allowed(live, current.commandPermission())) {
                platform.info(logs.format("log.confirm-permission", Map.of("player", pending.playerName())));
                cooldowns.clear(id, kind(pending));
                return;
            }
            execute(live, pending, texts);
            return;
        }
        PlayerRef live = resolve(pending.id(), pending.playerName());
        if (live != null) {
            live.sendMessage(texts.text("confirm.expired"));
        }
        cooldowns.clear(id, kind(pending));
        if (pending.source() == Confirmations.Source.MESSAGE) {
            reply(current, pending.replySource(), pending.playerName(), false,
                    logs.text("confirm.timeout-reason"));
        }
        platform.info(logs.format("log.confirm-timeout", Map.of("player", pending.playerName())));
    }

    private void execute(PlayerRef player, Confirmations.Pending pending, Messages texts) {
        if (pending.source() == Confirmations.Source.COMMAND) {
            commandTransfer(player, player.currentServer(), texts);
            return;
        }
        messageTransfer(player, pending.replySource(), settings, logs());
    }

    private void commandTransfer(PlayerRef player, String from, Messages texts) {
        transfer(player, from, result -> {
            if (!result.notice.isEmpty()) {
                player.sendMessage(result.notice);
                return;
            }
            if (result.ok) {
                player.sendMessage(texts.format("command.success", Map.of("server", result.server)));
            } else if (result.detail.isEmpty()) {
                player.sendMessage(texts.text("command.failed"));
            } else {
                player.sendMessage(texts.format("command.failed-detail", Map.of("reason", result.detail)));
            }
        }, () -> player.sendMessage(texts.text("confirm.reload-cancelled")));
    }

    private void messageTransfer(PlayerRef player, String source, Settings current, Messages logs) {
        transfer(player, player.currentServer(), result -> {
            if (result.notice.isEmpty() && !result.ok) {
                platform.warn(logs.format("log.drop-no-lobby", Map.of("player", player.name())));
            }
            reply(current, source, player.name(), result.ok, result.detail);
        }, () -> reply(settings, source, player.name(), false,
                logs.text("confirm.reload-reason")));
    }

    private void transfer(PlayerRef player, String from, Consumer<TransferResult> done,
                          Runnable onCancel) {
        UUID id = player.uuid();
        CancelToken token = new CancelToken(onCancel);
        if (inFlight.putIfAbsent(id, token) != null) {
            done.accept(new TransferResult("", false,
                    logs().text("confirm.busy-reason"), textsFor(player).text("confirm.busy")));
            return;
        }
        attempt(player, from, settings.retryCount(), new HashSet<>(), result -> {
            if (inFlight.remove(id, token) && !token.cancelled()) {
                done.accept(result);
            }
        });
    }

    private void attempt(PlayerRef player, String from, int retriesLeft, Set<String> tried,
                         Consumer<TransferResult> done) {
        Messages logs = logs();
        Optional<ServerRef> target = selector.pick(from, tried);
        if (target.isEmpty()) {
            done.accept(new TransferResult("", false, ""));
            return;
        }
        ServerRef chosen = target.get();
        tried.add(chosen.name());
        Settings current = settings;
        boolean watch = current.resultMode().equals("event");
        if (watch) {
            DeliveryWatch.Watch expected = delivery.await(player.uuid(), chosen.name(), arrived -> {
                done.accept(new TransferResult(chosen.name(), arrived,
                        arrived ? "" : logs.format("confirm.not-arrived",
                                Map.of("server", chosen.name()))));
            });
            if (expected == null) {
                done.accept(new TransferResult("", false,
                        logs.text("confirm.busy-reason"), textsFor(player).text("confirm.busy")));
                return;
            }
            platform.schedule(() -> {
                if (delivery.expire(player.uuid(), expected)) {
                    platform.info(logs.format("log.delivery-timeout", Map.of(
                            "player", player.name(), "server", chosen.name())));
                }
            }, current.resultWaitMillis());
        }
        player.connect(chosen, (ok, detail) -> {
            if (!ok) {
                if (watch) {
                    delivery.cancel(player.uuid());
                }
                if (retriesLeft <= 0) {
                    done.accept(new TransferResult(chosen.name(), false, detail == null ? "" : detail));
                    return;
                }
                platform.schedule(() -> attempt(player, from, retriesLeft - 1, tried, done),
                        settings.retryDelayMillis());
                return;
            }
            if (watch) {
                return;
            }
            verifyDelivery(player, chosen.name(), detail == null ? "" : detail, done);
        });
    }

    private void verifyDelivery(PlayerRef player, String server, String detail,
                                Consumer<TransferResult> done) {
        Settings current = settings;
        if (current.resultMode().equals("poll")) {
            platform.schedule(() -> {
                boolean arrived = player.currentServer().equalsIgnoreCase(server);
                done.accept(new TransferResult(server, arrived,
                        arrived ? "" : logs().format("confirm.not-arrived", Map.of("server", server))));
            }, current.resultPollDelayMillis());
            return;
        }
        done.accept(new TransferResult(server, true, detail));
    }

    private void reply(Settings current, String source, String player, boolean ok, String detail) {
        if (!current.replyEnabled() || source.isEmpty()) {
            return;
        }
        byte[] payload = MessageCodec.reply(current.replyTag(), player, ok, detail);
        platform.sendToServer(source, current.channel(), payload);
    }

    private void drop(Messages texts, String key, String source, String player) {
        if (!settings.debug()) {
            return;
        }
        Map<String, String> vars = new HashMap<>();
        vars.put("source", source == null ? "" : source);
        vars.put("player", player == null ? "" : player);
        platform.info(texts.format(key, vars));
    }

    private PlayerRef resolve(UUID id, String name) {
        return platform.player(name)
                .filter(player -> player.uuid().equals(id))
                .orElse(null);
    }

    private static boolean matches(String typed, String expected, boolean ignoreCase) {
        return ignoreCase ? typed.equalsIgnoreCase(expected) : typed.equals(expected);
    }

    private static boolean allowed(CommandActor actor, String node) {
        return node.isEmpty() || actor.hasPermission(node);
    }

    private static boolean bypassed(String node, Predicate<String> has) {
        return !node.isEmpty() && has.test(node);
    }

    private static boolean inLobby(String server, Settings current) {
        for (String name : current.lobbyNames()) {
            if (name.equalsIgnoreCase(server)) {
                return true;
            }
        }
        return false;
    }

    private static Cooldowns.Kind kind(Confirmations.Pending pending) {
        return pending.source() == Confirmations.Source.COMMAND
                ? Cooldowns.Kind.COMMAND : Cooldowns.Kind.MESSAGE;
    }

    private static String describe(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    public interface KickView {
        String playerName();

        String fromServer();

        String reason();

        boolean hasPermission(String node);

        Locale locale();

        void redirectTo(ServerRef target, String message);
    }

    private static final class TransferResult {
        private final String server;
        private final boolean ok;
        private final String detail;
        private final String notice;

        TransferResult(String server, boolean ok, String detail) {
            this(server, ok, detail, "");
        }

        TransferResult(String server, boolean ok, String detail, String notice) {
            this.server = server;
            this.ok = ok;
            this.detail = detail;
            this.notice = notice;
        }
    }

    private static final class CancelToken {
        private final Runnable onCancel;
        private final AtomicBoolean cancelled = new AtomicBoolean();

        CancelToken(Runnable onCancel) {
            this.onCancel = onCancel;
        }

        void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                onCancel.run();
            }
        }

        boolean cancelled() {
            return cancelled.get();
        }
    }
}
