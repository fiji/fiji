package util;

import java.io.File;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.*;

import java.util.Random;

public class TestFindConnectedRegions {

	public static ImagePlus generateCubedStack( int [] valueInEachSubCube ) {
		return generateCubedStack( valueInEachSubCube,
					   valueInEachSubCube );
	}

	/* This method generates a cube shaped image stack made up of
	   27 sub-cubes - the values within each sub-cube are
	   uniformly randomly distributed in a range between
	   minimumsInEachSubCube[i] and maximumsInEachSubCube[i] */

	public static ImagePlus generateCubedStack( int [] minimumsInEachSubCube,
						    int [] maximumsInEachSubCube ) {

		int smallCubeSide = 5;
		int smallCubesAlongSide = 3;
		int largeCubeSide = smallCubeSide * smallCubesAlongSide;

		int numberOfSmallCubes = smallCubesAlongSide * smallCubesAlongSide * smallCubesAlongSide;

		if( minimumsInEachSubCube.length != numberOfSmallCubes )
			throw new IllegalArgumentException(
				"minimumsInEachSubCube must be "+smallCubesAlongSide+
				"\u00b3 = "+numberOfSmallCubes+", in fact was: "+minimumsInEachSubCube.length );

		if( maximumsInEachSubCube.length != numberOfSmallCubes )
			throw new IllegalArgumentException(
				"maximumsInEachSubCube must be "+smallCubesAlongSide+
				"\u00b3 = "+numberOfSmallCubes+", in fact was: "+maximumsInEachSubCube.length );

		ImageStack stack = new ImageStack(largeCubeSide,largeCubeSide);

		Random rng = new Random();

		for( int z = 0; z < largeCubeSide; ++z ) {
			int subCubeZ = z / smallCubeSide;

			byte [] pixels = new byte[ largeCubeSide * largeCubeSide ];

			for( int y = 0; y < largeCubeSide; ++y ) {
				int subCubeY = y / smallCubeSide;

				for( int x = 0; x < largeCubeSide; ++x ) {
					int subCubeX = x / smallCubeSide;

					int subCubeIndex = subCubeX + smallCubesAlongSide * (subCubeY + smallCubesAlongSide * subCubeZ);

					int min = minimumsInEachSubCube[subCubeIndex];
					int max = maximumsInEachSubCube[subCubeIndex];

					int value = min;
					if( min > max )
						throw new RuntimeException("At index "+subCubeIndex+", the minimum ("+min+") was greater than the maximum ("+max+")");
					else if( max > min ) {
						int range = (max - min) + 1;
						value = min + rng.nextInt(range);
					}

					pixels[x+y*largeCubeSide] = (byte)value;
				}
			}
			ByteProcessor bp = new ByteProcessor(largeCubeSide,largeCubeSide);
			bp.setPixels(pixels);
			stack.addSlice("",bp);
		}
		ImagePlus result = new ImagePlus("SubCubes Test",stack);
		return result;
	}

	public static boolean stacksHaveSameValues(ImageStack aStack, ImageStack bStack) {
		int aWidth = aStack.getWidth();
		int bWidth = bStack.getWidth();
		int aHeight = aStack.getHeight();
		int bHeight = bStack.getHeight();
		int aDepth = aStack.getSize();
		int bDepth = bStack.getSize();
		if( aWidth != bWidth )
			throw new RuntimeException("The widths of aStack ("+aWidth+") "+
						   "and bStack ("+bWidth+") didn't match");
		if( aHeight != bHeight )
			throw new RuntimeException("The heights of aStack ("+aHeight+") "+
						   "and bStack ("+bHeight+") didn't match");
		if( aDepth != bDepth )
			throw new RuntimeException("The depths of aStack ("+aDepth+") "+
						   "and bStack ("+bDepth+") didn't match");
		for( int z = 0; z < aDepth; ++z ) {
			ImageProcessor processorA = aStack.getProcessor(z+1);
			ImageProcessor processorB = bStack.getProcessor(z+1);
			for( int y = 0; y < aHeight; ++y ) {
				for( int x = 0; x < aWidth; ++x ) {
					int valueA = processorA.get(x,y);
					int valueB = processorB.get(x,y);
					if( valueA != valueB ) {
						System.out.println("Value mismatch ("+valueA+" vs "+valueB+") was detected at ("+x+","+y+","+z+"): returning false");
						return false;
					}
				}
			}
		}
		return true;
	}

	public ImagePlus smallRandom3DCheckerBoard;

	@Before
	public void setup() {
		int [] minimums = { 64, 196, 64, 196, 64, 196, 64, 196, 64,
				    196, 64, 196, 64, 196, 64, 196, 64, 196,
				    64, 196, 64, 196, 64, 196, 64, 196, 64 };
		int [] maximums = {128, 255, 128, 255, 128, 255, 128, 255, 128,
				     255, 128, 255, 128, 255, 128, 255, 128, 255,
				     128, 255, 128, 255, 128, 255, 128, 255, 128 };
		smallRandom3DCheckerBoard = generateCubedStack(
			minimums,
			maximums );
	}

	@Test
	public void test26ConnectedThresholdSearches() {

		FindConnectedRegions fcr = new FindConnectedRegions();
		FindConnectedRegions.Results results;

		results = fcr.run( smallRandom3DCheckerBoard,
				   true,  // diagonal
				   false, // imagePerRegion,
				   true,  // imageAllRegions,
				   false, // showResults
				   false, // mustHaveSameValue,
				   false, // startFromPointROI,
				   false, // autoSubtract
				   130,   // valuesOverDouble,
				   1,     // minimumPointsInRegionDouble,
				   -1, 	  // stopAfterNumberOfRegions,
				   true   // noUI
			);

		int [] values = { 0, 1, 0, 1, 0, 1, 0, 1, 0,
				  1, 0, 1, 0, 1, 0, 1, 0, 1,
				  0, 1, 0, 1, 0, 1, 0, 1, 0 };

		ImagePlus expectedResult = generateCubedStack(values);

		assertTrue(stacksHaveSameValues(
				   results.allRegions.getStack(),
				   expectedResult.getStack() ));

	}

	@Test
	public void test6ConnectedThresholdSearches() {

		FindConnectedRegions fcr = new FindConnectedRegions();
		FindConnectedRegions.Results results;

		results = fcr.run( smallRandom3DCheckerBoard,
				   false, // diagonal
				   false, // imagePerRegion,
				   true,  // imageAllRegions,
				   false, // showResults
				   false, // mustHaveSameValue,
				   false, // startFromPointROI,
				   false, // autoSubtract
				   130,   // valuesOverDouble,
				   1,     // minimumPointsInRegionDouble,
				   -1, 	  // stopAfterNumberOfRegions,
				   true   // noUI
			);

		int [] values = { 0,  1,  0,  2,  0,  3,  0,  4,  0,
				  5,  0,  6,  0,  7,  0,  8,  0,  9,
				  0,  10, 0,  11, 0,  12, 0,  13, 0 };

		ImagePlus expectedResult = generateCubedStack(values);

		assertTrue(stacksHaveSameValues(
				   results.allRegions.getStack(),
				   expectedResult.getStack() ));

	}


}
