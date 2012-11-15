package archipelago.data;


import archipelago.FijiArchipelago;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class FileChunk extends DataChunk<String>
{
    private final String fileName;

    public FileChunk(String path)
    {
        super();
        File f = new File(path);
        fileName = FijiArchipelago.truncateFileRoot(f.getAbsolutePath());
    }
    
    public FileChunk(String path, DataChunk oldChunk)
    {
        super(oldChunk);
        File f = new File(path);
        fileName = FijiArchipelago.truncateFileRoot(f.getAbsolutePath());
    }
    
    public String getData()
    {
        return FijiArchipelago.getFileRoot() + fileName;
    }

    public Iterator<DataChunk<String>> iterator()
    {
        // Return a correctly-typed iterator with nothing in it.
        return new ArrayList<DataChunk<String>>().iterator();
    }
    
    public String toString()
    {
        return getData();
    }
}
