package archipelago;

import java.io.PrintStream;

public class PrintStreamLogger implements EasyLogger
{
    
    private final PrintStream stream;
    
    public PrintStreamLogger()
    {
        this(System.out);
    }
    
    public PrintStreamLogger(PrintStream s)
    {
        stream = s;
    }
    
    public synchronized void log(final String msg)
    {
        stream.println(msg);
    }
}
