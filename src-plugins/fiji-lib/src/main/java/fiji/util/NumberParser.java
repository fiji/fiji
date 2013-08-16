package fiji.util;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * A static utility class for parsing numbers from strings. This class differs
 * from {@link Integer#parseInt(String)}, {@link Double#parseDouble(String)},
 * etc., in that it respects the current {@link Locale}, allowing to parse
 * numbers in various formats worldwide.
 * 
 * @author Curtis Rueden
 */
public final class NumberParser {

	private NumberParser() {
		// prevent instantiation of utility class
	}

	/**
	 * Parses a {@link Number} from the given string, using the given
	 * {@link NumberFormat}.
	 * 
	 * @throws NumberFormatException if the number cannot be parsed
	 */
	public static Number parseNumber(final String number,
		final NumberFormat format)
	{
		try {
			return format.parse(number);
		}
		catch (final ParseException exc) {
			final NumberFormatException nfe = new NumberFormatException();
			nfe.initCause(exc);
			throw nfe;
		}
	}

	/**
	 * Parses a {@link Number} from the given string, respecting the default
	 * {@link Locale}.
	 * 
	 * @throws NumberFormatException if the number cannot be parsed
	 */
	public static Number parseNumber(final String number) {
		return parseNumber(number, NumberFormat.getNumberInstance());
	}

	/**
	 * Parses an {@code int} from the given string, respecting the default
	 * {@link Locale}. If a decimal number is given as input, the result is
	 * rounded to the nearest integer; see
	 * {@link NumberFormat#getIntegerInstance()} for details.
	 * 
	 * @throws NumberFormatException if the number cannot be parsed
	 */
	public static int parseInteger(final String number) {
		return parseNumber(number, NumberFormat.getIntegerInstance()).intValue();
	}

	/**
	 * Parses a {@code double} from the given string, respecting the default
	 * {@link Locale}.
	 * 
	 * @throws NumberFormatException if the number cannot be parsed
	 */
	public static double parseDouble(final String number) {
		return parseNumber(number).doubleValue();
	}

}