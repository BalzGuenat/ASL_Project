package guenatb.asl;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Created by Balz Guenat on 17.09.2015.
 */
public class NormalMessage extends AbstractMessage {

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

}
