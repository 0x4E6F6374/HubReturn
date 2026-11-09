# Configuration

中文：[configuration.md](configuration.md)

`config.yml` and the language files live in the data directory: `plugins/hubreturn/` on Velocity and `plugins/HubReturn/` on BungeeCord. Each template inside the jar is written out first, then the copy in the data directory overrides it key by key on every load. The template holds every default, so the data directory file may contain only the keys you change; lists are replaced as a whole, not appended to.

A missing key, a wrong type, or an unknown enum value raises a configuration error. At startup the exception is passed to the proxy and the plugin fails to load; during `/hub reload` the previous configuration stays in use. Set `debug: true` to log dropped messages and skipped kick takeovers, which is the first thing to turn on when debugging the message path.

## config.yml

### Global

| Key | Template value | Meaning |
| --- | --- | --- |
| `debug` | `false` | Log drop reasons and skipped kick takeovers |

Booleans accept only `true` / `false` (case-insensitive) or a YAML boolean literal. `1` and `yes` are configuration errors.

### language

| Key | Template value | Meaning |
| --- | --- | --- |
| `default` | `en` | Used for players whose locale matches nothing, proxy logs, the console, and the `detail` field in replies to backend servers |
| `files` | `zh: messages.yml`, `en: messages_en.yml` | Language code to file name mapping, also the list of available languages; `default` must be one of its keys |

Matching goes `Locale.toString()` (for example `zh_CN`) -> `getLanguage()` (for example `zh`) -> `default`. Velocity before the player has sent client settings, BungeeCord when no locale is available, and the console all use `default`.

`messages.yml` keeps the v1.0.0 filename for `zh`, so custom Chinese text survives an upgrade. The file named by `default` must have every key or loading fails; missing keys in other languages fall back to the same key in `default`. A test in the repository checks that all language files have the same key set. Adding a language means one more entry in `files` and one more file with the same structure.

### command

| Key | Template value | Meaning |
| --- | --- | --- |
| `name` | `hub` | Main command name; re-registered on reload |
| `aliases` | `lobby`, `back` | Alias list, also refreshed on reload |
| `permission` | `hubreturn.use` | Permission for the command; empty means no check |
| `bypass-cooldown-permission` | `hubreturn.bypass.cooldown` | Skip the command cooldown |
| `cooldown-millis` | `1500` | Command cooldown in milliseconds |
| `already-in-lobby` | `true` | Send a notice when the player is already in a lobby; `false` returns silently |
| `reload-subcommand` | `reload` | Reload subcommand name |
| `reload-permission` | `hubreturn.reload` | Reload permission; empty means no check |

Cooldowns are rolled back when a confirmation is cancelled, times out, or fails, so a player can retry immediately afterwards.

### lobbies

| Key | Template value | Meaning |
| --- | --- | --- |
| `strategy` | `least-players` | Selection strategy, see below |
| `servers` | `name: lobby`, `weight: 1` | Candidate lobbies; `name` must be a server registered on the proxy |
| `skip-current` | `true` | Exclude the player's current server from the pool |
| `retry-count` | `2` | Retries after a failed connection, not counting the first attempt |
| `retry-delay-millis` | `500` | Delay between retries in milliseconds |

| strategy | Behavior |
| --- | --- |
| `least-players` | Fewest online players |
| `round-robin` | Rotate in order |
| `random` | Uniform random |
| `weighted` | Weighted random using `weight`; values below 1 count as 1 |
| `first` | First entry in the list |

Only servers the proxy can resolve enter the pool. Retries exclude servers already tried in the same transfer, so one broken server is not selected repeatedly.

### kick

| Key | Template value | Meaning |
| --- | --- | --- |
| `enabled` | `true` | Take over players kicked by backend servers |
| `mode` | `all` | `all` handles every kick / `whitelist` only servers in `servers` / `blacklist` excludes servers in `servers` |
| `servers` | empty list | Server names used by `mode`, compared case-insensitively |
| `reason-contains` | empty list | Only take over when the kick reason contains one of these substrings; empty means ignore the reason, matching is case-insensitive |
| `bypass-permission` | `hubreturn.bypass.kick` | Players with this permission are not taken over |
| `show-custom-reason` | `true` | Replace the original kick reason with `kick.redirected`; `false` keeps the original |

Kick takeover never goes through confirmation. `confirm.apply-to` covers only the command and plugin message entry points.

### join

| Key | Template value | Meaning |
| --- | --- | --- |
| `enabled` | `true` | Set the initial server on join to the selected lobby |
| `bypass-permission` | `hubreturn.bypass.join` | Leave the initial server unchanged |

### messaging

| Key | Template value | Meaning |
| --- | --- | --- |
| `enabled` | `true` | Process backend plugin messages |
| `channel` | `hubreturn:main` | Channel name; Velocity requires `namespace:name`, and an invalid name only warns in the log when registration fails |
| `request-action` | `hub` | Expected action in the request |
| `reply-tag` | `reply` | Tag in the reply |
| `reply-enabled` | `true` | Send a reply to the originating backend server |
| `allowed-source-servers` | empty list | Empty accepts any backend server; otherwise only these names are accepted |
| `allow-player-source` | `false` | Accept messages sent directly by a client; refused by default |
| `cooldown-millis` | `0` | Per-player cooldown for plugin messages in milliseconds |

Changing the channel unregisters the old one and registers the new one during `reload`. Replies go only to the backend server that sent the request. See [integration.en.md](integration.en.md) for the wire format.

### confirm

| Key | Template value | Meaning |
| --- | --- | --- |
| `mode` | `repeat-command` | Confirmation mode, see below |
| `timeout-millis` | `10000` | Confirmation window in milliseconds |
| `on-timeout` | `cancel` | `cancel` drops the request / `execute` runs it on timeout |
| `apply-to.command` | `true` | Require confirmation for commands |
| `apply-to.messaging` | `true` | Require confirmation for plugin messages |
| `subcommand.confirm` | `confirm` | Confirm subcommand name |
| `subcommand.cancel` | `cancel` | Cancel subcommand name |

| mode | How the player confirms |
| --- | --- |
| `none` | No confirmation, transfer immediately |
| `repeat-command` | Run the main command again within the window |
| `subcommand` | Run `/hub confirm`, or `/hub cancel` to abort |
| `chat` | Send the keyword in chat, or the cancel keyword to abort |
| `click` | Click the button in the prompt, which runs the command built from `click.command` |

`subcommand.confirm` and `subcommand.cancel` must not be empty. `mode: chat` requires a non-empty `chat.keyword`. `mode: click` requires all three `click` keys.

| Key | Template value | Meaning |
| --- | --- | --- |
| `chat.keyword` | `confirm` | Chat confirmation word, shared by all players |
| `chat.cancel-keyword` | `cancel` | Chat cancel word; empty means only timeout can end the request |
| `chat.ignore-case` | `true` | Case-insensitive matching |
| `chat.replacement` | `""` | Text to replace the chat message with; empty uses `confirm.chat-echo` from the player's language |
| `click.label` | `&a[点击确认]` | Button text, shared by all players |
| `click.command` | `/{command} {confirm}` | Command the button runs; `{command}` is the main command name, `{confirm}` the confirm subcommand |
| `click.placeholder` | `{button}` | Placeholder in the prompt replaced by the button |

Chat mode replaces the message content instead of cancelling it: since 1.19.1, setting a Velocity chat result to denied disconnects the player. The confirmation word does not appear on public chat. Confirmation words and the button label come from `config.yml` and do not change with the player's language.

### confirm.result

| Key | Template value | Meaning |
| --- | --- | --- |
| `mode` | `callback` | Delivery check, see below |
| `wait-millis` | `5000` | How long `event` mode waits for the server-connected event, in milliseconds |
| `poll-delay-millis` | `1000` | How long `poll` mode waits before reading the current server, in milliseconds |

| mode | Check |
| --- | --- |
| `callback` | Use the connection callback only; does not verify arrival |
| `event` | Wait for the server-connected event; no event within `wait-millis` counts as failure |
| `poll` | Compare the player's current server name after the delay |

All three modes retry when the connection callback fails. Once the connection succeeded but arrival could not be confirmed, the result is reported as a failure without another retry. None of the three time values may be negative.

## Language files

The template ships two: `messages.yml` is Chinese (`zh`) and `messages_en.yml` is English (`en`, also `language.default`). Each is merged from the jar template and the file in the data directory by nested key; run `/hub reload` after editing.

`&` starts a legacy colour code on both proxies. `{prefix}` expands to the value of `prefix`; other `{xxx}` placeholders are supplied by the caller. The file named by `default` must have every key; missing keys in other files fall back to its same key. A test checks that all language files have the same key set.

Player-facing messages follow the client locale, while logs, console output, and reply details always use `default`. Edit the matching file to use your own wording, or follow `language.files` above to add a language.

Common placeholders:

| Message key | Placeholders |
| --- | --- |
| `command.success` | `{server}` |
| `command.failed-detail` | `{reason}` |
| `command.cooldown` | `{seconds}` |
| `command.reload-failed` | `{error}` |
| `confirm.prompt-repeat` | `{command}`, `{seconds}` |
| `confirm.prompt-subcommand` | `{command}`, `{confirm}`, `{cancel}`, `{seconds}` |
| `confirm.prompt-chat` | `{keyword}`, `{cancel-keyword}`, `{seconds}` |
| `confirm.prompt-click` | `{seconds}` plus `{button}`, which `click.placeholder` points at |
| `confirm.pending` | `{seconds}` |
| `confirm.not-arrived` | `{server}` |
| `kick.redirected` | `{server}` |
| `log.*` | `{player}`, `{server}`, `{lobby}`, `{channel}`, `{lobbies}`, `{strategy}`, `{platform}`, `{error}`, `{source}` as needed |

`confirm.busy-reason`, `confirm.reload-reason`, `confirm.pending-reason`, `confirm.cancel-reason`, and `confirm.timeout-reason` are reply details sent to the originating backend server, always taken from `default`; `confirm.busy` is the player-facing notice. `log.*` entries only go to the proxy log and also use `default`. The generated files are the full list of keys and current text.

## What reload does

`reload` builds and validates a new snapshot first, replaces the in-memory configuration and language bundle only if validation passes, then re-registers commands and unregisters/registers the channel as needed. Requests waiting for confirmation are cancelled one by one: the player receives `confirm.reload-cancelled` in the language recorded when the request started, the originating backend server receives `ok=false` with `confirm.reload-reason` as detail, and the matching cooldown entry is rolled back. Transfers in flight are cancelled too, and their callbacks are dropped so nothing is notified twice. The command cooldown table is cleared.
