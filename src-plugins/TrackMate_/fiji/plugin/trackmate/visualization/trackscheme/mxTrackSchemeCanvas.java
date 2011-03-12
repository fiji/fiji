package fiji.plugin.trackmate.visualization.trackscheme;

import java.awt.Image;
import java.awt.Rectangle;
import java.util.Map;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.view.mxInteractiveCanvas;
import com.mxgraph.view.mxCellState;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.trackscheme.SpotCellViewFactory.SpotCell;

public class mxTrackSchemeCanvas extends mxInteractiveCanvas {

	
	protected mxGraphComponent graphComponent;

	public mxTrackSchemeCanvas(mxGraphComponent graphComponent) {
		this.graphComponent = graphComponent;
	}

	@Override
	public Object drawCell(mxCellState state) {
		if (state.getCell() instanceof SpotCell) {
			return drawSpotCell(state, (SpotCell) state.getCell());
		}
		return super.drawCell(state);
	};


	public Object drawSpotCell(mxCellState state, SpotCell cell)	{
		Map<String, Object> style = state.getStyle();
		
		// Paint mxCell shape
		getShape(style).paintShape(this, state);
		
		// Paint icon
		Spot spot = cell.getSpot();
		Image img = spot.getIcon().getImage();
		Rectangle bounds = state.getRectangle();
		int iscale = (int) scale; // We will add / subtract this amount to fit well in rectangle shape
		int x = bounds.x + iscale / 2;
		int y = bounds.y - iscale / 2;
		double iw = img.getWidth(null) - scale;
		double ih = img.getHeight(null) - scale;
		double s = Math.min(bounds.width / iw, bounds.height / ih);
		int w = (int) (iw * s);
		int h = (int) (ih * s);
		y += (bounds.height - h) / 2;
		g.drawImage(img, x, y, w, h,  null);
		
		// Paint spot name
		String str = " " + ((spot.getName() == null || spot.getName().equals("")) ? "ID "+spot.ID() : spot.getName());
		getTextShape(style, false).paintShape(this, str, state, style);
		
		return null;
	}


}
