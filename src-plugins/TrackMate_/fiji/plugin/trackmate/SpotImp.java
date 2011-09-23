package fiji.plugin.trackmate;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import com.mxgraph.util.mxBase64;

import mpicbg.imglib.util.Util;
import fiji.plugin.trackmate.gui.TrackMateFrame;

/**
 * Plain implementation of the {@link Spot} interface.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Sep 16, 2010
 *
 */
public class SpotImp implements Spot {
	
	/*
	 * FIELDS
	 */
	
	private static String DEFAULT_IMAGE_STRING = "";
	static {
		try {
			BufferedImage img = ImageIO.read(TrackMateFrame.class.getResource("images/spot_icon.png"));
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ImageIO.write(img, "png", bos);
			DEFAULT_IMAGE_STRING = mxBase64.encodeToString(bos.toByteArray(), false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static AtomicInteger IDcounter = new AtomicInteger(0); 
	
	/** Store the individual features, and their values. */
	private EnumMap<SpotFeature, Float> features = new EnumMap<SpotFeature, Float>(SpotFeature.class);
	/** A user-supplied name for this spot. */
	private String name;
	/** This spot ID */
	private int ID;
	/** This spot's image. */
	private String imageString = DEFAULT_IMAGE_STRING;

	/*
	 * CONSTRUCTORS
	 */
	
	/**
	 * Instantiate a Spot. 
	 * <p>
	 * The given coordinate float array <b>must</b> have 3 elements. If the 3rd one is not
	 * used (2D case), it can be set to a constant value 0. This constructor ensures that
	 * none of the {@link Spot#POSITION_FEATURES} will be <code>null</code>, and ensure relevance
	 * when calculating distances and so on.
	 */
	public SpotImp(float[] coordinates, String name) {
		this.ID = IDcounter.getAndIncrement();
		for (int i = 0; i < 3; i++)
			putFeature(POSITION_FEATURES[i], coordinates[i]);
		if (null == name)
			this.name = "ID"+ID;
		else
			this.name = name;
	}
	
	public SpotImp(float[] coordinates) {
		this(coordinates, null);
	}
	
	/**
	 * Blank constructor meant to be used when loading a spot collection from a file. <b>Will</b> mess with
	 * the {@link #IDcounter} field, so this constructor should not be used for normal spot creation. 
	 * @param ID  the spot ID to set
	 */
	public SpotImp(int ID) {
		this.ID = ID;
		if (SpotImp.IDcounter.get() < ID) {
			SpotImp.IDcounter.set(ID+1);
		}
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	@Override
	public Spot clone() {
		SpotImp newSpot = new SpotImp(ID);
		// Deal with features
		Float val;
		for(SpotFeature key : features.keySet()) {
			val = features.get(key);
			if (null != val)
				val = new Float(val);
			newSpot.putFeature(key, val);
		}
		// Deal with name
		newSpot.name = name;
		return newSpot;
	};
	
	/**
	 * Convenience method that returns the X, Y and optionally Z feature in a float array.
	 */
	public void getCoordinates(float[] coords) {
		for (int i = 0; i < coords.length; i++)
			coords[i] = getFeature(POSITION_FEATURES[i]);
	}
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public int ID() {
		return ID;
	}
	
	@Override
	public String toString() {
		String str;
		if (null == name || name.equals(""))
			str = "ID"+ID;
		else 
			str = name;
		return str;
	}
	
	/**
	 * Return a string representation of this spot, with calculated features.
	 */
	public String echo() {
		StringBuilder s = new StringBuilder();
		
		// Name
		if (null == name) 
			s.append("Spot: <no name>\n");
		else
			s.append("Spot: "+name+"\n");
		
		// Frame
		s.append("Frame: "+getFeature(SpotFeature.POSITION_T)+'\n');

		// Coordinates
		float[] coordinates = getPosition(null);
		if (null == coordinates)
			s.append("Position: <no coordinates>\n");
		else 
			s.append("Position: "+Util.printCoordinates(coordinates)+"\n");
		
		// Feature list
		if (null == features || features.size() < 1) 
			s.append("No features calculated\n");
		else {
			s.append("Feature list:\n");
			float val;
			for (SpotFeature key : features.keySet()) {
				s.append("\t"+key.toString()+": ");
				val = features.get(key);
				if (val >= 1e4)
					s.append(String.format("%.1g", val));
				else
					s.append(String.format("%.1f", val));
				s.append('\n');
			}
		}
		return s.toString();
	}
	
	@Override
	public float[] getPosition(float[] position) {
		if (null == position) 
			position = new float[3];
		for (int i = 0; i < 3; i++) 
			position[i] = getFeature(POSITION_FEATURES[i]);
		return position;
	}
	
	/*
	 * FEATURE RELATED METHODS
	 */
	
	
	@Override
	public EnumMap<SpotFeature, Float> getFeatures() {
		return features;
	}
	
	@Override
	public final Float getFeature(final SpotFeature feature) {
		return features.get(feature);
	}
	
	@Override
	public final void putFeature(final SpotFeature feature, final float value) {
		features.put(feature, value);
	}

	@Override
	public Float diffTo(Spot s, SpotFeature feature) {
		Float f1 = getFeature(feature);
		Float f2 = s.getFeature(feature);
		if (f1 == null || f2 == null)
			return null;
		return f1 - f2;
	}
	
	@Override
	public Float normalizeDiffTo(Spot s, SpotFeature feature) {
		final Float a = getFeature(feature);
		final Float b = s.getFeature(feature);
		if (a == -b)
			return 0f;
		else
			return Math.abs(a-b)/((a+b)/2);
	}

	@Override
	public Float squareDistanceTo(Spot s) {
		Float sumSquared = 0f;
		Float thisVal, otherVal;
		
		for (SpotFeature f : POSITION_FEATURES) {
			thisVal = getFeature(f);
			otherVal = s.getFeature(f);
			sumSquared += ( otherVal - thisVal ) * ( otherVal - thisVal ); 
		}
		return sumSquared;
	}

	@Override
	public void setImageString(String str) {
		this.imageString = str;
	}

	@Override
	public String getImageString() {
		return imageString;
	}
	
	

}
