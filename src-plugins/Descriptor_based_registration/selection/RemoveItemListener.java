package selection;

import ij.IJ;

import java.awt.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class RemoveItemListener implements ActionListener
{
	final List list1, list2;
	final ArrayList< ExtendedPointMatch > matches;
	final Select_Points parent;
	
	public RemoveItemListener( final Select_Points parent, final List list1, final List list2, final ArrayList< ExtendedPointMatch > matches )
	{
		this.parent = parent;
		this.list1 = list1;
		this.list2 = list2;
		this.matches = matches;
	}
	
	@Override
	public void actionPerformed( final ActionEvent arg0 ) 
	{ 
		final int index1 = list1.getSelectedIndex();
		final int index2 = list2.getSelectedIndex();
		
		if ( index1 == -1 )
		{
			IJ.log( "Please select entry to remove in left list." );
		}
		else if ( index2 == -1 )
		{
			IJ.log( "Please select entry to remove in right list." );			
		}
		else if ( index1 != index2 )
		{
			IJ.log( "Please select corresponding entries to remove" );
		}
		else
		{
			list1.remove( index1 );
			list2.remove( index1 );
			matches.remove( index1 );
			
			parent.activeIndex = -1;
			parent.drawCurrentSelection();
		}
	}
}