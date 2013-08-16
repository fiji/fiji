package vib.app.module;

import ij.ImagePlus;

import vib.NaiveResampler;

public class ResampleLabels extends Module {
	public String getName() { return "ResampleLabels"; }
	protected String getMessage() { return "Resampling label"; }

	protected void run(State state, int index) {
		if (index < 0)
			new Label().runOnAllImagesAndTemplate(state);

		prereqsDone(state, index);

		if (state.options.resamplingFactor == 1)
			return;
		String labelPath = state.getImagePath(-1, index);
		String resampledPath = state.getResampledPath(-1, index);
		if (state.upToDate(labelPath, resampledPath))
			return;

		ImagePlus image = state.getImage(labelPath);
		ImagePlus resampled = NaiveResampler.resample(image,
				state.options.resamplingFactor);
		if(!state.save(resampled, resampledPath))
			throw new RuntimeException("Could not save " + 
				resampledPath);
	}
}

