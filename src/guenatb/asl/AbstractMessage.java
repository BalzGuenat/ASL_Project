package guenatb.asl;

import guenatb.asl.client.AbstractClient;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.UUID;

/**
 * Created by Balz Guenat on 17.09.2015.
 */
public abstract class AbstractMessage implements Serializable {

    static final Logger log = Logger.getLogger(AbstractMessage.class.getName());

    public static AbstractMessage fromStream(InputStream in)
            throws CommunicationException {
        try {
            return (AbstractMessage) new ObjectInputStream(in).readObject();
        } catch (ClassNotFoundException | ClassCastException e) {
            log.error("Invalid message received.");
            throw new CommunicationException(e);
        } catch (IOException e) {
            log.error("Communication unsuccessful.");
            throw new CommunicationException(e);
        }
    }

}
