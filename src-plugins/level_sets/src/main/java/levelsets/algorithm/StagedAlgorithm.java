// $Revision$, $Date$, $Author$

package levelsets.algorithm;

/**
 * Interface for algorithms that can be divided into multiple steps.
 */
public interface StagedAlgorithm
{
   /**
    * Executes a defineable portion of the algorithm.
    * @param granularity Number of steps to be executed before retuning (until further execution by a
    * consecutive call)
    * @return True if this algorithm can be continued by a further call, false otherwise.
    */
   public boolean step(int granularity);
}
