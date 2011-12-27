package vib.app.module;

import ij.IJ;
import ij.ImagePlus;

import java.awt.image.IndexColorModel;

import vib.app.gui.Console;

public class Show extends Module {
	public String getName() { return "Show"; }
	protected String getMessage() { return "Displaying the results"; }
	protected boolean runsOnce() { return true; }

	protected void run(State state, int index) {
		if (index != 0)
			return;
		prereqsDone(state, index);

		for (int i = -1; i < state.options.numChannels; i++) {
			IJ.open(state.getOutputPath(i));
			if (i < 0) {
				setPhysicsLUT(IJ.getImage());
				IJ.setMinAndMax(0, 100);
			}
		}
	}

	private final static int[] redSteps = { 0, 45,
		25, 0, 40, 0, 65, 25, 140, 20, 170, 150, 190, 210, 255, 255
	};

	private final static int[] greenSteps = {
		0, 2, 30, 0, 100, 180, 190, 210, 255, 35
	};

	private final static int[] blueSteps = {
		0, 125, 70, 170, 100, 180, 140, 25, 255, 20
	};

	private static byte[] makeColorChannel(int[] steps) {
		byte[] result = new byte[256];
		int j = 0;
		for (int i = 0; i < 256; i++) {
			if (i > steps[j + 2])
				j += 2;
			result[i] = (byte)(int)(steps[j + 1] + (i - steps[j])
				* (steps[j + 3] - steps[j + 1])
				/ (steps[j + 2] - steps[j]));
		}
		return result;
	}

	private static void setPhysicsLUT(ImagePlus ip) {
		IndexColorModel c = new IndexColorModel(8, 256,
			makeColorChannel(redSteps),
			makeColorChannel(greenSteps),
			makeColorChannel(blueSteps));
                ip.getProcessor().setColorModel(c);
                if (ip.getStackSize() > 1)
                        ip.getStack().setColorModel(c);

	}

}
