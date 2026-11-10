# 配置说明

English: [configuration.en.md](configuration.en.md)

`config.yml` 与各语言文案文件都在数据目录里：Velocity 是 `plugins/hubreturn/`，BungeeCord 是 `plugins/HubReturn/`。jar 内的每份模板先原样落地，之后每次加载再用数据目录里的同名文件按嵌套键覆盖。模板是完整默认值，所以数据目录里只写要改的键也能用；列表是整段替换，不是追加。

缺键、类型不对、枚举值不在允许范围内会抛配置错误：启动时插件加载失败，异常交给代理端；`/hub reload` 时旧配置继续生效。`debug: true` 会把各类丢弃原因打进日志，排消息链路时先开它。

## config.yml

### 全局

| 键 | 模板值 | 说明 |
| --- | --- | --- |
| `debug` | `false` | 记录丢弃原因、踢出未接管等排查信息 |

布尔值只接受 `true` / `false`（大小写不敏感）或 YAML 布尔字面量，写 `1`、`yes` 会报配置错误。

### language

| 键 | 模板值 | 说明 |
| --- | --- | --- |
| `default` | `en` | 没命中语言码的玩家、代理端日志、控制台、发给子服的回包 detail 都用它 |
| `files` | `zh: messages.yml`、`en: messages_en.yml` | 语言码到文件名的映射，也是可用语言清单；`default` 必须是这里的某个 key |

匹配顺序是 `Locale.toString()` 精确命中（例如 `zh_CN`）→ `getLanguage()`（例如 `zh`）→ `default`。Velocity 在玩家还没发客户端设置时、BungeeCord 在取不到 locale 时、以及控制台，都按 `default` 处理。

`messages.yml` 沿用老版本文件名给 `zh`，`v1.0.0` 升级上来自定义的中文文案不会丢。`default` 指向的那套文案键必须完整，缺键会在加载时抛配置错误；其它语言缺键回退到 `default` 的同名键。仓库里有个测试检查所有语言文件的键集合一致。新增语言只要在 `files` 加一项，并放一份同结构的文案文件。

### command

| 键 | 模板值 | 说明 |
| --- | --- | --- |
| `name` | `hub` | 主命令名；重载后重新注册 |
| `aliases` | `lobby`, `back` | 别名列表，同样随重载生效 |
| `permission` | `hubreturn.use` | 使用命令的权限；留空不检查 |
| `bypass-cooldown-permission` | `hubreturn.bypass.cooldown` | 有此权限时跳过命令冷却 |
| `cooldown-millis` | `1500` | 命令冷却，毫秒 |
| `already-in-lobby` | `true` | 已经在大厅时是否给提示；`false` 为静默返回 |
| `reload-subcommand` | `reload` | 重载子命令名 |
| `reload-permission` | `hubreturn.reload` | 重载权限；留空不检查 |

冷却在取消确认、确认超时、确认失败时回滚，所以被取消后可以立即重新发起。

### lobbies

| 键 | 模板值 | 说明 |
| --- | --- | --- |
| `strategy` | `least-players` | 选服策略，见下表 |
| `servers` | `name: lobby`, `weight: 1` | 候选大厅列表，`name` 必须是代理端已注册的服务器名 |
| `skip-current` | `true` | 挑选时排除玩家当前所在的服务器 |
| `retry-count` | `2` | 连接失败后的重试次数，不含首次 |
| `retry-delay-millis` | `500` | 每次重试的间隔，毫秒 |

| strategy | 行为 |
| --- | --- |
| `least-players` | 在线人数最少 |
| `round-robin` | 依次轮询 |
| `random` | 等概率随机 |
| `weighted` | 按 `weight` 加权随机，小于 1 的权重按 1 处理 |
| `first` | 取列表第一台 |

只有代理端能查到的服务器才会进候选池。连接失败重试时，本次已试过的服务器会被排除，不会反复选中同一台故障服。

### kick

| 键 | 模板值 | 说明 |
| --- | --- | --- |
| `enabled` | `false` | 是否接管被子服踢出的玩家；模板默认关闭 |
| `mode` | `all` | `all` 全接管 / `whitelist` 只接 `servers` 里的 / `blacklist` 排除 `servers` 里的 |
| `servers` | 空列表 | 配合 `mode` 的服务器名列表，忽略大小写 |
| `reason-contains` | 空列表 | 踢出原因命中任意一个子串才接管；留空表示不看原因，匹配忽略大小写 |
| `bypass-permission` | `hubreturn.bypass.kick` | 有此权限的玩家不被接管 |
| `show-custom-reason` | `true` | 用 `kick.redirected` 文案替换原有踢出原因；`false` 保留原原因 |

模板默认不接管任何踢人。启用前先设 `mode` 和 `servers`：`mode: all` 配空 `servers` 会把所有后端踢出都送进大厅，混合网络里 Forge 子服的踢出也走这条路，通常不是想要的结果。踢出接管不走确认流程，`confirm.apply-to` 只管命令和插件消息两类入口。

### join

| 键 | 模板值 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 首次进代理时，在初始服是代理默认服的情况下换成选中的大厅 |
| `override-existing` | `false` | `true` 时无视 forced-hosts 和其他插件设的初始服，一律送大厅 |
| `bypass-permission` | `hubreturn.bypass.join` | 有此权限时不改初始服务器 |

两端都在最后执行：Velocity 是 `PostOrder.LAST`，BungeeCord 是 `EventPriority.HIGHEST`，因此能读到别的监听器已经改过的目标。只有目标名等于代理默认初始服时才替换，Velocity 比对 `try` 列表，BungeeCord 比对 `defaultServer` 与 `serverPriority`；forced-hosts 或其他插件指定的服务器保持原样。事件没给出目标时由 HubReturn 兜底。

### messaging

| 键 | 模板值 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 是否处理子服插件消息 |
| `channel` | `hubreturn:main` | 通道名；Velocity 侧必须是 `namespace:name` 形式，格式不合法时注册失败只会在日志里警告 |
| `request-action` | `hub` | 请求里的 action，必须匹配才处理 |
| `reply-tag` | `reply` | 回包里的 tag |
| `reply-enabled` | `true` | 是否给来源子服回包 |
| `allowed-source-servers` | 空列表 | 留空表示任何子服都能发；填了只认这些服务器名 |
| `allow-player-source` | `false` | 是否接受客户端直接发的包，默认拒绝 |
| `cooldown-millis` | `0` | 按玩家计的插件消息冷却，毫秒 |

通道名改动后由 `reload` 注销旧通道再注册新通道。回包只发给发起请求的那台子服。协议格式见 [integration.md](integration.md)。

### confirm

| 键 | 模板值 | 说明 |
| --- | --- | --- |
| `mode` | `repeat-command` | 确认方式，见下表 |
| `timeout-millis` | `10000` | 确认窗口，毫秒 |
| `on-timeout` | `cancel` | `cancel` 超时取消 / `execute` 超时直接执行 |
| `apply-to.command` | `true` | 命令入口是否需要确认 |
| `apply-to.messaging` | `true` | 插件消息入口是否需要确认 |
| `subcommand.confirm` | `confirm` | 确认子命令名 |
| `subcommand.cancel` | `cancel` | 取消子命令名 |

| mode | 玩家怎么确认 |
| --- | --- |
| `none` | 不需要确认，直接传送 |
| `repeat-command` | 在窗口内再输一次主命令 |
| `subcommand` | 输 `/hub confirm`，或 `/hub cancel` 取消 |
| `chat` | 在聊天栏发关键词确认，或发取消关键词取消 |
| `click` | 点提示里的按钮，按钮执行 `click.command` 拼出的命令 |

`subcommand.confirm` 与 `subcommand.cancel` 不能留空；`mode: chat` 时 `chat.keyword` 不能留空；`mode: click` 时 `click` 下三个键都不能留空。

| 键 | 模板值 | 说明 |
| --- | --- | --- |
| `chat.keyword` | `confirm` | 聊天确认词，全服统一 |
| `chat.cancel-keyword` | `cancel` | 聊天取消词，留空表示只能等超时 |
| `chat.ignore-case` | `true` | 匹配是否忽略大小写 |
| `chat.replacement` | `""` | 命中后把聊天内容替换成这段文本；留空用当前玩家语言文案的 `confirm.chat-echo` |
| `click.label` | `&a[点击确认]` | 按钮文字，全服统一 |
| `click.command` | `/{command} {confirm}` | 按钮执行的命令，`{command}` 换主命令名，`{confirm}` 换确认子命令名 |
| `click.placeholder` | `{button}` | 提示文案里要被按钮替换的占位符 |

chat 模式只替换聊天内容，不取消消息：Velocity 从 1.19.1 起把聊天结果设成 denied 会把玩家踢下线。确认词不会原样出现在公屏上。确认词、取消词与按钮文字都在 `config.yml` 里，不随玩家语言变化。

### confirm.result

| 键 | 模板值 | 说明 |
| --- | --- | --- |
| `mode` | `callback` | 送达判定方式，见下表 |
| `wait-millis` | `5000` | `event` 模式等待进服事件的时长，毫秒 |
| `poll-delay-millis` | `1000` | `poll` 模式延迟多久查当前服务器，毫秒 |

| mode | 判定 |
| --- | --- |
| `callback` | 只看连接回调的结果，不验证是否真的到达 |
| `event` | 等进服事件确认到达，`wait-millis` 内没等到算失败 |
| `poll` | 延迟后比较玩家当前服务器名 |

三种模式在连接回调失败时都会重试；已经连接成功但没确认到达时只回报失败，不再发起重试。三个时间值都不能为负。

## 文案文件

模板带两套：`messages.yml` 是中文（`zh`），`messages_en.yml` 是英文（`en`，也是 `language.default`）。每份都由 jar 内模板与数据目录里的同名文件按嵌套键合并；改完 `/hub reload` 生效。

`&` 后面跟颜色码，两端都按 legacy 格式解析。`{prefix}` 会自动展开成 `prefix` 的值，其它 `{xxx}` 由调用处传入。`default` 指向的那套不能有缺键，其它文件缺键回退到它的同名键；仓库里有个测试检查所有语言文件的键集合一致。

玩家看到的提示按客户端语言选，日志、控制台与回包 detail 固定用 `default`。要换成自己的语言，改对应文件即可；想加一门语言按 `language.files` 的说明来。

常用占位符：

| 文案键 | 占位符 |
| --- | --- |
| `command.success` | `{server}` |
| `command.failed-detail` | `{reason}` |
| `command.cooldown` | `{seconds}` |
| `command.reload-failed` | `{error}` |
| `confirm.prompt-repeat` | `{command}`, `{seconds}` |
| `confirm.prompt-subcommand` | `{command}`, `{confirm}`, `{cancel}`, `{seconds}` |
| `confirm.prompt-chat` | `{keyword}`, `{cancel-keyword}`, `{seconds}` |
| `confirm.prompt-click` | `{seconds}`，另有 `{button}` 由 `click.placeholder` 指向按钮 |
| `confirm.pending` | `{seconds}` |
| `confirm.not-arrived` | `{server}` |
| `kick.redirected` | `{server}` |
| `log.*` | 按需用 `{player}`, `{server}`, `{lobby}`, `{channel}`, `{lobbies}`, `{strategy}`, `{platform}`, `{error}`, `{source}` |

`confirm.busy-reason`、`confirm.reload-reason`、`confirm.pending-reason`、`confirm.cancel-reason`、`confirm.timeout-reason` 是发给来源子服的回包 detail，始终取 `default` 那套文案，不是玩家看到的提示；`confirm.busy` 才是给玩家的提示。`log.*` 只进代理端日志，同样取 `default`。完整键和当前文案以生成的文件为准。

## 重载时发生什么

`reload` 先构造并校验新快照，全部通过才替换内存里的配置与语言包，然后重新注册命令、按需注销并注册通道。等待确认的请求逐个取消：玩家按发起时记下的语言码收到 `confirm.reload-cancelled`，消息来源的子服收到 `ok=false` 且 detail 为 `confirm.reload-reason`，对应的冷却记录一并回滚。传送中的请求也被取消，回调被丢弃，避免重复通知。命令冷却表整体清空。
