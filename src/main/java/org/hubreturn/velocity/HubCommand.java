package org.hubreturn.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import org.hubreturn.core.CommandActor;
import org.hubreturn.core.HubCore;

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
}
