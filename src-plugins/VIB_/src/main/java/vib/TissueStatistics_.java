package vib;

import amira.AmiraParameters;
import amira.AmiraTable;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import vib.app.module.TissueStatistics;
import vib.app.module.TissueStatistics.Statistics;

public class TissueStatistics_ implements PlugInFilter {
	ImagePlus image;

	public void run(ImageProcessor ip) {
		AmiraTable table = calculateStatistics(image);
		table.show();
	}

	public static AmiraTable calculateStatistics(ImagePlus labelfield) {
		if (!AmiraParameters.isAmiraLabelfield(labelfield)) {
			IJ.error("Need a labelfield!");
			return null;
		}
		String title = "Statistics for " + labelfield.getTitle();
		String headings = "Nr\tMaterial\tCount\tVolume\t" + 
							"CenterX\tCenterY\tCenterZ\t" + 
							"MinX\tMaxX\tMinY\tMaxY\tMinZ\tMaxZ";

		Statistics stat = TissueStatistics.getStatistics(labelfield);

		AmiraTable table = new AmiraTable(title, headings,
				stat.getResult(), true);
		return table;
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G | DOES_8C;
	}
}

