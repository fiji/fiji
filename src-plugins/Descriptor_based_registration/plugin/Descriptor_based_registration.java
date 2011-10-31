package plugin;

import ij.gui.GenericDialog;
import ij.gui.MultiLineLabel;
import ij.plugin.PlugIn;
import mpicbg.spim.io.IOFunctions;

public class Descriptor_based_registration implements PlugIn 
{
	final private String myURL = "http://fly.mpi-cbg.de/preibisch";
	final private String paperURL = "http://www.nature.com/nmeth/journal/v7/n6/full/nmeth0610-418.html";

	@Override
	public void run(String arg0) 
	{
		// output to IJ.log
		IOFunctions.printIJLog = true;
		
		final GenericDialog gd = new GenericDialog( "Bead based registration" );
		
		//gd.addChoice( "Select type of registration", beadRegistration, beadRegistration[ defaultBeadRegistration ] );		
		gd.addMessage( "Please note that the SPIM Registration is based on a publication.\n" +
						"If you use it successfully for your research please be so kind to cite our work:\n" +
						"Preibisch et al., Nature Methods (2010), 7(6):418-419\n" );

		MultiLineLabel text =  (MultiLineLabel) gd.getMessage();
		//addHyperLinkListener( text, paperURL );

		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return;
	}
}
