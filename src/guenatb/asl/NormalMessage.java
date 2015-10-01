package guenatb.asl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * Created by Balz Guenat on 17.09.2015.
 */
public class NormalMessage extends AbstractMessage {

    UUID messageId;
    UUID senderId;
    UUID receiverId;
    UUID queueId;

    Timestamp timeOfArrival;

    String body;

    public NormalMessage(UUID amessageId, UUID asenderId, UUID areceiverId,
                         UUID aqueueId, String amessageBody) {
        messageId = amessageId;
        senderId = asenderId;
        receiverId = areceiverId;
        queueId = aqueueId;
        body = amessageBody;
    }

    public NormalMessage(ResultSet rs) {
        try {
            messageId = (UUID) rs.getObject("messageid");
            senderId = (UUID) rs.getObject("senderid");
            receiverId = (UUID) rs.getObject("receiverid");
            queueId = (UUID) rs.getObject("queueid");
            timeOfArrival = rs.getTimestamp("timeofarrival");
            body = rs.getString("body");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not instantiate message from data row.");
        }
    }

    public UUID getMessageId() {
        return messageId;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public UUID getReceiverId() {
        return receiverId;
    }

    public UUID getQueueId() {
        return queueId;
    }

    public Timestamp getTimeOfArrival() {
        return timeOfArrival;
    }

    public void setTimeOfArrival(Timestamp timeOfArrival) {
        this.timeOfArrival = timeOfArrival;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return String.format("NormalMessage:%s", messageId.toString());
    }

}
