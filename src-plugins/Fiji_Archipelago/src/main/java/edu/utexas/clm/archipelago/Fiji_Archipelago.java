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

package edu.utexas.clm.archipelago;

import edu.utexas.clm.archipelago.listen.MessageType;
import edu.utexas.clm.archipelago.network.MessageXC;
import edu.utexas.clm.archipelago.network.client.ArchipelagoClient;
import edu.utexas.clm.archipelago.ui.ClusterUI;
import edu.utexas.clm.archipelago.util.IJLogger;
import edu.utexas.clm.archipelago.util.IJPopupLogger;
import edu.utexas.clm.archipelago.util.PrintStreamLogger;
import edu.utexas.clm.archipelago.util.XCErrorAdapter;
import ij.plugin.PlugIn;

import java.io.EOFException;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.Socket;

/**
 *
 * @author Larry Lindsey
 */
public class Fiji_Archipelago implements PlugIn 
{

    public void run(String arg)
    {

        if (arg.equals("gui"))
        {
            FijiArchipelago.setDebugLogger(new PrintStreamLogger());
            FijiArchipelago.setInfoLogger(new IJLogger());
            FijiArchipelago.setErrorLogger(new IJPopupLogger());
            new ClusterUI();
        }
        else if (!arg.equals(""))
        {
            FijiArchipelago.runClusterGUI(arg);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException
    {
        /*
        main should only be called on client nodes. This sets up a socket connection with the
        cluster server, whose information is passed into args[] at the command line.
        */
        System.out.println("Fiji Archipelago main called");
        
        
        if (args.length == 3)
        {
            Socket s;
            ArchipelagoClient client;
            String host = args[0];

            XCErrorAdapter xcEListener = new XCErrorAdapter()
            {
                protected boolean handleCustomRX(final Throwable t, final MessageXC xc)
                {
                    if (t instanceof ClassCastException)
                    {
                        reportRX(t, t.toString(), xc);
                        xc.queueMessage(MessageType.ERROR, t);
                        return false;
                    }
                    else if (t instanceof EOFException)
                    {
                        reportRX(t, "Received EOF", xc);
                        xc.close();
                        return false;
                    }
                    else if (t instanceof StreamCorruptedException)
                    {
                        reportRX(t, "Stream corrupted: " + t, xc);
                        xc.close();
                        return false;
                    }
                    else
                    {
                        xc.queueMessage(MessageType.ERROR, t);
                        return true;
                    }
                }
                
                protected boolean handleCustomTX(final Throwable t, final MessageXC xc)
                {
                    if (t instanceof IOException)
                    {
                        reportTX(t, t.toString(), xc);
                        xc.close();                        
                    }
                    else
                    {
                        xc.queueMessage(MessageType.ERROR, t);
                    }
                    return true;
                }
            };

            int port = Integer.parseInt(args[1]);
            long id = Long.parseLong(args[2]);

            FijiArchipelago.setDebugLogger(new PrintStreamLogger());
            FijiArchipelago.setErrorLogger(new PrintStreamLogger());
            FijiArchipelago.setInfoLogger(new PrintStreamLogger());

            s = new Socket(host, port);
            
            client = new ArchipelagoClient(id, host, s.getInputStream(), s.getOutputStream(), xcEListener);
            
            while (client.isActive())
            {
                Thread.sleep(1000);
            }
            
            s.close();
        }
        else
        {
            System.err.println("Usage: Fiji_Archipelago host port ID");
        }


    }
}
