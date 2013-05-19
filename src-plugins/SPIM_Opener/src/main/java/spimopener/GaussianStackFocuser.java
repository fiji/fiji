package spimopener;

import ij.macro.Interpreter;
import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.ArrayList;


/*
 * Shamelessly copied from Johannes Schindelin's Gaussian_Stack_Focuser,
 * found in the plugins he wrote for the Oates lab.
 */
public class GaussianStackFocuser implements Projector {

	private static final int RADIUS = 3;

	private final ArrayList<FloatProcessor> ips = new ArrayList<FloatProcessor>();

	@Override
	public void reset() {
		ips.clear();
	}

	@Override
	public void add(ImageProcessor ip) {
		ips.add((FloatProcessor)ip.convertToFloat());

	}

	@Override
	public ImageProcessor getProjection() {
		FloatProcessor[] fps = new FloatProcessor[ips.size()];
		ips.toArray(fps);
		return focus(fps, RADIUS).convertToShort(true);
	}

	public static FloatProcessor focus(FloatProcessor[] slices, double radius) {
                boolean wasBatchMode = Interpreter.batchMode;
                Interpreter.batchMode = true;

                // calculate weights
                GaussianBlur blur = new GaussianBlur();
                int pixelCount = slices[0].getWidth() * slices[0].getHeight();
                FloatProcessor[] weights = new FloatProcessor[slices.length];
                for (int i = 0; i < slices.length; i++) {
                        weights[i] = (FloatProcessor)slices[i].duplicate();
                        blur.blur(weights[i], radius);
                        float[] pixels1 = (float[])slices[i].getPixels();
                        float[] pixels2 = (float[])weights[i].getPixels();
                        for (int j = 0; j < pixelCount; j++)
                                pixels2[j] = Math.abs(pixels2[j] - pixels1[j]);
                }

                FloatProcessor result = (FloatProcessor)slices[0].duplicate();
                for (int j = 0; j < pixelCount; j++) {
                        float cumul = 0, totalWeight = 0;
                        for (int i = 0; i < slices.length; i++) {
                                float value = slices[i].getf(j);
                                float weight = weights[i].getf(j);
                                cumul += value * weight;
                                totalWeight += weight;
                        }
                        if (totalWeight != 0)
                                result.setf(j, cumul / totalWeight);
                }
                Interpreter.batchMode = wasBatchMode;
                return result;
        }

}
