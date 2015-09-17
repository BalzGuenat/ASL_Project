package guenatb.asl.client;

import guenatb.asl.*;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Collection;
import java.util.UUID;

/**
 * Created by Balz Guenat on 17.09.2015.
 */
public abstract class AbstractClient {

    final UUID clientId;

    AbstractClient(UUID aclientId) {
        clientId = aclientId;
    }

    void sendMessage(AbstractMessage msg) throws IOException, ConnectException {
        Socket socket = new Socket(
                GlobalConfig.FIRST_MIDDLEWARE_HOST,
                GlobalConfig.FIRST_MIDDLEWARE_PORT);
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(msg);
        oos.flush();
        oos.close();
    }

    AbstractMessage sendQuery(ControlMessage msg) throws IOException {
        Socket socket = new Socket(
                GlobalConfig.FIRST_MIDDLEWARE_HOST,
                GlobalConfig.FIRST_MIDDLEWARE_PORT);

        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(msg);
        oos.flush();

        return AbstractMessage.fromStream(socket.getInputStream());
    }

    NormalMessage peekFromQueue(UUID queueId) throws IOException {
        ControlMessage msg = new ControlMessage(clientId,
                ControlMessage.ControlType.PEEK_QUEUE,
                queueId);
        return (NormalMessage) sendQuery(msg);
    }

    NormalMessage popFromQueue(UUID queueId) throws IOException {
        ControlMessage msg = new ControlMessage(clientId,
                ControlMessage.ControlType.POP_QUEUE,
                queueId);
        return (NormalMessage) sendQuery(msg);
    }

    Collection<UUID> fetchReadyQueues() throws IOException {
        ControlMessage msg = new ControlMessage(clientId,
                ControlMessage.ControlType.GET_READY_QUEUES);
        return ((QueueListMessage) sendQuery(msg)).getQueueList();
    }

}
