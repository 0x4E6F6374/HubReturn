# 子服接入

English: [integration.en.md](integration.en.md)

子服与代理端通过插件消息通信。通道名、请求 action、回包 tag 都取自 `config.yml` 的 `messaging` 段，模板值分别是 `hubreturn:main`、`hub`、`reply`。子服要用同一套值，改过配置就跟着改。字节格式用 Java 的 `DataOutputStream` / `DataInputStream` 读写，字段顺序固定。

## 请求：子服发给代理端

通道 `hubreturn:main`，依次写两个 UTF 字符串：

1. `action`：`hub`
2. `player`：目标玩家名

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

两个字符串都不能为空。消息必须由子服发出：默认 `allow-player-source: false`，客户端直接发的包会被丢弃。`allowed-source-servers` 留空表示任何子服都能发，填了就按服务器名过滤，忽略大小写。通道名在 Velocity 侧必须是 `namespace:name` 形式，格式不合法时注册失败只会在日志里警告，消息功能不可用。

## 回包：代理端发给子服

同一通道，只在 `reply-enabled: true` 且来源服务器名非空时发送，而且只发给发起请求的那台子服。四个字段：

1. `tag`（UTF）：`reply`
2. `player`（UTF）：请求里的玩家名
3. `ok`（boolean）：是否成功
4. `detail`（UTF）：失败原因或补充说明

```java
private static final String REPLY_TAG = "reply";

// data 来自 PluginMessageListener 的回调
try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
    String tag = in.readUTF();
    String player = in.readUTF();
    boolean ok = in.readBoolean();
    String detail = in.readUTF();
    if (!REPLY_TAG.equals(tag)) {
        return;
    }
    // 用 player 找自己记下的等待项，ok 决定后续提示，detail 直接展示给需要的人
}
```

`detail` 不是错误码。它固定取 `language.default` 指向的那套文案（模板为 `messages_en.yml`，英文），不随玩家语言变化；可能是 `confirm.busy-reason`、`confirm.reload-reason`、`confirm.pending-reason`、`confirm.cancel-reason`、`confirm.timeout-reason`、`confirm.not-arrived` 这类短语，也可能是连接失败的原文；`ok=true` 时通常为空字符串。改文案会改掉这段内容，子服不要拿它做分支判断。

## 处理顺序

1. 校验通道、来源、来源服务器白名单、字节格式、action。任一不通过就丢弃，`debug: true` 时日志里能看到具体原因。
2. 查玩家是否在线，再查 `messaging.cooldown-millis`。玩家不在线或还在冷却里，直接回 `ok=false`。
3. 如果 `confirm.apply-to.messaging` 为 `true` 且 `confirm.mode` 不是 `none`，先进入确认流程。玩家确认、取消、超时、以及代理端 `/hub reload` 都会触发回包，失败时 detail 写明原因。
4. 同一玩家已有请求在处理时（命令、消息、等待确认都算），新请求直接拒绝，回包 detail 为 `confirm.busy-reason`。请求不排队。
5. 传送按 `lobbies.retry-count` 重试，间隔 `lobbies.retry-delay-millis`，每次排除本次已经试过的服务器。重试只在连接回调失败时发生。
6. 连接成功后按 `confirm.result.mode` 判定：`callback` 只看连接结果；`event` 等进服事件，`confirm.result.wait-millis` 内没等到算失败；`poll` 延迟 `confirm.result.poll-delay-millis` 后比较当前服务器名。没能确认到达只回报失败，不再重试。

`reload` 会取消在途请求，玩家收到提示，来源子服收到 `ok=false` 且 detail 为 `confirm.reload-reason`。

如果子服只需要"发个请求、等个结果"，把 `confirm.mode` 设为 `none`、`confirm.result.mode` 设为 `callback`、消息冷却按自己的频率调一下，链路最短。配置项含义见 [configuration.md](configuration.md)。
