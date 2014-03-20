package fiji;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

/**
 * A very simple plugin for use with the {@link DebugTest}.
 * 
 * @author Johannes Schindelin
 */
public class Dimension_Test_PlugInFilter implements PlugInFilter {
	private int[] dimensions;

	@Override
	public int setup(final String arg, final ImagePlus image) {
		dimensions = image.getDimensions();
		return DOES_ALL;
	}

	@Override
	public void run(final ImageProcessor ip) {
		final GenericDialog gd = new GenericDialog("Test");
		gd.addStringField("output", "");
		gd.showDialog();
		if (gd.wasCanceled()) return;

		final String output = gd.getNextString();
		try {
			final PrintStream out = new PrintStream(output);
			out.println(Arrays.toString(dimensions));
			out.close();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
