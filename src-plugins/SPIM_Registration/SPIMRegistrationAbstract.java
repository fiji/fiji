import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import mpicbg.spim.Reconstruction;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;

import ij.IJ;
import ij.gui.MultiLineLabel;
import ij.plugin.BrowserLauncher;
import ij.plugin.PlugIn;

import fiji.util.gui.GenericDialogPlus;

public abstract class SPIMRegistrationAbstract implements PlugIn
{
	final private String myURL = "http://fly.mpi-cbg.de/~preibisch/contact.html";
	
	@Override
	public void run( final String arg0 )
	{
		// we want to print everything into the IJ log window
		IOFunctions.printIJLog = true;
		
		final GenericDialogPlus gd = createGenericDialogPlus();
		
		gd.addMessage( "" );
		gd.addMessage( "This Plugin is developed by Stephan Preibisch\n" + myURL );

		MultiLineLabel text = (MultiLineLabel) gd.getMessage();
		addHyperLinkListener( text, myURL );

		gd.showDialog();
		
		if ( gd.wasCanceled() )			
			return;
		
		getParameters( gd );
		
		final Reconstruction spimReconstruction = execute();
		final SPIMConfiguration conf = spimReconstruction.getSPIMConfiguration();
		
		if ( !conf.registerOnly && !conf.timeLapseRegistration && conf.scale > 1 )
		{
			
		}
	}
	
	protected abstract Reconstruction execute();	
	protected abstract void getParameters( final GenericDialogPlus gd );	
	protected abstract GenericDialogPlus createGenericDialogPlus();

	protected static final void addHyperLinkListener(final MultiLineLabel text, final String myURL)
	{
		text.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				try
				{
					BrowserLauncher.openURL(myURL);
				}
				catch (Exception ex)
				{
					IJ.error("" + ex);
				}
			}

			public void mouseEntered(MouseEvent e)
			{
				text.setForeground(Color.BLUE);
				text.setCursor(new Cursor(Cursor.HAND_CURSOR));
			}

			public void mouseExited(MouseEvent e)
			{
				text.setForeground(Color.BLACK);
				text.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		});
	}
	
}
