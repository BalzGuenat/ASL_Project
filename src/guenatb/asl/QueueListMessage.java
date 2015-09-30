package guenatb.asl;

import java.util.Collection;
import java.util.UUID;

/**
 * Created by Balz Guenat on 17.09.2015.
 */
public class QueueListMessage extends AbstractMessage {

    UUID receiverId;
    Collection<UUID> queueList;

    public QueueListMessage(Collection<UUID> aqueueList, UUID receiverId) {
        this.receiverId = receiverId;
        queueList = aqueueList;
    }

    public Collection<UUID> getQueueList() {
        return queueList;
    }
}
