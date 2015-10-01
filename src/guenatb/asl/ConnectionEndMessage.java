package guenatb.asl;

import java.util.Collection;
import java.util.UUID;

/**
 * Created by Balz Guenat on 17.09.2015.
 */
public class ConnectionEndMessage extends AbstractMessage {

    public static final ConnectionEndMessage SUCCESS = new ConnectionEndMessage(ConnectionEndType.SUCCESS);
    public static final ConnectionEndMessage ERROR = new ConnectionEndMessage(ConnectionEndType.ERROR);
    public static final ConnectionEndMessage INVALID_OPERATION = new ConnectionEndMessage(ConnectionEndType.INVALID_OPERATION);

    public final ConnectionEndType type;

    private ConnectionEndMessage(ConnectionEndType type) {
        this.type = type;
    }

    public enum ConnectionEndType {
        SUCCESS,
        ERROR,
        INVALID_OPERATION
    }

}


