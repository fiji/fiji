package plugin;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.plugin.PlugIn;

import java.awt.Choice;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class Stitching_Grid implements PlugIn
{
	String[] choose1 = new String[]{ "Row-by-row", "Column-by-column", "Snake by rows", "Snake by columns", "Fixed position" };
	String[][] choose2 = new String[ choose1.length ][];
	
	final ImageIcon[][] images = new ImageIcon[ choose1.length ][];

	public static int defaultChoice1 = 0;
	public static int defaultChoice2 = 0;
	
	@Override
	public void run( String arg0 ) 
	{
		//run("Grid stitching (beta)", "grid_type=[Snake by columns] grid_order=[Up & Right]");
		//run("Grid stitching (beta)", "grid_type=[Fixed position] grid_order=[Defined by filename]");
		
		images[ 0 ] = new ImageIcon[ 4 ];
		choose2[ 0 ] = new String[]{ "Right & Down", "Left & Down", "Right & Up", "Left & Up" };
		images[ 0 ][ 0 ] = GenericDialogPlus.createImageIcon( getClass().getResource( "/images/row1.png" ) );
		images[ 0 ][ 1 ] = GenericDialogPlus.createImageIcon( getClass().getResource( "/images/row2.png" ) );
		images[ 0 ][ 2 ] = GenericDialogPlus.createImageIcon( getClass().getResource( "/images/row3.png" ) );
		images[ 0 ][ 3 ] = GenericDialogPlus.createImageIcon( getClass().getResource( "/images/row4.png" ) );

		images[ 1 ] = new ImageIcon[ 4 ];
		choose2[ 1 ] = new String[]{ "Down & Right", "Down & Left", "Up & Right", "Up & Left" };
		images[ 1 ][ 0 ] = GenericDialogPlus.createImageIcon( getClass().getResource( "/images/column1.png" ) );
		images[ 1 ][ 1 ] = GenericDialogPlus.createImageIcon( getClass().getResource( "/images/column2.png" ) );
		images[ 1 ][ 2 ] = GenericDialogPlus.createImageIcon( getClass().getResource( "/images/column3.png" ) );
		images[ 1 ][ 3 ] = GenericDialogPlus.createImageIcon( getClass().getResource( "/images/column4.png" ) );

		images[ 2 ] = new ImageIcon[ 4 ];
		choose2[ 2 ] = new String[]{ "Right & Down", "Left & Down", "Right & Up", "Left & Up" };
		images[ 2 ][ 0 ] = GenericDialogPlus.createImageIcon( getClass().getResource( "/images/snake1.png" ) );
		images[ 2 ][ 1 ] = GenericDialogPlus.createImageIcon( getClass().getResource( "/images/snake3.png" ) );
		images[ 2 ][ 2 ] = GenericDialogPlus.createImageIcon( getClass().getResource( "/images/snake5.png" ) );
		images[ 2 ][ 3 ] = GenericDialogPlus.createImageIcon( getClass().getResource( "/images/snake7.png" ) );

		images[ 3 ] = new ImageIcon[ 4 ];
		images[ 3 ] = new ImageIcon[ 4 ];
		choose2[ 3 ] = new String[]{ "Down & Right", "Down & Left", "Up & Right", "Up & Left" };
		images[ 3 ][ 0 ] = GenericDialogPlus.createImageIcon( getClass().getResource( "/images/snake2.png" ) );
		images[ 3 ][ 1 ] = GenericDialogPlus.createImageIcon( getClass().getResource( "/images/snake4.png" ) );
		images[ 3 ][ 2 ] = GenericDialogPlus.createImageIcon( getClass().getResource( "/images/snake6.png" ) );
		images[ 3 ][ 3 ] = GenericDialogPlus.createImageIcon( getClass().getResource( "/images/snake8.png" ) );

		images[ 4 ] = new ImageIcon[ 1 ];
		images[ 4 ] = new ImageIcon[ 1 ];
		choose2[ 4 ] = new String[]{ "Defined by filename" };
		images[ 4 ][ 0 ] = GenericDialogPlus.createImageIcon( getClass().getResource( "/images/position.png" ) );

		final GenericDialogPlus gd = new GenericDialogPlus( "test" );
		
		gd.addChoice( "Grid_type", choose1, choose1[ defaultChoice1 ] );
		
		if ( !IJ.isMacro() )
		{
			gd.addChoice( "Grid_order", choose2[ defaultChoice1 ], choose2[ defaultChoice1 ][ defaultChoice2 ] );
			
			final ImageIcon display = new ImageIcon( images[ defaultChoice1 ][ defaultChoice2 ].getImage() );
			final JLabel label = gd.addImage( display );	
			
			// start the listener
			imageSwitch( (Choice) gd.getChoices().get(0), (Choice) gd.getChoices().get(1), images, display, label );
		}
		else
		{
			// the interactive changing is not compatible with the macro language, 
			// thats why we show all possible options and figure out what was meant
			final String[] allChoices = new String[ 9 ];
			allChoices[ 0 ] = choose2[ 0 ][ 0 ];
			allChoices[ 1 ] = choose2[ 0 ][ 1 ];
			allChoices[ 2 ] = choose2[ 0 ][ 2 ];
			allChoices[ 3 ] = choose2[ 0 ][ 3 ];
			allChoices[ 4 ] = choose2[ 1 ][ 0 ];
			allChoices[ 5 ] = choose2[ 1 ][ 1 ];
			allChoices[ 6 ] = choose2[ 1 ][ 2 ];
			allChoices[ 7 ] = choose2[ 1 ][ 3 ];
			allChoices[ 8 ] = choose2[ 4 ][ 0 ];
			gd.addChoice( "Grid_order", allChoices, allChoices[ 0 ] );
		}

		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return;
		
		final int choice1 = defaultChoice1 = gd.getNextChoiceIndex();
		final int choice2;
		
		if ( !IJ.isMacro() )
		{
			choice2 = gd.getNextChoiceIndex();
		}
		else
		{
			final int tmp = gd.getNextChoiceIndex();
			
			// position
			if ( tmp == 8 )
				choice2 = 0;
			else
				choice2 = tmp % 4;				
		}
		
		defaultChoice2 = choice2;
		
		IJ.log( choice1 +  " "  + choice2 );
	}
	
	protected final void imageSwitch( final Choice choice1, final Choice choice2, final ImageIcon[][] images, final ImageIcon display, final JLabel label )
	{
		choice1.addItemListener( new ItemListener()
		{
			public void itemStateChanged(ItemEvent ie)
			{
				final int state1 = choice1.getSelectedIndex();
				final int state2;
				
				if ( state1 == 4 )
					state2 = 0;
				else
					state2 = choice2.getSelectedIndex();
				
				// update the texts in choice2
				choice2.removeAll();
				for ( int i = 0; i < choose2[ state1 ].length; ++i )
					choice2.add( choose2[ state1 ][ i ] );
				
				choice2.select( state2 );
				
				
				display.setImage( images[ state1 ][ state2 ].getImage() );
				label.update( label.getGraphics() );
			}
		});

		choice2.addItemListener( new ItemListener()
		{
			public void itemStateChanged(ItemEvent ie)
			{
				final int state1 = choice1.getSelectedIndex();
				final int state2;
				
				if ( state1 == 4 )
					state2 = 0;
				else
					state2 = choice2.getSelectedIndex();
					
				display.setImage( images[ state1 ][ state2 ].getImage() );
				label.update( label.getGraphics() );
			}
		});
	}
}
 