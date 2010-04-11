package vib.app.module;

import amira.AmiraParameters;
import amira.AmiraTable;
import amira.AmiraTableEncoder;

import ij.ImagePlus;

import ij.measure.Calibration;

import vib.InterpolatedImage;

import vib.app.ImageMetaData;

public class TissueStatistics extends Module {
	public String getName() { return "TissueStatistics"; }
	protected String getMessage() { return "Calculating tissue statistics"; }

	protected void run(State state, int index) {
		new ResampleLabels().runOnOneImage(state, index);

		prereqsDone(state, index);

		String statisticsPath = state.getStatisticsPath(index);
		String labelsPath = state.getImagePath(-1, index);
		if (state.upToDate(labelsPath, statisticsPath))
			return;

		ImagePlus labelField = state.getImage(labelsPath);
		Statistics stats = getStatistics(labelField);
		ImageMetaData metaData = new ImageMetaData();
		for (int i = 0; i < stats.materials.length; i++)
			metaData.setMaterial(stats.materials[i],
					(int)stats.count[i], stats.count[i] *
					stats.voxelVolume(),
					stats.centerX(i), stats.centerY(i),
					stats.centerZ(i));
		if(!metaData.saveTo(statisticsPath))
			throw new RuntimeException("Could not save " + 
				statisticsPath);
	}

	public static class Statistics {
		Calibration cal;
		AmiraParameters parameters;
		public String[] materials;
		public long[] count, cX, cY, cZ;
		public int[] minX, maxX, minY, maxY, minZ, maxZ;

		public Statistics(InterpolatedImage ii) {
			cal = ii.image.getCalibration();
			parameters = new AmiraParameters(ii.image);
			materials = parameters.getMaterialList();
			count = new long[materials.length];
			cX = new long[materials.length];
			cY = new long[materials.length];
			cZ = new long[materials.length];
			minX = new int[materials.length];
			maxX = new int[materials.length];
			minY = new int[materials.length];
			maxY = new int[materials.length];
			minZ = new int[materials.length];
			maxZ = new int[materials.length];

			for (int i = 0; i < materials.length; i++)
				minX[i] = minY[i] = minZ[i] = Integer.MAX_VALUE;

			doit(ii);
		}

		public void doit(InterpolatedImage ii) {
			InterpolatedImage.Iterator iter = ii.iterator(true);
			while (iter.next() != null) {
				int v = ii.getNoInterpol(iter.i,
						iter.j, iter.k);
				count[v]++;
				cX[v] += iter.i;
				cY[v] += iter.j;
				cZ[v] += iter.k;
				if (minX[v] > iter.i)
					minX[v] = iter.i;
				else if (maxX[v] < iter.i)
					maxX[v] = iter.i;
				if (minY[v] > iter.j)
					minY[v] = iter.j;
				else if (maxY[v] < iter.j)
					maxY[v] = iter.j;
				if (minZ[v] > iter.k)
					minZ[v] = iter.k;
				else if (maxZ[v] < iter.k)
					maxZ[v] = iter.k;
			}
		}

		public double x(double i) {
			return cal.xOrigin + (i + 0.5) * cal.pixelWidth;
		}

		public double y(double j) {
			return cal.yOrigin + (j + 0.5) * cal.pixelHeight;
		}

		public double z(double k) {
			return cal.yOrigin + (k + 0.5) * cal.pixelDepth;
		}

		public double voxelVolume() {
			return cal.pixelWidth * cal.pixelHeight
				* cal.pixelDepth;
		}

		public double centerX(int index) {
			return x(cX[index] / (double)count[index]);
		}

		public double centerY(int index) {
			return y(cY[index] / (double)count[index]);
		}

		public double centerZ(int index) {
			return z(cZ[index] / (double)count[index]);
		}

		public String getResult() {
			double voxelVolume = voxelVolume();
			String result = "";
			for (int i = 0; i < materials.length; i++) {
				result += (i + 1) + "\t";
				result += materials[i] + "\t";
				result += count[i] + "\t";
				if (count[i] == 0) {
					result += "0\t0\t0\t0\t"
						+ "0\t0\t0\t0\t0\t0\n";
					continue;
				}
				result += (count[i] * voxelVolume) + "\t";
				result += centerX(i) + "\t";
				result += centerY(i) + "\t";
				result += centerZ(i) + "\t";
				result += x(minX[i]) + "\t";
				result += x(maxX[i]) + "\t";
				result += y(minY[i]) + "\t";
				result += y(maxY[i]) + "\t";
				result += z(minZ[i]) + "\t";
				result += z(maxZ[i]) + "\n";
			}

			return result;
		}
	}

	public static Statistics getStatistics(ImagePlus labelfield) {
		InterpolatedImage ii = new InterpolatedImage(labelfield);
		return new Statistics(ii);
	}

}
