/**
 * 
 * @author Benjamin Schmid
 * 
 * @date 07.08.2006
 */
package vib;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class LocalRigidRegistration_ implements PlugInFilter{

	private ImagePlus image;
	
	public void run(ImageProcessor ip) {
		int[] wIDs = WindowManager.getIDList();
		if(wIDs == null){
			IJ.error("No images open");
			return;
		}
		String[] titles = new String[wIDs.length];
		for(int i=0;i<wIDs.length;i++){
			titles[i] = WindowManager.getImage(wIDs[i]).getTitle();
		}
		
		GenericDialog gd = new GenericDialog("Registration parameters");
		gd.addChoice("Template",titles,WindowManager.getCurrentImage().getTitle());
		gd.addChoice("Image",titles,titles[0]);
		
		
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		ImagePlus templ = WindowManager.getImage(gd.getNextChoice());
		image = WindowManager.getImage(gd.getNextChoice());
		TransformedImage trans = new TransformedImage(templ, image);
		
		trans.measure = new distance.Euclidean();
		
		
		try {
			RigidRegistration r = new RigidRegistration();
			FastMatrix global = r.rigidRegistration(
					trans, //trans, 
					null,  //materialBBox, 
					null,  //initial, 
					-1,    //mat1, 
					-1,    //mat2, 
					false, //noOptimization, 
					4,     //level, 
					2,     //stopLevel, 
					1.0,   //tolerance, 
					1,     //nInitialPositions, 
					true,  //showTransformed, 
					true,  //showDifferenceImage
					false, //fastButInaccurate
					null   //alsoTransform
                                        );

			// Retrieve landmark sets and look for agreements
			PointList pl_image = PointList.load(image);
			PointList pl_template = PointList.load(templ);
			if(pl_image == null || pl_template == null){
				IJ.error("Landmarks could not be loaded. Abort");
			}
			PointList commonPoints = PointList.pointsInBoth(pl_image, pl_template);
			int n_landmarks = commonPoints.size();
			System.out.println("Number of landmarks: " + n_landmarks);
			if(n_landmarks == 0){
				IJ.error("No common landmarks. Abort");
			}
			VIB.println("Common landmarks:\n" + commonPoints);
			
			// for each landmark build a BB and repeat the RR
			String initial = global.toStringForAmira();
			FastMatrix[] transformations = new FastMatrix[n_landmarks]; 
			for(int i=0;i<n_landmarks;i++){
				BenesNamedPoint p = commonPoints.get(i);
				System.out.println("LANDMARK " + p);
				String bb = p.x + " " + p.y + " " + p.z + " " +  
							(p.x-50) + " " + (p.y-50) + " " + (p.z-50) + " " + 
							(p.x+50) + " " + (p.y+50) + " " + (p.z+50);
				
				transformations[i] = new RigidRegistration().rigidRegistration(
						trans,  //trans, 
						bb,     //materialBBox, 
						initial,//initial, 
						-1,     //mat1, 
						-1,     //mat2, 
						false,  //noOptimization, 
						4,      //level, 
						2,      //stopLevel, 
						1.0,    //tolerance, 
						1,      //nInitialPositions, 
						false,  //showTransformed, 
						false,  //showDifferenceImage
						false,  //fastButInaccurate
                                                null    //alsoTransform
						);
			}
			
			AugmentedLandmarkWarp_ aw = new AugmentedLandmarkWarp_();
			aw.setCenter(commonPoints.toArray());
			aw.matrix = transformations;
			aw.ii = new InterpolatedImage(templ);
			aw.model = new InterpolatedImage(image);
			aw.run();
			
		}catch (RuntimeException e){
			e.printStackTrace();
		}
			
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G | DOES_8C;
	}
}

	
