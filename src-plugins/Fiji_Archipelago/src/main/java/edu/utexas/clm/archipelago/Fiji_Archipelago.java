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

import java.io.*;
import java.net.Socket;
import java.util.Date;

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
        //System.out.println("Fiji Archipelago main called");
        
        
        if (args.length == 3 || args.length == 1)
        {
            final boolean useSocket = args.length == 3;
            InputStream is;
            OutputStream os;
            
            Socket s = null;
            ArchipelagoClient client;

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

            long id = useSocket ? Long.parseLong(args[2]) : Long.parseLong(args[0]);
            final File logFile = new File(System.getProperty("user.home") + "/cluster_" + id + ".log");
            final PrintStream filePrinter = new PrintStream(new FileOutputStream(logFile));

            FijiArchipelago.setDebugLogger(new PrintStreamLogger(filePrinter));
            FijiArchipelago.setErrorLogger(new PrintStreamLogger(filePrinter));
            FijiArchipelago.setInfoLogger(new PrintStreamLogger(filePrinter));

            FijiArchipelago.log("Main called at " + new Date());
            
            if (useSocket)
            {
                FijiArchipelago.log("Using socket");
                s = new Socket(args[0], Integer.parseInt(args[1]));
                is = s.getInputStream();
                os = s.getOutputStream();
            }
            else
            {
                FijiArchipelago.log("Using System.in/out");
                is = System.in;
                os = System.out;
                System.setOut(filePrinter);
            }
            
            client = new ArchipelagoClient(id, is, os, xcEListener);
            
            while (client.isActive())
            {
                Thread.sleep(1000);
            }
            
            FijiArchipelago.log("Client is inactive, closing...");
            
            if (s != null)
            {
                s.close();
            }
            
            filePrinter.close();
        }
        else
        {
            System.err.println("Usage: Fiji_Archipelago [host port] ID");
        }


    }
}
