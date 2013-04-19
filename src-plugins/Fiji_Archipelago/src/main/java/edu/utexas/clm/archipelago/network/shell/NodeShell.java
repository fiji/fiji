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

import edu.utexas.clm.archipelago.exception.ShellExecutionException;
import edu.utexas.clm.archipelago.network.node.NodeManager;
import edu.utexas.clm.archipelago.listen.NodeShellListener;

/**
 * Interface used to start remote nodes and potentially execute shell commands.
 *
 * NodeShell should be implemented as singleton classes - in other words, only
 * one of each class that implements this interface should need to be created.
 *
 * To register a NodeShell with Cluster, add a call to registerNodeShell
 * to the static block a the end of Cluster.java
 *
 * In the future, this may function similarly to Fiji's PlugIn or TrakEM2's TPlugIn.
 *
 */

public interface NodeShell
{
    /**
     * Start the ClusterNode given by the NodeParameters, by starting an ArchipelagoClient in a
     * Fiji or ImageJ instance on the remote node.
     * This method should handle the passed-in NodeShellListener, by passing it an InputStream and
     * OutputStream corresponding to the remote node when they become ready, and calling
     * execFinished when the remote connection (ie ssh shell) is closed.
     * @param param parameters used to connect to the machine in question
     * @param listener used to handle open IO streams and closing of the shell connection
     * @return true if started successfully.
     * @throws ShellExecutionException if an error occurs while attempting to
     */
    public boolean start(final NodeManager.NodeParameters param, final NodeShellListener listener)
            throws ShellExecutionException;
    
    public NodeShellParameters defaultParameters();
    
    public String paramToString(final NodeShellParameters nsp);
    
    public String name();
    
    public String description();
}
