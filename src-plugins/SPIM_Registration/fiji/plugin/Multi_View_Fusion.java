package fiji.plugin;

import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import ij.gui.GenericDialog;
import ij.gui.MultiLineLabel;
import ij.plugin.PlugIn;

public class Multi_View_Fusion implements PlugIn
{
	final private String myURL = "http://fly.mpi-cbg.de/preibisch";
	final private String paperURL = "http://www.nature.com/nmeth/journal/v7/n6/full/nmeth0610-418.html";

	final String fusionType[] = new String[] { "Single-channel", "Multi-channel" };
	final String timeseriesType[] = new String[] { "Single timepoint", "Timeseries" };
	static int defaultFusionType = 0;
	static int defaultTimeseriesType = 0;

	@Override
	public void run(String arg0) 
	{
		// output to IJ.log
		IOFunctions.printIJLog = true;
		
		final GenericDialog gd = new GenericDialog( "Multi-view fusion" );
		
		gd.addChoice( "Select_channel type", fusionType, fusionType[ defaultFusionType ] );		
		gd.addChoice( "Select_timeseries type", timeseriesType, timeseriesType[ defaultTimeseriesType ] );		
		gd.addMessage( "Please note that the Multi-view fusion is based on a publication.\n" +
						"If you use it successfully for your research please be so kind to cite our work:\n" +
						"Preibisch et al., Nature Methods (2010), 7(6):418-419\n" );

		MultiLineLabel text =  (MultiLineLabel) gd.getMessage();
		Bead_Registration.addHyperLinkListener( text, paperURL );

		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return;
		
		final int channelChoice = gd.getNextChoiceIndex();
		final int timeseriesChoice = gd.getNextChoiceIndex();
		defaultFusionType = channelChoice;
		defaultTimeseriesType = timeseriesChoice;

		final SPIMConfiguration conf;
		final boolean isTimeSeries;
		
		if ( defaultTimeseriesType == 0 )
			isTimeSeries = false;
		else
			isTimeSeries = true;
		
		if ( channelChoice == 0 )
			conf = singleChannel( isTimeSeries );
		else 
			conf = multiChannel( isTimeSeries );
		
		// cancelled
		if ( conf == null )
			return;
	}

	public static String allChannels = "0, 1";
	
	protected SPIMConfiguration multiChannel( final boolean timeseries ) 
	{
		// first ask for the channel configuration
		final GenericDialog gd1 = new GenericDialog( "Channel properties" );
		
		gd1.addStringField( "All_aquired_channels", allChannels );

		
		return null;
	}

	protected SPIMConfiguration singleChannel( final boolean timeseries ) 
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	

}