package archipelago.listen;

/**
 * MessageType enumeration used for ClusterMessages. The terms get and set are from the
 * perspective of the root node.
 *
 * @author Larry Lindsey
 */
public enum MessageType
{
    SETID,
    GETID,
    PING,
    HALT,
    PROCESS,
    USER,
    SETFILEROOT,
    GETFILEROOT,
    SETEXECROOT,
    GETEXECROOT,
    CANCELJOB,
    ERROR,
    NUMTHREADS,
    MBRAM
}
