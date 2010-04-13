/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Some very basic unit tests for the distance.MutualInformation class */

package distance;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class TestMutualInformation extends BaseOfTests {
	
	MutualInformation exampleMeasure;

	@Test
	public void testMutualInformationValue() {
	    
		// Try an example slightly adapted from David MacKay's book:

		exampleMeasure = new MutualInformation( 1, 4, 4 );

		exampleMeasure.reset();

		addMacKayExample(exampleMeasure);
	    
		float mi = exampleMeasure.mutualInformation();
		assertEquals( 0.375, mi, 0.0000001 );

		exampleMeasure.reset();
		addMacKayExample(exampleMeasure);
		float miAfterReset = exampleMeasure.mutualInformation();
		assertEquals( 0.375, miAfterReset, 0.0000001 );
	}

	@Test
	public void testEntropies() {

		exampleMeasure = new MutualInformation( 1, 4, 4 );
		exampleMeasure.reset();

		addMacKayExample(exampleMeasure);

		float mi = exampleMeasure.mutualInformation();

		float h1 = exampleMeasure.getEntropy1();
		assertEquals( 1.75, h1, 0.0000001 );

		float h2 = exampleMeasure.getEntropy2();
		assertEquals( 2, h2, 0.0000001 );

		float h12 = exampleMeasure.getJointEntropy();
		assertEquals( 3.375, h12, 0.0000001 );
	}

	@Test
	public void testFillIn8Bit() {
		
		exampleMeasure = new MutualInformation();
		exampleMeasure.reset();

		addUniform8Bit(exampleMeasure);

		/* Strictly speaking we shouldn't be examining the
		   internal state, but it's important that binning in
		   the 8 bit case still works properly... */

		for( int i = 0; i < 256 * 256; ++i ) {
			assertEquals( 1, exampleMeasure.joint[i], 0.0000001 );
		}

		float mi = exampleMeasure.mutualInformation();

		assertEquals( 8, exampleMeasure.getEntropy1(), 0.0000001 );
		assertEquals( 8, exampleMeasure.getEntropy2(), 0.0000001 );
		assertEquals( 16, exampleMeasure.getJointEntropy(), 0.0000001 );

		assertEquals( 0, mi, 0.0000001 );
	}
}
