package org.hubreturn.core;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MessageCodecTest {

    @Test
    void parsesRequestFrame() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(buffer)) {
            out.writeUTF("hub");
            out.writeUTF("Notch");
        }

        MessageCodec.Request request = MessageCodec.parse(buffer.toByteArray());

        assertNotNull(request);
        assertEquals("hub", request.action());
        assertEquals("Notch", request.player());
    }

    @Test
    void rejectsBrokenFrame() {
        assertNull(MessageCodec.parse(null));
        assertNull(MessageCodec.parse(new byte[]{1, 2, 3}));
    }

    @Test
    void replyKeepsResultAndDetail() throws IOException {
        byte[] payload = MessageCodec.reply("reply", "Notch", false, "大厅不可用");

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            assertEquals("reply", in.readUTF());
            assertEquals("Notch", in.readUTF());
            assertFalse(in.readBoolean());
            assertEquals("大厅不可用", in.readUTF());
        }
    }
}
