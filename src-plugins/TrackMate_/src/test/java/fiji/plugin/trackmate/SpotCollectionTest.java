package fiji.plugin.trackmate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

public class SpotCollectionTest {
	
	private static final int N_SPOTS = 100;
	private static final int N_FRAMES = 50;
	private SpotCollection sc;
	private ArrayList<Integer> frames;

	@Before
	public void setUp() throws Exception {
		
		// Create a spot collection of 50 odd frame number, ranging from 1 to 99
		frames = new ArrayList<Integer>(50);
		sc = new SpotCollection();
		
		for (int i = 1; i < N_FRAMES*2; i = i+2) {
			
			// Store frames
			frames.add(i);
			
			// For each frame, create 100 spots, with X, Y, Z, T and QUALITY linearly increasing
			HashSet<Spot> spots = new HashSet<Spot>(100);
			for (int j = 0; j < N_SPOTS; j++) {
				double[] pos = new double[] { j, j, j };
				Spot spot = new Spot(pos);
				spot.putFeature(Spot.POSITION_T, j);
				spot.putFeature(Spot.QUALITY, j);
				spot.putFeature(Spot.RADIUS, j/2);
				spots.add(spot);
			}
			sc.put(i, spots);
		}
		
	}

	@Test
	public void testAdd() {
		// Pre-Test
		for (Integer frame : frames) {
			assertEquals(N_SPOTS, sc.getNSpots(frame, false));
		}
		// Add a spot to target frame 
		int targetFrame = 1 + 2 * new Random().nextInt(50);
		Spot spot = new Spot(new double[] { 0, 0, 0 });
		sc.add(spot, targetFrame);
		// Test
		for (Integer frame : frames) {
			if (frame == targetFrame) {
				assertEquals(N_SPOTS+1, sc.getNSpots(frame, false));
			} else {
				assertEquals(N_SPOTS, sc.getNSpots(frame, false));
			}
		}
	}
	
	@Test
	public void testRemove() {
		// Pre-Test
		for (Integer frame : frames) {
			assertEquals(N_SPOTS, sc.getNSpots(frame, false));
		}
		
		// Remove a random spot from target frame
		int targetFrame = 1 + 2 * new Random().nextInt(50);
		Iterator<Spot> it = sc.iterator(targetFrame, false);
		Spot targetSpot = null;
		for (int i = 0; i < new Random().nextInt(N_SPOTS); i++) {
			targetSpot = it.next();
		}
		boolean flag = sc.remove(targetSpot, targetFrame);
		assertTrue("The target spot " + targetSpot +" could not be removed from target frame " + targetFrame + ".", flag);
			
		// Test
		for (Integer frame : frames) {
			if (frame == targetFrame) {
				assertEquals(N_SPOTS-1, sc.getNSpots(frame, false));
			} else {
				assertEquals(N_SPOTS, sc.getNSpots(frame, false));
			}
		}
		
		// Remove the spot from the wrong frame - we should fail
		targetFrame++;
		flag = sc.remove(targetSpot, targetFrame);
		assertFalse("The target spot " + targetSpot +" could be removed from wrong frame " + targetFrame + ".", flag);
	}

	@Test
	public void testIsVisible() {
		// In the beginning, none shall be visible
		Iterator<Spot> it = sc.iterator(false);
		while (it.hasNext()) {
			Spot spot = it.next();
			int frame = spot.getFeature(Spot.FRAME).intValue();
			assertFalse("Spot " + spot + " is visible, but should not.", sc.isVisible(spot, frame));
		}
		// Mark a random spot as visible
		int targetFrame = 1 + 2 * new Random().nextInt(N_FRAMES);
		it = sc.iterator(targetFrame, false);
		Spot targetSpot = null;
		for (int i = 0; i < new Random().nextInt(N_SPOTS); i++) {
			targetSpot = it.next();
		}
		sc.setVisible(targetSpot, targetFrame, true);
		// Test for visibility
		it = sc.iterator(false);
		while (it.hasNext()) {
			Spot spot = it.next();
			int frame = spot.getFeature(Spot.FRAME).intValue();
			if (spot == targetSpot) {
				assertTrue("Target spot " + spot + " should be visible, but is not.", sc.isVisible(spot, frame));
			} else {
				assertFalse("Spot " + spot + " is visible, but should not.", sc.isVisible(spot, frame));
			}
		}
	}


	@Test
	public void testFilterFeatureFilter() {
		
		// Test that all are invisible for now
		assertEquals(0, sc.getNSpots(true));
		// Filter by quality below 2. Should leave 3 spots per frame (0, 1 & 2)
		FeatureFilter filter = new FeatureFilter(Spot.QUALITY, 2d, false);
		sc.filter(filter);
		assertEquals(3 * N_FRAMES, sc.getNSpots(true));
	}

	@Test
	public void testFilterCollectionOfFeatureFilter() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetClosestSpot() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetSpotAt() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetNClosestSpots() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetNSpots() {
		assertEquals(N_SPOTS * N_FRAMES, sc.getNSpots(false) );
		
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetNSpotsInt() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testIterator() {
		// Iterate over all
		Iterator<Spot> it = sc.iterator(false);
		int iteratedOver = 0;
		while (it.hasNext()) {
			it.next();
			iteratedOver++;
		}
		assertEquals(N_SPOTS * N_FRAMES, iteratedOver);
		
		// Iterate over visible
		it = sc.iterator(true);
		iteratedOver = 0;
		while (it.hasNext()) {
			it.next();
			iteratedOver++;
		}
		assertEquals(0, iteratedOver);
		
		// Mark 10 spots as visible in eaxh frame
		int N_MARKED_PER_FRAME = 10;
		List<Spot> markedSpots = new ArrayList<Spot>(N_MARKED_PER_FRAME * N_FRAMES);
		for (Integer frame : frames) {
			it = sc.iterator(frame, false);
			for (int i = 0; i < N_MARKED_PER_FRAME; i++) {
				Spot spot = it.next();
				markedSpots.add(spot);
				sc.setVisible(spot, frame, true);
			}
		}
		
		// See if we iterate over them.
		it = sc.iterator(true);
		iteratedOver = 0;
		while (it.hasNext()) {
			Spot spot = it.next();
			assertTrue("The spot " + spot + " should be contained in the list of marked spot, but is not.", markedSpots.contains(spot));
			iteratedOver++;
		}
		assertEquals("We should have iterated over " + (N_MARKED_PER_FRAME*N_FRAMES) + " marked spots, but have iterated over " + iteratedOver + ".", 
				N_MARKED_PER_FRAME*N_FRAMES, iteratedOver);
	}

	@Test
	public void testIteratorFrame() {
		int targetFrame = frames.get(0);
		// Iterate over all
		Iterator<Spot> it = sc.iterator(targetFrame, false);
		int iteratedOver = 0;
		while (it.hasNext()) {
			it.next();
			iteratedOver++;
		}
		assertEquals(N_SPOTS, iteratedOver);
		// Iterate over visible
		it = sc.iterator(targetFrame, true);
		iteratedOver = 0;
		while (it.hasNext()) {
			it.next();
			iteratedOver++;
		}
		assertEquals(0, iteratedOver);
		// Mark 10 spots as visible
		it = sc.iterator(targetFrame, false);
		int N_MARKED = 10;
		List<Spot> markedSpots = new ArrayList<Spot>(N_MARKED);
		for (int i = 0; i < N_MARKED; i++) {
			Spot spot = it.next();
			markedSpots.add(spot);
			sc.setVisible(spot, targetFrame, true);
		}
		// See if we iterate over them.
		it = sc.iterator(targetFrame, true);
		iteratedOver = 0;
		while (it.hasNext()) {
			Spot spot = it.next();
			assertTrue("The spot " + spot + " should be contained in the list of marked spot, but is not.", markedSpots.contains(spot));
			iteratedOver++;
		}
		assertEquals("We should have iterated over " + N_MARKED + " marked spots, but have iterated over " + iteratedOver + ".", 
				N_MARKED, iteratedOver);
	}

	@Test
	public void testPut() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testFirstKey() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testLastKey() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testKeySet() {
		fail("Not yet implemented"); // TODO
	}

}
