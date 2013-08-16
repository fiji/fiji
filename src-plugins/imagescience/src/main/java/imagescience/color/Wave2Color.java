package imagescience.color;

import java.awt.Color;

/** Converts wavelengths of light to corresponding RGBA color values. The underlying algorithm is based on the FORTRAN program by Dan Bruton available on his <a href="http://www.physics.sfasu.edu/astro/color.html" target="newbrowser">Color Science</a> website and uses piecewise linear models of the sensitivity curves of the components. */
public class Wave2Color {
	
	/** Default constructor. */
	public Wave2Color() { }
	
	/** Converts a wavelength to its corresponding RGBA color values.
		
		@param wavelen the wavelength (in nanometers) to be converted.
		
		@param rgba the {@code double} array in which the RGBA values will be stored:<br>
		{@code [0]} = the value of the red (R) component,<br>
		{@code [1]} = the value of the green (G) component,<br>
		{@code [2]} = the value of the blue (B) component,<br>
		{@code [3]} = the value of the alpha (A) component.<br>
		The value of each component is in the range {@code [0.0,1.0]}.
		
		@exception NullPointerException if {@code rgba} is {@code null}.
		
		@exception ArrayIndexOutOfBoundsException if the length of {@code rgba} is not {@code 4}.
	*/
	public void rgba(final double wavelen, final double[] rgba) {
		
		double r, g, b;
		if (wavelen > 645) {
			r = 1;
			g = 0;
			b = 0;
		} else if (wavelen > 580) {
			r = 1;
			g = (645 - wavelen)/(645 - 580);
			b = 0;
		} else if (wavelen > 510) {
			r = (wavelen - 510)/(580 - 510);
			g = 1;
			b = 0;
		} else if (wavelen > 490) {
			r = 0;
			g = 1;
			b = (510 - wavelen)/(510 - 490);
		} else if (wavelen > 440) {
			r = 0;
			g = (wavelen - 440)/(490 - 440);
			b = 1;
		} else if (wavelen > 380) {
			r = (440 - wavelen)/(440 - 380);
			g = 0;
			b = 1;
		} else {
			r = 1;
			g = 0;
			b = 1;
		}
		
		double a;
		if (wavelen > 850) a = 0;
		else if (wavelen > 700) a = (850 - wavelen)/(850 - 700);
		else if (wavelen > 420) a = 1;
		else if (wavelen > 350) a = (wavelen - 350)/(420 - 350);
		else a = 0;
		
		rgba[0] = r;
		rgba[1] = g;
		rgba[2] = b;
		rgba[3] = a;
	}
	
	/** Converts a wavelength to its corresponding RGBA color values.
		
		@param wavelen the wavelength (in nanometers) to be converted.
		
		@return a new {@code double} array with four elements:<br>
		{@code [0]} = the value of the red (R) component,<br>
		{@code [1]} = the value of the green (G) component,<br>
		{@code [2]} = the value of the blue (B) component,<br>
		{@code [3]} = the value of the alpha (A) component.<br>
		The value of each component is in the range {@code [0.0,1.0]}.
	*/
	public double[] rgba(final double wavelen) {
		
		final double[] rgba = new double[4];
		rgba(wavelen,rgba);
		return rgba;
	}
	
	/** Converts a wavelength to its corresponding RGBA color values.
		
		@param wavelen the wavelength (in nanometers) to be converted.
		
		@return a new {@code Color} object containing the RGBA color values.
	*/
	public Color color(final double wavelen) {
		
		final double[] rgba = rgba(wavelen);
		return new Color((float)rgba[0],(float)rgba[1],(float)rgba[2],(float)rgba[3]);
	}
	
}
