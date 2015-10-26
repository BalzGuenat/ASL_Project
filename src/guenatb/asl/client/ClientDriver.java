package guenatb.asl.client;

import guenatb.asl.CommunicationException;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by Balz Guenat on 18.09.2015.
 */
public abstract class ClientDriver {

    static final Logger log = Logger.getLogger(ClientDriver.class);
    /**
     * This is the least amount of time (in millis) we wait for client threads after sending them an interrupt.
     */
    static final long WAIT_TIME_AFTER_INTERRUPT = 100;

    protected static final Map<UUID, AbstractClient> clients = new HashMap<>();

    protected static final String USAGE_INFO = "Usage:\n" +
            "ClientDriver configFile\n" +
            "\n" +
            "configFile is a file specifying the configuration of the clients.\n" +
            "Each line not starting with '#' should be of the following format:\n" +
            "clientClass n [clientArgs]\n" +
            "where clientClass is the name of a non-abstract client class, n is the number of such\n" +
            "clients to be created and clientArgs are additional arguments passed to each client.";

    public static void main(String[] args) {
        int lastReadLine = 0;
        if (args.length == 0) {
            System.out.print(USAGE_INFO);
        } else {
            try {
                BufferedReader r = new BufferedReader(new FileReader(args[0]));
                while (r.ready()) {
                    lastReadLine++;
                    String line = r.readLine();
                    if (!line.startsWith("#"))
                        startClient(line);
                }
            } catch (FileNotFoundException e) {
                System.err.println("configFile not found; exiting.");
            } catch (IOException e) {
                System.err.println("Error in configFile on line " + String.valueOf(lastReadLine));
            }
        }
    }

    protected static void startClient(String spec) throws IOException {
        try {
            String[] args = spec.split(" ");
            String[] clientArgs = Arrays.copyOfRange(args, 2, args.length);
            int numOfClients = Integer.parseInt(args[1]);
            List<Thread> threads = new ArrayList<>();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    for (Thread t : threads) {
                        if (t.isAlive()) {
                            t.interrupt();
                            t.join(WAIT_TIME_AFTER_INTERRUPT);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }));

            switch (args[0]) {
                case "RandomClient":
                    for (int i = 0; i < numOfClients; i++) {
                        threads.add(new Thread(() -> {
                            RandomClient.main(clientArgs);
                        }));
                    }
                    break;
                case "NoDbClient":
                    for (int i = 0; i < numOfClients; i++) {
                        threads.add(new Thread(() -> {
                            NoDbClient.main(clientArgs);
                        }));
                    }
                    break;

                default:
                    log.error(String.format("Don't know how to instantiate client class %s.", args[0]));
                    break;
            }

            for (Thread t : threads)
                t.start();
        } catch (NumberFormatException e) {
            log.error("Could not parse number in config line.", e);
        }
    }

}
