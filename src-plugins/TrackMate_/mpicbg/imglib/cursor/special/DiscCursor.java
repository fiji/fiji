package mpicbg.imglib.cursor.special;

import ij.ImagePlus;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.Localizable;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import mpicbg.imglib.type.numeric.real.FloatType;

public final class DiscCursor <T extends RealType<T>>  extends DomainCursor<T> {

	/** The state of the cursor. */
	private CursorState state, nextState;
	/** When drawing a line, the line length. */
	private int rx;
	/** Store X line bounds for all Y */
	private int[] rxs;

	private boolean allDone;
	
	/**
	 * Indicates what state the cursor is currently in, so as to choose the right routine 
	 * to get coordinates */
	private enum CursorState {
		DRAWING_LINE					,
		INITIALIZED						,
		INCREMENT_Y						,
		MIRROR_Y						;
	}
	
	/*
	 * CONSTRUCTORS
	 */
	
	/**
	 * Construct a {@link DiscCursor} on an image with a given spatial calibration.
	 * @param img  the image
	 * @param center  the disc center, in physical units
	 * @param radius  the disc radius, in physical units
	 * @param calibration  the spatial calibration (pixel size); if <code>null</code>, 
	 * a calibration of 1 in all directions will be used
	 * @param outOfBoundsFactory  the {@link OutOfBoundsStrategyFactory} that will be used to handle off-bound locations
	 */
	public DiscCursor(final Image<T> img, final float[] center, float radius, final float[] calibration, final OutOfBoundsStrategyFactory<T> outOfBoundsFactory) {
		this.img = img;
		this.size = radius;
		this.cursor = img.createLocalizableByDimCursor(outOfBoundsFactory);		
		if (null == calibration)
			this.calibration = new float[] {1, 1, 1};
		 else 
			this.calibration = calibration.clone();
		this.origin = new int[img.getNumDimensions()];
		for (int i = 0; i < origin.length; i++)
			origin[i] = Math.round(center[i] / this.calibration[i]);
		rxs = new int [ (int) (Math.max(Math.ceil(radius/this.calibration[0]), Math.ceil(radius/this.calibration[1]))  +  1) ];
		reset();
	}
	
	/**
	 * Construct a {@link DiscCursor} on an image with a given spatial calibration,
	 * using a default {@link OutOfBoundsStrategyValueFactory} to handle off-bounds locations.
	 * @param img  the image
	 * @param center  the disc center, in physical units
	 * @param radius  the disc radius, in physical units
	 * @param calibration  the spatial calibration (pixel size); if <code>null</code>, 
	 * a calibration of 1 in all directions will be used
	 */
	public DiscCursor(final Image<T> img, final float[] center, float radius, final float[] calibration) {
		this(img, center, radius, calibration, new OutOfBoundsStrategyValueFactory<T>());
	}


	/**
	 * Construct a {@link DiscCursor} on an image, using the spatial calibration
	 * stored in the image and a default {@link OutOfBoundsStrategyValueFactory}
	 * to handle off-bounds locations.
	 * 
	 * @param img  the image
	 * @param center  the disc center, in physical units
	 * @param radius  the disc radius, in physical units
	 * @see Image#setCalibration(float[])
	 */
	public DiscCursor(final Image<T> img, final float[] center, float radius) {
		this(img, center, radius, img.getCalibration());
	}

	/**
	 * Construct a {@link DiscCursor} on an image with a given spatial calibration
	 * and a given {@link OutOfBoundsStrategyFactory} to handle off-bounds locations.
 	 * The center of the disc is set by the {@link Localizable} given in argument.
	 * 
	 * @param img  the image
	 * @param centerCursor  the localizable object which position will set the disc center 
	 * @param radius  the disc radius, in physical units
	 * @param calibration  the spatial calibration (pixel size); if <code>null</code>, 
	 * a calibration of 1 in all directions will be used
	 */
	public DiscCursor(final Image<T> img, final Localizable centerCursor, float radius, final float[] calibration, OutOfBoundsStrategyFactory<T> outOfBoundsFactory) {
		this.img = img;
		this.size = radius;
		this.cursor = img.createLocalizableByDimCursor(outOfBoundsFactory);		
		if (null == calibration) 
			this.calibration = new float[] {1, 1, 1};
		else
			this.calibration = calibration.clone();		
		this.origin = centerCursor.getPosition();
		rxs = new int [ Math.max(Math.round(radius/calibration[0]), Math.round(radius/calibration[1]))  +  1 ];
		reset();
	}
	
	/**
	 * Construct a {@link DiscCursor} on an image, using the given spatial calibration
	 * and a default {@link OutOfBoundsStrategyValueFactory}
	 * to handle off-bounds locations. The center of the disc
	 * is set by the {@link Localizable} given in argument.
	 * 
	 * @param img  the image
	 * @param centerCursor  the cursor which position will set the disc center 
	 * @param radius  the disc radius, in physical units
	 */
	public DiscCursor(final Image<T> img, final Localizable centerCursor, float radius, final float[] calibration) {
		this(img, centerCursor, radius, calibration, new OutOfBoundsStrategyValueFactory<T>());
	}
	
	
	/**
	 * Construct a {@link DiscCursor} on an, using the spatial calibration
	 * stored in the image and a default {@link OutOfBoundsStrategyValueFactory}
	 * to handle off-bounds locations. The center of the disc
	 * is set by the {@link Localizable} given in argument.
	 * 
	 * @param img  the image
	 * @param centerCursor  the localizable object which position will set the disc center 
	 * @param radius  the disc radius, in physical units
	 */
	public DiscCursor(final Image<T> img, final Localizable centerCursor, float radius) {
		this(img, centerCursor, radius, img.getCalibration(), new OutOfBoundsStrategyValueFactory<T>());
	}
	
	/*
	 * METHODS
	 */
	
	/**
	 * Change the radius of the disc this cursor iterates on.  This <b>resets</b> this cursor.
	 * @param  size  the radius to set, in physical units.
	 */
	@Override
	public void setSize(float size) {
		this.size = size;
		rxs = new int [ Math.max(Math.round(size/calibration[0]), Math.round(size/calibration[1]))  +  1 ];		
		reset();
	}
	
	@Override
	public int getNPixels() {
		int pixel_count = 0;
		final int[] local_rxs = new int [ Math.max(Math.round(size/calibration[1]), Math.round(size/calibration[0]))  +  1 ];
		Utils.getXYEllipseBounds(Math.round(size/calibration[0]), Math.round(size/calibration[1]), local_rxs);
		int local_rx;

		pixel_count += 2 * local_rxs[0] + 1;
		for (int i = 1; i <= Math.round(size/calibration[1]); i++) {
			local_rx = local_rxs[i];
			pixel_count += 2 * (2 * local_rx + 1); // Twice because we mirror
		}
		return pixel_count;
	}
	

	/**
	 * Return the azimuth of the spherical coordinates of this cursor, with respect 
	 * to its center. Will be in the range ]-π, π].
	 * <p>
	 * In cylindrical coordinates, the azimuth is the angle measured between 
	 * the X axis and the line OM where O is the sphere center and M is the cursor location
	 */
	public final double getPhi() {
		return Math.atan2(position[1]*calibration[1], position[0]*calibration[0]);
	}

	@Override
	public void reset() {
		cursor.reset();
		state = CursorState.INITIALIZED;
		position = new int[img.getNumDimensions()];
		hasNext = true;
		allDone = false;
	}
	
	@Override
	public void fwd() {
		switch(state) {

		case DRAWING_LINE:

			cursor.fwd(0);
			position[0]++;
			if (position[0] >= rx) {
				state = nextState;
				if (allDone)
					hasNext = false;
			}
			break;

		case INITIALIZED:

			// Compute circle radiuses in advance
			Utils.getXYEllipseBounds(Math.round(size/calibration[0]), Math.round(size/calibration[1]), rxs);
			
			rx = rxs[0] ; 
			cursor.setPosition(origin);
			cursor.setPosition(origin[0] - rx, 0);
			position[0] = -rx;
			state = CursorState.DRAWING_LINE;
			nextState = CursorState.INCREMENT_Y;
			break;

		case INCREMENT_Y:

			position[1] = -position[1] + 1; // y should be negative (coming from mirroring or init = 0)
			rx = rxs[position[1]];

			cursor.setPosition(origin[1] + position[1], 1);
			position[0] = -rx;
			cursor.setPosition(origin[0] - rx, 0);
			nextState = CursorState.MIRROR_Y;
			if (rx ==0)
				state = CursorState.MIRROR_Y;
			else
				state = CursorState.DRAWING_LINE;				
			break;

		case MIRROR_Y:

			position[0] = -rx;
			position[1] = - position[1];
			cursor.setPosition(origin[1] + position[1], 1);
			cursor.setPosition(origin[0] - rx, 0);
			if (position[1] <= - Math.round(size/calibration[1]))
				allDone  = true;
			else 
				nextState = CursorState.INCREMENT_Y;
			if (rx ==0)
				if (allDone)
					hasNext = false;
				else
					state = nextState;
			else
				state = CursorState.DRAWING_LINE;

			break;
		}
	}
	
	/*
	 * MAIN METHOD
	 */
	
	public static void main(String[] args) {

		Image<UnsignedByteType> testImage = new ImageFactory<UnsignedByteType>(
				new UnsignedByteType(),
				new ArrayContainerFactory()
		).createImage(new int[] {80, 80});

		float radius = 5; // µm
		float[] calibration = new float[] {0.5f, 1f}; 
		DiscCursor<UnsignedByteType> cursor = new DiscCursor<UnsignedByteType>(
				testImage, 
				new float[] {20, 20}, // in µm
				radius, // µm
				calibration);
		int volume = 0;
		while(cursor.hasNext()) {
			volume++;
			cursor.fwd();
//			cursor.getType().set((int) cursor.getDistanceSquared()); // to check we paint a disc in physical coordinates
			cursor.getType().inc(); // to check we did not walk multiple times on a single pixel
		}
		cursor.close();

		int  maxPixelValue = 0;
		Cursor<UnsignedByteType> c = testImage.createCursor();
		while(c.hasNext()) {
			c.fwd();
			if (c.getType().get() > maxPixelValue) 
				maxPixelValue = c.getType().get();
		}
		c.close();

		System.out.println(String.format("Cursor for a disc of radius %.1f", radius));
		System.out.println(String.format("Volume iterated prediction: %d pixels.", cursor.getNPixels()));
		System.out.println(String.format("Iterated actually over %d pixels, real volume is: %.1f", volume, 4/3.0*Math.PI*radius*radius*radius));
		System.out.println(String.format("Each pixel have been walked on at most %d times.", maxPixelValue));


		// Visualize results
		ij.ImageJ.main(args);

		ImagePlus imp = ImageJFunctions.copyToImagePlus(testImage);
		imp.getCalibration().pixelWidth = calibration[0];
		imp.getCalibration().pixelHeight = calibration[1];
		imp.getCalibration().setUnit("um");
		imp.show();
		float iRadius = 5;

		// Iterates over all pixels of the image, using the sphere cursor as a neighborhood cursor.
		// We simply convolve.

		Image<FloatType> newImage = new ImageFactory<FloatType>(
				new FloatType(),
				new ArrayContainerFactory()
		).createImage(testImage.getDimensions());
		LocalizableCursor<UnsignedByteType> mainCursor = testImage.createLocalizableCursor();
		LocalizableByDimCursor<FloatType> destCursor = newImage.createLocalizableByDimCursor();
		DiscCursor<UnsignedByteType> discCursor = new DiscCursor<UnsignedByteType>(testImage, mainCursor, iRadius, calibration);
		System.out.println("\nUsing the sphere cursor to convolve the whole image with a disc of radius " + iRadius + "...");
		int sum;
		int pixelNumber = 0;
		long start = System.currentTimeMillis();
		while (mainCursor.hasNext()) {
			mainCursor.fwd();
			discCursor.moveCenterTo(mainCursor);
			sum = 0;
			while (discCursor.hasNext()) {
				discCursor.fwd();
				sum += discCursor.getType().get();
				pixelNumber++;
			}
			destCursor.setPosition(mainCursor);
			destCursor.getType().set(sum);
		}
		discCursor.close();
		mainCursor.close();
		destCursor.close();
		long end = System.currentTimeMillis();
		System.out.println(String.format("Iterated over in total %d pixels in %d ms: %.1e pixel/s.", pixelNumber, (end-start), pixelNumber/((float) (end-start)/1000) ));

		ImagePlus dest = ImageJFunctions.copyToImagePlus(newImage);
		dest.getCalibration().pixelWidth = calibration[0];
		dest.getCalibration().pixelHeight = calibration[1];
		dest.getCalibration().setUnit("um");
		dest.show();


	}


}
