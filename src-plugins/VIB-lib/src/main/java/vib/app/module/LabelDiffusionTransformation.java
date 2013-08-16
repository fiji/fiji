package vib.app.module;

import ij.ImagePlus;

import vib.app.ImageMetaData;
import vib.app.Options;

import vib.DiffusionInterpol2;
import vib.FloatMatrix;
import vib.InterpolatedImage;
import vib.TransformedImage;

public class LabelDiffusionTransformation extends Module {
	public String getName() { return "LabelDiffusionTransformation"; }
	protected String getMessage() {
		return "Calculating label diffusion transformation";
	}

	protected void run(State state, int index) {
		new Resample().runOnOneImage(state, index);
		new LabelCenterTransformation().runOnOneImage(state, index);

		prereqsDone(state, index);

		ImagePlus templateLabels = null;

		DiffusionInterpol2 interpol = new DiffusionInterpol2();
		boolean rememberDistortion = true;
		boolean reuseDistortion = false;
		float tolerance = 0.5f;

		FloatMatrix[] transformations = null;

		// DiffusionInterpolation for all channels
		for(int i = -1; i < state.options.numChannels; i++) {
			String imagePath = state.getResampledPath(i, index);
			String warpedPath = state.getWarpedPath(i, index);
			String statisticsPath = state.getStatisticsPath(index);
			if (state.upToDate(new String[] { imagePath,
					statisticsPath }, warpedPath))
				continue;

			if (templateLabels == null)
				templateLabels = state.getTemplateLabels();
			InterpolatedImage ii =
				new InterpolatedImage(templateLabels);
			ImagePlus scratch =
				ii.cloneDimensionsOnly().getImage();

			if (transformations == null)
				transformations =
					readTransformations(state, index);

			ImagePlus model = state.getImage(imagePath);

			/*
			 * Copy transformations because they get transformed
			 * in DiffusionInterpol2.
			 */
			FloatMatrix[] trans = copyMatrices(transformations);
			interpol.initialize(scratch, templateLabels, model,
					trans,
					reuseDistortion, rememberDistortion,
					tolerance);
			interpol.doit();
			reuseDistortion = true; // true after the first channel
			if(!state.save(scratch, warpedPath))
				throw new RuntimeException("Could not save " + 
					warpedPath);
		}

	}

	private static FloatMatrix[] readTransformations(State state,
			int index) {
		ImageMetaData templStats = state.getStatistics(-1);
		ImageMetaData stats = state.getStatistics(index);

		FloatMatrix[] result =
			new FloatMatrix[templStats.materials.length];
		for (int i = 0; i < result.length; i++) {
			String name = templStats.materials[i].name;
			String transformName = state.getTransformLabel() +
				name;
			result[i] = stats.getMatrix(transformName);
		}

		return result;
	}

	private static FloatMatrix[] copyMatrices(FloatMatrix[] orig) {
		FloatMatrix[] res = new FloatMatrix[orig.length];
		System.arraycopy(orig, 0, res, 0, orig.length);
		return res;
	}
}
