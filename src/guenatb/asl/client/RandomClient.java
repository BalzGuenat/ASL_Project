package guenatb.asl.client;

import guenatb.asl.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Random;
import java.util.UUID;

/**
 * Created by Balz Guenat on 17.09.2015.
 * This client creates a queue and then repeatedly does the following steps:
 * It sends a message with no specific receiver to the created queue.
 * It checks if there are queues with messages for itself.
 * If there is such a queue, a message is popped from it.
 */
public class RandomClient extends AbstractClient {

    private static final Random random = new Random();
    private static final char[] symbols = ("" +
            "0123456789" +
            "qwertyuiopasdfghjklzxcvbnm" +
            "QWERTYUIOPASDFGHJKLZXCVBNM" +
            "., ?!"
            ).toCharArray();

    RandomClient(String host) {
        super(UUID.randomUUID(), host);
    }

    /**
     *
     * @param args args[0] is an integer determining the maximal number of messages each client thread sends per second.
     *             If it is negative then clients do not sleep between messages.
     *             args[1] is the hostname the clients should connect to.
     */
    public static void main(String[] args) {
        try {
            if (args.length < 2)
                throw new RuntimeException("Not enough arguments passed to RandomClient.");
            RandomClient client = new RandomClient(args[1]);
            UUID queueId = UUID.randomUUID();
            client.createQueue(queueId);
            long millisBetweenMessages = 0;
            try {
                millisBetweenMessages = 1000 / Integer.valueOf(args[0]);
            } catch (ArithmeticException e) {
                millisBetweenMessages = 0;
            } finally {
                if (millisBetweenMessages < 0)
                    millisBetweenMessages = 0;
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    client.deleteQueue(queueId);
                } catch (CommunicationException e) {
                    log.error("Error when deleting queue.", e);
                }
                log.info(String.format("Client %s has exited.", client.clientId.toString()));
            }));
            Instant timeOfLastMessage = Instant.now();
            while (true) {
                NormalMessage msg = new NormalMessage(UUID.randomUUID(), client.clientId, null, queueId, randomMessage());
                long timeToSleep = Instant.now().until(timeOfLastMessage.plusMillis(millisBetweenMessages), ChronoUnit.MILLIS);
                if (timeToSleep > 0) try {
                    Thread.sleep(timeToSleep);
                } catch (InterruptedException e) {
                    log.error("Interrupted while sleeping.");
                }
                timeOfLastMessage = Instant.now();
                client.sendMessage(msg);
                Collection<UUID> readyQueues = client.fetchReadyQueues();
                if (!readyQueues.isEmpty()) {
                    client.popFromQueue(readyQueues.iterator().next());
                }
            }
        } catch (CommunicationException e) {
            log.error("Communication failed.");
            e.printStackTrace();
        } catch (InvalidOperation invalidOperation) {
            log.info("Queue does not exist.");
            invalidOperation.printStackTrace();
        }
    }

    static String randomMessage() {
        int messageSize = random.nextInt(GlobalConfig.BODY_SIZE_UPPER_BOUND - GlobalConfig.BODY_SIZE_LOWER_BOUND)
                + GlobalConfig.BODY_SIZE_LOWER_BOUND;
        char[] msg = new char[messageSize];
        for (int i = 0; i < messageSize; i++)
            msg[i] = symbols[random.nextInt(symbols.length)];
        return String.valueOf(msg);
    }
}
