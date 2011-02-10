package imagescience.utility;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.IndexColorModel;
import java.awt.image.MemoryImageSource;

/** Provides custom-made mouse cursors. */
public class MouseCursor {
	
	/** The custom arrow cursor type. */
	public static final int ARROW=0;
	
	/** The custom crosshair cursor type. */
	public static final int CROSSHAIR=1;
	
	/** The custom magnifier cursor type. */
	public static final int MAGNIFIER=2;
	
	/** The custom hand cursor type. */
	public static final int HAND=3;
	
	private static final int SIZE = 32; // Cursor size
	private static final byte o = (byte)0; // Zero value
	private static final byte l = (byte)1; // Low value
	private static final byte m = (byte)180; // Medium value
	private static final byte M = (byte)255; // Maximum value
	
	/** Default constructor. */
	public MouseCursor() { }
	
	/** Creates a new cursor of the requested custom type.
		
		@param type the cursor type. Must be one of {@link #ARROW}, {@link #CROSSHAIR}, {@link #MAGNIFIER}, {@link #HAND}.
		
		@return a new cursor of the requested custom type. If, for any reason, the requested custom cursor could not be created, this method returns a standard replacement cursor provided by the system.
		
		@exception IllegalArgumentException if {@code type} is not one of the indicated values.
	*/
	public Cursor create(final int type) {
		
		Cursor cursor = null;
		
		try {
			byte[] pixels = null;
			Point hotspot = new Point();
			String name = "Cursor";
			switch(type) {
				case ARROW:
					messenger.log("Creating custom arrow cursor");
					pixels = new byte[] {
						l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,M,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,M,M,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,M,M,M,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,M,M,M,M,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,M,M,M,M,M,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,M,M,M,M,M,M,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,M,M,M,M,l,l,l,l,l,l,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o
					};
					hotspot.x = 1;
					hotspot.y = 2;
					name = "Arrow";
					break;
				case CROSSHAIR:
					messenger.log("Creating custom crosshair cursor");
					pixels = new byte[] {
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,l,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,l,l,l,l,l,l,l,l,l,l,l,M,l,l,l,l,l,l,l,l,l,l,l,o,o,o,o,o,
						o,o,o,o,l,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,l,o,o,o,o,o,
						o,o,o,o,l,l,l,l,l,l,l,l,l,l,l,M,l,l,l,l,l,l,l,l,l,l,l,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,l,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o
					};
					hotspot.x = 15;
					hotspot.y = 15;
					name = "Cross";
					break;
				case MAGNIFIER:
					messenger.log("Creating custom magnifier cursor");
					pixels = new byte[] {
						o,o,o,o,l,l,l,l,l,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,l,l,l,M,M,M,M,l,l,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,l,l,M,M,M,M,M,M,M,M,l,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,l,M,M,M,M,M,M,M,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,l,M,M,M,M,M,M,M,M,M,M,l,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,M,M,M,M,M,M,M,M,M,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,M,M,M,M,M,M,M,M,M,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,M,M,M,M,M,M,M,M,M,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,M,M,M,M,M,M,M,M,M,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,l,M,M,M,M,M,M,M,M,M,M,l,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,l,M,M,M,M,M,M,M,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,l,l,M,M,M,M,M,M,M,M,l,l,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,l,l,l,M,M,M,M,l,l,l,m,l,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,l,l,l,l,l,l,o,l,l,m,l,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,l,l,m,l,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,l,l,m,l,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,l,m,l,l,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,l,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o
					};
					hotspot.x = 7;
					hotspot.y = 6;
					name = "Magnifier";
					break;
				case HAND:
					messenger.log("Creating custom hand cursor");
					pixels = new byte[] {
						o,o,o,o,o,o,o,o,o,l,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,l,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,l,M,M,l,l,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,l,l,l,M,M,l,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,l,M,M,l,M,M,l,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,l,M,M,l,M,M,l,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,l,M,M,l,M,M,l,M,M,l,l,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,l,M,M,l,M,M,l,M,M,l,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,l,M,M,l,M,M,l,M,M,l,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,l,M,M,l,M,M,l,M,M,l,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,l,M,M,l,M,M,l,M,M,l,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,l,M,M,l,M,M,l,M,M,l,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,l,l,o,o,l,M,M,M,M,M,M,M,M,l,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						l,M,M,l,l,l,M,M,M,M,M,M,M,M,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,l,M,M,M,l,M,M,M,M,M,M,M,M,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,l,M,M,l,M,M,M,M,M,M,M,M,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,l,M,M,M,M,M,M,M,M,M,M,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,l,M,M,M,M,M,M,M,M,M,M,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,l,M,M,M,M,M,M,M,M,M,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,l,M,M,M,M,M,M,M,M,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,l,M,M,M,M,M,M,M,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,l,M,M,M,M,M,M,M,M,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,l,l,l,l,l,l,l,l,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,
						o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o
					};
					hotspot.x = 11;
					hotspot.y = 15;
					name = "Hand";
					break;
				default:
					throw new IllegalArgumentException();
			}
			final Toolkit dtk = Toolkit.getDefaultToolkit();
			final Dimension dim = dtk.getBestCursorSize(SIZE,SIZE);
			if (dim.width != SIZE || dim.height != SIZE) throw new IllegalStateException();
			final byte[] lut = new byte[256];
			for(int i=0; i<256; ++i) lut[i] = (byte)i;
			final MemoryImageSource source = new MemoryImageSource(SIZE,SIZE,new IndexColorModel(8,256,lut,lut,lut,0),pixels,0,SIZE);
			source.setAnimated(false);
			cursor = dtk.createCustomCursor(dtk.createImage(source),hotspot,name);
			
		} catch (Throwable e) {
			messenger.log("Could not create requested cursor");
			switch(type) {
				case ARROW:
					messenger.log("Creating system arrow cursor");
					cursor = new Cursor(Cursor.DEFAULT_CURSOR);
					break;
				case CROSSHAIR:
					messenger.log("Creating system crosshair cursor");
					cursor = new Cursor(Cursor.CROSSHAIR_CURSOR);
					break;
				case MAGNIFIER:
					messenger.log("Creating system magnifier cursor");
					cursor = new Cursor(Cursor.MOVE_CURSOR);
					break;
				case HAND:
					messenger.log("Creating system hand cursor");
					cursor = new Cursor(Cursor.HAND_CURSOR);
					break;
				default:
					messenger.log("Non-supported cursor type");
					throw new IllegalArgumentException("Non-supported cursor type");
			}
		}
		
		return cursor;
	}
	
	/** The object used for message displaying. */
	public final Messenger messenger = new Messenger();
	
}
