package edu.utexas.clm.archipelago.network.translation;

import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.compute.ProcessManager;
import edu.utexas.clm.archipelago.listen.MessageType;
import edu.utexas.clm.archipelago.network.MessageXC;
import ij.IJ;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class PMAcknowledgingSender
{
    public static final long ACK_WAIT_TIME = 10000; // Wait 10 seconds for acknowledge.
    final MessageXC messageXC;
    final ProcessManager pm;
    final AtomicBoolean ack;

    public PMAcknowledgingSender(final MessageXC messageXC, final ProcessManager pm)
    {
        this.messageXC = messageXC;
        this.pm = pm;
        ack = new AtomicBoolean(false);
    }

    public long getID()
    {
        return pm.getID();
    }

    public boolean go()
    {
        if (messageXC.queueMessage(MessageType.PROCESS, pm))
        {
            new Thread()
            {
                public void run()
                {
                    try
                    {
                        while (!ack.get())
                        {
                            Thread.sleep(ACK_WAIT_TIME);
                            if (!ack.get())
                            {
                                FijiArchipelago.log("Waited " + ACK_WAIT_TIME +
                                        "ms for ack. Resending message");
                                messageXC.queueMessage(MessageType.PROCESS, pm);
                            }
                        }
                    }
                    catch (InterruptedException ie)
                    {
                        FijiArchipelago.log("Interrupted while waiting for ack for job " +
                                getID());
                    }
                    FijiArchipelago.debug("Ack sender: finished here for id " + getID());
                }
            }.start();

            return true;
        }
        else
        {
            return false;
        }
    }

    public void acknowledge()
    {
        FijiArchipelago.debug("Got ack for id " + getID());
        ack.set(true);
    }
}
