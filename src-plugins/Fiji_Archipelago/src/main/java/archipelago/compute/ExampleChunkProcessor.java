package archipelago.compute;


import archipelago.data.DataChunk;
import archipelago.data.SimpleChunk;

/**
 * A very simple ChunkProcessor
 *
 *
 * @author Larry Lindsey
 */
public class ExampleChunkProcessor implements ChunkProcessor<Integer, Integer>
{
    /**
     * Increments the integer stored by dataChunk.
     * @param dataChunk an Integer DataChunk
     * @return an Integer DataChunk storing a value that is one more than what was passed in.
     */
    public DataChunk<Integer> process(DataChunk<Integer> dataChunk)
    {
        return new SimpleChunk<Integer>(dataChunk.getData() + 1);
    }
}
