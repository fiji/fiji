package vib.app.module;

import ij.ImagePlus;

import vib.app.Options;

import vib.NaiveResampler;

public class Resample extends Module {
	public String getName() { return "Resample"; }
	protected String getMessage() { return "Resampling"; }

	protected void run(State state, int index) {
		if (state.options.numChannels > 1)
			new SplitChannels().runOnOneImage(state, index);

		prereqsDone(state, index);
		
		if (state.options.resamplingFactor == 1)
			return;

		for (int c = 0; c < state.options.numChannels; c++)
			if (c != state.options.refChannel - 1)
				run(state, c, index);
		/*
		 * Make sure that the reference channel's image is cached,
		 * since the next steps are more likely to need that.
		 * Caching it avoids unnecessary loading...
		 */
		run(state, state.options.refChannel - 1, index);
	}

	private void run(State state, int channel, int index) {
		String imagePath = state.getImagePath(channel, index);
		String resampledPath = state.getResampledPath(channel, index);
		if (state.upToDate(imagePath, resampledPath))
			return;

		ImagePlus image = state.getImage(imagePath);
		if (image == null)
			return;
		ImagePlus resampled = NaiveResampler.resample(image,
				state.options.resamplingFactor);
	
		if(!state.save(resampled, resampledPath))
			throw new RuntimeException("Could not save " + 
				resampledPath);
	}
}
