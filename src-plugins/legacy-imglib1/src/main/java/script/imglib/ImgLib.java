/*
 * #%L
 * ImgLib: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2013 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package script.imglib;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RealType;

/* TODO license? */

/**
 * A simple wrapper class that is supposed to contain only functions for scripting.
 *
 * To make things very scriptable, the only exception thrown is a RuntimeException, and
 * the corresponding stack traces are output to stderr.
 * 
 * @version 1.0 2010-12-07
 * @see Image
 *
 * @author Johannes Schindelin
 * @author Albert Cardona
 */
public class ImgLib {
	/** Open an image from a file path or a web URL. */
	public static<T extends RealType<T>> Image<T> open(String pathOrURL) {
		try {
			// In the future, when dimensions can be called by name properly:
			//return new ImageOpener().<T>openImage(pathOrURL);
			// For now:
			return wrap(IJ.openImage(pathOrURL));
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Got I/O exception: " + e, e);
		}
	}

	// TODO virtual images with ImgLib.
	// TODO At least planes, like ImageJ's VirtualStack.
	// TODO One should be able to define the minimum X*Y*Z*T*etc block unit
	//      to have loaded at any given time. This could be the cell storage strategy,
	//      where each cell is paged in and out.
	/** //The PlanarContainer grabs the native array, so it's not virtual anymore.
	static public final <R extends RealType<R>> Image<R> openVirtual(final String filepath) throws FormatException, IOException {
		ChannelSeparator r = new ChannelSeparator();
		r.setId(filepath);
		BFVirtualStack bfv = new BFVirtualStack(filepath, r, false, false, false);
		return ImgLib.wrap(new ImagePlus(filepath, bfv));
	}
	*/

	/** Wrap an ImageJ's {@link ImagePlus} as an Imglib {@link Image} of the appropriate type.
	 * The data is not copied, but merely accessed with a PlanarArrayContainer.
	 * @see ImagePlusAdapter */
	public static<T extends RealType<T>> Image<T> wrap(ImagePlus imp) {
		return ImagePlusAdapter.<T>wrap(imp);
	}

	/** Wrap an Imglib's {@link Image} as an ImageJ's {@link ImagePlus} of the appropriate type.
	 * The data is not copied, but accessed with a special-purpose VirtualStack subclass. */
	static public final ImagePlus wrap(final Image<?> img) {
		return ImageJFunctions.displayAsVirtualStack(img);
	}

	/** Save an image in the appropriate file format according to
	 * the filename extension specified in {@param path}. */
	public static<T extends RealType<T>> boolean save(Image<T> image, String path) {
		int dot = path.lastIndexOf('.');
		if (dot < 0 || path.length() - dot - 1 > 4)
			throw new RuntimeException("Could not infer file type from filename: " + path);
		return save(image, path.substring(dot + 1), path);
	}

	/** Save an image in the format specified by {@param fileType}, which can be any of:
	 *  "tif", "tiff", "zip", "gif", "jpg", "jpeg", "bmp", "pgm", "png", "raw".
	 *  
	 *  When saving as TIFF, if the image has more than 2 dimensions, it will be saved
	 *  as a stack. */
	public static<T extends RealType<T>> boolean save(Image<T> image, String fileType, String path) {
		// TODO: use LOCI for this
		ImagePlus imp = ImageJFunctions.displayAsVirtualStack(image);
		FileSaver saver = new FileSaver(imp);
		fileType = fileType.toLowerCase();
		if (fileType.equals("tif") || fileType.equals("tiff")) {
			if (image.getNumDimensions() > 2) {
				return saver.saveAsTiffStack(path);
			} else {
				return saver.saveAsTiff(path);
			}
		} else if (fileType.equals("zip"))
			return saver.saveAsZip(path);
		else if (fileType.equals("gif"))
			return saver.saveAsGif(path);
		else if (fileType.equals("jpg") || fileType.equals("jpeg"))
			return saver.saveAsJpeg(path);
		else if (fileType.equals("bmp"))
			return saver.saveAsBmp(path);
		else if (fileType.equals("pgm"))
			return saver.saveAsPgm(path);
		else if (fileType.equals("png"))
			return saver.saveAsPng(path);
		else if (fileType.equals("raw"))
			return saver.saveAsRaw(path);
		else
			throw new RuntimeException("Unknown fileformat: " + fileType);
	}
}
