package guenatb.asl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.UUID;

/**
 * Created by Balz Guenat on 17.09.2015.
 */
public abstract class AbstractMessage implements Serializable {

    UUID messageId;
    UUID senderId;

    public static AbstractMessage fromStream(InputStream in)
            throws IOException {
        try {
            return (AbstractMessage) new ObjectInputStream(in).readObject();
        } catch (ClassNotFoundException | ClassCastException e) {
            System.err.println("Invalid message received.");
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public UUID getMessageId() {
        return messageId;
    }

    public UUID getSenderId() {
        return senderId;
    }

}
