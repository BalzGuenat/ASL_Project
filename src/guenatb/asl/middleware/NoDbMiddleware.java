package guenatb.asl.middleware;

import guenatb.asl.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by Balz Guenat on 17.09.2015.
 * This class is used to test the middleware without the DB.
 */
public class NoDbMiddleware {

    static final Logger log = Logger.getLogger(NoDbMiddleware.class);

    static List<Thread> workerThreads = new ArrayList<>(GlobalConfig.THREADS_PER_MIDDLEWARE);
    static ArrayBlockingQueue<Socket> connectionQueue = new ArrayBlockingQueue<>(GlobalConfig.QUEUE_CAPACITY);

    public static void main(String[] args) {
        for (int i = 0; i < GlobalConfig.THREADS_PER_MIDDLEWARE; i++) {
            NoDbMiddleware mw = new NoDbMiddleware();
            Thread t = new Thread(mw::doWork);
            workerThreads.add(t);
            t.start();
        }

        log.info("Started all worker threads.");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Thread t : workerThreads) {
                t.interrupt();
                try {
                    t.join(5000);
                    if (t.isAlive())
                        log.error("Middleware thread did not die after interrupt.");
                } catch (InterruptedException e) {
                    log.error("Interrupted while waiting for child thread to die.", e);
                }
            }
        }));

        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(GlobalConfig.MIDDLEWARE_PORT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.info("Server socket created and now listening...");

        while (!Thread.interrupted()) {
            try {
                Socket clientSocket = serverSocket.accept();
                connectionQueue.add(clientSocket);
            } catch (IOException e) {
                log.error("Error while waiting for client to connect.", e);
            } catch (IllegalStateException e) {
                log.error("Queue is full.");
            }
        }
        log.info("Middleware main thread shutting down after interrupt.");
    }

    private void doWork() {
        try {
            while (!Thread.interrupted()) {
                Socket clientSocket = connectionQueue.take();
                try {
                    ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
                    AbstractMessage msg = AbstractMessage.fromStream(ois);
                    Instant timeAccepted = Instant.now();
                    AbstractMessage response = handleMessage(msg);
                    ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
                    oos.writeObject(response);
                    oos.flush();
                    log.info("Total connection time: " + String.valueOf(timeAccepted.until(Instant.now(),
                            ChronoUnit.MILLIS) + "ms."));
                    // Request fulfilled, put socket back in queue.
                    connectionQueue.add(clientSocket);
                } catch (IOException e) {
                    log.info("Client (apparently) closed the connection.");
                    try {clientSocket.close();} catch (IOException ignored) {}
                } catch (CommunicationException e) {
                    log.error("Error during connection.", e);
                }
            }
        } catch (InterruptedException ignored) {}
        log.info("Middleware worker thread shutting down after interrupt.");
    }

    private AbstractMessage handleMessage(AbstractMessage msg) {
        if (msg instanceof NormalMessage) {
            NormalMessage nmsg = (NormalMessage) msg;
            Timestamp now = new Timestamp(new java.util.Date().getTime());
            nmsg.setTimeOfArrival(now);
            addMessage(nmsg);
            return ConnectionEndMessage.SUCCESS;
        } else {
            log.error("Invalid message type received.");
            return ConnectionEndMessage.INVALID_OPERATION;
        }
    }

    void addMessage(NormalMessage msg) {}

}
