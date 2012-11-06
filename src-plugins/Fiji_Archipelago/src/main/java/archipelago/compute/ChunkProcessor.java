package archipelago.compute;

import archipelago.data.DataChunk;

import java.io.Serializable;

public interface ChunkProcessor<S, T> extends Serializable
{
    
    public DataChunk<S> process(DataChunk<T> chunk);
    
}
