package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;

import java.awt.Color;
import java.awt.Component;

import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import org.jgraph.JGraph;
import org.jgraph.graph.VertexView;

import fiji.plugin.trackmate.Spot;


public class SpotView extends VertexView {

	private static final long serialVersionUID = 1L;
	private Color color;


	public SpotView(SpotCell spotCell) {
		super(spotCell);
	}
	
	public Spot getSpot() {
		return ((SpotCell) cell).getSpot();
	}

	
	@Override
	public Component getRendererComponent(JGraph graph, boolean selected, boolean focus, boolean preview) {
		renderer.setHorizontalAlignment(SwingConstants.LEFT);
		renderer.setIcon(getSpot().getIcon());
		renderer.setBorder(new LineBorder(color, 2));
		renderer.setFont(FONT);
		String name = getSpot().getName();
		if (name == null || name.equals(""))
			name = "<no name>";
		renderer.setText(name);
		if (selected)
			renderer.setForeground(Color.RED);
		else 
			renderer.setForeground(Color.BLACK);
		return renderer;
	}

	public void setColor(Color color) {
		this.color = color;		
	}
}
