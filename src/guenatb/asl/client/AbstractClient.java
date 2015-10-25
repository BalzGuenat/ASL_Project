package guenatb.asl.client;

import guenatb.asl.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.UUID;

/**
 * Created by Balz Guenat on 17.09.2015.
 */
public abstract class AbstractClient {

    static final Logger log = Logger.getLogger(AbstractClient.class);

    final String host;
    final UUID clientId;
    Socket socket = null;

    AbstractClient(UUID aclientId, String host) {
        this.host = host;
        clientId = aclientId;
        try {
            register();
        } catch (CommunicationException e) {
            throw new RuntimeException("Could not register client.", e);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        closeSocket();
        super.finalize();
    }

    void closeSocket() {
        try {socket.close();} catch (IOException ignored) {}
    }

    void register() throws CommunicationException {
        ControlMessage msg = new ControlMessage(clientId, ControlMessage.ControlType.REGISTER_CLIENT);
        transmitMessage(msg);
    }

    private AbstractMessage transmitMessage(AbstractMessage msg) throws CommunicationException {
        try {
            if (socket == null || socket.isClosed())
                socket = new Socket(host, GlobalConfig.MIDDLEWARE_PORT);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(msg);
            oos.flush();
            Instant sent = Instant.now();
            log.info(String.format("Sent message: %s", msg.toString()));
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            AbstractMessage abstractResponse = AbstractMessage.fromStream(ois);
            long responseTime = sent.until(Instant.now(), ChronoUnit.MILLIS);
            log.info(String.format("Received response to message %s: %s after %d ms.",
                    msg, abstractResponse, responseTime));
            return abstractResponse;
        } catch (IOException e) {
            log.error("Error while sending message.", e);
            throw new CommunicationException(e);
        }
    }

    void sendMessage(NormalMessage msg) throws CommunicationException {
        AbstractMessage abstractResponse = transmitMessage(msg);
        if (!(abstractResponse instanceof ConnectionEndMessage &&
                ((ConnectionEndMessage) abstractResponse).type == ConnectionEndMessage.ConnectionEndType.SUCCESS)) {
            throw new CommunicationException();
        }
    }

    void createQueue(UUID queueId) throws CommunicationException {
        ControlMessage createQueueMsg = new ControlMessage(clientId, ControlMessage.ControlType.CREATE_QUEUE, queueId);
        transmitMessage(createQueueMsg);
    }

    void deleteQueue(UUID queueId) throws CommunicationException {
        ControlMessage deleteQueueMsg = new ControlMessage(clientId, ControlMessage.ControlType.DELETE_QUEUE, queueId);
        transmitMessage(deleteQueueMsg);
    }

    NormalMessage peekFromQueue(UUID queueId) throws CommunicationException {
        ControlMessage msg = new ControlMessage(clientId,
                ControlMessage.ControlType.PEEK_QUEUE,
                queueId);
        return (NormalMessage) transmitMessage(msg);
    }

    /**
     *
     * @param queueId
     * @return null if no message was available.
     * @throws CommunicationException thrown if queue does not exist
     */
    NormalMessage popFromQueue(UUID queueId) throws CommunicationException, InvalidOperation {
        ControlMessage msg = new ControlMessage(clientId,
                ControlMessage.ControlType.POP_QUEUE,
                queueId);
        AbstractMessage response = transmitMessage(msg);
        if (response instanceof NormalMessage)
            return (NormalMessage) response;
        else if (response instanceof ConnectionEndMessage &&
                ((ConnectionEndMessage) response).type == ConnectionEndMessage.SUCCESS.type)
            return null;
        else if (response instanceof ConnectionEndMessage &&
                ((ConnectionEndMessage) response).type == ConnectionEndMessage.ConnectionEndType.INVALID_OPERATION)
            throw new InvalidOperation();
        else
            throw new RuntimeException();
    }

    Collection<UUID> fetchReadyQueues() throws CommunicationException {
        ControlMessage msg = new ControlMessage(clientId,
                ControlMessage.ControlType.GET_READY_QUEUES);
        return ((QueueListMessage) transmitMessage(msg)).getQueueList();
    }

}
