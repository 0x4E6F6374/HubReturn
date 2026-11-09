package org.hubreturn.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import org.hubreturn.core.CommandActor;
import org.hubreturn.core.HubCore;
import org.hubreturn.core.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class HubCommand implements SimpleCommand {

    private final HubCore core;
    private final VelocityBootstrap platform;

    HubCommand(HubCore core, VelocityBootstrap platform) {
        this.core = core;
        this.platform = platform;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandActor actor = invocation.source() instanceof Player
                ? new VelocityPlayer((Player) invocation.source())
                : platform.console();
        core.onCommand(actor, invocation.arguments());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length > 1) {
            return List.of();
        }
        Settings current = core.settings();
        String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        CommandSource source = invocation.source();
        List<String> out = new ArrayList<>();
        add(out, prefix, current.reloadSubcommand(), permitted(source, current.reloadPermission()));
        add(out, prefix, current.confirmSubcommand(), permitted(source, current.commandPermission()));
        add(out, prefix, current.confirmCancelSubcommand(), permitted(source, current.commandPermission()));
        return out;
    }

    private static void add(List<String> out, String prefix, String value, boolean permitted) {
        if (permitted && value.toLowerCase(Locale.ROOT).startsWith(prefix)) {
            out.add(value);
        }
    }

    private static boolean permitted(CommandSource source, String node) {
        return node.isEmpty() || source.hasPermission(node);
    }
}
