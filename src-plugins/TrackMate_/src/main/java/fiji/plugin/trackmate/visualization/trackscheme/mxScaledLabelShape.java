package fiji.plugin.trackmate.visualization.trackscheme;

import java.awt.Image;
import java.awt.Rectangle;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.shape.mxRectangleShape;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;

/**
 * This is a shape that is made to display a cell in a way that suits for our 
 * spots objects. It displays an image on the left, that scales with the cell dimension,
 * and a label on the right.
 *<p>
 * We re-used the JGraphX classes as far as we could. It turned out we
 * only need to recalculate the image bounds to have them scaling with 
 * the cell size.
 *   
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Mar 2011 - 2012
 */
public class mxScaledLabelShape extends mxRectangleShape {

	public static final String SHAPE_NAME = "scaledLabel";
	
	@Override
	public void paintShape(mxGraphics2DCanvas canvas, mxCellState state) {
		
		super.paintShape(canvas, state);
		
		String imgStr = mxUtils.getString(state.getStyle(), mxConstants.STYLE_IMAGE);
		if (imgStr != null)  {
			Image img = canvas.loadImage(mxUtils.getString(state.getStyle(), mxConstants.STYLE_IMAGE));
			if (img != null) {
				Rectangle bounds = getImageBounds(canvas, state);
				int x = bounds.x;
				int y = bounds.y;
				int w = bounds.width;
				int h = bounds.height;
				if (h > 0 && w > 0) {
					Image scaledImage = img.getScaledInstance(w, h, Image.SCALE_FAST);
					canvas.getGraphics().drawImage(scaledImage, x, y, null);
				}
			}
		}
	}

	private final Rectangle getImageBounds(mxGraphics2DCanvas canvas, mxCellState state) {
		Rectangle cellR = state.getRectangle();
		int arc = getArcSize(cellR.width, cellR.height) / 2;
		int minSize = Math.min(cellR.width - arc*2, cellR.height - 4);
		Rectangle imageBounds = new Rectangle(cellR.x + arc, cellR.y+2, minSize, minSize);
		return imageBounds;
	}

}
