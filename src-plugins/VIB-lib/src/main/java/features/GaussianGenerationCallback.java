/*
 */

package features;

public interface GaussianGenerationCallback {

    /* Any proportion >= 1.0 indicates completion.  A proportion less
     * than zero indicates that the generation of the Gaussian has
     * been cancelled. */

    public void proportionDone( double proportion );

}
