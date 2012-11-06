package archipelago.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

public class SimpleChunk<T> extends DataChunk<T> implements Serializable 
{
    private final T t;
    
    public SimpleChunk(final T inT)
    {
        t = inT;
    }
    
    @Override
    public long getID() {
        return 0;
    }

    @Override
    public T getData() {
        return t;
    }

    public Iterator<DataChunk<T>> iterator() {
        return new ArrayList<DataChunk<T>>().iterator();
    }
}
