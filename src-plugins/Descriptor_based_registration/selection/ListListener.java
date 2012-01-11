package selection;

import java.awt.List;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class ListListener implements ItemListener
{
	final List linkedList, thisList;
	
	public ListListener( final List thisList, final List linkedList )
	{
		this.thisList = thisList;
		this.linkedList = linkedList;
	}
	
	@Override
	public void itemStateChanged( ItemEvent e ) 
	{
		linkedList.select( thisList.getSelectedIndex() );
	}

}
