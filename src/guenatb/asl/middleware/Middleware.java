package guenatb.asl.middleware;

import guenatb.asl.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * Created by Balz Guenat on 17.09.2015.
 */
public class Middleware {

    static final Logger log = Logger.getLogger(Middleware.class.getName());
    private static final int MAX_ACCEPT_ATTEMPTS = 3;

    Connection dbConnection;
    ServerSocket serverSocket;

    Middleware() throws SQLException, IOException {
        dbConnection = DriverManager.getConnection(
                GlobalConfig.DB_URL,
                GlobalConfig.DB_USER,
                GlobalConfig.DB_PASSWORD);
        serverSocket = new ServerSocket(GlobalConfig.FIRST_MIDDLEWARE_PORT);
    }

    public static void main(String[] args) {
        try {
            Middleware mw = new Middleware();
            mw.listen();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    private void listen() {
        int failed = 0;
        while (failed < MAX_ACCEPT_ATTEMPTS) {
            try {
                Socket clientSocket = serverSocket.accept();
                AbstractMessage msg = AbstractMessage.fromStream(clientSocket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
                AbstractMessage response = handleMessage(msg);
                oos.writeObject(response);
                clientSocket.close();
                failed = 0;
            } catch (IOException | CommunicationException e) {
                failed++;
                log.error("Could not accept server socket.", e);
            }
        }
        log.error("Failed 10 times in a row, stop.");
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
                        NormalMessage response = popQueue(cmsg.firstArg);
                        if (response != null)
                            return response;
                        else
                            return ConnectionEndMessage.SUCCESS;
                    case PEEK_QUEUE:
                        response = peekQueue(cmsg.firstArg);
                        if (response != null)
                            return response;
                        else
                            return ConnectionEndMessage.SUCCESS;
                    case POP_FROM_SENDER:
                        return popFromSender(cmsg.firstArg, cmsg.secondArg);
                    case PEEK_FROM_SENDER:
                        return peekFromSender(cmsg.firstArg, cmsg.secondArg);
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

    private NormalMessage popQueue(UUID queueId) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
                "DELETE FROM asl.message " +
                        "WHERE messageid = any(array(" +
                        "SELECT messageid " +
                        "FROM  asl.message " +
                        "WHERE queueid = ? " +
                        "LIMIT 1)) " +
                        "RETURNING *;"
        );
        int argNumber = 1;
        stmt.setObject(argNumber++, queueId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next())
            return new NormalMessage(rs);
        else
            return null;
    }

    private NormalMessage peekQueue(UUID queueId) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
                "SELECT * " +
                        "FROM  asl.message " +
                        "WHERE queueid = ? " +
                        "LIMIT 1;"
        );
        int argNumber = 1;
        stmt.setObject(argNumber++, queueId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next())
            return new NormalMessage(rs);
        else
            return null;
    }

    private NormalMessage popFromSender(UUID queueId, UUID senderId) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
                "DELETE FROM asl.message " +
                        "WHERE messageid = any(array(" +
                        "SELECT messageid " +
                        "FROM  asl.message " +
                        "WHERE queueid = ? AND sennderid = ? " +
                        "LIMIT 1)) " +
                        "RETURNING *;"
        );
        int argNumber = 1;
        stmt.setObject(argNumber++, queueId);
        stmt.setObject(argNumber++, senderId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next())
            return new NormalMessage(rs);
        else
            return null;
    }

    private NormalMessage peekFromSender(UUID queueId, UUID senderId) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
                "SELECT * " +
                        "FROM  asl.message " +
                        "WHERE queueid = ? AND senderid = ? " +
                        "LIMIT 1;"
        );
        int argNumber = 1;
        stmt.setObject(argNumber++, queueId);
        stmt.setObject(argNumber++, senderId);
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
