package selection;

import java.awt.List;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class ListListener implements ItemListener
{
	final List linkedList, thisList;
	final Select_Points parent;
	
	public ListListener( final Select_Points parent, final List thisList, final List linkedList )
	{
		this.thisList = thisList;
		this.linkedList = linkedList;
		this.parent = parent;
	}
	
	@Override
	public void itemStateChanged( ItemEvent e ) 
	{
		if ( e.getStateChange() == ItemEvent.DESELECTED )
		{
			parent.activeIndex = -1; 
			linkedList.deselect( linkedList.getSelectedIndex() );
		}
		else
		{
			parent.activeIndex = thisList.getSelectedIndex(); 
			linkedList.select( thisList.getSelectedIndex() );		
		}
		
		parent.drawCurrentSelection();
	}

}
