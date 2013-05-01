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

import ij.IJ;
import ij.ImagePlus;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import mpicbg.imglib.algorithm.pde.AnisotropicDiffusion;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RealType;

/**
 * TODO
 *
 */
public class AnistotropicDiffusion_TestDrive {

	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void main(String[] args) throws InterruptedException, URISyntaxException, MalformedURLException {
	
		ij.ImageJ.main(args);
		
		ImagePlus imp = IJ.openImage("http://rsb.info.nih.gov/ij/images/boats.gif");
		
		Image<? extends RealType> source = ImageJFunctions.wrap(imp);
		
		AnisotropicDiffusion<?> algo = new AnisotropicDiffusion(source, 2, 10);
//		AnisotropicDiffusion<?> algo = new AnisotropicDiffusion(source, 1, new AnisotropicDiffusion.WideRegionEnhancer(20));
		algo.setNumThreads();
		
		if (!algo.checkInput()) {
			System.out.println("Check input failed! With: "+algo.getErrorMessage());
			return;
		}
		
		imp.show();

		int niter = 10;
		algo.setDimensions(new int[] { 1 } );
		for (int i = 0; i < niter; i++) {
			System.out.println("Iteration "+(i+1)+" of "+niter+".");
			algo.process();
			imp.updateAndDraw();
			Thread.sleep(500);
		}
		
		System.out.println("Done in "+algo.getProcessingTime()+" ms.");

		

	}
}
