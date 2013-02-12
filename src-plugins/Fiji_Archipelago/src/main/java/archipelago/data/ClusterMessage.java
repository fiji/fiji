package archipelago.data;

import archipelago.listen.MessageType;

import java.io.Serializable;
/**
 *
 * @author Larry Lindsey
 */
public class ClusterMessage implements Serializable
{

    public MessageType type;
    public Serializable o = null;

    public ClusterMessage(final MessageType type)
    {
        this.type = type;
    }
    
    public String toString()
    {
        return typeToString(type);
    }
    
    public static String typeToString(final MessageType type)
    {
        switch (type)
        {
            case BEAT:
                return "beat";
            case SETID:
                return "set id";
            case GETID:
                return "get id";
            case PING:
                return "ping";
            case HALT:
                return "halt";
            case PROCESS:
                return "process";
            case USER:
                return "user";
            case SETFILEROOT:
                return "set file root";
            case GETFILEROOT:
                return "get file root";
            case SETEXECROOT:
                return "set exec root";
            case GETEXECROOT:
                return "get exec root";
            case CANCELJOB:
                return "cancel job";
            case ERROR:
                return "error";
            case NUMTHREADS:
                return "num threads";
            case MBRAM:
                return "MB ram";
            default:
                return "unknown";
        }
    }

}
