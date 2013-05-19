package fiji.plugin.trackmate.visualization.trackscheme;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.mxgraph.canvas.mxSvgCanvas;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxUtils;

public class TrackSchemeSvgCanvas extends mxSvgCanvas {

	public TrackSchemeSvgCanvas(Document document) {
		super(document);
	}

	@Override
	public Element drawShape(int x, int y, int w, int h, Map<String, Object> style) {

		String fillColor = mxUtils.getString(style, mxConstants.STYLE_FILLCOLOR, "none");
		String gradientColor = mxUtils.getString(style, mxConstants.STYLE_GRADIENTCOLOR, "none");
		String strokeColor = mxUtils.getString(style, mxConstants.STYLE_STROKECOLOR, "none");
		float strokeWidth = (float) (mxUtils.getFloat(style, mxConstants.STYLE_STROKEWIDTH, 1) * scale);
		float opacity = mxUtils.getFloat(style, mxConstants.STYLE_OPACITY, 100);

		// Draws the shape
		String shape = mxUtils.getString(style, mxConstants.STYLE_SHAPE, "");
		Element elem = null;
		Element background = null;

		if (!shape.equals(mxScaledLabelShape.SHAPE_NAME)) {
			
			return super.drawShape(x, y, w, h, style);
			
		} else {

			background = document.createElement("rect");
			elem = background;

			elem.setAttribute("x", String.valueOf(x));
			elem.setAttribute("y", String.valueOf(y));
			elem.setAttribute("width", String.valueOf(w));
			elem.setAttribute("height", String.valueOf(h));

			if (mxUtils.isTrue(style, mxConstants.STYLE_ROUNDED, false)) {
				String r = String.valueOf(Math.min(w
						* mxConstants.RECTANGLE_ROUNDING_FACTOR, h
						* mxConstants.RECTANGLE_ROUNDING_FACTOR));

				elem.setAttribute("rx", r);
				elem.setAttribute("ry", r);
			}

			String img = getImageForStyle(style);

			if (img != null) {
				String imgAlign = mxUtils.getString(style, mxConstants.STYLE_IMAGE_ALIGN, mxConstants.ALIGN_LEFT);
				String imgValign = mxUtils.getString(style, mxConstants.STYLE_IMAGE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE);
				int imgWidth = (int) (mxUtils.getInt(style, mxConstants.STYLE_IMAGE_WIDTH, mxConstants.DEFAULT_IMAGESIZE) * scale);
				int imgHeight = (int) (mxUtils.getInt(style, mxConstants.STYLE_IMAGE_HEIGHT, mxConstants.DEFAULT_IMAGESIZE) * scale);
				int spacing = (int) (mxUtils.getInt(style, mxConstants.STYLE_SPACING, 2) * scale);

				mxRectangle imageBounds = getImageBounds(x, y, w, h);

					if (imgAlign.equals(mxConstants.ALIGN_CENTER)) {
						imageBounds.setX(imageBounds.getX() + (imageBounds.getWidth() - imgWidth) / 2);
					} else if (imgAlign.equals(mxConstants.ALIGN_RIGHT)) {
						imageBounds.setX(imageBounds.getX()
								+ imageBounds.getWidth() - imgWidth - spacing
								- 2);
					} else {
						imageBounds.setX(imageBounds.getX() + spacing + 4);
					}

				if (imgValign.equals(mxConstants.ALIGN_TOP)) {
					imageBounds.setY(imageBounds.getY() + spacing);
				} else if (imgValign.equals(mxConstants.ALIGN_BOTTOM)) {
					imageBounds.setY(imageBounds.getY()
							+ imageBounds.getHeight() - imgHeight
							- spacing);
				} else {
					imageBounds.setY(imageBounds.getY() + (imageBounds.getHeight() - imgHeight) / 2);
				}

				imageBounds.setWidth(imgWidth);
				imageBounds.setHeight(imgHeight);

				elem = document.createElement("g");
				elem.appendChild(background);

				Element imageElement = createImageElement(
						imageBounds.getX(), imageBounds.getY(),
						imageBounds.getWidth(), imageBounds.getHeight(),
						img, false, false, false, isEmbedded());

				if (opacity != 100) {
					String value = String.valueOf(opacity / 100);
					imageElement.setAttribute("opacity", value);
				}

				elem.appendChild(imageElement);
			}

			// Paints the glass effect
			if (mxUtils.isTrue(style, mxConstants.STYLE_GLASS, false)) {
				double size = 0.4;

				// TODO: Mask with rectangle or rounded rectangle of label
				// Creates glass overlay
				Element glassOverlay = document.createElement("path");

				// LATER: Not sure what the behaviour is for mutiple SVG elements in page.
				// Probably its possible that this points to an element in another SVG
				// node which when removed will result in an undefined background.
				glassOverlay.setAttribute("fill", "url(#"
						+ getGlassGradientElement().getAttribute("id")
						+ ")");

				String d = "m " + (x - strokeWidth) + ","
				+ (y - strokeWidth) + " L " + (x - strokeWidth)
				+ "," + (y + h * size) + " Q " + (x + w * 0.5)
				+ "," + (y + h * 0.7) + " " + (x + w + strokeWidth)
				+ "," + (y + h * size) + " L "
				+ (x + w + strokeWidth) + "," + (y - strokeWidth)
				+ " z";
				glassOverlay.setAttribute("stroke-width",
						String.valueOf(strokeWidth / 2));
				glassOverlay.setAttribute("d", d);
				elem.appendChild(glassOverlay);
			}
			
		} 

		double rotation = mxUtils.getDouble(style, mxConstants.STYLE_ROTATION);
		int cx = x + w / 2;
		int cy = y + h / 2;

		Element bg = background;

		if (bg == null) {
			bg = elem;
		}

		if (!bg.getNodeName().equalsIgnoreCase("use") && !bg.getNodeName().equalsIgnoreCase("image")) {
			if (!fillColor.equalsIgnoreCase("none") && !gradientColor.equalsIgnoreCase("none")) {
				String direction = mxUtils.getString(style, mxConstants.STYLE_GRADIENT_DIRECTION);
				Element gradient = getGradientElement(fillColor, gradientColor, direction);

				if (gradient != null) {
					bg.setAttribute("fill", "url(#" + gradient.getAttribute("id") + ")");
				}
			} else {
				bg.setAttribute("fill", fillColor);
			}

			bg.setAttribute("stroke", strokeColor);
			bg.setAttribute("stroke-width", String.valueOf(strokeWidth));

			// Adds the shadow element
			Element shadowElement = null;

			if (mxUtils.isTrue(style, mxConstants.STYLE_SHADOW, false) && !fillColor.equals("none")) {
				shadowElement = (Element) bg.cloneNode(true);

				shadowElement.setAttribute("transform", mxConstants.SVG_SHADOWTRANSFORM);
				shadowElement.setAttribute("fill", mxConstants.W3C_SHADOWCOLOR);
				shadowElement.setAttribute("stroke", mxConstants.W3C_SHADOWCOLOR);
				shadowElement.setAttribute("stroke-width", String.valueOf(strokeWidth));

				if (rotation != 0) {
					shadowElement.setAttribute("transform", "rotate("
							+ rotation + "," + cx + "," + cy + ") "
							+ mxConstants.SVG_SHADOWTRANSFORM);
				}

				if (opacity != 100) {
					String value = String.valueOf(opacity / 100);
					shadowElement.setAttribute("fill-opacity", value);
					shadowElement.setAttribute("stroke-opacity", value);
				}

				appendSvgElement(shadowElement);
			}
		}

		if (rotation != 0) {
			elem.setAttribute("transform", elem.getAttribute("transform")
					+ " rotate(" + rotation + "," + cx + "," + cy + ")");

		}

		if (opacity != 100) {
			String value = String.valueOf(opacity / 100);
			elem.setAttribute("fill-opacity", value);
			elem.setAttribute("stroke-opacity", value);
		}

		if (mxUtils.isTrue(style, mxConstants.STYLE_DASHED)) {
			elem.setAttribute("stroke-dasharray", "3, 3");
		}

		appendSvgElement(elem);

		return elem;
	}



	public mxRectangle getImageBounds(int x, int y, int width, int height) {
		int arc = getArcSize(width, height) / 2;
		int minSize = Math.min(width - arc*2, height - 4);
		mxRectangle imageBounds = new mxRectangle(x + arc, y+2, minSize, minSize);
		return imageBounds;
	}

	/**
	 * Computes the arc size for the given dimension.
	 * 
	 * @param w Width of the rectangle.
	 * @param h Height of the rectangle.
	 * @return Returns the arc size for the given dimension.
	 */
	public int getArcSize(int w, int h) {
		int arcSize;

		if (w <= h) {
			arcSize = (int) Math.round(h * mxConstants.RECTANGLE_ROUNDING_FACTOR);

			if (arcSize > (w / 2)) {
				arcSize = w / 2;
			}
		} else {
			arcSize = (int) Math.round(w * mxConstants.RECTANGLE_ROUNDING_FACTOR);

			if (arcSize > (h / 2)) {
				arcSize = h / 2;
			}
		}
		return arcSize;
	}


}
