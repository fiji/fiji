package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.threedviewer.SpotDisplayer3D;

public class ViewProvider {

	/** The view names, in the order they will appear in the GUI.
	 * These names will be used as keys to access relevant view classes.  */
	protected List<String> names;
	protected final TrackMateModel model;

	/*
	 * BLANK CONSTRUCTOR
	 */

	/**
	 * This provider provides the GUI with the model views currently available in the 
	 * TrackMate plugin. Each view is identified by a key String, which can be used 
	 * to retrieve new instance of the view.
	 * <p>
	 * If you want to add custom views to TrackMate, a simple way is to extend this
	 * factory so that it is registered with the custom views and provide this 
	 * extended factory to the {@link TrackMate_} plugin.
	 */
	public ViewProvider(TrackMateModel model) {
		this.model = model;
		registerViews();
	}


	/*
	 * METHODS
	 */

	/**
	 * Register the standard views shipped with TrackMate.
	 */
	protected void registerViews() {
		// Names
		names = new ArrayList<String>(2);
		names.add(HyperStackDisplayer.NAME);
		names.add(SpotDisplayer3D.NAME);
	}

	/**
	 * @return a new instance of the target view identified by the key parameter. 
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */
	public TrackMateModelView getView(String key) {
		int index = names.indexOf(key);
		if (index < 0) {
			return null;
		}
		switch (index) {
		case 0:
			return new HyperStackDisplayer(model);
		case 1:
			return new SpotDisplayer3D(model);
		default:
			return null;
		}
	}

	/**
	 * @return a list of the view names available through this factory.
	 */
	public List<String> getAvailableViews() {
		return names;
	}

	/**
	 * @return the html String containing a descriptive information about the target view,
	 * or <code>null</code> if it is unknown to this factory.
	 */
	public String getInfoText(String key) {
		int index = names.indexOf(key);
		if (index < 0) {
			return null;
		}
		switch (index) {
		case 0:
			return HyperStackDisplayer.INFO_TEXT;
		case 1:
			return SpotDisplayer3D.INFO_TEXT;
		default:
			return null;
		}
	}

}
