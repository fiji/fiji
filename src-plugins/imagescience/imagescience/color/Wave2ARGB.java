package imagescience.color;

/** Converts wavelengths of light to corresponding ARGB values. The underlying algorithm is based on the FORTRAN progam by Dan Bruton available on his <a href="http://www.physics.sfasu.edu/astro/color.html" target="newbrowser">Color Science</a> website and uses piecewise linear models of the sensitivity curves of the components. */
public class Wave2ARGB {
	
	/** Default constructor. */
	public Wave2ARGB() { }
	
	/** Converts a wavelength to its corresponding ARGB values.
		
		@param wavelen the wavelength (in nanometers) to be converted.
		
		@return a new {@code double} array with four elements:<br>
		{@code [0]} = the alpha (A) component,<br>
		{@code [1]} = the red (R) component,<br>
		{@code [2]} = the green (G) component,<br>
		{@code [3]} = the blue (B) component.
	*/
	public double[] convert(final double wavelen) {
		
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
		
		final double[] argb = new double[4];
		argb[0] = a;
		argb[1] = r;
		argb[2] = g;
		argb[3] = b;
		
		return argb;
	}
	
}
