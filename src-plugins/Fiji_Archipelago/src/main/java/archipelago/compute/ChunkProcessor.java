package archipelago.compute;

import archipelago.data.DataChunk;

import java.io.Serializable;
/**
 * ChunkProcessor interface
 *
 * @author Larry Lindsey
 */
public interface ChunkProcessor<S, T> extends Serializable
{
    /**
     * Processes a chunk of type T into a chunk of type S.
     * @param chunk a chunk of type T
     * @return the result of processing chunk
     */
    public DataChunk<S> process(DataChunk<T> chunk);
    
}
