# Backend integration

中文：[integration.md](integration.md)

Backend servers and the proxy talk over plugin messages. The channel, request action, and reply tag come from the `messaging` section of `config.yml`; the template values are `hubreturn:main`, `hub`, and `reply`. Backend servers must use the same values and follow them if the configuration changes. The payload uses Java `DataOutputStream` / `DataInputStream` with a fixed field order.

## Request: backend to proxy

Channel `hubreturn:main`, two UTF strings in this order:

1. `action`: `hub`
2. `player`: target player name

```java
private static final String CHANNEL = "hubreturn:main";
private static final String ACTION = "hub";

ByteArrayOutputStream buffer = new ByteArrayOutputStream();
try (DataOutputStream out = new DataOutputStream(buffer)) {
    out.writeUTF(ACTION);
    out.writeUTF(player.getName());
}
player.sendPluginMessage(plugin, CHANNEL, buffer.toByteArray());
```

Neither string may be empty. The message must come from a backend server: `allow-player-source: false` by default drops packets sent directly by a client. An empty `allowed-source-servers` accepts any backend server; a filled list filters by server name, case-insensitively. Velocity requires a `namespace:name` channel, and an invalid name only produces a warning in the log when registration fails, leaving messaging unusable.

## Reply: proxy to backend

Same channel, sent only when `reply-enabled: true` and the source server name is non-empty, and only to the backend server that sent the request. Four fields:

1. `tag` (UTF): `reply`
2. `player` (UTF): the player name from the request
3. `ok` (boolean): success
4. `detail` (UTF): failure reason or extra text

```java
private static final String REPLY_TAG = "reply";

// data comes from the PluginMessageListener callback
try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
    String tag = in.readUTF();
    String player = in.readUTF();
    boolean ok = in.readBoolean();
    String detail = in.readUTF();
    if (!REPLY_TAG.equals(tag)) {
        return;
    }
    // look up your own pending entry by player; ok drives the next step, detail is display text
}
```

`detail` is not an error code. It always comes from the file named by `language.default` (`messages_en.yml` in the template, English) and does not change with the player's language. It may be a short phrase such as `confirm.busy-reason`, `confirm.reload-reason`, `confirm.pending-reason`, `confirm.cancel-reason`, `confirm.timeout-reason`, `confirm.not-arrived`, or the raw connection failure text; on `ok=true` it is usually an empty string. Editing the message file changes this text, so backend servers should not branch on it.

## Processing order

1. Channel, source, source server allowlist, byte format, and action are checked. A mismatch drops the message; `debug: true` logs the specific reason.
2. The player must be online, then `messaging.cooldown-millis` is checked. An offline player or one still in cooldown gets `ok=false` immediately.
3. If `confirm.apply-to.messaging` is `true` and `confirm.mode` is not `none`, the request enters confirmation. Confirming, cancelling, timing out, and a proxy `/hub reload` all produce a reply, with a reason in `detail` on failure.
4. If the player already has a request in flight (command, message, or pending confirmation), the new one is refused with `confirm.busy-reason` as detail. Requests are not queued.
5. Transfers retry up to `lobbies.retry-count` with `lobbies.retry-delay-millis` between attempts, excluding servers already tried in the same transfer. Retries happen only when the connection callback fails.
6. After a successful connection, `confirm.result.mode` decides the outcome: `callback` uses the connection result only; `event` waits for the server-connected event and fails if it does not arrive within `confirm.result.wait-millis`; `poll` compares the current server name after `confirm.result.poll-delay-millis`. Arrival that cannot be confirmed is reported as a failure without another retry.

`reload` cancels in-flight requests: the player gets a notice and the originating backend server gets `ok=false` with `confirm.reload-reason` as detail.

If a backend server only needs to send a request and read a result, set `confirm.mode` to `none`, `confirm.result.mode` to `callback`, and tune the message cooldown to your own rate. Key meanings are in [configuration.en.md](configuration.en.md).
