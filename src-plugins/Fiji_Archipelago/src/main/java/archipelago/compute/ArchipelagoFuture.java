package archipelago.compute;

import archipelago.Cluster;
import archipelago.FijiArchipelago;

import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;


public class ArchipelagoFuture<T> implements Future<T>
{
    private T t;
    private final long id;
    private final Cluster.ProcessScheduler scheduler;
    private final AtomicBoolean wasCancelled, done;    
    private final Vector<Thread> waitingThreads;
    private Exception e;
   
    
    public ArchipelagoFuture(Cluster.ProcessScheduler scheduler)
    {
        this(scheduler, null);
    }
    
    public ArchipelagoFuture(Cluster.ProcessScheduler s, T inData)
    {
        id = FijiArchipelago.getUniqueID();
        t = inData;
        scheduler = s;
        waitingThreads = new Vector<Thread>();
        wasCancelled = new AtomicBoolean(false);
        done = new AtomicBoolean(false); 
    }

    // FijiArchipelago-specific methods

    public long getID()
    {
        return id;
    }
    
    public void finish(ProcessManager<?> pm) throws ClassCastException
    {
        if (pm != null)
        {
            final Object o = pm.getOutput();
            T result = (T)o;

            if (result != null)
            {
                t = result;
            }
            e = pm.getRemoteException();
            done.set(true);
            // MUST set done to true before interrupting threads, or we'll get a bunch of
            // InterruptedExceptions on get()
            smoochThreads();
        }
        else
        {
            t = null;
        }
    }

    private synchronized void smoochThreads()
    {
        for (Thread t: waitingThreads)   //TODO use a lock.
        {
            t.interrupt();
        }
    }

    // Future-only methods.

    /**
     * Cancels execution of this thread, optionally continuing execution if the associated
     * Callable is already running. In the case that a running Callable is cancelled, any
     * waiting get() methods will throw an InterruptedException
     * @param b if true, the job associated with this Future will be cancelled even if it is
     *          currently executing, if false, this call to cancel() will be ignored.
     * @return true if the job was cancelled, false otherwise.
     */
    public synchronized boolean cancel(boolean b)
    {
        if (done.get())
        {
            FijiArchipelago.debug("Job " + getID() + ": Cancel called, but job is already done");
            return false;
        }
        else
        {
            FijiArchipelago.debug("Job " + getID() + ": Cancel called.");
            boolean cancelled = scheduler.cancelJob(id, b);
            done.set(cancelled);
            if (cancelled)
            {
                FijiArchipelago.debug("Job " + getID() + " cancel SUCCESS");
                smoochThreads();
            }
            else
            {
                FijiArchipelago.debug("Job " + getID() + " was NOT CANCELLED");
            }
            wasCancelled.set(cancelled);
            return cancelled;
        }
    }

    public boolean isCancelled() {
        return wasCancelled.get();
    }

    public boolean isDone() {
        return done.get();
    }

    public T get() throws InterruptedException, ExecutionException
    {
        try
        {
            return get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException te)
        {
            throw new ExecutionException(te);
        }
    }

    public T get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException
    {
        if (!done.get())
        {
            final Thread currThread = Thread.currentThread();
            waitingThreads.add(currThread);
            try
            {
                Thread.sleep(timeUnit.convert(l, TimeUnit.MILLISECONDS));
                waitingThreads.remove(currThread);
                throw new TimeoutException();
            }
            catch (InterruptedException ie)
            {
                waitingThreads.remove(currThread);
                if (!done.get())
                {
                    throw ie;
                }
            }

            if (e != null)
            {
                throw new ExecutionException(e);
            }
        }
        return t;
    }
}

