package fiji.plugin.spottracker.tracking.costfunction;

import java.util.ArrayList;

import Jama.Matrix;
import fiji.plugin.spottracker.Feature;
import fiji.plugin.spottracker.Spot;
import fiji.plugin.spottracker.Utils;
import fiji.plugin.spottracker.features.BlobMorphology;

/**
 * Similar to {@link SplittingCostFunction}, but in addition to the cost
 * function described by the paper, includes morphology and temporal 
 * information into a cost.
 * 
 * The inspiration for the morphology and temporal cost additions can be
 * found in:
 * 
 * Bao, Z. et al. Automated cell lineage tracing in Caenorhabditis elegans.
 * PNAS, 2006. 
 * 
 * @author Nicholas Perry
 *
 */
public class EnhancedSplittingCostFunction implements CostFunctions {

	/** The cost matrix. */
	protected Matrix m;
	/** The distance threshold. */
	protected double maxDist;
	/** The value used to block an assignment in the cost matrix. */
	protected double blocked;
	/** The list of track segments. */
	protected ArrayList< ArrayList<Spot> > trackSegments;
	/** The list of middle points. */
	protected ArrayList<Spot> middlePoints;
	/** Thresholds for the intensity ratios. */
	protected double[] intensityThresholds;
	/** Has the same length as middlePoints, but stores which track segment
	 * each middle point belongs to. */
	protected int[] middlePointSegmentIndices;
	/** Number of frames that (approximately) constitutes a single cell cycle (division). */
	protected int divisionLimit;
	/** Number of frames before an after a nucleus which should contain a division. */
	protected int noDivisionLimit;
	
	
	public EnhancedSplittingCostFunction(Matrix m, ArrayList< ArrayList<Spot> > trackSegments, ArrayList<Spot> middlePoints, double maxDist, double blocked, double[] intensityThresholds, int[] middlePointSegmentIndices) {
		this.m = m;
		this.trackSegments = trackSegments;
		this.middlePoints = middlePoints;
		this.maxDist = maxDist;
		this.blocked = blocked;
		this.intensityThresholds = intensityThresholds;
	}
	
	
	@Override
	public void applyCostFunction() {
		double iRatio, dist, score, morphologyFactor, temporalFactor;
		Spot start, mother, otherDaughter;
		
		// 1 - Fill in splitting scores
		for (int i = 0; i < middlePoints.size(); i++) {
			for (int j = 0; j < trackSegments.size(); j++) {
				start = trackSegments.get(j).get(0);
				mother = middlePoints.get(i);
				
				// Frame threshold - middle Spot must be one frame behind of the start Spot
				if (mother.getFrame() != start.getFrame() - 1) {
					m.set(i, j, blocked);
					continue;
				}
				
				// Radius threshold
				dist = Utils.euclideanDistance(start, mother);
				if (dist > maxDist) {
					m.set(i, j, blocked);
					continue;
				}

				otherDaughter = mother.getNext().get(0);
				iRatio = mother.getFeature(Feature.MEAN_INTENSITY) / (otherDaughter.getFeature(Feature.MEAN_INTENSITY) + start.getFeature(Feature.MEAN_INTENSITY));
				
				// Intensity threshold -  must be within INTENSITY_RATIO_CUTOFFS ([min, max])
				if (iRatio > intensityThresholds[1] || iRatio < intensityThresholds[0]) {
					m.set(i, j, blocked);
					continue;
				}
				
				// Morphology
				
				// Mother's shape
				morphologyFactor = 1.0;
				if (mother.getFeature(Feature.MORPHOLOGY) == BlobMorphology.ELLIPSOID) {
					morphologyFactor -= 0.05;
				} else {
					morphologyFactor += 0.05;
				}
				
				// Daughter 1's shape
				if (otherDaughter.getFeature(Feature.MORPHOLOGY) == BlobMorphology.ELLIPSOID) {
					morphologyFactor -= 0.05;
				} else {
					morphologyFactor += 0.05;
				}
				
				if (otherDaughter.getFeature(Feature.ESTIMATED_DIAMETER) < 0.95 * mother.getFeature(Feature.ESTIMATED_DIAMETER)) {
					morphologyFactor -= 0.05;
				} else {
					morphologyFactor += 0.05;
				}
				
				// Daughter 2's shape
				if (start.getFeature(Feature.MORPHOLOGY) == BlobMorphology.ELLIPSOID) {
					morphologyFactor -= 0.05;
				} else {
					morphologyFactor += 0.05;
				}
				
				if (start.getFeature(Feature.ESTIMATED_DIAMETER) < 0.95 * mother.getFeature(Feature.ESTIMATED_DIAMETER)) {
					morphologyFactor -= 0.05;
				} else {
					morphologyFactor += 0.05;
				}
				
				// Intensity of daughters
				double daughterIRatio = otherDaughter.getFeature(Feature.MEAN_INTENSITY) / start.getFeature(Feature.MEAN_INTENSITY);
				if (daughterIRatio <= 1.1 && daughterIRatio >= 0.9) {  // difference <= 10%
					morphologyFactor -= 0.05;
				} else {
					morphologyFactor += 0.05;
				}
				
				// Diameter of daughters
				double daughterDiamRatio = otherDaughter.getFeature(Feature.ESTIMATED_DIAMETER) / start.getFeature(Feature.ESTIMATED_DIAMETER);
				if (daughterDiamRatio <= 1.1 && daughterDiamRatio >= 0.9) {	// difference <= 10%
					morphologyFactor -= 0.05;
				} else {
					morphologyFactor += 0.05;
				}

				// Set score
				if (iRatio >= 1) {
					score = dist * dist * iRatio * morphologyFactor;
				} else {
					score = dist * dist * ( 1 / (iRatio * iRatio) ) * morphologyFactor;
				}
				m.set(i, j, score);
			}
		}
		
		// 2 - Adjust splitting scores based on temporal information
		
		// Penalize dividing too many times per approximately cell cycle length.
		
		// Penalize not dividing enough.
	}
	
	public static void main (String[] args) {
		
	}
}
