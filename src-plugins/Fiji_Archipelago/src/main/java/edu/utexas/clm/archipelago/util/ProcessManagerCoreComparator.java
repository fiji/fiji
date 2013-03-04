package edu.utexas.clm.archipelago.util;

import edu.utexas.clm.archipelago.compute.ProcessManager;

import java.util.Comparator;


public class ProcessManagerCoreComparator implements Comparator<ProcessManager>
{
    int threadCount = 128;
    
    public void setThreadCount(final int count)
    {
        threadCount = count;
    }

    public int compare(ProcessManager pm1, ProcessManager pm2) {
        final int t1 = pm1.requestedCores(threadCount);
        final int t2 = pm2.requestedCores(threadCount);
        if (t1 == t2)
        {
            final long id1 = pm1.getID();
            final long id2 = pm2.getID();
            return id1 > id2 ? 1 : id1 == id2 ? 0 : -1;
        }
        else 
        {
            return t1 > t2 ? -1 : 1;
        }
    }
}
