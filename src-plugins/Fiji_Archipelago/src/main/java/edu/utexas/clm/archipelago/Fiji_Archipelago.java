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

import edu.utexas.clm.archipelago.network.client.ArchipelagoClient;
import edu.utexas.clm.archipelago.ui.ClusterUI;
import edu.utexas.clm.archipelago.util.IJLogger;
import edu.utexas.clm.archipelago.util.IJPopupLogger;
import edu.utexas.clm.archipelago.util.PrintStreamLogger;
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
        else if (arg.equals("client"))
        {
            FijiArchipelago.runClientGUI();
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

        InputStream is;
        OutputStream os;

        ArchipelagoClient client;
        long id = -1;

        File logFile;
        PrintStream filePrinter;
        Socket s = null;

        FijiArchipelago.log("Main called at " + new Date());

        switch (args.length)
        {
            case 3:
                id = Long.parseLong(args[2]);
            case 2:
                s = new Socket(args[0], Integer.parseInt(args[1]));
                is = s.getInputStream();
                os = s.getOutputStream();

                break;
            case 1:
                id = Long.parseLong(args[0]);
                is = System.in;
                os = System.out;

                break;
            default:
                System.err.println("Usage: Fiji_Archipelago [host port] [ID]");
                return;
        }


        logFile = new File(System.getProperty("user.home") + "/cluster_" + id + ".log");
        filePrinter = new PrintStream(new FileOutputStream(logFile));

        System.setOut(filePrinter);
        System.setErr(filePrinter);

        FijiArchipelago.setDebugLogger(new PrintStreamLogger(filePrinter));
        FijiArchipelago.setErrorLogger(new PrintStreamLogger(filePrinter));
        FijiArchipelago.setInfoLogger(new PrintStreamLogger(filePrinter));
        
        FijiArchipelago.log(args.length == 1 ? "Using System.in/out" : "Using socket");
        
        client = new ArchipelagoClient(id, is, os);

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
}
