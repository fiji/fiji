package fiji.plugin.trackmate.action;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ImageIcon;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.features.spot.SpotIconGrabber;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.util.TMUtils;

public class ExtractTrackStackAction extends AbstractTMAction {

	private static final ImageIcon ICON = new ImageIcon(TrackMateWizard.class.getResource("images/magnifier.png"));
	/** By how much we resize the capture window to get a nice border around the spot. */
	private static final float RESIZE_FACTOR = 1.5f;
	
	
	
	/*
	 * CONSTRUCTOR
	 */

	public ExtractTrackStackAction() {
		this.icon = ICON;
	}

	/*
	 * METHODS
	 */
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void execute(TrackMate_ plugin) {
		logger.log("Capturing track stack.\n");
		
		TrackMateModel model = plugin.getModel();
		Set<Spot> selection = model.getSpotSelection();
		int nspots = selection.size();
		if (nspots != 2) {
			logger.error("Expected 2 spots in the selection, got "+nspots+".\nAborting.\n");
			return;
		}
		
		// Get start & end
		Spot tmp1, tmp2, start, end;
		Iterator<Spot> it = selection.iterator();
		tmp1 = it.next();
		tmp2 = it.next();
		if (tmp1.getFeature(Spot.POSITION_T) > tmp2.getFeature(Spot.POSITION_T)) {
			end = tmp1;
			start = tmp2;
		} else {
			end = tmp2;
			start = tmp1;
		}
		
		// Find path
		List<DefaultWeightedEdge> edges = model.dijkstraShortestPath(start, end);
		if (null == edges) {
			logger.error("The 2 spots are not connected.\nAborting\n");
			return;
		}
		
		// Build spot list
		// & Get largest diameter
		List<Spot> path = new ArrayList<Spot>(edges.size());
		path.add(start);
		Spot previous = start;
		Spot current;
		float radius = Math.abs(start.getFeature(Spot.RADIUS));
		for (DefaultWeightedEdge edge : edges) {
			
			current = model.getEdgeSource(edge);
			if (current == previous) {
				current = model.getEdgeTarget(edge); // We have to check both in case of bad oriented edges
			}
			path.add(current);
			float ct = Math.abs(current.getFeature(Spot.RADIUS));
			if (ct > radius) {
				radius = ct;
			}
			previous = current;
		}
		path.add(end);
		
		// Sort spot by ascending frame number
		TreeSet<Spot> sortedSpots = new TreeSet<Spot>(Spot.frameComparator);
		sortedSpots.addAll(path);
		nspots = sortedSpots.size();

		// Common coordinates
		Settings settings = model.getSettings();
		float[] calibration = settings.getCalibration();
		final int targetChannel = settings.imp.getChannel(); // We do this for current channel
		final int width 	= (int) Math.ceil(2 * radius * RESIZE_FACTOR / calibration[0]);
		final int height 	= (int) Math.ceil(2 * radius * RESIZE_FACTOR / calibration[0]);
		
		// Prepare new image holder:
		ImageStack stack = new ImageStack(width, height);
		
		// Iterate over set to grab imglib image
		int zpos = 0;
		for (Spot spot : sortedSpots) {
			
			// Extract image for current frame
			int frame = model.getSpots().getFrame(spot);
			Image img = TMUtils.getUncroppedSingleFrameAsImage(settings.imp, frame, targetChannel);
			
			// Compute target coordinates for current spot
			int x = Math.round((spot.getFeature(Spot.POSITION_X)) / calibration[0]) - width/2; 
			int y = Math.round((spot.getFeature(Spot.POSITION_Y)) / calibration[1]) - height/2;
			int slice = 0;
			if (img.getNumDimensions() > 2) {
				slice = Math.round(spot.getFeature(Spot.POSITION_Z) / calibration[2]);
				if (slice < 0) {
					slice = 0;
				}
				if (slice >= img.getDimension(2)) {
					slice = img.getDimension(2) -1;
				}
			}
			
			SpotIconGrabber grabber = new SpotIconGrabber();
			grabber.setTarget(img, calibration);
			Image crop = grabber.grabImage(x, y, slice, width, height);
			
			// Copy to central holder
			stack.addSlice(spot.toString(), ImageJFunctions.copyToImagePlus(crop).getProcessor());
			
			logger.setProgress((float) (zpos + 1) / nspots);
			zpos++;
		
		}
		
		// Convert to plain ImageJ
		ImagePlus stackTrack = new ImagePlus("", stack);
		stackTrack.setTitle("Path from "+start+" to "+end);
		Calibration impCal = stackTrack.getCalibration();
		impCal.setTimeUnit(settings.imp.getCalibration().getTimeUnit());
		impCal.setUnit(settings.imp.getCalibration().getUnit());
		impCal.pixelWidth 		= settings.imp.getCalibration().pixelWidth;
		impCal.pixelHeight 		= settings.imp.getCalibration().pixelHeight;
		impCal.frameInterval 	= settings.imp.getCalibration().frameInterval;
		stackTrack.setDimensions(1, 1, nspots);

		//Display it
		stackTrack.show();
		stackTrack.resetDisplayRange();

	}

	@Override
	public String getInfoText() {
		return "<html> " +
				"Generate a stack of images taken from the track " +
				"that joins two selected spots. " +
				"<p>" +
				"There must be exactly 2 spots selected for this action " +
				"to work, and they must belong to a track that connects " +
				"them." +
				"<p>" +
				"A stack of images will be generated from the spots that join " +
				"them, defining the image size with the largest spot encountered. " +
				"The central spot slice is taken in case of 3D data." +
				"</html>";
	}
	
	@Override
	public String toString() {
		return "Extract track stack";
	}

}
