package edu.utexas.clm.archipelago.listen;


import edu.utexas.clm.archipelago.Cluster;

public interface ClusterStateListener
{
    public void stateChanged(Cluster cluster);
}
