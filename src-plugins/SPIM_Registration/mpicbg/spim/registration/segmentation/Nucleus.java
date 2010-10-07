package mpicbg.spim.registration.segmentation;

import java.util.ArrayList;
import java.util.Collection;

import fiji.util.node.Leaf;
import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.models.Point;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;

public class Nucleus extends Point implements Leaf<Nucleus>
{
	private static final long serialVersionUID = 1L;

	final protected int id;
	final protected ViewDataBeads myView;
	
	protected double weight = 1;
	protected float distance = -1;
	float diameter = 1;

	final private ArrayList<NucleusIdentification> descriptorCorrespondence = new ArrayList<NucleusIdentification>();
	final private ArrayList<NucleusIdentification> ransacCorrespondence = new ArrayList<NucleusIdentification>();
	final private ArrayList<NucleusIdentification> icpCorrespondence = new ArrayList<NucleusIdentification>();

	public Nucleus( final int id, final float[] location, final ViewDataBeads myView ) 
	{
		super( location );
		this.id = id;
		this.myView = myView;
	}

	public ViewDataBeads getView() { return myView; }
	public int getViewID() { return myView.getID(); }
	public int getID() { return id; }	
	public void setWeight( final double weight ){ this.weight = weight; }
	public double getWeight(){ return weight; }
	public void setDiameter( final float diameter ) { this.diameter = diameter; }
	public float getDiameter() { return diameter; }

	public ArrayList<NucleusIdentification> getDescriptorCorrespondence() { return descriptorCorrespondence; }
	public ArrayList<NucleusIdentification> getRANSACCorrespondence() { return ransacCorrespondence; }
	public ArrayList<NucleusIdentification> getICPCorrespondence() { return icpCorrespondence; }

	final public void setW( final float[] wn )
	{
		for ( int i = 0; i < Math.min( w.length, wn.length ); ++i )
			w[i] = wn[i];
	}
	
	final public void resetW()
	{
		for ( int i = 0; i < w.length; ++i )
			w[i] = l[i];
	}

	protected boolean useW = true;
	
	public void setUseW( final boolean useW ) { this.useW = useW; } 
	public boolean getUseW() { return useW; } 
	
	public void setDistance( float distance ) { this.distance = distance;	}
	public float getDistance() { return this.distance;	}

	public synchronized void addICPCorrespondence( final Nucleus nucleus ) { addICPCorrespondence( nucleus, 1 ); }
	public synchronized void addICPCorrespondence( final Nucleus nucleus, final double weight )
	{		
		icpCorrespondence.add( new NucleusIdentification( nucleus ) );
	}

	public synchronized void addPointDescriptorCorrespondence( final Nucleus nucleus ) { addPointDescriptorCorrespondence( nucleus, 1 ); }
	public synchronized void addPointDescriptorCorrespondence( final Nucleus nucleus, final double weight )
	{		
		descriptorCorrespondence.add( new NucleusIdentification( nucleus ) );
	}

	public synchronized void addRANSACCorrespondence( final Nucleus nucleus ) { addRANSACCorrespondence( nucleus, 1 ); }
	public synchronized void addRANSACCorrespondence( final Nucleus nucleus, final double weight )
	{
		if ( !containsNucleusID( descriptorCorrespondence, nucleus ) )
		{
			IOFunctions.println( "Error: Cannot set RANSAC correspondence for nucleus " + this.getID() + "(" +getViewID() + ")<->" + nucleus.getID() + "(" +nucleus.getViewID() + "); it has no correspondence from a point descriptor." );
			return;
		}

		int sameViewIndex = -1;
		
		for ( int i = 0; i < ransacCorrespondence.size(); i++ )
			if ( ransacCorrespondence.get( i ).getViewID() == nucleus.getViewID() )
				sameViewIndex = i;
		
		if ( sameViewIndex >= 0 )
		{
			IOFunctions.println( "Warning: RANSAC Correspondence for nucleus " + this.getID() + "(" +getViewID() + "), currently " + ransacCorrespondence.get( sameViewIndex ) + " overwritten by " + nucleus );
			ransacCorrespondence.remove( sameViewIndex );
		}

		ransacCorrespondence.add( new NucleusIdentification( nucleus ) );
	}

	public static boolean containsNucleusID( Collection<NucleusIdentification> list, final Nucleus nucleus )
	{
		boolean contains = false;
		
		for ( final NucleusIdentification content : list )
			if ( content.getNucleusID() == nucleus.getID() && content.getViewID() == nucleus.getViewID() )
				contains = true;
		
		return contains;
	}

	@Override
	public float get( final int k ) 
	{
		if ( useW )
			return w[ k ];
		else
			return l[ k ];
	}	
	
	public void set( final float v, final int k ) 
	{
		if ( useW )
			w[ k ] = v;
		else
			l[ k ] = v;
	}	

	@Override
	public String toString()
	{
		String desc = "Nucleus " + getID() + " l"+ MathLib.printCoordinates( getL() ) + "; w"+ MathLib.printCoordinates( getW() );
		
		if ( myView != null)
			return desc + " of view " + myView;
		else
			return desc + " - no view assigned";
	}

	public boolean isLeaf() { return true; }

	@Override
	public float distanceTo( final Nucleus o )
	{
		final float x = o.get( 0 ) - get( 0 );
		final float y = o.get( 1 ) - get( 1 );
		final float z = o.get( 2 ) - get( 2 );
		
		return (float)Math.sqrt(x*x + y*y + z*z);
	}
	
	@Override
	public Nucleus[] createArray( final int n ){ return new Nucleus[ n ];	}

	@Override
	public int getNumDimensions(){ return 3; }
	
	public boolean equals( final Nucleus o )
	{
		if ( useW )
		{
			for ( int d = 0; d < 3; ++d )
				if ( w[ d ] != o.w[ d ] )
					return false;			
		}
		else
		{
			for ( int d = 0; d < 3; ++d )
				if ( l[ d ] != o.l[ d ] )
					return false;						
		}
				
		return true;
	}
	
	public static boolean equals( final Nucleus nucleus1, final Nucleus nucleus2 )
	{
		if ( nucleus1.getID() == nucleus2.getID() && nucleus1.getViewID() == nucleus2.getViewID() )
			return true;
		else
			return false;
	}
	
	public boolean isTrueCorrespondence = false;
	public boolean isFalseCorrespondence = false;
	public boolean isAmbigous = false;
	public boolean isUnique = false;
	public int numCorr = 0;
}
