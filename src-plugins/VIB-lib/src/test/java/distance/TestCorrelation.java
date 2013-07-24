/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Some very basic unit tests for the distance.Correlation class */

package distance;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class TestCorrelation extends BaseOfTests {

	@Test
	public void testCorrelation() {

		Correlation c = new Correlation();

		c.reset();
		addMacKayExample(c);
		float correlation=c.correlation();
		assertEquals( -0.2388352,
			      c.correlation(),
			      0.0000001 );

		/* Try it again to check that reset() works
		 * correctly... */

		c.reset();
		addMacKayExample(c);
		correlation=c.correlation();
		assertEquals( -0.2388352,
			      c.correlation(),
			      0.0000001 );

		/* Try the uniform set of values to check we get zero
		   correlation. */

		c.reset();
		addUniform8Bit(c);		
		correlation=c.correlation();
		assertEquals( 0,
			      c.correlation(),
			      0.00001 );


	}

}
