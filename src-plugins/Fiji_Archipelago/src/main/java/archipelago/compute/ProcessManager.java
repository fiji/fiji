package archipelago.compute;

import archipelago.data.DataChunk;

import java.io.Serializable;
/**
 *
 * @author Larry Lindsey
 */
public class ProcessManager<S, T> implements Runnable, Serializable
{
    
    private DataChunk<T> inputChunk;
    private DataChunk<S> outputChunk;
    private final ChunkProcessor<S, T> processor;
    private transient ProcessListener listener = null;
    
    public ProcessManager(DataChunk<T> tIn, ChunkProcessor<S, T> processorIn, ProcessListener pl)
    {
        inputChunk = tIn;
        processor = processorIn;
        outputChunk = null;
        listener = pl;
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
    
    public ProcessListener getListener()
    {
        return listener;
    }

    
}
