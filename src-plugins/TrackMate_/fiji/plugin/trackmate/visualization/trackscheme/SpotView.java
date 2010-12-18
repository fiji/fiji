package fiji.plugin.trackmate.visualization.trackscheme;

import java.awt.Color;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.jgraph.JGraph;
import org.jgraph.graph.VertexView;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.TrackMateFrame;


public class SpotView extends VertexView {

	private static final long serialVersionUID = 1L;
	private static final String ADD_ICON = "images/add.png";
	private static final String REMOVE_ICON = "images/delete.png";

	private final JLabel label;
	private final Spot spot;

	public SpotView(final SpotCell cell) {
		super();
		this.label = new JLabel("----");
		this.spot  = cell.getSpot();
		this.cell  = cell;
	}

	@Override
	public Component getRendererComponent(JGraph graph, boolean selected, boolean focus, boolean preview) {
		label.setText("t="+spot.getFeature(Feature.POSITION_T));
		if (selected)
			label.setForeground(Color.RED);
		else 
			label.setForeground(Color.BLACK);
		label.setIcon(new ImageIcon(TrackMateFrame.class.getResource(ADD_ICON)));
		return label;
	}

	
}
