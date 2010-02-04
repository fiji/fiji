package fiji;

import fiji.drawing.Linear_Gradient;

import ij.IJ;
import ij.ImagePlus;

import ij.gui.Line;

import ij.plugin.filter.PlugInFilter;

import ij.process.Blitter;
import ij.process.ImageProcessor;

import ij3d.Content;
import ij3d.Image3DUniverse;
import ij3d.ImageCanvas3D;

import java.awt.Color;
import java.awt.Font;

import javax.media.j3d.Transform3D;

public class Prettify_Wiki_Screenshot implements PlugInFilter {
	public String label = "";

	public int setup(String arg, ImagePlus imp) {
		return DOES_RGB | NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		int h = ip.getHeight();

		// make a gradient image
		ImageProcessor mask = ip.duplicate();
		Line line = new Line(0, 0, 0, h);
		int from = (int)(0.99 * 255);
		int to = (int)(0.5 * 255);
		from = from | (from << 8) | (from << 16);
		to = to | (to << 8) | (to << 16);
		Linear_Gradient.makeLinearGradient(mask, from, to, line);

		// "blend" mask with original image

		mask.copyBits(ip, 0, 0, Blitter.MAX);
		mask.flipVertical();

		// instantiate the 3D viewer

		Image3DUniverse univ = new Image3DUniverse();
		univ.show();

		// add the screenshot

		Content cImage = univ.addOrthoslice(new ImagePlus("screenshot", ip),
				null, "image", 0, new boolean[] {true, true, true}, 1);
		int dy = (int)(-h / 8);
		cImage.setTransform(new Transform3D(new double[] {
			1.0, 0.0, 0.0, 0.0,
			0.0, 1.0, 0.0, dy,
			0.0, 0.0, 1.0, 0.0,
			0.0, 0.0, 0.0, 1.0}));

		// add the mirror image
		Content cMirror = univ.addOrthoslice(new ImagePlus("mirror", mask),
				null, "mirror", 0, new boolean[] {true, true, true}, 1);

		double cos = 0.0;
		double sin = 1.0;
		// flap forward
		cMirror.applyTransform(new Transform3D(new double[] {
			1.0, 0.0, 0.0, 0.0,
			0.0, cos, sin, 0.0,
			0.0, -sin, cos, 0.0,
			0.0, 0.0, 0.0, 1.0}));

		// move
		cMirror.applyTransform(new Transform3D(new double[] {
			1.0, 0.0, 0.0, 0.0,
			0.0, 1.0, 0.0, h,
			0.0, 0.0, 1.0, dy,
			0.0, 0.0, 0.0, 1.0}));

		// rotate nicely

		sleep(1);
		univ.rotateY(Math.PI / 12);
		univ.fireTransformationUpdated();
		sleep(1);
		univ.adjustView();
		univ.fireTransformationUpdated();

		// set background

		float background = 1.0f;
		ImageCanvas3D canvas = (ImageCanvas3D)univ.getCanvas();
		canvas.getBG().setColor(background, background, background);

		// take snapshot

		int snapshotWidth = 2047;
		ImagePlus snapshot = univ.takeSnapshot(snapshotWidth, snapshotWidth);
		univ.close();

		// downsample

		int w2 = 400;
		int h2 = 400;
		IJ.run(snapshot, "downsample ", "width=" + w2 + " height=" + h2 + " source=0.50 target=0.50 keep");

		// write label
		if (label != null && !label.equals("")) {
			ImagePlus smallImage = IJ.getImage();
			drawOutlineText(smallImage.getProcessor(), label, 24, 30, smallImage.getHeight() - 30);
			smallImage.updateAndDraw();
		}
	}

	public static void drawOutlineText(ImageProcessor ip, String string, int size, int x, int y) {
		Font font = new Font("Arial", Font.BOLD, size);
		int[][] offsets = {
			{-1, 0}, {-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}
		};
		int shadowOffset = 3;

		ip.setFont(font);
		ip.setAntialiasedText(true);

		// cast shadow
		int shadowGray = 64;
		ip.setColor(new Color(shadowGray, shadowGray, shadowGray, 32));
		for (int[] dxy : offsets)
			ip.drawString(string, x + shadowOffset + dxy[0], y + shadowOffset + dxy[1]);
		ip.setColor(new Color(shadowGray, shadowGray, shadowGray, 64));
		ip.drawString(string, x + shadowOffset, y + shadowOffset);

		// simulate outline

		// setColor(int) does not set the drawingColor; go figure!
		ip.setColor(new Color(0, 0, 0, 80));
		for (int[] dxy : offsets)
			ip.drawString(string, x + dxy[0], y + dxy[1]);
		ip.setColor(new Color(255, 255, 255, 255));
		ip.drawString(string, x, y);
	}

	public static void sleep(double seconds) {
		try {
			Thread.sleep((long)(seconds * 1000));
		} catch (InterruptedException e) { /* ignore */ }
	}
}
