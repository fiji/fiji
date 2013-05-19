//==============================================================================
//
// Project: EDF - Extended Depth of Focus
//
// Author: Daniel Sage
//
// Organization: Biomedical Imaging Group (BIG)
// Ecole Polytechnique Federale de Lausanne (EPFL), Lausanne, Switzerland
//
// Information: http://bigwww.epfl.ch/demo/edf/
//
// Reference: B. Forster, D. Van De Ville, J. Berent, D. Sage, M. Unser
// Complex Wavelets for Extended Depth-of-Field: A New Method for the Fusion
// of Multichannel Microscopy Images, Microscopy Research and Techniques,
// 65(1-2), pp. 33-42, September 2004.
//
// Conditions of use: You'll be free to use this software for research purposes,
// but you should not redistribute it without our consent. In addition, we
// expect you to include a citation or acknowledgment whenever you present or
// publish results that are based on it.
//
// History:
// - Updated (Daniel Sage, 21 December 2010)
//
//==============================================================================

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.gui.GUI;
import ij.plugin.PlugIn;

import java.util.StringTokenizer;

import edfgui.BasicDialog;

/**
 * This class is a plugin of ImageJ. It offers an easy dialog box to run
 * the EDF program.
 */
public class EDF_Easy_ implements PlugIn {

	public BasicDialog dl;

	/**
	 * Implements the run method of PlugIn.
	 */
	public void run(String args) {
		// Check the ImageJ version
		if (IJ.versionLessThan("1.21a"))
			return;

		// Check the presence of the image
		ImagePlus imp = WindowManager.getCurrentImage();

		if (imp == null) {
			IJ.error("No stack of images open");
			return;
		}

		// Check the size of the image
		if (imp.getWidth() < 4) {
			IJ.error("The image is too small (nx=" + imp.getWidth() + ")");
			return;
		}

		if (imp.getHeight() < 4) {
			IJ.error("The image is too small (ny=" + imp.getHeight() + ")");
			return;
		}

		if (imp.getStackSize() < 2) {
			IJ.error("The stack of images is too small (nz=" + imp.getStackSize() + ")");
			return;
		}

		// Color or grayscale image
		boolean color = false;
		if (imp.getType() == ImagePlus.COLOR_RGB)
			color = true;
		else if (imp.getType() == ImagePlus.GRAY8)
			color = false;
		else if (imp.getType() == ImagePlus.GRAY16)
			color = false;
		else if (imp.getType() == ImagePlus.GRAY32)
			color = false;
		else {
			IJ.error("Only process 8-bits, 16-bits, 32-bits and RGB images");
			return;
		}

		if (Macro.getOptions() != null) {
			dl = new BasicDialog(new int[]{imp.getWidth(), imp.getHeight()}, color, args.equals("applet"));

			String params = Macro.getValue(Macro.getOptions(), "quality", "");
			if (!params.equals("")) {
				String arguments[] = split(params);
				if (arguments.length != 1) {
					IJ.error("The arguments of the quality are not valid. Correct example: quality='0'");
					return;
				}
				int quality = (new Integer(arguments[0])).intValue();
				dl.parameters.setQualitySettings(quality);
			}

			params = Macro.getValue(Macro.getOptions(), "topology", "");
			if (!params.equals("")) {
				String arguments[] = split(params);
				if (arguments.length != 1) {
					IJ.error("The arguments of the topology are not valid. Correct example: topology='0'");
					return;
				}
				int topology = (new Integer(arguments[0])).intValue();
				dl.parameters.setTopologySettings(topology);
			}

			params = Macro.getValue(Macro.getOptions(), "show-view", "");
			if (!params.equals("")) {
				String arguments[] = split(params);
				if (arguments.length != 1) {
					IJ.error("The arguments of the show-view are not valid. Correct example: show-view='on'");
					return;
				}
				dl.parameters.show3dView = arguments[0].equals("on");
			}

			params = Macro.getValue(Macro.getOptions(), "show-topology", "");
			if (!params.equals("")) {
				String arguments[] = split(params);
				if (arguments.length != 1) {
					IJ.error("The arguments of the show-topology are not valid. Correct example: show-topology='on'");
					return;
				}
				dl.parameters.showTopology = arguments[0].equals("on");
			}

			dl.process();
		}
		else {
			dl = new BasicDialog(new int[]{imp.getWidth(), imp.getHeight()},color, args.equals("applet"));
			dl.initialize();
			dl.pack();
			GUI.center(dl);
			dl.setVisible(true);
		}

	}

	/**
	 * Split a macro options string.
	 */
	private String[] split(String s) {
		StringTokenizer t = new StringTokenizer(s);
		String[] items = new String[t.countTokens()];
		for (int k = 0; (k < items.length); k++) {
			items[k] = t.nextToken();
		}
		return(items);
	}

}
