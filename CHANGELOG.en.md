# Changelog

中文：[CHANGELOG.md](CHANGELOG.md)

Versions follow `v*` tags; dates are UTC.

## v1.0.0 — 2026-11-09

First public release. The GitHub Release carries the jar and a `.sha256` checksum.

- Four entry points: command, kick takeover, initial server on join, and backend plugin message requests
- Lobby strategies: least-players, round-robin, random, weighted, first
- Confirmation modes: none, repeat the command, subcommand, chat keyword, click button; timeout can cancel or execute
- Delivery checks: callback, event, poll; failed connections are retried up to `retry-count`, excluding servers already tried
- Plugin messages validate the source server and action; replies carry tag, player, ok, detail
- Configuration and message templates merge by nested key; a missing key or wrong type stops the load; `/hub reload` re-registers command names, aliases, and channels
- Reload cancels in-flight requests: the player gets a notice and the originating backend server gets a failure reply; duplicate requests for the same player are refused
- Cooldowns roll back on cancel, timeout, and failed confirmation; command-sourced requests recheck permission before confirmation and timeout execution
- Chat confirmation words are replaced with `confirm.chat-echo` from `messages.yml` instead of being broadcast
- Tab completion on both proxies is filtered by permission; a startup validation error is passed to the proxy, while a failed reload keeps the previous configuration
- Java 17 bytecode, compiled against Velocity 3.4.0 / BungeeCord 1.21-R0.4; CI verifies both JDK builds and publishes a Release on tags

Limitations: no loading test on a running proxy, so no Minecraft version range is claimed; Velocity 4.x has only been compile-checked on JDK 25, runtime compatibility remains unverified.
