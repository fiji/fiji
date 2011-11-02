package fiji.plugin;

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
		
		
	}
}
