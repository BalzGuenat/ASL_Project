package guenatb.asl.client;

import guenatb.asl.ControlMessage;
import guenatb.asl.GlobalConfig;
import guenatb.asl.NormalMessage;

import java.io.IOException;
import java.util.Collection;
import java.util.Random;
import java.util.UUID;

/**
 * Created by Balz Guenat on 17.09.2015.
 */
public class RandomClient extends AbstractClient {

    static final Random random = new Random();

    RandomClient() {
        super(UUID.randomUUID());
    }

    public static void main(String[] args) throws IOException {
        RandomClient client = new RandomClient();
        UUID queueId = UUID.randomUUID();
        ControlMessage createQueueMsg = new ControlMessage(client.clientId, ControlMessage.ControlType.CREATE_QUEUE, queueId);
        client.sendMessage(createQueueMsg);
        NormalMessage msg = new NormalMessage(UUID.randomUUID(), client.clientId, null, queueId, randomMessage());
        client.sendMessage(msg);
        Collection<UUID> readyQueues = client.fetchReadyQueues();
        if (!readyQueues.isEmpty()) {
            NormalMessage inbound = client.popFromQueue(readyQueues.iterator().next());
            System.out.println("Client received message: " + inbound.getBody());
        } else {
            System.out.println("No message available.");
        }
        ControlMessage deleteQueueMsg = new ControlMessage(client.clientId, ControlMessage.ControlType.DELETE_QUEUE, queueId);
        client.sendMessage(deleteQueueMsg);
    }

    static String randomMessage() {
        int messageSize = random.nextInt(GlobalConfig.BODY_SIZE_UPPER_BOUND - GlobalConfig.BODY_SIZE_LOWER_BOUND)
                + GlobalConfig.BODY_SIZE_LOWER_BOUND;
        byte[] messageBytes = new byte[messageSize];
        random.nextBytes(messageBytes);
        return new String(messageBytes);
    }
}
