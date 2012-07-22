package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.action.CaptureOverlayAction;
import fiji.plugin.trackmate.action.CopyOverlayAction;
import fiji.plugin.trackmate.action.ExportTracksToXML;
import fiji.plugin.trackmate.action.ExtractTrackStackAction;
import fiji.plugin.trackmate.action.GrabSpotImageAction;
import fiji.plugin.trackmate.action.LinkNew3DViewerAction;
import fiji.plugin.trackmate.action.PlotNSpotsVsTimeAction;
import fiji.plugin.trackmate.action.RadiusToEstimatedAction;
import fiji.plugin.trackmate.action.RecalculateFeatureAction;
import fiji.plugin.trackmate.action.ResetRadiusAction;
import fiji.plugin.trackmate.action.ResetSpotTimeFeatureAction;
import fiji.plugin.trackmate.action.TrackMateAction;

public class ActionFactory <T extends RealType<T> & NativeType<T>> {

	/** The detector names, in the order they will appear in the GUI.
	 * These names will be used as keys to access relevant detecrtor classes.  */
	protected List<String> names;
	
	/*
	 * BLANK CONSTRUCTOR
	 */
	
	/**
	 * This factory provides the GUI with the TrackMate actions currently available in the 
	 * TrackMate plugin. Each detector is identified by a key String, which can be used 
	 * to retrieve new instance of the action.
	 * <p>
	 * If you want to add custom actions to TrackMate, a simple way is to extend this
	 * factory so that it is registered with the custom actions and provide this 
	 * extended factory to the {@link TrackMate_} plugin.
	 */
	public ActionFactory() {
		registerActions();
	}
	
	
	/*
	 * METHODS
	 */
	
	/**
	 * Register the standard actions shipped with TrackMate.
	 */
	protected void registerActions() {
		// Names
		names = new ArrayList<String>(10);
		names.add(GrabSpotImageAction.NAME);
		names.add(ExtractTrackStackAction.NAME);
		names.add(LinkNew3DViewerAction.NAME);
		names.add(CopyOverlayAction.NAME);
		names.add(PlotNSpotsVsTimeAction.NAME);
		names.add(CaptureOverlayAction.NAME);
		names.add(ResetSpotTimeFeatureAction.NAME);
		names.add(RecalculateFeatureAction.NAME);
		names.add(RadiusToEstimatedAction.NAME);
		names.add(ResetRadiusAction.NAME);
//		names.add(fiji.plugin.trackmate.action.ISBIChallengeExporter.NAME);
		names.add(ExportTracksToXML.NAME);
	}
	
	/**
	 * @return a new instance of the target action identified by the key parameter. 
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */
	public TrackMateAction<T> getAction(String key) {
		int index = names.indexOf(key);
		if (index < 0) {
			return null;
		}
		switch (index) {
		case 0:
			return new GrabSpotImageAction<T>();
		case 1:
			return new ExtractTrackStackAction<T>();
		case 2:
			return new LinkNew3DViewerAction<T>();
		case 3:
			return new CopyOverlayAction<T>();
		case 4:
			return new PlotNSpotsVsTimeAction<T>();
		case 5:
			return new CaptureOverlayAction<T>();
		case 6:
			return new ResetSpotTimeFeatureAction<T>();
		case 7:
			return new RecalculateFeatureAction<T>();
		case 8: 
			return new RadiusToEstimatedAction<T>();
		case 9:
			return new ResetRadiusAction<T>();
		case 10:
			return new ExportTracksToXML<T>();
		default:
			return null;
		}
	}
	
	
	/**
	 * @return the html String containing a descriptive information about the target action,
	 * or <code>null</code> if it is unknown to this factory.
	 */
	public String getInfoText(String key) {
		int index = names.indexOf(key);
		if (index < 0) {
			return null;
		}
		switch (index) {
		case 0:
			return GrabSpotImageAction.INFO_TEXT;
		case 1:
			return ExtractTrackStackAction.INFO_TEXT;
		case 2:
			return LinkNew3DViewerAction.INFO_TEXT;
		case 3:
			return CopyOverlayAction.INFO_TEXT;
		case 4:
			return PlotNSpotsVsTimeAction.INFO_TEXT;
		case 5:
			return CaptureOverlayAction.INFO_TEXT;
		case 6:
			return ResetSpotTimeFeatureAction.INFO_TEXT;
		case 7:
			return RecalculateFeatureAction.INFO_TEXT;
		case 8: 
			return RadiusToEstimatedAction.INFO_TEXT;
		case 9:
			return ResetRadiusAction.INFO_TEXT;
		case 10:
			return ExportTracksToXML.INFO_TEXT;
		default:
			return null;
		}
	}
	
	/**
	 * @return the descriptive icon for target action,
	 * or <code>null</code> if it is unknown to this factory.
	 */
	public ImageIcon getIcon(String key) {
		int index = names.indexOf(key);
		if (index < 0) {
			return null;
		}
		switch (index) {
		case 0:
			return GrabSpotImageAction.ICON;
		case 1:
			return ExtractTrackStackAction.ICON;
		case 2:
			return LinkNew3DViewerAction.ICON;
		case 3:
			return CopyOverlayAction.ICON;
		case 4:
			return PlotNSpotsVsTimeAction.ICON;
		case 5:
			return CaptureOverlayAction.ICON;
		case 6:
			return ResetSpotTimeFeatureAction.ICON;
		case 7:
			return RecalculateFeatureAction.ICON;
		case 8: 
			return RadiusToEstimatedAction.ICON;
		case 9:
			return ResetRadiusAction.ICON;
		case 10:
			return ExportTracksToXML.ICON;
		default:
			return null;
		}
	}

	/**
	 * @return a list of the detector names available through this factory.
	 */
	public List<String> getAvailableActions() {
		return names;
	}

}
