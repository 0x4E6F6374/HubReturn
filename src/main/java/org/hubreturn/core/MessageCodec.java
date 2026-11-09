package org.hubreturn.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class MessageCodec {

    private MessageCodec() {
    }

    public static byte[] reply(String tag, String player, boolean ok, String detail) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(buffer)) {
            out.writeUTF(tag);
            out.writeUTF(player);
            out.writeBoolean(ok);
            out.writeUTF(detail == null ? "" : detail);
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
        return buffer.toByteArray();
    }

    public static Request parse(byte[] data) {
        if (data == null) {
            return null;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            String action = in.readUTF();
            String player = in.readUTF();
            if (action.isEmpty() || player.isEmpty()) {
                return null;
            }
            return new Request(action, player);
        } catch (IOException | IllegalArgumentException bad) {
            return null;
        }
    }

    public static final class Request {
        private final String action;
        private final String player;

        Request(String action, String player) {
            this.action = action;
            this.player = player;
        }

        public String action() {
            return action;
        }

        public String player() {
            return player;
        }
    }
}
