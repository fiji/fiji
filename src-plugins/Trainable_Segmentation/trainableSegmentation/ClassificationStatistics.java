package trainableSegmentation;

import ij.IJ;

public class ClassificationStatistics 
{
	public int truePositives = 0;
	public int trueNegatives = 0;
	public int falsePositives = 0;
	public int falseNegatives = 0;
	
	public double metricValue = 0;
	
	public double precision = 0;
	public double recall = 0;
	public double fScore = 0;
	
	public ClassificationStatistics(
			int truePositives,
			int trueNegatives,
			int falsePositives,
			int falseNegatives,
			double metricValue)
	{
		this.truePositives = truePositives;
		this.trueNegatives = trueNegatives;
		this.falsePositives = falsePositives;
		this.falseNegatives = falseNegatives;
		this.metricValue = metricValue;
		
		this.precision = (double) truePositives / ((double)truePositives + (double)falsePositives);
		this.recall = (double) truePositives / ((double)truePositives + (double)falseNegatives);
		this.fScore = 2 * precision * recall / ( precision + recall );
		
		IJ.log("truePositives = " + truePositives);
		IJ.log("trueNegatives = " + trueNegatives);
		IJ.log("falsePositives = " + falsePositives);
		IJ.log("falseNegatives = " + falseNegatives);
		IJ.log("precision = " + precision);
		IJ.log("recall = " + recall);
		IJ.log("fScore = " + fScore);
		
	}
}
