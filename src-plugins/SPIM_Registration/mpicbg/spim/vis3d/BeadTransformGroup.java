package mpicbg.spim.vis3d;

import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import mpicbg.spim.registration.bead.Bead;

public class BeadTransformGroup extends TransformGroup
{
	final Vector3f beadPositionVector;
	final Point3f beadPositionPoint;
	final protected Bead bead;
	
	public BeadTransformGroup( final Transform3D transformation, final Bead bead )
	{
		super( transformation );
		
		this.bead = bead;
		this.beadPositionVector = new Vector3f( bead.getL() );
		this.beadPositionPoint = new Point3f( bead.getL() );
	}
	
	public Vector3f getBeadPositionVector() { return new Vector3f( beadPositionVector ); }
	public Point3f getBeadPositionPoint() { return new Point3f( beadPositionPoint ); }
	
	public void getBeadPositionPoint( final Point3f point ) { point.set( beadPositionPoint ); }
	public void getBeadPositionVector( final Vector3f vector ) { vector.set( beadPositionVector ); }
	
	public Bead getBead() { return bead; }
}