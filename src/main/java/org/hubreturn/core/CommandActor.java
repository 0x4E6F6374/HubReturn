package org.hubreturn.core;

public interface CommandActor {

    String name();

    boolean isPlayer();

    boolean hasPermission(String node);

    void sendMessage(String legacy);

    void sendActionMessage(String template, String placeholder, String label, String command);
}
