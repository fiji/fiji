package tests;

import static org.junit.Assert.assertEquals;
import net.imglib2.TwinCursor;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import org.junit.Test;

import algorithms.MandersColocalization;
import algorithms.MandersColocalization.MandersResults;
import algorithms.MissingPreconditionException;

public class MandersColocalizationTest extends ColocalisationTest {

	@Test
	public void mandersPaperImagesTest() throws MissingPreconditionException {
		MandersColocalization<UnsignedByteType> mc =
				new MandersColocalization<UnsignedByteType>();

		TwinCursor<UnsignedByteType> cursor;
		MandersResults r;
		// test A-A combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.randomAccess(),
				mandersA.randomAccess(),
				Views.iterable(mandersAlwaysTrueMask).localizingCursor());

		r = mc.calculateMandersCorrelation(cursor,
				Util.getTypeFromRandomAccess(mandersA).createVariable());

		assertEquals(1.0d, r.m1, 0.0001);
		assertEquals(1.0d, r.m2, 0.0001);

		// test A-B combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.randomAccess(),
				mandersB.randomAccess(),
				Views.iterable(mandersAlwaysTrueMask).localizingCursor());

		r = mc.calculateMandersCorrelation(cursor,
				Util.getTypeFromRandomAccess(mandersA).createVariable());

		assertEquals(0.75d, r.m1, 0.0001);
		assertEquals(0.75d, r.m2, 0.0001);

		// test A-C combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.randomAccess(),
				mandersC.randomAccess(),
				Views.iterable(mandersAlwaysTrueMask).localizingCursor());
		
		r = mc.calculateMandersCorrelation(cursor,
				Util.getTypeFromRandomAccess(mandersA).createVariable());
		
		assertEquals(0.5d, r.m1, 0.0001);
		assertEquals(0.5d, r.m2, 0.0001);

		// test A-D combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.randomAccess(),
				mandersD.randomAccess(),
				Views.iterable(mandersAlwaysTrueMask).localizingCursor());

		r = mc.calculateMandersCorrelation(cursor,
				Util.getTypeFromRandomAccess(mandersA).createVariable());

		assertEquals(0.25d, r.m1, 0.0001);
		assertEquals(0.25d, r.m2, 0.0001);

		// test A-E combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.randomAccess(),
				mandersE.randomAccess(),
				Views.iterable(mandersAlwaysTrueMask).localizingCursor());

		r = mc.calculateMandersCorrelation(cursor,
				Util.getTypeFromRandomAccess(mandersA).createVariable());

		assertEquals(0.0d, r.m1, 0.0001);
		assertEquals(0.0d, r.m2, 0.0001);

		// test A-F combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.randomAccess(),
				mandersF.randomAccess(),
				Views.iterable(mandersAlwaysTrueMask).localizingCursor());

		r = mc.calculateMandersCorrelation(cursor,
				Util.getTypeFromRandomAccess(mandersA).createVariable());

		assertEquals(0.25d, r.m1, 0.0001);
		assertEquals(0.3333d, r.m2, 0.0001);

		// test A-G combination.firstElement(
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.randomAccess(),
				mandersG.randomAccess(),
				Views.iterable(mandersAlwaysTrueMask).localizingCursor());

		r = mc.calculateMandersCorrelation(cursor,
				Util.getTypeFromRandomAccess(mandersA).createVariable());

		assertEquals(0.25d, r.m1, 0.0001);
		assertEquals(0.50d, r.m2, 0.0001);

		// test A-H combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.randomAccess(),
				mandersH.randomAccess(),
				Views.iterable(mandersAlwaysTrueMask).localizingCursor());

		r = mc.calculateMandersCorrelation(cursor,
				Util.getTypeFromRandomAccess(mandersA).createVariable());

		assertEquals(0.25d, r.m1, 0.0001);
		assertEquals(1.00d, r.m2, 0.0001);

		// test A-I combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.randomAccess(),
				mandersI.randomAccess(),
				Views.iterable(mandersAlwaysTrueMask).localizingCursor());

		r = mc.calculateMandersCorrelation(cursor,
				Util.getTypeFromRandomAccess(mandersA).createVariable());

		assertEquals(0.083d, r.m1, 0.001);
		assertEquals(0.75d, r.m2, 0.0001);
	}
}