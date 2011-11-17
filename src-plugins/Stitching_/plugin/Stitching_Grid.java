package plugin;

import fiji.util.gui.GenericDialogPlus;
import ij.plugin.PlugIn;

import java.awt.Choice;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class Stitching_Grid implements PlugIn
{
	String[] choose = new String[]{ "Book-like arrangement", "fiji" };
	ImageIcon[] images = new ImageIcon[ choose.length ];

	public static int defaultChoice = 0;
	
	@Override
	public void run(String arg0) 
	{
		images[ 0 ] = GenericDialogPlus.createImageIcon( getClass().getResource( "/images/snake1.png" ) );
		images[ 1 ] = GenericDialogPlus.createImageIcon( getClass().getResource( "/images/snake2.png" ) );					
		
        final GenericDialogPlus gd = new GenericDialogPlus( "test" );
		
		gd.addChoice( "Choose type of grid", choose, choose[ defaultChoice ] );
		
		final ImageIcon display = new ImageIcon( images[ defaultChoice ].getImage() );
		final JLabel label = gd.addImage( display );	
		
		// start the listener
		imageSwitch( (Choice) gd.getChoices().get(0), images, display, label );

		gd.showDialog();		
	}
	
	protected final void imageSwitch( final Choice choice, final ImageIcon[] images, final ImageIcon display, final JLabel label )
	{
		choice.addItemListener( new ItemListener()
		{
			public void itemStateChanged(ItemEvent ie)
			{
				final int state = choice.getSelectedIndex();
				
				display.setImage( images[ state ].getImage() );
				label.update( label.getGraphics() );
			}
		});
	}

}
