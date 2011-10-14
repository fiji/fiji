package fiji.plugin.trackmate.segmentation;

/** 
 * Mother interface for spot segmenter settings, to pass settings to the concrete 
 * implementations of {@link SpotSegmenter}s.
 * <p>
 * The concrete derivation of this class should be matched to the concrete implementation
 * of {@link SpotSegmenter}, and contain only public fields.
 * <p>
 * There is a bit of a edgy part: the {@link #createConfurationPanel()} method. It 
 * links a GUI object (the panel) to this settings object. Also, a settings object
 * should be able to save itself to xml, so there is a   
 * 
 * This is the only
 * way I could come with - yet - in order to have a generic segmenter framework,
 * with objects having methods that can generate the whole context needed to
 * configure them. A more clever approach might be investigated. 
 *
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2010, 2011
 *
 */
public interface SegmenterSettings {

}
