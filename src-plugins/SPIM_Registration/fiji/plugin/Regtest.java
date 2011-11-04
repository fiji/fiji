package fiji.plugin;

import ij.ImageJ;
import ij.ImagePlus;

import java.io.File;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.real.FloatType;

import huisken.opener.SPIMExperiment;

public class Regtest
{
	public static void main( String[] args )
	{
		String fn = "/home/tobias/workspace/data/fast fly/111007_weber/e001.xml";
		
		SPIMExperiment exp = new SPIMExperiment( fn );
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
		for ( String a : exp.angles )
			System.out.println( "angle " + a );
		
		int tp = exp.timepointStart;
		int angle = exp.angleStart;
		int channel = exp.channelStart;
		int zMin = exp.planeStart;
		int zMax = exp.planeEnd;
		int xMin = 0;
		int xMax = exp.w - 1;
		int yMin = 0;
		int yMax = exp.h - 1;
		ImagePlus imp = exp.openNotProjected( exp.sampleStart, tp, tp, exp.regionStart, angle, channel, zMin, zMax, exp.frameStart, exp.frameStart, yMin, yMax, xMin, xMax, SPIMExperiment.X, SPIMExperiment.Y, SPIMExperiment.Z, false );
		Image<FloatType> image = ImageJFunctions.convertFloat( imp );
				
		new ImageJ();
		imp.show();
		ImageJFunctions.show( image );
	}
	public static void main1( String[] args )
	{
		String fn = "/home/tobias/workspace/data/fast fly/111007_weber/e001.xml";
		File f = new File(fn);
		System.out.println( f.getParent() );
	}
}
