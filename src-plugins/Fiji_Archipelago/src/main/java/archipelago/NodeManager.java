package archipelago;

import archipelago.network.ClusterNode;

import java.util.Hashtable;

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
        
        /*public NodeParameters()
        {
            this("");
        }
        
        public NodeParameters(String hostIn)
        {
            this(stdUser, hostIn, stdExecRoot, stdFileRoot, stdPort);
        }*/
        
        public NodeParameters(String userIn, String hostIn, String execPath, String filePath, int portIn)
        {
            user = userIn;
            host = hostIn;
            port = portIn;
            execRoot = execPath;
            fileRoot = filePath;
            id = FijiArchipelago.getUniqueID();
            numThreads = 1;
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
            return user + ":" + host + ":" + port + " id: " + id;
        }
    }
    
    private final Hashtable<Long, NodeParameters> nodeTable;
    
    private String stdExecRoot = "";
    private String stdFileRoot = "";
    private String stdUser = "";
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

    public synchronized void setStandardUser(String user)
    {
        stdUser = user;
    }

    public synchronized void setStandartPort(int port)
    {
        stdPort = port;
    }
    
    public synchronized void setStdExecRoot(String execRoot)
    {
        stdExecRoot = execRoot;
    }
    
    public synchronized void setStdFileRoot(String fileRoot)
    {
        stdFileRoot = fileRoot;
    }

    public NodeParameters newParam()
    {
        return newParam("");
    }

    public NodeParameters newParam(String hostIn)
    {
        return newParam(stdUser, hostIn, stdExecRoot, stdFileRoot, stdPort);
    }

    public NodeParameters newParam(String userIn, String hostIn, String execPath, String filePath, int portIn)
    {
        NodeParameters param = new NodeParameters(userIn, hostIn, execPath, filePath, portIn);
        FijiArchipelago.debug("Created new Param with id  " + param.id);
        
        nodeTable.put(param.id, param);
        
        if (nodeTable.get(param.id) == null)
        {
            System.out.println("I put it in, but I get null back");
        }
        return param;
    }




}
