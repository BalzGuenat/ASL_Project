package guenatb.asl.client;

import guenatb.asl.*;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
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
        register();
    }

    void register() {
        ControlMessage msg = new ControlMessage(clientId, ControlMessage.ControlType.REGISTER_CLIENT);
        try {
            sendMessage(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void sendMessage(AbstractMessage msg) throws IOException, ConnectException {
        Socket socket = new Socket(
                GlobalConfig.FIRST_MIDDLEWARE_HOST,
                GlobalConfig.FIRST_MIDDLEWARE_PORT);
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(msg);
        oos.flush();
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        try {
            ConnectionEndMessage endmsg = (ConnectionEndMessage) ois.readObject();
        } catch (ClassNotFoundException | ClassCastException e) {
            e.printStackTrace();
        }
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

    void createQueue(UUID queueId) throws IOException {
        ControlMessage createQueueMsg = new ControlMessage(clientId, ControlMessage.ControlType.CREATE_QUEUE, queueId);
        sendMessage(createQueueMsg);
    }

    void deleteQueue(UUID queueId) throws IOException {
        ControlMessage deleteQueueMsg = new ControlMessage(clientId, ControlMessage.ControlType.DELETE_QUEUE, queueId);
        sendMessage(deleteQueueMsg);
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
