package mpicbg.spim.fusion.entropy;

import mpicbg.imglib.container.ContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor3D;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.real.FloatType;

public class EntropyFloatArray3D
{
	// final variables
	public final float[] preComputed;
	public final int[] absFreq;
	public boolean outOfImageX, outOfImageY, outOfImageZ;  
	public final Image<FloatType> img;
	public final int histogramBins, windowSizeX, windowSizeY, windowSizeZ;
	public final float size;
	public final int windowSizeXHalf, windowSizeYHalf, windowSizeZHalf;
	
	final LocalizableByDimCursor3D<FloatType> cIn, cOut;

	// changing variables
	private float entropy;
	private int x, y, z;
	
	final public float getEntropy() { return entropy; }
	final public int getX() { return x; }
	final public int getY() { return y; }
	final public int getZ() { return z; }
	
	final public void close()
	{
		cIn.close();
		cOut.close();
	}
	
	public EntropyFloatArray3D( final float stepSize, final Image<FloatType> img, final LocalizableByDimCursor3D<FloatType> cIn, final LocalizableByDimCursor3D<FloatType> cOut,
								final int histogramBins, final int windowSizeX, final int windowSizeY, final int windowSizeZ, int x, int y, int z)
	{
		absFreq = new int[histogramBins];
	
		this.img = img;
		this.cIn = cIn;
		this.cOut = cOut;
		this.histogramBins = histogramBins;
		
		if (windowSizeX %2 == 0)
			this.windowSizeX = windowSizeX + 1;
		else
			this.windowSizeX = windowSizeX;
			
		if (windowSizeY %2 == 0)
			this.windowSizeY = windowSizeY + 1;
		else
			this.windowSizeY = windowSizeY;

		if (windowSizeZ %2 == 0)
			this.windowSizeZ = windowSizeZ + 1;
		else
			this.windowSizeZ = windowSizeZ;
		
		size = this.windowSizeX * this.windowSizeY * this.windowSizeZ;
		
		windowSizeXHalf = this.windowSizeX/2;
		windowSizeYHalf = this.windowSizeY/2;
		windowSizeZHalf = this.windowSizeZ/2;
		
		if (z - windowSizeZHalf >= 0 && z + windowSizeZHalf < img.getDimension(2) )
			outOfImageZ = false;
		else
			outOfImageZ = true;
		
		if (y - windowSizeYHalf >= 0 && y + windowSizeYHalf < img.getDimension(1) )
			outOfImageY = false;
		else 
			outOfImageY = true;
		
		if (x - windowSizeXHalf >= 0 && x + windowSizeXHalf < img.getDimension(0))
			outOfImageX = false;
		else
			outOfImageX = true;		
		
		preComputed = preComputeProbabilities(stepSize);
		
		this.x = x;
		this.y = y;		
		this.z = z;
	}	
		
	public static Image<FloatType> computeEntropy(final Image<FloatType> image, final ContainerFactory entropyType, final int histogramBins, final int windowSizeX, final int windowSizeY, final int windowSizeZ)
	{
		final float maxEntropy = getMaxEntropy(histogramBins);
		
		final ImageFactory<FloatType> factory = new ImageFactory<FloatType>( new FloatType(), entropyType );
		final Image<FloatType> entropy = factory.createImage( image.getDimensions(), "Entropy of " + image.getName() );

		final LocalizableByDimCursor3D<FloatType> it = (LocalizableByDimCursor3D<FloatType>)entropy.createLocalizableByDimCursor();
		
		final EntropyFloatArray3D entropyObject = EntropyFloatArray3D.initEntropy( image, histogramBins, windowSizeX, windowSizeY, windowSizeZ, 0, 0, 0 );
		
		final int directionZ = +1; 
		int directionY = +1;
		int directionX = +1;
		
		for (int z = 0; z < image.getDimension( 2 ); z++)
		{
			for (int y = 0; y < image.getDimension( 1 ); y++)
    		{
    			for (int x = 0; x < image.getDimension( 0 ); x++)
    			{
    				if (x != 0)
    					entropyObject.updateEntropyX(directionX);
    				    				
    				it.setPosition( entropyObject.getX(), entropyObject.getY(), entropyObject.getZ() );
    				it.getType().set( entropyObject.getEntropy() / maxEntropy );
				}    			
    			directionX *= -1;
    			
    			if (y != image.getDimension( 1 ) - 1)
    				entropyObject.updateEntropyY(directionY);
    		}                            		
    		directionY *= -1;
    		
			if (z != image.getDimension( 2 ) - 1)
				entropyObject.updateEntropyZ(directionZ);                            		
		}
		
		entropyObject.cIn.close();
		entropyObject.cOut.close();
		
		return entropy;
	}

	public static EntropyFloatArray3D initEntropy( final Image<FloatType> img, final int histogramBins, final int windowSizeX, final int windowSizeY, final int windowSizeZ, final int x, final int y, final int z )
	{
		final LocalizableByDimCursor3D<FloatType> cIn = (LocalizableByDimCursor3D<FloatType>)img.createLocalizableByDimCursor(); 
		final LocalizableByDimCursor3D<FloatType> cOut = (LocalizableByDimCursor3D<FloatType>)img.createLocalizableByDimCursor( new OutOfBoundsStrategyMirrorFactory<FloatType>() );

		// arrays for guessing the probabilities
		final EntropyFloatArray3D entropyObject = new EntropyFloatArray3D(0.001f, img, cIn, cOut, histogramBins, windowSizeX, windowSizeY, windowSizeZ, x, y, z);
		
		//
		// fill arrays
		//
		if (!entropyObject.outOfImageX && !entropyObject.outOfImageY && !entropyObject.outOfImageZ)
		{
			for (int zs = z - entropyObject.windowSizeZHalf; zs <= z + entropyObject.windowSizeZHalf; zs++)
				for (int ys = y - entropyObject.windowSizeYHalf; ys <= y + entropyObject.windowSizeYHalf; ys++)
					for (int xs = x - entropyObject.windowSizeXHalf; xs <= x + entropyObject.windowSizeXHalf; xs++)
					{
						// compute bin
						cIn.moveTo(xs, ys, zs);
						int bin = (int) (cIn.getType().get() * histogramBins);

						// for the case of value being exactly 1
						if (bin >= histogramBins) bin = histogramBins - 1;
						if (bin < 0) bin = 0;

						entropyObject.absFreq[bin]++;
					}
		}
		else
		{
			for (int zs = z - entropyObject.windowSizeZHalf; zs <= z + entropyObject.windowSizeZHalf; zs++)
				for (int ys = y - entropyObject.windowSizeYHalf; ys <= y + entropyObject.windowSizeYHalf; ys++)
					for (int xs = x - entropyObject.windowSizeXHalf; xs <= x + entropyObject.windowSizeXHalf; xs++)
					{
						// compute bin
						cOut.moveTo(xs, ys, zs);
						int bin = (int) (cOut.getType().get() * histogramBins);

						// for the case of value being exactly 1
						if (bin >= histogramBins) bin = histogramBins - 1;
						if (bin < 0) bin = 0;

						entropyObject.absFreq[bin]++;											
					}
		}
				
		//
		// make probablities and compute the entropy
		//		
		entropyObject.entropy = 0;
		for (int bin = 0; bin < histogramBins; bin++)
		{
			if (entropyObject.absFreq[bin] > 0)
			{
				final float prob = entropyObject.absFreq[bin] / entropyObject.size;				
				entropyObject.entropy -= /*ei.getEntropyValue(prob, ei.preComputed); //*/prob * (Math.log(prob) / Math.log(2));
			}
		}

		return entropyObject;
	}	
	
	public static float getMaxEntropy(int bins)
	{
		return (float)(Math.log(bins) / Math.log(2));
	}

	final private float getEntropyValue(final float probability, final float[] preComputed)
	{
		int index = (int)(probability * (preComputed.length - 2)) + 1;		
		return preComputed[index];
	}

	final private float[] preComputeProbabilities(final float stepSize)
	{
		// the +2 is just to compensate for numerical instabilities
		// we can prevent a (if index < 0 index = 0) and (if index > 1 index = 1) 
		float tmp[] = new float[Math.round(1/stepSize)+2];
		
		for (int i = 0; i < tmp.length - 2; i++)
		{
			double prob = (i*stepSize + (i+1)*stepSize)/2;
			tmp[i+1] = (float)(prob * (Math.log(prob) / Math.log(2)));
		}
		
		tmp[0] = tmp[1];
		tmp[tmp.length - 1] = tmp[tmp.length - 2];
		
		return tmp;
	}

	private final void updateEntropyValue()
	{
		entropy = 0;
		
		for (final int bin : absFreq)
			if (bin > 0)
				entropy -= getEntropyValue(bin / size, preComputed);				
	}
	
	private static final void updateBinNegative(final int[] absFreq, final int histogramBins, final int x, final int y, final int z, final LocalizableByDimCursor3D<FloatType> c )
	{
		// compute bins
		c.moveTo(x, y, z);
		int bin = (int) (c.getType().get() * histogramBins);

		// for the case of value being exactly 1
		if (bin >= histogramBins) bin = histogramBins - 1;
		if (bin < 0) bin = 0;

		absFreq[bin]--;			
	}

	private static final void updateBinPositive(final int[] absFreq, final int histogramBins, final int x, final int y, final int z, final LocalizableByDimCursor3D<FloatType> c)
	{
		// compute bins
		c.moveTo(x, y, z);
		int bin = (int) (c.getType().get() * histogramBins);

		// for the case of value being exactly 1
		if (bin >= histogramBins) bin = histogramBins - 1;
		if (bin < 0) bin = 0;

		absFreq[bin]++;			
	}

	public void updateEntropyX(final int direction)
	{
		final int xs1 = x - direction*windowSizeXHalf;
		x += direction;
		final int xs2 = x + direction*windowSizeXHalf;
		
		for (int zs = z - windowSizeZHalf; zs <= z + windowSizeZHalf; zs++)
			for (int ys = y - windowSizeYHalf; ys <= y + windowSizeYHalf; ys++)
			{
				updateBinNegative(absFreq, histogramBins, xs1, ys, zs, cOut);
				updateBinPositive(absFreq, histogramBins, xs2, ys, zs, cOut);
			}
		
		updateEntropyValue();
	}

	public void updateEntropyY(final int direction)
	{
		final int ys1 = y - direction*windowSizeYHalf;
		y += direction;
		final int ys2 = y + direction*windowSizeYHalf;
		
		for (int zs = z - windowSizeZHalf; zs <= z + windowSizeZHalf; zs++)
			for (int xs = x - windowSizeXHalf; xs <= x + windowSizeXHalf; xs++)
			{
				updateBinNegative(absFreq, histogramBins, xs, ys1, zs, cOut);
				updateBinPositive(absFreq, histogramBins, xs, ys2, zs, cOut);
			}
									
		updateEntropyValue();
	}

	public void updateEntropyZ(final int direction)
	{
		final int zs1 = z - direction*windowSizeZHalf;
		z += direction;
		final int zs2 = z + direction*windowSizeZHalf;
		
		for (int ys = y - windowSizeYHalf; ys <= y + windowSizeYHalf; ys++)
			for (int xs = x - windowSizeXHalf; xs <= x + windowSizeXHalf; xs++)
			{
				updateBinNegative(absFreq, histogramBins, xs, ys, zs1, cOut);
				updateBinPositive(absFreq, histogramBins, xs, ys, zs2, cOut);
			}
				
		updateEntropyValue();			
	}	
}
