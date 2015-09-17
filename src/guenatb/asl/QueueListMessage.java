package guenatb.asl;

import java.util.Collection;
import java.util.UUID;

/**
 * Created by Balz Guenat on 17.09.2015.
 */
public class QueueListMessage extends AbstractMessage {

    UUID receiverId;
    Collection<UUID> queueList;

    QueueListMessage(Collection<UUID> aqueueList) {
        queueList = aqueueList;
    }

    public Collection<UUID> getQueueList() {
        return queueList;
    }
}
