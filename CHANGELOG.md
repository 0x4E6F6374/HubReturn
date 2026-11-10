# 更新日志

English: [CHANGELOG.en.md](CHANGELOG.en.md)

版本以 `v*` tag 为准，日期用 UTC。

## v1.0.2 — 2026-11-10

- 进服选服不再无条件覆盖初始服：两端改到最后执行，只有代理默认初始服才换成大厅，forced-hosts 与其他插件指定的服务器保持原样
- `config.yml` 新增 `join.override-existing`（默认 `false`），设为 `true` 恢复旧的无条件覆盖行为
- `kick.enabled` 模板默认值改为 `false`，避免装好就接管所有后端踢人；启用前需自行配置 `kick.mode` 与 `kick.servers`
- 中英 README 与配置文档补充混合网络、Forge 子服以及事件顺序的说明

事件顺序、forced-hosts 与混合网络下的表现只经过单元测试，未在真实代理端验证；Velocity 4.x 运行时兼容仍待实机确认。

## v1.0.1 — 2026-11-09

- 新增多语言文案：玩家提示按客户端语言选择（`zh_CN → zh → default`），默认语言 `en`；代理端日志、控制台与回包 detail 固定用默认语言
- `config.yml` 新增 `language.default` 与 `language.files`；新增英文文案模板 `messages_en.yml`，原 `messages.yml` 保留为中文，升级不丢自定义文案
- 其它语言缺键回退默认语言的同名键；新增测试校验各语言文案键集合一致
- `config.yml` 里的确认词、取消词、点击按钮文字仍全服统一，不随玩家语言变化

限制与 v1.0.0 相同；多语言只做过编译与单元测试，客户端语言是否生效需要在真实代理端确认。

## v1.0.0 — 2026-11-09

首个公开版本。GitHub Release 附带 jar 与 `.sha256` 校验文件。

- 四类入口：命令回大厅、被子服踢出时接管、首次进服优选大厅、子服插件消息请求
- 大厅选择策略：least-players、round-robin、random、weighted、first
- 确认方式：不确认、重复命令、子命令、聊天关键词、点击按钮；超时可取消或直接执行
- 送达判定：callback、event、poll；连接失败按 retry-count 重试，并排除本次已试过的服务器
- 插件消息带来源服务器白名单与 action 校验，回包含 tag、player、ok、detail
- 配置与文案模板按嵌套键合并，缺键或类型错误拒绝加载；`/hub reload` 热更命令名、别名与通道
- 重载取消在途请求：玩家收到提示，来源子服收到失败回包；同一玩家的重复请求直接拒绝
- 冷却在取消、超时、确认失败时回滚；命令来源在确认与超时执行前复检权限
- chat 确认词替换为 messages.yml 的 `confirm.chat-echo`，不会原样广播
- 两端子命令补全按权限过滤；启动校验失败时把异常交给代理端，重载校验失败则保留旧配置
- Java 17 字节码，Velocity 3.4.0 / BungeeCord 1.21-R0.4 编译；CI 双矩阵验证加 tag 自动发 Release

限制：没有在真实代理端上做过加载测试，不声明 Minecraft 版本范围；Velocity 4.x 只在 JDK 25 下做过编译签名复验，运行时兼容待实机验证。
