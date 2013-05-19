package selection;

import ij.gui.GenericDialog;
import ij.gui.Overlay;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DoneButtonListener implements ActionListener
{
	final Frame frame;
	final Select_Points parent;
	
	public DoneButtonListener( final Select_Points parent, final Frame frame )
	{
		this.frame = frame;
		this.parent = parent;
	}
	
	@Override
	public void actionPerformed( final ActionEvent arg0 ) 
	{ 
		if ( parent.matches.size() > 0 )
		{
			final GenericDialog gd = new GenericDialog( "Query" );
			gd.addMessage( "The list that you created is not empty. Do you really want to quit?" );
			
			gd.showDialog();
			
			if ( gd.wasCanceled() )
				return;
		}
		
		if ( frame != null )
			frame.dispose();
		
		parent.imp1.setOverlay( new Overlay() );
		parent.imp2.setOverlay( new Overlay() );
	}
}
