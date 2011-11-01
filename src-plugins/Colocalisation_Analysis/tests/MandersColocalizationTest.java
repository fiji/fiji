package tests;

import static org.junit.Assert.assertEquals;
import mpicbg.imglib.cursor.special.TwinCursor;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;

import org.junit.Test;

import algorithms.MandersColocalization;
import algorithms.MissingPreconditionException;
import algorithms.MandersColocalization.MandersResults;

public class MandersColocalizationTest extends ColocalisationTest {

	@Test
	public void mandersPaperImagesTest() throws MissingPreconditionException {
		MandersColocalization<UnsignedByteType> mc =
				new MandersColocalization<UnsignedByteType>();

		TwinCursor<UnsignedByteType> cursor;
		MandersResults r;
		// test A-A combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.createLocalizableByDimCursor(),
				mandersA.createLocalizableByDimCursor(),
				mandersAlwaysTrueMask.createLocalizableCursor());

		r = mc.calculateMandersCorrelation(cursor,
				mandersA.createType());
		cursor.close();

		assertEquals(1.0d, r.m1, 0.0001);
		assertEquals(1.0d, r.m2, 0.0001);

		// test A-B combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.createLocalizableByDimCursor(),
				mandersB.createLocalizableByDimCursor(),
				mandersAlwaysTrueMask.createLocalizableCursor());

		r = mc.calculateMandersCorrelation(cursor,
				mandersA.createType());
		cursor.close();

		assertEquals(0.75d, r.m1, 0.0001);
		assertEquals(0.75d, r.m2, 0.0001);

		// test A-C combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.createLocalizableByDimCursor(),
				mandersC.createLocalizableByDimCursor(),
				mandersAlwaysTrueMask.createLocalizableCursor());
		
		r = mc.calculateMandersCorrelation(cursor,
				mandersA.createType());
		cursor.close();
		
		assertEquals(0.5d, r.m1, 0.0001);
		assertEquals(0.5d, r.m2, 0.0001);

		// test A-D combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.createLocalizableByDimCursor(),
				mandersD.createLocalizableByDimCursor(),
				mandersAlwaysTrueMask.createLocalizableCursor());

		r = mc.calculateMandersCorrelation(cursor,
				mandersA.createType());
		cursor.close();

		assertEquals(0.25d, r.m1, 0.0001);
		assertEquals(0.25d, r.m2, 0.0001);

		// test A-E combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.createLocalizableByDimCursor(),
				mandersE.createLocalizableByDimCursor(),
				mandersAlwaysTrueMask.createLocalizableCursor());

		r = mc.calculateMandersCorrelation(cursor,
				mandersA.createType());
		cursor.close();

		assertEquals(0.0d, r.m1, 0.0001);
		assertEquals(0.0d, r.m2, 0.0001);

		// test A-F combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.createLocalizableByDimCursor(),
				mandersF.createLocalizableByDimCursor(),
				mandersAlwaysTrueMask.createLocalizableCursor());

		r = mc.calculateMandersCorrelation(cursor,
				mandersA.createType());
		cursor.close();

		assertEquals(0.25d, r.m1, 0.0001);
		assertEquals(0.3333d, r.m2, 0.0001);

		// test A-G combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.createLocalizableByDimCursor(),
				mandersG.createLocalizableByDimCursor(),
				mandersAlwaysTrueMask.createLocalizableCursor());

		r = mc.calculateMandersCorrelation(cursor,
				mandersA.createType());
		cursor.close();

		assertEquals(0.25d, r.m1, 0.0001);
		assertEquals(0.50d, r.m2, 0.0001);

		// test A-H combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.createLocalizableByDimCursor(),
				mandersH.createLocalizableByDimCursor(),
				mandersAlwaysTrueMask.createLocalizableCursor());

		r = mc.calculateMandersCorrelation(cursor,
				mandersA.createType());
		cursor.close();

		assertEquals(0.25d, r.m1, 0.0001);
		assertEquals(1.00d, r.m2, 0.0001);

		// test A-I combination
		cursor = new TwinCursor<UnsignedByteType>(
				mandersA.createLocalizableByDimCursor(),
				mandersI.createLocalizableByDimCursor(),
				mandersAlwaysTrueMask.createLocalizableCursor());

		r = mc.calculateMandersCorrelation(cursor,
				mandersA.createType());
		cursor.close();

		assertEquals(0.083d, r.m1, 0.001);
		assertEquals(0.75d, r.m2, 0.0001);
	}
}