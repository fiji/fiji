/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Some very basic unit tests for the distance.Euclidean class */

package distance;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class TestEuclidean extends BaseOfTests {
	
    
	/* Use the same example from MacKay: */

	@Test
	public void testEuclidean() {
		Euclidean e = new Euclidean();
		e.reset();
		addMacKayExample(e);
		float distance = e.distance();
		assertEquals(1.82002747232013,distance,0.0000001);
	}
}
