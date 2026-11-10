package org.hubreturn.velocity;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListenerOrderTest {

    @Test
    void initialServerListenerRunsLast() throws NoSuchMethodException {
        Method onChoose = VelocityListeners.class.getMethod(
                "onChoose", PlayerChooseInitialServerEvent.class);

        assertEquals(PostOrder.LAST, onChoose.getAnnotation(Subscribe.class).order());
    }
}
