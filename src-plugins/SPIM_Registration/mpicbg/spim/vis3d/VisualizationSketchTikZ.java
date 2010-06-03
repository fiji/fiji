package mpicbg.spim.vis3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3f;

import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.bead.Bead;

public class VisualizationSketchTikZ
{
	public static String drawBeads( final Collection<Bead> beads, final Transform3D globalTransform, final String beadType, final float factor )
	{
		return drawBeads( beads, globalTransform, beadType, factor, 1 );
	}
	
	public static String drawBeads( final Collection<Bead> beads, final Transform3D globalTransform, final String beadType, final float factor, final int skipBeads )
	{				
		// we will insert lines like this
		// put { translate([0,0,1]) } {Bead}
		
		final String template1 = "\t\tput { translate([";
		final String template2 = "]) } {";
		final String template3 = "}\n";
		String insert = "";

		// the transformed bead position
		final Point3f translation = new Point3f();
		
		int beadCount = 0;
		
		// add all beads
		for ( Iterator<Bead> i = beads.iterator(); i.hasNext(); )
		{
			final Bead bead = i.next();
			
			// if it is a RANSAC bead draw it anyways, otherwise draw only every nth bead
			if ( bead.getRANSACCorrespondence().size() > 0 || beadCount % skipBeads == 0)
			{
				// set the bead coordinates
				translation.set( bead.getL() );
				
				// transform the bead coordinates into the position of the view
				globalTransform.transform( translation );
				
				insert += template1 + (translation.x*factor) + "," + (translation.y*factor) +"," + (translation.z*factor) + template2 + beadType + template3;
			}
			++beadCount;
		}				

		return insert;
	}

	public static String drawBead( final Bead bead, final Transform3D globalTransform, final String beadType, final float factor )
	{				
		// we will insert lines like this
		// put { translate([0,0,1]) } {Bead}
		
		final String template1 = "\t\tput { translate([";
		final String template2 = "]) } {";
		final String template3 = "}\n";
		String insert = "";

		// the transformed bead position
		final Point3f translation = new Point3f();
		
		// add beads	
		
		// set the bead coordinates
		translation.set( bead.getL() );
		
		// transform the bead coordinates into the position of the view
		globalTransform.transform( translation );
		
		insert += template1 + (translation.x*factor) + "," + (translation.y*factor) +"," + (translation.z*factor) + template2 + beadType + template3;

		return insert;
	}
	
	public static String drawView( final ViewDataBeads view, final float factor )
	{
		final ArrayList<Point3f> box = VisualizationFunctions.getTransformedBoundingBox( view );
		
		// we will insert lines like this
		// line[lineStyle](0,0,0)(10,0,10)
		
		final String template1 = "\t\tline[lineStyle](";
		final String template2 = ")(";
		final String template3 = ")\n";
		String insert = "";

		for ( int i = 0; i < box.size()/2; i++ )
		{
			Point3f from = box.get( i*2 );
			Point3f to = box.get( i*2 + 1 );
			
			insert += template1 + (from.x*factor) + "," + (from.y*factor) +"," + (from.z*factor) + template2 + (to.x*factor) + "," + (to.y*factor) +"," + (to.z*factor) + template3;
		}
		
		return insert;
	}
	
	public static String drawViews( final ViewDataBeads[] views, final float factor )
	{
		String insert = "";
		
		for (int i = 0; i < views.length; i++)
			insert += drawView( views[i], i );	
		
		return insert;
	}
	
}
