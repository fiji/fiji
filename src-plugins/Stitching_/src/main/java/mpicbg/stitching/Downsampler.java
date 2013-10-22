
package mpicbg.stitching;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.awt.TextComponent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.util.List;
import java.util.Vector;

/**
 * Helper class for running the " Image > Scale..." plugin on a collection of
 * {@link ImagePlus} objects. Displays one dialog to collect scale values, which
 * are applied to all provided images.
 * <p>
 * NB: scaling different images in a stack to different proportions is not
 * currently supported.
 * </p>
 * 
 * @author Mark Hiner
 */
public class Downsampler {

	// Fields of the dialog
	private TextComponent xField;
	private TextComponent yField;
	private TextComponent widthField;
	private TextComponent heightField;

	// focused field in the dialog
	private Object fieldWithFocus;

	// original width and height
	private int originalWidth;
	private int originalHeight;

	// x and y scaling %
	private double xScale;
	private double yScale;

	// parameters to send to IJ Scale plugin
	private String params = null;

	// listener instances for updating scaling %'s and dimensions
	private final DownsampleTextListener textListener =
		new DownsampleTextListener();
	private final DownsampleFocusListener focusListener =
		new DownsampleFocusListener();

	/**
	 * Displays a dialog to harvest user input, allowing scaling from a
	 * specified image width and height.
	 * 
	 * @param imgWidth Base image width
	 * @param imgHeight Base image height
	 */
	public void getInput(int imgWidth, int imgHeight) {
		originalWidth = imgWidth;
		originalHeight = imgHeight;
		final GenericDialogPlus gdDownSample = new GenericDialogPlus("Downsample");
		String[] methods = ImageProcessor.getInterpolationMethods();

		gdDownSample.addNumericField("x scale", 1, 1);
		gdDownSample.addNumericField("y scale", 1, 1);
		gdDownSample.addNumericField("width (pixels)", imgWidth, 0);
		gdDownSample.addNumericField("height (pixels)", imgHeight, 0);
		gdDownSample.addChoice("Interpolation:", methods,
			methods[methods.length - 1]);
		gdDownSample.addCheckbox("Average when downsizing", true);
		Vector<?> fields = gdDownSample.getNumericFields();
		xField = (TextComponent) fields.get(0);
		yField = (TextComponent) fields.get(1);
		widthField = (TextComponent) fields.get(2);
		heightField = (TextComponent) fields.get(3);

		xField.addTextListener(textListener);
		xField.addFocusListener(focusListener);
		yField.addTextListener(textListener);
		yField.addFocusListener(focusListener);
		widthField.addTextListener(textListener);
		widthField.addFocusListener(focusListener);
		heightField.addTextListener(textListener);
		heightField.addFocusListener(focusListener);
		gdDownSample.showDialog();

		if (gdDownSample.wasOKed()) {
			xScale = gdDownSample.getNextNumber();
			yScale = gdDownSample.getNextNumber();
			double width = gdDownSample.getNextNumber();
			double height = gdDownSample.getNextNumber();
			String method = gdDownSample.getNextChoice();
			String average = gdDownSample.getNextBoolean() ? " average" : "";
			params =
				"width=" + width + " height=" +
					height + average + " interpolation=" + method;
		}
	}

	/**
	 * @return true iff getInput has been called.
	 */
	public boolean hasInput() {
		return params != null;
	}

	/**
	 * Runs the "Image > Scale..." plugin on all provided ImagePlus objects, based
	 * on the previous {@link #getInput(int, int)} call.
	 */
	public void run(ImagePlus... imps) {
		checkInit();
		for (int i = 0; i < imps.length; i++) {
			ImagePlus imp = imps[i];
			IJ.run(imp, "Size...", params);
		}
	}

	/**
	 * Scales the specified {@link ImageCollectionElement} based on the previous
	 * {@link #getInput(int, int)} call.
	 */
	public void run(ImageCollectionElement element) {
		checkInit();
		if (element.getOffset() == null) return;
		element.getOffset()[0] *= xScale;
		element.getOffset()[1] *= yScale;
	}

	/**
	 * Scales all provided {@link ImageCollectionElement}s based on the previous
	 * {@link #getInput(int, int)} call.
	 */
	public void run(List<ImageCollectionElement> elements) {
		checkInit();
		for (ImageCollectionElement element : elements) {
			run(element);
		}
	}

	/**
	 * Make sure params have been harvested via a {@link #getInput(int, int)}
	 * call.
	 */
	private void checkInit() {
		if (!hasInput()) throw new IllegalStateException(
			"Downsample failed: please"
				+ " run getInput before attempting to downsample.");
	}

	/**
	 * Nested {@link FocusListener} to help ensuring the x/y %'s and absolute
	 * lengths are kept synchronized.
	 */
	private class DownsampleFocusListener implements java.awt.event.FocusListener
	{

		@Override
		public void focusGained(FocusEvent e) {
			fieldWithFocus = e.getSource();
		}

		@Override
		public void focusLost(FocusEvent e) {}
	}

	/**
	 * Nested {@link TextListener} to help ensuring the x/y %'s and absolute
	 * lengths are kept synchronized.
	 */
	private class DownsampleTextListener implements TextListener {

		@Override
		public void textValueChanged(TextEvent e) {
			Object source = e.getSource();

			try {
				Double widthText = Double.parseDouble(widthField.getText());
				Double heightText = Double.parseDouble(heightField.getText());
				Double xText = Double.parseDouble(xField.getText());
				Double yText = Double.parseDouble(yField.getText());

				if (source == widthField && widthField == fieldWithFocus) {
					double newX = widthText / originalWidth;
					xField.setText(String.valueOf(newX));
				}
				else if (source == heightField && heightField == fieldWithFocus) {
					double newY = heightText / originalHeight;
					yField.setText(String.valueOf(newY));
				}
				else if (source == xField && xField == fieldWithFocus) {
					int newWidth = (int) Math.round(xText * originalWidth);
					widthField.setText(String.valueOf(newWidth));
				}
				else if (source == yField && yField == fieldWithFocus) {
					int newHeight = (int) Math.round(yText * originalHeight);
					heightField.setText(String.valueOf(newHeight));
				}
			}
			catch (NumberFormatException nfe) {}
		}

	}
}
