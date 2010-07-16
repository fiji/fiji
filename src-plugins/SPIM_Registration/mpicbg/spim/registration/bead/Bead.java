package mpicbg.spim.registration.bead;

import java.util.ArrayList;
import java.util.Collection;

import javax.vecmath.Point3d;
import javax.vecmath.Point3f;

import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.models.Point;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;

public class Bead extends Point
{
	private static final long serialVersionUID = -2875282502611466531L;
	
	// this is updated when the bead is cloned, so that on a print we see if it is the original bead or a n-th generation clone
	String cloneString = "";
	boolean isClone  = false;
	
	final protected int id;
	final protected ViewDataBeads myView;
	protected double weight = 1;
	protected float distance = -1;
	private Bead parent = null;
	private Bead child = null;
	
	protected boolean isUsed = false;
	
	final private ArrayList<BeadIdentification> descriptorCorrespondence = new ArrayList<BeadIdentification>();
	final private ArrayList<BeadIdentification> ransacCorrespondence = new ArrayList<BeadIdentification>();
	
	public Bead( final int id, final Point3d location, final ViewDataBeads myView )
	{
		super( new float[] { (float)location.x, (float)location.y, (float)location.z} );
		this.id = id;
		this.myView = myView;
	}

	public Bead( final int id, final Point3f location, final ViewDataBeads myView )
	{
		super( new float[] { location.x, location.y, location.z} );
		this.id = id;
		this.myView = myView;
	}

	public Bead( final int id, final float[] location, final ViewDataBeads myView )
	{
		super( location );
		this.id = id;
		this.myView = myView;
	}
		
	public boolean isClonedBead() { return isClone; }
	public Bead getChild() { return child; }
	public Bead getParent() { return parent; }
	private void setChild( final Bead bead ) { this.child = bead; }
	private void setParent( final Bead bead ) { this.parent = bead; }
	
	public void setWeight( final double weight ){ this.weight = weight; }
	public double getWeight(){ return weight; }
	
	public ArrayList<BeadIdentification> getDescriptorCorrespondence() { return descriptorCorrespondence; }
	public ArrayList<BeadIdentification> getRANSACCorrespondence() { return ransacCorrespondence; }
	
	public boolean isUsed() { return isUsed; }
	public void setUsed( final boolean isUsed ) { this.isUsed = isUsed; }
	
	public synchronized void addPointDescriptorCorrespondence( final Bead bead ) { addPointDescriptorCorrespondence(bead, 1); }
	public synchronized void addPointDescriptorCorrespondence( final Bead bead, final double weight )
	{		
		descriptorCorrespondence.add( new BeadIdentification( bead ) );
	}

	public synchronized void addRANSACCorrespondence( final Bead bead ) { addRANSACCorrespondence( bead, 1 ); }
	public synchronized void addRANSACCorrespondence( final Bead bead, final double weight )
	{
		if ( !containsBeadID( descriptorCorrespondence, bead ) )
		{
			IOFunctions.println( "Error: Cannot set RANSAC correspondence for bead " + this +"; it has no correspondence from a point descriptor." );
			return;
		}

		int sameViewIndex = -1;
		
		for ( int i = 0; i < ransacCorrespondence.size(); i++ )
			if ( ransacCorrespondence.get( i ).getViewID() == bead.getViewID() )
				sameViewIndex = i;
		
		if ( sameViewIndex >= 0 )
		{
			IOFunctions.println( "Warning: RANSAC Correspondence for Bead " + this + ", currently " + ransacCorrespondence.get( sameViewIndex ) + " overwritten by " + bead );
			ransacCorrespondence.remove( sameViewIndex );
		}

		ransacCorrespondence.add( new BeadIdentification( bead ) );
	}
	
	public static boolean containsBeadID( Collection<BeadIdentification> list, final Bead bead)
	{
		boolean contains = false;
		
		for ( final BeadIdentification content : list )
			if ( content.getBeadID() == bead.getID() && content.getViewID() == bead.getViewID() )
				contains = true;
		
		return contains;
	}

	public static boolean containsBead( Collection<Bead> list, final Bead bead)
	{
		boolean contains = false;
		
		for ( final Bead content : list )
			if ( content.getID() == bead.getID() && content.getViewID() == bead.getViewID() )
				contains = true;
		
		return contains;
	}
	
	public ViewDataBeads getView() { return myView; }
	public int getViewID() { return myView.getID(); }
	public int getID() { return id; }
	
	public Point getLocation() 
	{
		Point p = new Point( getL() );
		p.apply( myView.getTile().getModel() );

		return p;
	}
	
	public double getDistance( final Bead bead2 )
	{
		double distance = 0;
				
		for ( int i = 0; i < getL().length; i++ )
			distance += Math.pow(getL()[i] - bead2.getW()[i],2);
		
		return Math.sqrt( distance );
	}
	
	public void setDistance( float distance ) 
	{ 
		this.distance = distance;
		
		// also set the appropriate distance for the parent bead so that we can draw properly
		//if ( this.isClonedBead() )
			//parent.setDistance( distance );
		
		if ( this.child != null )
			child.setDistance( distance );
	}
	public float getDistance() { return distance; }
	
	@Override
	public String toString()
	{
		String desc = cloneString + "Bead " + getID() + " l"+ MathLib.printCoordinates( getL() ) + "; w"+ MathLib.printCoordinates( getW() );
		
		if ( myView != null)
			return desc + " of view " + myView;
		else
			return desc + " - no view assigned";
	}
	
	public boolean equals( final Bead bead )
	{
		if ( this.getID() == bead.getID() && this.getViewID() == bead.getViewID() )
			return true;
		else
			return false;
	}

	public boolean equals( final BeadIdentification beadID )
	{
		if ( this.getID() == beadID.getBeadID() && this.getViewID() == beadID.getViewID() )
			return true;
		else
			return false;
	}

	public static boolean equals( final Bead bead1, final Bead bead2 )
	{
		if ( bead1.getID() == bead2.getID() && bead1.getViewID() == bead2.getViewID() )
			return true;
		else
			return false;
	}
	
	@Override
	public synchronized Bead clone()
	{
		// instantiate the clone 		
		final Bead bead = new Bead( getID(), getL().clone(), myView );
		
		// set global coordinates
		bead.setW( getW() );
		
		// the new bead is a clone
		bead.isClone = true;
		
		// the toString method will show that it is a clone
		bead.cloneString = this.cloneString + "clone of ";
				
		// update all correspondence candidates
		for ( final BeadIdentification b : this.descriptorCorrespondence )
			bead.descriptorCorrespondence.add( b );
		
		// update all true correspondences
		for ( final BeadIdentification b : this.ransacCorrespondence )
			bead.ransacCorrespondence.add( b );		
		
		// the new bead's parent is the current bead
		bead.setParent( this );

		// the new bead is the child of the current bead
		this.setChild( bead );
		
		return bead;		
	}
	
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
	
}
