/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package util;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class TestPenalty {

	@Test
	public void testLogisticPenalty() {

		double epsilon = 0.00000001;

		assertEquals( 0.0, Penalty.logisticPenalty( 1, 5, 10, 100 ), epsilon );

		assertEquals( 0.0, Penalty.logisticPenalty( 3, 5, 10, 100 ), epsilon );

		assertEquals( 0.0, Penalty.logisticPenalty( 5, 5, 10, 100 ), epsilon );

		assertEquals( 2.424426427877, Penalty.logisticPenalty( 6, 5, 10, 100 ), epsilon );

		assertEquals( 23.01406957716, Penalty.logisticPenalty( 7, 5, 10, 100 ), epsilon );

		assertEquals( 76.98593042283, Penalty.logisticPenalty( 8, 5, 10, 100 ), epsilon );

		assertEquals( 97.57557357212, Penalty.logisticPenalty( 9, 5, 10, 100 ), epsilon );

		assertEquals( 100.0, Penalty.logisticPenalty( 10, 5, 10, 100 ), epsilon );

		assertEquals( 100.0, Penalty.logisticPenalty( 11, 5, 10, 100 ), epsilon );

		assertEquals( 100.0, Penalty.logisticPenalty( 115, 5, 10, 100 ), epsilon );
	}
}


