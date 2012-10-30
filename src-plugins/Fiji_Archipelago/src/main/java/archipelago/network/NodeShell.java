package archipelago.network;

public interface NodeShell
{
    
    public ClusterNode getNode();

    public boolean exec(String command);
    
    public String getExecResponse();

    public boolean verify();
    
    public boolean isActive();
    
    public String lastMessage();

    public boolean lastSuccess();

    public void join();
}
