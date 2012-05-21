package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import net.imglib2.TwinCursor;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

import org.junit.Test;

import algorithms.MissingPreconditionException;
import algorithms.SpearmanRankCorrelation;

/**
 * This class contains JUnit 4 test cases for the calculation of
 * Spearman's Rank Correlation (rho).
 *
 * @author Leonardo Guizzetti
 */
public class SpearmanRankTest extends ColocalisationTest {

	/**
	 * Checks Spearman's Rank Correlation rho for positive correlated images.
	 */
	@Test
	public void spearmanPositiveCorrTest() throws MissingPreconditionException {
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				positiveCorrelationImageCh1.randomAccess(),
				positiveCorrelationImageCh2.randomAccess(),
				Views.iterable(positiveCorrelationAlwaysTrueMask).localizingCursor());
		// calculate Spearman's Rank rho value
		double rho = SpearmanRankCorrelation.calculateSpearmanRank(cursor);
		// Rho value = 0.5463...
		assertTrue(rho > 0.54 && rho < 0.55);
	}

	/**
	 * Checks Spearman's Rank Correlation value for zero correlated images. The rho value
	 * should be about zero.
	 */
	@Test
	public void spearmanZeroCorrTest() throws MissingPreconditionException {
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				zeroCorrelationImageCh1.randomAccess(),
				zeroCorrelationImageCh2.randomAccess(),
				Views.iterable(zeroCorrelationAlwaysTrueMask).localizingCursor());
		// calculate Spearman's Rank rho value
		double rho = SpearmanRankCorrelation.calculateSpearmanRank(cursor);
		// Rho value = -0.11...
		assertTrue(Math.abs(rho) < 0.012);
	}
	
	/**
	 * Checks Spearman's Rank Correlation value for slightly negative correlated synthetic data.
	 * 
	 */
	@Test
	public void statisticsTest() throws MissingPreconditionException {
		
		double[][] data = new double[][] {
			{1,113}, 
			{2,43},
			{3,11},
			{6,86},
			{5,59},
			{8,47},
			{4,92},
			{0,152},
			{6,23},
			{4,9},
			{7,33},
			{3,69},
			{2,75},
			{9,135},
			{3,30}
		 };
		int n = data.length;
		double[] x = new double[n];
		double[] y = new double[n];
		
		for (int i = 0; i<n; i++) {
			x[i] = data[i][0];
			y[i] = data[i][1];
		}
		
		/*
		 * Check the arithmetic for the rho calculation.
		 * Rho is exactly -0.1743 using the exact calculation for Spearman's rho 
		 * as implemented here.
		 */
		double rho = SpearmanRankCorrelation.calculateRho(x, y);
		assertEquals(-0.1743, rho, 0.001);
		
		// check the degrees of freedom calculation ( df = n - 2 )
		int df = 0;
		df = SpearmanRankCorrelation.getSpearmanDF(n);
		assertEquals(df, n - 2);
		
		// check the t-statistic calculation ( t = rho * sqrt( df / (1-rho^2) ) )
		// The t-stat = -0.6382
		double tstat = 0.0;
		tstat = SpearmanRankCorrelation.getTStatistic(rho, df);
		assertTrue((tstat > -0.64) && (tstat < -0.63));
	}
	
	/**
	 * Checks Spearman's Rank Correlation value for synthetic test image.
	 * This tests the same dataset as the statisticsTest() but tests reading in image
	 * data, the rank transform, and the calling of the statistics calculation methods.
	 */
	@Test
	public void spearmanSyntheticNegCorrTest() throws MissingPreconditionException {
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				syntheticNegativeCorrelationImageCh1.randomAccess(),
				syntheticNegativeCorrelationImageCh2.randomAccess(),
				Views.iterable(syntheticNegativeCorrelationAlwaysTrueMask).localizingCursor());
		
		// calculate Spearman's Rank rho value
		double rho = SpearmanRankCorrelation.calculateSpearmanRank(cursor);
		assertTrue((rho > -0.178) && (rho < -0.173));
	}
	
}
