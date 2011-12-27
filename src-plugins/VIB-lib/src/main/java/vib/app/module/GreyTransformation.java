package vib.app.module;

import distance.Euclidean;

import ij.ImagePlus;

import vib.app.ImageMetaData;
import vib.app.Options;

import vib.FastMatrix;
import vib.FloatMatrix;
import vib.RigidRegistration;
import vib.TransformedImage;

public class GreyTransformation extends Module {
	public String getName() { return "GreyTransformation"; }
	protected String getMessage() { return "Registering the brains"; }

	protected void run(State state, int index) {
		new Resample().runOnOneImage(state, -1);
		new Resample().runOnOneImage(state, index);

		prereqsDone(state, index);

		int refChannel = state.options.refChannel - 1;
		String imagePath = state.getResampledPath(refChannel, index);
		String statisticsPath = state.getStatisticsPath(index);
		ImageMetaData stats = new ImageMetaData(statisticsPath);
		String transformLabel = state.getTransformLabel(Options.GREY);
		if (stats.upToDate(imagePath, transformLabel))
			return;

		String initialTransform = "";
		int level = 4;
		int stopLevel = 1;
		double tolerance = 4.0;
		String materialBBox = "";
		boolean noOptimization = false;
		int nInitialPositions = 1;
		boolean showTransformed = false;
		boolean showDifferenceImage = false;
		boolean fastButInaccurate = false;
		ImagePlus templ = state.getTemplate();
		ImagePlus image = state.getImage(imagePath);

		while (level > 0 && (templ.getWidth() >> level) < 32)
			level--;
		if (stopLevel > level)
			stopLevel = level;
		TransformedImage trans = new TransformedImage( templ, image);
		if (stopLevel > 0)
			trans = trans.resample(1 << stopLevel);
		trans.measure = new Euclidean();
		RigidRegistration rr = new RigidRegistration();

		console.append("...rigidRegistration");
		FastMatrix matrix2 = rr.rigidRegistration(trans,
				materialBBox, initialTransform,
				-1, -1, noOptimization,
				level, stopLevel, tolerance,
				nInitialPositions, showTransformed, 
				showDifferenceImage,
				fastButInaccurate, null);
		String forAmira = matrix2.toStringForAmira();
		FloatMatrix floatMatrix = FloatMatrix.parseMatrix(forAmira);
		stats.setMatrix(transformLabel, floatMatrix);
		if(!stats.saveTo(statisticsPath))
			throw new RuntimeException("Could not save " + 
				statisticsPath);
	}
}
