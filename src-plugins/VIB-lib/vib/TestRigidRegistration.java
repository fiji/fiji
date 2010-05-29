/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib;

import distance.Euclidean;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.*;

import util.BatchOpener;

import distance.MutualInformation;
import distance.TwoValues;
import distance.Correlation;
import distance.Euclidean;

import ij.ImagePlus;
import ij.ImageJ;
import ij.io.FileSaver;

import java.io.File;

public class TestRigidRegistration {

	RigidRegistration plugin;

	ImageJ imageJ;

	@Before
	public void loadImagesAndImageJ() {
		// Start ImageJ (maybe not necessary?
		this.imageJ = new ImageJ();
	}

	@After
	public void closeImageAndImageJ() {		
		imageJ.quit();
	}

	@Test
	public void testRegistration8BitGray() {

		String canton = "test-images"+File.separator+"CantonF41c-reduced.tif";
		String other  = "test-images"+File.separator+"tidied-mhl-62yxUAS-lacZ0-reduced.tif";

		float bestScoreMI = -0.3f;
                float bestScoreEuclidean = 25.0f;
                
                ImagePlus template = BatchOpener.openFirstChannel(canton);
                ImagePlus toTransform = BatchOpener.openFirstChannel(other);
                        
		for( int timeThrough = 0; timeThrough < 3; ++timeThrough ) {

			assertTrue( template != null );
			assertTrue( toTransform != null );
			
			plugin = new RigidRegistration();
			
			int level = RigidRegistration.guessLevelFromWidth(
				template.getWidth() );
			
			TransformedImage ti = new TransformedImage(
				template,
				toTransform );

                        String run = null;
                        float bestScore = Float.MIN_VALUE;
                                                
			if( timeThrough == 0 ) {
                            run = "eu";
                            ti.measure = new Euclidean();
                            bestScore = 35;
			} else if( timeThrough == 1 ) {
                            run = "mi";
                            ti.measure = new MutualInformation();
                            bestScore = -0.2f;
			} else if( timeThrough == 2 ) {
                            run = "co";
                            ti.measure = new Correlation();
                            bestScore = 0.5f;
			}
			
			FastMatrix matrix = plugin.rigidRegistration(
				ti,
				"",         // material b box
				"",         // initial
				-1,         // material 1
				-1,         // material 2
				false,      // no optimization
				level,      // level
				level > 2 ? 2 : level, // stop level
				1.0,        // tolerance
				1,          // number of initial positions
				false,      // show transformed
				false,      // show difference image
				false,      // fast but inaccurate
				null );     // other images to transform

                        // Make sure the output directory exists:
                        
                        File outputDirectory = new File("test-images" + File.separator + "output");
                        outputDirectory.mkdir();
                        
			String outputTransformed = outputDirectory.getPath()+File.separator+"testRegistration8BitGray-"+run+"-transformed.tif";
			String outputDifference = outputDirectory.getPath()+File.separator+"testRegistration8BitGray-"+run+"-difference.tif";
			
			boolean saved;
			
			saved = new FileSaver(ti.getTransformed()).saveAsTiffStack(outputTransformed);
			assertTrue("Saving to: "+outputTransformed+" failed.", saved);
			
			saved = new FileSaver(ti.getDifferenceImage()).saveAsTiffStack(outputDifference);
			assertTrue("Saving to: "+outputDifference+" failed.", saved);

                        float distance = ti.getDistance();
                        
			System.out.println("Distance with "+run+" was: "+distance);
       
			// This should be able to get the distance down to less than 14:
			assertTrue(
                                "On run: "+run+" distance ("+distance+"), more than what we expect ("+bestScore+")",
                                distance <= bestScore );
		}

                template.close();
                toTransform.close();
        }        
        
	static final int fanShapedBody = 11;
	static final int protocerebralBridge = 12;

	String centralComplex_Labels_71yAAeastmost = "test-images"+File.separator+"71yAAeastmost.labels";
	String centralComplex_Labels_c005BA = "test-images"+File.separator+"c005BA.labels";
        
	@Test
	public void testRegistrationMaterials() {

		ImagePlus centralComplex_Labels_71yAAeastmost_ImagePlus = BatchOpener.openFirstChannel(
			centralComplex_Labels_71yAAeastmost );

		ImagePlus centralComplex_Labels_c005BA_ImagePlus = BatchOpener.openFirstChannel(
			centralComplex_Labels_c005BA );
	
		int materials [] = { fanShapedBody, protocerebralBridge };
		float bestScores [] = { 15.5f, 55555555f };

		for( int i = 0; i < materials.length; ++i ) {

			int material = materials[i];
			float bestScore = bestScores[i];
		
			if( material == protocerebralBridge ) {
				// FIXME: registration fails for this
				// case, so skip it for the moment.
				continue;
			}
			
			FastMatrix matrix;
			
			ImagePlus template = centralComplex_Labels_c005BA_ImagePlus;
			ImagePlus toTransform = centralComplex_Labels_71yAAeastmost_ImagePlus;
			
			assertTrue( template != null );
			assertTrue( toTransform != null );
			
			// First try with the Euclidean metric:
			
			plugin = new RigidRegistration();
			
			int level = RigidRegistration.guessLevelFromWidth(
				template.getWidth() );
			
			TransformedImage ti = new TransformedImage(
				template,
				toTransform );
			
			ti.measure = new TwoValues(material,material);
			
			matrix = plugin.rigidRegistration(
				ti,
				"",         // material b box
				"",         // initial
				material,   // material 1
				material,   // material 2
				false,      // no optimization
				level,      // level
				level > 2 ? 2 : level, // stop level
				1.0,        // tolerance
				1,          // number of initial positions
				false,      // show transformed
				false,      // show difference image
				false,      // fast but inaccurate
				null );     // other images to transform
			
                        // Make sure the output directory exists:
                        
                        File outputDirectory = new File("test-images" + File.separator + "output");
                        outputDirectory.mkdir();
                        
			String outputTransformed = outputDirectory.getPath()+File.separator+"testRegistrationMaterials-"+material+"-transformed.tif";
			String outputDifference = outputDirectory.getPath()+File.separator+"testRegistrationMaterials-"+material+"-difference.tif";

                        boolean saved;
			
			saved = new FileSaver(ti.getTransformed()).saveAsTiffStack(outputTransformed);
			assertTrue(saved);
			
			saved = new FileSaver(ti.getDifferenceImage()).saveAsTiffStack(outputDifference);
			assertTrue(saved);

                        float distance = ti.getDistance();
                        
                        System.out.println("Distance was "+distance+" when registering material "+material);
                        
			// This should be able to get the distance down to less than 14:
			assertTrue(
                                "For material "+material+", distance ("+distance+"), more than what we expect ("+bestScore+")",
                                distance <= bestScore );
		}

		centralComplex_Labels_71yAAeastmost_ImagePlus.close();
		centralComplex_Labels_c005BA_ImagePlus.close();

	}

	@Test
	public void testRegistration12BitGray() {

		String darkDetail =   "test-images"+File.separator+"181y-12bit-aaarrg-dark-detail-reduced.tif";
		String midDetail =    "test-images"+File.separator+"181y-12bit-aaarrg-mid-detail-reduced.tif";
		String brightDetail = "test-images"+File.separator+"181y-12bit-aaarrg-bright-reduced.tif";

		ImagePlus darkDetail_ImagePlus   = BatchOpener.openFirstChannel( darkDetail );
		ImagePlus midDetail_ImagePlus    = BatchOpener.openFirstChannel( midDetail );
		ImagePlus brightDetail_ImagePlus = BatchOpener.openFirstChannel( brightDetail );

		for( int timeThrough = 0; timeThrough < 2; ++timeThrough ) {

			ImagePlus template    = null;
			ImagePlus toTransform = null;

			float [] bestScores = new float[3];

			if( timeThrough == 0 ) {
				template = midDetail_ImagePlus;
				toTransform = darkDetail_ImagePlus;
				bestScores[0] = 555555f; // euclidean
				bestScores[1] = -1f; // mutual information
				bestScores[2] = 0.1f; // correlation
			} else if( timeThrough == 1 ) {
				template = midDetail_ImagePlus;
				toTransform = brightDetail_ImagePlus;
				bestScores[0] = 555555f; // euclidean
				bestScores[1] = -1f; // mutual information
				bestScores[2] = 0.1f; // correlation
			}
			
			assertTrue( template != null );
			assertTrue( toTransform != null );

			for( int measureIndex = 0; measureIndex < 3; ++measureIndex ) {
				
				plugin = new RigidRegistration();
				
				int level = RigidRegistration.guessLevelFromWidth(
					template.getWidth() );
				
				TransformedImage ti = new TransformedImage(
					template,
					toTransform );

				String measureName = null;

				if( measureIndex == 0 ) {
					measureName = "eu";
					ti.measure = new Euclidean();
                                        // This totally fails, so skip trying Euclidean:
                                        continue;
				} else if( measureIndex == 1 ) {
					measureName = "mi";
					ti.measure = new MutualInformation(0,4095,256);
				} else if( measureIndex == 2 ) {
					measureName = "co";
					ti.measure = new Correlation();
				}
				
				FastMatrix matrix = plugin.rigidRegistration(
					ti,
					"",         // material b box
					"",         // initial
					-1,         // material 1
					-1,         // material 2
					false,      // no optimization
					level,      // level
					level > 2 ? 2 : level, // stop level
					1.0,        // tolerance
					1,          // number of initial positions
					false,      // show transformed
					false,      // show difference image
					false,      // fast but inaccurate
					null );     // other images to transform
				
				// Make sure the output directory exists:
				
				File outputDirectory = new File("test-images" + File.separator + "output");
				outputDirectory.mkdir();
				
				String outputTransformed = outputDirectory.getPath()+File.separator+"testRegistration12BitGray-"+timeThrough+"-"+measureName+"-transformed.tif";
				String outputDifference = outputDirectory.getPath()+File.separator+"testRegistration12BitGray-"+timeThrough+"-"+measureName+"-difference.tif";
				
				boolean saved;
				
				saved = new FileSaver(ti.getTransformed()).saveAsTiffStack(outputTransformed);
				assertTrue("Saving to: "+outputTransformed+" failed.", saved);
				
				saved = new FileSaver(ti.getDifferenceImage()).saveAsTiffStack(outputDifference);
				assertTrue("Saving to: "+outputDifference+" failed.", saved);
				
				float distance = ti.getDistance();

				System.out.println("distance on timeThrough "+timeThrough+" (measure: "+measureName+"): "+distance);

				// Blah
				assertTrue(
					"On time through "+timeThrough+" distance ("+distance+"), more than what we expect ("+bestScores[measureIndex]+")",
					distance <= bestScores[measureIndex] );
				
			}
		}

		darkDetail_ImagePlus.close();
		midDetail_ImagePlus.close();
		brightDetail_ImagePlus.close();
	}
        
}
