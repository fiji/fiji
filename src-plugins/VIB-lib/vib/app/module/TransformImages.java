package vib.app.module;

import vib.app.Options;

public class TransformImages extends Module {
	public String getName() { return "TransformImages"; }
	protected String getMessage() { return "Transforming images"; }

	protected void run(State state, int index) {
		Module module = null;
		switch (state.options.transformationMethod) {
		case Options.GREY:
			module = new GreyTransformation();
			break;
		case Options.CENTER:
			module = new CenterTransformation();
			break;
		case Options.LABEL_DIFFUSION:
			module = new LabelDiffusionTransformation();
			break;
		default:
			throw new RuntimeException("Invalid transformation: "
				+ state.options.transformationMethod);
		}
		module.runOnOneImage(state, index);
	}
}

