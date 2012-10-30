package archipelago.compute;

import archipelago.data.DataChunk;

public interface ChunkProcessor<S, T>
{
    
    public DataChunk<S> process(DataChunk<T> chunk);
    
}
