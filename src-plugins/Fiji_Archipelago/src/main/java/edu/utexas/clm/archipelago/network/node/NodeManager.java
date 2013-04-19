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

package edu.utexas.clm.archipelago.network.node;

import edu.utexas.clm.archipelago.Cluster;
import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.exception.ShellExecutionException;
import edu.utexas.clm.archipelago.listen.NodeShellListener;
import edu.utexas.clm.archipelago.network.shell.NodeShell;
import edu.utexas.clm.archipelago.network.shell.NodeShellParameters;
import edu.utexas.clm.archipelago.network.shell.SocketNodeShell;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 *
 * @author Larry Lindsey
 */
public class NodeManager
{

    public class NodeParameters
    {
        private ClusterNode node = null;
        private String host;
        private String user;

        private String fileRoot;
        private String execRoot;
        private final long id;
        private int numThreads;
        private NodeShell shell;
        private NodeShellParameters shellParams;
        
        private boolean editable;
        
        public NodeParameters(NodeParameters np)
        {
            id = FijiArchipelago.getUniqueID();
            user = np.getUser();
            host = np.getHost();
            shell = np.getShell();
            execRoot = np.getExecRoot();
            fileRoot = np.getFileRoot();
            numThreads = np.getThreadLimit();
            shellParams = shell.defaultParameters();
            editable = true;
        }

        public NodeParameters(String userIn,
                              String hostIn,
                              NodeShell shellIn,
                              String execPath,
                              String filePath)
        {
            user = userIn;
            host = hostIn;
            shell = shellIn;
            execRoot = execPath;
            fileRoot = filePath;
            id = FijiArchipelago.getUniqueID();
            shellParams = shellIn.defaultParameters();
            numThreads = 0;
        }

        public synchronized boolean startNode(NodeShellListener listener)
                throws ShellExecutionException
        {

            if (shell != null && shell.start(this, listener))
            {
                editable = false;
                return true;
            }
            else
            {
                return false;
            }
        }

        /**
         * Returns true if this NodeParameters is editable. This is a soft constraint. In other
         * words, NodeParameters does not enforce editability; it is up to the UI to enforce it.
         * @return true if this NodeParameters is editable.
         */
        public boolean isEditable()
        {
            return editable;
        }
        
        public synchronized void setThreadLimit(final int n)
        {
            numThreads = n;
        }

        public synchronized void setUser(final String user)
        {
            this.user = user;
        }

        public synchronized void setHost(final String host)
        {
            this.host = host;
        }
        
        public synchronized void setShell(final NodeShell shell, final NodeShellParameters params)
        {
            this.shell = shell;
            setShellParams(params);
        }

        public synchronized void setShell(final String className)
        {
            this.shell = Cluster.getNodeShell(className);
        }

        public synchronized void setNode(final ClusterNode node)
        {
            this.node = node;
        }

        public synchronized void setExecRoot(final String execRoot)
        {
            this.execRoot = execRoot;
        }

        public synchronized void setFileRoot(final String fileRoot)
        {
            this.fileRoot = fileRoot;
        }

        public synchronized void setShellParams(final NodeShellParameters shellParams)
        {
            this.shellParams = shellParams;
        }

        public String getUser()
        {
            return user;
        }

        public String getHost()
        {
            return host;
        }
        
        public NodeShell getShell()
        {
            return shell;
        }
        
        public NodeShellParameters getShellParams()
        {
            return shellParams;
        }

        public ClusterNode getNode()
        {
            return node;
        }

        public String getExecRoot()
        {
            return execRoot;
        }

        public String getFileRoot()
        {
            return fileRoot;
        }

        public long getID()
        {
            return id;
        }
        
        public int getThreadLimit()
        {
            return numThreads;
        }
        
        public String toString()
        {
            return user + "@" + host + " id: " + id + " " + shell.paramToString(shellParams);
        }
    }
    
    private final Hashtable<Long, NodeParameters> nodeTable;
    
    private final NodeParameters defaultParameters;

    public NodeManager()
    {
        NodeShell defaultShell = SocketNodeShell.getShell();
        defaultParameters = new NodeParameters("", "", defaultShell , "", "");
        nodeTable = new Hashtable<Long, NodeParameters>();
    }
    
    public synchronized NodeParameters getParam(final long id)
    {
        return nodeTable.get(id);
    }
    
    public NodeParameters getDefaultParameters()
    {
        return defaultParameters;
    }

    public ArrayList<NodeParameters> getParams()
    {
        return new ArrayList<NodeParameters>(nodeTable.values());
    }

    public synchronized void removeParam(final long id)
    {
        nodeTable.remove(id);
    }

    public void clear()
    {
        nodeTable.clear();
    }

    public NodeParameters newParam()
    {
        return newParam("");
    }

    public NodeParameters newParam(String hostIn)
    {
        NodeParameters param = new NodeParameters(defaultParameters);
        param.setHost(hostIn);

        nodeTable.put(param.id, param);

        FijiArchipelago.debug("Created new Param with id  " + param.id);

        return param;
    }

    public NodeParameters newParam(String userIn, String hostIn, NodeShell shell, String execPath, String filePath)
    {         
        NodeParameters param = newParam(hostIn);

        param.setUser(userIn);
        param.setShell(shell, shell.defaultParameters());
        param.setExecRoot(execPath);
        param.setFileRoot(filePath);
        return param;
    }




}
