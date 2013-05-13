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

package script.imglib.test;

import mpicbg.imglib.algorithm.gauss.GaussianConvolutionReal;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import mpicbg.imglib.type.numeric.real.FloatType;
import script.imglib.ImgLib;
import script.imglib.math.ASin;
import script.imglib.math.Abs;
import script.imglib.math.Add;
import script.imglib.math.Cbrt;
import script.imglib.math.Compute;
import script.imglib.math.Difference;
import script.imglib.math.Divide;
import script.imglib.math.Multiply;
import script.imglib.math.Pow;
import script.imglib.math.Sin;
import script.imglib.math.Sqrt;
import script.imglib.math.Subtract;

/* Tested in a MacBookPro 5,5, 4 Gb RAM, 2.4 Ghz
 * running Ubuntu 10.04 with Java 1.6.0_21
 *
 * 2010-11-24
 *
Opening '/home/albert/Desktop/t2/bridge.gif' [512x512x1 type=uint8 image=Image<ByteType>]
LOCI.openLOCI(): Cannot read metadata, setting calibration to 1
Gauss processing time: 2060
Start direct (correct illumination)...
  elapsed: 108.590441  image: result
Start script (correct illumination)...
  elapsed: 56.862998 image result
Start direct (correct illumination)...
  elapsed: 138.54601  image: result
Start script (correct illumination)...
  elapsed: 18.287601 image result
Start direct (correct illumination)...
  elapsed: 7.65112  image: result
Start script (correct illumination)...
  elapsed: 15.75756 image result
Start direct (correct illumination)...
  elapsed: 8.854321  image: result
Start script (correct illumination)...
  elapsed: 15.40572 image result
Start direct with heavy operations...
  elapsed: 361
Start script with heavy operations...
  elapsed: 390
Start direct with heavy operations...
  elapsed: 402
Start script with heavy operations...
  elapsed: 352
Start direct with heavy operations...
  elapsed: 346
Start script with heavy operations...
  elapsed: 355
Start direct with heavy operations...
  elapsed: 349
Start script with heavy operations...
  elapsed: 372
Start differenceFn
  elapsed: 17.939521
Start differenceCompFn
  elapsed: 25.551281
Start differenceFn
  elapsed: 15.748361
Start differenceCompFn
  elapsed: 10.02808
Start differenceFn
  elapsed: 6.3574
Start differenceCompFn
  elapsed: 9.96048
Start differenceFn
  elapsed: 5.7024
Start differenceCompFn
  elapsed: 6.0376
Original pixel at 348,95: 190.0
After varargs addition, pixel at 348,95: 760.0 which is 4 * val: true

In conclusion: the scripting way is about 1.8x slower for relatively simple operations,
but about 1x for heavy operations!

 *
 */
public class Benchmark {

	static public final void p(String s) {
		System.out.println(s);
	}

	static public Image<FloatType> scriptCorrectIllumination(
			final Image<? extends RealType<?>> img,
			final Image<? extends RealType<?>> brightfield,
			final Image<? extends RealType<?>> darkfield,
			final double mean) throws Exception {
		p("Start script (correct illumination)...");
		long t0 = System.nanoTime();
		Image<FloatType> corrected = Compute.inFloats(1,
				new Multiply(
						new Divide(
								new Subtract(img, brightfield),
								new Subtract(brightfield, darkfield)),
						mean));
		p("  elapsed: " + (System.nanoTime() - t0)/1000000.0 + " image " + corrected.getName() );
		return corrected;
	}

	static public Image<FloatType> correctIllumination(
			final Image<? extends RealType<?>> img,
			final Image<? extends RealType<?>> brightfield,
			final Image<? extends RealType<?>> darkfield,
			final double mean) {
		p("Start direct (correct illumination)...");
		long t0 = System.nanoTime();
		ImageFactory<FloatType> factory = new ImageFactory<FloatType>(new FloatType(), img.getContainerFactory());
		Image<FloatType> corrected = factory.createImage(img.getDimensions(), "result");
		final Cursor<FloatType> c = corrected.createCursor();
		final Cursor<? extends RealType<?>> ci = img.createCursor(),
											cb = brightfield.createCursor(),
											cd = darkfield.createCursor();
		while (c.hasNext()) {
			c.fwd();
			ci.fwd();
			cb.fwd();
			cd.fwd();
			c.getType().setReal( (  (ci.getType().getRealDouble() - cb.getType().getRealDouble())
								  / (cb.getType().getRealDouble() - cd.getType().getRealDouble()))
								 * mean);
		}
		c.close();
		ci.close();
		cb.close();
		cd.close();
		p("  elapsed: " + (System.nanoTime() - t0)/1000000.0 + "  image: " + corrected.getName());
		return corrected;
	}

	static public Image<FloatType> scriptHeavyOperations(
			final Image<? extends RealType<?>> img) throws Exception {
		p("Start script with heavy operations...");
		long t0 = System.currentTimeMillis();
		Image<FloatType> corrected = Compute.inFloats(1, 
				new Multiply(
					new ASin(
						new Sin(
							new Divide(
								new Pow(new Sqrt(img), 2),
								new Pow(new Cbrt(img), 3)))),
					img));
		p("  elapsed: " + (System.currentTimeMillis() - t0));
		return corrected;
	}

	static public Image<FloatType> heavyOperations(
			final Image<? extends RealType<?>> img) {
		p("Start direct with heavy operations...");
		long t0 = System.currentTimeMillis();
		ImageFactory<FloatType> factory = new ImageFactory<FloatType>(new FloatType(), img.getContainerFactory());
		Image<FloatType> corrected = factory.createImage(img.getDimensions(), "result");
		final Cursor<FloatType> c = corrected.createCursor();
		final Cursor<? extends RealType<?>> ci = img.createCursor();
		while (c.hasNext()) {
			c.fwd();
			ci.fwd();
			c.getType().setReal(
					Math.asin(
						Math.sin(
							Math.pow(Math.sqrt(ci.getType().getRealDouble()), 2)
							/ Math.pow(Math.cbrt(ci.getType().getRealDouble()), 3)))
					* ci.getType().getRealDouble());					
		}
		c.close();
		ci.close();
		p("  elapsed: " + (System.currentTimeMillis() - t0));
		return corrected;
	}
	
	static public Image<FloatType> sum(
			final Image<? extends RealType<?>> img) throws Exception {
		LocalizableByDimCursor<? extends RealType<?>> c = img.createLocalizableByDimCursor();
		c.setPosition(new int[]{348, 95});
		System.out.println("Original pixel at 348,95: " + c.getType().getRealDouble());

		Image<FloatType> result = Compute.inFloats(new Add(img, img, img, img));

		LocalizableByDimCursor<? extends RealType<?>> r = result.createLocalizableByDimCursor();
		r.setPosition(new int[]{348, 95});
		System.out.println("After varargs addition, pixel at 348,95: " + r.getType().getRealDouble()
				+ " which is 4 * val: " + (c.getType().getRealDouble() * 4 == r.getType().getRealDouble()));

		img.removeAllCursors();
		result.removeAllCursors();
		
		return result;
	}

	static public Image<FloatType> differenceFn(
			final Image<? extends RealType<?>> img) throws Exception {
		p("Start differenceFn");
		long t0 = System.nanoTime();
		try {
			return Compute.inFloats(new Difference(img, img));
		} finally {
			p("  elapsed: " + (System.nanoTime() - t0)/1000000.0);
		}
	}

	static public Image<FloatType> differenceCompFn(
			final Image<? extends RealType<?>> img) throws Exception {
		p("Start differenceCompFn");
		long t0 = System.nanoTime();
		try {
			return Compute.inFloats(new Abs(new Subtract(img, img)));
		} finally {
			p("  elapsed: " + (System.nanoTime() - t0)/1000000.0);
		}
	}

	public static void main(String[] args) {
		try {
			String src = "http://imagej.nih.gov/ij/images/bridge.gif";
			//String src = "/home/albert/Desktop/t2/bridge.gif";
			//Image<UnsignedByteType> img = LOCI.openLOCIUnsignedByteType(src, new ArrayContainerFactory());
			Image<UnsignedByteType> img = ImgLib.open(src);
			//
			double mean = 0;
			for (final UnsignedByteType t : img) mean += t.getRealDouble();
			mean /= img.size();
			//

			GaussianConvolutionReal<UnsignedByteType> gauss = new GaussianConvolutionReal<UnsignedByteType>( img, new OutOfBoundsStrategyMirrorFactory<UnsignedByteType>(), 60 );
			gauss.process();
			
			System.out.println( "Gauss processing time: " + gauss.getProcessingTime() );
			
			Image<UnsignedByteType> brightfield = gauss.getResult();
			
			/*
			DownSample<UnsignedByteType> downSample = new DownSample<UnsignedByteType>( img, 0.25f );			
			downSample.process();
			Image<UnsignedByteType> down = downSample.getResult();
			
			ImageJFunctions.show( down );
			
			AffineModel2D model = new AffineModel2D();
			model.set( 4.03f, 0, 0, 4.03f, 0, 0 );
			
			ImageTransform<UnsignedByteType> imgTransform = new ImageTransform<UnsignedByteType>( brightfield, model, new LinearInterpolatorFactory<UnsignedByteType>( new OutOfBoundsStrategyMirrorFactory<UnsignedByteType>()) );
			imgTransform.process();
			
			brightfield = imgTransform.getResult();
			*/
			
			ImageJFunctions.show( img );
			ImageJFunctions.show( brightfield );
						
			//
			Image<UnsignedByteType> darkfield = img.createNewImage(); // empty

			// Test:
			for (int i=0; i<4; i++) {
				correctIllumination(img, brightfield, darkfield, mean);
				Image<FloatType> corrected = scriptCorrectIllumination(img, brightfield, darkfield, mean);
				
				if ( i == 0 )
					ImageJFunctions.show( corrected );
					
			}

			for (int i=0; i<4; i++) {
				heavyOperations(img);
				scriptHeavyOperations(img);
			}

			// Compare Difference(img) vs. Abs(Subtract(img1, img2))
			for (int i=0; i<4; i++) {
				differenceFn(img);
				differenceCompFn(img);
			}

			// Test varargs:
			sum(img);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
