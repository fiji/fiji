package trainableSegmentation;

/**
*
* License: GPL
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License 2
* as published by the Free Software Foundation.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*
* Authors: Ignacio Arganda-Carreras (iarganda@mit.edu), 
*/

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.SingularValueDecompositionImpl;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * This class implements methods to calculate the principal
 * components of an image. 
 *  
 * @author Ignacio Arganda-Carreras (iarganda at mit dot edu)
 *
 */
public class PrincipalComponentAnalysis 
{

	public static ImagePlus getPrincipalComponents(
			final ImagePlus inputImage,
			final int patchSize,
			final int step)
	{
		final int maxX = (inputImage.getWidth() - patchSize);
		final int maxY = (inputImage.getHeight() - patchSize);
		
		final int matrixHeight = patchSize * patchSize;
		final int matrixWidth = (maxX/step+1) * (maxY/step+1) * inputImage.getImageStackSize();
		
		final double[][] matrix = new double [ matrixWidth ] [ matrixHeight ];
		
		int n = 0;
		
		for(int i=1;  i<= inputImage.getImageStack().getSize(); i++)
		{
			final ImageProcessor ip = inputImage.getImageStack().getProcessor(i).convertToFloat();
			
			for(int j=0; j < maxX; j+=step)
				for(int k=0; k < maxY; k+=step)
				{
					final Roi roi = new Roi(j, k, patchSize, patchSize);
					ip.setRoi(roi);
					final ImageProcessor patch = ip.crop();
					float[] pixels = (float[])patch.getPixels();
					
					for(int l=0; l<matrixHeight; l++)
						matrix[n][l] = pixels[l];
					
					n++;
				}
							
		}
			
		final Array2DRowRealMatrix m = new Array2DRowRealMatrix(matrix); 
		
		// Calculate SVD and get V
		final long start = System.currentTimeMillis();

		final SingularValueDecompositionImpl svd = new SingularValueDecompositionImpl( m ); 
		
		final RealMatrix v = svd.getV();

		final long end = System.currentTimeMillis();
		IJ.log("SVD took: " + (end-start) + "ms");
		
		final ImageStack result = new ImageStack(patchSize, patchSize);
		
		for(int i=0; i<v.getRowDimension(); i++)
		{
			final double[] column = new double[v.getColumnDimension()];
			for(int j=0; j<v.getColumnDimension(); j++)
				column[j] = v.getEntry(j, i);
			result.addSlice("PCA " + i, new FloatProcessor(patchSize, patchSize, column));
		}
		
		return new ImagePlus("PCA", result);
		
	}
		
	
}
