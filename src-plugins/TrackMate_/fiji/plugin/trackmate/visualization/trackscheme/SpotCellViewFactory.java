package fiji.plugin.trackmate.visualization.trackscheme;

import java.awt.Image;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;

import org.jgraph.graph.DefaultCellViewFactory;
import org.jgraph.graph.VertexView;

public class SpotCellViewFactory extends DefaultCellViewFactory {

	private static final long serialVersionUID = 1L;
	private static final float DEFAULT_RADIUS  = 5;
	private static final float ENLARGE_FACTOR = 1.1f;
	private float[] calibration;
	private ImagePlus imp;
	private float radius;

	/*
	 * CONSTRUCTORS
	 */

	public SpotCellViewFactory() {
		this(null);
	}
	
	public SpotCellViewFactory(ImagePlus imp) {
		this(imp, DEFAULT_RADIUS);
	}
	
	public SpotCellViewFactory(ImagePlus imp, float radius) {
		this(imp, radius, null);
	}
	
	public SpotCellViewFactory(ImagePlus imp, float radius, float[] calibration) {
		if (null == calibration) {
			if (null == imp)
				this.calibration = new float[] {1, 1, 1};
			else
				this.calibration = new float[] {
					(float) imp.getCalibration().pixelWidth, 
					(float) imp.getCalibration().pixelHeight, 
					(float) imp.getCalibration().pixelDepth };
			} else 
				this.calibration = calibration;
		this.imp = imp;	
		this.radius = radius;
	}

	
	/*
	 * METHODS
	 */
	
	@Override
	protected VertexView createVertexView(Object cell) {
		SpotCell spotCell = (SpotCell) cell;
		if (null == imp)
			return new SpotView(spotCell);
		// Grab image since we can
		Spot spot = spotCell.getSpot();		
		int slice = 1;
		if (calibration.length > 2)
			slice = Math.round(spot.getFeature(Feature.POSITION_Z) / calibration[2]) + 1;
		int frame = Math.round(spot.getFeature(Feature.POSITION_T)) + 1;
		int index = imp.getStackIndex(1, slice, frame);
		int x = Math.round((spot.getFeature(Feature.POSITION_X) - ENLARGE_FACTOR*radius) / calibration[0]); 
		int y = Math.round((spot.getFeature(Feature.POSITION_Y) - ENLARGE_FACTOR*radius) / calibration[1]);
		int width = Math.round(2 * ENLARGE_FACTOR * radius / calibration[0]);
		int height = Math.round(2 * ENLARGE_FACTOR * radius / calibration[1]);
		Roi roi = new Roi(x, y, width, height);
		ImageProcessor ip = imp.getStack().getProcessor(index).duplicate();
		ip.setRoi(roi);
		ip = ip.crop();
		Image image = ip.createImage();
		ImageIcon icon = new ImageIcon(image);
		return new SpotView(spotCell, icon);
	}
	
}
