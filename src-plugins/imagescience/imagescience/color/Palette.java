package imagescience.color;
import java.awt.Color;

/** A palette of 100 different colors. */
public class Palette {
	
	private final static int SIZE = 100;
	
	private final static Color[] spectrum = {
		new Color(0x8B0015), new Color(0xBE0521), new Color(0xFF0000), new Color(0xF7556D), new Color(0xFF8B9C),
		new Color(0x8B3900), new Color(0xBE5205), new Color(0xFF6900), new Color(0xF79855), new Color(0xFFBB8B),
		new Color(0x8B5300), new Color(0xBE7405), new Color(0xFF9900), new Color(0xF7B655), new Color(0xFFD18B),
		new Color(0x8B6E00), new Color(0xBE9705), new Color(0xFFC900), new Color(0xF7D555), new Color(0xFFE68B),
		new Color(0x8B8800), new Color(0xBEBA05), new Color(0xFFFF00), new Color(0xF7F355), new Color(0xFFFC8B),
		new Color(0x748B00), new Color(0xA0BE05), new Color(0xD6FF00), new Color(0xDDF755), new Color(0xECFF8B),
		new Color(0x5A8B00), new Color(0x7EBE05), new Color(0xA6FF00), new Color(0xBEF755), new Color(0xD6FF8B),
		new Color(0x268B00), new Color(0x38BE05), new Color(0x00FF00), new Color(0x81F755), new Color(0xABFF8B),
		new Color(0x008B42), new Color(0x05BE5D), new Color(0x00FF79), new Color(0x55F7A2), new Color(0x8BFFC2),
		new Color(0x008B76), new Color(0x05BEA3), new Color(0x00FFFF), new Color(0x55F7DF), new Color(0x8BFFEE),
		new Color(0x006C8B), new Color(0x0595BE), new Color(0x00C6FF), new Color(0x55D2F7), new Color(0x8BE5FF),
		new Color(0x00528B), new Color(0x0572BE), new Color(0x0096FF), new Color(0x55B4F7), new Color(0x8BCFFF),
		new Color(0x00388B), new Color(0x054FBE), new Color(0x0066FF), new Color(0x5596F7), new Color(0x8BB9FF),
		new Color(0x001E8B), new Color(0x052CBE), new Color(0x0036FF), new Color(0x5577F7), new Color(0x8BA4FF),
		new Color(0x00038B), new Color(0x050ABE), new Color(0x0000FF), new Color(0x5559F7), new Color(0x8B8EFF),
		new Color(0x31008B), new Color(0x4605BE), new Color(0x5900FF), new Color(0x8E55F7), new Color(0xB48BFF),
		new Color(0x4B008B), new Color(0x6905BE), new Color(0x8900FF), new Color(0xAC55F7), new Color(0xC98BFF),
		new Color(0x65008B), new Color(0x8B05BE), new Color(0xB900FF), new Color(0xCA55F7), new Color(0xDF8BFF),
		new Color(0x7F008B), new Color(0xAE05BE), new Color(0xFF00FF), new Color(0xE955F7), new Color(0xF58BFF),
		new Color(0x8B0063), new Color(0xBE0589), new Color(0xFF00B6), new Color(0xF755C8), new Color(0xFF8BDE)
	};
	
	private final static int[] arbitrary = {
		2, 22, 37, 47, 72, 92, 12, 86, 40, 14,
		60, 34, 1, 4, 15, 83, 39, 45, 26, 7,
		96, 68, 42, 8, 95, 64, 5, 32, 81, 94,
		49, 35, 24, 6, 61, 90, 3, 75, 38, 23,
		0, 98, 74, 25, 11, 88, 57, 36, 9, 66,
		87, 51, 33, 54, 50, 13, 97, 80, 73, 48,
		10, 17, 41, 56, 70, 93, 78, 52, 27, 65,
		18, 30, 63, 76, 84, 62, 29, 16, 59, 82,
		55, 28, 43, 69, 99, 53, 20, 71, 46, 19,
		91, 67, 31, 21, 85, 77, 79, 89, 44, 58
	};
	
	/** The spectrum mode. If the palette is operating in this mode, the {@link #get(int)} method (with index running from {@code 0} to {@code 99}) and the {@link #next()} method both yield a 5x20 color spectrum (meaning 5 different tints of each of the 20 different colors sampled from the visible spectrum). */
	public final static int SPECTRUM = 0;
	
	/** The arbitrary mode. If the palette is operating in this mode, the {@link #get(int)} method (with index running from {@code 0} to {@code 99}) and the {@link #next()} method both yield the same set of colors as in the spectrum mode, but put in arbitrary (yet fixed) order, such that the perceptual difference between successive colors is much larger. */
	public final static int ARBITRARY = 1;
	
	private final Color[] colors = new Color[SIZE];
	
	private final boolean[] used = new boolean[SIZE];
	
	private final int mode;
	
	private int nextindex;
	
	/** Default constructor. Results in a palette operating in the arbitrary mode. */
	public Palette() { this(ARBITRARY); }
	
	/** Mode constructor. Results in a palette operating in the given mode.
		
		@param mode the palette mode. Must be one of the static fields of this class.
		
		@exception IllegalArgumentException if {@code mode} is not one of the static fields of this class.
	*/
	public Palette(final int mode) {
		
		this.mode = mode;
		switch (mode) {
			case SPECTRUM:
				for (int i=0; i<SIZE; ++i)
				colors[i] = spectrum[i];
				break;
			case ARBITRARY:
				for (int i=0; i<SIZE; ++i)
				colors[i] = spectrum[arbitrary[i]];
				break;
			default:
				throw new IllegalArgumentException("Invalid palette mode");
		}
		reset();
	}
	
	/** Returns the palette mode.
		
		@return the palette mode. The returned value is equal to one of the static fields of this class.
	*/
	public int mode() { return mode; }
	
	/** Returns the color at the given index (modulo 100).
		
		@return the color at the given index (modulo 100). The actual color returned depends on the mode of the palette.
	*/
	public Color get(final int index) {
		
		return colors[(index<0)?((index+1)%SIZE+(SIZE-1)):(index%SIZE)];
	}
	
	/** Returns the index of the given color.
		
		@param color the color whose index is to be returned.
		
		@return the index of the given color. The actual index returned depends on the mode of the palette. If {@code color} is {@code null} or it is not in the palette, the method returns -1.
	*/
	public int index(final Color color) {
		
		int index = -1;
		for (int i=0; i<SIZE; ++i) {
			if (colors[i].equals(color)) {
				index = i;
				break;
			}
		}
		return index;
	}
	
	/** Returns the next color. Calling this method repeatedly yields a sequence of colors with a period of 100 colors. Colors that have been marked explicitly as used will first be skipped until all colors have been used and a new cycle is started.
		
		@return the next color. The actual color returned depends on the mode of the palette.
	*/
	public Color next() {
		
		while (true) {
			if (nextindex == SIZE) { reset(); break; }
			else if (!used[nextindex]) break;
			else ++nextindex;
		}
		used[nextindex] = true;
		return colors[nextindex++];
	}
	
	/** Marks the given color as used if it is in the palette. Colors that have been marked as used will first be skipped by the {@link #next()} method until all colors have been used and a new cycle is started.
		
		@param color the color to be marked as used.
	*/
	public void used(final Color color) {
		
		for (int i=0; i<SIZE; ++i) {
			if (colors[i].equals(color)) {
				used[i] = true;
				break;
			}
		}
	}
	
	/** Resets the counter used by the {@link #next()} method. */
	public void reset() {
		
		for (int i=0; i<SIZE; ++i) used[i] = false;
		nextindex = 0;
	}
	
}
