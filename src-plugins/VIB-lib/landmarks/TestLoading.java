/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package landmarks;

import ij.ImagePlus;
import util.BatchOpener;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class TestLoading {

	@Test
	public void testLoadingXML() {
		// Manually create what the test file should produce:
		NamedPointSet nps = new NamedPointSet();
		nps.add( new NamedPointWorld(
				 "the centre of the ellipsoid body",
				 295.00580720092915,
				 157.95586527293844,
				 91.85064935064935 ) );
		nps.add( new NamedPointWorld( "the top of the left alpha lobe of the mushroom body" ) );
		nps.add( new NamedPointWorld(
				 "the top of the right alpha lobe of the mushroom body",
				 360.04645760743324,
				 72.00929152148665,
				 100.81168831168831 ) );
		nps.add( new NamedPointWorld( "the left tip of the protocerebral bridge" ) );
		nps.add( new NamedPointWorld( "the right tip of the protocerebral bridge" ) );
		nps.add( new NamedPointWorld( "the most dorsal point of the left part of the protocerebral bridge" ) );
		nps.add( new NamedPointWorld( "the most dorsal point of the right part of the protocerebral bridge" ) );
		nps.add( new NamedPointWorld( "the most lateral part of the mushroom body on the left" ) );
		nps.add( new NamedPointWorld( "the most lateral part of the mushroom body on the right" ) );

		NamedPointSet testNPS = null;

		try {
			testNPS = NamedPointSet.fromFile( "test-images/CantonF41c-reduced.tif.points.xml" );
		} catch( NamedPointSet.PointsFileException e ) {
			assertTrue( false );
		}
		assertTrue( testNPS.equals(nps) );

		try {
			testNPS = NamedPointSet.forImage( "test-images/CantonF41c-reduced.tif" );
		} catch( NamedPointSet.PointsFileException e ) {
			assertTrue( false );
		}
		assertTrue( testNPS.equals(nps) );

		ImagePlus imagePlus = BatchOpener.openFirstChannel( "test-images/CantonF41c-reduced.tif" );
		try {
			testNPS = NamedPointSet.forImage( imagePlus );
		} catch( NamedPointSet.PointsFileException e ) {
			assertTrue( false );
		}
		assertTrue( testNPS.equals(nps) );
	}


	@Test
	public void testLoadingTSV() {
		NamedPointSet nps = new NamedPointSet();
		nps.add( new NamedPointWorld( "the centre of the ellipsoid body" ) );
		nps.add( new NamedPointWorld( "the left tip of the protocerebral bridge",
					      329.31556348952125,
					      182.13852265821257,
					      62.983801242131975 ) );
		nps.add( new NamedPointWorld( "the right tip of the protocerebral bridge" ) );
		nps.add( new NamedPointWorld( "the most dorsal point of the left part of the protocerebral bridge" ) );
		nps.add( new NamedPointWorld( "the most dorsal point of the right part of the protocerebral bridge" ) );
		nps.add( new NamedPointWorld( "the top of the left alpha lobe of the mushroom body" ) );
		nps.add( new NamedPointWorld( "the top of the right alpha lobe of the mushroom body",
					      302.32248451497037,
					      55.90390299410485,
					      28.792594853546046 ) );
		nps.add( new NamedPointWorld( "the most lateral part of the mushroom body on the left" ) );
		nps.add( new NamedPointWorld( "the most lateral part of the mushroom body on the right"  ) );

		NamedPointSet testNPS = null;

		try {
			testNPS = NamedPointSet.fromFile( "test-images/tidied-mhl-62yxUAS-lacZ0-reduced.tif.points.R" );
		} catch( NamedPointSet.PointsFileException e ) {
			assertTrue( false );
		}
		assertTrue( testNPS.equals(nps) );

		try {
			testNPS = NamedPointSet.forImage( "test-images/tidied-mhl-62yxUAS-lacZ0-reduced.tif" );
		} catch( NamedPointSet.PointsFileException e ) {
			assertTrue( false );
		}
		assertTrue( testNPS.equals(nps) );

		ImagePlus imagePlus = BatchOpener.openFirstChannel( "test-images/tidied-mhl-62yxUAS-lacZ0-reduced.tif" );
		try {
			testNPS = NamedPointSet.forImage( imagePlus );
		} catch( NamedPointSet.PointsFileException e ) {
			assertTrue( false );
		}
		assertTrue( testNPS.equals(nps) );
	}

	@Test
	public void testLoadingPseudoYAML() {

		NamedPointSet nps = new NamedPointSet();
		nps.add( new NamedPointWorld(
				 "the centre of the ellipsoid body",
				 76.71103997439151,
				 130.92786521945018,
				 81.6 ) );
		nps.add( new NamedPointWorld(
				 "the left tip of the protocerebral bridge",
				 106.12655154351908,
				 155.72917889538127,
				 14.399999999999999 ) );
		nps.add( new NamedPointWorld( "the right tip of the protocerebral bridge" ) );
		nps.add( new NamedPointWorld( "the most dorsal point of the left part of the protocerebral bridge" ) );
		nps.add( new NamedPointWorld( "the most dorsal point of the right part of the protocerebral bridge" ) );
		nps.add( new NamedPointWorld( "the top of the left alpha lobe of the mushroom body" ) );
		nps.add( new NamedPointWorld( "the top of the right alpha lobe of the mushroom body" ) );
		nps.add( new NamedPointWorld( "the most lateral part of the mushroom body on the left" ) );
		nps.add( new NamedPointWorld( "the most lateral part of the mushroom body on the right" ) );


		NamedPointSet npsUncalibrated = new NamedPointSet();
		npsUncalibrated.add( new NamedPointWorld(
				 "the centre of the ellipsoid body",
				 266, 454, 68 ) );
		npsUncalibrated.add( new NamedPointWorld(
				 "the left tip of the protocerebral bridge",
				 368, 540, 12 ) );
		npsUncalibrated.add( new NamedPointWorld( "the right tip of the protocerebral bridge" ) );
		npsUncalibrated.add( new NamedPointWorld( "the most dorsal point of the left part of the protocerebral bridge" ) );
		npsUncalibrated.add( new NamedPointWorld( "the most dorsal point of the right part of the protocerebral bridge" ) );
		npsUncalibrated.add( new NamedPointWorld( "the top of the left alpha lobe of the mushroom body" ) );
		npsUncalibrated.add( new NamedPointWorld( "the top of the right alpha lobe of the mushroom body" ) );
		npsUncalibrated.add( new NamedPointWorld( "the most lateral part of the mushroom body on the left" ) );
		npsUncalibrated.add( new NamedPointWorld( "the most lateral part of the mushroom body on the right" ) );

		NamedPointSet testNPS = null;

		/*
		try {
			testNPS = NamedPointSet.fromFile( "test-images/71yAAeastmost.labels.points" );
		} catch( NamedPointSet.PointsFileException e ) {
			assertTrue( false );
		}
		assertTrue( testNPS.equals(npsUncalibrated) );
		*/

		try {
			testNPS = NamedPointSet.forImage( "test-images/71yAAeastmost.labels" );
		} catch( NamedPointSet.PointsFileException e ) {
			assertTrue( false );
		}
		assertTrue( testNPS.equals(nps) );

		ImagePlus imagePlus = BatchOpener.openFirstChannel( "test-images/71yAAeastmost.labels" );
		try {
			testNPS = NamedPointSet.forImage( imagePlus );
		} catch( NamedPointSet.PointsFileException e ) {
			assertTrue( false );
		}
		assertTrue( testNPS.equals(nps) );
	}
}
