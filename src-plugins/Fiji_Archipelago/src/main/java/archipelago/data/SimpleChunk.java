package archipelago.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
/**
 *
 * @author Larry Lindsey
 */
public class SimpleChunk<T> extends DataChunk<T> implements Serializable 
{
    private final T t;
    
    public SimpleChunk(final T inT)
    {
        t = inT;
    }
    
    public SimpleChunk(final T inT, DataChunk oldChunk)
    {
        super(oldChunk);
        t = inT;
    }

    @Override
    public T getData() {
        return t;
    }

    public Iterator<DataChunk<T>> iterator() {
        return new ArrayList<DataChunk<T>>().iterator();
    }
    
    public String toString()
    {
        return t.toString();
    }
}
