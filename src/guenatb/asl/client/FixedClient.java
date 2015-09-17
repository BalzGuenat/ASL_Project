package guenatb.asl.client;

import guenatb.asl.NormalMessage;

import java.io.IOException;
import java.util.Random;
import java.util.UUID;

/**
 * Created by Balz Guenat on 17.09.2015.
 */
public class FixedClient extends AbstractClient {

    static final Random random = new Random();
    int noOfMessagesSent = 0;

    FixedClient(UUID clientId) {
        super(clientId);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1)
            throw new RuntimeException("Must supply an ID as argument.");

        long id = (long) Long.parseLong(args[0]);
        FixedClient client = new FixedClient(new UUID(0, id));

        client.sendMessage();
    }

    void sendMessage() throws IOException {
        String msgBody = String.format("This is client %s sending message number %d.",
                clientId.toString(), noOfMessagesSent);
        NormalMessage msg = new NormalMessage(UUID.randomUUID(), clientId,
                new UUID(0, 0), new UUID(0, 0), msgBody);
        sendMessage(msg);
    }

}
