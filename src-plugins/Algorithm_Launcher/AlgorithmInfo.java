//
// AlgorithmInfo.java
//

/*
Simple imglib algorithm launcher.
Copyright (c) 2010, UW-Madison LOCI.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the UW-Madison LOCI nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY UW-MADISON LOCI ''AS IS'' AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL UW-MADISON LOCI BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;

import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.type.numeric.RealType;

/** Helper class for storing algorithm details. */
public class AlgorithmInfo<T extends RealType<T>> {

	// -- Fields --

	private String label;
	private Class<?>[] argTypes;
	private String[] argNames;
	private String[] argValues;
	private Constructor<? extends OutputAlgorithm> con;

	// -- Constructor --

	public AlgorithmInfo(String label,
		Class<?>[] argTypes, String[] argNames, String[] argValues,
		Constructor<? extends OutputAlgorithm> con)
	{
		this.label = label;
		this.argTypes = argTypes;
		this.argNames = argNames;
		this.argValues = argValues;
		this.con = con;
	}

	// -- AlgorithmInfo API methods --

	public String getLabel() {
		return label;
	}

	public Image<T> run(Image<T> input) {
		OutputAlgorithm<T> alg = create(input);
		boolean compatible = alg.checkInput();
		if (!compatible) {
			IJ.error("Algorithm is incompatible: " + alg.getErrorMessage());
			return null;
		}
		boolean success = alg.process();
		if (!success) {
			IJ.error("Algorithm unsuccessful: " + alg.getErrorMessage());
			return null;
		}
		return alg.getResult();
	}

	// -- Helper methods --

	private OutputAlgorithm<T> create(Image<T> img) {
		Object[] args;
		if (argTypes.length == 0) {
			// no arguments
			args = new Object[0];
		}
		else if (argTypes.length == 1 &&
			Image.class.isAssignableFrom(argTypes[0]))
		{
			// only argument is the image itself
			args = new Object[] {img};
		}
		else {
			// multiple arguments; need to prompt
			args = promptArgs(img);
		}

		// instantiate the algorithm
		Exception exc = null;
		try {
			OutputAlgorithm<T> alg = con.newInstance(args);
			return alg;
		}
		catch (InstantiationException e) { exc = e; }
		catch (IllegalAccessException e) { exc = e; }
		catch (InvocationTargetException e) { exc = e; }
		if (exc != null) IJ.handleException(exc);
		return null;
	}

	private Object[] promptArgs(Image<T> img) {
		Object[] args = new Object[argTypes.length];
		ImagePlus[] images = null;
		String[] imageNames = null;

		// build generic dialog
		GenericDialog gd = new GenericDialog("Choose parameters");
		boolean firstImage = true;
		for (int i=0; i<argTypes.length; i++) {
			Class<?> type = argTypes[i];
			String label = argNames[i];
			String value = argValues[i];
			if (type == boolean.class || type == Boolean.class) {
				gd.addCheckbox(label, false);
			}
			else if (type == byte.class || type == Byte.class ||
				type == int.class || type == Integer.class ||
				type == long.class || type == Long.class ||
				type == short.class || type == Short.class ||
				type == float.class || type == Float.class ||
				type == double.class || type == Double.class)
			{
				int dot = value.indexOf(".");
				int digits = dot < 0 ? 0 : (value.length() - dot - 1);
				double defaultValue = 0;
				try {
					defaultValue = Double.parseDouble(value);
				}
				catch (NumberFormatException exc) { }
				gd.addNumericField(label, defaultValue, digits);
			}
			else if (type == char.class || type == Character.class ||
				type == String.class ||
				type == BigDecimal.class || type == BigInteger.class)
			{
				gd.addStringField(label, value);
			}
			else if (type == Image.class) {
				if (firstImage) firstImage = false; // use img
				else {
					if (images == null) {
						// create list of images
						int nImages = WindowManager.getImageCount();
						images = new ImagePlus[nImages];
						imageNames = new String[nImages];
						for (int j=0; j<images.length; j++) {
							images[j] = WindowManager.getImage(j + 1);
							imageNames[j] = (j + 1) + " " + images[j].getTitle();
						}
					}
					gd.addChoice(label, imageNames, imageNames[0]);
				}
			}
			else {
				IJ.error("Unsupported argument type: " + type.getName());
				return null;
			}
		}
		gd.showDialog();
		if (gd.wasCanceled()) return null;

		// harvest results
		firstImage = true;
		for (int i=0; i<args.length; i++) {
			Class<?> type = argTypes[i];
			if (type == boolean.class || type == Boolean.class) {
				args[i] = new Boolean(gd.getNextBoolean());
			}
			else if (type == byte.class || type == Byte.class) {
				args[i] = new Byte((byte) gd.getNextNumber());
			}
			else if (type == int.class || type == Integer.class) {
				args[i] = new Integer((int) gd.getNextNumber());
			}
			else if (type == long.class || type == Long.class) {
				args[i] = new Long((long) gd.getNextNumber());
			}
			else if (type == short.class || type == Short.class) {
				args[i] = new Short((short) gd.getNextNumber());
			}
			else if (type == float.class || type == Float.class) {
				args[i] = new Float((float) gd.getNextNumber());
			}
			else if (type == double.class || type == Double.class) {
				args[i] = new Double((double) gd.getNextNumber());
			}
			else if (type == char.class || type == Character.class) {
				String s = gd.getNextString();
				char c = s.length() == 0 ? '\0' : s.charAt(0);
				args[i] = new Character(c);
			}
			else if (type == String.class) {
				args[i] = gd.getNextString();
			}
			else if (type == BigDecimal.class) {
				args[i] = new BigDecimal(gd.getNextString());
			}
			else if (type == BigInteger.class) {
				args[i] = new BigInteger(gd.getNextString());
			}
			else if (type == Image.class) {
				if (firstImage) {
					args[i] = img;
					firstImage = false;
				}
				else {
					int index = gd.getNextChoiceIndex();
					ImagePlus imp = images[index];
					Image<T> img2 = ImagePlusAdapter.wrap(imp);
					args[i] = img2;
				}
			}
		}
		return args;
	}

}
