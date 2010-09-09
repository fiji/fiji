package fiji.plugin.spottracker.gui.test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import fiji.plugin.spottracker.Feature;
import fiji.plugin.spottracker.Spot;
import fiji.plugin.spottracker.features.FeatureFacade;
import fiji.plugin.spottracker.gui.SpotDisplayer2D;
import fiji.plugin.spottracker.gui.ThresholdGuiPanel;
import fiji.plugin.spottracker.segmentation.SpotSegmenter;
import ij.IJ;
import ij.ImagePlus;

public class SpotDisplayer2DTestDrive {

	public static void main(String[] args) {
		
//		ij.ImageJ.main(args);
		
		ImagePlus imp = IJ.openImage("http://rsb.info.nih.gov/ij/images/blobs.gif");
		imp.show();
		final float DIAMETER = 20;
		
		Image<UnsignedByteType> img =  ImagePlusAdapter.wrapByte(imp);
		SpotSegmenter<UnsignedByteType> segmenter = new SpotSegmenter<UnsignedByteType>(img, DIAMETER);
		Collection<Spot> spots;
		System.out.println("Segmenting...");
		if (segmenter.checkInput() && segmenter.process())
			spots = segmenter.getResult();
		else {
			System.out.println("Problem with segmentation:\n"+segmenter.getErrorMessage());
			return;
		}
		System.out.println("Segmentation done. Found "+spots.size()+" spots.");
		
		TreeMap<Integer, Collection<Spot>> allSpots = new TreeMap<Integer, Collection<Spot>>();
		allSpots.put(0, spots);
		
		System.out.println("Calculating features..");
		FeatureFacade<UnsignedByteType> featureCalculator = new FeatureFacade<UnsignedByteType>(img, segmenter.getFilteredImage(), DIAMETER);
		featureCalculator.processFeature(Feature.MEAN_INTENSITY, spots);
		System.out.println("Features done.");
		
		final SpotDisplayer2D displayer = new SpotDisplayer2D(allSpots, imp, DIAMETER/2);
		displayer.render();
		
		System.out.println("Starting threshold GUI...");
		final ThresholdGuiPanel gui = new ThresholdGuiPanel();
		gui.setSpots(allSpots.values());
		gui.addThresholdPanel(Feature.MEAN_INTENSITY);
		JFrame frame = new JFrame();
		frame.getContentPane().add(gui);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		System.out.println("Done.");
		
		gui.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				displayer.refresh(gui.getFeatures(), gui.getThresholds(), gui.getIsAbove());
			}
		});
		
	}

}
