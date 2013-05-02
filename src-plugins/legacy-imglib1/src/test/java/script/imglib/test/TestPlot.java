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

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import script.imglib.ImgLib;
import script.imglib.analysis.Histogram;

/**
 * TODO
 *
 */
public class TestPlot {
	static public final void main(String[] args) {
		//String src = "http://imagej.nih.gov/ij/images/bridge.gif";
		String src = "/home/albert/Desktop/t2/bridge.gif";
		
		Image<UnsignedByteType> img = ImgLib.<UnsignedByteType>open(src);

		try {
			 // Show the histogram in a JFrame
			new Histogram<UnsignedByteType>(img).asChart(true);
			
			// Show the histogram as an imglib image in a virtual ImageJ display
			ImgLib.wrap(new Histogram<UnsignedByteType>(img).asImage()).show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
