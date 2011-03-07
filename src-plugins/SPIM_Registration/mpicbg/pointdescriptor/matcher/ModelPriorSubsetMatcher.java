package mpicbg.pointdescriptor.matcher;

import java.util.ArrayList;

import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import mpicbg.models.PointMatch;
import mpicbg.pointdescriptor.fit.FitResult;
import mpicbg.pointdescriptor.model.RigidModel3D;
import mpicbg.pointdescriptor.model.TranslationInvariantRigidModel3D;

public class ModelPriorSubsetMatcher extends SubsetMatcher
{
	final RigidModel3D model;
	final Point3f referenceAxis;
	final float angle;
	
	final Matrix3f referenceMatrix, invertedReferenceMatrix;

	public ModelPriorSubsetMatcher( final int subsetSize, final int numNeighbors, final RigidModel3D model )
	{
		super( subsetSize, numNeighbors );
		
		this.model = model;
		
		this.referenceMatrix = new Matrix3f();                
        model.getTransform3D().get( referenceMatrix );

        this.invertedReferenceMatrix = new Matrix3f( this.referenceMatrix );
		this.invertedReferenceMatrix.invert();

		final Quat4f quaternion = new Quat4f();	     
        quaternion.set( referenceMatrix );
        
        this.angle = (float)Math.toDegrees( Math.acos( quaternion.getW() ) * 2 );
        final Vector3f axis = new Vector3f( quaternion.getX(), quaternion.getY(), quaternion.getZ() );
        axis.normalize();        
        this.referenceAxis = new Point3f( axis );
	}

	@Override
	public double getNormalizationFactor( final ArrayList<PointMatch> matches, final FitResult fitResult ) 
	{
		final TranslationInvariantRigidModel3D matchModel = (TranslationInvariantRigidModel3D)fitResult;
		
		/* get input matrices and quaternion that we can alter */
		final Quat4f quaternion = new Quat4f();
		final Matrix3f templateMatrix = new Matrix3f();
		matchModel.getMatrix3f( templateMatrix );
		
		/* Compute the rotation angle between the two rigid 3d transformations */
		templateMatrix.mul( invertedReferenceMatrix );		
        quaternion.set( templateMatrix );
        
        final float angle = Math.max( 5, (float)Math.toDegrees( Math.acos( quaternion.getW() )  * 2 ) ) - 5;
        
        /* Compute vector difference between the two rotation axes */
        //final Vector3f axis = new Vector3f( quaternion.getX(), quaternion.getY(), quaternion.getZ() );
        //axis.normalize();        
       	//final Point3f templateAxis = new Point3f( axis );
        //final float difference = templateAxis.distance( referenceAxis );

        final float weight = ( 1.0f + 0.03f * angle * angle );
        
        
		return weight; 
        //return Math.pow( 10, difference );	
	}
	
}
