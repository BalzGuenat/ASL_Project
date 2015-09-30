package guenatb.asl.client;

import guenatb.asl.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.rmi.server.ExportException;
import java.util.Collection;
import java.util.UUID;

/**
 * Created by Balz Guenat on 17.09.2015.
 */
public abstract class AbstractClient {

    static final Logger log = Logger.getLogger(AbstractClient.class.getName());

    final UUID clientId;

    AbstractClient(UUID aclientId) {
        clientId = aclientId;
        try {
            register();
        } catch (CommunicationException e) {
            throw new RuntimeException("Could not register client.", e);
        }
    }

    void register() throws CommunicationException {
        ControlMessage msg = new ControlMessage(clientId, ControlMessage.ControlType.REGISTER_CLIENT);
        sendMessage(msg);
    }

    static void sendMessage(AbstractMessage msg) throws CommunicationException {
        ObjectOutputStream oos = null;
        try {
            Socket socket = new Socket(
                    GlobalConfig.FIRST_MIDDLEWARE_HOST,
                    GlobalConfig.FIRST_MIDDLEWARE_PORT);
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(msg);
            oos.flush();
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            ConnectionEndMessage endmsg = (ConnectionEndMessage) ois.readObject();
            if (endmsg.type == ConnectionEndMessage.ConnectionEndType.SUCCESS)
                log.info("Communication successful.");
            else
                log.error("Received error message from server.");
        } catch (ClassNotFoundException | ClassCastException e) {
            log.error("Received broken message.", e);
            throw new CommunicationException(e);
        } catch (IOException e) {
            log.error("Exception thrown by socket.", e);
            throw new CommunicationException(e);
        } finally {
            try {
                oos.close();
            } catch (IOException e) {
                log.error("Could not close socket.", e);
            }
        }
    }

    AbstractMessage sendQuery(ControlMessage msg) throws CommunicationException {
        try {
            Socket socket = new Socket(
                    GlobalConfig.FIRST_MIDDLEWARE_HOST,
                    GlobalConfig.FIRST_MIDDLEWARE_PORT);

            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(msg);
            oos.flush();

            return AbstractMessage.fromStream(socket.getInputStream());
        } catch (IOException e) {
            throw new CommunicationException(e);
        }
    }

    void createQueue(UUID queueId) throws CommunicationException {
        ControlMessage createQueueMsg = new ControlMessage(clientId, ControlMessage.ControlType.CREATE_QUEUE, queueId);
        sendMessage(createQueueMsg);
    }

    void deleteQueue(UUID queueId) throws CommunicationException {
        ControlMessage deleteQueueMsg = new ControlMessage(clientId, ControlMessage.ControlType.DELETE_QUEUE, queueId);
        sendMessage(deleteQueueMsg);
    }

    NormalMessage peekFromQueue(UUID queueId) throws CommunicationException {
        ControlMessage msg = new ControlMessage(clientId,
                ControlMessage.ControlType.PEEK_QUEUE,
                queueId);
        return (NormalMessage) sendQuery(msg);
    }

    NormalMessage popFromQueue(UUID queueId) throws CommunicationException {
        ControlMessage msg = new ControlMessage(clientId,
                ControlMessage.ControlType.POP_QUEUE,
                queueId);
        return (NormalMessage) sendQuery(msg);
    }

    Collection<UUID> fetchReadyQueues() throws CommunicationException {
        ControlMessage msg = new ControlMessage(clientId,
                ControlMessage.ControlType.GET_READY_QUEUES);
        return ((QueueListMessage) sendQuery(msg)).getQueueList();
    }

}
