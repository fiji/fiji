package edu.utexas.clm.archipelago.ijsupport.bottle;

import edu.utexas.clm.archipelago.network.MessageXC;
import edu.utexas.clm.archipelago.network.translation.Bottle;
import mpicbg.trakem2.align.RegularizedAffineLayerAlignment;

import java.io.IOException;

/**
 *
 */
public class SIFTParamBottle implements Bottle
{
    private final int fdSize;
    private final int fdBins;
    private final int maxOctaveSize;
    private final int minOctaveSize;
    private final int steps;
    private final float initialSigma;

    private final float maxEpsilon;
    private final float minInlierRatio;
    private final int minNumInliers;
    private final int expectedModelIndex;
    private final boolean multipleHypotheses;
    private final boolean rejectIdentity;
    private final float identityTolerance;
    private final int maxNumNeighbors;
    private final int maxNumFailures;
    private final int desiredModelIndex;
    private final int maxIterationsOptimize;
    private final int maxPlateauwidthOptimize;
    private final boolean visualize;
    private final int maxNumThreads;
    private final boolean regularize;
    private final int regularizerIndex;
    private final float lambda;

    private final float rod;
    private final boolean clearCache;
    private final int maxNumThreadsSift;

    public SIFTParamBottle(RegularizedAffineLayerAlignment.Param param)
    {
        fdSize = param.ppm.sift.fdSize;
        fdBins = param.ppm.sift.fdBins;
        steps = param.ppm.sift.steps;
        maxOctaveSize = param.ppm.sift.maxOctaveSize;
        minOctaveSize = param.ppm.sift.minOctaveSize;
        initialSigma = param.ppm.sift.initialSigma;
        rod = param.ppm.rod;
        clearCache = param.ppm.clearCache;
        maxNumThreadsSift = param.ppm.maxNumThreadsSift;

        maxEpsilon = param.maxEpsilon;
        minInlierRatio = param.minInlierRatio;
        minNumInliers = param.minNumInliers;
        expectedModelIndex = param.expectedModelIndex;
        multipleHypotheses = param.multipleHypotheses;
        rejectIdentity = param.rejectIdentity;
        identityTolerance = param.identityTolerance;
        maxNumNeighbors = param.maxNumNeighbors;
        maxNumFailures = param.maxNumFailures;
        desiredModelIndex = param.desiredModelIndex;
        maxIterationsOptimize = param.maxIterationsOptimize;
        maxPlateauwidthOptimize = param.maxPlateauwidthOptimize;
        visualize = param.visualize;
        maxNumThreads = param.maxNumThreads;
        regularize = param.regularize;
        regularizerIndex = param.regularizerIndex;
        lambda = param.lambda;


    }

    public Object unBottle(MessageXC xc) throws IOException
    {
        return
                new RegularizedAffineLayerAlignment.Param(
            fdBins,
            fdSize,
            initialSigma,
            maxOctaveSize,
            minOctaveSize,
            steps,

            clearCache,
            maxNumThreadsSift,
            rod,

            desiredModelIndex,
            expectedModelIndex,
            identityTolerance,
            lambda,
            maxEpsilon,
            maxIterationsOptimize,
            maxNumFailures,
            maxNumNeighbors,
            maxNumThreads,
            maxPlateauwidthOptimize,
            minInlierRatio,
            minNumInliers,
            multipleHypotheses,
            regularize,
            regularizerIndex,
            rejectIdentity,
            visualize );
    }
}
