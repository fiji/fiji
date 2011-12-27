package oldsegmenters;

import adt.PixelStats;
import adt.ByteProbability;

import java.io.IOException;
import java.util.HashMap;

import ij.IJ;

/**
 * User: Tom Larkworthy
 * Date: 12-Jul-2006
 * Time: 19:12:08
 */
public class AutoLabellerNaive extends AutoLabeller {
	public AutoLabellerNaive(String summeryLocation) throws IOException {
		super(summeryLocation);
	}

	public void segment(SegmentatorModel model) {
		
        System.out.println("starting naive segmentation");
		int width = model.data.getWidth();
		int height = model.data.getHeight();
		int volume = model.data.getStackSize() * model.data.getWidth() * model.data.getHeight();



		//p(label|data) is proportioanl to
		//p(data|label)p(label)
		//where p(label) is what proportion on average is that label present
		byte[] pLabels = new byte[labelCount];
		for(int i=0;i<labelCount;i++){
			LabelStats stat = stats.get(labelIds[i]);
            pLabels[i] = ByteProbability.toByte(stat.volumeMean / volume);
			System.out.println("p(label= " + (labelIds[i]&0xFF)+") = " + pLabels[i]);
		}




		//we can assume the labels are blank allready
		//so only within the labels bounding box should we label
		for (int z = zMin; z <= zMax; z++) {
    	//for (int z = 55; z <= 56; z++) {
			IJ.showProgress(z, zMax);
            System.out.println("z = " + z);

			byte[] pixels = (byte[]) model.data.getStack().getProcessor(z).getPixels();
			byte[] labelPixels = (byte[])model.getLabelImagePlus().getStack().getProcessor(z).getPixels();
			for(int x=xMin; x<xMax; x++)
			for(int y=yMin; y<yMax; y++)
			//for(int x=95; x<120; x++)
			//for(int y=290; y<305; y++)
			{
				int i= y*width + x;

				//probabilities based on pixel intensities
				byte[] intensityProbs = getIntensityProbs(pixels[i]);

				byte[] spatialProbs = getSpatialProbs(x,y,z);

				//now find which label is ML
				byte ML = 0;

				//System.out.printf("(%d, %d)", x, y);
				byte MLid = 0;


				for(int materialIndex=0; materialIndex < labelCount; materialIndex++) {
					byte materialId = labelIds[materialIndex];

					LabelStats stat = stats.get(materialId);

					//where p(data|label) depends on the spatial sposition, and intensity vals
					byte spatialProb = spatialProbs[materialIndex];
					//spatialProb = Math.min(spatialProb, .95);
					byte intensityProb = intensityProbs[materialIndex];

					byte p = ByteProbability.multiply(spatialProb, intensityProb) ;
				    
					if((p&0xFF) > (ML&0xFF)){
						ML = p;
						MLid = materialId;
					}

				}
				labelPixels[i] = MLid;
			}
			model.updateSlice(z);
		}
	}
}
