/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package util;

public class Penalty {

	/* When adding a penalty for extreme values while optimising
	   some function, we often want to have a smooth ramp up to
	   the maximum penalty over some range to avoid sudden jumps
	   in the function whose parameters are being optimized.

	   This function will return a penalty of zero when 'value' is
	   less than 'minPenaltyAt', a penalty of 'maxPenalty' when
	   'value' is greater than 'maxPenaltyAt' and use a scaled
	   logistic function to provide a smooth increase in the
	   penalty when value is in the range
	   [ 'minPenaltyAt', 'maxPenaltyAt' ]

	   See: http://en.wikipedia.org/wiki/Logistic_function
	*/

	public static double logisticPenalty( double value,
					      double minPenaltyAt,
					      double maxPenaltyAt,
					      double maxPenalty ) {

		if( value < minPenaltyAt )
			return 0;
		if( value > maxPenaltyAt )
			return maxPenalty;

		double midPoint = (minPenaltyAt + maxPenaltyAt) / 2;

		double minAtMinus6 = 0.00247262315663477;
		double maxAtPlus6 = 0.997527376843365;

		double logisticRange = maxAtPlus6 - minAtMinus6;

		double scaleUpT = 6.0 / (maxPenaltyAt - midPoint);

		double t = ( value - midPoint ) * scaleUpT;

		double rawValue = 1 / (1 + Math.exp( -t ) );

		// Make this piecewise continuous:
		double adjustedValue = (rawValue - minAtMinus6) / (maxAtPlus6 - minAtMinus6);

		return maxPenalty * adjustedValue;
	}
}
