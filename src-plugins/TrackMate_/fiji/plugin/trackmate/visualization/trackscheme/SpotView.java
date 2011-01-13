package fiji.plugin.trackmate.visualization.trackscheme;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.jgraph.JGraph;
import org.jgraph.graph.VertexView;

import fiji.plugin.trackmate.Spot;


public class SpotView extends VertexView {

	private static final long serialVersionUID = 1L;
	

	private final JLabel label;
	private final Spot spot;


	public SpotView(SpotCell spotCell) {
		super();
		this.label 	= new JLabel();
		this.spot  	= spotCell.getSpot();
		this.cell 	= spotCell;
		initLabel();
	}
	
	public Spot getSpot() {
		return spot;
	}

	
	@Override
	public Component getRendererComponent(JGraph graph, boolean selected, boolean focus, boolean preview) {
		if (selected)
			label.setForeground(Color.RED);
		else 
			label.setForeground(Color.BLACK);
		return label;
	}
	

	private void initLabel() {
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setIcon(spot.getIcon());
		String name = spot.getName();
		if (name == null || name.equals(""))
			name = "<no name>";
 		label.setText(name);
	}

	
}
