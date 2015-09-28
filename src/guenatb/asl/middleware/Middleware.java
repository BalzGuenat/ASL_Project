package guenatb.asl.middleware;

import guenatb.asl.AbstractMessage;
import guenatb.asl.ControlMessage;
import guenatb.asl.GlobalConfig;
import guenatb.asl.NormalMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.UUID;

/**
 * Created by Balz Guenat on 17.09.2015.
 */
public class Middleware {

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
        while (failed < 10) {
            try {
                Socket clientSocket = serverSocket.accept();
                AbstractMessage msg = AbstractMessage.fromStream(clientSocket.getInputStream());
                handleMessage(msg);
                failed = 0;
            } catch (IOException e) {
                failed++;
                System.err.println("Could not accept server socket.");
                e.printStackTrace();
            }
        }
        System.err.println("Failed 10 times in a row, stop.");
    }

    private void handleMessage(AbstractMessage msg) {
        try {
            if (msg instanceof ControlMessage) {
                ControlMessage cmsg = (ControlMessage) msg;
                switch (cmsg.type) {
                    case CREATE_QUEUE:
                        createQueue(cmsg.firstArg);
                        return;
                    case DELETE_QUEUE:
                        deleteQueue(cmsg.firstArg);
                        return;
                    case POP_QUEUE:
                        popQueue(cmsg.firstArg);
                        return;
                    case PEEK_QUEUE:
                        peekQueue(cmsg.firstArg);
                        return;
                    case GET_FROM_SENDER:
                        fetchMessagesFromSender(cmsg.firstArg, cmsg.secondArg);
                        return;
                    case GET_READY_QUEUES:
                        fetchReadyQueues(cmsg.getSenderId());
                        return;
                    default:
                        throw new RuntimeException("Unknown ControlMessage type.");
                }
            } else if (msg instanceof NormalMessage) {
                NormalMessage nmsg = (NormalMessage) msg;
                Timestamp now = new Timestamp(new java.util.Date().getTime());
                nmsg.setTimeOfArrival(now);
                addMessage(nmsg);
                return;
            }
        } catch (SQLException e) {
            System.err.println("SQL error while handling message.");
            System.err.println(e.getMessage());
        }
    }

    private void createQueue(UUID queueId) {
        throw new RuntimeException("createQueue is not implemented yet.");
    }

    private void deleteQueue(UUID queueId) {
        throw new RuntimeException("deleteQueue is not implemented yet.");
    }

    private void popQueue(UUID queueId) {
        throw new RuntimeException("popQueue is not implemented yet.");
    }

    private void peekQueue(UUID queueId) {
        throw new RuntimeException("peekQueue is not implemented yet.");
    }

    private void fetchMessagesFromSender(UUID queueId, UUID senderId) {
        throw new RuntimeException("fetchMessageFromSender is not implemented yet.");
    }

    private void fetchReadyQueues(UUID receiverId) {
        throw new RuntimeException("fetchReadyQueues is not implemented yet.");
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
