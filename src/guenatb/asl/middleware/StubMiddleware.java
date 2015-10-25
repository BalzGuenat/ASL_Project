package guenatb.asl.middleware;

import guenatb.asl.*;
import guenatb.asl.client.RandomClient;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by Balz Guenat on 17.09.2015.
 * This class is used to micro-benchmark the database
 */
public class StubMiddleware {

    final static int MESSAGE_LENGTH = 200;
    final static int THREAD_NUMBER = 8;

    static final int SERIALIZATION_FAILURE = 40001; // According to PostgreSQL docs.

    static final Logger log = Logger.getLogger(StubMiddleware.class);
    static List<Thread> workerThreads = new ArrayList<>(GlobalConfig.THREADS_PER_MIDDLEWARE);


    private static final Random random = new Random();
    private static final char[] symbols = ("" +
            "0123456789" +
            "qwertyuiopasdfghjklzxcvbnm" +
            "QWERTYUIOPASDFGHJKLZXCVBNM" +
            "., ?!"
    ).toCharArray();
    Connection dbConnection;

    StubMiddleware() {
        try {
            dbConnection = DriverManager.getConnection(
                    GlobalConfig.DB_URL,
                    GlobalConfig.DB_USER,
                    GlobalConfig.DB_PASSWORD);
            //dbConnection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < THREAD_NUMBER; i++) {
            StubMiddleware mw = new StubMiddleware();
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

        log.info("Server socket created and now listening...");

        while (!Thread.interrupted()) {}
        log.info("Middleware main thread shutting down after interrupt.");
    }

    static String randomMessage() {
        int messageSize = MESSAGE_LENGTH;
        char[] msg = new char[messageSize];
        for (int i = 0; i < messageSize; i++)
            msg[i] = symbols[random.nextInt(symbols.length)];
        return String.valueOf(msg);
    }

    private void doWork() {

        UUID clientId = UUID.randomUUID();
        UUID queueId = UUID.randomUUID();
        String msgbody = randomMessage();

        try {
            registerClient(clientId);
            createQueue(queueId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        while (!Thread.interrupted()) {
            NormalMessage msg = new NormalMessage(UUID.randomUUID(), clientId, null, queueId, msgbody);
            Timestamp now = new Timestamp(new java.util.Date().getTime());
            msg.setTimeOfArrival(now);
            try {
                Instant timeAccepted = Instant.now();
                addMessage(msg);
                long delay = timeAccepted.until(Instant.now(), ChronoUnit.MILLIS);
                log.info("T: " + String.valueOf(delay));
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        }
        log.info("Middleware worker thread shutting down after interrupt.");
    }

    private void registerClient(UUID senderId) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
                "INSERT INTO asl.active_clients " +
                        "(id) " +
                        "VALUES (?);"
        );
        int argNumber = 1;
        stmt.setObject(argNumber++, senderId);
        stmt.execute();
    }

    private void createQueue(UUID queueId) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
                "INSERT INTO asl.active_queues " +
                "(id) " +
                "VALUES (?);"
        );
        int argNumber = 1;
        stmt.setObject(argNumber++, queueId);
        stmt.execute();
    }

    private void deleteQueue(UUID queueId) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
                "DELETE FROM asl.active_queues " +
                "WHERE id = ?;"
        );
        int argNumber = 1;
        stmt.setObject(argNumber++, queueId);
        stmt.execute();
    }

    private NormalMessage popQueue(UUID queueId, UUID receiverId) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
                "SELECT * FROM popqueue(?, ?);"
        );
        int argNumber = 1;
        stmt.setObject(argNumber++, queueId);
        stmt.setObject(argNumber++, receiverId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next())
            return new NormalMessage(rs);
        else
            return null;
    }

    private NormalMessage peekQueue(UUID queueId, UUID receiverId) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
                "SELECT * FROM peekqueue(?, ?);"
        );
        int argNumber = 1;
        stmt.setObject(argNumber++, queueId);
        stmt.setObject(argNumber++, receiverId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next())
            return new NormalMessage(rs);
        else
            return null;
    }

    private NormalMessage popFromSender(UUID queueId, UUID senderId, UUID receiverId) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
                "DELETE FROM asl.message " +
                        "WHERE messageid = any(array(" +
                        "SELECT messageid " +
                        "FROM  asl.message " +
                        "WHERE queueid = ? AND sennderid = ? AND (receiverid IS NULL OR receiverid = ?) " +
                        "LIMIT 1)) " +
                        "RETURNING *;"
        );
        int argNumber = 1;
        stmt.setObject(argNumber++, queueId);
        stmt.setObject(argNumber++, senderId);
        stmt.setObject(argNumber++, receiverId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next())
            return new NormalMessage(rs);
        else
            return null;
    }

    private NormalMessage peekFromSender(UUID queueId, UUID senderId, UUID receiverId) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
                "SELECT * " +
                        "FROM  asl.message " +
                        "WHERE queueid = ? AND senderid = ? AND (receiverid IS NULL OR receiverid = ?) " +
                        "LIMIT 1;"
        );
        int argNumber = 1;
        stmt.setObject(argNumber++, queueId);
        stmt.setObject(argNumber++, senderId);
        stmt.setObject(argNumber++, receiverId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next())
            return new NormalMessage(rs);
        else
            return null;
    }

    private Collection<UUID> fetchReadyQueues(UUID receiverId) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
                "SELECT DISTINCT queueid " +
                "FROM asl.message " +
                "WHERE receiverid IS NULL OR receiverid = ?;"
        );
        int argNumber = 1;
        stmt.setObject(argNumber++, receiverId);
        ResultSet rs = stmt.executeQuery();
        ArrayList<UUID> queues = new ArrayList<>();
        while (rs.next())
            queues.add((UUID) rs.getObject(1));
        return queues;
    }

    void addMessage(NormalMessage msg) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
                "INSERT INTO asl.message " +
                "(messageid, senderid, receiverid, queueid, timeofarrival, body) " +
                "VALUES (?, ?, ?, ?, ?, ?);"
        );
        int argNumber = 1;
        stmt.setObject(argNumber++, msg.getMessageId());
        stmt.setObject(argNumber++, msg.getSenderId());
        stmt.setObject(argNumber++, msg.getReceiverId());
        stmt.setObject(argNumber++, msg.getQueueId());
        stmt.setTimestamp(argNumber++, msg.getTimeOfArrival());
        stmt.setString(argNumber++, msg.getBody());
        stmt.execute();
    }

}
