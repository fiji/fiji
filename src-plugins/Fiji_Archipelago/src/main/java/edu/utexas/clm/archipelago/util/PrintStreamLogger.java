package edu.utexas.clm.archipelago.util;

import java.io.PrintStream;
/**
 *
 * @author Larry Lindsey
 */
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
