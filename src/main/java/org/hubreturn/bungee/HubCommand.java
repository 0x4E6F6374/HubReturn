package org.hubreturn.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.hubreturn.core.CommandActor;
import org.hubreturn.core.HubCore;

final class HubCommand extends Command {

    private final HubCore core;
    private final BungeeBootstrap plugin;

    HubCommand(HubCore core, BungeeBootstrap plugin) {
        super(core.settings().commandName(), null,
                core.settings().commandAliases().toArray(new String[0]));
        this.core = core;
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        CommandActor actor = sender instanceof ProxiedPlayer
                ? new BungeePlayer((ProxiedPlayer) sender)
                : plugin.console();
        core.onCommand(actor, args);
    }
}
