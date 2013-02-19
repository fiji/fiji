package imagescience.utility;

/** Contains the name and version number of the ImageScience library. */
public class ImageScience {
	
	private static final String NAME = "ImageScience";
	private static final String VERSION = "2.4.1";
	
	/** Default constructor. */
	public ImageScience() { }
	
	/** Returns the version number of the library. */
	public static String version() { return VERSION; }
	
	/** Returns the name of the library. */
	public static String name() { return NAME; }
	
	/** Returns the name and version number of the library appended with a colon and space. */
	public static String prelude() { return (NAME + " " + VERSION + ": "); }
	
}
