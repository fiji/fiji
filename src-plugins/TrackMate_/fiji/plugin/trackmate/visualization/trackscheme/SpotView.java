package fiji.plugin.trackmate.visualization.trackscheme;

import java.awt.Color;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.jgraph.JGraph;
import org.jgraph.graph.VertexView;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;


public class SpotView extends VertexView {

	private static final long serialVersionUID = 1L;

	private final JLabel label;
	private final Spot spot;
	private ImageIcon icon;

	public SpotView(final SpotCell spotCell) {
		this(spotCell, null);
	}

	public SpotView(SpotCell spotCell, ImageIcon icon) {
		super();
		this.label 	= new JLabel();
		this.spot  	= spotCell.getSpot();
		this.cell 	= spotCell;
		this.icon 	= icon;
	}

	@Override
	public Component getRendererComponent(JGraph graph, boolean selected, boolean focus, boolean preview) {
		label.setText("t="+spot.getFeature(Feature.POSITION_T));
		if (selected)
			label.setForeground(Color.RED);
		else 
			label.setForeground(Color.BLACK);
		label.setIcon(icon);
		return label;
	}

	
}
