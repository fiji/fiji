/*====================================================================	
| Version: June 19, 2008
\===================================================================*/

/*====================================================================
| Philippe Thevenaz
| EPFL/STI/IOA/LIB
| Station 17
| CH-1015 Lausanne VD
| Switzerland
|
| phone (CET): +41(21)693.51.61
| fax: +41(21)693.37.01
| RFC-822: philippe.thevenaz@epfl.ch
| X-400: /C=ch/A=400net/P=switch/O=epfl/S=thevenaz/G=philippe/
| URL: http://bigwww.epfl.ch/
\===================================================================*/

/*====================================================================
| This work is based on the following paper:
|
| P. Thevenaz, U.E. Ruttimann, M. Unser
| A Pyramid Approach to Subpixel Registration Based on Intensity
| IEEE Transactions on Image Processing
| vol. 7, no. 1, pp. 27-41, January 1998.
|
| This paper is available on-line at
| http://bigwww.epfl.ch/publications/thevenaz9801.html
|
| Other relevant on-line publications are available at
| http://bigwww.epfl.ch/publications/
\===================================================================*/

/*====================================================================
| Additional help available at http://bigwww.epfl.ch/thevenaz/turboreg/
|
| You'll be free to use this software for research purposes, but you
| should not redistribute it without our consent. In addition, we expect
| you to include a citation or acknowledgment whenever you present or
| publish results that are based on it.
\===================================================================*/

// ImageJ
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.WindowManager;
import ij.gui.GUI;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.StackWindow;
import ij.gui.Toolbar;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.StackConverter;

// Java 1.1
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Event;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Scrollbar;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.Stack;
import java.util.Vector;

/*====================================================================
|	TurboReg_
\===================================================================*/

/*********************************************************************
 This class is the only one that is accessed directly by imageJ;
 it launches a modeless dialog and dies. Note that it implements
 <code>PlugIn</code> rather than <code>PlugInFilter</code>.
 ********************************************************************/
public class TurboReg_
	implements
		PlugIn

{ /* begin class TurboReg_ */

/*....................................................................
	Private variables
....................................................................*/

private double[][] sourcePoints =
	new double[turboRegPointHandler.NUM_POINTS][2];
private double[][] targetPoints =
	new double[turboRegPointHandler.NUM_POINTS][2];
private ImagePlus transformedImage = null;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 Accessor method for the <code>(double[][])sourcePoints</code> variable.
 This variable is valid only after a call to <code>run</code> with the
 option <code>-align</code> has been issued. What is returned is a
 two-dimensional array of type <code>double[][]</code> that contains
 coordinates from the image <code>sourceFilename</code>. These coordinates
 are given relative to the original image, before that the cropping
 described by the <code>sourceCropLeft</code>, <code>sourceCropTop</code>,
 <code>sourceCropRight</code>, and <code>sourceCropBottom</code> has been
 applied. These coordinates match those available from
 <code>targetPoints</code>. The total number of coordinates, equal to
 <code>sourcePoints[*].length</code>, is given by the constant
 <code>turboRegPointHandler.NUM_POINTS</code> which corresponds to four
 coordinates in the present version. The second index gives the horizontal
 component for <code>[0]</code> and the vertical component for
 <code>[1]</code>. The number of <i>useful</i> coordinates depends on the
 specific transformation for which the alignment has been performed:
 translation (1), scaled rotation (2), rotation (3), affine transformation
 (3), and bilinear transformation (4).
 @see TurboReg_#run
 @see TurboReg_#getTargetPoints
 ********************************************************************/
public double[][] getSourcePoints (
) {
	return(sourcePoints);
} /* end getSourcePoints */

/*********************************************************************
 Accessor method for the <code>(double[][])targetPoints</code> variable.
 This variable is valid only after a call to <code>run</code> with the
 option <code>-align</code> has been issued. What is returned is a
 two-dimensional array of type <code>double[][]</code> that contains
 coordinates from the image <code>targetFilename</code>. These coordinates
 are given relative to the original image, before that the cropping
 described by the <code>targetCropLeft</code>, <code>targetCropTop</code>,
 <code>targetCropRight</code>, and <code>targetCropBottom</code> has been
 applied. These coordinates match those available from
 <code>sourcePoints</code>. The total number of coordinates, equal to
 <code>targetPoints[*].length</code>, is given by the constant
 <code>turboRegPointHandler.NUM_POINTS</code> which corresponds to four
 coordinates in the present version. The second index gives the horizontal
 component for <code>[0]</code> and the vertical component for
 <code>[1]</code>. The number of <i>useful</i> coordinates depends on the
 specific transformation for which the alignment has been performed:
 translation (1), scaled rotation (2), rotation (3), affine transformation
 (3), and bilinear transformation (4).
 @see TurboReg_#run
 @see TurboReg_#getSourcePoints
********************************************************************/
public double[][] getTargetPoints (
) {
	return(targetPoints);
} /* end getTargetPoints */

/*********************************************************************
 Accessor method for the <code>(ImagePlus)transformedImage</code>
 variable. This variable is valid only after a call to <code>run</code>
 with the option <code>-transform</code> has been issued. What is returned
 is an <code>ImagePlus</code> object of the size described by the
 <code>outputWidth</code> and <code>outputHeight</code> parameters of
 the call to the <code>run</code> method of <code>TurboReg_</code>.
 @see TurboReg_#run
 ********************************************************************/
public ImagePlus getTransformedImage (
) {
	return(transformedImage);
} /* end getTransformedImage */

/*********************************************************************
 This method is the only one called by ImageJ. It checks that there
 are at least two grayscale images to register. If the command line
 is empty, then the plugin is executed in interactive mode. Else, the
 command line has the following syntax:
 <br>
 <br>
 <table border="1">
 <tr>
 <th>command</th><th>parameter</th><th>index</th><th>comment</th>
 </tr><tr>
 <td>&#8722;help</td><td></td><td>00</td><td>prints out the syntax</td>
 </tr><tr>
 <td>&#8722;align</td><td></td><td>00</td><td>do refine the landmarks</td>
 </tr><tr>
 <td></td><td>&#8722;file<br>&#8722;window</td>
 <td>01</td><td>reference type</td>
 </tr><tr>
 <td></td><td>sourceFilename<br>sourceWindowTitle</td><td>02</td>
 <td>string with optional quotes</td>
 </tr><tr>
 <td></td><td>sourceCropLeft</td><td>03</td><td>integer</td>
 </tr><tr>
 <td></td><td>sourceCropTop</td><td>04</td><td>integer</td>
 </tr><tr>
 <td></td><td>sourceCropRight</td><td>05</td><td>integer</td>
 </tr><tr>
 <td></td><td>sourceCropBottom</td><td>06</td><td>integer</td>
 </tr><tr>
 <td></td><td>&#8722;file<br>&#8722;window</td><td>07</td>
 <td>reference type</td>
 </tr><tr>
 <td></td><td>targetFilename<br>targetWindowTitle</td><td>08</td>
 <td>string with optional quotes</td>
 </tr><tr>
 <td></td><td>targetCropLeft</td><td>09</td><td>integer</td>
 </tr><tr>
 <td></td><td>targetCropTop</td><td>10</td><td>integer</td>
 </tr><tr>
 <td></td><td>targetCropRight</td><td>11</td><td>integer</td>
 </tr><tr>
 <td></td><td>targetCropBottom</td><td>12</td><td>integer</td>
 </tr><tr>
 <td></td><td>&#8722;translation<br>&#8722;rigidBody<br>
 &#8722;scaledRotation<br>&#8722;affine<br>&#8722;bilinear</td>
 <td>13</td><td>transformation (TRSAB)</td>
 </tr><tr>
 <td></td><td>sourcePoints[0][0]</td><td>14</td><td>floating-point (TRSAB)</td>
 </tr><tr>
 <td></td><td>sourcePoints[0][1]</td><td>15</td><td>floating-point (TRSAB)</td>
 </tr><tr>
 <td></td><td>targetPoints[0][0]</td><td>16</td><td>floating-point (TRSAB)</td>
 </tr><tr>
 <td></td><td>targetPoints[0][1]</td><td>17</td><td>floating-point (TRSAB)</td>
 </tr><tr>
 <td></td><td>sourcePoints[1][0]</td><td>18</td><td>floating-point (RSAB)</td>
 </tr><tr>
 <td></td><td>sourcePoints[1][1]</td><td>19</td><td>floating-point (RSAB)</td>
 </tr><tr>
 <td></td><td>targetPoints[1][0]</td><td>20</td><td>floating-point (RSAB)</td>
 </tr><tr>
 <td></td><td>targetPoints[1][1]</td><td>21</td><td>floating-point (RSAB)</td>
 </tr><tr>
 <td></td><td>sourcePoints[2][0]</td><td>22</td><td>floating-point (RAB)</td>
 </tr><tr>
 <td></td><td>sourcePoints[2][1]</td><td>23</td><td>floating-point (RAB)</td>
 </tr><tr>
 <td></td><td>targetPoints[2][0]</td><td>24</td><td>floating-point (RAB)</td>
 </tr><tr>
 <td></td><td>targetPoints[2][1]</td><td>25</td><td>floating-point (RAB)</td>
 </tr><tr>
 <td></td><td>sourcePoints[3][0]</td><td>26</td><td>floating-point (B)</td>
 </tr><tr>
 <td></td><td>sourcePoints[3][1]</td><td>27</td><td>floating-point (B)</td>
 </tr><tr>
 <td></td><td>targetPoints[3][0]</td><td>28</td><td>floating-point (B)</td>
 </tr><tr>
 <td></td><td>targetPoints[3][1]</td><td>29</td><td>floating-point (B)</td>
 </tr><tr>
 <td></td><td>&#8722;hideOutput<br>&#8722;showOutput</td><td>18 (T)<br>
 22 (S)<br>26 (RA)<br>30 (B)</td><td></td>
 </tr><tr>
 <td>&#8722;transform</td><td></td>
 <td>00</td><td>do not refine the landmarks</td>
 </tr><tr>
 <td></td><td>&#8722;file<br>&#8722;window</td><td>01</td>
 <td>reference type</td>
 </tr><tr>
 <td></td><td>sourceFilename<br>sourceWindowTitle</td><td>02</td>
 <td>string with optional quotes</td>
 </tr><tr>
 <td></td><td>outputWidth</td><td>03</td><td>integer</td>
 </tr><tr>
 <td></td><td>outputHeight</td><td>04</td><td>integer</td>
 </tr><tr>
 <td></td><td>&#8722;translation<br>&#8722;rigidBody<br>
 &#8722;scaledRotation<br>
 &#8722;affine<br>&#8722;bilinear</td><td>05</td><td>transformation (TRSAB)</td>
 </tr><tr>
 <td></td><td>sourcePoints[0][0]</td><td>06</td><td>floating-point (TRSAB)</td>
 </tr><tr>
 <td></td><td>sourcePoints[0][1]</td><td>07</td><td>floating-point (TRSAB)</td>
 </tr><tr>
 <td></td><td>targetPoints[0][0]</td><td>08</td><td>floating-point (TRSAB)</td>
 </tr><tr>
 <td></td><td>targetPoints[0][1]</td><td>09</td><td>floating-point (TRSAB)</td>
 </tr><tr>
 <td></td><td>sourcePoints[1][0]</td><td>10</td><td>floating-point (RSAB)</td>
 </tr><tr>
 <td></td><td>sourcePoints[1][1]</td><td>11</td><td>floating-point (RSAB)</td>
 </tr><tr>
 <td></td><td>targetPoints[1][0]</td><td>12</td><td>floating-point (RSAB)</td>
 </tr><tr>
 <td></td><td>targetPoints[1][1]</td><td>13</td><td>floating-point (RSAB)</td>
 </tr><tr>
 <td></td><td>sourcePoints[2][0]</td><td>14</td><td>floating-point (RAB)</td>
 </tr><tr>
 <td></td><td>sourcePoints[2][1]</td><td>15</td><td>floating-point (RAB)</td>
 </tr><tr>
 <td></td><td>targetPoints[2][0]</td><td>16</td><td>floating-point (RAB)</td>
 </tr><tr>
 <td></td><td>targetPoints[2][1]</td><td>17</td><td>floating-point (RAB)</td>
 </tr><tr>
 <td></td><td>sourcePoints[3][0]</td><td>18</td><td>floating-point (B)</td>
 </tr><tr>
 <td></td><td>sourcePoints[3][1]</td><td>19</td><td>floating-point (B)</td>
 </tr><tr>
 <td></td><td>targetPoints[3][0]</td><td>20</td><td>floating-point (B)</td>
 </tr><tr>
 <td></td><td>targetPoints[3][1]</td><td>21</td><td>floating-point (B)</td>
 </tr><tr>
 <td></td><td>&#8722;hideOutput<br>&#8722;showOutput</td><td>10 (T)<br>
 14 (S)<br>18 (RA)<br>22 (B)</td><td></td>
 </tr>
 </table>
 <br>
 Example: The command line
 <br>
 <code>&#8722;align &#8722;file path/source.tif 40 80 639 479
 &#8722;file path/target.tif 0 0 639 479
 &#8722;translation 320 240 331.7 210 &#8722;showOutput</code>
 <br>
 aligns 'source.tif' to 'target.tif' by translation, by refining an
 initial horizontal offset
 <br>
 <i>dx</i> = (331.7 &#8722; 320) = 11.7
 <br>
 and an initial vertical offset
 <br>
 <i>dy</i> = (210 &#8722; 240) = &#8722;30
 <br>
 The effective size of 'source.tif' is 600 x 400. The effective size
 of 'target.tif' is 640 x 480. The source and target points are given
 in relation to the original (uncropped) system of coordinates. The
 resulting output will be displayed.
 <br>
 <div style="color:red">IMPORTANT: You MUST use a pair of double quotes
 to enclose paths and filenames that contain white characters. Example:
 <code>"my path/my file"</code>. Escape characters (<i>e.g.</i>, a
 backslash) are not interpreted.</div>
 @param commandLine <code>String</code> optional list of parameters.
 ********************************************************************/
public void run (
	final String commandLine
) {
	String options = Macro.getOptions();
	if (!commandLine.equals("")) {
		options = commandLine;
	}
	if (options == null) {
		Runtime.getRuntime().gc();
		final ImagePlus[] admissibleImageList = createAdmissibleImageList();
		if (admissibleImageList.length < 2) {
			IJ.error(
				"At least two grayscale or RGB-stack images are required");
			return;
		}
		final turboRegDialog dialog = new turboRegDialog(IJ.getInstance(),
			admissibleImageList);
		GUI.center(dialog);
		dialog.setVisible(true);
	}
	else {
		final String[] token = getTokens(options);
		if (token.length < 1) {
			dumpSyntax(options);
			IJ.error(
				"Invalid syntax");
			return;
		}
		if (token[0].equals("-help")) {
			dumpSyntax(options);
			return;
		}
		else if (token[0].equals("-align")) {
			switch (token.length) {
				case 19:
				case 23:
				case 27:
				case 31: {
					break;
				}
				default: {
					dumpSyntax(options);
					IJ.error(
						"Invalid syntax");
					return;
				}
			}
			ImagePlus source = null;
			final int[] sourceCrop = new int[4];
			ImagePlus target = null;
			final int[] targetCrop = new int[4];
			int transformation = turboRegDialog.GENERIC_TRANSFORMATION;
			Boolean interactive = null;
			try {
				if (token[1].equals("-file")) {
					source = new ImagePlus(token[2]);
				}
				else if (token[1].equals("-window")) {
					final int[] IDlist = WindowManager.getIDList();
					if (IDlist == null) {
						source = null;
					}
					else {
						for (int k = 0; (k < IDlist.length); k++) {
							source = WindowManager.getImage(IDlist[k]);
							if (source.getTitle().equals(token[2])) {
								break;
							}
							else {
								source = null;
							}
						}
					}
				}
				else {
					dumpSyntax(options);
					IJ.error(
						"Invalid reference type: " + token[1]);
					return;
				}
				if (source == null) {
					dumpSyntax(options);
					IJ.error(
						"Invalid source: " + token[2]);
					return;
				}
				for (int i = 0; (i < 4); i++) {
					sourceCrop[i] = new Integer(token[i + 3]).intValue();
				}
				if (token[7].equals("-file")) {
					target = new ImagePlus(token[8]);
				}
				else if (token[7].equals("-window")) {
					final int[] IDlist = WindowManager.getIDList();
					if (IDlist == null) {
						target = null;
					}
					else {
						for (int k = 0; (k < IDlist.length); k++) {
							target = WindowManager.getImage(IDlist[k]);
							if (target.getTitle().equals(token[8])) {
								break;
							}
							else {
								target = null;
							}
						}
					}
				}
				else {
					dumpSyntax(options);
					IJ.error(
						"Invalid reference type: " + token[7]);
					return;
				}
				if (target == null) {
					dumpSyntax(options);
					IJ.error(
						"Invalid target: " + token[8]);
					return;
				}
				for (int i = 0; (i < 4); i++) {
					targetCrop[i] = new Integer(token[i + 9]).intValue();
				}
				transformation = getTransformation(token[13]);
				switch (transformation) {
					case turboRegDialog.TRANSLATION: {
						if (token.length != 19) {
							dumpSyntax(options);
							IJ.error(
								"Invalid number of source and target points");
							return;
						}
						sourcePoints[0][0] =
							new Double(token[14]).doubleValue();
						sourcePoints[0][1] =
							new Double(token[15]).doubleValue();
						targetPoints[0][0] =
							new Double(token[16]).doubleValue();
						targetPoints[0][1] =
							new Double(token[17]).doubleValue();
						interactive = getInteractive(token[18]);
						break;
					}
					case turboRegDialog.SCALED_ROTATION: {
						if (token.length != 23) {
							dumpSyntax(options);
							IJ.error(
								"Invalid number of source and target points");
							return;
						}
						sourcePoints[0][0] =
							new Double(token[14]).doubleValue();
						sourcePoints[0][1] =
							new Double(token[15]).doubleValue();
						targetPoints[0][0] =
							new Double(token[16]).doubleValue();
						targetPoints[0][1] =
							new Double(token[17]).doubleValue();
						sourcePoints[1][0] =
							new Double(token[18]).doubleValue();
						sourcePoints[1][1] =
							new Double(token[19]).doubleValue();
						targetPoints[1][0] =
							new Double(token[20]).doubleValue();
						targetPoints[1][1] =
							new Double(token[21]).doubleValue();
						interactive = getInteractive(token[22]);
						break;
					}
					case turboRegDialog.RIGID_BODY:
					case turboRegDialog.AFFINE: {
						if (token.length != 27) {
							dumpSyntax(options);
							IJ.error(
								"Invalid number of source and target points");
							return;
						}
						sourcePoints[0][0] =
							new Double(token[14]).doubleValue();
						sourcePoints[0][1] =
							new Double(token[15]).doubleValue();
						targetPoints[0][0] =
							new Double(token[16]).doubleValue();
						targetPoints[0][1] =
							new Double(token[17]).doubleValue();
						sourcePoints[1][0] =
							new Double(token[18]).doubleValue();
						sourcePoints[1][1] =
							new Double(token[19]).doubleValue();
						targetPoints[1][0] =
							new Double(token[20]).doubleValue();
						targetPoints[1][1] =
							new Double(token[21]).doubleValue();
						sourcePoints[2][0] =
							new Double(token[22]).doubleValue();
						sourcePoints[2][1] =
							new Double(token[23]).doubleValue();
						targetPoints[2][0] =
							new Double(token[24]).doubleValue();
						targetPoints[2][1] =
							new Double(token[25]).doubleValue();
						interactive = getInteractive(token[26]);
						break;
					}
					case turboRegDialog.BILINEAR: {
						if (token.length != 31) {
							dumpSyntax(options);
							IJ.error(
								"Invalid number of source and target points");
							return;
						}
						sourcePoints[0][0] =
							new Double(token[14]).doubleValue();
						sourcePoints[0][1] =
							new Double(token[15]).doubleValue();
						targetPoints[0][0] =
							new Double(token[16]).doubleValue();
						targetPoints[0][1] =
							new Double(token[17]).doubleValue();
						sourcePoints[1][0] =
							new Double(token[18]).doubleValue();
						sourcePoints[1][1] =
							new Double(token[19]).doubleValue();
						targetPoints[1][0] =
							new Double(token[20]).doubleValue();
						targetPoints[1][1] =
							new Double(token[21]).doubleValue();
						sourcePoints[2][0] =
							new Double(token[22]).doubleValue();
						sourcePoints[2][1] =
							new Double(token[23]).doubleValue();
						targetPoints[2][0] =
							new Double(token[24]).doubleValue();
						targetPoints[2][1] =
							new Double(token[25]).doubleValue();
						sourcePoints[3][0] =
							new Double(token[26]).doubleValue();
						sourcePoints[3][1] =
							new Double(token[27]).doubleValue();
						targetPoints[3][0] =
							new Double(token[28]).doubleValue();
						targetPoints[3][1] =
							new Double(token[29]).doubleValue();
						interactive = getInteractive(token[30]);
						break;
					}
					default: {
						dumpSyntax(options);
						IJ.error(
							"Invalid transformation");
						return;
					}
				}
			} catch (NumberFormatException e) {
				dumpSyntax(options);
				IJ.log(
					"Number format exception " + e.getMessage());
				IJ.error(
					"Invalid syntax");
				return;
			}
			if (interactive == null) {
				dumpSyntax(options);
				IJ.error(
					"Invalid directive for interactivity");
				return;
			}
			transformedImage = alignImages(source, sourceCrop,
				target, targetCrop,
				transformation, interactive.booleanValue());
		}
		else if (token[0].equals("-transform")) {
			switch (token.length) {
				case 11:
				case 15:
				case 19:
				case 23: {
					break;
				}
				default: {
					dumpSyntax(options);
					IJ.error(
						"Invalid syntax");
					return;
				}
			}
			ImagePlus source = null;
			int outputWidth = -1;
			int outputHeight = -1;
			int transformation = turboRegDialog.GENERIC_TRANSFORMATION;
			Boolean interactive = null;
			try {
				if (token[1].equals("-file")) {
					source = new ImagePlus(token[2]);
				}
				else if (token[1].equals("-window")) {
					final int[] IDlist = WindowManager.getIDList();
					for (int k = 0; (k < IDlist.length); k++) {
						source = WindowManager.getImage(IDlist[k]);
						if (source.getTitle().equals(token[2])) {
							break;
						}
						else {
							source = null;
						}
					}
				}
				else {
					dumpSyntax(options);
					IJ.error(
						"Invalid reference type: " + token[1]);
					return;
				}
				if (source == null) {
					dumpSyntax(options);
					IJ.error(
						"Invalid source: " + token[2]);
					return;
				}
				outputWidth = new Integer(token[3]).intValue();
				if (outputWidth <= 0) {
					dumpSyntax(options);
					IJ.error(
						"Invalid output width: " + token[3]);
					return;
				}
				outputHeight = new Integer(token[4]).intValue();
				if (outputHeight <= 0) {
					dumpSyntax(options);
					IJ.error(
						"Invalid output height: " + token[4]);
					return;
				}
				transformation = getTransformation(token[5]);
				switch (transformation) {
					case turboRegDialog.TRANSLATION: {
						if (token.length != 11) {
							dumpSyntax(options);
							IJ.error(
								"Invalid number of source and target points");
							return;
						}
						sourcePoints[0][0] =
							new Double(token[6]).doubleValue();
						sourcePoints[0][1] =
							new Double(token[7]).doubleValue();
						targetPoints[0][0] =
							new Double(token[8]).doubleValue();
						targetPoints[0][1] =
							new Double(token[9]).doubleValue();
						interactive = getInteractive(token[10]);
						break;
					}
					case turboRegDialog.SCALED_ROTATION: {
						if (token.length != 15) {
							dumpSyntax(options);
							IJ.error(
								"Invalid number of source and target points");
							return;
						}
						sourcePoints[0][0] =
							new Double(token[6]).doubleValue();
						sourcePoints[0][1] =
							new Double(token[7]).doubleValue();
						targetPoints[0][0] =
							new Double(token[8]).doubleValue();
						targetPoints[0][1] =
							new Double(token[9]).doubleValue();
						sourcePoints[1][0] =
							new Double(token[10]).doubleValue();
						sourcePoints[1][1] =
							new Double(token[11]).doubleValue();
						targetPoints[1][0] =
							new Double(token[12]).doubleValue();
						targetPoints[1][1] =
							new Double(token[13]).doubleValue();
						interactive = getInteractive(token[14]);
						break;
					}
					case turboRegDialog.RIGID_BODY:
					case turboRegDialog.AFFINE: {
						if (token.length != 19) {
							dumpSyntax(options);
							IJ.error(
								"Invalid number of source and target points");
							return;
						}
						sourcePoints[0][0] =
							new Double(token[6]).doubleValue();
						sourcePoints[0][1] =
							new Double(token[7]).doubleValue();
						targetPoints[0][0] =
							new Double(token[8]).doubleValue();
						targetPoints[0][1] =
							new Double(token[9]).doubleValue();
						sourcePoints[1][0] =
							new Double(token[10]).doubleValue();
						sourcePoints[1][1] =
							new Double(token[11]).doubleValue();
						targetPoints[1][0] =
							new Double(token[12]).doubleValue();
						targetPoints[1][1] =
							new Double(token[13]).doubleValue();
						sourcePoints[2][0] =
							new Double(token[14]).doubleValue();
						sourcePoints[2][1] =
							new Double(token[15]).doubleValue();
						targetPoints[2][0] =
							new Double(token[16]).doubleValue();
						targetPoints[2][1] =
							new Double(token[17]).doubleValue();
						interactive = getInteractive(token[18]);
						break;
					}
					case turboRegDialog.BILINEAR: {
						if (token.length != 23) {
							dumpSyntax(options);
							IJ.error(
								"Invalid number of source and target points");
							return;
						}
						sourcePoints[0][0] =
							new Double(token[6]).doubleValue();
						sourcePoints[0][1] =
							new Double(token[7]).doubleValue();
						targetPoints[0][0] =
							new Double(token[8]).doubleValue();
						targetPoints[0][1] =
							new Double(token[9]).doubleValue();
						sourcePoints[1][0] =
							new Double(token[10]).doubleValue();
						sourcePoints[1][1] =
							new Double(token[11]).doubleValue();
						targetPoints[1][0] =
							new Double(token[12]).doubleValue();
						targetPoints[1][1] =
							new Double(token[13]).doubleValue();
						sourcePoints[2][0] =
							new Double(token[14]).doubleValue();
						sourcePoints[2][1] =
							new Double(token[15]).doubleValue();
						targetPoints[2][0] =
							new Double(token[16]).doubleValue();
						targetPoints[2][1] =
							new Double(token[17]).doubleValue();
						sourcePoints[3][0] =
							new Double(token[18]).doubleValue();
						sourcePoints[3][1] =
							new Double(token[19]).doubleValue();
						targetPoints[3][0] =
							new Double(token[20]).doubleValue();
						targetPoints[3][1] =
							new Double(token[21]).doubleValue();
						interactive = getInteractive(token[22]);
						break;
					}
					default: {
						dumpSyntax(options);
						IJ.error(
							"Invalid transformation");
						return;
					}
				}
			} catch (NumberFormatException e) {
				dumpSyntax(options);
				IJ.log(
					"Number format exception " + e.getMessage());
				IJ.error(
					"Invalid syntax");
				return;
			}
			if (interactive == null) {
				dumpSyntax(options);
				IJ.error(
					"Invalid directive for interactivity");
				return;
			}
			transformedImage = transformImage(source, outputWidth, outputHeight,
				transformation, interactive.booleanValue());
		}
		else {
			dumpSyntax(options);
			IJ.error(
				"Invalid operation");
			return;
		}
	}
} /* end run */

/*....................................................................
	Private methods
....................................................................*/

/*------------------------------------------------------------------*/
private ImagePlus alignImages (
	final ImagePlus source,
	final int[] sourceCrop,
	final ImagePlus target,
	final int[] targetCrop,
	final int transformation,
	final boolean interactive
) {
	if ((source.getType() != source.GRAY16)
		&& (source.getType() != source.GRAY32)
		&& ((source.getType() != source.GRAY8)
		|| source.getStack().isRGB() || source.getStack().isHSB())) {
		IJ.error(
			source.getTitle() + " should be grayscale (8, 16, or 32 bit)");
		return(null);
	}
	if ((target.getType() != target.GRAY16)
		&& (target.getType() != target.GRAY32)
		&& ((target.getType() != target.GRAY8)
		|| target.getStack().isRGB() || target.getStack().isHSB())) {
		IJ.error(
			target.getTitle() + " should be grayscale (8, 16, or 32 bit)");
		return(null);
	}
	source.setRoi(sourceCrop[0], sourceCrop[1], sourceCrop[2], sourceCrop[3]);
	target.setRoi(targetCrop[0], targetCrop[1], targetCrop[2], targetCrop[3]);
	source.setSlice(1);
	target.setSlice(1);
	final ImagePlus sourceImp = new ImagePlus("source",
		source.getProcessor().crop());
	final ImagePlus targetImp = new ImagePlus("target",
		target.getProcessor().crop());
	final turboRegImage sourceImg = new turboRegImage(
		sourceImp, transformation, false);
	final turboRegImage targetImg = new turboRegImage(
		targetImp, transformation, true);
	final int pyramidDepth = getPyramidDepth(
		sourceImp.getWidth(), sourceImp.getHeight(),
		targetImp.getWidth(), targetImp.getHeight());
	sourceImg.setPyramidDepth(pyramidDepth);
	targetImg.setPyramidDepth(pyramidDepth);
	sourceImg.getThread().start();
	targetImg.getThread().start();
	if (2 <= source.getStackSize()) {
		source.setSlice(2);
	}
	if (2 <= target.getStackSize()) {
		target.setSlice(2);
	}
	final ImagePlus sourceMskImp = new ImagePlus("source mask",
		source.getProcessor().crop());
	final ImagePlus targetMskImp = new ImagePlus("target mask",
		target.getProcessor().crop());
	final turboRegMask sourceMsk = new turboRegMask(sourceMskImp);
	final turboRegMask targetMsk = new turboRegMask(targetMskImp);
	source.setSlice(1);
	target.setSlice(1);
	if (source.getStackSize() < 2) {
		sourceMsk.clearMask();
	}
	if (target.getStackSize() < 2) {
		targetMsk.clearMask();
	}
	sourceMsk.setPyramidDepth(pyramidDepth);
	targetMsk.setPyramidDepth(pyramidDepth);
	sourceMsk.getThread().start();
	targetMsk.getThread().start();
	switch (transformation) {
		case turboRegDialog.TRANSLATION: {
			sourcePoints[0][0] -= sourceCrop[0];
			sourcePoints[0][1] -= sourceCrop[1];
			targetPoints[0][0] -= targetCrop[0];
			targetPoints[0][1] -= targetCrop[1];
			break;
		}
		case turboRegDialog.SCALED_ROTATION: {
			for (int k = 0; (k < 2); k++) {
				sourcePoints[k][0] -= sourceCrop[0];
				sourcePoints[k][1] -= sourceCrop[1];
				targetPoints[k][0] -= targetCrop[0];
				targetPoints[k][1] -= targetCrop[1];
			}
			break;
		}
		case turboRegDialog.RIGID_BODY:
		case turboRegDialog.AFFINE: {
			for (int k = 0; (k < 3); k++) {
				sourcePoints[k][0] -= sourceCrop[0];
				sourcePoints[k][1] -= sourceCrop[1];
				targetPoints[k][0] -= targetCrop[0];
				targetPoints[k][1] -= targetCrop[1];
			}
			break;
		}
		case turboRegDialog.BILINEAR: {
			for (int k = 0; (k < 4); k++) {
				sourcePoints[k][0] -= sourceCrop[0];
				sourcePoints[k][1] -= sourceCrop[1];
				targetPoints[k][0] -= targetCrop[0];
				targetPoints[k][1] -= targetCrop[1];
			}
			break;
		}
	}
	final turboRegPointHandler sourcePh = new turboRegPointHandler(
		sourceImp, transformation);
	final turboRegPointHandler targetPh = new turboRegPointHandler(
		targetImp, transformation);
	sourcePh.setPoints(sourcePoints);
	targetPh.setPoints(targetPoints);
	try {
		sourceMsk.getThread().join();
		targetMsk.getThread().join();
		sourceImg.getThread().join();
		targetImg.getThread().join();
	} catch (InterruptedException e) {
		IJ.log(
			"Unexpected interruption exception " + e.getMessage());
	}
	final turboRegFinalAction finalAction = new turboRegFinalAction(
		sourceImg, sourceMsk, sourcePh,
		targetImg, targetMsk, targetPh, transformation);
	finalAction.getThread().start();
	try {
		finalAction.getThread().join();
	} catch (InterruptedException e) {
		IJ.log(
			"Unexpected interruption exception " + e.getMessage());
	}
	sourcePoints = sourcePh.getPoints();
	targetPoints = targetPh.getPoints();
	final ResultsTable table = Analyzer.getResultsTable();
	table.reset();
	switch (transformation) {
		case turboRegDialog.TRANSLATION: {
			table.incrementCounter();
			sourcePoints[0][0] += sourceCrop[0];
			table.addValue("sourceX", sourcePoints[0][0]);
			sourcePoints[0][1] += sourceCrop[1];
			table.addValue("sourceY", sourcePoints[0][1]);
			targetPoints[0][0] += targetCrop[0];
			table.addValue("targetX", targetPoints[0][0]);
			targetPoints[0][1] += targetCrop[1];
			table.addValue("targetY", targetPoints[0][1]);
			break;
		}
		case turboRegDialog.SCALED_ROTATION: {
			for (int k = 0; (k < 2); k++) {
				table.incrementCounter();
				sourcePoints[k][0] += sourceCrop[0];
				table.addValue("sourceX", sourcePoints[k][0]);
				sourcePoints[k][1] += sourceCrop[1];
				table.addValue("sourceY", sourcePoints[k][1]);
				targetPoints[k][0] += targetCrop[0];
				table.addValue("targetX", targetPoints[k][0]);
				targetPoints[k][1] += targetCrop[1];
				table.addValue("targetY", targetPoints[k][1]);
			}
			break;
		}
		case turboRegDialog.RIGID_BODY:
		case turboRegDialog.AFFINE: {
			for (int k = 0; (k < 3); k++) {
				table.incrementCounter();
				sourcePoints[k][0] += sourceCrop[0];
				table.addValue("sourceX", sourcePoints[k][0]);
				sourcePoints[k][1] += sourceCrop[1];
				table.addValue("sourceY", sourcePoints[k][1]);
				targetPoints[k][0] += targetCrop[0];
				table.addValue("targetX", targetPoints[k][0]);
				targetPoints[k][1] += targetCrop[1];
				table.addValue("targetY", targetPoints[k][1]);
			}
			break;
		}
		case turboRegDialog.BILINEAR: {
			for (int k = 0; (k < 4); k++) {
				table.incrementCounter();
				sourcePoints[k][0] += sourceCrop[0];
				table.addValue("sourceX", sourcePoints[k][0]);
				sourcePoints[k][1] += sourceCrop[1];
				table.addValue("sourceY", sourcePoints[k][1]);
				targetPoints[k][0] += targetCrop[0];
				table.addValue("targetX", targetPoints[k][0]);
				targetPoints[k][1] += targetCrop[1];
				table.addValue("targetY", targetPoints[k][1]);
			}
			break;
		}
	}
	if (interactive) {
		table.show("Refined Landmarks");
	}
	source.killRoi();
	target.killRoi();
	return(transformImage(source, target.getWidth(), target.getHeight(),
		transformation, interactive));
} /* end alignImages */

/*------------------------------------------------------------------*/
private ImagePlus[] createAdmissibleImageList (
) {
	final int[] windowList = WindowManager.getIDList();
	final Stack<ImagePlus> stack = new Stack<ImagePlus>();
	for (int k = 0; ((windowList != null) && (k < windowList.length)); k++) {
		final ImagePlus imp = WindowManager.getImage(windowList[k]);
		if ((imp != null) && ((imp.getType() == imp.GRAY16)
			|| (imp.getType() == imp.GRAY32)
			|| ((imp.getType() == imp.GRAY8) && !imp.getStack().isHSB()))) {
			stack.push(imp);
		}
	}
	final ImagePlus[] admissibleImageList = new ImagePlus[stack.size()];
	int k = 0;
	while (!stack.isEmpty()) {
		admissibleImageList[k++] = stack.pop();
	}
	return(admissibleImageList);
} /* end createAdmissibleImageList */

/*------------------------------------------------------------------*/
private void dumpSyntax (
	final String options
) {
	IJ.write(options);
	IJ.write("");
	IJ.write("___");
	IJ.write("");
	IJ.write("ARGUMENTS: { -help | -align | -transform }");
	IJ.write("");
	IJ.write("-help SHOWS THIS MESSAGE");
	IJ.write("");
	IJ.write("-align");
	IJ.write("{ -file | -window }");
	IJ.write("  sourceFilename STRING WITH OPTIONAL QUOTES");
	IJ.write("  sourceWindowTitle STRING WITH OPTIONAL QUOTES");
	IJ.write("sourceCropLeft INTEGER");
	IJ.write("sourceCropTop INTEGER");
	IJ.write("sourceCropRight INTEGER");
	IJ.write("sourceCropBottom INTEGER");
	IJ.write("{ -file | -window }");
	IJ.write("  targetFilename STRING WITH OPTIONAL QUOTES");
	IJ.write("  targetWindowTitle STRING WITH OPTIONAL QUOTES");
	IJ.write("targetCropLeft INTEGER");
	IJ.write("targetCropTop INTEGER");
	IJ.write("targetCropRight INTEGER");
	IJ.write("targetCropBottom INTEGER");
	IJ.write(
		"{ -translation | -rigidBody | -scaledRotation | -affine | -bilinear }");
	IJ.write("sourcePointsX[<*>] FLOATING-POINT");
	IJ.write("sourcePointsY[<*>] FLOATING-POINT");
	IJ.write("targetPointsX[<*>] FLOATING-POINT");
	IJ.write("targetPointsY[<*>] FLOATING-POINT");
	IJ.write("{ -hideOutput | -showOutput }");
	IJ.write("");
	IJ.write("-transform");
	IJ.write("{ -file | -window }");
	IJ.write("  sourceFilename STRING WITH OPTIONAL QUOTES");
	IJ.write("  sourceWindowTitle STRING WITH OPTIONAL QUOTES");
	IJ.write("outputWidth INTEGER");
	IJ.write("outputHeight INTEGER");
	IJ.write(
		"{ -translation | -rigidBody | -scaledRotation | -affine | -bilinear }");
	IJ.write("sourcePointsX[<*>] FLOATING-POINT");
	IJ.write("sourcePointsY[<*>] FLOATING-POINT");
	IJ.write("targetPointsX[<*>] FLOATING-POINT");
	IJ.write("targetPointsY[<*>] FLOATING-POINT");
	IJ.write("{ -hideOutput | -showOutput }");
	IJ.write("");
	IJ.write("<*> FOR TRANSLATION: 1 BLOCK OF FOUR COORDINATES");
	IJ.write("<*> FOR RIGID-BODY: 3 BLOCKS OF FOUR COORDINATES");
	IJ.write("<*> FOR SCALED-ROTATION: 2 BLOCKS OF FOUR COORDINATES");
	IJ.write("<*> FOR AFFINE: 3 BLOCKS OF FOUR COORDINATES");
	IJ.write("<*> FOR BILINEAR: 4 BLOCKS OF FOUR COORDINATES");
	IJ.write("");
	IJ.write("~~~");
} /* end dumpSyntax */

/*------------------------------------------------------------------*/
private Boolean getInteractive (
	final String token
) {
	if (token.equals("-hideOutput")) {
		return(new Boolean(false));
	}
	else if (token.equals("-showOutput")) {
		return(new Boolean(true));
	}
	else {
		return(null);
	}
} /* end getInteractive */

/*------------------------------------------------------------------*/
private int getPyramidDepth (
	int sw,
	int sh,
	int tw,
	int th
) {
	int pyramidDepth = 1;
	while (((2 * turboRegDialog.MIN_SIZE) <= sw)
		&& ((2 * turboRegDialog.MIN_SIZE) <= sh)
		&& ((2 * turboRegDialog.MIN_SIZE) <= tw)
		&& ((2 * turboRegDialog.MIN_SIZE) <= th)) {
		sw /= 2;
		sh /= 2;
		tw /= 2;
		th /= 2;
		pyramidDepth++;
	}
	return(pyramidDepth);
} /* end getPyramidDepth */

/*------------------------------------------------------------------*/
private String[] getTokens (
	String options
) {
	final String fileSeparator = System.getProperty("file.separator");
	if (fileSeparator.equals("\\")) {
		options = options.replaceAll("\\\\", "/");
	}
	else {
		options = options.replaceAll(fileSeparator, "/");
	}
	String[] token = new String[0];
	final StringReader sr = new StringReader(options);
	final StreamTokenizer st = new StreamTokenizer(sr);
	st.resetSyntax();
	st.whitespaceChars(0, ' ');
	st.wordChars('!', 255);
	st.quoteChar('\"');
	final Vector<String> v = new Vector<String>();
	try {
		while (st.nextToken() != st.TT_EOF) {
			v.add(new String(st.sval));
		}
	} catch (IOException e) {
		IJ.log(
			"IOException exception " + e.getMessage());
		return(token);
	}
	token = v.toArray(token);
	return(token);
} /* end getTokens */

/*------------------------------------------------------------------*/
private int getTransformation (
	final String token
) {
	if (token.equals("-translation")) {
		return(turboRegDialog.TRANSLATION);
	}
	else if (token.equals("-rigidBody")) {
		return(turboRegDialog.RIGID_BODY);
	}
	else if (token.equals("-scaledRotation")) {
		return(turboRegDialog.SCALED_ROTATION);
	}
	else if (token.equals("-affine")) {
		return(turboRegDialog.AFFINE);
	}
	else if (token.equals("-bilinear")) {
		return(turboRegDialog.BILINEAR);
	}
	else {
		return(turboRegDialog.GENERIC_TRANSFORMATION);
	}
} /* end getTransformation */

/*------------------------------------------------------------------*/
private ImagePlus transformImage (
	final ImagePlus source,
	final int width,
	final int height,
	final int transformation,
	final boolean interactive
) {
	if ((source.getType() != source.GRAY16)
		&& (source.getType() != source.GRAY32)
		&& ((source.getType() != source.GRAY8)
		|| source.getStack().isRGB() || source.getStack().isHSB())) {
		IJ.error(
			source.getTitle() + " should be grayscale (8, 16, or 32 bit)");
		return(null);
	}
	source.setSlice(1);
	final turboRegImage sourceImg = new turboRegImage(source,
		turboRegDialog.GENERIC_TRANSFORMATION, false);
	sourceImg.getThread().start();
	if (2 <= source.getStackSize()) {
		source.setSlice(2);
	}
	final turboRegMask sourceMsk = new turboRegMask(source);
	source.setSlice(1);
	if (source.getStackSize() < 2) {
		sourceMsk.clearMask();
	}
	final turboRegPointHandler sourcePh = new turboRegPointHandler(
		sourcePoints, transformation);
	final turboRegPointHandler targetPh = new turboRegPointHandler(
		targetPoints, transformation);
	try {
		sourceImg.getThread().join();
	} catch (InterruptedException e) {
		IJ.log(
			"Unexpected interruption exception " + e.getMessage());
	}
	final turboRegTransform regTransform = new turboRegTransform(
		sourceImg, sourceMsk, sourcePh,
		null, null, targetPh, transformation, false, false);
	final ImagePlus transformedImage = regTransform.doFinalTransform(
		width, height);
	if (interactive) {
		transformedImage.setSlice(1);
		transformedImage.getProcessor().resetMinAndMax();
		transformedImage.show();
		transformedImage.updateAndDraw();
	}
	return(transformedImage);
} /* end transformImage */

} /* end class TurboReg_ */

/*====================================================================
|	turboRegCredits
\===================================================================*/

/*********************************************************************
 This class creates the credits dialog.
 ********************************************************************/
class turboRegCredits
	extends
		Dialog

{ /* begin class turboRegCredits */

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 Return some additional margin to the dialog, for aesthetic purposes.
 Necessary for the current MacOS X Java version, lest the first item
 disappears from the frame.
 ********************************************************************/
public Insets getInsets (
) {
	return(new Insets(0, 20, 20, 20));
} /* end getInsets */

/*********************************************************************
 This constructor prepares the dialog box.
 @param parentWindow Parent window.
 ********************************************************************/
public turboRegCredits (
	final Frame parentWindow
) {
	super(parentWindow, "TurboReg", true);
	setLayout(new BorderLayout(0, 20));
	final Label separation = new Label("");
	final Panel buttonPanel = new Panel();
	buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
	final Button doneButton = new Button("Done");
	doneButton.addActionListener(
		new ActionListener (
		) {
			public void actionPerformed (
				final ActionEvent ae
			) {
				if (ae.getActionCommand().equals("Done")) {
					dispose();
				}
			}
		}
	);
	buttonPanel.add(doneButton);
	final TextArea text = new TextArea(26, 72);
	text.setEditable(false);
	text.append(
		" \n");
	text.append(
		" This TurboReg version is dated June 19, 2008\n");
	text.append(
		" \n");
	text.append(
		" ###\n");
	text.append(
		" \n");
	text.append(
		" This work is based on the following paper:\n");
	text.append(
		"\n");
	text.append(
		" P. Th\u00E9venaz, U.E. Ruttimann, M. Unser\n");
	text.append(
		" A Pyramid Approach to Subpixel Registration Based on Intensity\n");
	text.append(
		" IEEE Transactions on Image Processing\n");
	text.append(
		" vol. 7, no. 1, pp. 27-41, January 1998.\n");
	text.append(
		"\n");
	text.append(
		" This paper is available on-line at\n");
	text.append(
		" http://bigwww.epfl.ch/publications/thevenaz9801.html\n");
	text.append(
		"\n");
	text.append(
		" Other relevant on-line publications are available at\n");
	text.append(
		" http://bigwww.epfl.ch/publications/\n");
	text.append(
		"\n");
	text.append(
		" Additional help available at\n");
	text.append(
		" http://bigwww.epfl.ch/thevenaz/turboreg/\n");
	text.append(
		"\n");
	text.append(
		" You'll be free to use this software for research purposes, but\n");
	text.append(
		" you should not redistribute it without our consent. In addition,\n");
	text.append(
		" we expect you to include a citation or acknowledgment whenever\n");
	text.append(
		" you present or publish results that are based on it.\n");
	add("North", separation);
	add("Center", text);
	add("South", buttonPanel);
	pack();
} /* end turboRegCredits */

} /* end class turboRegCredits */

/*====================================================================
|	turboRegDialog
\===================================================================*/

/*********************************************************************
 This class creates the main dialog.
 ********************************************************************/
class turboRegDialog
	extends
		Dialog
	implements
		ActionListener

{ /* begin class turboRegDialog */

/*....................................................................
	Public variables
....................................................................*/

/*********************************************************************
 Three points generate an affine transformation, which is any
 combination of translation, rotation, isotropic scaling, anisotropic
 scaling, shearing, and skewing. An affine transformation maps
 parallel lines onto parallel lines and is determined by 6 parameters.
 ********************************************************************/
public static final int AFFINE = 6;

/*********************************************************************
 Generic geometric transformation.
 ********************************************************************/
public static final int GENERIC_TRANSFORMATION = -1;

/*********************************************************************
 Four points describe a bilinear transformation, where a point of
 coordinates (x, y) is mapped on a point of coordinates (u, v) such
 that u = p0 + p1 x + p2 y + p3 x y and v = q0 + q1 x + q2 y + q3 x y.
 Thus, u and v are both linear in x, and in y as well. The bilinear
 transformation is determined by 8 parameters.
 ********************************************************************/
public static final int BILINEAR = 8;

/*********************************************************************
 Blue slice.
 ********************************************************************/
public static final int BLUE = 3;

/*********************************************************************
 Graylevel slice.
 ********************************************************************/
public static final int BLACK = 0;

/*********************************************************************
 Green slice.
 ********************************************************************/
public static final int GREEN = 2;

/*********************************************************************
 Minimal linear dimension of an image in the multiresolution pyramid.
 ********************************************************************/
public static final int MIN_SIZE = 12;

/*********************************************************************
 Red slice.
 ********************************************************************/
public static final int RED = 1;

/*********************************************************************
 A single points determines the translation component of a rigid-body
 transformation. As the rotation is given by a scalar number, it is
 not natural to represent this number by coordinates of a point. The
 rigid-body transformation is determined by 3 parameters.
 ********************************************************************/
public static final int RIGID_BODY = 3;

/*********************************************************************
 A pair of points determines the combination of a translation, of
 a rotation, and of an isotropic scaling. Angles are conserved. A
 scaled rotation is determined by 4 parameters.
 ********************************************************************/
public static final int SCALED_ROTATION = 4;

/*********************************************************************
 A translation is described by a single point. It keeps area, angle,
 and orientation. A translation is determined by 2 parameters.
 ********************************************************************/
public static final int TRANSLATION = 2;

/*....................................................................
	Private variables
....................................................................*/

/*********************************************************************
 Admissible values are {<code>TRANSLATION</code>, <code>RIGID_BODY</code>,
 <code>SCALED_ROTATION</code>, <code>AFFINE</code>, <code>BILINEAR<code>}.
 ********************************************************************/
private static int transformation = RIGID_BODY;
private static int sourceColorPlane = BLACK;
private static int targetColorPlane = BLACK;
private static boolean saveOnExit = false;
private static boolean accelerated = true;
private final turboRegFinalAction finalAction = new turboRegFinalAction(this);
private final turboRegPointToolbar tb = new turboRegPointToolbar(
	Toolbar.getInstance());
private final Button automaticButton = new Button("Automatic");
private final Button batchButton = new Button("Batch");
private final CheckboxGroup sourceKrgbGroup = new CheckboxGroup();
private final Checkbox sourceBlack = new Checkbox(
	"K", sourceKrgbGroup, sourceColorPlane == BLACK);
private final Checkbox sourceRed = new Checkbox(
	"R", sourceKrgbGroup, sourceColorPlane == RED);
private final Checkbox sourceGreen = new Checkbox(
	"G", sourceKrgbGroup, sourceColorPlane == GREEN);
private final Checkbox sourceBlue = new Checkbox(
	"B", sourceKrgbGroup, sourceColorPlane == BLUE);
private final CheckboxGroup targetKrgbGroup = new CheckboxGroup();
private final Checkbox targetBlack = new Checkbox(
	"K", targetKrgbGroup, targetColorPlane == BLACK);
private final Checkbox targetRed = new Checkbox(
	"R", targetKrgbGroup, targetColorPlane == RED);
private final Checkbox targetGreen = new Checkbox(
	"G", targetKrgbGroup, targetColorPlane == GREEN);
private final Checkbox targetBlue = new Checkbox(
	"B", targetKrgbGroup, targetColorPlane == BLUE);
private final CheckboxGroup transformationGroup = new CheckboxGroup();
private final Checkbox translation = new Checkbox(
	"Translation", transformationGroup, transformation == TRANSLATION);
private final Checkbox rigidBody = new Checkbox(
	"Rigid Body", transformationGroup, transformation == RIGID_BODY);
private final Checkbox scaledRotation = new Checkbox(
	"Scaled Rotation", transformationGroup, transformation == SCALED_ROTATION);
private final Checkbox affine = new Checkbox(
	"Affine", transformationGroup, transformation == AFFINE);
private final Checkbox bilinear = new Checkbox(
	"Bilinear", transformationGroup, transformation == BILINEAR);
private ImagePlus[] admissibleImageList;
private ImageCanvas sourceIc;
private ImageCanvas targetIc;
private ImagePlus sourceImp;
private ImagePlus targetImp;
private	Scrollbar sourceScrollbar;
private	Scrollbar targetScrollbar;
private turboRegImage sourceImg;
private turboRegImage targetImg;
private turboRegMask sourceMsk;
private turboRegMask targetMsk;
private turboRegPointAction sourcePa;
private turboRegPointAction targetPa;
private turboRegPointHandler sourcePh;
private turboRegPointHandler targetPh;
private int sourceChoiceIndex = 0;
private int targetChoiceIndex = 1;
private int pyramidDepth;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 This method processes the button actions.
 @param ae The expected actions are as follows:
 <ul><li><code>Load...</code>: Load landmarks from a file;</li>
 <li><code>Save Now...</code>: Save landmarks into a file;</li>
 <li><code>Cancel</code>: Restore the progress bar and the toolbar,
 reset the ROI's, and then quit;</li>
 <li><code>Manual</code>: Create an output image that is distorted in
 such a way that the landmarks of the source are made to coincide
 with those of the target;</li>
 <li><code>Automatic</code>: Refine the landmarks of the source image
 in such a way that the least-squares error between the transformed
 source image and the target is minimized.</li></ul>
 ********************************************************************/
public void actionPerformed (
	final ActionEvent ae
) {
	if (ae.getActionCommand().equals("Load...")) {
		if (!loadLandmarks()) {
			IJ.error(
				"Invalid Landmarks");
		}
	}
	else if (ae.getActionCommand().equals("Save Now...")) {
		final turboRegTransform tt = new turboRegTransform(
			sourceImg, sourceMsk, sourcePh,
			targetImg, targetMsk, targetPh, transformation, accelerated, true);
		tt.saveTransformation(null);
	}
	else if (ae.getActionCommand().equals("Cancel")) {
		dispose();
		restoreAll();
	}
	else if (ae.getActionCommand().equals("Manual")) {
		dispose();
		if (sourceImp.getStack().isRGB()) {
			finalAction.setup(
				sourceImp, sourceImg, sourceMsk, sourcePh, sourceColorPlane,
				targetImg, targetMsk, targetPh, transformation, accelerated,
				saveOnExit, turboRegFinalAction.MANUAL);
		}
		else {
			finalAction.setup(sourceImg, sourceMsk, sourcePh,
				targetImg, targetMsk, targetPh, transformation, accelerated,
				saveOnExit, turboRegFinalAction.MANUAL);
		}
		finalAction.getThread().start();
	}
	else if (ae.getActionCommand().equals("Automatic")) {
		dispose();
		if (sourceImp.getStack().isRGB()) {
			finalAction.setup(
				sourceImp, sourceImg, sourceMsk, sourcePh, sourceColorPlane,
				targetImg, targetMsk, targetPh, transformation, accelerated,
				saveOnExit, turboRegFinalAction.AUTOMATIC);
		}
		else {
			finalAction.setup(sourceImg, sourceMsk, sourcePh,
				targetImg, targetMsk, targetPh, transformation, accelerated,
				saveOnExit, turboRegFinalAction.AUTOMATIC);
		}
		finalAction.getThread().start();
	}
	else if (ae.getActionCommand().equals("Batch")) {
		dispose();
		finalAction.setup(sourceImp, sourceImg, sourcePh,
			targetImp, targetImg, targetMsk, targetPh,
			transformation, accelerated, saveOnExit, pyramidDepth);
		finalAction.getThread().start();
	}
	else if (ae.getActionCommand().equals("Credits...")) {
		final turboRegCredits dialog = new turboRegCredits(IJ.getInstance());
		GUI.center(dialog);
		dialog.setVisible(true);
	}
} /* end actionPerformed */

/*********************************************************************
 Return some additional margin to the dialog, for aesthetic purposes.
 Necessary for the current MacOS X Java version, lest the first item
 disappears from the frame.
 ********************************************************************/
public Insets getInsets (
) {
	return(new Insets(0, 20, 20, 20));
} /* end getInsets */

/*********************************************************************
 Wait until the threads that compute spline coefficients, image and
 mask pyramids are done.
 ********************************************************************/
public void joinThreads (
) {
	try {
		sourceImg.getThread().join();
		sourceMsk.getThread().join();
		targetImg.getThread().join();
		targetMsk.getThread().join();
	} catch (InterruptedException e) {
		IJ.log(
			"Unexpected interruption exception " + e.getMessage());
	}
} /* end joinThreads */

/*********************************************************************
 Restore the regular listener interfaces, restore the regular ImageJ
 toolbar, stop the progress bar, and ask for garbage collection.
 @see turboRegDialog#cancelImages()
 ********************************************************************/
public void restoreAll (
) {
	cancelImages();
	tb.restorePreviousToolbar();
	Toolbar.getInstance().repaint();
	turboRegProgressBar.resetProgressBar();
	Runtime.getRuntime().gc();
} /* end restoreAll */

/*********************************************************************
 Interrupt the two preprocessing threads.
 ********************************************************************/
public void stopThreads (
) {
	stopSourceThreads();
	stopTargetThreads();
} /* end stopThreads */

/*********************************************************************
 This constructor prepares the dialog box and starts the first
 computational threads. The threads will need to be killed and
 restarted upon need. This constructor must be called when at least
 two images are available.
 @param parentWindow Parent window.
 @param admissibleImageList Array of <code>ImagePlus</code> images
 that are candidate to registration.
 ********************************************************************/
public turboRegDialog (
	final Frame parentWindow,
	final ImagePlus[] admissibleImageList
) {
	super(parentWindow, "TurboReg", false);
	this.admissibleImageList = admissibleImageList;
	defaultSourceColorPlane();
	defaultTargetColorPlane();
	createImages();
	startThreads();
	setLayout(new GridLayout(0, 1));
	final Choice sourceChoice = new Choice();
	for (int k = 0; (k < admissibleImageList.length); k++) {
		sourceChoice.add(admissibleImageList[k].getTitle());
	}
	sourceChoice.select(sourceChoiceIndex);
	final Choice targetChoice = new Choice();
	for (int k = 0; (k < admissibleImageList.length); k++) {
		targetChoice.add(admissibleImageList[k].getTitle());
	}
	targetChoice.select(targetChoiceIndex);
	final Panel sourcePanel = new Panel();
	sourcePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
	sourceChoice.addItemListener(
		new ItemListener (
		) {
			public void itemStateChanged (
				final ItemEvent ie
			) {
				final int newChoiceIndex = sourceChoice.getSelectedIndex();
				if (sourceChoiceIndex != newChoiceIndex) {
					if (targetChoiceIndex != newChoiceIndex) {
						sourceChoiceIndex = newChoiceIndex;
						cancelSource();
						defaultSourceColorPlane();
						createSourceImages();
						startSourceThreads();
					}
					else {
						targetChoiceIndex = sourceChoiceIndex;
						sourceChoiceIndex = newChoiceIndex;
						targetChoice.select(targetChoiceIndex);
						permuteImages();
						startThreads();
					}
					sourcePa.setSecondaryPointHandler(targetImp, targetPh);
					targetPa.setSecondaryPointHandler(sourceImp, sourcePh);
					batchButton.setEnabled((1 < sourceImp.getStackSize())
						&& !(sourceImp.getStack().isRGB()
						|| targetImp.getStack().isRGB()));
				}
				repaint();
			}
		}
	);
	sourceRed.addItemListener(
		new ItemListener (
		) {
			public void itemStateChanged (
				final ItemEvent ie
			) {
				if (sourceKrgbGroup.getSelectedCheckbox().getLabel().equals("R")
					&& (sourceColorPlane != RED)) {
					sourceKrgbGroup.setSelectedCheckbox(sourceRed);
					stopSourceThreads();
					setSourceColorPlane(RED);
					startSourceThreads();
				}
				repaint();
			}
		}
	);
	sourceGreen.addItemListener(
		new ItemListener (
		) {
			public void itemStateChanged (
				final ItemEvent ie
			) {
				if (sourceKrgbGroup.getSelectedCheckbox().getLabel().equals("G")
					&& (sourceColorPlane != GREEN)) {
					sourceKrgbGroup.setSelectedCheckbox(sourceGreen);
					stopSourceThreads();
					setSourceColorPlane(GREEN);
					startSourceThreads();
				}
				repaint();
			}
		}
	);
	sourceBlue.addItemListener(
		new ItemListener (
		) {
			public void itemStateChanged (
				final ItemEvent ie
			) {
				if (sourceKrgbGroup.getSelectedCheckbox().getLabel().equals("B")
					&& (sourceColorPlane != BLUE)) {
					sourceKrgbGroup.setSelectedCheckbox(sourceBlue);
					stopSourceThreads();
					setSourceColorPlane(BLUE);
					startSourceThreads();
				}
				repaint();
			}
		}
	);
	final Label sourceLabel = new Label("Source: ");
	sourcePanel.add(sourceLabel);
	sourcePanel.add(sourceChoice);
	sourcePanel.add(sourceRed);
	sourcePanel.add(sourceGreen);
	sourcePanel.add(sourceBlue);
	sourcePanel.add(sourceBlack);
	final Panel targetPanel = new Panel();
	targetPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
	targetChoice.addItemListener(
		new ItemListener (
		) {
			public void itemStateChanged (
				final ItemEvent ie
			) {
				final int newChoiceIndex = targetChoice.getSelectedIndex();
				if (targetChoiceIndex != newChoiceIndex) {
					if (sourceChoiceIndex != newChoiceIndex) {
						targetChoiceIndex = newChoiceIndex;
						cancelTarget();
						defaultTargetColorPlane();
						createTargetImages();
						startTargetThreads();
					}
					else {
						sourceChoiceIndex = targetChoiceIndex;
						targetChoiceIndex = newChoiceIndex;
						sourceChoice.select(sourceChoiceIndex);
						permuteImages();
						startThreads();
					}
					sourcePa.setSecondaryPointHandler(targetImp, targetPh);
					targetPa.setSecondaryPointHandler(sourceImp, sourcePh);
					batchButton.setEnabled((1 < sourceImp.getStackSize())
						&& !(sourceImp.getStack().isRGB()
						|| targetImp.getStack().isRGB()));
				}
				repaint();
			}
		}
	);
	targetRed.addItemListener(
		new ItemListener (
		) {
			public void itemStateChanged (
				final ItemEvent ie
			) {
				if (targetKrgbGroup.getSelectedCheckbox().getLabel().equals("R")
					&& (targetColorPlane != RED)) {
					targetKrgbGroup.setSelectedCheckbox(targetRed);
					stopTargetThreads();
					setTargetColorPlane(RED);
					startTargetThreads();
				}
				repaint();
			}
		}
	);
	targetGreen.addItemListener(
		new ItemListener (
		) {
			public void itemStateChanged (
				final ItemEvent ie
			) {
				if (targetKrgbGroup.getSelectedCheckbox().getLabel().equals("G")
					&& (targetColorPlane != GREEN)) {
					targetKrgbGroup.setSelectedCheckbox(targetGreen);
					stopTargetThreads();
					setTargetColorPlane(GREEN);
					startTargetThreads();
				}
				repaint();
			}
		}
	);
	targetBlue.addItemListener(
		new ItemListener (
		) {
			public void itemStateChanged (
				final ItemEvent ie
			) {
				if (targetKrgbGroup.getSelectedCheckbox().getLabel().equals("B")
					&& (targetColorPlane != BLUE)) {
					targetKrgbGroup.setSelectedCheckbox(targetBlue);
					stopTargetThreads();
					setTargetColorPlane(BLUE);
					startTargetThreads();
				}
				repaint();
			}
		}
	);
	final Label targetLabel = new Label("Target: ");
	targetPanel.add(targetLabel);
	targetPanel.add(targetChoice);
	targetPanel.add(targetRed);
	targetPanel.add(targetGreen);
	targetPanel.add(targetBlue);
	targetPanel.add(targetBlack);
	final Panel transformationPanel = new Panel();
	transformationPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
	translation.addItemListener(
		new ItemListener (
		) {
			public void itemStateChanged (
				final ItemEvent ie
			) {
				if (transformationGroup.getSelectedCheckbox().getLabel().equals(
					"Translation") && (transformation != TRANSLATION)) {
					if (transformation == BILINEAR) {
						transformation = TRANSLATION;
						cancelImages();
						createImages();
						startThreads();
					}
					else {
						transformation = TRANSLATION;
						setTransformation();
					}
				}
				repaint();
			}
		}
	);
	rigidBody.addItemListener(
		new ItemListener (
		) {
			public void itemStateChanged (
				final ItemEvent ie
			) {
				if (transformationGroup.getSelectedCheckbox().getLabel().equals(
					"Rigid Body") && (transformation != RIGID_BODY)) {
					if (transformation == BILINEAR) {
						transformation = RIGID_BODY;
						cancelImages();
						createImages();
						startThreads();
					}
					else {
						transformation = RIGID_BODY;
						setTransformation();
					}
				}
				repaint();
			}
		}
	);
	scaledRotation.addItemListener(
		new ItemListener (
		) {
			public void itemStateChanged (
				final ItemEvent ie
			) {
				if (transformationGroup.getSelectedCheckbox().getLabel().equals(
					"Scaled Rotation") && (transformation != SCALED_ROTATION)) {
					if (transformation == BILINEAR) {
						transformation = SCALED_ROTATION;
						cancelImages();
						createImages();
						startThreads();
					}
					else {
						transformation = SCALED_ROTATION;
						setTransformation();
					}
				}
				repaint();
			}
		}
	);
	affine.addItemListener(
		new ItemListener (
		) {
			public void itemStateChanged (
				final ItemEvent ie
			) {
				if (transformationGroup.getSelectedCheckbox().getLabel().equals(
					"Affine") && (transformation != AFFINE)) {
					if (transformation == BILINEAR) {
						transformation = AFFINE;
						cancelImages();
						createImages();
						startThreads();
					}
					else {
						transformation = AFFINE;
						setTransformation();
					}
				}
				repaint();
			}
		}
	);
	bilinear.addItemListener(
		new ItemListener (
		) {
			public void itemStateChanged (
				final ItemEvent ie
			) {
				if (transformationGroup.getSelectedCheckbox().getLabel().equals(
					"Bilinear") && (transformation != BILINEAR)) {
					transformation = BILINEAR;
					cancelImages();
					createImages();
					startThreads();
				}
				repaint();
			}
		}
	);
	transformationPanel.add(translation);
	transformationPanel.add(rigidBody);
	transformationPanel.add(scaledRotation);
	transformationPanel.add(affine);
	transformationPanel.add(bilinear);
	final Panel pointPanel = new Panel();
	pointPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
	final Label pointLabel = new Label("Landmarks: ");
	final Button loadButton = new Button("Load...");
	loadButton.addActionListener(this);
	final Button saveButton = new Button("Save Now...");
	saveButton.addActionListener(this);
	final Checkbox saveCheck = new Checkbox("Save on Exit", saveOnExit);
	saveCheck.addItemListener(
		new ItemListener (
		) {
			public void itemStateChanged (
				final ItemEvent ie
			) {
				saveOnExit = saveCheck.getState();
				repaint();
			}
		}
	);
	pointPanel.add(pointLabel);
	pointPanel.add(loadButton);
	pointPanel.add(saveButton);
	pointPanel.add(saveCheck);
	final Panel accelerationPanel = new Panel();
	accelerationPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
	final Label acceleratedLabel = new Label("Quality: ");
	final CheckboxGroup accelerationGroup = new CheckboxGroup();
	final Checkbox fast = new Checkbox(
		"Fast", accelerationGroup, accelerated);
	final Checkbox accurate = new Checkbox(
		"Accurate", accelerationGroup, !accelerated);
	fast.addItemListener(
		new ItemListener (
		) {
			public void itemStateChanged (
				final ItemEvent ie
			) {
				if (accelerationGroup.getSelectedCheckbox().getLabel().equals(
					"Fast")) {
					accelerated = true;
				}
				automaticButton.setEnabled((!accelerated)
					|| (1 < pyramidDepth));
				repaint();
			}
		}
	);
	accurate.addItemListener(
		new ItemListener (
		) {
			public void itemStateChanged (
				final ItemEvent ie
			) {
				if (accelerationGroup.getSelectedCheckbox().getLabel().equals(
					"Accurate")) {
					accelerated = false;
				}
				automaticButton.setEnabled((!accelerated)
					|| (1 < pyramidDepth));
				repaint();
			}
		}
	);
	accelerationPanel.add(acceleratedLabel);
	accelerationPanel.add(fast);
	accelerationPanel.add(accurate);
	final Panel creditsPanel = new Panel();
	creditsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
	final Button creditsButton = new Button("Credits...");
	creditsButton.addActionListener(this);
	creditsPanel.add(creditsButton);
	final Panel buttonPanel = new Panel();
	buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
	final Button cancelButton = new Button("Cancel");
	cancelButton.addActionListener(this);
	final Button manualButton = new Button("Manual");
	manualButton.addActionListener(this);
	automaticButton.addActionListener(this);
	batchButton.setEnabled((1 < sourceImp.getStackSize())
		&& !(sourceImp.getStack().isRGB() || targetImp.getStack().isRGB()));
	batchButton.addActionListener(this);
	buttonPanel.add(cancelButton);
	buttonPanel.add(manualButton);
	buttonPanel.add(automaticButton);
	buttonPanel.add(batchButton);
	final Label separation1 = new Label("");
	final Label separation2 = new Label("");
	add(separation1);
	add(sourcePanel);
	add(targetPanel);
	add(transformationPanel);
	add(pointPanel);
	add(accelerationPanel);
	add(separation2);
	add(buttonPanel);
	add(creditsPanel);
	pack();
} /* end turboRegDialog */

/*....................................................................
	Private methods
....................................................................*/

/*------------------------------------------------------------------*/
private void cancelImages (
) {
	cancelSource();
	cancelTarget();
} /* end cancelImages */

/*------------------------------------------------------------------*/
private void cancelSource (
) {
	cancelSource(sourcePh);
	this.sourcePh = null;
	Runtime.getRuntime().gc();
} /* end cancelSource */

/*------------------------------------------------------------------*/
private double[][] cancelSource (
	final turboRegPointHandler sourcePh
) {
	stopSourceThreads();
	if (sourceScrollbar != null) {
		sourceScrollbar.removeAdjustmentListener(sourcePa);
		sourceScrollbar = null;
	}
	final ImageWindow iw = sourceImp.getWindow();
	iw.removeKeyListener(sourcePa);
	sourceIc.removeKeyListener(sourcePa);
	sourceIc.removeMouseMotionListener(sourcePa);
	sourceIc.removeMouseListener(sourcePa);
	sourceIc.addMouseListener(sourceIc);
	sourceIc.addMouseMotionListener(sourceIc);
	sourceIc.addKeyListener(IJ.getInstance());
	iw.addKeyListener(IJ.getInstance());
	sourceIc = null;
	sourceImp.killRoi();
	sourceImp.updateAndDraw();
	sourceImp = null;
	sourceImg = null;
	sourceMsk = null;
	sourcePa = null;
	sourceScrollbar = null;
	return(sourcePh.getPoints());
} /* end cancelSource */

/*------------------------------------------------------------------*/
private void cancelTarget (
) {
	cancelTarget(targetPh);
	this.targetPh = null;
	Runtime.getRuntime().gc();
} /* end cancelTarget */

/*------------------------------------------------------------------*/
private double[][] cancelTarget (
	final turboRegPointHandler targetPh
) {
	stopTargetThreads();
	if (targetScrollbar != null) {
		targetScrollbar.removeAdjustmentListener(targetPa);
		targetScrollbar = null;
	}
	final ImageWindow iw = targetImp.getWindow();
	iw.removeKeyListener(targetPa);
	targetIc.removeKeyListener(targetPa);
	targetIc.removeMouseMotionListener(targetPa);
	targetIc.removeMouseListener(targetPa);
	targetIc.addMouseListener(targetIc);
	targetIc.addMouseMotionListener(targetIc);
	targetIc.addKeyListener(IJ.getInstance());
	iw.addKeyListener(IJ.getInstance());
	targetIc = null;
	targetImp.killRoi();
	targetImp.updateAndDraw();
	targetImp = null;
	targetImg = null;
	targetMsk = null;
	targetPa = null;
	targetScrollbar = null;
	return(targetPh.getPoints());
} /* end cancelTarget */

/*------------------------------------------------------------------*/
private void createImages (
) {
	createSourceImages();
	createTargetImages();
	sourcePa.setSecondaryPointHandler(targetImp, targetPh);
	targetPa.setSecondaryPointHandler(sourceImp, sourcePh);
	getPyramidDepth();
	sourceImg.setPyramidDepth(pyramidDepth);
	sourceMsk.setPyramidDepth(pyramidDepth);
	targetImg.setPyramidDepth(pyramidDepth);
	targetMsk.setPyramidDepth(pyramidDepth);
} /* end createImages */

/*------------------------------------------------------------------*/
private void createSourceImages (
) {
	sourceImp = admissibleImageList[sourceChoiceIndex];
	if (sourceImp.getStack().isRGB()) {
		sourceBlack.setEnabled(false);
		sourceRed.setEnabled(true);
		sourceGreen.setEnabled(true);
		sourceBlue.setEnabled(true);
		switch (sourceColorPlane) {
			case RED: {
				sourceKrgbGroup.setSelectedCheckbox(sourceRed);
				break;
			}
			case GREEN: {
				sourceKrgbGroup.setSelectedCheckbox(sourceGreen);
				break;
			}
			case BLUE: {
				sourceKrgbGroup.setSelectedCheckbox(sourceBlue);
				break;
			}
		}
		sourceImp.setSlice(sourceColorPlane);
		sourceImg = new turboRegImage(sourceImp, transformation, false);
		sourceMsk = new turboRegMask(sourceImp);
		sourceMsk.clearMask();
	}
	else {
		sourceBlack.setEnabled(true);
		sourceRed.setEnabled(false);
		sourceGreen.setEnabled(false);
		sourceBlue.setEnabled(false);
		sourceKrgbGroup.setSelectedCheckbox(sourceBlack);
		sourceColorPlane = 0;
		sourceImp.setSlice(1);
		sourceImg = new turboRegImage(sourceImp, transformation, false);
		if (2 <= sourceImp.getStackSize()) {
			sourceImp.setSlice(2);
		}
		sourceMsk = new turboRegMask(sourceImp);
		sourceImp.setSlice(1);
		if (sourceImp.getStackSize() < 2) {
			sourceMsk.clearMask();
		}
	}
	final ImageWindow iw = sourceImp.getWindow();
	sourceIc = iw.getCanvas();
	iw.removeKeyListener(IJ.getInstance());
	sourceIc.removeKeyListener(IJ.getInstance());
	sourceIc.removeMouseMotionListener(sourceIc);
	sourceIc.removeMouseListener(sourceIc);
	sourcePh = new turboRegPointHandler(sourceImp, transformation);
	sourcePa = new turboRegPointAction(sourceImp, sourcePh, tb);
	sourceIc.addMouseListener(sourcePa);
	sourceIc.addMouseMotionListener(sourcePa);
	sourceIc.addKeyListener(sourcePa);
	iw.addKeyListener(sourcePa);
	if (sourceImp.getWindow() instanceof StackWindow) {
		StackWindow sw = (StackWindow)sourceImp.getWindow();
		final Component component[] = sw.getComponents();
		for (int i = 0; (i < component.length); i++) {
			if (component[i] instanceof Scrollbar) {
				sourceScrollbar = (Scrollbar)component[i];
				sourceScrollbar.addAdjustmentListener(sourcePa);
			}
		}
	}
	else {
		sourceScrollbar = null;
	}
	sourceImp.updateAndDraw();
} /* end createSourceImages */

/*------------------------------------------------------------------*/
private void createTargetImages (
) {
	targetImp = admissibleImageList[targetChoiceIndex];
	if (targetImp.getStack().isRGB()) {
		targetBlack.setEnabled(false);
		targetRed.setEnabled(true);
		targetGreen.setEnabled(true);
		targetBlue.setEnabled(true);
		switch (targetColorPlane) {
			case RED: {
				targetKrgbGroup.setSelectedCheckbox(targetRed);
				break;
			}
			case GREEN: {
				targetKrgbGroup.setSelectedCheckbox(targetGreen);
				break;
			}
			case BLUE: {
				targetKrgbGroup.setSelectedCheckbox(targetBlue);
				break;
			}
		}
		targetImp.setSlice(targetColorPlane);
		targetImg = new turboRegImage(targetImp, transformation, true);
		targetMsk = new turboRegMask(targetImp);
		targetMsk.clearMask();
	}
	else {
		targetBlack.setEnabled(true);
		targetRed.setEnabled(false);
		targetGreen.setEnabled(false);
		targetBlue.setEnabled(false);
		targetKrgbGroup.setSelectedCheckbox(targetBlack);
		targetColorPlane = 0;
		targetImp.setSlice(1);
		targetImg = new turboRegImage(targetImp, transformation, true);
		if (2 <= targetImp.getStackSize()) {
			targetImp.setSlice(2);
		}
		targetMsk = new turboRegMask(targetImp);
		targetImp.setSlice(1);
		if (targetImp.getStackSize() < 2) {
			targetMsk.clearMask();
		}
	}
	final ImageWindow iw = targetImp.getWindow();
	targetIc = iw.getCanvas();
	iw.removeKeyListener(IJ.getInstance());
	targetIc.removeKeyListener(IJ.getInstance());
	targetIc.removeMouseMotionListener(targetIc);
	targetIc.removeMouseListener(targetIc);
	targetPh = new turboRegPointHandler(targetImp, transformation);
	targetPa = new turboRegPointAction(targetImp, targetPh, tb);
	targetIc.addMouseListener(targetPa);
	targetIc.addMouseMotionListener(targetPa);
	targetIc.addKeyListener(targetPa);
	iw.addKeyListener(targetPa);
	if (targetImp.getWindow() instanceof StackWindow) {
		StackWindow sw = (StackWindow)targetImp.getWindow();
		final Component component[] = sw.getComponents();
		for (int i = 0; (i < component.length); i++) {
			if (component[i] instanceof Scrollbar) {
				targetScrollbar = (Scrollbar)component[i];
				targetScrollbar.addAdjustmentListener(targetPa);
			}
		}
	}
	else {
		targetScrollbar = null;
	}
	targetImp.updateAndDraw();
} /* end createTargetImages */

/*------------------------------------------------------------------*/
private void defaultSourceColorPlane (
) {
	sourceImp = admissibleImageList[sourceChoiceIndex];
	if (sourceImp.getStack().isRGB()) {
		sourceColorPlane = sourceImp.getCurrentSlice();
	}
	else {
		sourceColorPlane = 0;
	}
} /* end defaultSourceColorPlane */

/*------------------------------------------------------------------*/
private void defaultTargetColorPlane (
) {
	targetImp = admissibleImageList[targetChoiceIndex];
	if (targetImp.getStack().isRGB()) {
		targetColorPlane = targetImp.getCurrentSlice();
	}
	else {
		targetColorPlane = 0;
	}
} /* end defaultTargetColorPlane */

/*------------------------------------------------------------------*/
private void getPyramidDepth (
) {
	int sw = sourceImp.getWidth();
	int sh = sourceImp.getHeight();
	int tw = targetImp.getWidth();
	int th = targetImp.getHeight();
	pyramidDepth = 1;
	while (((2 * MIN_SIZE) <= sw) && ((2 * MIN_SIZE) <= sh)
		&& ((2 * MIN_SIZE) <= tw) && ((2 * MIN_SIZE) <= th)) {
		sw /= 2;
		sh /= 2;
		tw /= 2;
		th /= 2;
		pyramidDepth++;
	}
	automaticButton.setEnabled((!accelerated) || (1 < pyramidDepth));
} /* end getPyramidDepth */

/*------------------------------------------------------------------*/
private boolean loadLandmarks (
) {
	final Frame f = new Frame();
	final FileDialog fd = new FileDialog(f, "Landmarks", FileDialog.LOAD);
	fd.setVisible(true);
	String path = fd.getDirectory();
	String filename = fd.getFile();
	if ((path == null) || (filename == null)) {
		return(true);
	}
	final double[][] targetPoint =
		new double[turboRegPointHandler.NUM_POINTS][2];
	final double[][] sourcePoint =
		new double[turboRegPointHandler.NUM_POINTS][2];
	int transformation;
	try {
		final FileReader fr = new FileReader(path + filename);
		final BufferedReader br = new BufferedReader(fr);
		String line;
		String xString;
		String yString;
		int separatorIndex;
		int k = 1;
		if (!(line = br.readLine()).equals("Transformation")) {
			fr.close();
			IJ.write("Line " + k + ": 'Transformation'");
			return(false);
		}
		++k;
		line = br.readLine();
		if (line.equals("TRANSLATION")) {
			transformation = TRANSLATION;
		}
		else if (line.equals("RIGID_BODY")) {
			transformation = RIGID_BODY;
		}
		else if (line.equals("SCALED_ROTATION")) {
			transformation = SCALED_ROTATION;
		}
		else if (line.equals("AFFINE")) {
			transformation = AFFINE;
		}
		else if (line.equals("BILINEAR")) {
			transformation = BILINEAR;
		}
		else {
			fr.close();
			IJ.write("Line " + k 
				+ ": 'TRANSLATION' "
				+ "| 'RIGID_BODY' "
				+ "| 'SCALED_ROTATION' "
				+ "| 'AFFINE' "
				+ "| 'BILINEAR'");
			return(false);
		}
		++k;
		if (!(line = br.readLine()).equals("")) {
			fr.close();
			IJ.write("Line " + k + ": ''");
			return(false);
		}
		++k;
		if (!(line = br.readLine()).equals("Source size")) {
			fr.close();
			IJ.write("Line " + k + ": 'Source size'");
			return(false);
		}
		++k;
		if ((line = br.readLine()) == null) {
			fr.close();
			IJ.write("Line " + k + ": #sourceWidth# <tab> #sourceHeight#");
			return(false);
		}
		line = line.trim();
		separatorIndex = line.indexOf('\t');
		if (separatorIndex == -1) {
			fr.close();
			IJ.write("Line " + k + ": #sourceWidth# <tab> #sourceHeight#");
			return(false);
		}
		xString = line.substring(0, separatorIndex);
		yString = line.substring(separatorIndex);
		xString = xString.trim();
		yString = yString.trim();
		if (Integer.parseInt(xString) != sourceImp.getWidth()) {
			fr.close();
			IJ.write("Line " + k + ": The source width"
				+ " should not differ from that in the landmarks file");
			return(false);
		}
		if (Integer.parseInt(yString) != sourceImp.getHeight()) {
			fr.close();
			IJ.write("Line " + k + ": The source height"
				+ " should not differ from that in the landmarks file");
			return(false);
		}
		++k;
		if (!(line = br.readLine()).equals("")) {
			fr.close();
			IJ.write("Line " + k + ": ''");
			return(false);
		}
		++k;
		if (!(line = br.readLine()).equals("Target size")) {
			fr.close();
			IJ.write("Line " + k + ": 'Target size'");
			return(false);
		}
		++k;
		if ((line = br.readLine()) == null) {
			fr.close();
			IJ.write("Line " + k + ": #targetWidth# <tab> #targetHeight#");
			return(false);
		}
		line = line.trim();
		separatorIndex = line.indexOf('\t');
		if (separatorIndex == -1) {
			fr.close();
			IJ.write("Line " + k + ": #targetWidth# <tab> #targetHeight#");
			return(false);
		}
		xString = line.substring(0, separatorIndex);
		yString = line.substring(separatorIndex);
		xString = xString.trim();
		yString = yString.trim();
		if (Integer.parseInt(xString) != targetImp.getWidth()) {
			fr.close();
			IJ.write("Line " + k + ": The target width"
				+ " should not differ from that in the landmarks file");
			return(false);
		}
		if (Integer.parseInt(yString) != targetImp.getHeight()) {
			fr.close();
			IJ.write("Line " + k + ": The target height"
				+ " should not differ from that in the landmarks file");
			return(false);
		}
		++k;
		if (!(line = br.readLine()).equals("")) {
			fr.close();
			IJ.write("Line " + k + ": ''");
			return(false);
		}
		++k;
		if (!(line = br.readLine()).equals("Refined source landmarks")) {
			fr.close();
			IJ.write("Line " + k + ": 'Refined source landmarks'");
			return(false);
		}
		if (transformation == RIGID_BODY) {
			for (int i = 0; (i < transformation); i++) {
				++k;
				if ((line = br.readLine()) == null) {
					fr.close();
					IJ.write("Line " + k
						+ ": #xSourcePoint# <tab> #ySourcePoint#");
					return(false);
				}
				line = line.trim();
				separatorIndex = line.indexOf('\t');
				if (separatorIndex == -1) {
					fr.close();
					IJ.write("Line " + k
						+ ": #xSourcePoint# <tab> #ySourcePoint#");
					return(false);
				}
				xString = line.substring(0, separatorIndex);
				yString = line.substring(separatorIndex);
				xString = xString.trim();
				yString = yString.trim();
				sourcePoint[i][0] = (new Double(xString)).doubleValue();
				sourcePoint[i][1] = (new Double(yString)).doubleValue();
			}
		}
		else {
			for (int i = 0; (i < (transformation / 2)); i++) {
				++k;
				if ((line = br.readLine()) == null) {
					fr.close();
					IJ.write("Line " + k
						+ ": #xSourcePoint# <tab> #ySourcePoint#");
					return(false);
				}
				line = line.trim();
				separatorIndex = line.indexOf('\t');
				if (separatorIndex == -1) {
					fr.close();
					IJ.write("Line " + k
						+ ": #xSourcePoint# <tab> #ySourcePoint#");
					return(false);
				}
				xString = line.substring(0, separatorIndex);
				yString = line.substring(separatorIndex);
				xString = xString.trim();
				yString = yString.trim();
				sourcePoint[i][0] = (new Double(xString)).doubleValue();
				sourcePoint[i][1] = (new Double(yString)).doubleValue();
			}
		}
		++k;
		if (!(line = br.readLine()).equals("")) {
			fr.close();
			IJ.write("Line " + k + ": ''");
			return(false);
		}
		++k;
		if (!(line = br.readLine()).equals("Target landmarks")) {
			fr.close();
			IJ.write("Line " + k + ": 'Target landmarks'");
			return(false);
		}
		if (transformation == RIGID_BODY) {
			for (int i = 0; (i < transformation); i++) {
				++k;
				if ((line = br.readLine()) == null) {
					fr.close();
					IJ.write("Line " + k
						+ ": #xTargetPoint# <tab> #yTargetPoint#");
					return(false);
				}
				line = line.trim();
				separatorIndex = line.indexOf('\t');
				if (separatorIndex == -1) {
					fr.close();
					IJ.write("Line " + k
						+ ": #xTargetPoint# <tab> #yTargetPoint#");
					return(false);
				}
				xString = line.substring(0, separatorIndex);
				yString = line.substring(separatorIndex);
				xString = xString.trim();
				yString = yString.trim();
				targetPoint[i][0] = (new Double(xString)).doubleValue();
				targetPoint[i][1] = (new Double(yString)).doubleValue();
			}
		}
		else {
			for (int i = 0; (i < (transformation / 2)); i++) {
				++k;
				if ((line = br.readLine()) == null) {
					fr.close();
					IJ.write("Line " + k
						+ ": #xTargetPoint# <tab> #yTargetPoint#");
					return(false);
				}
				line = line.trim();
				separatorIndex = line.indexOf('\t');
				if (separatorIndex == -1) {
					fr.close();
					IJ.write("Line " + k
						+ ": #xTargetPoint# <tab> #yTargetPoint#");
					return(false);
				}
				xString = line.substring(0, separatorIndex);
				yString = line.substring(separatorIndex);
				xString = xString.trim();
				yString = yString.trim();
				targetPoint[i][0] = (new Double(xString)).doubleValue();
				targetPoint[i][1] = (new Double(yString)).doubleValue();
			}
		}
		fr.close();
	} catch (FileNotFoundException e) {
		IJ.log(
			"File not found exception " + e.getMessage());
		return(false);
	} catch (IOException e) {
		IJ.log(
			"IOException exception " + e.getMessage());
		return(false);
	} catch (NumberFormatException e) {
		IJ.log(
			"Number format exception " + e.getMessage());
		return(false);
	}
	if (transformation != this.transformation) {
		if ((transformation == BILINEAR) || (this.transformation == BILINEAR)) {
			cancelImages();
			createImages();
			startThreads();
		}
		this.transformation = transformation;
		setTransformation();
	}
	sourcePh.setPoints(sourcePoint);
	targetPh.setPoints(targetPoint);
	switch (transformation) {
		case TRANSLATION: {
			transformationGroup.setSelectedCheckbox(translation);
			break;
		}
		case RIGID_BODY: {
			transformationGroup.setSelectedCheckbox(rigidBody);
			break;
		}
		case AFFINE: {
			transformationGroup.setSelectedCheckbox(affine);
			break;
		}
		case SCALED_ROTATION: {
			transformationGroup.setSelectedCheckbox(scaledRotation);
			break;
		}
		case BILINEAR: {
			transformationGroup.setSelectedCheckbox(bilinear);
			break;
		}
	}
	sourceImp.updateAndDraw();
	targetImp.updateAndDraw();
	return(true);
} /* end loadLandmarks */

/*------------------------------------------------------------------*/
private void permuteImages (
) {
	final int rescuedSourceColorPlane = sourceColorPlane;
	final int rescuedTargetColorPlane = targetColorPlane;
	final double[][] rescuedSourcePoints = cancelSource(sourcePh);
	final double[][] rescuedTargetPoints = cancelTarget(targetPh);
	sourceColorPlane = rescuedTargetColorPlane;
	targetColorPlane = rescuedSourceColorPlane;
	createSourceImages();
	sourcePh.setPoints(rescuedTargetPoints);
	createTargetImages();
	targetPh.setPoints(rescuedSourcePoints);
	sourceImp.updateAndDraw();
	targetImp.updateAndDraw();
} /* end permuteImages */

/*------------------------------------------------------------------*/
private void setSourceColorPlane (
	final int colorPlane
) {
	sourceColorPlane = colorPlane;
	switch (sourceColorPlane) {
		case RED: {
			sourceKrgbGroup.setSelectedCheckbox(sourceRed);
			break;
		}
		case GREEN: {
			sourceKrgbGroup.setSelectedCheckbox(sourceGreen);
			break;
		}
		case BLUE: {
			sourceKrgbGroup.setSelectedCheckbox(sourceBlue);
			break;
		}
	}
	sourceImp.setSlice(sourceColorPlane);
	sourceImg = new turboRegImage(sourceImp, transformation, false);
	sourceMsk = new turboRegMask(sourceImp);
	sourceMsk.clearMask();
} /* end setSourceColorPlane */

/*------------------------------------------------------------------*/
private void setTargetColorPlane (
	final int colorPlane
) {
	targetColorPlane = colorPlane;
	switch (targetColorPlane) {
		case RED: {
			targetKrgbGroup.setSelectedCheckbox(targetRed);
			break;
		}
		case GREEN: {
			targetKrgbGroup.setSelectedCheckbox(targetGreen);
			break;
		}
		case BLUE: {
			targetKrgbGroup.setSelectedCheckbox(targetBlue);
			break;
		}
	}
	targetImp.setSlice(targetColorPlane);
	targetImg = new turboRegImage(targetImp, transformation, true);
	targetMsk = new turboRegMask(targetImp);
	targetMsk.clearMask();
} /* end setTargetColorPlane */

/*------------------------------------------------------------------*/
private void setTransformation (
) {
	sourceImg.setTransformation(transformation);
	sourcePh.setTransformation(transformation);
	targetImg.setTransformation(transformation);
	targetPh.setTransformation(transformation);
} /* end setTransformation */

/*------------------------------------------------------------------*/
private void startSourceThreads (
) {
	getPyramidDepth();
	sourceImg.setPyramidDepth(pyramidDepth);
	sourceMsk.setPyramidDepth(pyramidDepth);
	if (pyramidDepth != targetImg.getPyramidDepth()) {
		ImagePlus imp = targetImp;
		double[][] points = cancelTarget(targetPh);
		targetImp = imp;
		createTargetImages();
		targetPh.setPoints(points);
		targetImp.updateAndDraw();
		targetImg.setPyramidDepth(pyramidDepth);
		targetMsk.setPyramidDepth(pyramidDepth);
		startTargetThreads();
	}
	sourceImg.getThread().start();
	sourceMsk.getThread().start();
} /* end startSourceThreads */

/*------------------------------------------------------------------*/
private void startTargetThreads (
) {
	getPyramidDepth();
	targetImg.setPyramidDepth(pyramidDepth);
	targetMsk.setPyramidDepth(pyramidDepth);
	if (pyramidDepth != sourceImg.getPyramidDepth()) {
		ImagePlus imp = sourceImp;
		double points[][] = cancelSource(sourcePh);
		sourceImp = imp;
		createSourceImages();
		sourcePh.setPoints(points);
		sourceImp.updateAndDraw();
		sourceImg.setPyramidDepth(pyramidDepth);
		sourceMsk.setPyramidDepth(pyramidDepth);
		startSourceThreads();
	}
	targetImg.getThread().start();
	targetMsk.getThread().start();
} /* end startTargetThreads */

/*------------------------------------------------------------------*/
private void startThreads (
) {
	getPyramidDepth();
	sourceImg.setPyramidDepth(pyramidDepth);
	sourceMsk.setPyramidDepth(pyramidDepth);
	sourceImg.getThread().start();
	sourceMsk.getThread().start();
	targetImg.setPyramidDepth(pyramidDepth);
	targetMsk.setPyramidDepth(pyramidDepth);
	targetImg.getThread().start();
	targetMsk.getThread().start();
} /* end startThreads */

/*------------------------------------------------------------------*/
private void stopSourceThreads (
) {
	while (sourceImg.getThread().isAlive()) {
		sourceImg.getThread().interrupt();
	}
	sourceImg.getThread().interrupted();
	while (sourceMsk.getThread().isAlive()) {
		sourceMsk.getThread().interrupt();
	}
	sourceMsk.getThread().interrupted();
} /* end stopSourceThreads */

/*------------------------------------------------------------------*/
private void stopTargetThreads (
) {
	while (targetImg.getThread().isAlive()) {
		targetImg.getThread().interrupt();
	}
	targetImg.getThread().interrupted();
	while (targetMsk.getThread().isAlive()) {
		targetMsk.getThread().interrupt();
	}
	targetMsk.getThread().interrupted();
} /* end stopTargetThreads */

} /* end class turboRegDialog */

/*====================================================================
|	turboRegFinalAction
\===================================================================*/

/*********************************************************************
 The purpose of this class is to allow access to the progress bar,
 since it is denied to the <code>turboRegDialog</code> class.
 It proceeds by wrapping <code>turboRegDialog</code> inside a thread
 that is under the main event loop control.
 ********************************************************************/
class turboRegFinalAction
	implements
		Runnable

{ /* begin class turboRegFinalAction */

/*....................................................................
	Public variables
....................................................................*/

/*********************************************************************
 Automatic registration: the initial source landmarks are
 refined to minimize the mean-square error.
 ********************************************************************/
public static final int AUTOMATIC = 1;

/*********************************************************************
 Manual registration: the initial source landmarks are used
 <i>as is</i> to produce the output image.
 ********************************************************************/
public static final int MANUAL = 2;

/*********************************************************************
 Batch registration: each slice of the source image is registered
 to the target image.
 ********************************************************************/
public static final int BATCH = 3;

/*....................................................................
	Private variables
....................................................................*/
private Thread t;
private turboRegDialog td;
private volatile ImagePlus sourceImp;
private volatile ImagePlus targetImp;
private volatile turboRegImage sourceImg;
private volatile turboRegImage targetImg;
private volatile turboRegMask sourceMsk;
private volatile turboRegMask targetMsk;
private volatile turboRegPointHandler sourcePh;
private volatile turboRegPointHandler targetPh;
private volatile int operation;
private volatile int pyramidDepth;
private volatile int sourceColorPlane;
private volatile int transformation;
private volatile boolean accelerated;
private volatile boolean saveOnExit;
private volatile boolean colorOutput;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 Return the thread associated with this <code>turboRegFinalAction</code>
 object.
 ********************************************************************/
public Thread getThread (
) {
	return(t);
} /* end getThread */

/*********************************************************************
 Start under the control of the main event loop, pause as long as
 the dialog event loop is active, and resume processing when
 <code>turboRegDialog</code> finally dies.
 ********************************************************************/
public void run (
) {
	double[][] sourcePoints = null;
	double[][] targetPoints = null;
	turboRegTransform tt = null;
	ImageStack outputStack = null;
	ImagePlus outputImp = null;
	switch (operation) {
		case AUTOMATIC:
		case MANUAL: {
			if (td != null) {
				if ((operation == MANUAL) && accelerated) {
					td.stopThreads();
				}
				else {
					td.joinThreads();
				}
			}
			tt = new turboRegTransform(sourceImg, sourceMsk, sourcePh,
				targetImg, targetMsk, targetPh, transformation, accelerated,
				(td != null));
			if (operation == AUTOMATIC) {
				tt.doRegistration();
			}
			if (colorOutput) {
				outputStack = new ImageStack(
					targetImg.getWidth(), targetImg.getHeight());
				FloatProcessor fp;
				fp = new FloatProcessor(
					targetImg.getWidth(), targetImg.getHeight());
				fp.setMinAndMax(0.0, 255.0);
				outputStack.addSlice("Red", fp);
				fp = new FloatProcessor(
					targetImg.getWidth(), targetImg.getHeight());
				fp.setMinAndMax(0.0, 255.0);
				outputStack.addSlice("Green", fp);
				fp = new FloatProcessor(
					targetImg.getWidth(), targetImg.getHeight());
				fp.setMinAndMax(0.0, 255.0);
				outputStack.addSlice("Blue", fp);
				outputImp = new ImagePlus("Registered", outputStack);
				outputStack.setPixels(tt.doFinalTransform(sourceImg, sourcePh,
					targetImg, targetPh, transformation, accelerated),
					sourceColorPlane);
				for (int c = turboRegDialog.RED; (c <= turboRegDialog.BLUE);
					c++) {
					if (c == sourceColorPlane) {
						continue;
					}
					sourceImp.setSlice(c);
					sourceImg = new turboRegImage(sourceImp,
						turboRegDialog.GENERIC_TRANSFORMATION, false);
					sourceImg.setPyramidDepth(1);
					sourceImg.getThread().start();
					try {
						sourceImg.getThread().join();
					} catch (InterruptedException e) {
						IJ.log(
							"Unexpected interruption exception "
							+ e.getMessage());
					}
					sourceImg.setTransformation(transformation);
					outputStack.setPixels(tt.doFinalTransform(
						sourceImg, sourcePh, targetImg, targetPh,
						transformation, accelerated), c);
				}
				final StackConverter scnv = new StackConverter(outputImp);
				scnv.convertToGray8();
				final ImageConverter icnv = new ImageConverter(outputImp);
				icnv.setDoScaling(false);
				icnv.convertRGBStackToRGB();
				if (td != null) {
					outputImp.show();
				}
			}
			else {
				outputImp = tt.doFinalTransform(
					targetImg.getWidth(), targetImg.getHeight());
			}
			if (saveOnExit) {
				tt.saveTransformation(null);
			}
			if (td != null) {
				td.restoreAll();
			}
			break;
		}
		case BATCH: {
			outputStack = new ImageStack(targetImg.getWidth(),
				targetImg.getHeight());
			for (int i = 0; (i < sourceImp.getStackSize()); i++) {
				outputStack.addSlice("", new FloatProcessor(
					targetImg.getWidth(), targetImg.getHeight()));
			}
			outputImp = new ImagePlus("Registered", outputStack);
			if (transformation == turboRegDialog.RIGID_BODY) {
				targetPoints = new double[transformation][2];
				sourcePoints = new double[transformation][2];
				for (int k = 0; (k < transformation); k++) {
					sourcePoints[k][0] = sourcePh.getPoints()[k][0];
					sourcePoints[k][1] = sourcePh.getPoints()[k][1];
					targetPoints[k][0] = targetPh.getPoints()[k][0];
					targetPoints[k][1] = targetPh.getPoints()[k][1];
				}
			}
			else {
				targetPoints = new double[transformation / 2][2];
				sourcePoints = new double[transformation / 2][2];
				for (int k = 0; (k < (transformation / 2)); k++) {
					sourcePoints[k][0] = sourcePh.getPoints()[k][0];
					sourcePoints[k][1] = sourcePh.getPoints()[k][1];
					targetPoints[k][0] = targetPh.getPoints()[k][0];
					targetPoints[k][1] = targetPh.getPoints()[k][1];
				}
			}
			if (td != null) {
				td.joinThreads();
			}
			tt = new turboRegTransform(sourceImg, null, sourcePh,
				targetImg, targetMsk, targetPh, transformation, accelerated,
				(td != null));
			if (2 <= sourceImp.getStackSize()) {
				sourceImp.setSlice(2);
				sourceImg = new turboRegImage(sourceImp, transformation, false);
				sourceImg.setPyramidDepth(pyramidDepth);
				sourceImg.getThread().start();
			}
			tt.doRegistration();
			String pathAndFilename = "";
			if (saveOnExit) {
				pathAndFilename = tt.saveTransformation(null);
			}
			tt.doBatchFinalTransform(
				(float[])outputStack.getProcessor(1).getPixels());
			outputImp.setSlice(1);
			outputImp.getProcessor().resetMinAndMax();
			if (td != null) {
				outputImp.show();
			}
			Runtime.getRuntime().gc();
			for (int i = 2; (i <= sourceImp.getStackSize()); i++) {
				targetPh.setPoints(targetPoints);
				sourcePh = new turboRegPointHandler(sourceImp, transformation);
				sourcePh.setPoints(sourcePoints);
				try {
					sourceImg.getThread().join();
				} catch (InterruptedException e) {
					IJ.log(
						"Unexpected interruption exception " + e.getMessage());
				}
				tt = new turboRegTransform(sourceImg, null, sourcePh,
					targetImg, targetMsk, targetPh, transformation, accelerated,
					(td != null));
				if (i < sourceImp.getStackSize()) {
					sourceImp.setSlice(i + 1);
					sourceImg = new turboRegImage(sourceImp, transformation,
						false);
					sourceImg.setPyramidDepth(pyramidDepth);
					sourceImg.getThread().start();
				}
				tt.doRegistration();
				if (saveOnExit) {
					tt.appendTransformation(pathAndFilename);
				}
				tt.doBatchFinalTransform(
					(float[])outputStack.getProcessor(i).getPixels());
				outputImp.setSlice(i);
				outputImp.getProcessor().resetMinAndMax();
				Runtime.getRuntime().gc();
			}
			sourceImp.killRoi();
			targetImp.killRoi();
			outputImp.setSlice(1);
			outputImp.getProcessor().resetMinAndMax();
			if (td != null) {
				td.restoreAll();
			}
			break;
		}
	}
} /* end run */

/*********************************************************************
 Pass parameter from <code>turboRegDialog</code> to
 <code>turboRegFinalAction</code>.
 ********************************************************************/
public void setup (
	final turboRegImage sourceImg,
	final turboRegMask sourceMsk,
	final turboRegPointHandler sourcePh,
	final turboRegImage targetImg,
	final turboRegMask targetMsk,
	final turboRegPointHandler targetPh,
	final int transformation,
	final boolean accelerated,
	final boolean saveOnExit,
	final int operation
) {
	this.sourceImg = sourceImg;
	this.sourceMsk = sourceMsk;
	this.sourcePh = sourcePh;
	this.targetImg = targetImg;
	this.targetMsk = targetMsk;
	this.targetPh = targetPh;
	this.transformation = transformation;
	this.accelerated = accelerated;
	this.saveOnExit = saveOnExit;
	this.operation = operation;
	colorOutput = false;
} /* end setup */

/*********************************************************************
 Pass parameter from <code>turboRegDialog</code> to
 <code>turboRegFinalAction</code>.
 ********************************************************************/
public void setup (
	final ImagePlus sourceImp,
	final turboRegImage sourceImg,
	final turboRegMask sourceMsk,
	final turboRegPointHandler sourcePh,
	final int sourceColorPlane,
	final turboRegImage targetImg,
	final turboRegMask targetMsk,
	final turboRegPointHandler targetPh,
	final int transformation,
	final boolean accelerated,
	final boolean saveOnExit,
	final int operation
) {
	this.sourceImp = sourceImp;
	this.sourceImg = sourceImg;
	this.sourceMsk = sourceMsk;
	this.sourcePh = sourcePh;
	this.sourceColorPlane = sourceColorPlane;
	this.targetImg = targetImg;
	this.targetMsk = targetMsk;
	this.targetPh = targetPh;
	this.transformation = transformation;
	this.accelerated = accelerated;
	this.saveOnExit = saveOnExit;
	this.operation = operation;
	colorOutput = true;
} /* end setup */

/*********************************************************************
 Pass parameter from <code>turboRegDialog</code> to
 <code>turboRegFinalAction</code>.
 ********************************************************************/
public void setup (
	final ImagePlus sourceImp,
	final turboRegImage sourceImg,
	final turboRegPointHandler sourcePh,
	final ImagePlus targetImp,
	final turboRegImage targetImg,
	final turboRegMask targetMsk,
	final turboRegPointHandler targetPh,
	final int transformation,
	final boolean accelerated,
	final boolean saveOnExit,
	final int pyramidDepth
) {
	this.sourceImp = sourceImp;
	this.sourceImg = sourceImg;
	this.sourcePh = sourcePh;
	this.targetImp = targetImp;
	this.targetImg = targetImg;
	this.targetMsk = targetMsk;
	this.targetPh = targetPh;
	this.transformation = transformation;
	this.accelerated = accelerated;
	this.saveOnExit = saveOnExit;
	this.pyramidDepth = pyramidDepth;
	operation = BATCH;
} /* end setup */

/*********************************************************************
 Start a thread under the control of the main event loop. This thread
 has access to the progress bar, while methods called directly from
 within <code>turboRegDialog</code> do not because they are under the
 control of its own event loop.
 @param dialog Gives access to some utility methods within
 <code>turboRegDialog</code>.
 ********************************************************************/
public turboRegFinalAction (
	final turboRegDialog dialog
) {
	td = dialog;
	t = new Thread(this);
} /* end turboRegFinalAction */

/*********************************************************************
 Start a thread under the control of the main event loop.
 ********************************************************************/
public turboRegFinalAction (
	final turboRegImage sourceImg,
	final turboRegMask sourceMsk,
	final turboRegPointHandler sourcePh,
	final turboRegImage targetImg,
	final turboRegMask targetMsk,
	final turboRegPointHandler targetPh,
	final int transformation
) {
	this.sourceImg = sourceImg;
	this.sourceMsk = sourceMsk;
	this.sourcePh = sourcePh;
	this.targetImg = targetImg;
	this.targetMsk = targetMsk;
	this.targetPh = targetPh;
	this.transformation = transformation;
	accelerated = false;
	saveOnExit = false;
	operation = AUTOMATIC;
	colorOutput = false;
	td = null;
	t = new Thread(this);
} /* end turboRegFinalAction */

} /* end class turboRegFinalAction */

/*====================================================================
|	turboRegImage
\===================================================================*/

/*********************************************************************
 This class is responsible for the image preprocessing that takes
 place concurrently with user-interface events. It contains methods
 to compute B-spline coefficients and their pyramids, image pyramids,
 gradients, and gradient pyramids.
 ********************************************************************/
class turboRegImage
	implements
		Runnable

{ /* begin class turboRegImage */

/*....................................................................
	Private variables
....................................................................*/
private final Stack<Object> pyramid = new Stack<Object>();
private Thread t;
private float[] image;
private float[] coefficient;
private float[] xGradient;
private float[] yGradient;
private int width;
private int height;
private int pyramidDepth;
private int transformation;
private boolean isTarget;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 Return the B-spline coefficients of the full-size image.
 ********************************************************************/
public float[] getCoefficient (
) {
	return(coefficient);
} /* end getCoefficient */

/*********************************************************************
 Return the full-size image height.
 ********************************************************************/
public int getHeight (
) {
	return(height);
} /* end getHeight */

/*********************************************************************
 Return the full-size image array.
 ********************************************************************/
public float[] getImage (
) {
	return(image);
} /* end getImage */

/*********************************************************************
 Return the image pyramid as a <code>Stack</code> object. The organization
 of the stack depends on whether the <code>turboRegImage</code>
 object corresponds to the target or the source image, and on the
 transformation (ML* = {<code>TRANSLATION</code>,<code>RIGID_BODY</code>,
 <code>SCALED_ROTATION</code>, <code>AFFINE</code>} vs.
 ML = {<code>BILINEAR<code>}). A single pyramid level consists of
 <p>
 <table border="1">
 <tr><th><code>isTarget</code></th>
 <th>ML*</th>
 <th>ML</th></tr>
 <tr><td>true</td>
 <td>width<br>height<br>B-spline coefficients</td>
 <td>width<br>height<br>samples</td></tr>
 <tr><td>false</td>
 <td>width<br>height<br>samples<br>horizontal gradients<br>
 vertical gradients</td>
 <td>width<br>height<br>B-spline coefficients</td></tr>
 </table>
 ********************************************************************/
public Stack<Object> getPyramid (
) {
	return(pyramid);
} /* end getPyramid */

/*********************************************************************
 Return the depth of the image pyramid. A depth <code>1</code> means
 that one coarse resolution level is present in the stack. The
 full-size level is not placed on the stack.
 ********************************************************************/
public int getPyramidDepth (
) {
	return(pyramidDepth);
} /* end getPyramidDepth */

/*********************************************************************
 Return the thread associated with this <code>turboRegImage</code>
 object.
 ********************************************************************/
public Thread getThread (
) {
	return(t);
} /* end getThread */

/*********************************************************************
 Return the full-size image width.
 ********************************************************************/
public int getWidth (
) {
	return(width);
} /* end getWidth */

/*********************************************************************
 Return the full-size horizontal gradient of the image, if available.
 @see turboRegImage#getPyramid()
 ********************************************************************/
public float[] getXGradient (
) {
	return(xGradient);
} /* end getXGradient */

/*********************************************************************
 Return the full-size vertical gradient of the image, if available.
 @see turboRegImage#getImage()
 ********************************************************************/
public float[] getYGradient (
) {
	return(yGradient);
} /* end getYGradient */

/*********************************************************************
 Start the image precomputations. The computation of the B-spline
 coefficients of the full-size image is not interruptible; all other
 methods are.
 ********************************************************************/
public void run (
) {
	coefficient = getBasicFromCardinal2D();
	switch (transformation) {
		case turboRegDialog.GENERIC_TRANSFORMATION: {
			break;
		}
		case turboRegDialog.TRANSLATION:
		case turboRegDialog.RIGID_BODY:
		case turboRegDialog.SCALED_ROTATION:
		case turboRegDialog.AFFINE: {
			if (isTarget) {
				buildCoefficientPyramid();
			}
			else {
				imageToXYGradient2D();
				buildImageAndGradientPyramid();
			}
			break;
		}
		case turboRegDialog.BILINEAR: {
			if (isTarget) {
				buildImagePyramid();
			}
			else {
				buildCoefficientPyramid();
			}
			break;
		}
	}
} /* end run */

/*********************************************************************
 Sets the depth up to which the pyramids should be computed.
 @see turboRegImage#getImage()
 ********************************************************************/
public void setPyramidDepth (
	final int pyramidDepth
) {
	this.pyramidDepth = pyramidDepth;
} /* end setPyramidDepth */

/*********************************************************************
 Set or modify the transformation.
 ********************************************************************/
public void setTransformation (
	final int transformation
) {
	this.transformation = transformation;
} /* end setTransformation */

/*********************************************************************
 Converts the pixel array of the incoming <code>ImagePlus</code>
 object into a local <code>float</code> array.
 @param imp <code>ImagePlus</code> object to preprocess.
 @param transformation Transformation code.
 @param isTarget Tags the current object as a target or source image.
 ********************************************************************/
public turboRegImage (
	final ImagePlus imp,
	final int transformation,
	final boolean isTarget
) {
	t = new Thread(this);
	t.setDaemon(true);
	this.transformation = transformation;
	this.isTarget = isTarget;
	width = imp.getWidth();
	height = imp.getHeight();
	int k = 0;
	turboRegProgressBar.addWorkload(height);
	if (imp.getType() == ImagePlus.GRAY8) {
		image = new float[width * height];
		final byte[] pixels = (byte[])imp.getProcessor().getPixels();
		for (int y = 0; (y < height); y++) {
			for (int x = 0; (x < width); x++, k++) {
				image[k] = (float)(pixels[k] & 0xFF);
			}
			turboRegProgressBar.stepProgressBar();
		}
	}
	else if (imp.getType() == ImagePlus.GRAY16) {
		image = new float[width * height];
		final short[] pixels = (short[])imp.getProcessor().getPixels();
		for (int y = 0; (y < height); y++) {
			for (int x = 0; (x < width); x++, k++) {
				if (pixels[k] < (short)0) {
					image[k] = (float)pixels[k] + 65536.0F;
				}
				else {
					image[k] = (float)pixels[k];
				}
			}
			turboRegProgressBar.stepProgressBar();
		}
	}
	else if (imp.getType() == ImagePlus.GRAY32) {
		image = (float[])imp.getProcessor().getPixels();
	}
	turboRegProgressBar.workloadDone(height);
} /* end turboRegImage */

/*....................................................................
	Private methods
....................................................................*/

/*------------------------------------------------------------------*/
private void antiSymmetricFirMirrorOffBounds1D (
	final double[] h,
	final double[] c,
	final double[] s
) {
	if (2 <= c.length) {
		s[0] = h[1] * (c[1] - c[0]);
		for (int i = 1; (i < (s.length - 1)); i++) {
			s[i] = h[1] * (c[i + 1] - c[i - 1]);
		}
		s[s.length - 1] = h[1] * (c[c.length - 1] - c[c.length - 2]);
	}
	else {
		s[0] = 0.0;
	}
} /* end antiSymmetricFirMirrorOffBounds1D */

/*------------------------------------------------------------------*/
private void basicToCardinal2D (
	final float[] basic,
	final float[] cardinal,
	final int width,
	final int height,
	final int degree
) {
	final double[] hLine = new double[width];
	final double[] vLine = new double[height];
	final double[] hData = new double[width];
	final double[] vData = new double[height];
	double[] h = null;
	switch (degree) {
		case 3: {
			h = new double[2];
			h[0] = 2.0 / 3.0;
			h[1] = 1.0 / 6.0;
			break;
		}
		case 7: {
			h = new double[4];
			h[0] = 151.0 / 315.0;
			h[1] = 397.0 / 1680.0;
			h[2] = 1.0 / 42.0;
			h[3] = 1.0 / 5040.0;
			break;
		}
		default: {
			h = new double[1];
			h[0] = 1.0;
		}
	}
	int workload = width + height;
	turboRegProgressBar.addWorkload(workload);
	for (int y = 0; ((y < height) && (!t.isInterrupted())); y++) {
		extractRow(basic, y, hLine);
		symmetricFirMirrorOffBounds1D(h, hLine, hData);
		putRow(cardinal, y, hData);
		turboRegProgressBar.stepProgressBar();
		workload--;
	}
	for (int x = 0; ((x < width) && (!t.isInterrupted())); x++) {
		extractColumn(cardinal, width, x, vLine);
		symmetricFirMirrorOffBounds1D(h, vLine, vData);
		putColumn(cardinal, width, x, vData);
		turboRegProgressBar.stepProgressBar();
		workload--;
	}
	turboRegProgressBar.skipProgressBar(workload);
	turboRegProgressBar.workloadDone(width + height);
} /* end basicToCardinal2D */

/*------------------------------------------------------------------*/
private void buildCoefficientPyramid (
) {
	int fullWidth;
	int fullHeight;
	float[] fullDual = new float[width * height];
	int halfWidth = width;
	int halfHeight = height;
	if (1 < pyramidDepth) {
		basicToCardinal2D(coefficient, fullDual, width, height, 7);
	}
	for (int depth = 1; ((depth < pyramidDepth) && (!t.isInterrupted()));
		depth++) {
		fullWidth = halfWidth;
		fullHeight = halfHeight;
		halfWidth /= 2;
		halfHeight /= 2;
		final float[] halfDual = getHalfDual2D(fullDual, fullWidth, fullHeight);
		final float[] halfCoefficient = getBasicFromCardinal2D(
			halfDual, halfWidth, halfHeight, 7);
		pyramid.push(halfCoefficient);
		pyramid.push(new Integer(halfHeight));
		pyramid.push(new Integer(halfWidth));
		fullDual = halfDual;
	}
} /* end buildCoefficientPyramid */

/*------------------------------------------------------------------*/
private void buildImageAndGradientPyramid (
) {
	int fullWidth;
	int fullHeight;
	float[] fullDual = new float[width * height];
	int halfWidth = width;
	int halfHeight = height;
	if (1 < pyramidDepth) {
		cardinalToDual2D(image, fullDual, width, height, 3);
	}
	for (int depth = 1; ((depth < pyramidDepth) && (!t.isInterrupted()));
		depth++) {
		fullWidth = halfWidth;
		fullHeight = halfHeight;
		halfWidth /= 2;
		halfHeight /= 2;
		final float[] halfDual = getHalfDual2D(fullDual, fullWidth, fullHeight);
		final float[] halfImage = getBasicFromCardinal2D(
			halfDual, halfWidth, halfHeight, 7);
		final float[] halfXGradient = new float[halfWidth * halfHeight];
		final float[] halfYGradient = new float[halfWidth * halfHeight];
		coefficientToXYGradient2D(halfImage, halfXGradient, halfYGradient,
			halfWidth, halfHeight);
		basicToCardinal2D(halfImage, halfImage, halfWidth, halfHeight, 3);
		pyramid.push(halfYGradient);
		pyramid.push(halfXGradient);
		pyramid.push(halfImage);
		pyramid.push(new Integer(halfHeight));
		pyramid.push(new Integer(halfWidth));
		fullDual = halfDual;
	}
} /* end buildImageAndGradientPyramid */

/*------------------------------------------------------------------*/
private void buildImagePyramid (
) {
	int fullWidth;
	int fullHeight;
	float[] fullDual = new float[width * height];
	int halfWidth = width;
	int halfHeight = height;
	if (1 < pyramidDepth) {
		cardinalToDual2D(image, fullDual, width, height, 3);
	}
	for (int depth = 1; ((depth < pyramidDepth) && (!t.isInterrupted()));
		depth++) {
		fullWidth = halfWidth;
		fullHeight = halfHeight;
		halfWidth /= 2;
		halfHeight /= 2;
		final float[] halfDual = getHalfDual2D(fullDual, fullWidth, fullHeight);
		final float[] halfImage = new float[halfWidth * halfHeight];
		dualToCardinal2D(halfDual, halfImage, halfWidth, halfHeight, 3);
		pyramid.push(halfImage);
		pyramid.push(new Integer(halfHeight));
		pyramid.push(new Integer(halfWidth));
		fullDual = halfDual;
	}
} /* end buildImagePyramid */

/*------------------------------------------------------------------*/
private void cardinalToDual2D (
	final float[] cardinal,
	final float[] dual,
	final int width,
	final int height,
	final int degree
) {
	basicToCardinal2D(getBasicFromCardinal2D(cardinal, width, height, degree),
		dual, width, height, 2 * degree + 1);
} /* end cardinalToDual2D */

/*------------------------------------------------------------------*/
private void coefficientToGradient1D (
	final double[] c
) {
	final double[] h = {0.0, 1.0 / 2.0};
	final double[] s = new double[c.length];
	antiSymmetricFirMirrorOffBounds1D(h, c, s);
	System.arraycopy(s, 0, c, 0, s.length);
} /* end coefficientToGradient1D */

/*------------------------------------------------------------------*/
private void coefficientToSamples1D (
	final double[] c
) {
	final double[] h = {2.0 / 3.0, 1.0 / 6.0};
	final double[] s = new double[c.length];
	symmetricFirMirrorOffBounds1D(h, c, s);
	System.arraycopy(s, 0, c, 0, s.length);
} /* end coefficientToSamples1D */

/*------------------------------------------------------------------*/
private void coefficientToXYGradient2D (
	final float[] basic,
	final float[] xGradient,
	final float[] yGradient,
	final int width,
	final int height
) {
	final double[] hLine = new double[width];
	final double[] hData = new double[width];
	final double[] vLine = new double[height];
	int workload = 2 * (width + height);
	turboRegProgressBar.addWorkload(workload);
	for (int y = 0; ((y < height) && (!t.isInterrupted())); y++) {
		extractRow(basic, y, hLine);
		System.arraycopy(hLine, 0, hData, 0, width);
		coefficientToGradient1D(hLine);
		turboRegProgressBar.stepProgressBar();
		workload--;
		coefficientToSamples1D(hData);
		putRow(xGradient, y, hLine);
		putRow(yGradient, y, hData);
		turboRegProgressBar.stepProgressBar();
		workload--;
	}
	for (int x = 0; ((x < width) && (!t.isInterrupted())); x++) {
		extractColumn(xGradient, width, x, vLine);
		coefficientToSamples1D(vLine);
		putColumn(xGradient, width, x, vLine);
		turboRegProgressBar.stepProgressBar();
		workload--;
		extractColumn(yGradient, width, x, vLine);
		coefficientToGradient1D(vLine);
		putColumn(yGradient, width, x, vLine);
		turboRegProgressBar.stepProgressBar();
		workload--;
	}
	turboRegProgressBar.skipProgressBar(workload);
	turboRegProgressBar.workloadDone(2 * (width + height));
} /* end coefficientToXYGradient2D */

/*------------------------------------------------------------------*/
private void dualToCardinal2D (
	final float[] dual,
	final float[] cardinal,
	final int width,
	final int height,
	final int degree
) {
	basicToCardinal2D(getBasicFromCardinal2D(dual, width, height,
		2 * degree + 1), cardinal, width, height, degree);
} /* end dualToCardinal2D */

/*------------------------------------------------------------------*/
private void extractColumn (
	final float[] array,
	final int width,
	int x,
	final double[] column
) {
	for (int i = 0; (i < column.length); i++) {
		column[i] = (double)array[x];
		x += width;
	}
} /* end extractColumn */

/*------------------------------------------------------------------*/
private void extractRow (
	final float[] array,
	int y,
	final double[] row
) {
	y *= row.length;
	for (int i = 0; (i < row.length); i++) {
		row[i] = (double)array[y++];
	}
} /* end extractRow */

/*------------------------------------------------------------------*/
private float[] getBasicFromCardinal2D (
) {
	final float[] basic = new float[width * height];
	final double[] hLine = new double[width];
	final double[] vLine = new double[height];
	turboRegProgressBar.addWorkload(width + height);
	for (int y = 0; (y < height); y++) {
		extractRow(image, y, hLine);
		samplesToInterpolationCoefficient1D(hLine, 3, 0.0);
		putRow(basic, y, hLine);
		turboRegProgressBar.stepProgressBar();
	}
	for (int x = 0; (x < width); x++) {
		extractColumn(basic, width, x, vLine);
		samplesToInterpolationCoefficient1D(vLine, 3, 0.0);
		putColumn(basic, width, x, vLine);
		turboRegProgressBar.stepProgressBar();
	}
	turboRegProgressBar.workloadDone(width + height);
	return(basic);
} /* end getBasicFromCardinal2D */

/*------------------------------------------------------------------*/
private float[] getBasicFromCardinal2D (
	final float[] cardinal,
	final int width,
	final int height,
	final int degree
) {
	final float[] basic = new float[width * height];
	final double[] hLine = new double[width];
	final double[] vLine = new double[height];
	int workload = width + height;
	turboRegProgressBar.addWorkload(workload);
	for (int y = 0; ((y < height) && (!t.isInterrupted())); y++) {
		extractRow(cardinal, y, hLine);
		samplesToInterpolationCoefficient1D(hLine, degree, 0.0);
		putRow(basic, y, hLine);
		turboRegProgressBar.stepProgressBar();
		workload--;
	}
	for (int x = 0; ((x < width) && (!t.isInterrupted())); x++) {
		extractColumn(basic, width, x, vLine);
		samplesToInterpolationCoefficient1D(vLine, degree, 0.0);
		putColumn(basic, width, x, vLine);
		turboRegProgressBar.stepProgressBar();
		workload--;
	}
	turboRegProgressBar.skipProgressBar(workload);
	turboRegProgressBar.workloadDone(width + height);
	return(basic);
} /* end getBasicFromCardinal2D */

/*------------------------------------------------------------------*/
private float[] getHalfDual2D (
	final float[] fullDual,
	final int fullWidth,
	final int fullHeight
) {
	final int halfWidth = fullWidth / 2;
	final int halfHeight = fullHeight / 2;
	final double[] hLine = new double[fullWidth];
	final double[] hData = new double[halfWidth];
	final double[] vLine = new double[fullHeight];
	final double[] vData = new double[halfHeight];
	final float[] demiDual = new float[halfWidth * fullHeight];
	final float[] halfDual = new float[halfWidth * halfHeight];
	int workload = halfWidth + fullHeight;
	turboRegProgressBar.addWorkload(workload);
	for (int y = 0; ((y < fullHeight) && (!t.isInterrupted())); y++) {
		extractRow(fullDual, y, hLine);
		reduceDual1D(hLine, hData);
		putRow(demiDual, y, hData);
		turboRegProgressBar.stepProgressBar();
		workload--;
	}
	for (int x = 0; ((x < halfWidth) && (!t.isInterrupted())); x++) {
		extractColumn(demiDual, halfWidth, x, vLine);
		reduceDual1D(vLine, vData);
		putColumn(halfDual, halfWidth, x, vData);
		turboRegProgressBar.stepProgressBar();
		workload--;
	}
	turboRegProgressBar.skipProgressBar(workload);
	turboRegProgressBar.workloadDone(halfWidth + fullHeight);
	return(halfDual);
} /* end getHalfDual2D */

/*------------------------------------------------------------------*/
private double getInitialAntiCausalCoefficientMirrorOffBounds (
	final double[] c,
	final double z,
	final double tolerance
) {
	return(z * c[c.length - 1] / (z - 1.0));
} /* end getInitialAntiCausalCoefficientMirrorOffBounds */

/*------------------------------------------------------------------*/
private double getInitialCausalCoefficientMirrorOffBounds (
	final double[] c,
	final double z,
	final double tolerance
) {
	double z1 = z;
	double zn = Math.pow(z, c.length);
	double sum = (1.0 + z) * (c[0] + zn * c[c.length - 1]);
	int horizon = c.length;
	if (0.0 < tolerance) {
		horizon = 2 + (int)(Math.log(tolerance) / Math.log(Math.abs(z)));
		horizon = (horizon < c.length) ? (horizon) : (c.length);
	}
	zn = zn * zn;
	for (int n = 1; (n < (horizon - 1)); n++) {
		z1 = z1 * z;
		zn = zn / z;
		sum = sum + (z1 + zn) * c[n];
	}
	return(sum / (1.0 - Math.pow(z, 2 * c.length)));
} /* end getInitialCausalCoefficientMirrorOffBounds */

/*------------------------------------------------------------------*/
private void imageToXYGradient2D (
) {
	final double[] hLine = new double[width];
	final double[] vLine = new double[height];
	xGradient = new float[width * height];
	yGradient = new float[width * height];
	int workload = width + height;
	turboRegProgressBar.addWorkload(workload);
	for (int y = 0; ((y < height) && (!t.isInterrupted())); y++) {
		extractRow(image, y, hLine);
		samplesToInterpolationCoefficient1D(hLine, 3, 0.0);
		coefficientToGradient1D(hLine);
		putRow(xGradient, y, hLine);
		turboRegProgressBar.stepProgressBar();
		workload--;
	}
	for (int x = 0; ((x < width) && (!t.isInterrupted())); x++) {
		extractColumn(image, width, x, vLine);
		samplesToInterpolationCoefficient1D(vLine, 3, 0.0);
		coefficientToGradient1D(vLine);
		putColumn(yGradient, width, x, vLine);
		turboRegProgressBar.stepProgressBar();
		workload--;
	}
	turboRegProgressBar.skipProgressBar(workload);
	turboRegProgressBar.workloadDone(width + height);
} /* end imageToXYGradient2D */

/*------------------------------------------------------------------*/
private void putColumn (
	final float[] array,
	final int width,
	int x,
	final double[] column
) {
	for (int i = 0; (i < column.length); i++) {
		array[x] = (float)column[i];
		x += width;
	}
} /* end putColumn */

/*------------------------------------------------------------------*/
private void putRow (
	final float[] array,
	int y,
	final double[] row
) {
	y *= row.length;
	for (int i = 0; (i < row.length); i++) {
		array[y++] = (float)row[i];
	}
} /* end putRow */

/*------------------------------------------------------------------*/
private void reduceDual1D (
	final double[] c,
	final double[] s
) {
	final double h[] = {6.0 / 16.0, 4.0 / 16.0, 1.0 / 16.0};
	if (2 <= s.length) {
		s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2]);
		for (int i = 2, j = 1; (j < (s.length - 1)); i += 2, j++) {
			s[j] = h[0] * c[i] + h[1] * (c[i - 1] + c[i + 1])
				+ h[2] * (c[i - 2] + c[i + 2]);
		}
		if (c.length == (2 * s.length)) {
			s[s.length - 1] = h[0] * c[c.length - 2]
				+ h[1] * (c[c.length - 3] + c[c.length - 1])
				+ h[2] * (c[c.length - 4] + c[c.length - 1]);
		}
		else {
			s[s.length - 1] = h[0] * c[c.length - 3]
				+ h[1] * (c[c.length - 4] + c[c.length - 2])
				+ h[2] * (c[c.length - 5] + c[c.length - 1]);
		}
	}
	else {
		switch (c.length) {
			case 3: {
				s[0] = h[0] * c[0]
					+ h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2]);
				break;
			}
			case 2: {
				s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + 2.0 * h[2] * c[1];
				break;
			}
		}
	}
} /* end reduceDual1D */

/*------------------------------------------------------------------*/
private void samplesToInterpolationCoefficient1D (
	final double[] c,
	final int degree,
	final double tolerance
) {
	double[] z = new double[0];
	double lambda = 1.0;
	switch (degree) {
		case 3: {
			z = new double[1];
			z[0] = Math.sqrt(3.0) - 2.0;
			break;
		}
		case 7: {
			z = new double[3];
			z[0] =
				-0.5352804307964381655424037816816460718339231523426924148812;
			z[1] =
				-0.122554615192326690515272264359357343605486549427295558490763;
			z[2] =
				-0.0091486948096082769285930216516478534156925639545994482648003;
			break;
		}
	}
	if (c.length == 1) {
		return;
	}
	for (int k = 0; (k < z.length); k++) {
		lambda *= (1.0 - z[k]) * (1.0 - 1.0 / z[k]);
	}
	for (int n = 0; (n < c.length); n++) {
		c[n] = c[n] * lambda;
	}
	for (int k = 0; (k < z.length); k++) {
		c[0] = getInitialCausalCoefficientMirrorOffBounds(c, z[k], tolerance);
		for (int n = 1; (n < c.length); n++) {
			c[n] = c[n] + z[k] * c[n - 1];
		}
		c[c.length - 1] = getInitialAntiCausalCoefficientMirrorOffBounds(
			c, z[k], tolerance);
		for (int n = c.length - 2; (0 <= n); n--) {
			c[n] = z[k] * (c[n+1] - c[n]);
		}
	}
} /* end samplesToInterpolationCoefficient1D */

/*------------------------------------------------------------------*/
private void symmetricFirMirrorOffBounds1D (
	final double[] h,
	final double[] c,
	final double[] s
) {
	switch (h.length) {
		case 2: {
			if (2 <= c.length) {
				s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]);
				for (int i = 1; (i < (s.length - 1)); i++) {
					s[i] = h[0] * c[i] + h[1] * (c[i - 1] + c[i + 1]);
				}
				s[s.length - 1] = h[0] * c[c.length - 1]
					+ h[1] * (c[c.length - 2] + c[c.length - 1]);
			}
			else {
				s[0] = (h[0] + 2.0 * h[1]) * c[0];
			}
			break;
		}
		case 4: {
			if (6 <= c.length) {
				s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2])
					+ h[3] * (c[2] + c[3]);
				s[1] = h[0] * c[1] + h[1] * (c[0] + c[2]) + h[2] * (c[0] + c[3])
					+ h[3] * (c[1] + c[4]);
				s[2] = h[0] * c[2] + h[1] * (c[1] + c[3]) + h[2] * (c[0] + c[4])
					+ h[3] * (c[0] + c[5]);
				for (int i = 3; (i < (s.length - 3)); i++) {
					s[i] = h[0] * c[i] + h[1] * (c[i - 1] + c[i + 1])
						+ h[2] * (c[i - 2] + c[i + 2])
						+ h[3] * (c[i - 3] + c[i + 3]);
				}
				s[s.length - 3] = h[0] * c[c.length - 3]
					+ h[1] * (c[c.length - 4] + c[c.length - 2])
					+ h[2] * (c[c.length - 5] + c[c.length - 1])
					+ h[3] * (c[c.length - 6] + c[c.length - 1]);
				s[s.length - 2] = h[0] * c[c.length - 2]
					+ h[1] * (c[c.length - 3] + c[c.length - 1])
					+ h[2] * (c[c.length - 4] + c[c.length - 1])
					+ h[3] * (c[c.length - 5] + c[c.length - 2]);
				s[s.length - 1] = h[0] * c[c.length - 1]
					+ h[1] * (c[c.length - 2] + c[c.length - 1])
					+ h[2] * (c[c.length - 3] + c[c.length - 2])
					+ h[3] * (c[c.length - 4] + c[c.length - 3]);
			}
			else {
				switch (c.length) {
					case 5: {
						s[0] = h[0] * c[0] + h[1] * (c[0] + c[1])
							+ h[2] * (c[1] + c[2]) + h[3] * (c[2] + c[3]);
						s[1] = h[0] * c[1] + h[1] * (c[0] + c[2])
							+ h[2] * (c[0] + c[3]) + h[3] * (c[1] + c[4]);
						s[2] = h[0] * c[2] + h[1] * (c[1] + c[3])
							+ (h[2] + h[3]) * (c[0] + c[4]);
						s[3] = h[0] * c[3] + h[1] * (c[2] + c[4])
							+ h[2] * (c[1] + c[4]) + h[3] * (c[0] + c[3]);
						s[4] = h[0] * c[4] + h[1] * (c[3] + c[4])
							+ h[2] * (c[2] + c[3]) + h[3] * (c[1] + c[2]);
						break;
					}
					case 4: {
						s[0] = h[0] * c[0] + h[1] * (c[0] + c[1])
							+ h[2] * (c[1] + c[2]) + h[3] * (c[2] + c[3]);
						s[1] = h[0] * c[1] + h[1] * (c[0] + c[2])
							+ h[2] * (c[0] + c[3]) + h[3] * (c[1] + c[3]);
						s[2] = h[0] * c[2] + h[1] * (c[1] + c[3])
							+ h[2] * (c[0] + c[3]) + h[3] * (c[0] + c[2]);
						s[3] = h[0] * c[3] + h[1] * (c[2] + c[3])
							+ h[2] * (c[1] + c[2]) + h[3] * (c[0] + c[1]);
						break;
					}
					case 3: {
						s[0] = h[0] * c[0] + h[1] * (c[0] + c[1])
							+ h[2] * (c[1] + c[2]) + 2.0 * h[3] * c[2];
						s[1] = h[0] * c[1] + (h[1] + h[2]) * (c[0] + c[2])
							+ 2.0 * h[3] * c[1];
						s[2] = h[0] * c[2] + h[1] * (c[1] + c[2])
							+ h[2] * (c[0] + c[1]) + 2.0 * h[3] * c[0];
						break;
					}
					case 2: {
						s[0] = (h[0] + h[1] + h[3]) * c[0]
							+ (h[1] + 2.0 * h[2] + h[3]) * c[1];
						s[1] = (h[0] + h[1] + h[3]) * c[1]
							+ (h[1] + 2.0 * h[2] + h[3]) * c[0];
						break;
					}
					case 1: {
						s[0] = (h[0] + 2.0 * (h[1] + h[2] + h[3])) * c[0];
						break;
					}
				}
			}
			break;
		}
	}
} /* end symmetricFirMirrorOffBounds1D */

} /* end class turboRegImage */

/*====================================================================
|	turboRegMask
\===================================================================*/

/*********************************************************************
 This class is responsible for the mask preprocessing that takes
 place concurrently with user-interface events. It contains methods
 to compute the mask pyramids.
 ********************************************************************/
class turboRegMask
	implements
		Runnable

{ /* begin class turboRegMask */

/*....................................................................
	Private variables
....................................................................*/
private final Stack<float[]> pyramid = new Stack<float[]>();
private Thread t;
private float[] mask;
private int width;
private int height;
private int pyramidDepth;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 Set to <code>true</code> every pixel of the full-size mask.
 ********************************************************************/
public void clearMask (
) {
	int k = 0;
	turboRegProgressBar.addWorkload(height);
	for (int y = 0; (y < height); y++) {
		for (int x = 0; (x < width); x++) {
			mask[k++] = 1.0F;
		}
		turboRegProgressBar.stepProgressBar();
	}
	turboRegProgressBar.workloadDone(height);
} /* end clearMask */

/*********************************************************************
 Return the full-size mask array.
 ********************************************************************/
public float[] getMask (
) {
	return(mask);
} /* end getMask */

/*********************************************************************
 Return the pyramid as a <code>Stack</code> object. A single pyramid
 level consists of
 <p>
 <table border="1">
 <tr><th><code>isTarget</code></th>
 <th>ML*</th>
 <th>ML</th></tr>
 <tr><td>true</td>
 <td>mask samples</td>
 <td>mask samples</td></tr>
 <tr><td>false</td>
 <td>mask samples</td>
 <td>mask samples</td></tr>
 </table>
 @see turboRegImage#getPyramid()
 ********************************************************************/
public Stack<float[]> getPyramid (
) {
	return(pyramid);
} /* end getPyramid */

/*********************************************************************
 Return the thread associated with this <code>turboRegMask</code>
 object.
 ********************************************************************/
public Thread getThread (
) {
	return(t);
} /* end getThread */

/*********************************************************************
 Start the mask precomputations, which are interruptible.
 ********************************************************************/
public void run (
) {
	buildPyramid();
} /* end run */

/*********************************************************************
 Set the depth up to which the pyramids should be computed.
 @see turboRegMask#getPyramid()
 ********************************************************************/
public void setPyramidDepth (
	final int pyramidDepth
) {
	this.pyramidDepth = pyramidDepth;
} /* end setPyramidDepth */

/*********************************************************************
 Converts the pixel array of the incoming <code>ImagePlus</code>
 object into a local <code>boolean</code> array.
 @param imp <code>ImagePlus</code> object to preprocess.
 ********************************************************************/
public turboRegMask (
	final ImagePlus imp
) {
	t = new Thread(this);
	t.setDaemon(true);
	width = imp.getWidth();
	height = imp.getHeight();
	int k = 0;
	turboRegProgressBar.addWorkload(height);
	mask = new float[width * height];
	if (imp.getType() == ImagePlus.GRAY8) {
		final byte[] pixels = (byte[])imp.getProcessor().getPixels();
		for (int y = 0; (y < height); y++) {
			for (int x = 0; (x < width); x++, k++) {
				mask[k] = (float)pixels[k];
			}
			turboRegProgressBar.stepProgressBar();
		}
	}
	else if (imp.getType() == ImagePlus.GRAY16) {
		final short[] pixels = (short[])imp.getProcessor().getPixels();
		for (int y = 0; (y < height); y++) {
			for (int x = 0; (x < width); x++, k++) {
				mask[k] = (float)pixels[k];
			}
			turboRegProgressBar.stepProgressBar();
		}
	}
	else if (imp.getType() == ImagePlus.GRAY32) {
		final float[] pixels = (float[])imp.getProcessor().getPixels();
		for (int y = 0; (y < height); y++) {
			for (int x = 0; (x < width); x++, k++) {
				mask[k] = pixels[k];
			}
			turboRegProgressBar.stepProgressBar();
		}
	}
	turboRegProgressBar.workloadDone(height);
} /* end turboRegMask */

/*....................................................................
	Private methods
....................................................................*/

/*------------------------------------------------------------------*/
private void buildPyramid (
) {
	int fullWidth;
	int fullHeight;
	float[] fullMask = mask;
	int halfWidth = width;
	int halfHeight = height;
	for (int depth = 1; ((depth < pyramidDepth) && (!t.isInterrupted()));
		depth++) {
		fullWidth = halfWidth;
		fullHeight = halfHeight;
		halfWidth /= 2;
		halfHeight /= 2;
		final float[] halfMask = getHalfMask2D(fullMask, fullWidth, fullHeight);
		pyramid.push(halfMask);
		fullMask = halfMask;
	}
} /* end buildPyramid */

/*------------------------------------------------------------------*/
private float[] getHalfMask2D (
	final float[] fullMask,
	final int fullWidth,
	final int fullHeight
) {
	final int halfWidth = fullWidth / 2;
	final int halfHeight = fullHeight / 2;
	final boolean oddWidth = ((2 * halfWidth) != fullWidth);
	int workload = 2 * halfHeight;
	final float[] halfMask = new float[halfWidth * halfHeight];
	int k = 0;
	for (int y = 0; ((y < halfHeight) && (!t.isInterrupted())); y++) {
		for (int x = 0; (x < halfWidth); x++) {
			halfMask[k++] = 0.0F;
		}
		turboRegProgressBar.stepProgressBar();
		workload--;
	}
	k = 0;
	int n = 0;
	for (int y = 0; ((y < (halfHeight - 1)) && (!t.isInterrupted())); y++) {
		for (int x = 0; (x < (halfWidth - 1)); x++) {
			halfMask[k] += Math.abs(fullMask[n++]);
			halfMask[k] += Math.abs(fullMask[n]);
			halfMask[++k] += Math.abs(fullMask[n++]);
		}
		halfMask[k] += Math.abs(fullMask[n++]);
		halfMask[k++] += Math.abs(fullMask[n++]);
		if (oddWidth) {
			n++;
		}
		for (int x = 0; (x < (halfWidth - 1)); x++) {
			halfMask[k - halfWidth] += Math.abs(fullMask[n]);
			halfMask[k] += Math.abs(fullMask[n++]);
			halfMask[k - halfWidth] += Math.abs(fullMask[n]);
			halfMask[k - halfWidth + 1] += Math.abs(fullMask[n]);
			halfMask[k] += Math.abs(fullMask[n]);
			halfMask[++k] += Math.abs(fullMask[n++]);
		}
		halfMask[k - halfWidth] += Math.abs(fullMask[n]);
		halfMask[k] += Math.abs(fullMask[n++]);
		halfMask[k - halfWidth] += Math.abs(fullMask[n]);
		halfMask[k++] += Math.abs(fullMask[n++]);
		if (oddWidth) {
			n++;
		}
		k -= halfWidth;
		turboRegProgressBar.stepProgressBar();
		workload--;
	}
	for (int x = 0; (x < (halfWidth - 1)); x++) {
		halfMask[k] += Math.abs(fullMask[n++]);
		halfMask[k] += Math.abs(fullMask[n]);
		halfMask[++k] += Math.abs(fullMask[n++]);
	}
	halfMask[k] += Math.abs(fullMask[n++]);
	halfMask[k++] += Math.abs(fullMask[n++]);
	if (oddWidth) {
		n++;
	}
	k -= halfWidth;
	for (int x = 0; (x < (halfWidth - 1)); x++) {
		halfMask[k] += Math.abs(fullMask[n++]);
		halfMask[k] += Math.abs(fullMask[n]);
		halfMask[++k] += Math.abs(fullMask[n++]);
	}
	halfMask[k] += Math.abs(fullMask[n++]);
	halfMask[k] += Math.abs(fullMask[n]);
	turboRegProgressBar.stepProgressBar();
	workload--;
	turboRegProgressBar.skipProgressBar(workload);
	turboRegProgressBar.workloadDone(2 * halfHeight);
	return(halfMask);
} /* end getHalfMask2D */

} /* end class turboRegMask */

/*====================================================================
|	turboRegPointAction
\===================================================================*/

/*********************************************************************
 This class implements the various listeners that are in charge of
 user interactions when dealing with landmarks. It overrides the
 listeners of ImageJ, if any. Those are restored upon restitution
 of this <code>ImageCanvas</code> object to ImageJ.
 ********************************************************************/
class turboRegPointAction
	extends
		ImageCanvas
	implements
		FocusListener,
		AdjustmentListener,
		KeyListener,
		MouseListener,
		MouseMotionListener

{ /* begin class turboRegPointAction */

/*....................................................................
	Private variables
....................................................................*/

private ImagePlus mainImp;
private ImagePlus secondaryImp;
private turboRegPointHandler mainPh;
private turboRegPointHandler secondaryPh;
private turboRegPointToolbar tb;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 Listen to <code>AdjustmentEvent</code> events.
 @param e Ignored.
 ********************************************************************/
public synchronized void adjustmentValueChanged (
	AdjustmentEvent e
) {
	updateAndDraw();
} /* adjustmentValueChanged */

/*********************************************************************
 Listen to <code>focusGained</code> events.
 @param e Ignored.
 ********************************************************************/
public void focusGained (
	final FocusEvent e
) {
	updateAndDraw();
} /* end focusGained */

/*********************************************************************
 Listen to <code>focusGained</code> events.
 @param e Ignored.
 ********************************************************************/
public void focusLost (
	final FocusEvent e
) {
	updateAndDraw();
} /* end focusLost */

/*********************************************************************
 Listen to <code>keyPressed</code> events.
 @param e The expected key codes are as follows:
 <ul><li><code>KeyEvent.VK_COMMA</code>:
 display the previous slice, if any;</li>
 <li><code>KeyEvent.VK_DOWN</code>: move down the current landmark;</li>
 <li><code>KeyEvent.VK_LEFT</code>: move the current landmark to the left;</li>
 <li><code>KeyEvent.VK_PERIOD</code>: display the next slice, if any;</li>
 <li><code>KeyEvent.VK_SPACE</code>: select the current landmark;</li>
 <li><code>KeyEvent.VK_RIGHT</code>:
 move the current landmark to the right;</li>
 <li><code>KeyEvent.VK_UP</code>: move up the current landmark.</li></ul>
 ********************************************************************/
public void keyPressed (
	final KeyEvent e
) {
	switch (e.getKeyCode()) {
		case KeyEvent.VK_COMMA: {
			if (1 < mainImp.getCurrentSlice()) {
				mainImp.setSlice(mainImp.getCurrentSlice() - 1);
				updateStatus();
			}
			return;
		}
		case KeyEvent.VK_PERIOD: {
			if (mainImp.getCurrentSlice() < mainImp.getStackSize()) {
				mainImp.setSlice(mainImp.getCurrentSlice() + 1);
				updateStatus();
			}
			return;
		}
	}
	final int x = mainPh.getPoint().x;
	final int y = mainPh.getPoint().y;
	switch (e.getKeyCode()) {
		case KeyEvent.VK_DOWN: {
			mainPh.movePoint(mainImp.getWindow().getCanvas().screenX(x),
				mainImp.getWindow().getCanvas().screenY(y
				+ (int)Math.ceil(1.0
				/ mainImp.getWindow().getCanvas().getMagnification())));
			mainImp.setRoi(mainPh);
			break;
		}
		case KeyEvent.VK_LEFT: {
			mainPh.movePoint(mainImp.getWindow().getCanvas().screenX(x
				- (int)Math.ceil(1.0
				/ mainImp.getWindow().getCanvas().getMagnification())),
				mainImp.getWindow().getCanvas().screenY(y));
			mainImp.setRoi(mainPh);
			break;
		}
		case KeyEvent.VK_RIGHT: {
			mainPh.movePoint(mainImp.getWindow().getCanvas().screenX(x
				+ (int)Math.ceil(1.0
				/ mainImp.getWindow().getCanvas().getMagnification())),
				mainImp.getWindow().getCanvas().screenY(y));
			mainImp.setRoi(mainPh);
			break;
		}
		case KeyEvent.VK_SPACE: {
			break;
		}
		case KeyEvent.VK_UP: {
			mainPh.movePoint(mainImp.getWindow().getCanvas().screenX(x),
				mainImp.getWindow().getCanvas().screenY(y
				- (int)Math.ceil(1.0
				/ mainImp.getWindow().getCanvas().getMagnification())));
			mainImp.setRoi(mainPh);
			break;
		}
	}
	updateStatus();
} /* end keyPressed */

/*********************************************************************
 Listen to <code>keyReleased</code> events.
 @param e Ignored.
 ********************************************************************/
public void keyReleased (
	final KeyEvent e
) {
} /* end keyReleased */

/*********************************************************************
 Listen to <code>keyTyped</code> events.
 @param e Ignored.
 ********************************************************************/
public void keyTyped (
	final KeyEvent e
) {
} /* end keyTyped */

/*********************************************************************
 Listen to <code>mouseClicked</code> events.
 @param e Ignored.
 ********************************************************************/
public void mouseClicked (
	final MouseEvent e
) {
} /* end mouseClicked */

/*********************************************************************
 Listen to <code>mouseDragged</code> events. Move the position of
 the current point. Update the window's ROI. Update ImageJ's window.
 @param e Event.
 @see turboRegPointHandler#movePoint(int, int)
 @see turboRegPointAction#mouseMoved(java.awt.event.MouseEvent)
 ********************************************************************/
public void mouseDragged (
	final MouseEvent e
) {
	final int x = e.getX();
	final int y = e.getY();
	if (tb.getCurrentTool() == turboRegPointHandler.MOVE_CROSS) {
		mainPh.movePoint(x, y);
		updateAndDraw();
	}
	mouseMoved(e);
} /* end mouseDragged */

/*********************************************************************
 Listen to <code>mouseEntered</code> events. Change the cursor to a
 crosshair.
 @param e Event.
 ********************************************************************/
public void mouseEntered (
	final MouseEvent e
) {
	WindowManager.setCurrentWindow(mainImp.getWindow());
	mainImp.getWindow().toFront();
	mainImp.getWindow().getCanvas().setCursor(crosshairCursor);
	updateAndDraw();
} /* end mouseEntered */

/*********************************************************************
 Listen to <code>mouseExited</code> events. Change the cursor to the
 default cursor. Update the ImageJ status.
 @param e Event.
 ********************************************************************/
public void mouseExited (
	final MouseEvent e
) {
	mainImp.getWindow().getCanvas().setCursor(defaultCursor);
	IJ.showStatus("");
} /* end mouseExited */

/*********************************************************************
 Listen to <code>mouseMoved</code> events. Update the ImageJ status
 by displaying the value of the pixel under the cursor hot spot.
 @param e Event.
 ********************************************************************/
public void mouseMoved (
	final MouseEvent e
) {
	int x = e.getX();
	int y = e.getY();
	x = mainImp.getWindow().getCanvas().offScreenX(x);
	y = mainImp.getWindow().getCanvas().offScreenY(y);
	IJ.showStatus(mainImp.getLocationAsString(x, y) + getValueAsString(x, y));
} /* end mouseMoved */

/*********************************************************************
 Listen to <code>mousePressed</code> events. Update the current point
 or call the ImageJ's zoom methods.
 @param e Event.
 ********************************************************************/
public void mousePressed (
	final MouseEvent e
) {
	final int x = e.getX();
	final int y = e.getY();
	switch (tb.getCurrentTool()) {
		case turboRegPointHandler.MAGNIFIER: {
			int flags = e.getModifiers();
			if ((flags & (Event.ALT_MASK | Event.META_MASK | Event.CTRL_MASK))
				!= 0) {
				mainImp.getWindow().getCanvas().zoomOut(x, y);
			}
			else {
				mainImp.getWindow().getCanvas().zoomIn(x, y);
			}
			break;
		}
		case turboRegPointHandler.MOVE_CROSS: {
			final int currentPoint = mainPh.findClosest(x, y);
			secondaryPh.setCurrentPoint(currentPoint);
			updateAndDraw();
			break;
		}
	}
} /* end mousePressed */

/*********************************************************************
 Listen to <code>mouseReleased</code> events.
 @param e Ignored.
 ********************************************************************/
public void mouseReleased (
	final MouseEvent e
) {
} /* end mouseReleased */

/*********************************************************************
 Set a reference to the <code>ImagePlus</code> and
 <code>turboRegPointHandler</code> objects of the other image.
 @param secondaryImp <code>ImagePlus</code> object.
 @param secondaryPh <code>turboRegPointHandler</code> object.
 ********************************************************************/
public void setSecondaryPointHandler (
	final ImagePlus secondaryImp,
	final turboRegPointHandler secondaryPh
) {
	this.secondaryImp = secondaryImp;
	this.secondaryPh = secondaryPh;
} /* end setSecondaryPointHandler */

/*********************************************************************
 Keep a local copy of the <code>turboRegPointHandler</code> and
 <code>turboRegPointToolbar</code> objects.
 @param imp <code>ImagePlus</code> object.
 @param ph <code>turboRegPointHandler</code> object.
 @param tb <code>turboRegPointToolbar</code> object.
 ********************************************************************/
public turboRegPointAction (
	final ImagePlus imp,
	final turboRegPointHandler ph,
	final turboRegPointToolbar tb
) {
	super(imp);
	this.mainImp = imp;
	this.mainPh = ph;
	this.tb = tb;
} /* end turboRegPointAction */

/*....................................................................
	Private methods
....................................................................*/

/*------------------------------------------------------------------*/
private String getValueAsString (
	final int x,
	final int y
) {
	final Calibration cal = imp.getCalibration();
	final int[] v = imp.getPixel(x, y);
	switch (imp.getType()) {
		case ImagePlus.GRAY8:
		case ImagePlus.GRAY16: {
			final double cValue = cal.getCValue(v[0]);
			if (cValue==v[0]) {
				return(", value=" + v[0]);
			}
			else {
				return(", value=" + IJ.d2s(cValue) + " (" + v[0] + ")");
			}
		}
		case ImagePlus.GRAY32: {
			return(", value=" + Float.intBitsToFloat(v[0]));
		}
		case ImagePlus.COLOR_256: {
			return(", index=" + v[3] + ", value="
				+ v[0] + "," + v[1] + "," + v[2]);
		}
		case ImagePlus.COLOR_RGB: {
			return(", value=" + v[0] + "," + v[1] + "," + v[2]);
		}
		default: {
			return("");
		}
	}
} /* end getValueAsString */

/*------------------------------------------------------------------*/
private void updateAndDraw (
) {
	mainImp.setRoi(mainPh);
	secondaryImp.setRoi(secondaryPh);
} /* end updateAndDraw */

/*------------------------------------------------------------------*/
private void updateStatus (
) {
	final Point p = mainPh.getPoint();
	if (p == null) {
		IJ.showStatus("");
		return;
	}
	final int x = p.x;
	final int y = p.y;
	IJ.showStatus(imp.getLocationAsString(x, y) + getValueAsString(x, y));
} /* end updateStatus */

} /* end class turboRegPointAction */

/*====================================================================
|	turboRegPointHandler
\===================================================================*/

/*********************************************************************
 This class implements the graphic interactions when dealing with
 landmarks.
 ********************************************************************/
class turboRegPointHandler
	extends
		Roi

{ /* begin class turboRegPointHandler */

/*....................................................................
	Public variables
....................................................................*/

/*********************************************************************
 The magnifying tool is set in eleventh position to be coherent with
 ImageJ.
 ********************************************************************/
public static final int MAGNIFIER = 11;

/*********************************************************************
 The moving tool is set in second position to be coherent with the
 <code>PointPicker_</code> plugin.
 ********************************************************************/
public static final int MOVE_CROSS = 1;

/*********************************************************************
 The number of points we are willing to deal with is at most
 <code>4</code>.
 @see turboRegDialog#transformation
 ********************************************************************/
public static final int NUM_POINTS = 4;

/*....................................................................
	Private variables
....................................................................*/

/*********************************************************************
 The drawn landmarks fit in a 11x11 matrix.
 ********************************************************************/
private static final int CROSS_HALFSIZE = 5;

/*********************************************************************
 The golden ratio mathematical constant determines where to put the
 initial landmarks.
 ********************************************************************/
private static final double GOLDEN_RATIO = 0.5 * (Math.sqrt(5.0) - 1.0);

private final Point[] point = new Point[NUM_POINTS];
private final Color[] spectrum = new Color[NUM_POINTS];
private double[][] precisionPoint = new double[NUM_POINTS][2];
private int transformation;
private int currentPoint = 0;
private boolean interactive = true;
private boolean started = false;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 Draw the landmarks. Outline the current point if the window has focus.
 @param g Graphics environment.
 ********************************************************************/
public void draw (
	final Graphics g
) {
	if (started) {
		final double mag = ic.getMagnification();
		final int dx = (int)(mag / 2.0);
		final int dy = (int)(mag / 2.0);
		Point p;
		if (transformation == turboRegDialog.RIGID_BODY) {
			if (currentPoint == 0) {
				for (int k = 1; (k < transformation); k++) {
					p = point[k];
					g.setColor(spectrum[k]);
					g.fillRect(ic.screenX(p.x) - 2 + dx,
						ic.screenY(p.y) - 2 + dy, 5, 5);
				}
				drawHorizon(g);
				p = point[0];
				g.setColor(spectrum[0]);
				if (WindowManager.getCurrentImage() == imp) {
					g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y - 1) + dy,
						ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y - 1) + dy);
					g.drawLine(ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y - 1) + dy,
						ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
					g.drawLine(ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
						ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
					g.drawLine(ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
						ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y - 1) + dy);
					g.drawLine(ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y - 1) + dy,
						ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y - 1) + dy);
					g.drawLine(ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y - 1) + dy,
						ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y + 1) + dy);
					g.drawLine(ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y + 1) + dy,
						ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y + 1) + dy);
					g.drawLine(ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y + 1) + dy,
						ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
					g.drawLine(ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
						ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
					g.drawLine(ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
						ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y + 1) + dy);
					g.drawLine(ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y + 1) + dy,
						ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y + 1) + dy);
					g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y + 1) + dy,
						ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y - 1) + dy);
					if (1.0 < ic.getMagnification()) {
						g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE) + dx,
							ic.screenY(p.y) + dy,
							ic.screenX(p.x + CROSS_HALFSIZE) + dx,
							ic.screenY(p.y) + dy);
						g.drawLine(ic.screenX(p.x) + dx,
							ic.screenY(p.y - CROSS_HALFSIZE) + dy,
							ic.screenX(p.x) + dx,
							ic.screenY(p.y + CROSS_HALFSIZE) + dy);
					}
				}
				else {
					g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE + 1) + dy,
						ic.screenX(p.x + CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE - 1) + dy);
					g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE - 1) + dy,
						ic.screenX(p.x + CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE + 1) + dy);
				}
			}
			else {
				p = point[0];
				g.setColor(spectrum[0]);
					g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE) + dx,
						ic.screenY(p.y) + dy,
						ic.screenX(p.x + CROSS_HALFSIZE) + dx,
						ic.screenY(p.y) + dy);
					g.drawLine(ic.screenX(p.x) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE) + dy,
						ic.screenX(p.x) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE) + dy);
				drawHorizon(g);
				if (WindowManager.getCurrentImage() == imp) {
					drawArcs(g);
					for (int k = 1; (k < transformation); k++) {
						p = point[k];
						g.setColor(spectrum[k]);
						if (k == currentPoint) {
							g.drawRect(ic.screenX(p.x) - 3 + dx,
								ic.screenY(p.y) - 3 + dy, 6, 6);
						}
						else {
							g.fillRect(ic.screenX(p.x) - 2 + dx,
								ic.screenY(p.y) - 2 + dy, 5, 5);
						}
					}
				}
				else {
					for (int k = 1; (k < transformation); k++) {
						p = point[k];
						g.setColor(spectrum[k]);
						if (k == currentPoint) {
							g.drawRect(ic.screenX(p.x) - 2 + dx,
								ic.screenY(p.y) - 2 + dy, 5, 5);
						}
						else {
							g.fillRect(ic.screenX(p.x) - 2 + dx,
								ic.screenY(p.y) - 2 + dy, 5, 5);
						}
					}
				}
			}
		}
		else {
			for (int k = 0; (k < (transformation / 2)); k++) {
				p = point[k];
				g.setColor(spectrum[k]);
				if (k == currentPoint) {
					if (WindowManager.getCurrentImage() == imp) {
						g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
							ic.screenY(p.y - 1) + dy,
							ic.screenX(p.x - 1) + dx,
							ic.screenY(p.y - 1) + dy);
						g.drawLine(ic.screenX(p.x - 1) + dx,
							ic.screenY(p.y - 1) + dy,
							ic.screenX(p.x - 1) + dx,
							ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
						g.drawLine(ic.screenX(p.x - 1) + dx,
							ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
							ic.screenX(p.x + 1) + dx,
							ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
						g.drawLine(ic.screenX(p.x + 1) + dx,
							ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
							ic.screenX(p.x + 1) + dx,
							ic.screenY(p.y - 1) + dy);
						g.drawLine(ic.screenX(p.x + 1) + dx,
							ic.screenY(p.y - 1) + dy,
							ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
							ic.screenY(p.y - 1) + dy);
						g.drawLine(ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
							ic.screenY(p.y - 1) + dy,
							ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
							ic.screenY(p.y + 1) + dy);
						g.drawLine(ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
							ic.screenY(p.y + 1) + dy,
							ic.screenX(p.x + 1) + dx,
							ic.screenY(p.y + 1) + dy);
						g.drawLine(ic.screenX(p.x + 1) + dx,
							ic.screenY(p.y + 1) + dy,
							ic.screenX(p.x + 1) + dx,
							ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
						g.drawLine(ic.screenX(p.x + 1) + dx,
							ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
							ic.screenX(p.x - 1) + dx,
							ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
						g.drawLine(ic.screenX(p.x - 1) + dx,
							ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
							ic.screenX(p.x - 1) + dx,
							ic.screenY(p.y + 1) + dy);
						g.drawLine(ic.screenX(p.x - 1) + dx,
							ic.screenY(p.y + 1) + dy,
							ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
							ic.screenY(p.y + 1) + dy);
						g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
							ic.screenY(p.y + 1) + dy,
							ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
							ic.screenY(p.y - 1) + dy);
						if (1.0 < ic.getMagnification()) {
							g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE) + dx,
								ic.screenY(p.y) + dy,
								ic.screenX(p.x + CROSS_HALFSIZE) + dx,
								ic.screenY(p.y) + dy);
							g.drawLine(ic.screenX(p.x) + dx,
								ic.screenY(p.y - CROSS_HALFSIZE) + dy,
								ic.screenX(p.x) + dx,
								ic.screenY(p.y + CROSS_HALFSIZE) + dy);
						}
					}
					else {
						g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE + 1) + dx,
							ic.screenY(p.y - CROSS_HALFSIZE + 1) + dy,
							ic.screenX(p.x + CROSS_HALFSIZE - 1) + dx,
							ic.screenY(p.y + CROSS_HALFSIZE - 1) + dy);
						g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE + 1) + dx,
							ic.screenY(p.y + CROSS_HALFSIZE - 1) + dy,
							ic.screenX(p.x + CROSS_HALFSIZE - 1) + dx,
							ic.screenY(p.y - CROSS_HALFSIZE + 1) + dy);
					}
				}
				else {
					g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE) + dx,
						ic.screenY(p.y) + dy,
						ic.screenX(p.x + CROSS_HALFSIZE) + dx,
						ic.screenY(p.y) + dy);
					g.drawLine(ic.screenX(p.x) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE) + dy,
						ic.screenX(p.x) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE) + dy);
				}
			}
		}
		if (updateFullWindow) {
			updateFullWindow = false;
			imp.draw();
		}
	}
} /* end draw */

/*********************************************************************
 Set the current point as that which is closest to (x, y).
 @param x Horizontal coordinate in canvas units.
 @param y Vertical coordinate in canvas units.
 ********************************************************************/
public int findClosest (
	int x,
	int y
) {
	x = ic.offScreenX(x);
	y = ic.offScreenY(y);
	int closest = 0;
	Point p = point[closest];
	double distance = (double)(x - p.x) * (double)(x - p.x)
		+ (double)(y - p.y) * (double)(y - p.y);
	double candidate;
	if (transformation == turboRegDialog.RIGID_BODY) {
		for (int k = 1; (k < transformation); k++) {
			p = point[k];
			candidate = (double)(x - p.x) * (double)(x - p.x)
				+ (double)(y - p.y) * (double)(y - p.y);
			if (candidate < distance) {
				distance = candidate;
				closest = k;
			}
		}
	}
	else {
		for (int k = 1; (k < (transformation / 2)); k++) {
			p = point[k];
			candidate = (double)(x - p.x) * (double)(x - p.x)
				+ (double)(y - p.y) * (double)(y - p.y);
			if (candidate < distance) {
				distance = candidate;
				closest = k;
			}
		}
	}
	currentPoint = closest;
	return(currentPoint);
} /* end findClosest */

/*********************************************************************
 Return the current point as a <code>Point</code> object.
 ********************************************************************/
public Point getPoint (
) {
	return(point[currentPoint]);
} /* end getPoint */

/*********************************************************************
 Return all landmarks as an array <code>double[transformation / 2][2]</code>,
 except for a rigid-body transformation for which the array has size
 <code>double[3][2]</code>.
 ********************************************************************/
public double[][] getPoints (
) {
	if (interactive) {
		if (transformation == turboRegDialog.RIGID_BODY) {
			double[][] points = new double[transformation][2];
			for (int k = 0; (k < transformation); k++) {
				points[k][0] = (double)point[k].x;
				points[k][1] = (double)point[k].y;
			}
			return(points);
		}
		else {
			double[][] points = new double[transformation / 2][2];
			for (int k = 0; (k < (transformation / 2)); k++) {
				points[k][0] = (double)point[k].x;
				points[k][1] = (double)point[k].y;
			}
			return(points);
		}
	}
	else {
		return(precisionPoint);
	}
} /* end getPoints */

/*********************************************************************
 Modify the location of the current point. Clip the admissible range
 to the image size.
 @param x Desired new horizontal coordinate in canvas units.
 @param y Desired new vertical coordinate in canvas units.
 ********************************************************************/
public void movePoint (
	int x,
	int y
) {
	interactive = true;
	x = ic.offScreenX(x);
	y = ic.offScreenY(y);
	x = (x < 0) ? (0) : (x);
	x = (imp.getWidth() <= x) ? (imp.getWidth() - 1) : (x);
	y = (y < 0) ? (0) : (y);
	y = (imp.getHeight() <= y) ? (imp.getHeight() - 1) : (y);
	if ((transformation == turboRegDialog.RIGID_BODY) && (currentPoint != 0)) {
		final Point p = new Point(x, y);
		final Point q = point[3 - currentPoint];
		final double radius = 0.5 * Math.sqrt(
			(ic.screenX(p.x) - ic.screenX(q.x))
			* (ic.screenX(p.x) - ic.screenX(q.x))
			+ (ic.screenY(p.y) - ic.screenY(q.y))
			* (ic.screenY(p.y) - ic.screenY(q.y)));
		if ((double)CROSS_HALFSIZE < radius) {
			point[currentPoint].x = x;
			point[currentPoint].y = y;
		}
	}
	else {
		point[currentPoint].x = x;
		point[currentPoint].y = y;
	}
} /* end movePoint */

/*********************************************************************
 Set a new current point.
 @param currentPoint New current point index.
 ********************************************************************/
public void setCurrentPoint (
	final int currentPoint
) {
	this.currentPoint = currentPoint;
} /* end setCurrentPoint */

/*********************************************************************
 Set new position for all landmarks, without clipping.
 @param precisionPoint New coordinates in canvas units.
 ********************************************************************/
public void setPoints (
	final double[][] precisionPoint
) {
	interactive = false;
	if (transformation == turboRegDialog.RIGID_BODY) {
		for (int k = 0; (k < transformation); k++) {
			point[k].x = (int)Math.round(precisionPoint[k][0]);
			point[k].y = (int)Math.round(precisionPoint[k][1]);
			this.precisionPoint[k][0] = precisionPoint[k][0];
			this.precisionPoint[k][1] = precisionPoint[k][1];
		}
	}
	else {
		for (int k = 0; (k < (transformation / 2)); k++) {
			point[k].x = (int)Math.round(precisionPoint[k][0]);
			point[k].y = (int)Math.round(precisionPoint[k][1]);
			this.precisionPoint[k][0] = precisionPoint[k][0];
			this.precisionPoint[k][1] = precisionPoint[k][1];
		}
	}
} /* end setPoints */

/*********************************************************************
 Reset the landmarks to their initial position for the given
 transformation.
 @param transformation Transformation code.
 ********************************************************************/
public void setTransformation (
	final int transformation
) {
	interactive = true;
	this.transformation = transformation;
	final int width = imp.getWidth();
	final int height = imp.getHeight();
	currentPoint = 0;
	switch (transformation) {
		case turboRegDialog.TRANSLATION: {
			point[0] = new Point(
				Math.round((float)(Math.floor(0.5 * (double)width))),
				Math.round((float)(Math.floor(0.5 * (double)height))));
			break;
		}
		case turboRegDialog.RIGID_BODY: {
			point[0] = new Point(
				Math.round((float)(Math.floor(0.5 * (double)width))),
				Math.round((float)(Math.floor(0.5 * (double)height))));
			point[1] = new Point(
				Math.round((float)(Math.floor(0.5 * (double)width))),
				Math.round((float)(Math.ceil(0.25 * GOLDEN_RATIO
				* (double)height))));
			point[2] = new Point(
				Math.round((float)(Math.floor(0.5 * (double)width))),
				height - Math.round((float)(Math.ceil(0.25 * GOLDEN_RATIO
				* (double)height))));
			break;
		}
		case turboRegDialog.SCALED_ROTATION: {
			point[0] = new Point(
				Math.round((float)(Math.floor(0.25 * GOLDEN_RATIO
				* (double)width))),
				Math.round((float)(Math.floor(0.5 * (double)height))));
			point[1] = new Point(
				width - Math.round((float)(Math.ceil(0.25 * GOLDEN_RATIO
				* (double)width))),
				Math.round((float)(Math.floor(0.5 * (double)height))));
			break;
		}
		case turboRegDialog.AFFINE: {
			point[0] = new Point(
				Math.round((float)(Math.floor(0.5 * (double)width))),
				Math.round((float)(Math.floor(0.25 * GOLDEN_RATIO
				* (double)height))));
			point[1] = new Point(
				Math.round((float)(Math.floor(0.25 * GOLDEN_RATIO
				* (double)width))),
				height - Math.round((float)(Math.ceil(0.25 * GOLDEN_RATIO
				* (double)height))));
			point[2] = new Point(
				width - Math.round((float)(Math.ceil(0.25 * GOLDEN_RATIO
				* (double)width))),
				height - Math.round((float)(Math.ceil(0.25 * GOLDEN_RATIO
				* (double)height))));
			break;
		}
		case turboRegDialog.BILINEAR: {
			point[0] = new Point(
				Math.round((float)(Math.floor(0.25 * GOLDEN_RATIO
				* (double)width))),
				Math.round((float)(Math.floor(0.25 * GOLDEN_RATIO
				* (double)height))));
			point[1] = new Point(
				Math.round((float)(Math.floor(0.25 * GOLDEN_RATIO
				* (double)width))),
				height - Math.round((float)(Math.ceil(0.25 * GOLDEN_RATIO
				* (double)height))));
			point[2] = new Point(
				width - Math.round((float)(Math.ceil(0.25 * GOLDEN_RATIO
				* (double)width))),
				Math.round((float)(Math.floor(0.25 * GOLDEN_RATIO
				* (double)height))));
			point[3] = new Point(
				width - Math.round((float)(Math.ceil(0.25 * GOLDEN_RATIO
				* (double)width))),
				height - Math.round((float)(Math.ceil(0.25 * GOLDEN_RATIO
				* (double)height))));
			break;
		}
	}
	setSpectrum();
	imp.updateAndDraw();
} /* end setTransformation */

/*********************************************************************
 Keep a local copy of the points and of the transformation.
 ********************************************************************/
public turboRegPointHandler (
	final double[][] precisionPoint,
	final int transformation
) {
	super(0, 0, 0, 0, null);
	this.transformation = transformation;
	this.precisionPoint = precisionPoint;
	interactive = false;
} /* end turboRegPointHandler */

/*********************************************************************
 Keep a local copy of the <code>ImagePlus</code> object. Set the
 landmarks to their initial position for the given transformation.
 @param imp <code>ImagePlus</code> object.
 @param transformation Transformation code.
 @see turboRegDialog#restoreAll()
 ********************************************************************/
public turboRegPointHandler (
	final ImagePlus imp,
	final int transformation
) {
	super(0, 0, imp.getWidth(), imp.getHeight(), imp);
	this.transformation = transformation;
	setTransformation(transformation);
	imp.setRoi(this);
	started = true;
} /* end turboRegPointHandler */

/*....................................................................
	Private methods
....................................................................*/

/*------------------------------------------------------------------*/
private void drawArcs (
	final Graphics g
) {
	final double mag = ic.getMagnification();
	final int dx = (int)(mag / 2.0);
	final int dy = (int)(mag / 2.0);
	final Point p = point[1];
	final Point q = point[2];
	final double x0 = (double)(ic.screenX(p.x) + ic.screenX(q.x));
	final double y0 = (double)(ic.screenY(p.y) + ic.screenY(q.y));
	final double dx0 = (double)(ic.screenX(p.x) - ic.screenX(q.x));
	final double dy0 = (double)(ic.screenY(p.y) - ic.screenY(q.y));
	final double radius = 0.5 * Math.sqrt(dx0 * dx0 + dy0 * dy0);
	final double orientation = Math.atan2(dx0, dy0);
	final double spacerAngle = Math.asin((double)CROSS_HALFSIZE / radius);
	g.setColor(spectrum[1]);
	g.drawArc((int)Math.round(0.5 * x0 - radius) + dx,
		(int)Math.round(0.5 * y0 - radius) + dy,
		(int)Math.round(2.0 * radius), (int)Math.round(2.0 * radius),
		(int)Math.round((orientation + spacerAngle + Math.PI)
		* 180.0 / Math.PI),
		(int)Math.round((Math.PI - 2.0 * spacerAngle) * 180.0 / Math.PI));
	g.setColor(spectrum[2]);
	g.drawArc((int)Math.round(0.5 * x0 - radius) + dx,
		(int)Math.round(0.5 * y0 - radius) + dy,
		(int)Math.round(2.0 * radius), (int)Math.round(2.0 * radius),
		(int)Math.round((orientation + spacerAngle) * 180.0 / Math.PI),
		(int)Math.round((Math.PI - 2.0 * spacerAngle) * 180.0 / Math.PI));
} /* end drawArcs */

/*------------------------------------------------------------------*/
private void drawHorizon (
	final Graphics g
) {
	final double mag = ic.getMagnification();
	final int dx = (int)(mag / 2.0);
	final int dy = (int)(mag / 2.0);
	final Point p = point[1];
	final Point q = point[2];
	final double x0 = (double)(ic.screenX(p.x) + ic.screenX(q.x));
	final double y0 = (double)(ic.screenY(p.y) + ic.screenY(q.y));
	final double dx0 = (double)(ic.screenX(p.x) - ic.screenX(q.x));
	final double dy0 = (double)(ic.screenY(p.y) - ic.screenY(q.y));
	final double radius = 0.5 * Math.sqrt(dx0 * dx0 + dy0 * dy0);
	final double spacerAngle = Math.asin((double)CROSS_HALFSIZE / radius);
	final double s0 = Math.sin(spacerAngle);
	final double s = 0.5 * dx0 / radius;
	final double c = 0.5 * dy0 / radius;
	double u;
	double v;
	g.setColor(spectrum[1]);
	u = 0.5 * (x0 + s0 * dx0);
	v = 0.5 * (y0 + s0 * dy0);
	if (Math.abs(s) < Math.abs(c)) {
		g.drawLine(-dx, (int)Math.round(
			v + (u + 2.0 * (double)dx) * s / c) + dy,
			(int)Math.round(mag * (double)ic.getSrcRect().width - 1.0) + dx,
			(int)Math.round(v - (mag * (double)ic.getSrcRect().width - 1.0 - u)
			* s / c) + dy);
	}
	else {
		g.drawLine((int)Math.round(
			u + (v + 2.0 * (double)dy) * c / s) + dx, -dy,
			(int)Math.round(u - (mag * (double)ic.getSrcRect().height - 1.0 - v)
			* c / s) + dx,
			(int)Math.round(mag * (double)ic.getSrcRect().height - 1.0) + dy);
	}
	g.setColor(spectrum[2]);
	u = 0.5 * (x0 - s0 * dx0);
	v = 0.5 * (y0 - s0 * dy0);
	if (Math.abs(s) < Math.abs(c)) {
		g.drawLine(-dx, (int)Math.round(
			v + (u + 2.0 * (double)dx) * s / c) + dy,
			(int)Math.round(mag * (double)ic.getSrcRect().width - 1.0) + dx,
			(int)Math.round(v - (mag * (double)ic.getSrcRect().width - 1.0 - u)
			* s / c) + dy);
	}
	else {
		g.drawLine((int)Math.round(
			u + (v + 2.0 * (double)dy) * c / s) + dx, -dy,
			(int)Math.round(u - (mag * (double)ic.getSrcRect().height - 1.0 - v)
			* c / s) + dx, (int)Math.round(
			mag * (double)ic.getSrcRect().height - 1.0) + dy);
	}
} /* end drawHorizon */

/*------------------------------------------------------------------*/
private void setSpectrum (
) {
	if (transformation == turboRegDialog.RIGID_BODY) {
		spectrum[0] = Color.green;
		spectrum[1] = new Color(16, 119, 169);
		spectrum[2] = new Color(119, 85, 51);
	}
	else {
		spectrum[0] = Color.green;
		spectrum[1] = Color.yellow;
		spectrum[2] = Color.magenta;
		spectrum[3] = Color.cyan;
	}
} /* end setSpectrum */

} /* end class turboRegPointHandler */

/*====================================================================
|	turboRegPointToolbar
\===================================================================*/

/*********************************************************************
 This class implements the user interactions when dealing with
 the toolbar in the ImageJ's window.
 ********************************************************************/
class turboRegPointToolbar
	extends
		Canvas
	implements
		MouseListener

{ /* begin class turboRegPointToolbar */

/*....................................................................
	Private variables
....................................................................*/

/*********************************************************************
 Same number of tools than in ImageJ version 1.22
 ********************************************************************/
private static final int NUM_TOOLS = 19;

/*********************************************************************
 Same tool offset than in ImageJ version 1.22
 ********************************************************************/
private static final int OFFSET = 3;

/*********************************************************************
 Same tool size than in ImageJ version 1.22
 ********************************************************************/
private static final int SIZE = 22;

private final Color gray = Color.lightGray;
private final Color brighter = gray.brighter();
private final Color darker = gray.darker();
private final Color evenDarker = darker.darker();
private final boolean[] down = new boolean[NUM_TOOLS];
private turboRegPointToolbar instance;
private Toolbar previousInstance;
private Graphics g;
private int currentTool = turboRegPointHandler.MOVE_CROSS;
private int x;
private int y;
private int xOffset;
private int yOffset;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 Return the current tool index.
 ********************************************************************/
public int getCurrentTool (
) {
	return(currentTool);
} /* getCurrentTool */

/*********************************************************************
 Listen to <code>mouseClicked</code> events.
 @param e Ignored.
 ********************************************************************/
public void mouseClicked (
	final MouseEvent e
) {
} /* end mouseClicked */

/*********************************************************************
 Listen to <code>mouseEntered</code> events.
 @param e Ignored.
 ********************************************************************/
public void mouseEntered (
	final MouseEvent e
) {
} /* end mouseEntered */

/*********************************************************************
 Listen to <code>mouseExited</code> events.
 @param e Ignored.
 ********************************************************************/
public void mouseExited (
	final MouseEvent e
) {
} /* end mouseExited */

/*********************************************************************
 Listen to <code>mousePressed</code> events. Set the current tool index.
 @param e Event.
 ********************************************************************/
public void mousePressed (
	final MouseEvent e
) {
	final int x = e.getX();
	final int y = e.getY();
	int newTool = 0;
	for (int i = 0; (i < NUM_TOOLS); i++) {
		if (((i * SIZE) < x) && (x < (i * SIZE + SIZE))) {
			newTool = i;
		}
	}
	setTool(newTool);
} /* mousePressed */

/*********************************************************************
 Listen to <code>mouseReleased</code> events.
 @param e Ignored.
 ********************************************************************/
public void mouseReleased (
	final MouseEvent e
) {
} /* end mouseReleased */

/*********************************************************************
 Draw the toolbar tools.
 @param g Graphics environment.
 ********************************************************************/
public void paint (
	final Graphics g
) {
	drawButtons(g);
} /* paint */

/*********************************************************************
 Restore the ImageJ toolbar.
 ********************************************************************/
public void restorePreviousToolbar (
) {
	final Container container = instance.getParent();
	final Component[] component = container.getComponents();
	for (int i = 0; (i < component.length); i++) {
		if (component[i] == instance) {
			container.remove(instance);
			container.add(previousInstance, i);
			container.validate();
			break;
		}
	}
} /* end restorePreviousToolbar */

/*********************************************************************
 Set the current tool and update its appearance on the toolbar.
 @param tool Tool index.
 ********************************************************************/
public void setTool (
	final int tool
) {
	if (tool == currentTool) {
		return;
	}
	down[tool] = true;
	down[currentTool] = false;
	final Graphics g = this.getGraphics();
	drawButton(g, currentTool);
	drawButton(g, tool);
	g.dispose();
	showMessage(tool);
	currentTool = tool;
} /* end setTool */

/*********************************************************************
 Override the ImageJ toolbar by this <code>turboRegToolbar</code>
 object. Store a local copy of the ImageJ's toolbar for later restore.
 @see turboRegPointToolbar#restorePreviousToolbar()
 ********************************************************************/
public turboRegPointToolbar (
	final Toolbar previousToolbar
) {
	previousInstance = previousToolbar;
	instance = this;
	final Container container = previousToolbar.getParent();
	final Component[] component = container.getComponents();
	for (int i = 0; (i < component.length); i++) {
		if (component[i] == previousToolbar) {
			container.remove(previousToolbar);
			container.add(this, i);
			break;
		}
	}
	resetButtons();
	down[currentTool] = true;
	setTool(currentTool);
	setForeground(Color.black);
	setBackground(gray);
	addMouseListener(this);
	container.validate();
} /* end turboRegPointToolbar */

/*....................................................................
	Private methods
....................................................................*/

/*------------------------------------------------------------------*/
private void d (
	int x,
	int y
) {
	x += xOffset;
	y += yOffset;
	g.drawLine(this.x, this.y, x, y);
	this.x = x;
	this.y = y;
} /* end d */

/*------------------------------------------------------------------*/
private void drawButton (
	final Graphics g,
	final int tool
) {
	fill3DRect(g, tool * SIZE + 1, 1, SIZE, SIZE - 1, !down[tool]);
	g.setColor(Color.black);
	int x = tool * SIZE + OFFSET;
	int y = OFFSET;
	if (down[tool]) {
		x++;
		y++;
	}
	this.g = g;
	switch (tool) {
		case turboRegPointHandler.MOVE_CROSS: {
			xOffset = x;
			yOffset = y;
			m(1, 1);
			d(1, 10);
			m(2, 2);
			d(2, 9);
			m(3, 3);
			d(3, 8);
			m(4, 4);
			d(4, 7);
			m(5, 5);
			d(5, 7);
			m(6, 6);
			d(6, 7);
			m(7, 7);
			d(7, 7);
			m(11, 5);
			d(11, 6);
			m(10, 7);
			d(10, 8);
			m(12, 7);
			d(12, 8);
			m(9, 9);
			d(9, 11);
			m(13, 9);
			d(13, 11);
			m(10, 12);
			d(10, 15);
			m(12, 12);
			d(12, 15);
			m(11, 9);
			d(11, 10);
			m(11, 13);
			d(11, 15);
			m(9, 13);
			d(13, 13);
			break;
		}
		case turboRegPointHandler.MAGNIFIER: {
			xOffset = x + 2;
			yOffset = y + 2;
			m(3, 0);
			d(3, 0);
			d(5, 0);
			d(8, 3);
			d(8, 5);
			d(7, 6);
			d(7, 7);
			d(6, 7);
			d(5, 8);
			d(3, 8);
			d(0, 5);
			d(0, 3);
			d(3, 0);
			m(8, 8);
			d(9, 8);
			d(13, 12);
			d(13, 13);
			d(12, 13);
			d(8, 9);
			d(8, 8);
			break;
		}
	}
} /* end drawButton */

/*------------------------------------------------------------------*/
private void drawButtons (
	final Graphics g
) {
	for (int i = 0; (i < NUM_TOOLS); i++) {
		drawButton(g, i);
	}
} /* end drawButtons */

/*------------------------------------------------------------------*/
private void fill3DRect (
	final Graphics g,
	final int x,
	final int y,
	final int width,
	final int height,
	final boolean raised
) {
	if (raised) {
		g.setColor(gray);
	}
	else {
		g.setColor(darker);
	}
	g.fillRect(x + 1, y + 1, width - 2, height - 2);
	g.setColor((raised) ? (brighter) : (evenDarker));
	g.drawLine(x, y, x, y + height - 1);
	g.drawLine(x + 1, y, x + width - 2, y);
	g.setColor((raised) ? (evenDarker) : (brighter));
	g.drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
	g.drawLine(x + width - 1, y, x + width - 1, y + height - 2);
} /* end fill3DRect */

/*------------------------------------------------------------------*/
private void m (
	final int x,
	final int y
) {
	this.x = xOffset + x;
	this.y = yOffset + y;
} /* end m */

/*------------------------------------------------------------------*/
private void resetButtons (
) {
	for (int i = 0; (i < NUM_TOOLS); i++) {
		down[i] = false;
	}
} /* end resetButtons */

/*------------------------------------------------------------------*/
private void showMessage (
	final int tool
) {
	switch (tool) {
		case turboRegPointHandler.MOVE_CROSS: {
			IJ.showStatus("Move crosses");
			break;
		}
		case turboRegPointHandler.MAGNIFIER: {
			IJ.showStatus("Magnifying glass");
			break;
		}
		default: {
			IJ.showStatus("Undefined operation");
			break;
		}
	}
} /* end showMessage */

} /* end class turboRegPointToolbar */

/*====================================================================
|	turboRegProgressBar
\===================================================================*/

/*********************************************************************
 This class implements the interactions when dealing with ImageJ's
 progress bar.
 ********************************************************************/
class turboRegProgressBar

{ /* begin class turboRegProgressBar */

/*....................................................................
	Private variables
....................................................................*/

/*********************************************************************
 Same time constant than in ImageJ version 1.22
 ********************************************************************/
private static final long TIME_QUANTUM = 50L;

private static volatile long lastTime = System.currentTimeMillis();
private static volatile int completed = 0;
private static volatile int workload = 0;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 Extend the amount of work to perform by <code>batch</code>.
 @param batch Additional amount of work that need be performed.
 ********************************************************************/
public static synchronized void addWorkload (
	final int batch
) {
	workload += batch;
} /* end addWorkload */

/*********************************************************************
 Erase the progress bar and cancel pending operations.
 ********************************************************************/
public static synchronized void resetProgressBar (
) {
	final long timeStamp = System.currentTimeMillis();
	if ((timeStamp - lastTime) < TIME_QUANTUM) {
		try {
			Thread.sleep(TIME_QUANTUM - timeStamp + lastTime);
		} catch (InterruptedException e) {
			IJ.log(
				"Unexpected interruption exception " + e.getMessage());
		}
	}
	lastTime = timeStamp;
	completed = 0;
	workload = 0;
	IJ.showProgress(1.0);
} /* end resetProgressBar */

/*********************************************************************
 Perform <code>stride</code> operations at once.
 @param stride Amount of work that is skipped.
 ********************************************************************/
public static synchronized void skipProgressBar (
	final int stride
) {
	completed += stride - 1;
	stepProgressBar();
} /* end skipProgressBar */

/*********************************************************************
 Perform <code>1</code> operation unit.
 ********************************************************************/
public static synchronized void stepProgressBar (
) {
	final long timeStamp = System.currentTimeMillis();
	completed = completed + 1;
	if ((TIME_QUANTUM <= (timeStamp - lastTime)) | (completed == workload)) {
		lastTime = timeStamp;
		IJ.showProgress((double)completed / (double)workload);
	}
} /* end stepProgressBar */

/*********************************************************************
 Acknowledge that <code>batch</code> work has been performed.
 @param batch Completed amount of work.
 ********************************************************************/
public static synchronized void workloadDone (
	final int batch
) {
	workload -= batch;
	completed -= batch;
} /* end workloadDone */

} /* end class turboRegProgressBar */

/*====================================================================
|	turboRegTransform
\===================================================================*/

/*********************************************************************
 This class implements the algorithmic methods of the plugin. It
 refines the landmarks and computes the final images.
 ********************************************************************/
class turboRegTransform

{ /* begin class turboRegTransform */

/*....................................................................
	Private variables
....................................................................*/

/*********************************************************************
 Maximal number of registration iterations per level, when
 speed is requested at the expense of accuracy. This number must be
 corrected so that there are more iterations at the coarse levels
 of the pyramid than at the fine levels.
 @see turboRegTransform#ITERATION_PROGRESSION
 ********************************************************************/
private static final int FEW_ITERATIONS = 5;

/*********************************************************************
 Initial value of the Marquardt-Levenberg fudge factor.
 ********************************************************************/
private static final double FIRST_LAMBDA = 1.0;

/*********************************************************************
 Update parameter of the Marquardt-Levenberg fudge factor.
 ********************************************************************/
private static final double LAMBDA_MAGSTEP = 4.0;

/*********************************************************************
 Maximal number of registration iterations per level, when
 accuracy is requested at the expense of speed. This number must be
 corrected so that there are more iterations at the coarse levels
 of the pyramid than at the fine levels.
 @see turboRegTransform#ITERATION_PROGRESSION
 ********************************************************************/
private static final int MANY_ITERATIONS = 10;

/*********************************************************************
 Minimal update distance of the landmarks, in pixel units, when
 accuracy is requested at the expense of speed. This distance does
 not depend on the pyramid level.
 ********************************************************************/
private static final double PIXEL_HIGH_PRECISION = 0.001;

/*********************************************************************
 Minimal update distance of the landmarks, in pixel units, when
 speed is requested at the expense of accuracy. This distance does
 not depend on the pyramid level.
 ********************************************************************/
private static final double PIXEL_LOW_PRECISION = 0.1;

/*********************************************************************
 Multiplicative factor that determines how many more iterations
 are allowed for a pyramid level one unit coarser.
 ********************************************************************/
private static final int ITERATION_PROGRESSION = 2;

private final double[] dxWeight = new double[4];
private final double[] dyWeight = new double[4];
private final double[] xWeight = new double[4];
private final double[] yWeight = new double[4];
private final int[] xIndex = new int[4];
private final int[] yIndex = new int[4];
private turboRegImage sourceImg;
private turboRegImage targetImg;
private turboRegMask sourceMsk;
private turboRegMask targetMsk;
private turboRegPointHandler sourcePh;
private turboRegPointHandler targetPh;
private double[][] sourcePoint;
private double[][] targetPoint;
private float[] inImg;
private float[] outImg;
private float[] xGradient;
private float[] yGradient;
private float[] inMsk;
private float[] outMsk;
private double targetJacobian;
private double s;
private double t;
private double x;
private double y;
private double c0;
private double c0u;
private double c0v;
private double c0uv;
private double c1;
private double c1u;
private double c1v;
private double c1uv;
private double c2;
private double c2u;
private double c2v;
private double c2uv;
private double c3;
private double c3u;
private double c3v;
private double c3uv;
private double pixelPrecision;
private int maxIterations;
private int p;
private int q;
private int inNx;
private int inNy;
private int outNx;
private int outNy;
private int twiceInNx;
private int twiceInNy;
private int transformation;
private int pyramidDepth;
private int iterationPower;
private int iterationCost;
private boolean accelerated;
private boolean interactive;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 Append the current landmarks into a text file. Rigid format.
 @param pathAndFilename Path and name of the file where batch results
 are being written.
 @see turboRegDialog#loadLandmarks()
 ********************************************************************/
public void appendTransformation (
	final String pathAndFilename
) {
	outNx = targetImg.getWidth();
	outNy = targetImg.getHeight();
	inNx = sourceImg.getWidth();
	inNy = sourceImg.getHeight();
	if (pathAndFilename == null) {
		return;
	}
	try {
		final FileWriter fw = new FileWriter(pathAndFilename, true);
		fw.write("\n");
		switch (transformation) {
			case turboRegDialog.TRANSLATION: {
				fw.write("TRANSLATION\n");
				break;
			}
			case turboRegDialog.RIGID_BODY: {
				fw.write("RIGID_BODY\n");
				break;
			}
			case turboRegDialog.SCALED_ROTATION: {
				fw.write("SCALED_ROTATION\n");
				break;
			}
			case turboRegDialog.AFFINE: {
				fw.write("AFFINE\n");
				break;
			}
			case turboRegDialog.BILINEAR: {
				fw.write("BILINEAR\n");
				break;
			}
		}
		fw.write("\n");
		fw.write("Source size\n");
		fw.write(inNx + "\t" + inNy + "\n");
		fw.write("\n");
		fw.write("Target size\n");
		fw.write(outNx + "\t" + outNy + "\n");
		fw.write("\n");
		fw.write("Refined source landmarks\n");
		if (transformation == turboRegDialog.RIGID_BODY) {
			for (int i = 0; (i < transformation); i++) {
				fw.write(sourcePoint[i][0] + "\t" + sourcePoint[i][1] + "\n");
			}
		}
		else {
			for (int i = 0; (i < (transformation / 2)); i++) {
				fw.write(sourcePoint[i][0] + "\t" + sourcePoint[i][1] + "\n");
			}
		}
		fw.write("\n");
		fw.write("Target landmarks\n");
		if (transformation == turboRegDialog.RIGID_BODY) {
			for (int i = 0; (i < transformation); i++) {
				fw.write(targetPoint[i][0] + "\t" + targetPoint[i][1] + "\n");
			}
		}
		else {
			for (int i = 0; (i < (transformation / 2)); i++) {
				fw.write(targetPoint[i][0] + "\t" + targetPoint[i][1] + "\n");
			}
		}
		fw.close();
	} catch (IOException e) {
		IJ.log(
			"IOException exception " + e.getMessage());
	} catch (SecurityException e) {
		IJ.log(
			"Security exception " + e.getMessage());
	}
} /* end appendTransformation */

/*********************************************************************
 Compute the final image.
 ********************************************************************/
public void doBatchFinalTransform (
	final float[] pixels
) {
	if (accelerated) {
		inImg = sourceImg.getImage();
	}
	else {
		inImg = sourceImg.getCoefficient();
	}
	inNx = sourceImg.getWidth();
	inNy = sourceImg.getHeight();
	twiceInNx = 2 * inNx;
	twiceInNy = 2 * inNy;
	outImg = pixels;
	outNx = targetImg.getWidth();
	outNy = targetImg.getHeight();
	final double[][] matrix = getTransformationMatrix(targetPoint, sourcePoint);
	switch (transformation) {
		case turboRegDialog.TRANSLATION: {
			translationTransform(matrix);
			break;
		}
		case turboRegDialog.RIGID_BODY:
		case turboRegDialog.SCALED_ROTATION:
		case turboRegDialog.AFFINE: {
			affineTransform(matrix);
			break;
		}
		case turboRegDialog.BILINEAR: {
			bilinearTransform(matrix);
			break;
		}
	}
} /* end doBatchFinalTransform */

/*********************************************************************
 Compute the final image.
 ********************************************************************/
public ImagePlus doFinalTransform (
	final int width,
	final int height
) {
	if (accelerated) {
		inImg = sourceImg.getImage();
	}
	else {
		inImg = sourceImg.getCoefficient();
	}
	inMsk = sourceMsk.getMask();
	inNx = sourceImg.getWidth();
	inNy = sourceImg.getHeight();
	twiceInNx = 2 * inNx;
	twiceInNy = 2 * inNy;
	final ImageStack is = new ImageStack(width, height);
	final FloatProcessor dataFp = new FloatProcessor(width, height);
	is.addSlice("Data", dataFp);
	final FloatProcessor maskFp = new FloatProcessor(width, height);
	is.addSlice("Mask", maskFp);
	final ImagePlus imp = new ImagePlus("Output", is);
	imp.setSlice(1);
	outImg = (float[])dataFp.getPixels();
	imp.setSlice(2);
	final float[] outMsk = (float[])maskFp.getPixels();
	outNx = imp.getWidth();
	outNy = imp.getHeight();
	final double[][] matrix = getTransformationMatrix(targetPoint, sourcePoint);
	switch (transformation) {
		case turboRegDialog.TRANSLATION: {
			translationTransform(matrix, outMsk);
			break;
		}
		case turboRegDialog.RIGID_BODY:
		case turboRegDialog.SCALED_ROTATION:
		case turboRegDialog.AFFINE: {
			affineTransform(matrix, outMsk);
			break;
		}
		case turboRegDialog.BILINEAR: {
			bilinearTransform(matrix, outMsk);
			break;
		}
	}
	imp.setSlice(1);
	imp.getProcessor().resetMinAndMax();
	if (interactive) {
		imp.show();
		imp.updateAndDraw();
	}
	return(imp);
} /* end doFinalTransform */

/*********************************************************************
 Compute the final image.
 ********************************************************************/
public float[] doFinalTransform (
	final turboRegImage sourceImg,
	final turboRegPointHandler sourcePh,
	final turboRegImage targetImg,
	final turboRegPointHandler targetPh,
	final int transformation,
	final boolean accelerated
) {
	this.sourceImg = sourceImg;
	this.targetImg = targetImg;
	this.sourcePh = sourcePh;
	this.targetPh = targetPh;
	this.transformation = transformation;
	this.accelerated = accelerated;
	sourcePoint = sourcePh.getPoints();
	targetPoint = targetPh.getPoints();
	if (accelerated) {
		inImg = sourceImg.getImage();
	}
	else {
		inImg = sourceImg.getCoefficient();
	}
	inNx = sourceImg.getWidth();
	inNy = sourceImg.getHeight();
	twiceInNx = 2 * inNx;
	twiceInNy = 2 * inNy;
	outNx = targetImg.getWidth();
	outNy = targetImg.getHeight();
	outImg = new float[outNx * outNy];
	final double[][] matrix = getTransformationMatrix(targetPoint, sourcePoint);
	switch (transformation) {
		case turboRegDialog.TRANSLATION: {
			translationTransform(matrix);
			break;
		}
		case turboRegDialog.RIGID_BODY:
		case turboRegDialog.SCALED_ROTATION:
		case turboRegDialog.AFFINE: {
			affineTransform(matrix);
			break;
		}
		case turboRegDialog.BILINEAR: {
			bilinearTransform(matrix);
			break;
		}
	}
	return(outImg);
} /* end doFinalTransform */

/*********************************************************************
 Refine the landmarks.
 ********************************************************************/
public void doRegistration (
) {
	Stack sourceImgPyramid;
	Stack sourceMskPyramid;
	Stack targetImgPyramid;
	Stack targetMskPyramid;
	if (sourceMsk == null) {
		sourceImgPyramid = sourceImg.getPyramid();
		sourceMskPyramid = null;
		targetImgPyramid = (Stack)targetImg.getPyramid().clone();
		targetMskPyramid = (Stack)targetMsk.getPyramid().clone();
	}
	else {
		sourceImgPyramid = sourceImg.getPyramid();
		sourceMskPyramid = sourceMsk.getPyramid();
		targetImgPyramid = targetImg.getPyramid();
		targetMskPyramid = targetMsk.getPyramid();
	}
	pyramidDepth = targetImg.getPyramidDepth();
	iterationPower = (int)Math.pow(
		(double)ITERATION_PROGRESSION, (double)pyramidDepth);
	turboRegProgressBar.addWorkload(
		pyramidDepth * maxIterations * iterationPower
		/ ITERATION_PROGRESSION
		- (iterationPower - 1) / (ITERATION_PROGRESSION - 1));
	iterationCost = 1;
	scaleBottomDownLandmarks();
	while (!targetImgPyramid.isEmpty()) {
		iterationPower /= ITERATION_PROGRESSION;
		if (transformation == turboRegDialog.BILINEAR) {
			inNx = ((Integer)sourceImgPyramid.pop()).intValue();
			inNy = ((Integer)sourceImgPyramid.pop()).intValue();
			inImg = (float[])sourceImgPyramid.pop();
			if (sourceMskPyramid == null) {
				inMsk = null;
			}
			else {
				inMsk = (float[])sourceMskPyramid.pop();
			}
			outNx = ((Integer)targetImgPyramid.pop()).intValue();
			outNy = ((Integer)targetImgPyramid.pop()).intValue();
			outImg = (float[])targetImgPyramid.pop();
			outMsk = (float[])targetMskPyramid.pop();
		}
		else {
			inNx = ((Integer)targetImgPyramid.pop()).intValue();
			inNy = ((Integer)targetImgPyramid.pop()).intValue();
			inImg = (float[])targetImgPyramid.pop();
			inMsk = (float[])targetMskPyramid.pop();
			outNx = ((Integer)sourceImgPyramid.pop()).intValue();
			outNy = ((Integer)sourceImgPyramid.pop()).intValue();
			outImg = (float[])sourceImgPyramid.pop();
			xGradient = (float[])sourceImgPyramid.pop();
			yGradient = (float[])sourceImgPyramid.pop();
			if (sourceMskPyramid == null) {
				outMsk = null;
			}
			else {
				outMsk = (float[])sourceMskPyramid.pop();
			}
		}
		twiceInNx = 2 * inNx;
		twiceInNy = 2 * inNy;
		switch (transformation) {
			case turboRegDialog.TRANSLATION: {
				targetJacobian = 1.0;
				inverseMarquardtLevenbergOptimization(
					iterationPower * maxIterations - 1);
				break;
			}
			case turboRegDialog.RIGID_BODY: {
				inverseMarquardtLevenbergRigidBodyOptimization(
					iterationPower * maxIterations - 1);
				break;
			}
			case turboRegDialog.SCALED_ROTATION: {
				targetJacobian = (targetPoint[0][0] - targetPoint[1][0])
					* (targetPoint[0][0] - targetPoint[1][0])
					+ (targetPoint[0][1] - targetPoint[1][1])
					* (targetPoint[0][1] - targetPoint[1][1]);
				inverseMarquardtLevenbergOptimization(
					iterationPower * maxIterations - 1);
				break;
			}
			case turboRegDialog.AFFINE: {
				targetJacobian = (targetPoint[1][0] - targetPoint[2][0])
					* targetPoint[0][1]
					+ (targetPoint[2][0] - targetPoint[0][0])
					* targetPoint[1][1]
					+ (targetPoint[0][0] - targetPoint[1][0])
					* targetPoint[2][1];
				inverseMarquardtLevenbergOptimization(
					iterationPower * maxIterations - 1);
				break;
			}
			case turboRegDialog.BILINEAR: {
				MarquardtLevenbergOptimization(
					iterationPower * maxIterations - 1);
				break;
			}
		}
		scaleUpLandmarks();
		sourcePh.setPoints(sourcePoint);
		iterationCost *= ITERATION_PROGRESSION;
	}
	iterationPower /= ITERATION_PROGRESSION;
	if (transformation == turboRegDialog.BILINEAR) {
		inNx = sourceImg.getWidth();
		inNy = sourceImg.getHeight();
		inImg = sourceImg.getCoefficient();
		if (sourceMsk == null) {
			inMsk = null;
		}
		else {
			inMsk = sourceMsk.getMask();
		}
		outNx = targetImg.getWidth();
		outNy = targetImg.getHeight();
		outImg = targetImg.getImage();
		outMsk = targetMsk.getMask();
	}
	else {
		inNx = targetImg.getWidth();
		inNy = targetImg.getHeight();
		inImg = targetImg.getCoefficient();
		inMsk = targetMsk.getMask();
		outNx = sourceImg.getWidth();
		outNy = sourceImg.getHeight();
		outImg = sourceImg.getImage();
		xGradient = sourceImg.getXGradient();
		yGradient = sourceImg.getYGradient();
		if (sourceMsk == null) {
			outMsk = null;
		}
		else {
			outMsk = sourceMsk.getMask();
		}
	}
	twiceInNx = 2 * inNx;
	twiceInNy = 2 * inNy;
	if (accelerated) {
		turboRegProgressBar.skipProgressBar(
			iterationCost * (maxIterations - 1));
	}
	else {
		switch (transformation) {
			case turboRegDialog.RIGID_BODY: {
				inverseMarquardtLevenbergRigidBodyOptimization(
					maxIterations - 1);
				break;
			}
			case turboRegDialog.TRANSLATION:
			case turboRegDialog.SCALED_ROTATION:
			case turboRegDialog.AFFINE: {
				inverseMarquardtLevenbergOptimization(maxIterations - 1);
				break;
			}
			case turboRegDialog.BILINEAR: {
				MarquardtLevenbergOptimization(maxIterations - 1);
				break;
			}
		}
	}
	sourcePh.setPoints(sourcePoint);
	iterationPower = (int)Math.pow(
		(double)ITERATION_PROGRESSION, (double)pyramidDepth);
	turboRegProgressBar.workloadDone(
		pyramidDepth * maxIterations * iterationPower / ITERATION_PROGRESSION
		- (iterationPower - 1) / (ITERATION_PROGRESSION - 1));
} /* end doRegistration */

/*********************************************************************
 Save the current landmarks into a text file and return the path
 and name of the file. Rigid format.
 @see turboRegDialog#loadLandmarks()
 ********************************************************************/
public String saveTransformation (
	String filename
) {
	inNx = sourceImg.getWidth();
	inNy = sourceImg.getHeight();
	outNx = targetImg.getWidth();
	outNy = targetImg.getHeight();
	String path = "";
	if (filename == null) {
		final Frame f = new Frame();
		final FileDialog fd = new FileDialog(
			f, "Save landmarks", FileDialog.SAVE);
		filename = "landmarks.txt";
		fd.setFile(filename);
		fd.setVisible(true);
		path = fd.getDirectory();
		filename = fd.getFile();
		if ((path == null) || (filename == null)) {
			return("");
		}
	}
	try {
		final FileWriter fw = new FileWriter(path + filename);
		fw.write("Transformation\n");
		switch (transformation) {
			case turboRegDialog.TRANSLATION: {
				fw.write("TRANSLATION\n");
				break;
			}
			case turboRegDialog.RIGID_BODY: {
				fw.write("RIGID_BODY\n");
				break;
			}
			case turboRegDialog.SCALED_ROTATION: {
				fw.write("SCALED_ROTATION\n");
				break;
			}
			case turboRegDialog.AFFINE: {
				fw.write("AFFINE\n");
				break;
			}
			case turboRegDialog.BILINEAR: {
				fw.write("BILINEAR\n");
				break;
			}
		}
		fw.write("\n");
		fw.write("Source size\n");
		fw.write(inNx + "\t" + inNy + "\n");
		fw.write("\n");
		fw.write("Target size\n");
		fw.write(outNx + "\t" + outNy + "\n");
		fw.write("\n");
		fw.write("Refined source landmarks\n");
		if (transformation == turboRegDialog.RIGID_BODY) {
			for (int i = 0; (i < transformation); i++) {
				fw.write(sourcePoint[i][0] + "\t" + sourcePoint[i][1] + "\n");
			}
		}
		else {
			for (int i = 0; (i < (transformation / 2)); i++) {
				fw.write(sourcePoint[i][0] + "\t" + sourcePoint[i][1] + "\n");
			}
		}
		fw.write("\n");
		fw.write("Target landmarks\n");
		if (transformation == turboRegDialog.RIGID_BODY) {
			for (int i = 0; (i < transformation); i++) {
				fw.write(targetPoint[i][0] + "\t" + targetPoint[i][1] + "\n");
			}
		}
		else {
			for (int i = 0; (i < (transformation / 2)); i++) {
				fw.write(targetPoint[i][0] + "\t" + targetPoint[i][1] + "\n");
			}
		}
		fw.close();
	} catch (IOException e) {
		IJ.log(
			"IOException exception " + e.getMessage());
	} catch (SecurityException e) {
		IJ.log(
			"Security exception " + e.getMessage());
	}
	return(path + filename);
} /* end saveTransformation */

/*********************************************************************
 Keep a local copy of most everything. Select among the pre-stored
 constants.
 @param targetImg Target image pyramid.
 @param targetMsk Target mask pyramid.
 @param sourceImg Source image pyramid.
 @param sourceMsk Source mask pyramid.
 @param targetPh Target <code>turboRegPointHandler</code> object.
 @param sourcePh Source <code>turboRegPointHandler</code> object.
 @param transformation Transformation code.
 @param accelerated Trade-off between speed and accuracy.
 @param interactive Shows or hides the resulting image.
 ********************************************************************/
public turboRegTransform (
	final turboRegImage sourceImg,
	final turboRegMask sourceMsk,
	final turboRegPointHandler sourcePh,
	final turboRegImage targetImg,
	final turboRegMask targetMsk,
	final turboRegPointHandler targetPh,
	final int transformation,
	final boolean accelerated,
	final boolean interactive
) {
	this.sourceImg = sourceImg;
	this.sourceMsk = sourceMsk;
	this.sourcePh = sourcePh;
	this.targetImg = targetImg;
	this.targetMsk = targetMsk;
	this.targetPh = targetPh;
	this.transformation = transformation;
	this.accelerated = accelerated;
	this.interactive = interactive;
	sourcePoint = sourcePh.getPoints();
	targetPoint = targetPh.getPoints();
	if (accelerated) {
		pixelPrecision = PIXEL_LOW_PRECISION;
		maxIterations = FEW_ITERATIONS;
	}
	else {
		pixelPrecision = PIXEL_HIGH_PRECISION;
		maxIterations = MANY_ITERATIONS;
	}
} /* end turboRegTransform */

/*....................................................................
	Private methods
....................................................................*/

/*------------------------------------------------------------------*/
private void affineTransform (
	final double[][] matrix
) {
	double yx;
	double yy;
	double x0;
	double y0;
	int xMsk;
	int yMsk;
	int k = 0;
	turboRegProgressBar.addWorkload(outNy);
	yx = matrix[0][0];
	yy = matrix[1][0];
	for (int v = 0; (v < outNy); v++) {
		x0 = yx;
		y0 = yy;
		for (int u = 0; (u < outNx); u++) {
			x = x0;
			y = y0;
			xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
			yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
			if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
				xMsk += yMsk * inNx;
				if (accelerated) {
					outImg[k++] = inImg[xMsk];
				}
				else {
					xIndexes();
					yIndexes();
					x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
					y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
					xWeights();
					yWeights();
					outImg[k++] = (float)interpolate();
				}
			}
			else {
				outImg[k++] = 0.0F;
			}
			x0 += matrix[0][1];
			y0 += matrix[1][1];
		}
		yx += matrix[0][2];
		yy += matrix[1][2];
		turboRegProgressBar.stepProgressBar();
	}
	turboRegProgressBar.workloadDone(outNy);
} /* affineTransform */

/*------------------------------------------------------------------*/
private void affineTransform (
	final double[][] matrix,
	final float[] outMsk
) {
	double yx;
	double yy;
	double x0;
	double y0;
	int xMsk;
	int yMsk;
	int k = 0;
	turboRegProgressBar.addWorkload(outNy);
	yx = matrix[0][0];
	yy = matrix[1][0];
	for (int v = 0; (v < outNy); v++) {
		x0 = yx;
		y0 = yy;
		for (int u = 0; (u < outNx); u++) {
			x = x0;
			y = y0;
			xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
			yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
			if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
				xMsk += yMsk * inNx;
				if (accelerated) {
					outImg[k] = inImg[xMsk];
				}
				else {
					xIndexes();
					yIndexes();
					x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
					y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
					xWeights();
					yWeights();
					outImg[k] = (float)interpolate();
				}
				outMsk[k++] = inMsk[xMsk];
			}
			else {
				outImg[k] = 0.0F;
				outMsk[k++] = 0.0F;
			}
			x0 += matrix[0][1];
			y0 += matrix[1][1];
		}
		yx += matrix[0][2];
		yy += matrix[1][2];
		turboRegProgressBar.stepProgressBar();
	}
	turboRegProgressBar.workloadDone(outNy);
} /* affineTransform */

/*------------------------------------------------------------------*/
private void bilinearTransform (
	final double[][] matrix
) {
	double yx;
	double yy;
	double yxy;
	double yyy;
	double x0;
	double y0;
	int xMsk;
	int yMsk;
	int k = 0;
	turboRegProgressBar.addWorkload(outNy);
	yx = matrix[0][0];
	yy = matrix[1][0];
	yxy = 0.0;
	yyy = 0.0;
	for (int v = 0; (v < outNy); v++) {
		x0 = yx;
		y0 = yy;
		for (int u = 0; (u < outNx); u++) {
			x = x0;
			y = y0;
			xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
			yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
			if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
				xMsk += yMsk * inNx;
				if (accelerated) {
					outImg[k++] = inImg[xMsk];
				}
				else {
					xIndexes();
					yIndexes();
					x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
					y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
					xWeights();
					yWeights();
					outImg[k++] = (float)interpolate();
				}
			}
			else {
				outImg[k++] = 0.0F;
			}
			x0 += matrix[0][1] + yxy;
			y0 += matrix[1][1] + yyy;
		}
		yx += matrix[0][2];
		yy += matrix[1][2];
		yxy += matrix[0][3];
		yyy += matrix[1][3];
		turboRegProgressBar.stepProgressBar();
	}
	turboRegProgressBar.workloadDone(outNy);
} /* bilinearTransform */

/*------------------------------------------------------------------*/
private void bilinearTransform (
	final double[][] matrix,
	final float[] outMsk
) {
	double yx;
	double yy;
	double yxy;
	double yyy;
	double x0;
	double y0;
	int xMsk;
	int yMsk;
	int k = 0;
	turboRegProgressBar.addWorkload(outNy);
	yx = matrix[0][0];
	yy = matrix[1][0];
	yxy = 0.0;
	yyy = 0.0;
	for (int v = 0; (v < outNy); v++) {
		x0 = yx;
		y0 = yy;
		for (int u = 0; (u < outNx); u++) {
			x = x0;
			y = y0;
			xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
			yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
			if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
				xMsk += yMsk * inNx;
				if (accelerated) {
					outImg[k] = inImg[xMsk];
				}
				else {
					xIndexes();
					yIndexes();
					x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
					y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
					xWeights();
					yWeights();
					outImg[k] = (float)interpolate();
				}
				outMsk[k++] = inMsk[xMsk];
			}
			else {
				outImg[k] = 0.0F;
				outMsk[k++] = 0.0F;
			}
			x0 += matrix[0][1] + yxy;
			y0 += matrix[1][1] + yyy;
		}
		yx += matrix[0][2];
		yy += matrix[1][2];
		yxy += matrix[0][3];
		yyy += matrix[1][3];
		turboRegProgressBar.stepProgressBar();
	}
	turboRegProgressBar.workloadDone(outNy);
} /* bilinearTransform */

/*------------------------------------------------------------------*/
private void computeBilinearGradientConstants (
) {
	final double u1 = targetPoint[0][0];
	final double u2 = targetPoint[1][0];
	final double u3 = targetPoint[2][0];
	final double u4 = targetPoint[3][0];
	final double v1 = targetPoint[0][1];
	final double v2 = targetPoint[1][1];
	final double v3 = targetPoint[2][1];
	final double v4 = targetPoint[3][1];
	final double u12 = u1 - u2;
	final double u13 = u1 - u3;
	final double u14 = u1 - u4;
	final double u23 = u2 - u3;
	final double u24 = u2 - u4;
	final double u34 = u3 - u4;
	final double v12 = v1 - v2;
	final double v13 = v1 - v3;
	final double v14 = v1 - v4;
	final double v23 = v2 - v3;
	final double v24 = v2 - v4;
	final double v34 = v3 - v4;
	final double uv12 = u1 * u2 * v12;
	final double uv13 = u1 * u3 * v13;
	final double uv14 = u1 * u4 * v14;
	final double uv23 = u2 * u3 * v23;
	final double uv24 = u2 * u4 * v24;
	final double uv34 = u3 * u4 * v34;
	final double det = uv12 * v34 - uv13 * v24 + uv14 * v23 + uv23 * v14
		- uv24 * v13 + uv34 * v12;
	c0 = (-uv34 * v2 + uv24 * v3 - uv23 * v4) / det;
	c0u = (u3 * v3 * v24 - u2 * v2 * v34 - u4 * v4 * v23) / det;
	c0v = (uv23 - uv24 + uv34) / det;
	c0uv = (u4 * v23 - u3 * v24 + u2 * v34) / det;
	c1 = (uv34 * v1 - uv14 * v3 + uv13 * v4) / det;
	c1u = (-u3 * v3 * v14 + u1 * v1 * v34 + u4 * v4 * v13) / det;
	c1v = (-uv13 + uv14 - uv34) / det;
	c1uv = (-u4 * v13 + u3 * v14 - u1 * v34) / det;
	c2 = (-uv24 * v1 + uv14 * v2 - uv12 * v4) / det;
	c2u = (u2 * v2 * v14 - u1 * v1 * v24 - u4 * v4 * v12) / det;
	c2v = (uv12 - uv14 + uv24) / det;
	c2uv = (u4 * v12 - u2 * v14 + u1 * v24) / det;
	c3 = (uv23 * v1 - uv13 * v2 + uv12 * v3) / det;
	c3u = (-u2 * v2 * v13 + u1 * v1 * v23 + u3 * v3 * v12) / det;
	c3v = (-uv12 + uv13 - uv23) / det;
	c3uv = (-u3 * v1 + u2 * v13 + u3 * v2 - u1 * v23) / det;
} /* end computeBilinearGradientConstants */

/*------------------------------------------------------------------*/
private double getAffineMeanSquares (
	final double[][] sourcePoint,
	final double[][] matrix
) {
	final double u1 = sourcePoint[0][0];
	final double u2 = sourcePoint[1][0];
	final double u3 = sourcePoint[2][0];
	final double v1 = sourcePoint[0][1];
	final double v2 = sourcePoint[1][1];
	final double v3 = sourcePoint[2][1];
	final double uv32 = u3 * v2 - u2 * v3;
	final double uv21 = u2 * v1 - u1 * v2;
	final double uv13 = u1 * v3 - u3 * v1;
	final double det = uv32 + uv21 + uv13;
	double yx;
	double yy;
	double x0;
	double y0;
	double difference;
	double meanSquares = 0.0;
	long area = 0L;
	int xMsk;
	int yMsk;
	int k = 0;
	if (outMsk == null) {
		yx = matrix[0][0];
		yy = matrix[1][0];
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				x = x0;
				y = y0;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)
					&& (0 <= yMsk) && (yMsk < inNy)) {
					if (inMsk[yMsk * inNx + xMsk] != 0.0F) {
						area++;
						xIndexes();
						yIndexes();
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xWeights();
						yWeights();
						difference = (double)outImg[k] - interpolate();
						meanSquares += difference * difference;
					}
				}
				x0 += matrix[0][1];
				y0 += matrix[1][1];
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
		}
	}
	else {
		yx = matrix[0][0];
		yy = matrix[1][0];
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				x = x0;
				y = y0;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)
					&& (0 <= yMsk) && (yMsk < inNy)) {
					if ((outMsk[k] * inMsk[yMsk * inNx + xMsk]) != 0.0F) {
						area++;
						xIndexes();
						yIndexes();
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xWeights();
						yWeights();
						difference = (double)outImg[k] - interpolate();
						meanSquares += difference * difference;
					}
				}
				x0 += matrix[0][1];
				y0 += matrix[1][1];
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
		}
	}
	return(meanSquares / ((double)area * Math.abs(det / targetJacobian)));
} /* getAffineMeanSquares */

/*------------------------------------------------------------------*/
private double getAffineMeanSquares (
	final double[][] sourcePoint,
	final double[][] matrix,
	final double[] gradient
) {
	final double u1 = sourcePoint[0][0];
	final double u2 = sourcePoint[1][0];
	final double u3 = sourcePoint[2][0];
	final double v1 = sourcePoint[0][1];
	final double v2 = sourcePoint[1][1];
	final double v3 = sourcePoint[2][1];
	double uv32 = u3 * v2 - u2 * v3;
	double uv21 = u2 * v1 - u1 * v2;
	double uv13 = u1 * v3 - u3 * v1;
	final double det = uv32 + uv21 + uv13;
	final double u12 = (u1 - u2) /det;
	final double u23 = (u2 - u3) /det;
	final double u31 = (u3 - u1) /det;
	final double v12 = (v1 - v2) /det;
	final double v23 = (v2 - v3) /det;
	final double v31 = (v3 - v1) /det;
	double yx;
	double yy;
	double x0;
	double y0;
	double difference;
	double meanSquares = 0.0;
	double g0;
	double g1;
	double g2;
	double dx0;
	double dx1;
	double dx2;
	double dy0;
	double dy1;
	double dy2;
	long area = 0L;
	int xMsk;
	int yMsk;
	int k = 0;
	uv32 /= det;
	uv21 /= det;
	uv13 /= det;
	for (int i = 0; (i < transformation); i++) {
		gradient[i] = 0.0;
	}
	if (outMsk == null) {
		yx = matrix[0][0];
		yy = matrix[1][0];
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				x = x0;
				y = y0;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)
					&& (0 <= yMsk) && (yMsk < inNy)) {
					if (inMsk[yMsk * inNx + xMsk] != 0.0F) {
						area++;
						xIndexes();
						yIndexes();
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xWeights();
						yWeights();
						difference = (double)outImg[k] - interpolate();
						meanSquares += difference * difference;
						g0 = u23 * (double)v - v23 * (double)u + uv32;
						g1 = u31 * (double)v - v31 * (double)u + uv13;
						g2 = u12 * (double)v - v12 * (double)u + uv21;
						dx0 = xGradient[k] * g0;
						dy0 = yGradient[k] * g0;
						dx1 = xGradient[k] * g1;
						dy1 = yGradient[k] * g1;
						dx2 = xGradient[k] * g2;
						dy2 = yGradient[k] * g2;
						gradient[0] += difference * dx0;
						gradient[1] += difference * dy0;
						gradient[2] += difference * dx1;
						gradient[3] += difference * dy1;
						gradient[4] += difference * dx2;
						gradient[5] += difference * dy2;
					}
				}
				x0 += matrix[0][1];
				y0 += matrix[1][1];
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
		}
	}
	else {
		yx = matrix[0][0];
		yy = matrix[1][0];
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				x = x0;
				y = y0;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)
					&& (0 <= yMsk) && (yMsk < inNy)) {
					if ((outMsk[k] * inMsk[yMsk * inNx + xMsk]) != 0.0F) {
						area++;
						xIndexes();
						yIndexes();
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xWeights();
						yWeights();
						difference = (double)outImg[k] - interpolate();
						meanSquares += difference * difference;
						g0 = u23 * (double)v - v23 * (double)u + uv32;
						g1 = u31 * (double)v - v31 * (double)u + uv13;
						g2 = u12 * (double)v - v12 * (double)u + uv21;
						dx0 = xGradient[k] * g0;
						dy0 = yGradient[k] * g0;
						dx1 = xGradient[k] * g1;
						dy1 = yGradient[k] * g1;
						dx2 = xGradient[k] * g2;
						dy2 = yGradient[k] * g2;
						gradient[0] += difference * dx0;
						gradient[1] += difference * dy0;
						gradient[2] += difference * dx1;
						gradient[3] += difference * dy1;
						gradient[4] += difference * dx2;
						gradient[5] += difference * dy2;
					}
				}
				x0 += matrix[0][1];
				y0 += matrix[1][1];
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
		}
	}
	return(meanSquares / ((double)area * Math.abs(det / targetJacobian)));
} /* getAffineMeanSquares */

/*------------------------------------------------------------------*/
private double getAffineMeanSquares (
	final double[][] sourcePoint,
	final double[][] matrix,
	final double[][] hessian,
	final double[] gradient
) {
	final double u1 = sourcePoint[0][0];
	final double u2 = sourcePoint[1][0];
	final double u3 = sourcePoint[2][0];
	final double v1 = sourcePoint[0][1];
	final double v2 = sourcePoint[1][1];
	final double v3 = sourcePoint[2][1];
	double uv32 = u3 * v2 - u2 * v3;
	double uv21 = u2 * v1 - u1 * v2;
	double uv13 = u1 * v3 - u3 * v1;
	final double det = uv32 + uv21 + uv13;
	final double u12 = (u1 - u2) /det;
	final double u23 = (u2 - u3) /det;
	final double u31 = (u3 - u1) /det;
	final double v12 = (v1 - v2) /det;
	final double v23 = (v2 - v3) /det;
	final double v31 = (v3 - v1) /det;
	double yx;
	double yy;
	double x0;
	double y0;
	double difference;
	double meanSquares = 0.0;
	double g0;
	double g1;
	double g2;
	double dx0;
	double dx1;
	double dx2;
	double dy0;
	double dy1;
	double dy2;
	long area = 0L;
	int xMsk;
	int yMsk;
	int k = 0;
	uv32 /= det;
	uv21 /= det;
	uv13 /= det;
	for (int i = 0; (i < transformation); i++) {
		gradient[i] = 0.0;
		for (int j = 0; (j < transformation); j++) {
			hessian[i][j] = 0.0;
		}
	}
	if (outMsk == null) {
		yx = matrix[0][0];
		yy = matrix[1][0];
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				x = x0;
				y = y0;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)
					&& (0 <= yMsk) && (yMsk < inNy)) {
					if (inMsk[yMsk * inNx + xMsk] != 0.0F) {
						area++;
						xIndexes();
						yIndexes();
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xWeights();
						yWeights();
						difference = (double)outImg[k] - interpolate();
						meanSquares += difference * difference;
						g0 = u23 * (double)v - v23 * (double)u + uv32;
						g1 = u31 * (double)v - v31 * (double)u + uv13;
						g2 = u12 * (double)v - v12 * (double)u + uv21;
						dx0 = xGradient[k] * g0;
						dy0 = yGradient[k] * g0;
						dx1 = xGradient[k] * g1;
						dy1 = yGradient[k] * g1;
						dx2 = xGradient[k] * g2;
						dy2 = yGradient[k] * g2;
						gradient[0] += difference * dx0;
						gradient[1] += difference * dy0;
						gradient[2] += difference * dx1;
						gradient[3] += difference * dy1;
						gradient[4] += difference * dx2;
						gradient[5] += difference * dy2;
						hessian[0][0] += dx0 * dx0;
						hessian[0][1] += dx0 * dy0;
						hessian[0][2] += dx0 * dx1;
						hessian[0][3] += dx0 * dy1;
						hessian[0][4] += dx0 * dx2;
						hessian[0][5] += dx0 * dy2;
						hessian[1][1] += dy0 * dy0;
						hessian[1][2] += dy0 * dx1;
						hessian[1][3] += dy0 * dy1;
						hessian[1][4] += dy0 * dx2;
						hessian[1][5] += dy0 * dy2;
						hessian[2][2] += dx1 * dx1;
						hessian[2][3] += dx1 * dy1;
						hessian[2][4] += dx1 * dx2;
						hessian[2][5] += dx1 * dy2;
						hessian[3][3] += dy1 * dy1;
						hessian[3][4] += dy1 * dx2;
						hessian[3][5] += dy1 * dy2;
						hessian[4][4] += dx2 * dx2;
						hessian[4][5] += dx2 * dy2;
						hessian[5][5] += dy2 * dy2;
					}
				}
				x0 += matrix[0][1];
				y0 += matrix[1][1];
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
		}
	}
	else {
		yx = matrix[0][0];
		yy = matrix[1][0];
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				x = x0;
				y = y0;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)
					&& (0 <= yMsk) && (yMsk < inNy)) {
					if ((outMsk[k] * inMsk[yMsk * inNx + xMsk]) != 0.0F) {
						area++;
						xIndexes();
						yIndexes();
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xWeights();
						yWeights();
						difference = (double)outImg[k] - interpolate();
						meanSquares += difference * difference;
						g0 = u23 * (double)v - v23 * (double)u + uv32;
						g1 = u31 * (double)v - v31 * (double)u + uv13;
						g2 = u12 * (double)v - v12 * (double)u + uv21;
						dx0 = xGradient[k] * g0;
						dy0 = yGradient[k] * g0;
						dx1 = xGradient[k] * g1;
						dy1 = yGradient[k] * g1;
						dx2 = xGradient[k] * g2;
						dy2 = yGradient[k] * g2;
						gradient[0] += difference * dx0;
						gradient[1] += difference * dy0;
						gradient[2] += difference * dx1;
						gradient[3] += difference * dy1;
						gradient[4] += difference * dx2;
						gradient[5] += difference * dy2;
						hessian[0][0] += dx0 * dx0;
						hessian[0][1] += dx0 * dy0;
						hessian[0][2] += dx0 * dx1;
						hessian[0][3] += dx0 * dy1;
						hessian[0][4] += dx0 * dx2;
						hessian[0][5] += dx0 * dy2;
						hessian[1][1] += dy0 * dy0;
						hessian[1][2] += dy0 * dx1;
						hessian[1][3] += dy0 * dy1;
						hessian[1][4] += dy0 * dx2;
						hessian[1][5] += dy0 * dy2;
						hessian[2][2] += dx1 * dx1;
						hessian[2][3] += dx1 * dy1;
						hessian[2][4] += dx1 * dx2;
						hessian[2][5] += dx1 * dy2;
						hessian[3][3] += dy1 * dy1;
						hessian[3][4] += dy1 * dx2;
						hessian[3][5] += dy1 * dy2;
						hessian[4][4] += dx2 * dx2;
						hessian[4][5] += dx2 * dy2;
						hessian[5][5] += dy2 * dy2;
					}
				}
				x0 += matrix[0][1];
				y0 += matrix[1][1];
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
		}
	}
	for (int i = 1; (i < transformation); i++) {
		for (int j = 0; (j < i); j++) {
			hessian[i][j] = hessian[j][i];
		}
	}
	return(meanSquares / ((double)area * Math.abs(det / targetJacobian)));
} /* getAffineMeanSquares */

/*------------------------------------------------------------------*/
private double getBilinearMeanSquares (
	final double[][] matrix
) {
	double yx;
	double yy;
	double yxy;
	double yyy;
	double x0;
	double y0;
	double difference;
	double meanSquares = 0.0;
	long area = 0L;
	int xMsk;
	int yMsk;
	int k = 0;
	if (inMsk == null) {
		yx = matrix[0][0];
		yy = matrix[1][0];
		yxy = 0.0;
		yyy = 0.0;
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				if (outMsk[k] != 0.0F) {
					x = x0;
					y = y0;
					xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
					yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
					if ((0 <= xMsk) && (xMsk < inNx)
						&& (0 <= yMsk) && (yMsk < inNy)) {
						xIndexes();
						yIndexes();
						area++;
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xWeights();
						yWeights();
						difference = interpolate() - (double)outImg[k];
						meanSquares += difference * difference;
					}
				}
				x0 += matrix[0][1] + yxy;
				y0 += matrix[1][1] + yyy;
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
			yxy += matrix[0][3];
			yyy += matrix[1][3];
		}
	}
	else {
		yx = matrix[0][0];
		yy = matrix[1][0];
		yxy = 0.0;
		yyy = 0.0;
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				x = x0;
				y = y0;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)
					&& (0 <= yMsk) && (yMsk < inNy)) {
					xMsk += yMsk * inNx;
					if ((outMsk[k] * inMsk[xMsk]) != 0.0F) {
						xIndexes();
						yIndexes();
						area++;
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xWeights();
						yWeights();
						difference = interpolate() - (double)outImg[k];
						meanSquares += difference * difference;
					}
				}
				x0 += matrix[0][1] + yxy;
				y0 += matrix[1][1] + yyy;
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
			yxy += matrix[0][3];
			yyy += matrix[1][3];
		}
	}
	return(meanSquares / (double)area);
} /* getBilinearMeanSquares */

/*------------------------------------------------------------------*/
private double getBilinearMeanSquares (
	final double[][] matrix,
	final double[][] hessian,
	final double[] gradient
) {
	double yx;
	double yy;
	double yxy;
	double yyy;
	double x0;
	double y0;
	double uv;
	double xGradient;
	double yGradient;
	double difference;
	double meanSquares = 0.0;
	double g0;
	double g1;
	double g2;
	double g3;
	double dx0;
	double dx1;
	double dx2;
	double dx3;
	double dy0;
	double dy1;
	double dy2;
	double dy3;
	long area = 0L;
	int xMsk;
	int yMsk;
	int k = 0;
	computeBilinearGradientConstants();
	for (int i = 0; (i < transformation); i++) {
		gradient[i] = 0.0;
		for (int j = 0; (j < transformation); j++) {
			hessian[i][j] = 0.0;
		}
	}
	if (inMsk == null) {
		yx = matrix[0][0];
		yy = matrix[1][0];
		yxy = 0.0;
		yyy = 0.0;
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				if (outMsk[k] != 0.0F) {
					x = x0;
					y = y0;
					xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
					yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
					if ((0 <= xMsk) && (xMsk < inNx)
						&& (0 <= yMsk) && (yMsk < inNy)) {
						area++;
						xIndexes();
						yIndexes();
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xDxWeights();
						yDyWeights();
						difference = interpolate() - (double)outImg[k];
						meanSquares += difference * difference;
						xGradient = interpolateDx();
						yGradient = interpolateDy();
						uv = (double)u * (double)v;
						g0 = c0uv * uv + c0u * (double)u + c0v * (double)v + c0;
						g1 = c1uv * uv + c1u * (double)u + c1v * (double)v + c1;
						g2 = c2uv * uv + c2u * (double)u + c2v * (double)v + c2;
						g3 = c3uv * uv + c3u * (double)u + c3v * (double)v + c3;
						dx0 = xGradient * g0;
						dy0 = yGradient * g0;
						dx1 = xGradient * g1;
						dy1 = yGradient * g1;
						dx2 = xGradient * g2;
						dy2 = yGradient * g2;
						dx3 = xGradient * g3;
						dy3 = yGradient * g3;
						gradient[0] += difference * dx0;
						gradient[1] += difference * dy0;
						gradient[2] += difference * dx1;
						gradient[3] += difference * dy1;
						gradient[4] += difference * dx2;
						gradient[5] += difference * dy2;
						gradient[6] += difference * dx3;
						gradient[7] += difference * dy3;
						hessian[0][0] += dx0 * dx0;
						hessian[0][1] += dx0 * dy0;
						hessian[0][2] += dx0 * dx1;
						hessian[0][3] += dx0 * dy1;
						hessian[0][4] += dx0 * dx2;
						hessian[0][5] += dx0 * dy2;
						hessian[0][6] += dx0 * dx3;
						hessian[0][7] += dx0 * dy3;
						hessian[1][1] += dy0 * dy0;
						hessian[1][2] += dy0 * dx1;
						hessian[1][3] += dy0 * dy1;
						hessian[1][4] += dy0 * dx2;
						hessian[1][5] += dy0 * dy2;
						hessian[1][6] += dy0 * dx3;
						hessian[1][7] += dy0 * dy3;
						hessian[2][2] += dx1 * dx1;
						hessian[2][3] += dx1 * dy1;
						hessian[2][4] += dx1 * dx2;
						hessian[2][5] += dx1 * dy2;
						hessian[2][6] += dx1 * dx3;
						hessian[2][7] += dx1 * dy3;
						hessian[3][3] += dy1 * dy1;
						hessian[3][4] += dy1 * dx2;
						hessian[3][5] += dy1 * dy2;
						hessian[3][6] += dy1 * dx3;
						hessian[3][7] += dy1 * dy3;
						hessian[4][4] += dx2 * dx2;
						hessian[4][5] += dx2 * dy2;
						hessian[4][6] += dx2 * dx3;
						hessian[4][7] += dx2 * dy3;
						hessian[5][5] += dy2 * dy2;
						hessian[5][6] += dy2 * dx3;
						hessian[5][7] += dy2 * dy3;
						hessian[6][6] += dx3 * dx3;
						hessian[6][7] += dx3 * dy3;
						hessian[7][7] += dy3 * dy3;
					}
				}
				x0 += matrix[0][1] + yxy;
				y0 += matrix[1][1] + yyy;
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
			yxy += matrix[0][3];
			yyy += matrix[1][3];
		}
	}
	else {
		yx = matrix[0][0];
		yy = matrix[1][0];
		yxy = 0.0;
		yyy = 0.0;
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				x = x0;
				y = y0;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)
					&& (0 <= yMsk) && (yMsk < inNy)) {
					xMsk += yMsk * inNx;
					if ((outMsk[k] * inMsk[xMsk]) != 0.0F) {
						area++;
						xIndexes();
						yIndexes();
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xDxWeights();
						yDyWeights();
						difference = interpolate() - (double)outImg[k];
						meanSquares += difference * difference;
						xGradient = interpolateDx();
						yGradient = interpolateDy();
						uv = (double)u * (double)v;
						g0 = c0uv * uv + c0u * (double)u + c0v * (double)v + c0;
						g1 = c1uv * uv + c1u * (double)u + c1v * (double)v + c1;
						g2 = c2uv * uv + c2u * (double)u + c2v * (double)v + c2;
						g3 = c3uv * uv + c3u * (double)u + c3v * (double)v + c3;
						dx0 = xGradient * g0;
						dy0 = yGradient * g0;
						dx1 = xGradient * g1;
						dy1 = yGradient * g1;
						dx2 = xGradient * g2;
						dy2 = yGradient * g2;
						dx3 = xGradient * g3;
						dy3 = yGradient * g3;
						gradient[0] += difference * dx0;
						gradient[1] += difference * dy0;
						gradient[2] += difference * dx1;
						gradient[3] += difference * dy1;
						gradient[4] += difference * dx2;
						gradient[5] += difference * dy2;
						gradient[6] += difference * dx3;
						gradient[7] += difference * dy3;
						hessian[0][0] += dx0 * dx0;
						hessian[0][1] += dx0 * dy0;
						hessian[0][2] += dx0 * dx1;
						hessian[0][3] += dx0 * dy1;
						hessian[0][4] += dx0 * dx2;
						hessian[0][5] += dx0 * dy2;
						hessian[0][6] += dx0 * dx3;
						hessian[0][7] += dx0 * dy3;
						hessian[1][1] += dy0 * dy0;
						hessian[1][2] += dy0 * dx1;
						hessian[1][3] += dy0 * dy1;
						hessian[1][4] += dy0 * dx2;
						hessian[1][5] += dy0 * dy2;
						hessian[1][6] += dy0 * dx3;
						hessian[1][7] += dy0 * dy3;
						hessian[2][2] += dx1 * dx1;
						hessian[2][3] += dx1 * dy1;
						hessian[2][4] += dx1 * dx2;
						hessian[2][5] += dx1 * dy2;
						hessian[2][6] += dx1 * dx3;
						hessian[2][7] += dx1 * dy3;
						hessian[3][3] += dy1 * dy1;
						hessian[3][4] += dy1 * dx2;
						hessian[3][5] += dy1 * dy2;
						hessian[3][6] += dy1 * dx3;
						hessian[3][7] += dy1 * dy3;
						hessian[4][4] += dx2 * dx2;
						hessian[4][5] += dx2 * dy2;
						hessian[4][6] += dx2 * dx3;
						hessian[4][7] += dx2 * dy3;
						hessian[5][5] += dy2 * dy2;
						hessian[5][6] += dy2 * dx3;
						hessian[5][7] += dy2 * dy3;
						hessian[6][6] += dx3 * dx3;
						hessian[6][7] += dx3 * dy3;
						hessian[7][7] += dy3 * dy3;
					}
				}
				x0 += matrix[0][1] + yxy;
				y0 += matrix[1][1] + yyy;
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
			yxy += matrix[0][3];
			yyy += matrix[1][3];
		}
	}
	for (int i = 1; (i < transformation); i++) {
		for (int j = 0; (j < i); j++) {
			hessian[i][j] = hessian[j][i];
		}
	}
	return(meanSquares / (double)area);
} /* getBilinearMeanSquares */

/*------------------------------------------------------------------*/
private double getRigidBodyMeanSquares (
	final double[][] matrix
) {
	double yx;
	double yy;
	double x0;
	double y0;
	double difference;
	double meanSquares = 0.0;
	long area = 0L;
	int xMsk;
	int yMsk;
	int k = 0;
	if (outMsk == null) {
		yx = matrix[0][0];
		yy = matrix[1][0];
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				x = x0;
				y = y0;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)
					&& (0 <= yMsk) && (yMsk < inNy)) {
					if (inMsk[yMsk * inNx + xMsk] != 0.0F) {
						area++;
						xIndexes();
						yIndexes();
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xWeights();
						yWeights();
						difference = (double)outImg[k] - interpolate();
						meanSquares += difference * difference;
					}
				}
				x0 += matrix[0][1];
				y0 += matrix[1][1];
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
		}
	}
	else {
		yx = matrix[0][0];
		yy = matrix[1][0];
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				x = x0;
				y = y0;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)
					&& (0 <= yMsk) && (yMsk < inNy)) {
					if ((outMsk[k] * inMsk[yMsk * inNx + xMsk]) != 0.0F) {
						area++;
						xIndexes();
						yIndexes();
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xWeights();
						yWeights();
						difference = (double)outImg[k] - interpolate();
						meanSquares += difference * difference;
					}
				}
				x0 += matrix[0][1];
				y0 += matrix[1][1];
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
		}
	}
	return(meanSquares / (double)area);
} /* getRigidBodyMeanSquares */

/*------------------------------------------------------------------*/
private double getRigidBodyMeanSquares (
	final double[][] matrix,
	final double[] gradient

) {
	double yx;
	double yy;
	double x0;
	double y0;
	double difference;
	double meanSquares = 0.0;
	long area = 0L;
	int xMsk;
	int yMsk;
	int k = 0;
	for (int i = 0; (i < transformation); i++) {
		gradient[i] = 0.0;
	}
	if (outMsk == null) {
		yx = matrix[0][0];
		yy = matrix[1][0];
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				x = x0;
				y = y0;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)
					&& (0 <= yMsk) && (yMsk < inNy)) {
					if (inMsk[yMsk * inNx + xMsk] != 0.0F) {
						area++;
						xIndexes();
						yIndexes();
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xWeights();
						yWeights();
						difference = (double)outImg[k] - interpolate();
						meanSquares += difference * difference;
						gradient[0] += difference * (yGradient[k] * (double)u
							- xGradient[k] * (double)v);
						gradient[1] += difference * xGradient[k];
						gradient[2] += difference * yGradient[k];
					}
				}
				x0 += matrix[0][1];
				y0 += matrix[1][1];
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
		}
	}
	else {
		yx = matrix[0][0];
		yy = matrix[1][0];
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				x = x0;
				y = y0;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)
					&& (0 <= yMsk) && (yMsk < inNy)) {
					if ((outMsk[k] * inMsk[yMsk * inNx + xMsk]) != 0.0F) {
						area++;
						xIndexes();
						yIndexes();
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xWeights();
						yWeights();
						difference = (double)outImg[k] - interpolate();
						meanSquares += difference * difference;
						gradient[0] += difference * (yGradient[k] * (double)u
							- xGradient[k] * (double)v);
						gradient[1] += difference * xGradient[k];
						gradient[2] += difference * yGradient[k];
					}
				}
				x0 += matrix[0][1];
				y0 += matrix[1][1];
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
		}
	}
	return(meanSquares / (double)area);
} /* getRigidBodyMeanSquares */

/*------------------------------------------------------------------*/
private double getRigidBodyMeanSquares (
	final double[][] matrix,
	final double[][] hessian,
	final double[] gradient
) {
	double yx;
	double yy;
	double x0;
	double y0;
	double dTheta;
	double difference;
	double meanSquares = 0.0;
	long area = 0L;
	int xMsk;
	int yMsk;
	int k = 0;
	for (int i = 0; (i < transformation); i++) {
		gradient[i] = 0.0;
		for (int j = 0; (j < transformation); j++) {
			hessian[i][j] = 0.0;
		}
	}
	if (outMsk == null) {
		yx = matrix[0][0];
		yy = matrix[1][0];
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				x = x0;
				y = y0;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)
					&& (0 <= yMsk) && (yMsk < inNy)) {
					if (inMsk[yMsk * inNx + xMsk] != 0.0F) {
						area++;
						xIndexes();
						yIndexes();
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xWeights();
						yWeights();
						difference = (double)outImg[k] - interpolate();
						meanSquares += difference * difference;
						dTheta = yGradient[k] * (double)u
							- xGradient[k] * (double)v;
						gradient[0] += difference * dTheta;
						gradient[1] += difference * xGradient[k];
						gradient[2] += difference * yGradient[k];
						hessian[0][0] += dTheta * dTheta;
						hessian[0][1] += dTheta * xGradient[k];
						hessian[0][2] += dTheta * yGradient[k];
						hessian[1][1] += xGradient[k] * xGradient[k];
						hessian[1][2] += xGradient[k] * yGradient[k];
						hessian[2][2] += yGradient[k] * yGradient[k];
					}
				}
				x0 += matrix[0][1];
				y0 += matrix[1][1];
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
		}
	}
	else {
		yx = matrix[0][0];
		yy = matrix[1][0];
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				x = x0;
				y = y0;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)
					&& (0 <= yMsk) && (yMsk < inNy)) {
					if ((outMsk[k] * inMsk[yMsk * inNx + xMsk]) != 0.0F) {
						area++;
						xIndexes();
						yIndexes();
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xWeights();
						yWeights();
						difference = (double)outImg[k] - interpolate();
						meanSquares += difference * difference;
						dTheta = yGradient[k] * (double)u
							- xGradient[k] * (double)v;
						gradient[0] += difference * dTheta;
						gradient[1] += difference * xGradient[k];
						gradient[2] += difference * yGradient[k];
						hessian[0][0] += dTheta * dTheta;
						hessian[0][1] += dTheta * xGradient[k];
						hessian[0][2] += dTheta * yGradient[k];
						hessian[1][1] += xGradient[k] * xGradient[k];
						hessian[1][2] += xGradient[k] * yGradient[k];
						hessian[2][2] += yGradient[k] * yGradient[k];
					}
				}
				x0 += matrix[0][1];
				y0 += matrix[1][1];
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
		}
	}
	for (int i = 1; (i < transformation); i++) {
		for (int j = 0; (j < i); j++) {
			hessian[i][j] = hessian[j][i];
		}
	}
	return(meanSquares / (double)area);
} /* getRigidBodyMeanSquares */

/*------------------------------------------------------------------*/
private double getScaledRotationMeanSquares (
	final double[][] sourcePoint,
	final double[][] matrix
) {
	final double u1 = sourcePoint[0][0];
	final double u2 = sourcePoint[1][0];
	final double v1 = sourcePoint[0][1];
	final double v2 = sourcePoint[1][1];
	final double u12 = u1 - u2;
	final double v12 = v1 - v2;
	final double uv2 = u12 * u12 + v12 * v12;
	double yx;
	double yy;
	double x0;
	double y0;
	double difference;
	double meanSquares = 0.0;
	long area = 0L;
	int xMsk;
	int yMsk;
	int k = 0;
	if (outMsk == null) {
		yx = matrix[0][0];
		yy = matrix[1][0];
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				x = x0;
				y = y0;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)
					&& (0 <= yMsk) && (yMsk < inNy)) {
					if (inMsk[yMsk * inNx + xMsk] != 0.0F) {
						area++;
						xIndexes();
						yIndexes();
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xWeights();
						yWeights();
						difference = (double)outImg[k] - interpolate();
						meanSquares += difference * difference;
					}
				}
				x0 += matrix[0][1];
				y0 += matrix[1][1];
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
		}
	}
	else {
		yx = matrix[0][0];
		yy = matrix[1][0];
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				x = x0;
				y = y0;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)
					&& (0 <= yMsk) && (yMsk < inNy)) {
					if ((outMsk[k] * inMsk[yMsk * inNx + xMsk]) != 0.0F) {
						area++;
						xIndexes();
						yIndexes();
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xWeights();
						yWeights();
						difference = (double)outImg[k] - interpolate();
						meanSquares += difference * difference;
					}
				}
				x0 += matrix[0][1];
				y0 += matrix[1][1];
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
		}
	}
	return(meanSquares / ((double)area * uv2 / targetJacobian));
} /* getScaledRotationMeanSquares */

/*------------------------------------------------------------------*/
private double getScaledRotationMeanSquares (
	final double[][] sourcePoint,
	final double[][] matrix,
	final double[] gradient
) {
	final double u1 = sourcePoint[0][0];
	final double u2 = sourcePoint[1][0];
	final double v1 = sourcePoint[0][1];
	final double v2 = sourcePoint[1][1];
	final double u12 = u1 - u2;
	final double v12 = v1 - v2;
	final double uv2 = u12 * u12 + v12 * v12;
	final double c = 0.5 * (u2 * v1 - u1 * v2) / uv2;
	final double c1 = u12 / uv2;
	final double c2 = v12 / uv2;
	final double c3 = (uv2 - u12 * v12) / uv2;
	final double c4 = (uv2 + u12 * v12) / uv2;
	final double c5 = c + u1 * c1 + u2 * c2;
	final double c6 = c * (u12 * u12 - v12 * v12) / uv2;
	final double c7 = c1 * c4;
	final double c8 = c1 - c2 - c1 * c2 * v12;
	final double c9 = c1 + c2 - c1 * c2 * u12;
	final double c0 = c2 * c3;
	final double dgxx0 = c1 * u2 + c2 * v2;
	final double dgyx0 = 2.0 * c;
	final double dgxx1 = c5 + c6;
	final double dgyy1 = c5 - c6;
	double yx;
	double yy;
	double x0;
	double y0;
	double difference;
	double meanSquares = 0.0;
	double gxx0;
	double gxx1;
	double gxy0;
	double gxy1;
	double gyx0;
	double gyx1;
	double gyy0;
	double gyy1;
	double dx0;
	double dx1;
	double dy0;
	double dy1;
	long area = 0L;
	int xMsk;
	int yMsk;
	int k = 0;
	for (int i = 0; (i < transformation); i++) {
		gradient[i] = 0.0;
	}
	if (outMsk == null) {
		yx = matrix[0][0];
		yy = matrix[1][0];
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				x = x0;
				y = y0;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)
					&& (0 <= yMsk) && (yMsk < inNy)) {
					if (inMsk[yMsk * inNx + xMsk] != 0.0F) {
						area++;
						xIndexes();
						yIndexes();
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xWeights();
						yWeights();
						difference = (double)outImg[k] - interpolate();
						meanSquares += difference * difference;
						gxx0 = (double)u * c1 + (double)v * c2 - dgxx0;
						gyx0 = (double)v * c1 - (double)u * c2 + dgyx0;
						gxy0 = -gyx0;
						gyy0 = gxx0;
						gxx1 = (double)v * c8 - (double)u * c7 + dgxx1;
						gyx1 = -c3 * gyx0;
						gxy1 = c4 * gyx0;
						gyy1 = dgyy1 - (double)u * c9 - (double)v * c0;
						dx0 = xGradient[k] * gxx0 + yGradient[k] * gyx0;
						dy0 = xGradient[k] * gxy0 + yGradient[k] * gyy0;
						dx1 = xGradient[k] * gxx1 + yGradient[k] * gyx1;
						dy1 = xGradient[k] * gxy1 + yGradient[k] * gyy1;
						gradient[0] += difference * dx0;
						gradient[1] += difference * dy0;
						gradient[2] += difference * dx1;
						gradient[3] += difference * dy1;
					}
				}
				x0 += matrix[0][1];
				y0 += matrix[1][1];
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
		}
	}
	else {
		yx = matrix[0][0];
		yy = matrix[1][0];
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				x = x0;
				y = y0;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)
					&& (0 <= yMsk) && (yMsk < inNy)) {
					if ((outMsk[k] * inMsk[yMsk * inNx + xMsk]) != 0.0F) {
						area++;
						xIndexes();
						yIndexes();
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xWeights();
						yWeights();
						difference = (double)outImg[k] - interpolate();
						meanSquares += difference * difference;
						gxx0 = (double)u * c1 + (double)v * c2 - dgxx0;
						gyx0 = (double)v * c1 - (double)u * c2 + dgyx0;
						gxy0 = -gyx0;
						gyy0 = gxx0;
						gxx1 = (double)v * c8 - (double)u * c7 + dgxx1;
						gyx1 = -c3 * gyx0;
						gxy1 = c4 * gyx0;
						gyy1 = dgyy1 - (double)u * c9 - (double)v * c0;
						dx0 = xGradient[k] * gxx0 + yGradient[k] * gyx0;
						dy0 = xGradient[k] * gxy0 + yGradient[k] * gyy0;
						dx1 = xGradient[k] * gxx1 + yGradient[k] * gyx1;
						dy1 = xGradient[k] * gxy1 + yGradient[k] * gyy1;
						gradient[0] += difference * dx0;
						gradient[1] += difference * dy0;
						gradient[2] += difference * dx1;
						gradient[3] += difference * dy1;
					}
				}
				x0 += matrix[0][1];
				y0 += matrix[1][1];
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
		}
	}
	return(meanSquares / ((double)area * uv2 / targetJacobian));
} /* getScaledRotationMeanSquares */

/*------------------------------------------------------------------*/
private double getScaledRotationMeanSquares (
	final double[][] sourcePoint,
	final double[][] matrix,
	final double[][] hessian,
	final double[] gradient
) {
	final double u1 = sourcePoint[0][0];
	final double u2 = sourcePoint[1][0];
	final double v1 = sourcePoint[0][1];
	final double v2 = sourcePoint[1][1];
	final double u12 = u1 - u2;
	final double v12 = v1 - v2;
	final double uv2 = u12 * u12 + v12 * v12;
	final double c = 0.5 * (u2 * v1 - u1 * v2) / uv2;
	final double c1 = u12 / uv2;
	final double c2 = v12 / uv2;
	final double c3 = (uv2 - u12 * v12) / uv2;
	final double c4 = (uv2 + u12 * v12) / uv2;
	final double c5 = c + u1 * c1 + u2 * c2;
	final double c6 = c * (u12 * u12 - v12 * v12) / uv2;
	final double c7 = c1 * c4;
	final double c8 = c1 - c2 - c1 * c2 * v12;
	final double c9 = c1 + c2 - c1 * c2 * u12;
	final double c0 = c2 * c3;
	final double dgxx0 = c1 * u2 + c2 * v2;
	final double dgyx0 = 2.0 * c;
	final double dgxx1 = c5 + c6;
	final double dgyy1 = c5 - c6;
	double yx;
	double yy;
	double x0;
	double y0;
	double difference;
	double meanSquares = 0.0;
	double gxx0;
	double gxx1;
	double gxy0;
	double gxy1;
	double gyx0;
	double gyx1;
	double gyy0;
	double gyy1;
	double dx0;
	double dx1;
	double dy0;
	double dy1;
	long area = 0L;
	int xMsk;
	int yMsk;
	int k = 0;
	for (int i = 0; (i < transformation); i++) {
		gradient[i] = 0.0;
		for (int j = 0; (j < transformation); j++) {
			hessian[i][j] = 0.0;
		}
	}
	if (outMsk == null) {
		yx = matrix[0][0];
		yy = matrix[1][0];
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				x = x0;
				y = y0;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)
					&& (0 <= yMsk) && (yMsk < inNy)) {
					if (inMsk[yMsk * inNx + xMsk] != 0.0F) {
						area++;
						xIndexes();
						yIndexes();
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xWeights();
						yWeights();
						difference = (double)outImg[k] - interpolate();
						meanSquares += difference * difference;
						gxx0 = (double)u * c1 + (double)v * c2 - dgxx0;
						gyx0 = (double)v * c1 - (double)u * c2 + dgyx0;
						gxy0 = -gyx0;
						gyy0 = gxx0;
						gxx1 = (double)v * c8 - (double)u * c7 + dgxx1;
						gyx1 = -c3 * gyx0;
						gxy1 = c4 * gyx0;
						gyy1 = dgyy1 - (double)u * c9 - (double)v * c0;
						dx0 = xGradient[k] * gxx0 + yGradient[k] * gyx0;
						dy0 = xGradient[k] * gxy0 + yGradient[k] * gyy0;
						dx1 = xGradient[k] * gxx1 + yGradient[k] * gyx1;
						dy1 = xGradient[k] * gxy1 + yGradient[k] * gyy1;
						gradient[0] += difference * dx0;
						gradient[1] += difference * dy0;
						gradient[2] += difference * dx1;
						gradient[3] += difference * dy1;
						hessian[0][0] += dx0 * dx0;
						hessian[0][1] += dx0 * dy0;
						hessian[0][2] += dx0 * dx1;
						hessian[0][3] += dx0 * dy1;
						hessian[1][1] += dy0 * dy0;
						hessian[1][2] += dy0 * dx1;
						hessian[1][3] += dy0 * dy1;
						hessian[2][2] += dx1 * dx1;
						hessian[2][3] += dx1 * dy1;
						hessian[3][3] += dy1 * dy1;
					}
				}
				x0 += matrix[0][1];
				y0 += matrix[1][1];
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
		}
	}
	else {
		yx = matrix[0][0];
		yy = matrix[1][0];
		for (int v = 0; (v < outNy); v++) {
			x0 = yx;
			y0 = yy;
			for (int u = 0; (u < outNx); u++, k++) {
				x = x0;
				y = y0;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)
					&& (0 <= yMsk) && (yMsk < inNy)) {
					if ((outMsk[k] * inMsk[yMsk * inNx + xMsk]) != 0.0F) {
						area++;
						xIndexes();
						yIndexes();
						x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
						y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
						xWeights();
						yWeights();
						difference = (double)outImg[k] - interpolate();
						meanSquares += difference * difference;
						gxx0 = (double)u * c1 + (double)v * c2 - dgxx0;
						gyx0 = (double)v * c1 - (double)u * c2 + dgyx0;
						gxy0 = -gyx0;
						gyy0 = gxx0;
						gxx1 = (double)v * c8 - (double)u * c7 + dgxx1;
						gyx1 = -c3 * gyx0;
						gxy1 = c4 * gyx0;
						gyy1 = dgyy1 - (double)u * c9 - (double)v * c0;
						dx0 = xGradient[k] * gxx0 + yGradient[k] * gyx0;
						dy0 = xGradient[k] * gxy0 + yGradient[k] * gyy0;
						dx1 = xGradient[k] * gxx1 + yGradient[k] * gyx1;
						dy1 = xGradient[k] * gxy1 + yGradient[k] * gyy1;
						gradient[0] += difference * dx0;
						gradient[1] += difference * dy0;
						gradient[2] += difference * dx1;
						gradient[3] += difference * dy1;
						hessian[0][0] += dx0 * dx0;
						hessian[0][1] += dx0 * dy0;
						hessian[0][2] += dx0 * dx1;
						hessian[0][3] += dx0 * dy1;
						hessian[1][1] += dy0 * dy0;
						hessian[1][2] += dy0 * dx1;
						hessian[1][3] += dy0 * dy1;
						hessian[2][2] += dx1 * dx1;
						hessian[2][3] += dx1 * dy1;
						hessian[3][3] += dy1 * dy1;
					}
				}
				x0 += matrix[0][1];
				y0 += matrix[1][1];
			}
			yx += matrix[0][2];
			yy += matrix[1][2];
		}
	}
	for (int i = 1; (i < transformation); i++) {
		for (int j = 0; (j < i); j++) {
			hessian[i][j] = hessian[j][i];
		}
	}
	return(meanSquares / ((double)area * uv2 / targetJacobian));
} /* getScaledRotationMeanSquares */

/*------------------------------------------------------------------*/
private double[][] getTransformationMatrix (
	final double[][] fromCoord,
	final double[][] toCoord
) {
	double[][] matrix = null;
	double[][] a = null;
	double[] v = null;
	switch (transformation) {
		case turboRegDialog.TRANSLATION: {
			matrix = new double[2][1];
			matrix[0][0] = toCoord[0][0] - fromCoord[0][0];
			matrix[1][0] = toCoord[0][1] - fromCoord[0][1];
			break;
		}
		case turboRegDialog.RIGID_BODY: {
			final double angle = Math.atan2(fromCoord[2][0] - fromCoord[1][0],
				fromCoord[2][1] - fromCoord[1][1])
				- Math.atan2(toCoord[2][0] - toCoord[1][0],
				toCoord[2][1] - toCoord[1][1]);
			final double c = Math.cos(angle);
			final double s = Math.sin(angle);
			matrix = new double[2][3];
			matrix[0][0] = toCoord[0][0]
				- c * fromCoord[0][0] + s * fromCoord[0][1];
			matrix[0][1] = c;
			matrix[0][2] = -s;
			matrix[1][0] = toCoord[0][1]
				- s * fromCoord[0][0] - c * fromCoord[0][1];
			matrix[1][1] = s;
			matrix[1][2] = c;
			break;
		}
		case turboRegDialog.SCALED_ROTATION: {
			matrix = new double[2][3];
			a = new double[3][3];
			v = new double[3];
			a[0][0] = 1.0;
			a[0][1] = fromCoord[0][0];
			a[0][2] = fromCoord[0][1];
			a[1][0] = 1.0;
			a[1][1] = fromCoord[1][0];
			a[1][2] = fromCoord[1][1];
			a[2][0] = 1.0;
			a[2][1] = fromCoord[0][1] - fromCoord[1][1] + fromCoord[1][0];
			a[2][2] = fromCoord[1][0] + fromCoord[1][1] - fromCoord[0][0];
			invertGauss(a);
			v[0] = toCoord[0][0];
			v[1] = toCoord[1][0];
			v[2] = toCoord[0][1] - toCoord[1][1] + toCoord[1][0];
			for (int i = 0; (i < 3); i++) {
				matrix[0][i] = 0.0;
				for (int j = 0; (j < 3); j++) {
					matrix[0][i] += a[i][j] * v[j];
				}
			}
			v[0] = toCoord[0][1];
			v[1] = toCoord[1][1];
			v[2] = toCoord[1][0] + toCoord[1][1] - toCoord[0][0];
			for (int i = 0; (i < 3); i++) {
				matrix[1][i] = 0.0;
				for (int j = 0; (j < 3); j++) {
					matrix[1][i] += a[i][j] * v[j];
				}
			}
			break;
		}
		case turboRegDialog.AFFINE: {
			matrix = new double[2][3];
			a = new double[3][3];
			v = new double[3];
			a[0][0] = 1.0;
			a[0][1] = fromCoord[0][0];
			a[0][2] = fromCoord[0][1];
			a[1][0] = 1.0;
			a[1][1] = fromCoord[1][0];
			a[1][2] = fromCoord[1][1];
			a[2][0] = 1.0;
			a[2][1] = fromCoord[2][0];
			a[2][2] = fromCoord[2][1];
			invertGauss(a);
			v[0] = toCoord[0][0];
			v[1] = toCoord[1][0];
			v[2] = toCoord[2][0];
			for (int i = 0; (i < 3); i++) {
				matrix[0][i] = 0.0;
				for (int j = 0; (j < 3); j++) {
					matrix[0][i] += a[i][j] * v[j];
				}
			}
			v[0] = toCoord[0][1];
			v[1] = toCoord[1][1];
			v[2] = toCoord[2][1];
			for (int i = 0; (i < 3); i++) {
				matrix[1][i] = 0.0;
				for (int j = 0; (j < 3); j++) {
					matrix[1][i] += a[i][j] * v[j];
				}
			}
			break;
		}
		case turboRegDialog.BILINEAR: {
			matrix = new double[2][4];
			a = new double[4][4];
			v = new double[4];
			a[0][0] = 1.0;
			a[0][1] = fromCoord[0][0];
			a[0][2] = fromCoord[0][1];
			a[0][3] = fromCoord[0][0] * fromCoord[0][1];
			a[1][0] = 1.0;
			a[1][1] = fromCoord[1][0];
			a[1][2] = fromCoord[1][1];
			a[1][3] = fromCoord[1][0] * fromCoord[1][1];
			a[2][0] = 1.0;
			a[2][1] = fromCoord[2][0];
			a[2][2] = fromCoord[2][1];
			a[2][3] = fromCoord[2][0] * fromCoord[2][1];
			a[3][0] = 1.0;
			a[3][1] = fromCoord[3][0];
			a[3][2] = fromCoord[3][1];
			a[3][3] = fromCoord[3][0] * fromCoord[3][1];
			invertGauss(a);
			v[0] = toCoord[0][0];
			v[1] = toCoord[1][0];
			v[2] = toCoord[2][0];
			v[3] = toCoord[3][0];
			for (int i = 0; (i < 4); i++) {
				matrix[0][i] = 0.0;
				for (int j = 0; (j < 4); j++) {
					matrix[0][i] += a[i][j] * v[j];
				}
			}
			v[0] = toCoord[0][1];
			v[1] = toCoord[1][1];
			v[2] = toCoord[2][1];
			v[3] = toCoord[3][1];
			for (int i = 0; (i < 4); i++) {
				matrix[1][i] = 0.0;
				for (int j = 0; (j < 4); j++) {
					matrix[1][i] += a[i][j] * v[j];
				}
			}
			break;
		}
	}
	return(matrix);
} /* end getTransformationMatrix */

/*------------------------------------------------------------------*/
private double getTranslationMeanSquares (
	final double[][] matrix
) {
	double dx = matrix[0][0];
	double dy = matrix[1][0];
	final double dx0 = dx;
	double difference;
	double meanSquares = 0.0;
	long area = 0L;
	int xMsk;
	int yMsk;
	int k = 0;
	x = dx - Math.floor(dx);
	y = dy - Math.floor(dy);
	xWeights();
	yWeights();
	if (outMsk == null) {
		for (int v = 0; (v < outNy); v++) {
			y = dy++;
			yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
			if ((0 <= yMsk) && (yMsk < inNy)) {
				yMsk *= inNx;
				yIndexes();
				dx = dx0;
				for (int u = 0; (u < outNx); u++, k++) {
					x = dx++;
					xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
					if ((0 <= xMsk) && (xMsk < inNx)) {
						if (inMsk[yMsk + xMsk] != 0.0F) {
							xIndexes();
							area++;
							difference = (double)outImg[k] - interpolate();
							meanSquares += difference * difference;
						}
					}
				}
			}
			else {
				k += outNx;
			}
		}
	}
	else {
		for (int v = 0; (v < outNy); v++) {
			y = dy++;
			yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
			if ((0 <= yMsk) && (yMsk < inNy)) {
				yMsk *= inNx;
				yIndexes();
				dx = dx0;
				for (int u = 0; (u < outNx); u++, k++) {
					x = dx++;
					xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
					if ((0 <= xMsk) && (xMsk < inNx)) {
						if ((outMsk[k] * inMsk[yMsk + xMsk]) != 0.0F) {
							xIndexes();
							area++;
							difference = (double)outImg[k] - interpolate();
							meanSquares += difference * difference;
						}
					}
				}
			}
			else {
				k += outNx;
			}
		}
	}
	return(meanSquares / (double)area);
} /* end getTranslationMeanSquares */

/*------------------------------------------------------------------*/
private double getTranslationMeanSquares (
	final double[][] matrix,
	final double[] gradient
) {
	double dx = matrix[0][0];
	double dy = matrix[1][0];
	final double dx0 = dx;
	double difference;
	double meanSquares = 0.0;
	long area = 0L;
	int xMsk;
	int yMsk;
	int k = 0;
	for (int i = 0; (i < transformation); i++) {
		gradient[i] = 0.0;
	}
	x = dx - Math.floor(dx);
	y = dy - Math.floor(dy);
	xWeights();
	yWeights();
	if (outMsk == null) {
		for (int v = 0; (v < outNy); v++) {
			y = dy++;
			yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
			if ((0 <= yMsk) && (yMsk < inNy)) {
				yMsk *= inNx;
				yIndexes();
				dx = dx0;
				for (int u = 0; (u < outNx); u++, k++) {
					x = dx++;
					xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
					if ((0 <= xMsk) && (xMsk < inNx)) {
						if (inMsk[yMsk + xMsk] != 0.0F) {
							area++;
							xIndexes();
							difference = (double)outImg[k] - interpolate();
							meanSquares += difference * difference;
							gradient[0] += difference * xGradient[k];
							gradient[1] += difference * yGradient[k];
						}
					}
				}
			}
			else {
				k += outNx;
			}
		}
	}
	else {
		for (int v = 0; (v < outNy); v++) {
			y = dy++;
			yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
			if ((0 <= yMsk) && (yMsk < inNy)) {
				yMsk *= inNx;
				yIndexes();
				dx = dx0;
				for (int u = 0; (u < outNx); u++, k++) {
					x = dx++;
					xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
					if ((0 <= xMsk) && (xMsk < inNx)) {
						if ((outMsk[k] * inMsk[yMsk + xMsk]) != 0.0F) {
							area++;
							xIndexes();
							difference = (double)outImg[k] - interpolate();
							meanSquares += difference * difference;
							gradient[0] += difference * xGradient[k];
							gradient[1] += difference * yGradient[k];
						}
					}
				}
			}
			else {
				k += outNx;
			}
		}
	}
	return(meanSquares / (double)area);
} /* end getTranslationMeanSquares */

/*------------------------------------------------------------------*/
private double getTranslationMeanSquares (
	final double[][] matrix,
	final double[][] hessian,
	final double[] gradient
) {
	double dx = matrix[0][0];
	double dy = matrix[1][0];
	final double dx0 = dx;
	double difference;
	double meanSquares = 0.0;
	long area = 0L;
	int xMsk;
	int yMsk;
	int k = 0;
	for (int i = 0; (i < transformation); i++) {
		gradient[i] = 0.0;
		for (int j = 0; (j < transformation); j++) {
			hessian[i][j] = 0.0;
		}
	}
	x = dx - Math.floor(dx);
	y = dy - Math.floor(dy);
	xWeights();
	yWeights();
	if (outMsk == null) {
		for (int v = 0; (v < outNy); v++) {
			y = dy++;
			yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
			if ((0 <= yMsk) && (yMsk < inNy)) {
				yMsk *= inNx;
				yIndexes();
				dx = dx0;
				for (int u = 0; (u < outNx); u++, k++) {
					x = dx++;
					xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
					if ((0 <= xMsk) && (xMsk < inNx)) {
						if (inMsk[yMsk + xMsk] != 0.0F) {
							area++;
							xIndexes();
							difference = (double)outImg[k] - interpolate();
							meanSquares += difference * difference;
							gradient[0] += difference * xGradient[k];
							gradient[1] += difference * yGradient[k];
							hessian[0][0] += xGradient[k] * xGradient[k];
							hessian[0][1] += xGradient[k] * yGradient[k];
							hessian[1][1] += yGradient[k] * yGradient[k];
						}
					}
				}
			}
			else {
				k += outNx;
			}
		}
	}
	else {
		for (int v = 0; (v < outNy); v++) {
			y = dy++;
			yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
			if ((0 <= yMsk) && (yMsk < inNy)) {
				yMsk *= inNx;
				yIndexes();
				dx = dx0;
				for (int u = 0; (u < outNx); u++, k++) {
					x = dx++;
					xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
					if ((0 <= xMsk) && (xMsk < inNx)) {
						if ((outMsk[k] * inMsk[yMsk + xMsk]) != 0.0F) {
							area++;
							xIndexes();
							difference = (double)outImg[k] - interpolate();
							meanSquares += difference * difference;
							gradient[0] += difference * xGradient[k];
							gradient[1] += difference * yGradient[k];
							hessian[0][0] += xGradient[k] * xGradient[k];
							hessian[0][1] += xGradient[k] * yGradient[k];
							hessian[1][1] += yGradient[k] * yGradient[k];
						}
					}
				}
			}
			else {
				k += outNx;
			}
		}
	}
	for (int i = 1; (i < transformation); i++) {
		for (int j = 0; (j < i); j++) {
			hessian[i][j] = hessian[j][i];
		}
	}
	return(meanSquares / (double)area);
} /* end getTranslationMeanSquares */

/*------------------------------------------------------------------*/
private double interpolate (
) {
	t = 0.0;
	for (int j = 0; (j < 4); j++) {
		s = 0.0;
		p = yIndex[j];
		for (int i = 0; (i < 4); i++) {
			s += xWeight[i] * (double)inImg[p + xIndex[i]];
		}
		t += yWeight[j] * s;
	}
	return(t);
} /* end interpolate */

/*------------------------------------------------------------------*/
private double interpolateDx (
) {
	t = 0.0;
	for (int j = 0; (j < 4); j++) {
		s = 0.0;
		p = yIndex[j];
		for (int i = 0; (i < 4); i++) {
			s += dxWeight[i] * (double)inImg[p + xIndex[i]];
		}
		t += yWeight[j] * s;
	}
	return(t);
} /* end interpolateDx */

/*------------------------------------------------------------------*/
private double interpolateDy (
) {
	t = 0.0;
	for (int j = 0; (j < 4); j++) {
		s = 0.0;
		p = yIndex[j];
		for (int i = 0; (i < 4); i++) {
			s += xWeight[i] * (double)inImg[p + xIndex[i]];
		}
		t += dyWeight[j] * s;
	}
	return(t);
} /* end interpolateDy */

/*------------------------------------------------------------------*/
private void inverseMarquardtLevenbergOptimization (
	int workload
) {
	final double[][] attempt = new double[transformation / 2][2];
	final double[][] hessian = new double[transformation][transformation];
	final double[][] pseudoHessian = new double[transformation][transformation];
	final double[] gradient = new double[transformation];
	double[][] matrix = getTransformationMatrix(sourcePoint, targetPoint);
	double[] update = new double[transformation];
	double bestMeanSquares = 0.0;
	double meanSquares = 0.0;
	double lambda = FIRST_LAMBDA;
	double displacement;
	int iteration = 0;
	switch (transformation) {
		case turboRegDialog.TRANSLATION: {
			bestMeanSquares = getTranslationMeanSquares(
				matrix, hessian, gradient);
			break;
		}
		case turboRegDialog.SCALED_ROTATION: {
			bestMeanSquares = getScaledRotationMeanSquares(
				sourcePoint, matrix, hessian, gradient);
			break;
		}
		case turboRegDialog.AFFINE: {
			bestMeanSquares = getAffineMeanSquares(
				sourcePoint, matrix, hessian, gradient);
			break;
		}
	}
	iteration++;
	do {
		for (int k = 0; (k < transformation); k++) {
			pseudoHessian[k][k] = (1.0 + lambda) * hessian[k][k];
		}
		invertGauss(pseudoHessian);
		update = matrixMultiply(pseudoHessian, gradient);
		displacement = 0.0;
		for (int k = 0; (k < (transformation / 2)); k++) {
			attempt[k][0] = sourcePoint[k][0] - update[2 * k];
			attempt[k][1] = sourcePoint[k][1] - update[2 * k + 1];
			displacement += Math.sqrt(update[2 * k] * update[2 * k]
				+ update[2 * k + 1] * update[2 * k + 1]);
		}
		displacement /= 0.5 * (double)transformation;
		matrix = getTransformationMatrix(attempt, targetPoint);
		switch (transformation) {
			case turboRegDialog.TRANSLATION: {
				if (accelerated) {
					meanSquares = getTranslationMeanSquares(
						matrix, gradient);
				}
				else {
					meanSquares = getTranslationMeanSquares(
						matrix, hessian, gradient);
				}
				break;
			}
			case turboRegDialog.SCALED_ROTATION: {
				if (accelerated) {
					meanSquares = getScaledRotationMeanSquares(
						attempt, matrix, gradient);
				}
				else {
					meanSquares = getScaledRotationMeanSquares(
						attempt, matrix, hessian, gradient);
				}
				break;
			}
			case turboRegDialog.AFFINE: {
				if (accelerated) {
					meanSquares = getAffineMeanSquares(
						attempt, matrix, gradient);
				}
				else {
					meanSquares = getAffineMeanSquares(
						attempt, matrix, hessian, gradient);
				}
				break;
			}
		}
		iteration++;
		if (meanSquares < bestMeanSquares) {
			bestMeanSquares = meanSquares;
			for (int k = 0; (k < (transformation / 2)); k++) {
				sourcePoint[k][0] = attempt[k][0];
				sourcePoint[k][1] = attempt[k][1];
			}
			lambda /= LAMBDA_MAGSTEP;
		}
		else {
			lambda *= LAMBDA_MAGSTEP;
		}
		turboRegProgressBar.skipProgressBar(iterationCost);
		workload--;
	} while ((iteration < (maxIterations * iterationPower - 1))
		&& (pixelPrecision <= displacement));
	invertGauss(hessian);
	update = matrixMultiply(hessian, gradient);
	for (int k = 0; (k < (transformation / 2)); k++) {
		attempt[k][0] = sourcePoint[k][0] - update[2 * k];
		attempt[k][1] = sourcePoint[k][1] - update[2 * k + 1];
	}
	matrix = getTransformationMatrix(attempt, targetPoint);
	switch (transformation) {
		case turboRegDialog.TRANSLATION: {
			meanSquares = getTranslationMeanSquares(matrix);
			break;
		}
		case turboRegDialog.SCALED_ROTATION: {
			meanSquares = getScaledRotationMeanSquares(attempt, matrix);
			break;
		}
		case turboRegDialog.AFFINE: {
			meanSquares = getAffineMeanSquares(attempt, matrix);
			break;
		}
	}
	iteration++;
	if (meanSquares < bestMeanSquares) {
		for (int k = 0; (k < (transformation / 2)); k++) {
			sourcePoint[k][0] = attempt[k][0];
			sourcePoint[k][1] = attempt[k][1];
		}
	}
	turboRegProgressBar.skipProgressBar(workload * iterationCost);
} /* end inverseMarquardtLevenbergOptimization */

/*------------------------------------------------------------------*/
private void inverseMarquardtLevenbergRigidBodyOptimization (
	int workload
) {
	final double[][] attempt = new double[2][3];
	final double[][] hessian = new double[transformation][transformation];
	final double[][] pseudoHessian = new double[transformation][transformation];
	final double[] gradient = new double[transformation];
	double[][] matrix = getTransformationMatrix(targetPoint, sourcePoint);
	double[] update = new double[transformation];
	double bestMeanSquares = 0.0;
	double meanSquares = 0.0;
	double lambda = FIRST_LAMBDA;
	double angle;
	double c;
	double s;
	double displacement;
	int iteration = 0;
	for (int k = 0; (k < transformation); k++) {
		sourcePoint[k][0] = matrix[0][0] + targetPoint[k][0] * matrix[0][1]
			+ targetPoint[k][1] * matrix[0][2];
		sourcePoint[k][1] = matrix[1][0] + targetPoint[k][0] * matrix[1][1]
			+ targetPoint[k][1] * matrix[1][2];
	}
	matrix = getTransformationMatrix(sourcePoint, targetPoint);
	bestMeanSquares = getRigidBodyMeanSquares(matrix, hessian, gradient);
	iteration++;
	do {
		for (int k = 0; (k < transformation); k++) {
			pseudoHessian[k][k] = (1.0 + lambda) * hessian[k][k];
		}
		invertGauss(pseudoHessian);
		update = matrixMultiply(pseudoHessian, gradient);
		angle = Math.atan2(matrix[0][2], matrix[0][1]) - update[0];
		attempt[0][1] = Math.cos(angle);
		attempt[0][2] = Math.sin(angle);
		attempt[1][1] = -attempt[0][2];
		attempt[1][2] = attempt[0][1];
		c = Math.cos(update[0]);
		s = Math.sin(update[0]);
		attempt[0][0] = (matrix[0][0] + update[1]) * c
			- (matrix[1][0] + update[2]) * s;
		attempt[1][0] = (matrix[0][0] + update[1]) * s
			+ (matrix[1][0] + update[2]) * c;
		displacement = Math.sqrt(update[1] * update[1] + update[2] * update[2])
			+ 0.25 * Math.sqrt((double)(inNx * inNx) + (double)(inNy * inNy))
			* Math.abs(update[0]);
		if (accelerated) {
			meanSquares = getRigidBodyMeanSquares(attempt, gradient);
		}
		else {
			meanSquares = getRigidBodyMeanSquares(attempt, hessian, gradient);
		}
		iteration++;
		if (meanSquares < bestMeanSquares) {
			bestMeanSquares = meanSquares;
			for (int i = 0; (i < 2); i++) {
				for (int j = 0; (j < 3); j++) {
					matrix[i][j] = attempt[i][j];
				}
			}
			lambda /= LAMBDA_MAGSTEP;
		}
		else {
			lambda *= LAMBDA_MAGSTEP;
		}
		turboRegProgressBar.skipProgressBar(iterationCost);
		workload--;
	} while ((iteration < (maxIterations * iterationPower - 1))
		&& (pixelPrecision <= displacement));
	invertGauss(hessian);
	update = matrixMultiply(hessian, gradient);
	angle = Math.atan2(matrix[0][2], matrix[0][1]) - update[0];
	attempt[0][1] = Math.cos(angle);
	attempt[0][2] = Math.sin(angle);
	attempt[1][1] = -attempt[0][2];
	attempt[1][2] = attempt[0][1];
	c = Math.cos(update[0]);
	s = Math.sin(update[0]);
	attempt[0][0] = (matrix[0][0] + update[1]) * c
		- (matrix[1][0] + update[2]) * s;
	attempt[1][0] = (matrix[0][0] + update[1]) * s
		+ (matrix[1][0] + update[2]) * c;
	meanSquares = getRigidBodyMeanSquares(attempt);
	iteration++;
	if (meanSquares < bestMeanSquares) {
		for (int i = 0; (i < 2); i++) {
			for (int j = 0; (j < 3); j++) {
				matrix[i][j] = attempt[i][j];
			}
		}
	}
	for (int k = 0; (k < transformation); k++) {
		sourcePoint[k][0] = (targetPoint[k][0] - matrix[0][0]) * matrix[0][1]
			+ (targetPoint[k][1] - matrix[1][0]) * matrix[1][1];
		sourcePoint[k][1] = (targetPoint[k][0] - matrix[0][0]) * matrix[0][2]
			+ (targetPoint[k][1] - matrix[1][0]) * matrix[1][2];
	}
	turboRegProgressBar.skipProgressBar(workload * iterationCost);
} /* end inverseMarquardtLevenbergRigidBodyOptimization */

/*------------------------------------------------------------------*/
private void invertGauss (
	final double[][] matrix
) {
	final int n = matrix.length;
	final double[][] inverse = new double[n][n];
	for (int i = 0; (i < n); i++) {
		double max = matrix[i][0];
		double absMax = Math.abs(max);
		for (int j = 0; (j < n); j++) {
			inverse[i][j] = 0.0;
			if (absMax < Math.abs(matrix[i][j])) {
				max = matrix[i][j];
				absMax = Math.abs(max);
			}
		}
		inverse[i][i] = 1.0 / max;
		for (int j = 0; (j < n); j++) {
			matrix[i][j] /= max;
		}
	}
	for (int j = 0; (j < n); j++) {
		double max = matrix[j][j];
		double absMax = Math.abs(max);
		int k = j;
		for (int i = j + 1; (i < n); i++) {
			if (absMax < Math.abs(matrix[i][j])) {
				max = matrix[i][j];
				absMax = Math.abs(max);
				k = i;
			}
		}
		if (k != j) {
			final double[] partialLine = new double[n - j];
			final double[] fullLine = new double[n];
			System.arraycopy(matrix[j], j, partialLine, 0, n - j);
			System.arraycopy(matrix[k], j, matrix[j], j, n - j);
			System.arraycopy(partialLine, 0, matrix[k], j, n - j);
			System.arraycopy(inverse[j], 0, fullLine, 0, n);
			System.arraycopy(inverse[k], 0, inverse[j], 0, n);
			System.arraycopy(fullLine, 0, inverse[k], 0, n);
		}
		for (k = 0; (k <= j); k++) {
			inverse[j][k] /= max;
		}
		for (k = j + 1; (k < n); k++) {
			matrix[j][k] /= max;
			inverse[j][k] /= max;
		}
		for (int i = j + 1; (i < n); i++) {
			for (k = 0; (k <= j); k++) {
				inverse[i][k] -= matrix[i][j] * inverse[j][k];
			}
			for (k = j + 1; (k < n); k++) {
				matrix[i][k] -= matrix[i][j] * matrix[j][k];
				inverse[i][k] -= matrix[i][j] * inverse[j][k];
			}
		}
	}
	for (int j = n - 1; (1 <= j); j--) {
		for (int i = j - 1; (0 <= i); i--) {
			for (int k = 0; (k <= j); k++) {
				inverse[i][k] -= matrix[i][j] * inverse[j][k];
			}
			for (int k = j + 1; (k < n); k++) {
				matrix[i][k] -= matrix[i][j] * matrix[j][k];
				inverse[i][k] -= matrix[i][j] * inverse[j][k];
			}
		}
	}
	for (int i = 0; (i < n); i++) {
		System.arraycopy(inverse[i], 0, matrix[i], 0, n);
	}
} /* end invertGauss */

/*------------------------------------------------------------------*/
private void MarquardtLevenbergOptimization (
	int workload
) {
	final double[][] attempt = new double[transformation / 2][2];
	final double[][] hessian = new double[transformation][transformation];
	final double[][] pseudoHessian = new double[transformation][transformation];
	final double[] gradient = new double[transformation];
	double[][] matrix = getTransformationMatrix(targetPoint, sourcePoint);
	double[] update = new double[transformation];
	double bestMeanSquares = 0.0;
	double meanSquares = 0.0;
	double lambda = FIRST_LAMBDA;
	double displacement;
	int iteration = 0;
	bestMeanSquares = getBilinearMeanSquares(matrix, hessian, gradient);
	iteration++;
	do {
		for (int k = 0; (k < transformation); k++) {
			pseudoHessian[k][k] = (1.0 + lambda) * hessian[k][k];
		}
		invertGauss(pseudoHessian);
		update = matrixMultiply(pseudoHessian, gradient);
		displacement = 0.0;
		for (int k = 0; (k < (transformation / 2)); k++) {
			attempt[k][0] = sourcePoint[k][0] - update[2 * k];
			attempt[k][1] = sourcePoint[k][1] - update[2 * k + 1];
			displacement += Math.sqrt(update[2 * k] * update[2 * k]
				+ update[2 * k + 1] * update[2 * k + 1]);
		}
		displacement /= 0.5 * (double)transformation;
		matrix = getTransformationMatrix(targetPoint, attempt);
		meanSquares = getBilinearMeanSquares(matrix, hessian, gradient);
		iteration++;
		if (meanSquares < bestMeanSquares) {
			bestMeanSquares = meanSquares;
			for (int k = 0; (k < (transformation / 2)); k++) {
				sourcePoint[k][0] = attempt[k][0];
				sourcePoint[k][1] = attempt[k][1];
			}
			lambda /= LAMBDA_MAGSTEP;
		}
		else {
			lambda *= LAMBDA_MAGSTEP;
		}
		turboRegProgressBar.skipProgressBar(iterationCost);
		workload--;
	} while ((iteration < (maxIterations * iterationPower - 1))
		&& (pixelPrecision <= displacement));
	invertGauss(hessian);
	update = matrixMultiply(hessian, gradient);
	for (int k = 0; (k < (transformation / 2)); k++) {
		attempt[k][0] = sourcePoint[k][0] - update[2 * k];
		attempt[k][1] = sourcePoint[k][1] - update[2 * k + 1];
	}
	matrix = getTransformationMatrix(targetPoint, attempt);
	meanSquares = getBilinearMeanSquares(matrix);
	iteration++;
	if (meanSquares < bestMeanSquares) {
		for (int k = 0; (k < (transformation / 2)); k++) {
			sourcePoint[k][0] = attempt[k][0];
			sourcePoint[k][1] = attempt[k][1];
		}
	}
	turboRegProgressBar.skipProgressBar(workload * iterationCost);
} /* end MarquardtLevenbergOptimization */

/*------------------------------------------------------------------*/
private double[] matrixMultiply (
	final double[][] matrix,
	final double[] vector
) {
	final double[] result = new double[matrix.length];
	for (int i = 0; (i < matrix.length); i++) {
		result[i] = 0.0;
		for (int j = 0; (j < vector.length); j++) {
			result[i] += matrix[i][j] * vector[j];
		}
	}
	return(result);
} /* end matrixMultiply */

/*------------------------------------------------------------------*/
private void scaleBottomDownLandmarks (
) {
	for (int depth = 1; (depth < pyramidDepth); depth++) {
		if (transformation == turboRegDialog.RIGID_BODY) {
			for (int n = 0; (n < transformation); n++) {
				sourcePoint[n][0] *= 0.5;
				sourcePoint[n][1] *= 0.5;
				targetPoint[n][0] *= 0.5;
				targetPoint[n][1] *= 0.5;
			}
		}
		else {
			for (int n = 0; (n < (transformation / 2)); n++) {
				sourcePoint[n][0] *= 0.5;
				sourcePoint[n][1] *= 0.5;
				targetPoint[n][0] *= 0.5;
				targetPoint[n][1] *= 0.5;
			}
		}
	}
} /* end scaleBottomDownLandmarks */

/*------------------------------------------------------------------*/
private void scaleUpLandmarks (
) {
	if (transformation == turboRegDialog.RIGID_BODY) {
		for (int n = 0; (n < transformation); n++) {
			sourcePoint[n][0] *= 2.0;
			sourcePoint[n][1] *= 2.0;
			targetPoint[n][0] *= 2.0;
			targetPoint[n][1] *= 2.0;
		}
	}
	else {
		for (int n = 0; (n < (transformation / 2)); n++) {
			sourcePoint[n][0] *= 2.0;
			sourcePoint[n][1] *= 2.0;
			targetPoint[n][0] *= 2.0;
			targetPoint[n][1] *= 2.0;
		}
	}
} /* end scaleUpLandmarks */

/*------------------------------------------------------------------*/
private void translationTransform (
	final double[][] matrix
) {
	double dx = matrix[0][0];
	double dy = matrix[1][0];
	final double dx0 = dx;
	int xMsk;
	int yMsk;
	x = dx - Math.floor(dx);
	y = dy - Math.floor(dy);
	if (!accelerated) {
		xWeights();
		yWeights();
	}
	int k = 0;
	turboRegProgressBar.addWorkload(outNy);
	for (int v = 0; (v < outNy); v++) {
		y = dy++;
		yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
		if ((0 <= yMsk) && (yMsk < inNy)) {
			yMsk *= inNx;
			if (!accelerated) {
				yIndexes();
			}
			dx = dx0;
			for (int u = 0; (u < outNx); u++) {
				x = dx++;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)) {
					xMsk += yMsk;
					if (accelerated) {
						outImg[k++] = inImg[xMsk];
					}
					else {
						xIndexes();
						outImg[k++] = (float)interpolate();
					}
				}
				else {
					outImg[k++] = 0.0F;
				}
			}
		}
		else {
			for (int u = 0; (u < outNx); u++) {
				outImg[k++] = 0.0F;
			}
		}
		turboRegProgressBar.stepProgressBar();
	}
	turboRegProgressBar.workloadDone(outNy);
} /* translationTransform */

/*------------------------------------------------------------------*/
private void translationTransform (
	final double[][] matrix,
	final float[] outMsk
) {
	double dx = matrix[0][0];
	double dy = matrix[1][0];
	final double dx0 = dx;
	int xMsk;
	int yMsk;
	x = dx - Math.floor(dx);
	y = dy - Math.floor(dy);
	if (!accelerated) {
		xWeights();
		yWeights();
	}
	int k = 0;
	turboRegProgressBar.addWorkload(outNy);
	for (int v = 0; (v < outNy); v++) {
		y = dy++;
		yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
		if ((0 <= yMsk) && (yMsk < inNy)) {
			yMsk *= inNx;
			if (!accelerated) {
				yIndexes();
			}
			dx = dx0;
			for (int u = 0; (u < outNx); u++, k++) {
				x = dx++;
				xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
				if ((0 <= xMsk) && (xMsk < inNx)) {
					xMsk += yMsk;
					if (accelerated) {
						outImg[k] = inImg[xMsk];
					}
					else {
						xIndexes();
						outImg[k] = (float)interpolate();
					}
					outMsk[k] = inMsk[xMsk];
				}
				else {
					outImg[k] = 0.0F;
					outMsk[k] = 0.0F;
				}
			}
		}
		else {
			for (int u = 0; (u < outNx); u++, k++) {
				outImg[k] = 0.0F;
				outMsk[k] = 0.0F;
			}
		}
		turboRegProgressBar.stepProgressBar();
	}
	turboRegProgressBar.workloadDone(outNy);
} /* translationTransform */

/*------------------------------------------------------------------*/
private void xDxWeights (
) {
	s = 1.0 - x;
	dxWeight[0] = 0.5 * x * x;
	xWeight[0] = x * dxWeight[0] / 3.0;
	dxWeight[3] = -0.5 * s * s;
	xWeight[3] = s * dxWeight[3] / -3.0;
	dxWeight[1] = 1.0 - 2.0 * dxWeight[0] + dxWeight[3];
	xWeight[1] = 2.0 / 3.0 + (1.0 + x) * dxWeight[3];
	dxWeight[2] = 1.5 * x * (x - 4.0/ 3.0);
	xWeight[2] = 2.0 / 3.0 - (2.0 - x) * dxWeight[0];
} /* xDxWeights */

/*------------------------------------------------------------------*/
private void xIndexes (
) {
	p = (0.0 <= x) ? ((int)x + 2) : ((int)x + 1);
	for (int k = 0; (k < 4); p--, k++) {
		q = (p < 0) ? (-1 - p) : (p);
		if (twiceInNx <= q) {
			q -= twiceInNx * (q / twiceInNx);
		}
		xIndex[k] = (inNx <= q) ? (twiceInNx - 1 - q) : (q);
	}
} /* xIndexes */

/*------------------------------------------------------------------*/
private void xWeights (
) {
	s = 1.0 - x;
	xWeight[3] = s * s * s / 6.0;
	s = x * x;
	xWeight[2] = 2.0 / 3.0 - 0.5 * s * (2.0 - x);
	xWeight[0] = s * x / 6.0;
	xWeight[1] = 1.0 - xWeight[0] - xWeight[2] - xWeight[3];
} /* xWeights */

/*------------------------------------------------------------------*/
private void yDyWeights (
) {
	t = 1.0 - y;
	dyWeight[0] = 0.5 * y * y;
	yWeight[0] = y * dyWeight[0] / 3.0;
	dyWeight[3] = -0.5 * t * t;
	yWeight[3] = t * dyWeight[3] / -3.0;
	dyWeight[1] = 1.0 - 2.0 * dyWeight[0] + dyWeight[3];
	yWeight[1] = 2.0 / 3.0 + (1.0 + y) * dyWeight[3];
	dyWeight[2] = 1.5 * y * (y - 4.0/ 3.0);
	yWeight[2] = 2.0 / 3.0 - (2.0 - y) * dyWeight[0];
} /* yDyWeights */

/*------------------------------------------------------------------*/
private void yIndexes (
) {
	p = (0.0 <= y) ? ((int)y + 2) : ((int)y + 1);
	for (int k = 0; (k < 4); p--, k++) {
		q = (p < 0) ? (-1 - p) : (p);
		if (twiceInNy <= q) {
			q -= twiceInNy * (q / twiceInNy);
		}
		yIndex[k] = (inNy <= q) ? ((twiceInNy - 1 - q) * inNx) : (q * inNx);
	}
} /* yIndexes */

/*------------------------------------------------------------------*/
private void yWeights (
) {
	t = 1.0 - y;
	yWeight[3] = t * t * t / 6.0;
	t = y * y;
	yWeight[2] = 2.0 / 3.0 - 0.5 * t * (2.0 - y);
	yWeight[0] = t * y / 6.0;
	yWeight[1] = 1.0 - yWeight[0] - yWeight[2] - yWeight[3];
} /* yWeights */

} /* end class turboRegTransform */
