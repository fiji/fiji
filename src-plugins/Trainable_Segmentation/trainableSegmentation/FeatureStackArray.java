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
 * Authors: Ignacio Arganda-Carreras (iarganda@mit.edu), Verena Kaynig (verena.kaynig@inf.ethz.ch),
 *          Albert Cardona (acardona@ini.phys.ethz.ch)
 */

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
	
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This class stores the feature stacks of a set of input slices.
 * It can be used so for 2D stacks or as the container of 3D features (by
 * using a feature stack per section). 
 * 
 * @author Ignacio Arganda-Carreras (iarganda@mit.edu)
 *
 */
public class FeatureStackArray 
{
	/** array of feature stacks */
	private FeatureStack featureStackArray[];
	
	/** index of the feature stack that is used as reference (to read attribute, etc.).
	 * -1 if not definded yet. */
	private int referenceStackIndex = -1;
	
	/** minmum sigma/radius used in the filters */
	private float minimumSigma = 1;
	/** maximum sigma/radius used in the filters */
	private float maximumSigma = 16;
	/** use neighborhood flag */
	private boolean useNeighbors = false;
	/** expected membrane thickness (in pixels) */
	private int membraneThickness = 1;	
	/** size of the patch to use to enhance membranes (in pixels, NxN) */
	private int membranePatchSize = 19;
	/** common enabled features */
	private boolean[] enabledFeatures = null;
	
	/**
	 * Initialize a feature stack list of a specific size
	 * 
	 * @param num number of elements in the list
	 * @param minimumSigma
	 * @param maximumSigma
	 * @param useNeighbors
	 * @param membraneSize
	 * @param membranePatchSize
	 * @param enabledFeatures
	 */
	public FeatureStackArray(
			final int num,
			final float minimumSigma,
			final float maximumSigma,
			final boolean useNeighbors,
			final int membraneSize,
			final int membranePatchSize,
			final boolean[] enabledFeatures)
	{
		this.featureStackArray = new FeatureStack[num]; 
		this.minimumSigma = minimumSigma;
		this.maximumSigma = maximumSigma;
		this.useNeighbors = useNeighbors;
		this.membraneThickness = membraneSize;
		this.membranePatchSize = membranePatchSize;
		this.enabledFeatures = enabledFeatures;
	}
	
	/**
	 * Create a feature stack array based on specific filters
	 * 
	 * @param inputImage original image
	 * @param filters stack of filters to apply to the original image in order to create the features
	 */
	public FeatureStackArray(
			final ImagePlus inputImage,
			final ImagePlus filters)
	{
		this.featureStackArray = new FeatureStack[ inputImage.getImageStackSize() ];
		
		
		for(int i=1; i <= featureStackArray.length; i++)
		{		
			featureStackArray[ i-1 ] = new FeatureStack(new ImagePlus("slice " + i, inputImage.getImageStack().getProcessor(i)));			
			featureStackArray[ i-1 ].addFeaturesMT( filters );			 
		}
	}
	
	/**
	 * Get the number of feature stacks
	 * 
	 * @return number of feature stacks stored in the array
	 */
	public int getSize()
	{
		return this.featureStackArray.length;
	}
	
	/**
	 * Get n-th stack in the array (remember n>=0)
	 * @param n position of the stack to get
	 * @return feature stack of the corresponding slice
	 */
	public FeatureStack get(int n)
	{
		return featureStackArray[n];
	}
	
	/**
	 * Set a member of the list
	 * @param fs new feature stack  
	 * @param index index of the new feature stack in the array
	 */
	public void set(FeatureStack fs, int index)
	{
		this.featureStackArray[ index ] = fs;
	}
	
	/**
	 * Update specific feature stacks in the list (multi-thread fashion)
	 * 
	 * @param update boolean array indicating which feature stack to update
	 * @return false if any feature stack was not properly updated
	 */
	public boolean updateFeaturesMT(boolean[] update)
	{
		if (Thread.currentThread().isInterrupted() )
			return false;
		
		final int numProcessors = Prefs.getThreads();
		final ExecutorService exe = Executors.newFixedThreadPool( numProcessors );
		
		final ArrayList< Future<Boolean> > futures = new ArrayList< Future<Boolean> >();
		
		try{
			for(int i=0; i<featureStackArray.length; i++)
			{
				if(null != featureStackArray[i])
					if(update[i])
					{
						IJ.log("Updating features of slice number " + (i+1) + "...");						
						featureStackArray[i].setEnabledFeatures(enabledFeatures);
						featureStackArray[i].setMembranePatchSize(membranePatchSize);
						featureStackArray[i].setMembraneSize(membraneThickness);
						featureStackArray[i].setMaximumSigma(maximumSigma);
						featureStackArray[i].setMinimumSigma(minimumSigma);
						featureStackArray[i].setUseNeighbors(useNeighbors);
						if ( featureStackArray.length == 1 )
						{
							if(false == featureStackArray[i].updateFeaturesMT() )							
								return false;							
						}
						else
							futures.add(exe.submit( updateFeatures( featureStackArray[i] ) ));

						if(referenceStackIndex == -1)
							this.referenceStackIndex = i;
					}
			}
			
			// Wait for the jobs to be done
			for(Future<Boolean> f : futures)
			{
				final boolean result = f.get();
				if(false == result)
					return false;
			}			
		}
		catch (InterruptedException e) 
		{
			IJ.log("The feature update was interrupted by the user.");
			exe.shutdownNow();
			return false;
		}
		catch(Exception ex)
		{
			IJ.log("Error when updating feature stack array.");
			ex.printStackTrace();
			exe.shutdownNow();
			return false;
		}
		finally{
			exe.shutdown();
		}	
		
		
		return true;
	}

	/**
	 * Update all feature stacks in the list (multi-thread fashion) 
	 */
	public boolean updateFeaturesMT()
	{
		final int numProcessors = Prefs.getThreads();
		final ExecutorService exe = Executors.newFixedThreadPool( numProcessors );
		//IJ.log("Num of processors = " + numProcessors);
		
		final ArrayList< Future<Boolean> > futures = new ArrayList< Future<Boolean> >();
		
		IJ.showStatus("Updating features...");
		
		try{
			for(int i=0; i<featureStackArray.length; i++)
			{
				if(null != featureStackArray[i])
				{
					IJ.log("Updating features of slice number " + (i+1) + "...");
					//System.out.println("Updating features of slice number " + (i+1));
					featureStackArray[i].setEnabledFeatures(enabledFeatures);
					featureStackArray[i].setMembranePatchSize(membranePatchSize);
					featureStackArray[i].setMembraneSize(membraneThickness);
					featureStackArray[i].setMaximumSigma(maximumSigma);
					featureStackArray[i].setMinimumSigma(minimumSigma);
					featureStackArray[i].setUseNeighbors(useNeighbors);
					if ( featureStackArray.length == 1 )
					{
						if(false == featureStackArray[i].updateFeaturesMT() )						
							return false;						
					}
					else
						futures.add(exe.submit( updateFeatures( featureStackArray[i] ) ));

					if(referenceStackIndex == -1)
						this.referenceStackIndex = i;
				}
			}
			
			// Wait for the jobs to be done
			int currentIndex = 0;
			final int finalIndex = featureStackArray.length;
			for(Future<Boolean> f : futures)
			{
				final boolean result = f.get();
				currentIndex++;
				IJ.showStatus("Updating features...");
				IJ.showProgress(currentIndex, finalIndex);
				if(false == result)
					return false;
			}			
		
		} 
		catch (InterruptedException e) 
		{
			IJ.log("The feature update was interrupted by the user.");
			IJ.showStatus("The feature update was interrupted by the user.");
			IJ.showProgress(1.0);
			exe.shutdownNow();
			return false;
		}
		catch(Exception ex)
		{
			IJ.log("Error when updating feature stack array.");
			ex.printStackTrace();
			exe.shutdownNow();
			return false;
		}
		finally{
			exe.shutdown();
		}	
		
		
		return true;
	}
	
	/**
	 * Update features of a feature stack (to be submitted to an Executor Service)
	 * 
	 * @param fs feature stack to be updated
	 * @return true if everything went correct
	 */
	public Callable<Boolean> updateFeatures(
			final FeatureStack fs)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<Boolean>(){
			public Boolean call(){
				return fs.updateFeaturesST();
			}
		};
	}
	
	
	/**
	 * Reset the reference index (used when the are 
	 * changes in the features)
	 */
	public void resetReference()
	{
		this.referenceStackIndex = -1;
	}
	
	/**
	 * Shut down the executor service
	 */
	public void shutDownNow()
	{
		for(int i=0; i<featureStackArray.length; i++)
			if(null != featureStackArray[i])
			{
				featureStackArray[i].shutDownNow();
			}
	}
	
	/**
	 * Check if the array has not been yet initialized
	 * 
	 * @return true if the array has been initialized
	 */
	public boolean isEmpty() 
	{
		for(int i=0; i<getSize(); i++)
			if(featureStackArray[i].getSize()>1)
				return false;
		return true;
	}

	/**
	 * Get the number of features of the reference stack (consistent all along the array)
	 * @return number of features on each feature stack of the array
	 */
	public int getNumOfFeatures() {
		if(referenceStackIndex == -1)
			return -1;
		return featureStackArray[referenceStackIndex].getSize();
	}
	
	/**
	 * Get a specific label of the reference stack
	 * @param index slice index (>=1)
	 * @return
	 */
	public String getLabel(int index)
	{
		if(referenceStackIndex == -1)
			return null;
		return featureStackArray[referenceStackIndex].getSliceLabel(index);
	}
	
	/**
	 * Get the features enabled for the reference stack
	 * @return features to be calculated on each stack
	 */
	public boolean[] getEnabledFeatures()
	{
		if(referenceStackIndex == -1)
			return this.enabledFeatures;
		return featureStackArray[referenceStackIndex].getEnabledFeatures();
	}

	/**
	 * Set the features enabled for the reference stack
	 * @param newFeatures boolean flags for the features to use
	 */
	public void setEnabledFeatures(boolean[] newFeatures) 
	{
		this.enabledFeatures = newFeatures;
		if(referenceStackIndex != -1)
			featureStackArray[referenceStackIndex].setEnabledFeatures(newFeatures);
	}

	public void setMembraneSize(int thickness) 
	{
		this.membraneThickness = thickness;
		if(referenceStackIndex != -1)
			featureStackArray[referenceStackIndex].setMembraneSize(thickness);
	}

	public void setMembranePatchSize(int patchSize) 
	{
		this.membranePatchSize = patchSize;
		if(referenceStackIndex != -1)
			featureStackArray[referenceStackIndex].setMembranePatchSize(patchSize);
	}

	public void setMaximumSigma(float sigma) 
	{
		this.maximumSigma = sigma;
		if(referenceStackIndex != -1)
			featureStackArray[referenceStackIndex].setMaximumSigma(sigma);		
	}

	public void setMinimumSigma(float sigma) 
	{
		this.minimumSigma = sigma;
		if(referenceStackIndex != -1)
			featureStackArray[referenceStackIndex].setMinimumSigma(sigma);
	}

	public void setUseNeighbors(boolean useNeighbors) 
	{
		this.useNeighbors = useNeighbors;
		if(referenceStackIndex != -1)
			featureStackArray[referenceStackIndex].setUseNeighbors(useNeighbors);
	}

	public boolean useNeighborhood() {
		if(referenceStackIndex != -1)
			return featureStackArray[referenceStackIndex].useNeighborhood();
		return useNeighbors;
	}
	
	public int getReferenceSliceIndex()
	{
		return referenceStackIndex;
	}
	
	public int getWidth()
	{
		if(referenceStackIndex != -1)
			return featureStackArray[referenceStackIndex].getWidth();
		return -1;
	}
	
	public int getHeight()
	{
		if(referenceStackIndex != -1)
			return featureStackArray[referenceStackIndex].getHeight();
		return -1;
	}
	
}

	