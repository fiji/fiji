package selection;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DoneButtonListener implements ActionListener
{
	final Frame parent;
	
	public DoneButtonListener( Frame parent )
	{
		this.parent = parent;
	}
	
	@Override
	public void actionPerformed( final ActionEvent arg0 ) 
	{ 
		if ( parent != null )
			parent.dispose();
	}
}
