package mpicbg.spim.registration.bead.descriptor;

public class KDTreeFunctions 
{
	/*public static KDTree<Bead> insertIntoKDTree( final List<Bead> beads, final String name )
	{
		if ( beads == null || beads.size() == 0)
			return null;
		
		KDTree<Bead> tree = null;
		
		for (Iterator<Bead> i = beads.iterator(); i.hasNext();)
		{
			// get the bead
			Bead bead = i.next();

			if ( tree == null )
				tree = new KDTree<Bead>( bead, bead.getL()[0], bead.getL()[1], bead.getL()[2], 4, name );
			else
				tree.insert( bead, bead.getL()[0], bead.getL()[1], bead.getL()[2] );
		}

		return tree;
	}*/

}
