package archipelago.compute;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * ChunkProcessor interface
 *
 * @author Larry Lindsey
 */
public interface SerializableCallable<T> extends Serializable, Callable<T>
{


}
