package guenatb.asl;

import java.util.UUID;

/**
 * Created by Balz Guenat on 17.09.2015.
 */
public class ControlMessage extends AbstractMessage {

    public final ControlType type;

    public final UUID senderId;
    public final UUID firstArg;
    public final UUID secondArg;

    /**
     * @param asenderId
     * @param atype     This indicates what kind of control message this is.
     * @param args      If type is GET_READY_QUEUES then args should be empty.
     *                  If type is POP_FROM_SENDER then args should contain first the id of the queried
     *                  queue and then the id of the requested sender.
     *                  Otherwise args should only contain the id of the queried queue.
     */
    public ControlMessage(UUID asenderId, ControlType atype, UUID... args) {
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
            case POP_FROM_SENDER:
            case PEEK_FROM_SENDER:
                if (args.length != 2)
                    throw new RuntimeException("Invalid number or arguments for ControlMessage type.");
                firstArg = args[0];
                secondArg = args[1];
                break;
            case GET_READY_QUEUES:
            case REGISTER_CLIENT:
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
        CREATE_QUEUE("CreateQueue"),
        DELETE_QUEUE("DeleteQueue"),
        POP_QUEUE("PopQueue"),
        PEEK_QUEUE("PeekQueue"),
        POP_FROM_SENDER("PopFromSender"),
        PEEK_FROM_SENDER("PeekFromSender"),
        GET_READY_QUEUES("GetReadyQueues"),
        REGISTER_CLIENT("RegisterClient");

        final String name;

        ControlType(String name) {
            this.name = name;
        }
    }

    @Override
    public String toString() {
        return String.format("ControlMessage:%s", type.name);
    }

}
