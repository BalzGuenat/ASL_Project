package guenatb.asl.middleware;

import guenatb.asl.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

/**
 * Created by Balz Guenat on 17.09.2015.
 */
public class Middleware {

    static final Logger log = Logger.getLogger(Middleware.class);
    private static final int MAX_ACCEPT_ATTEMPTS = 3;

    static List<Thread> workerThreads = new ArrayList<>(GlobalConfig.THREADS_PER_MIDDLEWARE);
    static ArrayBlockingQueue<Socket> connectionQueue = new ArrayBlockingQueue<>(GlobalConfig.QUEUE_CAPACITY);
    //static ConcurrentLinkedQueue<Socket> connectionQueue = new ConcurrentLinkedQueue<>();

    Connection dbConnection;

    Middleware() {
        try {
            dbConnection = DriverManager.getConnection(
                    GlobalConfig.DB_URL,
                    GlobalConfig.DB_USER,
                    GlobalConfig.DB_PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < GlobalConfig.THREADS_PER_MIDDLEWARE; i++) {
            Middleware mw = new Middleware();
            Thread t = new Thread(mw::doWork);
            workerThreads.add(t);
            t.start();
        }

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
            serverSocket = new ServerSocket(GlobalConfig.FIRST_MIDDLEWARE_PORT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
                try {
                    Socket clientSocket = connectionQueue.take();
                    ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
                    AbstractMessage msg = AbstractMessage.fromStream(ois);
                    Instant timeAccepted = Instant.now();
                    AbstractMessage response = handleMessage(msg);
                    ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
                    oos.writeObject(response);
                    clientSocket.close();
                    log.info("Total connection time: " + String.valueOf(timeAccepted.until(Instant.now(),
                            ChronoUnit.MILLIS) + "ms."));
                } catch (IOException | CommunicationException e) {
                    log.error("Error during connection.", e);
                }
            }
        } catch (InterruptedException e) {}
        log.info("Middleware worker thread shutting down after interrupt.");
    }

    private AbstractMessage handleMessage(AbstractMessage msg) {
        try {
            if (msg instanceof ControlMessage) {
                ControlMessage cmsg = (ControlMessage) msg;
                switch (cmsg.type) {
                    case CREATE_QUEUE:
                        createQueue(cmsg.firstArg);
                        return ConnectionEndMessage.SUCCESS;
                    case DELETE_QUEUE:
                        deleteQueue(cmsg.firstArg);
                        return ConnectionEndMessage.SUCCESS;
                    case POP_QUEUE:
                        NormalMessage response = popQueue(cmsg.firstArg, cmsg.senderId);
                        if (response != null)
                            return response;
                        else
                            return ConnectionEndMessage.SUCCESS;
                    case PEEK_QUEUE:
                        response = peekQueue(cmsg.firstArg, cmsg.senderId);
                        if (response != null)
                            return response;
                        else
                            return ConnectionEndMessage.SUCCESS;
                    case POP_FROM_SENDER:
                        return popFromSender(cmsg.firstArg, cmsg.secondArg, cmsg.senderId);
                    case PEEK_FROM_SENDER:
                        return peekFromSender(cmsg.firstArg, cmsg.secondArg, cmsg.senderId);
                    case GET_READY_QUEUES:
                        Collection<UUID> queues = fetchReadyQueues(cmsg.senderId);
                        return new QueueListMessage(queues, cmsg.senderId);
                    case REGISTER_CLIENT:
                        registerClient(cmsg.senderId);
                        return ConnectionEndMessage.SUCCESS;
                    default:
                        throw new RuntimeException("Unknown ControlMessage type.");
                }
            } else if (msg instanceof NormalMessage) {
                NormalMessage nmsg = (NormalMessage) msg;
                Timestamp now = new Timestamp(new java.util.Date().getTime());
                nmsg.setTimeOfArrival(now);
                addMessage(nmsg);
                return ConnectionEndMessage.SUCCESS;
            } else {
                throw new RuntimeException("Invalid message type received.");
            }
        } catch (SQLException e) {
            System.err.println("SQL error while handling message.");
            System.err.println(e.getMessage());
            return ConnectionEndMessage.ERROR;
        }
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
                "DELETE FROM asl.message " +
                        "WHERE messageid = any(array(" +
                        "SELECT messageid " +
                        "FROM  asl.message " +
                        "WHERE queueid = ? AND (receiverid IS NULL OR receiverid = ?) " +
                        "LIMIT 1)) " +
                        "RETURNING *;"
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
                "SELECT * " +
                        "FROM  asl.message " +
                        "WHERE queueid = ? AND (receiverid IS NULL OR receiverid = ?) " +
                        "LIMIT 1;"
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
