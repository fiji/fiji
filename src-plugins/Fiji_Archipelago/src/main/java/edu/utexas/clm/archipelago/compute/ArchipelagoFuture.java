/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * 
 * @author Larry Lindsey llindsey@clm.utexas.edu
 */

/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 *
 * @author Larry Lindsey llindsey@clm.utexas.edu
 */

package edu.utexas.clm.archipelago.compute;

import edu.utexas.clm.archipelago.Cluster;
import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.network.node.ClusterNode;

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
    private Throwable e;
    private ClusterNode ranOnNode = null;
   
    
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

    public boolean finish(final Throwable exception)
    {
        threadLock.lock();
        if (!finished.getAndSet(true))
        {
            e = exception;
            done.set(true);
            smoochThreads();
            threadLock.unlock();
            return true;
        }
        else
        {
            threadLock.unlock();
            return false;
        }

    }

    public boolean finish(ProcessManager<?> pm) throws ClassCastException
    {
        threadLock.lock();
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
                ranOnNode = scheduler.getNode(pm.getRunningOn());
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
            threadLock.unlock();
            return true;
        }
        else
        {
            threadLock.unlock();
            return false;
        }
    }

    private synchronized void smoochThreads()
    {
        /*
        This function assumes that threadLock is locked when it is called.
         */
        assert threadLock.isLocked();
        for (Thread t: waitingThreads)
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
        threadLock.lock();
        if (done.get())
        {
            FijiArchipelago.debug("Job " + getID() + ": Cancel called, but job is already done");
            threadLock.unlock();
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
                finished.set(true);
                smoochThreads();
            }
            else
            {
                FijiArchipelago.debug("Job " + getID() + " was NOT CANCELLED");
            }
            wasCancelled.set(cancelled);
            threadLock.unlock();
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
        FijiArchipelago.debug("Future: " + getID() + " get() called, acquiring lock...");
        threadLock.lock();

        if (!done.get())
        {
            final Thread currThread = Thread.currentThread();

            FijiArchipelago.debug("Future: " + getID() + " not done, adding Thread to queue");

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
        else
        {
            threadLock.unlock();
        }

        if (e != null)
        {
            throw new ExecutionException("On host " +
                    (ranOnNode == null ? "null" : ranOnNode.getHost()), e);
        }

        return t;
    }
}

