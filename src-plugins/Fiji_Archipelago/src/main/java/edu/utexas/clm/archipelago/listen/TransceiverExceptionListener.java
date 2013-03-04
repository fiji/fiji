package edu.utexas.clm.archipelago.listen;


import edu.utexas.clm.archipelago.network.MessageXC;

public interface TransceiverExceptionListener
{
    public void handleRXThrowable(final Throwable t, final MessageXC mxc);

    public void handleTXThrowable(final Throwable t, final MessageXC mxc);
}
