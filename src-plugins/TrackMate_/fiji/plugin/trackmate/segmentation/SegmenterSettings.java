package fiji.plugin.trackmate.segmentation;

import fiji.plugin.trackmate.gui.BasicSegmenterConfigurationPanel;
import fiji.plugin.trackmate.gui.SegmenterConfigurationPanel;


/** 
 * Mother class for spot segmenter settings, to pass settings to the concrete 
 * implementations of {@link SpotSegmenter}s.
 * <p>
 * The concrete derivation of this class should be matched to the concrete implementation
 * of {@link SpotSegmenter}, and contain only public fields.
 * Default fields are provided in this class, that are generic enough to be of use for
 * most spot segmenters.
 * <p>
 * There is a bit of a edgy part: the {@link #createConfurationPanel()} method. It 
 * links a GUI object (the panel) to this settings object. This is the only
 * way I could come with - yet - in order to have a generic segmenter framework,
 * with objects having methods that can generate the whole context needed to
 * configure them. A more clever approach might be investigated. 
 *
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2010, 2011
 *
 */
public class SegmenterSettings {
	
	private static final float DEFAULT_EXPECTED_DIAMETER	= 10f;

	/** The expected spot diameter in physical units. */
	public float 	expectedRadius = DEFAULT_EXPECTED_DIAMETER/2;
	
	/*
	 * METHODS
	 */
	
	@Override
	public String toString() {
		String str = "";
		str += String.format("  Expected radius: %f\n", expectedRadius);
		return str;
	}
	
	/**
	 * @return  an GUI panel that is able to configure this concrete settings object.
	 */
	public SegmenterConfigurationPanel createConfigurationPanel() {
		return new BasicSegmenterConfigurationPanel();
	}
	
}
