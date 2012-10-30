package archipelago.compute;

import archipelago.data.DataChunk;
import java.io.Serializable;

public class ProcessManager<S, T> implements Runnable, Serializable
{
    
    private DataChunk<T> inputChunk;
    private DataChunk<S> outputChunk;
    private final ChunkProcessor<S, T> processor;
    
    public ProcessManager(DataChunk<T> tIn, ChunkProcessor<S, T> processorIn)
    {
        inputChunk = tIn;
        processor = processorIn;
        outputChunk = null;
    }
    
    public void run()
    {
        outputChunk = processor.process(inputChunk);
    }

    public DataChunk<S> getOutput()
    {
        return outputChunk;
    }

    public long getID()
    {
        return inputChunk.getID();
    }

    
}
