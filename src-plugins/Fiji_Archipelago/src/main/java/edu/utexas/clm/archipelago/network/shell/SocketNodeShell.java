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

package edu.utexas.clm.archipelago.network.shell;

import com.jcraft.jsch.JSchException;
import edu.utexas.clm.archipelago.Cluster;
import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.exception.ShellExecutionException;
import edu.utexas.clm.archipelago.listen.NodeShellListener;
import edu.utexas.clm.archipelago.network.node.NodeManager;
import edu.utexas.clm.archipelago.network.server.ArchipelagoServer;
import edu.utexas.clm.archipelago.network.shell.ssh.JSchUtility;

public class SocketNodeShell extends SSHNodeShell
{
    private static final SocketNodeShell shell = new SocketNodeShell();

    protected String getArguments(final NodeManager.NodeParameters param,
                                  final NodeShellListener listener)
    {
        final String eroot = param.getExecRoot();
        final ArchipelagoServer server = ArchipelagoServer.getServer(listener);
        return "--allow-multiple --full-classpath " +
                " --main-class edu.utexas.clm.archipelago.Fiji_Archipelago " +
                Cluster.getCluster().getLocalHostName() + " " + server.getPort() + " " + param.getID() +
                " 2>&1 > ~/" + param.getHost() + "_" + param.getID() + ".log";    
    }

    public boolean startShell(final NodeManager.NodeParameters param, final NodeShellListener listener)
            throws ShellExecutionException
    {
        FijiArchipelago.debug("Starting Socket shell on " + param.getHost());
        try
        {
            final String execFile = param.getExecRoot() + "/" +
                    param.getShellParams().getString("executable");
            if (JSchUtility.fileExists(param, execFile))
            {
                final String command = execFile + " " + getArguments(param, listener);
                new JSchUtility(param, listener, command).start();
                return true;
            }
            else
            {
                return false;
            }
        }
        catch (JSchException jse)
        {
            handleJSE(jse, param);
            return false;
        }
        catch (Exception e)
        {
            throw new ShellExecutionException(e);
        }
    }
    
    public String name()
    {
        return "Insecure Socket Shell";
    }
    
    public String description()
    {
        return "Uses ssh to cause remote nodes to connect via an insecure socket";
    }

    public static SocketNodeShell getShell()
    {
        return shell;
    }
    
}
