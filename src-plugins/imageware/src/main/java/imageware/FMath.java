package imageware;

/**
* Class FMath.
* The <b>FMath</b> class provides methods for carrying out a number of
* elementary mathematical operations.<br><br>
* @author	Erik Meijering
*			Biomedical Imaging Group
*			Swiss Federal Institute of Technology Lausanne
*			EPFL, CH-1015 Lausanne, Switzerland
*/

public final class FMath {

    /** Returns the largest integral value that is not greater than the
        argument. If the argument value is already equal to a
        mathematical integer, then the result is the same as the
        argument. Note that the method works only for <b>float</b>
        values that fall within the range spanned by the integers. In
        that case, this method gives the same result as the
        corresponding method in Java's <b>Math</b> class, but is in
        general much faster. */
    public static int floor(final float f) {
		if (f >= 0.0f) return (int)f;
		else {
		    final int iAdd = (int)f - 1;
		    return (((int)(f - iAdd)) + iAdd);
		}
    }

    /** Returns the largest integral value that is not greater than the
        argument. If the argument value is already equal to a
        mathematical integer, then the result is the same as the
        argument. Note that the method works only for <b>double</b>
        values that fall within the range spanned by the integers. In
        that case, this method gives the same result as the
        corresponding method in Java's <b>Math</b> class, but is in
        general much faster. */
    public static int floor(final double d) {
		if (d >= 0.0) return (int)d;
		else {
		    final int iAdd = (int)d - 1;
		    return (((int)(d - iAdd)) + iAdd);
		}
    }

    /** Returns the smallest integral value that is not less than the
        argument. If the argument value is already equal to a
        mathematical integer, then the result is the same as the
        argument. Note that the method works only for <b>float</b>
        values that fall within the range spanned by the integers. In
        that case, this method gives the same result as the
        corresponding method in Java's <b>Math</b> class, but is in
        general much faster.  */
    public static int ceil(final float f) {
		final float mf = -f;
		if (mf >= 0.0f) return -((int)mf);
		else {
		    final int iAdd = (int)mf - 1;
		    return -(((int)(mf - iAdd)) + iAdd);
		}
    }

    /** Returns the smallest integral value that is not less than the
        argument. If the argument value is already equal to a
        mathematical integer, then the result is the same as the
        argument. Note that the method works only for <b>double</b>
        values that fall within the range spanned by the integers. In
        that case, this method gives the same result as the
        corresponding method in Java's <b>Math</b> class, but is in
        general much faster.  */
    public static int ceil(final double d) {
		final double md = -d;
		if (md >= 0.0) return -((int)md);
		else {
		    final int iAdd = (int)md - 1;
		    return -(((int)(md - iAdd)) + iAdd);
		}
    }

    /** Returns the integral value closest to the argument. Note that
        the method works only for <b>float</b> values that fall within
        the range spanned by the integers. In that case, this method
        gives the same result as the corresponding method in Java's
        <b>Math</b> class, but is in general much faster. */
    public static int round(final float f) {
		final float f05 = f + 0.5f;
		if (f05 >= 0.0) return (int)f05;
		else {
		    final int iAdd = (int)f05 - 1;
		    return (((int)(f05 - iAdd)) + iAdd);
		}
    }

    /** Returns the integral value closest to the argument. Note that
        the method works only for <b>double</b> values that fall within
        the range spanned by the integers. In that case, this method
        gives the same result as the corresponding method in Java's
        <b>Math</b> class, but is in general much faster. */
    public static int round(final double d) {
		final double d05 = d + 0.5;
		if (d05 >= 0.0) return (int)d05;
		else {
		    final int iAdd = (int)d05 - 1;
		    return (((int)(d05 - iAdd)) + iAdd);
		}
    }

    /** Returns the minimum value of the two arguments. */
    public static float min(final float f1, final float f2) {
		return ((f1 < f2) ? f1 : f2);
    }

    /** Returns the minimum value of the two arguments. */
    public static double min(final double d1, final double d2) {
		return ((d1 < d2) ? d1 : d2);
    }
    
    /** Returns the minimum value of the two arguments. */
    public static float max(final float f1, final float f2) {
		return ((f1 > f2) ? f1 : f2);
    }
    
    /** Returns the maximum value of the two arguments. */
    public static double max(final double d1, final double d2) {
		return ((d1 > d2) ? d1 : d2);
    }

}
