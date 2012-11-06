package archipelago.compute;


import archipelago.data.DataChunk;
import archipelago.data.SimpleChunk;

public class ExampleChunkProcessor implements ChunkProcessor<Integer, Integer>
{
    public DataChunk<Integer> process(DataChunk<Integer> dataChunk)
    {
        return new SimpleChunk<Integer>(dataChunk.getData() + 1);
    }
}
