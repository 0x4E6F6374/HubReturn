# HubReturn

English: [README.en.md](README.en.md)

跑在代理端的返回大厅插件。同一个 jar 放进 Velocity 或 BungeeCord 的插件目录即可，业务逻辑只有一份，两端各自接事件、调度和消息通道。

玩家可以用命令回大厅；被子服踢出时接回大厅；首次进服时优先送进大厅；子服也能通过插件消息把指定玩家送回。目标大厅从配置好的服务器列表里按策略挑选。

## 安装

1. 从 [Releases](https://github.com/0x4E6F6374/HubReturn/releases) 下载 jar 和同名的 `.sha256`，校验后放进代理端的 `plugins/` 目录。
2. 重启代理。首次启动会生成 `config.yml` 和各语言文案文件，两端的数据目录名不一样：
   - Velocity：`plugins/hubreturn/`
   - BungeeCord：`plugins/HubReturn/`
3. 在 `lobbies.servers` 里填上代理端已注册的大厅服务器名。留空时插件仍能启动，但所有返回请求都会落空，启动日志会给出警告。
4. 改完执行 `/hub reload`，或重启代理。命令名和别名在重载后立即生效。

产物是 Java 17 字节码，代理端需要 Java 17 或更高版本。校验命令里的文件名按实际下载到的版本写：

```
sha256sum -c HubReturn-1.0.1.jar.sha256
```

## 命令与权限

命令名、别名、子命令名和权限节点都在 `config.yml` 里，下表是模板默认值。权限节点留空表示不检查权限。控制台只能执行 `reload`，其它命令会收到一条提示。

| 命令 | 作用 | 权限 |
| --- | --- | --- |
| `/hub`（别名 `/lobby`、`/back`） | 返回大厅 | `hubreturn.use` |
| `/hub reload` | 重载配置 | `hubreturn.reload` |
| `/hub confirm` | 确认待处理的请求 | 见下方说明 |
| `/hub cancel` | 取消待处理的请求 | 见下方说明 |

| 权限节点 | 作用 |
| --- | --- |
| `hubreturn.bypass.cooldown` | 跳过命令冷却 |
| `hubreturn.bypass.kick` | 被子服踢出时不接管 |
| `hubreturn.bypass.join` | 进服时不改初始服务器 |

`confirm` 和 `cancel` 两个子命令本身不查权限：如果待确认的请求来自命令，执行前会重新检查 `hubreturn.use`，权限被撤回时请求会被丢弃；来自插件消息的请求按消息来源处理，不检查玩家权限。子命令补全两端都有，按权限过滤，只列出有权限的项。

## 语言

模板带中文与英文两套文案。玩家看到的提示按客户端语言选择：`zh_CN` 先找 `zh`，`en_US` 找 `en`，都没命中用 `language.default`（模板为 `en`）。代理端日志、控制台输出和发给子服的回包 `detail` 固定用 `default`，不随玩家变。

`config.yml` 的 `language.files` 列出语言码与文件名；`messages.yml` 是中文（沿用老版本文件名，`v1.0.0` 升级上来的自定义文案不会丢），`messages_en.yml` 是英文。新增一门语言只加一项映射和一份同结构的文案文件，缺的键回退到 `default` 那套。仓库里有个测试会检查各语言文案键集合一致。

`confirm.chat.keyword`、`confirm.chat.cancel-keyword`、`confirm.click.label`、`confirm.chat.replacement` 仍在 `config.yml` 里，全服统一，不随玩家语言变化。细节见 [docs/configuration.md](docs/configuration.md)。

## 兼容情况

| 项 | 情况 |
| --- | --- |
| 产物 | Java 17 字节码；Velocity 与 BungeeCord 共用同一个 jar，包内同时有 `bungee.yml` 和 `velocity-plugin.json` |
| Velocity | 对 3.4.0 编译；`-Pvelocity4` 用 4.3.1 在 JDK 25 下做过编译签名复验 |
| BungeeCord | 对 1.21-R0.4 编译 |
| 构建 JDK | 默认 JDK 21；`-Pvelocity4` 需要 JDK 25，因为 4.x 的 API 是 Java 25 字节码 |

没有在真实代理端上做过加载测试，所以这里不声明支持的 Minecraft 版本范围。Modrinth 的 `game-versions` 是必填字段，按自己实测的结果填。Velocity 4.x 带的是 Adventure 5.2.0，与默认编译用的 Adventure 4.x 不同，编译通过不等于运行时兼容，换版本前先在测试代理上试。

## 配置

每份模板先落到数据目录，再用数据目录里的同名文件按嵌套键覆盖，所以只写要改的键也可以；列表是整段替换，不是追加。缺键、类型不对、枚举值不在允许范围内，启动时会把异常交给代理端（插件加载失败），`/hub reload` 时则保留旧配置。

几件服主需要知道的事：

- `reload` 会重新注册命令和通道，并整体替换语言包；等待确认的请求与传送中的请求会被取消，玩家收到提示，消息来源的子服收到失败回包。
- 同一玩家已有请求在处理时，新请求直接拒绝并回包，不排队。
- 冷却在取消、超时、确认失败时回滚，重新发起不会被自己刚才的冷却挡住。
- 连接失败重试时排除本次已试过的服务器，不会反复撞同一台故障服。

完整配置项见 [docs/configuration.md](docs/configuration.md)，子服接入协议见 [docs/integration.md](docs/integration.md)，版本记录见 [CHANGELOG.md](CHANGELOG.md)。

## 构建与发布

```
mvn -B clean verify
mvn -B -Pvelocity4 clean verify   # 需要 JDK 25
```

push `main` 和 PR 会跑两路验证：JDK 21 默认构建、JDK 25 跑 `-Pvelocity4`。推 `v*` tag 时额外发布 GitHub Release，附件是 shade 后的 jar 与 SHA-256 校验文件。jar 名跟 pom 版本走，tag 名与 pom 版本之间没有自动校验，发版前自己对齐。

## 许可

MIT，见 [LICENSE](LICENSE)。
