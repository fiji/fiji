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
	 * <p>
	 * Strings representing integers or fractions with a decimal point, optionally
	 * signed, are parsed with the US locale. Other strings are first parsed with
	 * the default locale and if that fails, parsing is re-attempted using the
	 * en-US locale as well before giving up.
	 * </p>
	 * 
	 * @throws NumberFormatException if the number cannot be parsed
	 */
	public static Number parseNumber(final String number) {
		if (number.matches("-?([0-9]+|[0-9]*\\.[0-9]+)")) {
			return parseNumber(number, NumberFormat.getNumberInstance(Locale.US));
		}
		if ("Infinity".equals(number)) return Double.POSITIVE_INFINITY;
		if ("-Infinity".equals(number)) return Double.NEGATIVE_INFINITY;
		if ("NaN".equals(number)) return Double.NaN;
		try {
			return parseNumber(number, NumberFormat.getNumberInstance());
		}
		catch (final NumberFormatException exc) {
			// try again with en-US locale
			return parseNumber(number, NumberFormat.getNumberInstance(Locale.US));
		}
	}

	/**
	 * Parses an {@code int} from the given string, respecting the default
	 * {@link Locale}. If a decimal number is given as input, the result is
	 * rounded to the nearest integer; see
	 * {@link NumberFormat#getIntegerInstance()} for details.
	 * <p>
	 * If parsing with the default locale fails, parsing is reattempted using the
	 * en-US locale as well before giving up.
	 * </p>
	 * 
	 * @throws NumberFormatException if the number cannot be parsed
	 */
	public static int parseInteger(final String number) {
		Number n;
		try {
			n = parseNumber(number, NumberFormat.getIntegerInstance());
		}
		catch (final NumberFormatException exc) {
			// try again with en-US locale
			n = parseNumber(number, NumberFormat.getIntegerInstance(Locale.US));
		}
		return n.intValue();
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
