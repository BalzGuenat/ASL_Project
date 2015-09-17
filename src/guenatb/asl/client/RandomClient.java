package guenatb.asl.client;

import guenatb.asl.GlobalConfig;

import java.io.IOException;
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

    void main(String[] args) throws IOException{
        String messageBody = randomMessage();

    }

    String randomMessage() {
        int messageSize = random.nextInt(GlobalConfig.BODY_SIZE_UPPER_BOUND - GlobalConfig.BODY_SIZE_LOWER_BOUND)
                + GlobalConfig.BODY_SIZE_LOWER_BOUND;
        byte[] messageBytes = new byte[messageSize];
        random.nextBytes(messageBytes);
        return new String(messageBytes);
    }
}
