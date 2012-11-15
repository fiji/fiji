package archipelago;

import archipelago.network.ClusterNode;
import archipelago.network.shell.JSchNodeShell;
import archipelago.network.shell.NodeShell;

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
        private int port;

        private String fileRoot;
        private String execRoot;
        private final long id;
        private int numThreads;
        private NodeShell shell;
        
        /*public NodeParameters()
        {
            this("");
        }
        
        public NodeParameters(String hostIn)
        {
            this(stdUser, hostIn, stdExecRoot, stdFileRoot, stdPort);
        }*/
        
        public NodeParameters(String userIn, String hostIn, NodeShell shellIn, String execPath, String filePath, int portIn)
        {
            user = userIn;
            host = hostIn;
            port = portIn;
            shell = shellIn;
            execRoot = execPath;
            fileRoot = filePath;
            id = FijiArchipelago.getUniqueID();
            numThreads = 4;
        }

        public synchronized void setNumThreads(final int n)
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
        
        public synchronized void setShell(final NodeShell shell)
        {
            this.shell = shell;
        }

        public synchronized void setPort(final int port)
        {
            this.port = port;
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

        public int getPort()
        {
            return port;
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
        
        public int getNumThreads()
        {
            return numThreads;
        }
        
        public String toString()
        {
            return user + "@" + host + ":" + port + " id: " + id;
        }
    }
    
    private final Hashtable<Long, NodeParameters> nodeTable;
    
    private String stdExecRoot = "";
    private String stdFileRoot = "";
    private String stdUser = "";
    private NodeShell stdShell = null;
    private int stdPort = 22;
    
    public NodeManager()
    {
        nodeTable = new Hashtable<Long, NodeParameters>();
    }
    
    public synchronized NodeParameters getParam(final long id)
    {
        return nodeTable.get(id);
    }

    public synchronized void removeParam(final long id)
    {
        nodeTable.remove(id);
    }

    public synchronized void setStdUser(String user)
    {
        stdUser = user;
    }

    public String getStdUser()
    {
        return stdUser;
    }
    
    public synchronized void setStdPort(int port)
    {
        stdPort = port;
    }
    
    public int getStdPort()
    {
        return stdPort;
    }
    
    public synchronized void setStdExecRoot(String execRoot)
    {
        stdExecRoot = execRoot;
    }
    
    public String getStdExecRoot()
    {
        return stdExecRoot;
    }
    
    public synchronized void setStdFileRoot(String fileRoot)
    {
        stdFileRoot = fileRoot;
    }
    
    public String getStdFileRoot()
    {
        return stdFileRoot;
    }
    
    public synchronized void setStdShell(NodeShell shell)
    {
        stdShell = shell;
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
        return newParam(stdUser, hostIn, stdShell, stdExecRoot, stdFileRoot, stdPort);
    }

    public NodeParameters newParam(String userIn, String hostIn, NodeShell shell, String execPath, String filePath, int portIn)
    {
        NodeParameters param = new NodeParameters(userIn, hostIn, shell, execPath, filePath, portIn);
        FijiArchipelago.debug("Created new Param with id  " + param.id);
        
        nodeTable.put(param.id, param);
        
        if (nodeTable.get(param.id) == null)
        {
            System.out.println("I put it in, but I get null back");
        }
        return param;
    }




}
