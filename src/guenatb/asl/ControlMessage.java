package guenatb.asl;

import java.util.UUID;

/**
 * Created by Balz Guenat on 17.09.2015.
 */
public class ControlMessage extends AbstractMessage {

    public final ControlType type;

    public final UUID firstArg;
    public final UUID secondArg;

    /**
     * @param asenderId
     * @param atype     This indicates what kind of control message this is.
     * @param args      If type is GET_READY_QUEUES then args should be empty.
     *                  If type is GET_FROM_SENDER then args should contain first the id of the queried
     *                  queue and then the id of the requested sender.
     *                  Otherwise args should only contain the id of the queried queue.
     */
    public ControlMessage(UUID asenderId, ControlType atype, UUID... args) {
        messageId = UUID.randomUUID();
        senderId = asenderId;
        type = atype;
        switch (type) {
            case CREATE_QUEUE:
            case DELETE_QUEUE:
            case POP_QUEUE:
            case PEEK_QUEUE:
                if (args.length != 1)
                    throw new RuntimeException("Invalid number or arguments for ControlMessage type.");
                firstArg = args[0];
                secondArg = null;
                break;
            case GET_FROM_SENDER:
                if (args.length != 2)
                    throw new RuntimeException("Invalid number or arguments for ControlMessage type.");
                firstArg = args[0];
                secondArg = args[1];
                break;
            case GET_READY_QUEUES:
                if (args.length != 0)
                    throw new RuntimeException("Invalid number or arguments for ControlMessage type.");
                firstArg = null;
                secondArg = null;
                break;
            default:
                throw new RuntimeException("Invalid type argument for ControlMessage constructor.");
        }
    }

    public enum ControlType {
        CREATE_QUEUE,
        DELETE_QUEUE,
        POP_QUEUE,
        PEEK_QUEUE,
        GET_FROM_SENDER,
        GET_READY_QUEUES
    }

}
