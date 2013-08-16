package fiji.plugin.trackmate.providers;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.CaptureOverlayAction;
import fiji.plugin.trackmate.action.CopyOverlayAction;
import fiji.plugin.trackmate.action.ExportTracksToXML;
import fiji.plugin.trackmate.action.ExtractTrackStackAction;
import fiji.plugin.trackmate.action.ISBIChallengeExporter;
import fiji.plugin.trackmate.action.LinkNew3DViewerAction;
import fiji.plugin.trackmate.action.PlotNSpotsVsTimeAction;
import fiji.plugin.trackmate.action.RadiusToEstimatedAction;
import fiji.plugin.trackmate.action.RecalculateFeatureAction;
import fiji.plugin.trackmate.action.ResetRadiusAction;
import fiji.plugin.trackmate.action.ResetSpotTimeFeatureAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.gui.TrackMateGUIController;

public class ActionProvider {

	/** The action names, in the order they will appear in the GUI.
	 * These names will be used as keys to access relevant action classes.  */
	protected List<String> names;
	protected final TrackMate trackmate;
	protected final TrackMateGUIController controller;
	
	/*
	 * BLANK CONSTRUCTOR
	 */
	
	/**
	 * This provider provides the GUI with the TrackMate actions currently available in  
	 * TrackMate. Each action is identified by a key String, which can be used 
	 * to retrieve new instance of the action.
	 * <p>
	 * If you want to add custom actions to TrackMate, a simple way is to extend this
	 * factory so that it is registered with the custom actions and provide this 
	 * extended factory to the {@link TrackMate} trackmate.
	 * 
	 * @param trackmate  the {@link TrackMate} instance these actions will operate on. 
	 * @param guiController  the {@link TrackMateGUIController} controller that controls the GUI 
	 * these actions are launched from.
	 */
	public ActionProvider(TrackMate trackmate, TrackMateGUIController controller) {
		this.trackmate = trackmate;
		this.controller = controller;
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
		names.add(ExportTracksToXML.NAME);
		names.add(ExtractTrackStackAction.NAME);
		names.add(LinkNew3DViewerAction.NAME);
		names.add(CopyOverlayAction.NAME);
		names.add(PlotNSpotsVsTimeAction.NAME);
		names.add(CaptureOverlayAction.NAME);
//		names.add(ResetSpotTimeFeatureAction.NAME);
		names.add(RecalculateFeatureAction.NAME);
		names.add(RadiusToEstimatedAction.NAME);
		names.add(ResetRadiusAction.NAME);
//		names.add(fiji.plugin.trackmate.action.ISBIChallengeExporter.NAME);
	}
	
	/**
	 * Returns a new instance of the target action identified by the key parameter. 
	 * If the key is unknown to this factory, <code>null</code> is returned.
	 * @return a new {@link TrackMateAction}. 
	 */
	public TrackMateAction getAction(String key) {
		
		if (ExtractTrackStackAction.NAME.equals(key)) {
			return new ExtractTrackStackAction(trackmate, controller);
			
		} else if (LinkNew3DViewerAction.NAME.equals(key)) {
			return new LinkNew3DViewerAction(trackmate, controller);
			
		} else if (CopyOverlayAction.NAME.equals(key)) {
			return new CopyOverlayAction(trackmate, controller);
			
		} else if (PlotNSpotsVsTimeAction.NAME.equals(key))	{
			return new PlotNSpotsVsTimeAction(trackmate, controller);
			
		} else if (CaptureOverlayAction.NAME.equals(key)) {
			return new CaptureOverlayAction(trackmate, controller);
			
		} else if (ResetSpotTimeFeatureAction.NAME.equals(key)) {
			return new ResetSpotTimeFeatureAction(trackmate, controller);
			
		} else if (RecalculateFeatureAction.NAME.equals(key)) {
			return new RecalculateFeatureAction(trackmate, controller);
			
		} else if (RadiusToEstimatedAction.NAME.equals(key)) {
			return new RadiusToEstimatedAction(trackmate, controller);
			
		} else if (ResetRadiusAction.NAME.equals(key)) { 
			return new ResetRadiusAction(trackmate, controller);
			
		} else if (ExportTracksToXML.NAME.equals(key)) {
			return new ExportTracksToXML(trackmate, controller);
			
		} else if (ISBIChallengeExporter.NAME.equals(key)) {
			return new ISBIChallengeExporter(trackmate, controller);
		}
		
		return null;
	}
	
	
	/**
	 * @return the html String containing a descriptive information about the target action,
	 * or <code>null</code> if it is unknown to this factory.
	 */
	public String getInfoText(String key) {
		if (ExtractTrackStackAction.NAME.equals(key)) {
			return ExtractTrackStackAction.INFO_TEXT;
			
		} else if (LinkNew3DViewerAction.NAME.equals(key)) {
			return LinkNew3DViewerAction.INFO_TEXT;
			
		} else if (CopyOverlayAction.NAME.equals(key)) {
			return CopyOverlayAction.INFO_TEXT;
			
		} else if (PlotNSpotsVsTimeAction.NAME.equals(key))	{
			return PlotNSpotsVsTimeAction.INFO_TEXT;
			
		} else if (CaptureOverlayAction.NAME.equals(key)) {
			return CaptureOverlayAction.INFO_TEXT;
			
		} else if (ResetSpotTimeFeatureAction.NAME.equals(key)) {
			return ResetSpotTimeFeatureAction.INFO_TEXT;
			
		} else if (RecalculateFeatureAction.NAME.equals(key)) {
			return RecalculateFeatureAction.INFO_TEXT;
			
		} else if (RadiusToEstimatedAction.NAME.equals(key)) {
			return RadiusToEstimatedAction.INFO_TEXT;
			
		} else if (ResetRadiusAction.NAME.equals(key)) { 
			return ResetRadiusAction.INFO_TEXT;
			
		} else if (ExportTracksToXML.NAME.equals(key)) {
			return ExportTracksToXML.INFO_TEXT;
		}
		
		return null;
	}
	
	/**
	 * @return the descriptive icon for target action,
	 * or <code>null</code> if it is unknown to this factory.
	 */
	public ImageIcon getIcon(String key) {
		if (ExtractTrackStackAction.NAME.equals(key)) {
			return ExtractTrackStackAction.ICON;
			
		} else if (LinkNew3DViewerAction.NAME.equals(key)) {
			return LinkNew3DViewerAction.ICON;
			
		} else if (CopyOverlayAction.NAME.equals(key)) {
			return CopyOverlayAction.ICON;
			
		} else if (PlotNSpotsVsTimeAction.NAME.equals(key))	{
			return PlotNSpotsVsTimeAction.ICON;
			
		} else if (CaptureOverlayAction.NAME.equals(key)) {
			return CaptureOverlayAction.ICON;
			
		} else if (ResetSpotTimeFeatureAction.NAME.equals(key)) {
			return ResetSpotTimeFeatureAction.ICON;
			
		} else if (RecalculateFeatureAction.NAME.equals(key)) {
			return RecalculateFeatureAction.ICON;
			
		} else if (RadiusToEstimatedAction.NAME.equals(key)) {
			return RadiusToEstimatedAction.ICON;
			
		} else if (ResetRadiusAction.NAME.equals(key)) { 
			return ResetRadiusAction.ICON;
			
		} else if (ExportTracksToXML.NAME.equals(key)) {
			return ExportTracksToXML.ICON;
		}
		
		return null;
	}

	/**
	 * @return a list of the detector names available through this factory.
	 */
	public List<String> getAvailableActions() {
		return names;
	}

}
