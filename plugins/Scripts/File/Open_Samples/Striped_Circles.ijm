/*
 * Circles with alternating colored stripes.
 * 
 * The circles appear as alternating purple and yellow.
 * But actually, half of the red circles are striped
 * with blue, and the other half with green.
 * 
 * Even zooming in on two adjacent circles, the respective
 * red stripes may not appear the same shade of red!
 *
 * Inspired by Bite Size Psych:
 * https://www.youtube.com/channel/UCmHzqwSP0uEHwzCeDzomNsg
 * 
 * Try calling Analyze>Color Inspector 3D
 * and selecting Display Mode>Histogram.
 */

numX = 4;
numY = 3;
pad = 15;

newImage("Striped Circles", "RGB Black", 400, 300, 1);

w = getWidth() / numX;
h = getHeight() / numY;

for (y = 0; y < numY; y++) {
	for (x = 0; x < numX; x++) {
		xOff = getWidth() * (numX - x - 1) / numX;
		yOff = getHeight() * (numY - y - 1) / numY;

		even = (x + y) % 2 == 0;

		// draw the background
		if (even) { setForegroundColor(9, 239, 49); /* green */ }
		else { setForegroundColor(10, 110, 230); /* blue */ }
		fillRect(xOff, yOff, w, h);

		// draw the object
		setForegroundColor(195, 50, 80); // red
		fillOval(xOff + pad, yOff + pad, w - 2 * pad, h - 2 * pad);

		// draw the stripes
		if (!even) { setForegroundColor(9, 239, 49); /* green */ }
		else { setForegroundColor(10, 110, 230); /* blue */ }
		for (p = xOff + even; p < xOff + w; p += 2) {
			drawLine(p, yOff, p, yOff + h - 1);
		}
	}
}
