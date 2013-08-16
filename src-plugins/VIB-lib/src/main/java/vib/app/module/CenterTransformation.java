package vib.app.module;

import amira.AmiraTable;

import java.util.ArrayList;

import math3d.Point3d;

import vib.app.ImageMetaData;
import vib.app.Options;

import vib.FloatMatrix;

public class CenterTransformation extends Module {
	public String getName() { return "CenterTransformation"; }
	protected String getMessage() { return "Calculating center transformation"; }

	protected void run(State state, int index) {
		// make sure that the template gets statistics, too
		new TissueStatistics().runOnOneImage(state, -1);
		new TissueStatistics().runOnOneImage(state, index);

		prereqsDone(state, index);

		String statisticsPath = state.getStatisticsPath(index);
		String templStatisticsPath = state.getStatisticsPath(-1);
		ImageMetaData stats = new ImageMetaData(statisticsPath);
		String transformLabel = state.getTransformLabel(Options.CENTER);
		if (stats.upToDate(templStatisticsPath, transformLabel))
			return;
		ImageMetaData templStats =
			new ImageMetaData(templStatisticsPath);

		ArrayList templCenters = new ArrayList();
		ArrayList centers = new ArrayList();
		// skip i == 0 (it's Exterior)
		for (int i = 1; i < templStats.materials.length; i++) {
			ImageMetaData.Material m1 = templStats.materials[i];
			if (m1.count == 0)
				continue;
			String name = m1.name;
			ImageMetaData.Material m2 = stats.getMaterial(name);
			if (m2 == null || m2.count == 0)
				continue;
			Point3d center = new Point3d(m1.centerX,
					m1.centerY, m1.centerZ);
			templCenters.add(center);
			center = new Point3d(m2.centerX,
					m2.centerY, m2.centerZ);
			centers.add(center);
		}
		if (centers.size() == 0)
			return;
		if (centers.size() < 3) // TODO: check for this already after the segmentation
			throw new RuntimeException("Need at least 3 labels shared with the template!");
		Point3d[] c1 = new Point3d[centers.size()];
		Point3d[] c2 = new Point3d[centers.size()];
		for (int i = 0; i < c1.length; i++) {
			c1[i] = (Point3d)templCenters.get(i);
			c2[i] = (Point3d)centers.get(i);
		}
		FloatMatrix matrix = FloatMatrix.bestRigid(c2, c1);
		stats.setMatrix(transformLabel, matrix);
		if(!stats.saveTo(statisticsPath))
			throw new RuntimeException("Could not save " + 
				statisticsPath);
	}
}
