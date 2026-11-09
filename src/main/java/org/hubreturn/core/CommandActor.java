package org.hubreturn.core;

import java.util.Locale;

public interface CommandActor {

    String name();

    boolean isPlayer();

    boolean hasPermission(String node);

    Locale locale();

    void sendMessage(String legacy);

    void sendActionMessage(String template, String placeholder, String label, String command);
}
