package guenatb.asl.client;

import guenatb.asl.CommunicationException;
import guenatb.asl.NormalMessage;

import java.util.Random;
import java.util.UUID;

/**
 * Created by Balz Guenat on 17.09.2015.
 */
public class FixedClient extends AbstractClient {

    static final Random random = new Random();

    FixedClient(UUID clientId, String host) {
        super(clientId, host);
    }

    /**
     *
     * @param args Must supply the clientId, receiverId and number of messages in that order.
     */
    public static void main(String[] args) {
        if (args.length < 3)
            throw new RuntimeException("Invalid number of arguments.");

        long id = Long.parseLong(args[0]);
        UUID receiverId = new UUID(0,Long.parseLong(args[1]));
        UUID queueId = null; // TODO add this
        int numOfMsgs = Integer.parseInt(args[2]);
        FixedClient client = new FixedClient(new UUID(0, id), "localhost");
        for (int i = 0; i < numOfMsgs; i++) {
            String msgBody = String.format("This is client %s sending message number %d to client %s.",
                    client.clientId.toString(), i, receiverId.toString());
            NormalMessage msg = new NormalMessage(UUID.randomUUID(), client.clientId,
                    receiverId, queueId, msgBody);
            try {
                client.sendMessage(msg);
            } catch (CommunicationException e) {
                e.printStackTrace();
            }
        }
    }

}
