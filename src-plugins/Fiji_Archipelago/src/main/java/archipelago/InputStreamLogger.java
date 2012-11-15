package archipelago;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 *
 * @author Larry Lindsey
 */
public class InputStreamLogger extends Thread
{
    
    public InputStream inputStream;
    public final AtomicBoolean running;
    public EasyLogger logger;
    
    public InputStreamLogger(final EasyLogger el)
    {
        inputStream = null;
        logger = el;
        running = new AtomicBoolean(true);
    }
    
    public void setStream(InputStream is)
    {
        inputStream = is;
    }
    
    public void run()
    {
        if (inputStream != null)
        {
        BufferedReader bufferedIn = new BufferedReader(new InputStreamReader(inputStream));

            while (running.get())
            {
                try
                {
                    logger.log(bufferedIn.readLine());
                }
                catch (IOException ioe)
                {
                    logger.log("Got IOException while reading stream: " + ioe);
                }
            }
        }
        else
        {
            logger.log("Tried to run stream logger against null stream");
        }
    }
    
    public void close()
    {
        running.set(false);
    }
}
