package org.hubreturn.bungee;

import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListenerOrderTest {

    @Test
    void initialServerListenerRunsLast() throws NoSuchMethodException {
        Method onConnect = BungeeListeners.class.getMethod("onConnect", ServerConnectEvent.class);

        assertEquals((int) EventPriority.HIGHEST,
                (int) onConnect.getAnnotation(EventHandler.class).priority());
    }
}
