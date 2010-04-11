package oldsegmenters;

import adt.Sparse3DByteArray;
import adt.Byte3DArray;
import adt.Unsparse3DByteArray;
import adt.ByteProbability;

import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.LinkedHashMap;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import ij.text.TextWindow;
import ij.io.FileInfo;
import ij.ImagePlus;

import amira.AmiraMeshDecoder;

/**
 * User: Tom Larkworthy
 * Date: 12-Jul-2006
 * Time: 19:10:47
 */
public abstract class AutoLabeller {
	HashMap<Byte, LabelStats> stats = new LinkedHashMap<Byte, LabelStats>();

	//HashMap<Byte, HashMap<Byte, Double>> mixtureProbabilityCache = new HashMap<Byte, HashMap<Byte, Double>>();

	//HashMap<Byte, Double>[] mixtureProbabilityCache;


	byte[][] intensityProbabilities;

	final int labelCount;
	byte[] labelIds;

	//bounds of labels that are not external!
	int xMin = Integer.MAX_VALUE, xMax = Integer.MIN_VALUE;
	int yMin = Integer.MAX_VALUE, yMax = Integer.MIN_VALUE;
	int zMin = Integer.MAX_VALUE, zMax = Integer.MIN_VALUE;


	public AutoLabeller(String summeryLocation) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(summeryLocation));
		String line = in.readLine();


		TreeSet<LabelStats> orderedSet = new TreeSet<LabelStats>();
		while (line != null) {
			LabelStats labelStats = readStats(line);
			orderedSet.add(labelStats);

			System.out.println("loaded " + labelStats.toString());


			line = in.readLine();
		}
		in.close();

		labelIds = new byte[orderedSet.size()];
		labelCount = labelIds.length;

		int index = 0;
		for (LabelStats labelStats : orderedSet) {
			System.out.println("adding " + labelStats);
			stats.put(labelStats.id, labelStats);
			labelIds[index++] = labelStats.id;
		}

		System.out.println("summary file read");

		calculateLabelGivenIntensityProbabilites();

		System.out.flush();
	}

	private void calculateLabelGivenIntensityProbabilites() {
		//mixtureProbabilityCache = new LinkedHashMap[256];
		intensityProbabilities = new byte[256][labelCount];
		double[][] precicionCalc = new double[256][labelCount];


		for (int i = 0; i < 256; i++) {
			byte pixelIntensity = ByteProbability.INTEGER_TO_BYTE[i];
			double total = 0;
			double totalVolume=0;

			StringBuffer buf = new StringBuffer("intesity = ").append(i).append(" ");

            //buf.append("initial");
			for (int j = 0; j < labelCount; j++) {
				LabelStats stat = stats.get(labelIds[j]);

				precicionCalc[i][j] = stat.getPixelProb(pixelIntensity);
				total += precicionCalc[i][j];
				totalVolume+=stat.volumeMean;
			}
			//normalize and add effect of volume
			double total2=0;
			//buf.append("Norm+vol ");
            for (int j = 0; j < intensityProbabilities[i].length; j++) {
				LabelStats stat = stats.get(labelIds[j]);
				precicionCalc[i][j] = (precicionCalc[i][j] / total)/* * (stat.volumeMean/totalVolume)*/;
				total2+=precicionCalc[i][j];

				//buf.append(precicionCalc[i][j]).append(" ");
			}

			//normalize and pack into the byte version
			for (int j = 0; j < intensityProbabilities[i].length; j++) {
				LabelStats stat = stats.get(labelIds[j]);
				precicionCalc[i][j]/=total2;

				intensityProbabilities[i][j] = ByteProbability.toByte((precicionCalc[i][j]));

				buf.append(intensityProbabilities[i][j]).append(" ");
				//buf.append(precicionCalc[i][j]).append(" ");
			}

			System.out.println(buf);

		}
	}


	/**
	 * calculates the probability of the pixel intensity occuring given the material
	 *
	 * @param pixel
	 */
	/*
	private LinkedHashMap<Byte, Double> calcMixtureProb(byte pixel) {
		LinkedHashMap<Byte, Double> ret = new LinkedHashMap<Byte, Double>();

		double total = 0;



		double totalVolume = 0;
		for (Byte materialId : stats.keySet()) {
			totalVolume += stats.get(materialId).volumeMean;
		}

		for (Byte materialId : stats.keySet()) {
			double prob = stats.get(materialId).getProProb(pixel);
			total += prob;
			ret.put(materialId, prob);
		}
		//normalize
		for (Byte materialId : stats.keySet()) {
			ret.put(materialId, ret.get(materialId) / total);
		}
		total = 0;
		System.out.println("before volume = " + ret);
		//mix
		for (Byte materialId : stats.keySet()) {
			ret.put(materialId, ret.get(materialId) * stats.get(materialId).volumeMean / totalVolume);
			total += ret.get(materialId);
		}
		//normalize
		for (Byte materialId : stats.keySet()) {
			ret.put(materialId, ret.get(materialId) / total);
		}
		System.out.println("after volume = " + ret);


		return ret;
	}

	public HashMap<Byte, Double> getMixtureProbs(byte pixel) {
		return mixtureProbabilityCache[pixel & 0xFF];
	}     */

	/**
	 * returns(normailized) probs of a label given its intensity (label id given my amterialIds)
	 * @param pixel
	 * @return
	 */
	public byte[] getIntensityProbs(byte pixel) {
		return intensityProbabilities[pixel & 0xFF];
	}
	/**
	 * returns(normailized) probs of a label given its location
	 * @return
	 */
	public byte[] getSpatialProbs(int x, int y, int z) {
		byte[] ret = new byte[labelCount];

		for(int i=0; i < labelCount; i++){
			byte id = labelIds[i];
			LabelStats stat = stats.get(id);

			ret[i] = stat.spatialDistribution.get(x,y,z);
            //System.out.print(ret[i]);
			//System.out.print(" ");
		}
		//System.out.println();



		return ret;
	}

	private LabelStats readStats(String line) {
		StringTokenizer tokenizer = new StringTokenizer(line, "\t", false);

		LabelStats ret = new LabelStats();

		ret.id = Byte.parseByte(tokenizer.nextToken());
		ret.name = tokenizer.nextToken();

		/*
		ret.intensityMean = Double.parseDouble(tokenizer.nextToken());
		ret.intensityVarience = Double.parseDouble(tokenizer.nextToken());
        */

		ret.volumeMean = Double.parseDouble(tokenizer.nextToken());
		ret.volumeVariance = Double.parseDouble(tokenizer.nextToken());

		//read pixel intesity probs
		for (int i = 0; i < 256; i++) {
			ret.pixelProb[i] = Double.parseDouble(tokenizer.nextToken());
		}

		if (tokenizer.hasMoreTokens()) {
			String filename = tokenizer.nextToken();

            ImagePlus imagePlus = new ImagePlus();

            AmiraMeshDecoder d=new AmiraMeshDecoder();
            if(d.open(filename)) {
                if (d.isTable()) {
                    TextWindow table = d.getTable();
                } else {
                    FileInfo fi=new FileInfo();
                    File file = new File(filename);
                    fi.fileName=file.getName();
                    fi.directory=file.getParent();
                    imagePlus.setFileInfo(fi);
                    imagePlus.setStack(filename,d.getStack());
                    d.parameters.setParameters(imagePlus);
                }
            }

			Byte3DArray pd;

			if (ret.id != 0) {
				pd = new Sparse3DByteArray();
			} else {
				pd = new Unsparse3DByteArray(imagePlus.getWidth(), imagePlus.getHeight(), imagePlus.getStackSize());
			}

			int width = imagePlus.getWidth();

			for (int z = 1; z <= imagePlus.getStackSize(); z++) {
				byte[] pixels = (byte[]) imagePlus.getStack().getProcessor(z).getPixels();
				for (int i = 0; i < pixels.length; i++) {
					byte pixel = pixels[i];
					if (pixel != 0)
						pd.put(i % width, i / width, z, pixel);
				}
			}

			ret.spatialDistribution = pd;

			if (ret.id != 0) {
				//if it is not an external spatial distribution then we should add the bounds to the bounding box
				xMin = Math.min(pd.getxMin(), xMin);
				xMax = Math.max(pd.getxMax(), xMax);
				yMin = Math.min(pd.getyMin(), yMin);
				yMax = Math.max(pd.getyMax(), yMax);
				zMin = Math.min(pd.getzMin(), zMin);
				zMax = Math.max(pd.getzMax(), zMax);
			}

            imagePlus.close();
		}

		return ret;
	}

	public abstract void segment(SegmentatorModel model);

	public class LabelStats implements Comparable {
		byte id;
		String name;

		//double intensityMean;
		//double intensityVarience;

		double volumeMean;
		double volumeVariance;

		Byte3DArray spatialDistribution;
		double[] pixelProb = new double[256];

		//returns a proportional probability of this intensity value being generated by this material
		public double getPixelProb(byte pixel) {
			return pixelProb[pixel & 0xFF];
		}

		public String toString() {
			StringBuffer buf = new StringBuffer();
			buf.append(name).append(" ");
			buf.append(id & 0xFF).append(" ");
			buf.append("[");
			buf.append(spatialDistribution.getxMin()).append(", ");
			buf.append(spatialDistribution.getxMax()).append(", ");
			buf.append(spatialDistribution.getyMin()).append(", ");
			buf.append(spatialDistribution.getyMax()).append(", ");
			buf.append(spatialDistribution.getzMin()).append(", ");
			buf.append(spatialDistribution.getzMax()).append("]");
			return buf.toString();
		}

		public int compareTo(Object o) {
			return ((LabelStats) o).id < id ? 1 : -1;
		}
	}
}
