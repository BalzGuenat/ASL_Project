package guenatb.asl.client;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Balz Guenat on 18.09.2015.
 */
public abstract class ClientDriver {

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
                        startClient(r.readLine());
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
            if (args[0].equals("FixedClient")) {
                for (int i = 0; i < Integer.parseInt(args[1]); i++) {
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                FixedClient.main(args);
                            } catch (IOException e) {
                                System.err.println("FixedClient threw IOException.");
                            }
                        }
                    });
                    t.run();
                }
            }
        } catch (NumberFormatException e) {
            throw new IOException();
        }
    }

}
