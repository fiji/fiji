package fiji.plugin;

import ij.ImageJ;
import ij.ImagePlus;

import java.io.File;

import spimopener.SPIMExperiment;

public class Regtest
{
	public static void main( final String[] args )
	{
		final String fn = "/home/tobias/workspace/data/fast fly/111007_weber/e001.xml";

		final SPIMExperiment exp = new SPIMExperiment( fn );
		System.out.println( exp );

		System.out.println("sampleStart, sampleEnd       : " + exp.sampleStart + " " + exp.sampleEnd );
		System.out.println("timepointStart, timepointEnd : " + exp.timepointStart + " " + exp.timepointEnd );
		System.out.println("regionStart, regionEnd       : " + exp.regionStart + " " + exp.regionEnd );
		System.out.println("angleStart, angleEnd         : " + exp.angleStart + " " + exp.angleEnd );
		System.out.println("channelStart, channelEnd     : " + exp.channelStart + " " + exp.channelEnd );
		System.out.println("planeStart, planeEnd         : " + exp.planeStart + " " + exp.planeEnd );
		System.out.println("frameStart, frameEnd         : " + exp.frameStart + " " + exp.frameEnd );
		System.out.println("pathFormatString             : " + exp.pathFormatString );
		System.out.println("pw, ph, pd                   : " + exp.pw + " " + exp.ph + " " + exp.pd );
		System.out.println("w, h, d                      : " + exp.w + " " + exp.h + " " + exp.d );
		System.out.println("experimentName               : " + exp.experimentName );
		for ( final String a : exp.angles )
			System.out.println( "angle " + a );

		final boolean hasAlternatingIllumination = exp.d < (exp.planeEnd + 1 - exp.planeStart);
		System.out.println("hasAlternatingIllumination = " + hasAlternatingIllumination );

		final int tp = exp.timepointStart;
		final int angle = exp.angleStart;
		final int channel = exp.channelStart;
		final int zMin = exp.planeStart;
		final int zMax = exp.planeEnd;
		final int xMin = 0;
		final int xMax = exp.w - 1;
		final int yMin = 0;
		final int yMax = exp.h - 1;
		final ImagePlus imp = exp.openNotProjected( exp.sampleStart, tp, tp, exp.regionStart, angle, channel, zMin, zMax-1, 2, exp.frameStart, exp.frameStart, yMin, yMax, xMin, xMax, SPIMExperiment.X, SPIMExperiment.Y, SPIMExperiment.Z, false );
		final ImagePlus imp2 = exp.openNotProjected( exp.sampleStart, tp, tp, exp.regionStart, angle, channel, zMin+1, zMax, 2, exp.frameStart, exp.frameStart, yMin, yMax, xMin, xMax, SPIMExperiment.X, SPIMExperiment.Y, SPIMExperiment.Z, false );
//		final Image<FloatType> image = ImageJFunctions.convertFloat( imp );

		new ImageJ();
		imp.show();
		imp2.show();
//		ImageJFunctions.show( image );
	}
	public static void main1( final String[] args )
	{
		final String fn = "/home/tobias/workspace/data/fast fly/111007_weber/e001.xml";
		final File f = new File(fn);
		System.out.println( f.getParent() );
	}
}
