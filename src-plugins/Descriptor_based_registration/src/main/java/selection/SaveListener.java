package selection;

import ij.IJ;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.swing.JFileChooser;

public class SaveListener implements ActionListener
{
	final ArrayList< ExtendedPointMatch > matches;
	final Frame frame;
	
	public SaveListener( final Frame frame, final ArrayList< ExtendedPointMatch > matches )
	{
		this.matches = matches;
		this.frame = frame;
	}
	
	@Override
	public void actionPerformed( final ActionEvent arg0 ) 
	{
		if ( matches.size() == 0 )
		{
			IJ.log( "List is empty." );
			return;
		}
		
		String filename = File.separator + "txt";
		JFileChooser fc = new JFileChooser( new File( filename ) );

		// Show save dialog; this method does not return until the dialog is closed
		fc.showSaveDialog( frame );
		final File file = fc.getSelectedFile();
		
		IJ.log( "Saving to '" + file + "' ..." );
		
		final PrintWriter out = openFileWrite( file );
		
		for ( final ExtendedPointMatch pm : matches )
		{
			out.println( pm.getP1().getL()[ 0 ] + "\t" + pm.getP1().getL()[ 1 ] + "\t" +  pm.getP1().getL()[ 2 ] + "\t" + pm.getP1().getW()[ 0 ] + "\t" + pm.getP1().getW()[ 1 ] + "\t" +  pm.getP1().getW()[ 2 ] + "\t" + pm.radius1L + "\t" + pm.radius1W + "\t" +  
					     pm.getP2().getL()[ 0 ] + "\t" + pm.getP2().getL()[ 1 ] + "\t" +  pm.getP2().getL()[ 2 ] + "\t" + pm.getP2().getW()[ 0 ] + "\t" + pm.getP2().getW()[ 1 ] + "\t" +  pm.getP2().getW()[ 2 ] + "\t" + pm.radius2L + "\t" + pm.radius2W );
			
		}
		
		out.close();
	}
	
	// sorry ... but I do not want it to depend on SPIM project for that
	private static PrintWriter openFileWrite(final File file)
	{
		PrintWriter outputFile;
		try
		{
			outputFile = new PrintWriter(new FileWriter(file));
		}
		catch (IOException e)
		{
			System.out.println("TextFileAccess.openFileWrite(): " + e);
			outputFile = null;
		}
		return (outputFile);
	}

}
