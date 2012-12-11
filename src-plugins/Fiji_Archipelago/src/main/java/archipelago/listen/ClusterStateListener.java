package archipelago.listen;


import archipelago.Cluster;

public interface ClusterStateListener
{
    public void stateChanged(Cluster cluster);
}
