package archipelago.listen;


import archipelago.network.MessageXC;

public interface TransceiverExceptionListener
{
    public void handleRXThrowable(final Throwable t, final MessageXC mxc);

    public void handleTXThrowable(final Throwable t, final MessageXC mxc);
}
