package fiji.plugin.trackmate.visualization.trackscheme;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.model.mxCell;
import com.mxgraph.shape.mxConnectorShape;
import com.mxgraph.shape.mxDefaultTextShape;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;

/**
 * A text shape that draws edge label along the edge line and parallel to it.
 * Works only if the chosen edge style is {@link mxConnectorShape}.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Jun 30, 2011
 *
 */
public class mxSideTextShape extends mxDefaultTextShape {

	public static final String SHAPE_NAME = "sideLabel"; 
	public static final String STYLE_DISPLAY_COST = "displayCost";
	
	
	public mxSideTextShape() {}
	

	@Override
	public void paintShape(mxGraphics2DCanvas canvas, String text, mxCellState state, Map<String, Object> style) {
		
		mxCell cell = (mxCell) state.getCell();
		boolean isEdgeLabel = cell.isEdge(); 

		Rectangle rect = state.getLabelBounds().getRectangle();
		Graphics2D g = canvas.getGraphics();

		if (g.getClipBounds() == null || g.getClipBounds().intersects(rect)) {
			boolean horizontal = mxUtils.isTrue(style,
					mxConstants.STYLE_HORIZONTAL, true);
			double scale = canvas.getScale();
			int x = rect.x;
			int y = rect.y;
			int w = rect.width;
			int h = rect.height;

			if (isEdgeLabel) {
				
				if (!mxUtils.isTrue(style, STYLE_DISPLAY_COST, false))
					return;
				
				List<mxPoint> points = state.getAbsolutePoints();
				double px1 = points.get(0).getX();
				double py1 = points.get(0).getY();
				double px2 = points.get(1).getX();
				double py2 = points.get(1).getY();
				double dx = px2 - px1;
				double dy = py2 - py1;
				double theta = dx > 0 ? Math.atan2(dy, dx) : Math.atan2(dy, dx) + Math.PI; 
				g.rotate(theta, x + w/2 , y + h/2); 
			} else if (!horizontal)	{
				g.rotate(-Math.PI / 2, x + w / 2, y + h / 2);
				g.translate(w / 2 - h / 2, h / 2 - w / 2);
			}

			Color fontColor = mxUtils.getColor(style, mxConstants.STYLE_FONTCOLOR, Color.black);
			g.setColor(fontColor);

			// Shifts the y-coordinate down by the ascent plus a workaround
			// for the line not starting at the exact vertical location
			Font scaledFont = mxUtils.getFont(style, scale);
			g.setFont(scaledFont);
			int fontSize = mxUtils.getInt(style, mxConstants.STYLE_FONTSIZE, mxConstants.DEFAULT_FONTSIZE);
			FontMetrics fm = g.getFontMetrics();
			int scaledFontSize = scaledFont.getSize();
			double fontScaleFactor = ((double) scaledFontSize) / ((double) fontSize);
			// This factor is the amount by which the font is smaller/
			// larger than we expect for the given scale. 1 means it's
			// correct, 0.8 means the font is 0.8 the size we expected
			// when scaled, etc.
			double fontScaleRatio = fontScaleFactor / scale;
			// The y position has to be moved by (1 - ratio) * height / 2
			if (!isEdgeLabel)
				y += 2 * fm.getMaxAscent() - fm.getHeight()	+ mxConstants.LABEL_INSET * scale;

			Object vertAlign = mxUtils.getString(style,	mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE);
			double vertAlignProportion = 0.5;

			if (vertAlign.equals(mxConstants.ALIGN_TOP))	{
				vertAlignProportion = 0;
			} else if (vertAlign.equals(mxConstants.ALIGN_BOTTOM)) {
				vertAlignProportion = 1.0;
			}

			y += (1.0 - fontScaleRatio) * h * vertAlignProportion;

			// Gets the alignment settings
			Object align = mxUtils.getString(style, mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER);

			if (align.equals(mxConstants.ALIGN_LEFT)) {
				x += mxConstants.LABEL_INSET * scale;
			} else if (align.equals(mxConstants.ALIGN_RIGHT)) {
				x -= mxConstants.LABEL_INSET * scale;
			}

			// Draws the text line by line
			String[] lines = text.split("\n");

			for (int i = 0; i < lines.length; i++) {
				int dx = 0;

				if (align.equals(mxConstants.ALIGN_CENTER))	{
					int sw = fm.stringWidth(lines[i]);

					if (horizontal)	{
						dx = (w - sw) / 2;
					} else {
						dx = (h - sw) / 2;
					}
				} else if (align.equals(mxConstants.ALIGN_RIGHT)) {
					int sw = fm.stringWidth(lines[i]);
					dx = ((horizontal) ? w : h) - sw;
				}

				g.drawString(lines[i], x + dx, y);
				postProcessLine(text, lines[i], fm, canvas, x + dx, y);
				y += fm.getHeight() + mxConstants.LINESPACING;
			}
		}
	}
}
