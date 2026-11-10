# HubReturn

中文：[README.md](README.md)

A proxy-side plugin that sends players back to a lobby. The same jar works on Velocity and BungeeCord; the logic lives in one place and each proxy only supplies events, scheduling, and message channels.

Players can return with a command. Players kicked by a backend server can be redirected to a lobby. The initial server on join can be a lobby. A backend server can also request a transfer for a named player over plugin messages. The target lobby is picked from a configured list using one of several strategies.

## Install

1. Download the jar and the matching `.sha256` from [Releases](https://github.com/0x4E6F6374/HubReturn/releases), verify them, and put the jar in the proxy's `plugins/` directory.
2. Restart the proxy. `config.yml` and the language files are written on first start. The data directory differs between the two proxies:
   - Velocity: `plugins/hubreturn/`
   - BungeeCord: `plugins/HubReturn/`
3. List the registered lobby server names under `lobbies.servers`. With an empty list the plugin still loads, but every return request has nowhere to go and a warning is logged at startup.
4. Run `/hub reload` or restart the proxy. Command names and aliases take effect on reload.

The artifact is Java 17 bytecode, so the proxy needs Java 17 or newer. Use the file name of the version you downloaded for the checksum:

```
sha256sum -c HubReturn-1.0.2.jar.sha256
```

## Commands and permissions

Command names, aliases, subcommand names, and permission nodes all come from `config.yml`. The table below shows the template defaults. An empty permission node means no check. The console can only run `reload`; every other command gets a notice.

| Command | Purpose | Permission |
| --- | --- | --- |
| `/hub` (aliases `/lobby`, `/back`) | Return to a lobby | `hubreturn.use` |
| `/hub reload` | Reload configuration | `hubreturn.reload` |
| `/hub confirm` | Confirm a pending request | see below |
| `/hub cancel` | Cancel a pending request | see below |

| Permission node | Effect |
| --- | --- |
| `hubreturn.bypass.cooldown` | Skip the command cooldown |
| `hubreturn.bypass.kick` | Do not take over kicks from backend servers |
| `hubreturn.bypass.join` | Leave the initial server unchanged on join |

The `confirm` and `cancel` subcommands do not check a permission themselves. When the pending request came from a command, `hubreturn.use` is checked again before the transfer runs, so a revoked permission drops the request. Requests that came from a backend plugin message follow the message source and do not check the player's permission. Both proxies offer tab completion for subcommands, filtered by permission.

## Languages

The template ships Chinese and English text. Player-facing messages follow the client locale: `zh_CN` resolves to `zh`, `en_US` to `en`, and anything unmatched falls back to `language.default` (`en` in the template). Proxy logs, console output, and the `detail` field in replies to backend servers always use `default` and do not change with the player.

`language.files` in `config.yml` maps language codes to file names. `messages.yml` is Chinese and keeps the filename from v1.0.0, so custom text survives an upgrade; `messages_en.yml` is English. Adding a language means one more entry and one more file with the same keys; missing keys fall back to `default`. A test in the repository checks that every language file has the same key set.

`confirm.chat.keyword`, `confirm.chat.cancel-keyword`, `confirm.click.label`, and `confirm.chat.replacement` still live in `config.yml` and are shared by all players. See [docs/configuration.en.md](docs/configuration.en.md) for details.

## Compatibility

| Item | Status |
| --- | --- |
| Artifact | Java 17 bytecode; one jar for Velocity and BungeeCord, containing both `bungee.yml` and `velocity-plugin.json` |
| Velocity | Compiled against 3.4.0; `-Pvelocity4` rechecks API signatures against 4.3.1 on JDK 25 |
| BungeeCord | Compiled against 1.21-R0.4 |
| Build JDK | JDK 21 by default; `-Pvelocity4` needs JDK 25 because the 4.x API ships Java 25 bytecode |

No loading test has been done on a running proxy, so no Minecraft version range is claimed here. Modrinth requires `game-versions`; fill it from your own test results. Velocity 4.x ships Adventure 5.2.0 while the default build uses Adventure 4.x, so a successful compile does not prove runtime compatibility. Try it on a test proxy before switching.

## Configuration

Each template is written to the data directory first, then the copy in that directory overrides it key by key. Writing only the keys you want to change is fine; lists are replaced as a whole, not appended to. A missing key, a wrong type, or an unknown enum value fails validation: at startup the exception is passed to the proxy and the plugin fails to load, while `/hub reload` keeps the previous configuration.

Points worth knowing:

- `reload` re-registers commands and channels and replaces the whole language bundle. Requests waiting for confirmation and transfers in flight are cancelled; the player gets a notice and the originating backend server gets a failure reply.
- A new request for a player who already has one in flight is refused with a reply, not queued.
- Cooldowns are rolled back on cancel, timeout, and failed confirmation, so a fresh attempt is not blocked by the earlier one.
- Retries exclude servers already tried in the same transfer, so a broken server is not picked over and over.
- `kick.enabled` defaults to `false` in the template. Set `kick.mode` and `kick.servers` before enabling it: `mode: all` takes over every backend kick, so on a mixed network a Forge backend's kicks would also land players in a Paper lobby.
- `join` replaces the initial server only when it is the proxy default. Velocity runs at `PostOrder.LAST` and BungeeCord at `EventPriority.HIGHEST`, so forced hosts and other plugins have already set their target; `join.override-existing: true` restores the old unconditional behaviour.

Full configuration reference: [docs/configuration.en.md](docs/configuration.en.md). Backend integration protocol: [docs/integration.en.md](docs/integration.en.md). Version history: [CHANGELOG.en.md](CHANGELOG.en.md).

## Build and release

```
mvn -B clean verify
mvn -B -Pvelocity4 clean verify   # needs JDK 25
```

Pushes to `main` and pull requests run two verification jobs: the default build on JDK 21 and `-Pvelocity4` on JDK 25. Pushing a `v*` tag also publishes a GitHub Release with the shaded jar and a SHA-256 checksum. The jar name follows the pom version; nothing checks the tag name against the pom, so keep the two aligned before tagging.

## License

MIT, see [LICENSE](LICENSE).
