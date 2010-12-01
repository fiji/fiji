import ij.IJ;
import ij.ImagePlus;

import ij.macro.Interpreter;

import ij.plugin.filter.PlugInFilter;

import ij.process.ImageProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TemporalColorCoder implements PlugInFilter {
	public int setup(String arg, ImagePlus imp) {
		return DOES_8G | DOES_16 | DOES_32 | NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		try {
			new Interpreter().run(getResourceAsString("K_TimeRGBcolorcode.ijm"));
		} catch (Exception e) {
			IJ.handleException(e);
		}
	}

	public String getResourceAsString(String name) throws IOException {
		StringBuffer buffer = new StringBuffer();
		InputStream in = getClass().getResource("/" + name).openStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		for (;;) {
			String line = reader.readLine();
			if (line == null)
				break;
			buffer.append(line);
			buffer.append('\n');
		}
		reader.close();
		return buffer.toString();
	}
}