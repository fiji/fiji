package imagescience.utility;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Converts floating-point numbers to formatted strings. */
public class Formatter {
	
	private final DecimalFormat edf = new DecimalFormat("0.#E0",new DecimalFormatSymbols(Locale.US));
	private final DecimalFormat ndf = new DecimalFormat("0.#",new DecimalFormatSymbols(Locale.US));
	
	private int dfdecs = 1;
	private double dflobo = 0.1;
	private double limit = 1.0E-10;
	
	/** Default constructor. */
	public Formatter() { }
	
	/** Sets the limit below which numbers are chopped to {@code 0} by method {@link #d2s(double)}.
		
		@param limit the chop limit. The absolute value is taken and the default is {@code 10}<sup>{@code -10}</sup>. Numbers whose absolute value is less than the limit are considered {@code 0}.
	*/
	public void chop(final double limit) {
		
		this.limit = (limit < 0) ? -limit : limit;
	}
	
	/** Sets the maximum number of decimals used by method {@link #d2s(double)}.
		
		@param n the maximum number of decimals used by method {@link #d2s(double)}. Must be larger than or equal to {@code 0} and less than or equal to {@code 10}. The default number of decimals used is {@code 1}.
		
		@throws IllegalArgumentException if {@code n} is out of range.
	*/
	public void decs(final int n) {
		
		if (n < 0) throw new IllegalArgumentException("Number of decimals less than 0");
		else if (n > 10) throw new IllegalArgumentException("Number of decimals larger than 10");
		
		if (dfdecs != n) {
			dfdecs = n;
			dflobo = 1.0/Math.pow(10,dfdecs);
			final StringBuffer sb = new StringBuffer("0");
			if (dfdecs > 0) sb.append(".");
			for (int i=0; i<dfdecs; ++i) sb.append("#");
			final String patt = sb.toString();
			edf.applyPattern(patt+"E0");
			ndf.applyPattern(patt);
		}
	}
	
	private String nan = "NaN";
	
	/** Sets the string representation of the value {@code Double.NaN} used by method {@link #d2s(double)}.
		
		@param nan the string representation of the value {@code Double.NaN} used by method {@link #d2s(double)}. The default string is "NaN".
		
		@throws NullPointerException if {@code nan} is {@code null}.
	*/
	public void nan(final String nan) {
		
		if (nan == null) throw new NullPointerException();
		
		this.nan = nan;
	}
	
	private String inf = "Inf";
	
	/** Sets the string representation of an infinite value used by method {@link #d2s(double)}.
		
		@param inf the string representation of an infinite value used by method {@link #d2s(double)}. The default string is "Inf", which translates to "+Inf" for the value {@code Double.POSITIVE_INFINITY}, and "-Inf" for the value {@code Double.NEGATIVE_INFINITY}.
		
		@throws NullPointerException if {@code inf} is {@code null}.
	*/
	public void inf(final String inf) {
		
		if (inf == null) throw new NullPointerException();
		
		this.inf = inf;
	}
	
	/** Returns a {@code String} representation of a {@code double} value.
		
		@param d the {@code double} value to be represented.
		
		@return a new {@code String} object containing a string representation of {@code d}. The maximum number of decimals used in representing {@code d} can be specified with method {@link #decs(int)}. The value of {@code d} is rounded to the specified maximum number of decimals. The returned string will contain less than the maximum number of decimals if {@code d} can be represented exactly that way. In particular, if {@code d} is equal to an integer value, the returned string represents that integer value, without decimals and preceding decimal separator symbol. The string returned when {@code Double.isNaN(d)} yields {@code true} can be specified with method {@link #nan(String)}. Similarly, the string returned when {@code Double.isInfinite(d)} yields {@code true} can be specified with method {@link #inf(String)}. The returned string is "0" if the absolute value of {@code d} is less than the limit set with method {@link #chop(double)}.
	*/
	public String d2s(final double d) {
		
		if (Double.isNaN(d)) return nan;
		if (Double.isInfinite(d)) return (d < 0) ? ("-"+inf) : ("+"+inf);
		
		final long dr = Math.round(d);
		if (dr == d) return String.valueOf(dr);
		
		final double da = (d < 0) ? -d : d;
		if (da < limit) return "0";
		
		String ds = nan;
		if (da < dflobo || da > 10000000) ds = edf.format(d);
		else ds = ndf.format(d);
		
		return ds;
	}
	
}
