package mpicbg.spim.fusion.entropy;

import mpicbg.imglib.container.ContainerFactory;
import mpicbg.imglib.container.array.Array3D;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.spim.io.IOFunctions;

public class Entropy
{
	// final variables
	public final float[] preComputed;
	public final int[] absFreq;
	public boolean outOfImageX, outOfImageY, outOfImageZ;  
	public final Image<FloatType> img;
	public final int histogramBins, windowSizeX, windowSizeY, windowSizeZ;
	public final float size;
	public final int windowSizeXHalf, windowSizeYHalf, windowSizeZHalf;

	
	// changing variables
	private float entropy;
	final LocalizableByDimCursor<FloatType> cursor;
	private int x, y, z;
	
	final public float getEntropy() { return entropy; }
	final public int getX() { return x; }
	final public int getY() { return y; }
	final public int getZ() { return z; }
	
	public Entropy(final float stepSize, final Image<FloatType> img, final LocalizableByDimCursor<FloatType> cursor,
			final int histogramBins, final int windowSizeX, final int windowSizeY, final int windowSizeZ, int x, int y, int z)
	{
		absFreq = new int[histogramBins];
	
		this.img = img;
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
		
		if (z - windowSizeZHalf >= 0 && z + windowSizeZHalf < img.getDimension( 2 ) )
			outOfImageZ = false;
		else
			outOfImageZ = true;
		
		if (y - windowSizeYHalf >= 0 && y + windowSizeYHalf < img.getDimension( 1 ) )
			outOfImageY = false;
		else 
			outOfImageY = true;
		
		if (x - windowSizeXHalf >= 0 && x + windowSizeXHalf < img.getDimension( 0 ) )
			outOfImageX = false;
		else
			outOfImageX = true;		
		
		preComputed = preComputeProbabilities(stepSize);
		
		this.x = x;
		this.y = y;		
		this.z = z;

		this.cursor = cursor;
		cursor.setPosition( new int[]{ x, y, z} );
	}
	
	public void close() { cursor.close(); }
		
	public static Image<FloatType> computeEntropy(final Image<FloatType> img, final ContainerFactory entropyType, final int histogramBins, final int windowSizeX, final int windowSizeY, final int windowSizeZ)
	{
		// check if we can use fast forward algorithm		
		if ( Array3D.class.isInstance( img.getContainer() ) )
		{
			IOFunctions.println("Input is instance of Image<Float> using an Array3D, fast forward algorithm --- Fast Forward Algorithm available.");
			return EntropyFloatArray3D.computeEntropy( img, entropyType, histogramBins, windowSizeX, windowSizeY, windowSizeZ); 
		}	
		
		final float maxEntropy = getMaxEntropy(histogramBins);
		final ImageFactory<FloatType> factory = new ImageFactory<FloatType>( new FloatType(), entropyType );
		final Image<FloatType> entropy = factory.createImage( img.getDimensions(), "Entropy of " + img.getName() );
		
		final LocalizableByDimCursor<FloatType> entropyIterator = entropy.createLocalizableByDimCursor( new OutOfBoundsStrategyMirrorFactory<FloatType>() );
		
		final Entropy ei = Entropy.initEntropy( img, histogramBins, windowSizeX, windowSizeY, windowSizeZ, 0, 0, 0);
		
		final int directionZ = +1; 
		int directionY = +1;
		int directionX = +1;	
		
		final int width = img.getDimension( 0 );
		final int height = img.getDimension( 1 );
		final int depth = img.getDimension( 2 );
		
		for (int z = 0; z < depth; z++)
		{    			
			for (int y = 0; y < height; y++)
    		{
    			for (int x = 0; x < width; x++)
    			{
    				if (x != 0)
    					ei.updateEntropyX(directionX);
    				    	
    				entropyIterator.move( ei.getX() - entropyIterator.getPosition(0), 0 );
    				entropyIterator.move( ei.getY() - entropyIterator.getPosition(1), 1 );
    				entropyIterator.move( ei.getZ() - entropyIterator.getPosition(2), 2 );
    				
    				entropyIterator.getType().set( ei.getEntropy() / maxEntropy );
				}    			
    			directionX *= -1;
    			
    			if (y != height - 1)
    				ei.updateEntropyY(directionY);
    		}                            		
    		directionY *= -1;
    		
			if (z != depth - 1)
				ei.updateEntropyZ(directionZ);                            		
		}
				
		entropyIterator.close();
		ei.close();
		
		return entropy;
	}

	/*public static EntropyInformation initEntropy(final Float3D img, final int histogramBins, final int windowSizeX, final int windowSizeY, final int windowSizeZ, final int x, final int y, final int z)
	{
		// arrays for guessing the probabilities
		EntropyInformation ei = new EntropyInformation(0.001f, img, histogramBins, windowSizeX, windowSizeY, windowSizeZ, x, y, z);
		
		//
		// fill arrays
		//
		if (!ei.outOfImageX && !ei.outOfImageY && !ei.outOfImageZ)
		{
			for (int zs = z - ei.windowSizeZHalf; zs <= z + ei.windowSizeZHalf; zs++)
				for (int ys = y - ei.windowSizeYHalf; ys <= y + ei.windowSizeYHalf; ys++)
					for (int xs = x - ei.windowSizeXHalf; xs <= x + ei.windowSizeXHalf; xs++)
					{
						// compute bin
						int bin = (int) (img.get(xs, ys, zs) * histogramBins);

						// for the case of value being exactly 1
						if (bin >= histogramBins) bin = histogramBins - 1;
						if (bin < 0) bin = 0;

						ei.absFreq[bin]++;
					}
		}
		else
		{
			for (int zs = z - ei.windowSizeZHalf; zs <= z + ei.windowSizeZHalf; zs++)
				for (int ys = y - ei.windowSizeYHalf; ys <= y + ei.windowSizeYHalf; ys++)
					for (int xs = x - ei.windowSizeXHalf; xs <= x + ei.windowSizeXHalf; xs++)
					{
						// compute bin
						int bin = (int) (img.getMirror(xs, ys, zs) * histogramBins);

						// for the case of value being exactly 1
						if (bin >= histogramBins) bin = histogramBins - 1;
						if (bin < 0) bin = 0;

						ei.absFreq[bin]++;											
					}
		}
				
		//
		// make probablities and compute the entropy
		//		
		ei.entropy = 0;
		for (int bin = 0; bin < histogramBins; bin++)
		{
			if (ei.absFreq[bin] > 0)
			{
				final float prob = ei.absFreq[bin] / ei.size;				
				ei.entropy -= prob * (Math.log(prob) / Math.log(2)); //*ei.getEntropyValue(prob, ei.preComputed);
			}
		}

		return ei;
	}	*/

	public static Entropy initEntropy( final Image<FloatType> img, final int histogramBins, final int windowSizeX, final int windowSizeY, final int windowSizeZ, final int x, final int y, final int z)
	{
		final LocalizableByDimCursor<FloatType> cursor = img.createLocalizableByDimCursor( new OutOfBoundsStrategyMirrorFactory<FloatType>() );
		
		// arrays for guessing the probabilities
		final Entropy ei = new Entropy(0.001f, img, cursor, histogramBins, windowSizeX, windowSizeY, windowSizeZ, x, y, z);
		
		//
		// fill arrays
		//
		for (int zs = z - ei.windowSizeZHalf; zs <= z + ei.windowSizeZHalf; zs++)
			for (int ys = y - ei.windowSizeYHalf; ys <= y + ei.windowSizeYHalf; ys++)
				for (int xs = x - ei.windowSizeXHalf; xs <= x + ei.windowSizeXHalf; xs++)
				{
					// compute bin
					cursor.move( xs - cursor.getPosition( 0 ), 0 );
					cursor.move( ys - cursor.getPosition( 1 ), 1 );
					cursor.move( zs - cursor.getPosition( 2 ), 2 );
					
					int bin = (int) (cursor.getType().get() * histogramBins);

					// for the case of value being exactly 1
					if (bin >= histogramBins) bin = histogramBins - 1;
					if (bin < 0) bin = 0;

					ei.absFreq[bin]++;					
				}
				
		//
		// make probablities and compute the entropy
		//		
		ei.entropy = 0;
		for (int bin = 0; bin < histogramBins; bin++)
		{
			if (ei.absFreq[bin] > 0)
			{
				final float prob = ei.absFreq[bin] / ei.size;				
				ei.entropy -= prob * (Math.log(prob) / Math.log(2));
			}
		}
		
		IOFunctions.println(ei.entropy);
		
		return ei;
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
	

	private static final void updateBinNegative(final int[] absFreq, final int histogramBins, final int x, final int y, final int z, final LocalizableByDimCursor<FloatType> cursor)
	{
		// compute bins
		final int mx = x - cursor.getPosition( 0 );
		final int my = y - cursor.getPosition( 1 );
		final int mz = z - cursor.getPosition( 2 );
		
		cursor.move( mx, 0 );
		cursor.move( my, 1 );
		cursor.move( mz, 2 );
		
		int bin = (int) (cursor.getType().get() * histogramBins);

		cursor.move( -mx, 0 );
		cursor.move( -my, 1 );
		cursor.move( -mz, 2 );

		// for the case of value being exactly 1
		if (bin >= histogramBins) bin = histogramBins - 1;
		if (bin < 0) bin = 0;

		absFreq[bin]--;			
	}

	private static final void updateBinPositive(final int[] absFreq, final int histogramBins, final int x, final int y, final int z, final LocalizableByDimCursor<FloatType> cursor)
	{
		// compute bins
		final int mx = x - cursor.getPosition( 0 );
		final int my = y - cursor.getPosition( 1 );
		final int mz = z - cursor.getPosition( 2 );
		
		cursor.move( mx, 0 );
		cursor.move( my, 1 );
		cursor.move( mz, 2 );
		
		int bin = (int) (cursor.getType().get() * histogramBins);

		cursor.move( -mx, 0 );
		cursor.move( -my, 1 );
		cursor.move( -mz, 2 );

		// for the case of value being exactly 1
		if (bin >= histogramBins) bin = histogramBins - 1;
		if (bin < 0) bin = 0;

		absFreq[bin]++;			
	}

	/*
	private static final void updateBinNegative(final int[] absFreq, final int histogramBins, final int x, final int y, final int z, final Float3D img)
	{
		// compute bins
		int bin = (int) (img.getMirror(x, y, z) * histogramBins);

		// for the case of value being exactly 1
		if (bin >= histogramBins) bin = histogramBins - 1;
		if (bin < 0) bin = 0;

		absFreq[bin]--;			
	}

	private static final void updateBinPositive(final int[] absFreq, final int histogramBins, final int x, final int y, final int z, final Float3D img)
	{
		// compute bins
		int bin = (int) (img.getMirror(x, y, z) * histogramBins);

		// for the case of value being exactly 1
		if (bin >= histogramBins) bin = histogramBins - 1;
		if (bin < 0) bin = 0;

		absFreq[bin]++;			
	}
	*/

	public void updateEntropyX(final int direction)
	{
		final int xs1 = x - direction*windowSizeXHalf;
		x += direction;
		final int xs2 = x + direction*windowSizeXHalf;
		
		if (direction > 0)
			cursor.move( 1, 0); //.nextX();
		else
			cursor.move( -1, 0); //prevX();
		
		for (int zs = z - windowSizeZHalf; zs <= z + windowSizeZHalf; zs++)
			for (int ys = y - windowSizeYHalf; ys <= y + windowSizeYHalf; ys++)
			{
				updateBinNegative(absFreq, histogramBins, xs1, ys, zs, cursor);
				updateBinPositive(absFreq, histogramBins, xs2, ys, zs, cursor);
			}
		
		updateEntropyValue();
	}

	/*public void updateEntropyX(final int direction)
	{
		final int xs1 = x - direction*windowSizeXHalf;
		x += direction;
		final int xs2 = x + direction*windowSizeXHalf;
		
		for (int zs = z - windowSizeZHalf; zs <= z + windowSizeZHalf; zs++)
			for (int ys = y - windowSizeYHalf; ys <= y + windowSizeYHalf; ys++)
			{
				updateBinNegative(absFreq, histogramBins, xs1, ys, zs, img);
				updateBinPositive(absFreq, histogramBins, xs2, ys, zs, img);
			}
		
		updateEntropyValue();
	}*/
	
	public void updateEntropyY(final int direction)
	{
		final int ys1 = y - direction*windowSizeYHalf;
		y += direction;
		final int ys2 = y + direction*windowSizeYHalf;
	
		if (direction > 0)
			cursor.move(1, 1); //imgIterator.nextY();
		else
			cursor.move(-1, 1); //imgIterator.prevY();
		
		for (int zs = z - windowSizeZHalf; zs <= z + windowSizeZHalf; zs++)
			for (int xs = x - windowSizeXHalf; xs <= x + windowSizeXHalf; xs++)
			{
				updateBinNegative(absFreq, histogramBins, xs, ys1, zs, cursor);
				updateBinPositive(absFreq, histogramBins, xs, ys2, zs, cursor);
			}
									
		updateEntropyValue();
	}

	/*public void updateEntropyY(final int direction)
	{
		final int ys1 = y - direction*windowSizeYHalf;
		y += direction;
		final int ys2 = y + direction*windowSizeYHalf;
		
		for (int zs = z - windowSizeZHalf; zs <= z + windowSizeZHalf; zs++)
			for (int xs = x - windowSizeXHalf; xs <= x + windowSizeXHalf; xs++)
			{
				updateBinNegative(absFreq, histogramBins, xs, ys1, zs, img);
				updateBinPositive(absFreq, histogramBins, xs, ys2, zs, img);
			}
									
		updateEntropyValue();
	}*/
	
	public void updateEntropyZ(final int direction)
	{
		final int zs1 = z - direction*windowSizeZHalf;
		z += direction;
		final int zs2 = z + direction*windowSizeZHalf;
		
		if (direction > 0)
			cursor.move(1, 2); //imgIterator.nextZ();
		else
			cursor.move(-1, 2); //imgIterator.prevZ();
		
		for (int ys = y - windowSizeYHalf; ys <= y + windowSizeYHalf; ys++)
			for (int xs = x - windowSizeXHalf; xs <= x + windowSizeXHalf; xs++)
			{
				updateBinNegative(absFreq, histogramBins, xs, ys, zs1, cursor);
				updateBinPositive(absFreq, histogramBins, xs, ys, zs2, cursor);
			}
				
		updateEntropyValue();			
	}
	
	/*
	public void updateEntropyZ(final int direction)
	{
		final int zs1 = z - direction*windowSizeZHalf;
		z += direction;
		final int zs2 = z + direction*windowSizeZHalf;
		
		for (int ys = y - windowSizeYHalf; ys <= y + windowSizeYHalf; ys++)
			for (int xs = x - windowSizeXHalf; xs <= x + windowSizeXHalf; xs++)
			{
				updateBinNegative(absFreq, histogramBins, xs, ys, zs1, img);
				updateBinPositive(absFreq, histogramBins, xs, ys, zs2, img);
			}
				
		updateEntropyValue();			
	}
	*/
	
	/*public void updateEntropyX(final int direction)
	{
		// subtract old values
		if (!outOfImageX && !outOfImageY && !outOfImageZ)
		{
			final int xs = x - direction*windowSizeXHalf;
			
			for (int zs = z - windowSizeZHalf; zs <= z + windowSizeZHalf; zs++)
				for (int ys = y - windowSizeYHalf; ys <= y + windowSizeYHalf; ys++)
				{
					// compute bin
					int bin = (int) (img.getMirror(xs, ys, zs) * histogramBins);

					// for the case of value being exactly 1
					if (bin >= histogramBins) bin = histogramBins - 1;
					if (bin < 0) bin = 0;

					absFreq[bin]--;			
				}
		}
		else
		{
			final int xs = x - direction*windowSizeXHalf;
			
			for (int zs = z - windowSizeZHalf; zs <= z + windowSizeZHalf; zs++)
				for (int ys = y - windowSizeYHalf; ys <= y + windowSizeYHalf; ys++)
				{
					// compute bin
					int bin = (int) (img.getMirror(xs, ys, zs) * histogramBins);

					// for the case of value being exactly 1
					if (bin >= histogramBins) bin = histogramBins - 1;
					if (bin < 0) bin = 0;

					absFreq[bin]--;				
				}			
		}

		// add new values
		x += direction;

		if (x - windowSizeXHalf >= 0 && x + windowSizeXHalf < img.width)
			outOfImageX = false;
		else
			outOfImageX = true;
		
		//if (x > 3 && y > 3 && z > 3)
			//IOFunctions.println("inside: " + x + "x" + y + "x" + z + " " + outOfImageX  + " " + outOfImageY + " " + outOfImageZ);

		if (!outOfImageX && !outOfImageY && !outOfImageZ)
		{
			final int xs = x + direction*windowSizeXHalf;
			
			for (int zs = z - windowSizeZHalf; zs <= z + windowSizeZHalf; zs++)
				for (int ys = y - windowSizeYHalf; ys <= y + windowSizeYHalf; ys++)
				{
					// compute bin
					int bin = (int) (img.get(xs, ys, zs) * histogramBins);

					// for the case of value being exactly 1
					if (bin >= histogramBins) bin = histogramBins - 1;
					if (bin < 0) bin = 0;

					absFreq[bin]++;					
				}
		}
		else
		{
			final int xs = x + direction*windowSizeXHalf;
			
			for (int zs = z - windowSizeZHalf; zs <= z + windowSizeZHalf; zs++)
				for (int ys = y - windowSizeYHalf; ys <= y + windowSizeYHalf; ys++)
				{
					// compute bin
					int bin = (int) (img.getMirror(xs, ys, zs) * histogramBins);

					// for the case of value being exactly 1
					if (bin >= histogramBins) bin = histogramBins - 1;
					if (bin < 0) bin = 0;

					absFreq[bin]++;				
				}			
		}	
		
		updateEntropyValue();	
	}

	public void updateEntropyY(final int direction)
	{
		// subtract old values
		if (!outOfImageX && !outOfImageY && !outOfImageZ)
		{
			final int ys = y - direction*windowSizeYHalf;
			
			for (int zs = z - windowSizeZHalf; zs <= z + windowSizeZHalf; zs++)
				for (int xs = x - windowSizeXHalf; xs <= x + windowSizeXHalf; xs++)
				{
					// compute bin
					int bin = (int) (img.get(xs, ys, zs) * histogramBins);

					// for the case of value being exactly 1
					if (bin >= histogramBins) bin = histogramBins - 1;
					if (bin < 0) bin = 0;

					absFreq[bin]--;			
				}
		}
		else
		{
			final int ys = y - direction*windowSizeYHalf;
			
			for (int zs = z - windowSizeZHalf; zs <= z + windowSizeZHalf; zs++)
				for (int xs = x - windowSizeXHalf; xs <= x + windowSizeXHalf; xs++)
				{
					// compute bin
					int bin = (int) (img.getMirror(xs, ys, zs) * histogramBins);

					// for the case of value being exactly 1
					if (bin >= histogramBins) bin = histogramBins - 1;
					if (bin < 0) bin = 0;

					absFreq[bin]--;			
				}
		}

		// add new values
		y += direction;

		if (y - windowSizeYHalf >= 0 && y + windowSizeYHalf < img.height)
			outOfImageY = false;
		else
			outOfImageY = true;		

		if (!outOfImageX && !outOfImageY && !outOfImageZ)
		{
			final int ys = y + direction*windowSizeYHalf;
			
			for (int zs = z - windowSizeZHalf; zs <= z + windowSizeZHalf; zs++)
				for (int xs = x - windowSizeXHalf; xs <= x + windowSizeXHalf; xs++)
				{
					// compute bin
					int bin = (int) (img.get(xs, ys, zs) * histogramBins);

					// for the case of value being exactly 1
					if (bin >= histogramBins) bin = histogramBins - 1;
					if (bin < 0) bin = 0;

					absFreq[bin]++;					
				}
		}
		else
		{
			final int ys = y + direction*windowSizeYHalf;
			
			for (int zs = z - windowSizeZHalf; zs <= z + windowSizeZHalf; zs++)
				for (int xs = x - windowSizeXHalf; xs <= x + windowSizeXHalf; xs++)
				{
					// compute bin
					int bin = (int) (img.getMirror(xs, ys, zs) * histogramBins);

					// for the case of value being exactly 1
					if (bin >= histogramBins) bin = histogramBins - 1;
					if (bin < 0) bin = 0;

					absFreq[bin]++;					
				}
		}
		
		updateEntropyValue();	
	}

	public void updateEntropyZ(final int direction)
	{
		// subtract old values
		if (!outOfImageX && !outOfImageY && !outOfImageZ)
		{
			final int zs = z - direction*windowSizeZHalf;
			
			for (int ys = y - windowSizeYHalf; ys <= y + windowSizeYHalf; ys++)
				for (int xs = x - windowSizeXHalf; xs <= x + windowSizeXHalf; xs++)
				{
					// compute bin
					int bin = (int) (img.get(xs, ys, zs) * histogramBins);

					// for the case of value being exactly 1
					if (bin >= histogramBins) bin = histogramBins - 1;
					if (bin < 0) bin = 0;

					absFreq[bin]--;			
				}
		}
		else
		{
			final int zs = z - direction*windowSizeZHalf;
			
			for (int ys = y - windowSizeYHalf; ys <= y + windowSizeYHalf; ys++)
				for (int xs = x - windowSizeXHalf; xs <= x + windowSizeXHalf; xs++)
				{
					// compute bin
					int bin = (int) (img.getMirror(xs, ys, zs) * histogramBins);

					// for the case of value being exactly 1
					if (bin >= histogramBins) bin = histogramBins - 1;
					if (bin < 0) bin = 0;

					absFreq[bin]--;			
				}
		}

		// add new values
		z += direction;

		if (z - windowSizeZHalf >= 0 && z + windowSizeZHalf < img.depth)
			outOfImageZ = false;
		else
			outOfImageZ = true;		

		if (!outOfImageX && !outOfImageY && !outOfImageZ)
		{
			final int zs = z + direction*windowSizeZHalf;
			
			for (int ys = y - windowSizeYHalf; ys <= y + windowSizeYHalf; ys++)
				for (int xs = x - windowSizeXHalf; xs <= x + windowSizeXHalf; xs++)
				{
					// compute bin
					int bin = (int) (img.get(xs, ys, zs) * histogramBins);

					// for the case of value being exactly 1
					if (bin >= histogramBins) bin = histogramBins - 1;
					if (bin < 0) bin = 0;

					absFreq[bin]++;					
				}
		}
		else
		{
			final int zs = z + direction*windowSizeZHalf;
			
			for (int ys = y - windowSizeYHalf; ys <= y + windowSizeYHalf; ys++)
				for (int xs = x - windowSizeXHalf; xs <= x + windowSizeXHalf; xs++)
				{
					// compute bin
					int bin = (int) (img.getMirror(xs, ys, zs) * histogramBins);

					// for the case of value being exactly 1
					if (bin >= histogramBins) bin = histogramBins - 1;
					if (bin < 0) bin = 0;

					absFreq[bin]++;					
				}
		}		

		updateEntropyValue();	
	}*/
	
}
