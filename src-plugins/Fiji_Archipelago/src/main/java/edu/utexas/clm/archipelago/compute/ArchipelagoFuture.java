package edu.utexas.clm.archipelago.compute;

import edu.utexas.clm.archipelago.Cluster;
import edu.utexas.clm.archipelago.FijiArchipelago;

import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;


public class ArchipelagoFuture<T> implements Future<T>
{
    private T t;
    private final long id;
    private final Cluster.ProcessScheduler scheduler;
    private final AtomicBoolean wasCancelled, done, finished;    
    private final Vector<Thread> waitingThreads;
    private final ReentrantLock threadLock;
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
        finished = new AtomicBoolean(false);
        threadLock = new ReentrantLock();
    }

    // FijiArchipelago-specific methods

    public long getID()
    {
        return id;
    }
    
    public void setException(final Exception eIn)
    {
        e = eIn;
    }
    
    public boolean finish(ProcessManager<?> pm) throws ClassCastException
    {
        if (!finished.getAndSet(true))
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
            return true;
        }
        else
        {
            return false;
        }
    }

    private synchronized void smoochThreads()
    {
        threadLock.lock();
        for (Thread t: waitingThreads)
        {
            t.interrupt();
        }
        threadLock.unlock();
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
    
    private void removeWaitingThread(final Thread t)
    {
        threadLock.lock();
        waitingThreads.remove(t);
        threadLock.unlock();
    }

    public T get(final long l, final TimeUnit timeUnit) throws InterruptedException,
            ExecutionException, TimeoutException
    {
        FijiArchipelago.debug("Future: " + getID() + " get() called");
        if (!done.get())
        {
            FijiArchipelago.debug("Future: " + getID() + " not done");
            final Thread currThread = Thread.currentThread();

            FijiArchipelago.debug("Future: " + getID() + " acquiring lock...");
            threadLock.lock();
            FijiArchipelago.debug("Future: " + getID() + " lock acquired. Adding Thread to queue");
            waitingThreads.add(currThread);
            threadLock.unlock();
            try
            {
                FijiArchipelago.debug("Future: " + getID() + " sleeping...");
                Thread.sleep(timeUnit.convert(l, TimeUnit.MILLISECONDS));
                FijiArchipelago.debug("Future: " + getID() + " timed out");
                removeWaitingThread(currThread);                
                throw new TimeoutException();
            }
            catch (InterruptedException ie)
            {
                FijiArchipelago.debug("Future: " + getID() + " woken up");
                removeWaitingThread(currThread);
                if (!done.get())
                {
                    throw ie;
                }
            }
        }

        if (e != null)
        {
            throw new ExecutionException(e);
        }

        return t;
    }
}

