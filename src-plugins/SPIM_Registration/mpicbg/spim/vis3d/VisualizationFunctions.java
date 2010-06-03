package mpicbg.spim.vis3d;

import ij3d.Content;
import ij3d.Image3DUniverse;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.geometry.Sphere;

import mpicbg.models.Point;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.bead.Bead;
import mpicbg.spim.registration.segmentation.Nucleus;

public class VisualizationFunctions
{
	public static Image3DUniverse initStandardUniverse()
	{
		return initStandardUniverse( 1024, 768 );
	}
	
	public static Image3DUniverse initStandardUniverse( final int width, final int height )
	{
		final Image3DUniverse uni = new Image3DUniverse( width, height );
		uni.show();

		Viewer3dFunctions.setBackgroundColor( uni, new Color3f( 1,1,1 ) );
		Viewer3dFunctions.setStatusBarLayout( uni, new Color3f( 0f, 0.5f, 0.59f ), new Font("Sans", Font.PLAIN, 10) );		
		
		return uni;
	}

	public static Color3f getRandomPastellColor( final float lowerBorder ) { return getRandomPastellColor( System.currentTimeMillis(), lowerBorder ); }
	public static Color3f getRandomColor() { return getRandomColor( System.nanoTime() ); }

	public static Color3f getRandomColor( final long seed )
	{
		try
		{
			Thread.sleep(10);
		}
		catch (InterruptedException e)
		{}
		final Random rnd = new Random( seed );
		return new Color3f( rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat() );
	}

	public static Color3f getRandomPastellColor( final long seed, final float lowerBorder )
	{
		try
		{
			Thread.sleep(10);
		}
		catch (InterruptedException e)
		{}
		final Random rnd = new Random( seed );
		return new Color3f( rnd.nextFloat()*(1-lowerBorder) + lowerBorder, rnd.nextFloat()*(1-lowerBorder) + lowerBorder, rnd.nextFloat()*(1-lowerBorder) + lowerBorder );
	}
	

	public static Content drawArrow( final Image3DUniverse univ, 
			 						 final Vector3f to, final float arrowHeadAngle, final float arrowHeadLength,
			 						 final Color3f color )
	{
		return drawArrow( univ, new Point3f(), new Point3f( to ), arrowHeadAngle, arrowHeadLength, color );
	}
	
	public static Content drawArrow( final Image3DUniverse univ, 
			 						 final Point3f from, final Point3f to, final float arrowHeadAngle, final float arrowHeadLength,
			 						 final Color3f color )
	{
		return drawArrow( univ, from, to, arrowHeadAngle, arrowHeadLength, null, color );
	}

	public static Content drawArrow( final Image3DUniverse univ, 
									 final Vector3f to, final float arrowHeadAngle, final float arrowHeadLength )
	{
		return drawArrow( univ, new Point3f(), new Point3f( to ), arrowHeadAngle, arrowHeadLength );
	}
	
	public static Content drawArrow( final Image3DUniverse univ, 
			 						 final Point3f from, final Point3f to, final float arrowHeadAngle, final float arrowHeadLength )
	{
		return drawArrow( univ, from, to, arrowHeadAngle, arrowHeadLength, null, new Color3f( 0, 0, 0 ) );
	}

	public static Content drawArrow( final Image3DUniverse univ, 
			 						 final Vector3f to, final float arrowHeadAngle, final float arrowHeadLength, 
			 						 final LineAttributes attrib, final Color3f color )
	{
		return drawArrow(univ, new Point3f(), new Point3f( to ), arrowHeadAngle, arrowHeadLength, attrib, color);
	}

	public static Content drawArrow( final Image3DUniverse univ, 
									 final Point3f from, final Point3f to, final float arrowHeadAngle, final float arrowHeadLength, 
			 						 final LineAttributes attrib, final Color3f color )
	{
		final ArrayList<Point3f> list = VisualizationFunctions.makeArrow( from, to, arrowHeadAngle, arrowHeadLength );
		
		final Content c;
		
		if ( attrib == null )
			c = univ.addLineMesh( list, color, "arrow" + to, false ); //0, false );
		else
			c = univ.addLineMesh( list, color, "arrow" + to, false ); //( list, color, "arrow" + to, 0, false, attrib );		
	
		c.showCoordinateSystem(false);

		return c;
	}

	public static ArrayList<Point3f> makeArrow( final Point3f from, final Point3f to, final float arrowHeadAngle, final float arrowHeadLength)
	{
		// get the vector of the line
		final Vector3f v = new Vector3f( to );
		v.sub( from );
		v.normalize();

		// the orthogonal vectors to compute
		Vector3f a,b;

		final Vector3f x = new Vector3f( 1, 0, 0 );
		final float length = v.dot(x);

		if (length > 0.9999 && length < 1.0001)
		{
			a = new Vector3f(0, 1, 0);
		}
		else
		{
			final Vector3f tmp = new Vector3f( v );

			tmp.scale( x.dot(v) );

			a = new Vector3f( x );
			a.sub(tmp);
		}

		b = new Vector3f();
		b.cross(a, v);

		// create the arrow
		final ArrayList<Point3f> arrowList = new ArrayList<Point3f>();
		arrowList.add(from);
		arrowList.add(to);

		computeArrowLines(arrowList, to, v, a, b, arrowHeadAngle, arrowHeadLength);
		return arrowList;

	}

	protected static void computeArrowLines(final ArrayList<Point3f> list, final Point3f to, final Vector3f v, final Vector3f a, final Vector3f b, 
											final float arrowHeadAngle, final float arrowHeadLength)
	{
		final Vector3f a1 = new Vector3f(a);
		final Vector3f b1 = new Vector3f(b);
		final Vector3f v1 = new Vector3f(v);

		a1.scale((float)Math.sin(arrowHeadAngle));
		b1.scale((float)Math.sin(arrowHeadAngle));
		v1.scale((float)Math.cos(arrowHeadAngle));

		a1.scale( arrowHeadLength );
		b1.scale( arrowHeadLength );
		v1.scale( arrowHeadLength );

		Point3f arrow = new Point3f(to);
		arrow.sub(v1);
		arrow.sub(a1);
		list.add(to);
		list.add(arrow);

		arrow = new Point3f(to);
		arrow.sub(v1);
		arrow.add(a1);
		list.add(to);
		list.add(arrow);

		arrow = new Point3f(to);
		arrow.sub(v1);
		arrow.add(b1);
		list.add(to);
		list.add(arrow);

		arrow = new Point3f(to);
		arrow.sub(v1);
		arrow.sub(b1);
		list.add(to);
		list.add(arrow);
	}
	
	final public static boolean storeBeadPosition = true;

	public static BranchGroup drawBeads( final Image3DUniverse univ, final Collection<Bead> beads, final Transform3D globalTransform, 
			final Color3f color, final float beadSize, final float transparency )
	{
		return drawBeads(univ, beads, globalTransform, color, beadSize, transparency, false );
	}
	
	public static BranchGroup drawBeads( final Image3DUniverse univ, final Collection<Bead> beads, final Transform3D globalTransform, 
			final Color3f color, final float beadSize, final float transparency, boolean storeBeadPosition )
	{		
		// get the scene
		final BranchGroup parent = univ.getScene();

		// set color and transparency
		final Appearance appearance = new Appearance();
		appearance.setColoringAttributes( new ColoringAttributes( color, ColoringAttributes.SHADE_FLAT ) );
		appearance.setTransparencyAttributes( new TransparencyAttributes( TransparencyAttributes.FASTEST, transparency ) );

		// create a new branch group that contains all transform groups which contain one sphere each
		final BranchGroup viewBranch = new BranchGroup();
		viewBranch.setCapability( BranchGroup.ALLOW_CHILDREN_WRITE );
		
		// init the structures needed to code the position of the beads
		final Transform3D transform = new Transform3D();
		final Point3f translation = new Point3f();

		// add all beads
		for ( Iterator<Bead> i = beads.iterator(); i.hasNext(); )
		{
			final Bead bead = i.next();
			
			//if ( !drawNonRANSACBeads && bead.getRANSACCorrespondence().size() < 1 )
				//continue;
			
			// set the bead coordinates
			translation.set( bead.getL() );
			
			// transform the bead coordinates into the position of the view
			globalTransform.transform( translation );
			
			// create new TransformGroup with the altered coordinates 
			transform.setTranslation( new Vector3f( translation ) );			
			final TransformGroup transformGroup;
			
			if ( storeBeadPosition )
				transformGroup = new BeadTransformGroup( transform, bead );
			else
				transformGroup = new TransformGroup( transform );
			
			transformGroup.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
			transformGroup.setCapability( TransformGroup.ALLOW_CHILDREN_WRITE );

			// add the sphere
			final Sphere s = new Sphere( beadSize, Sphere.BODY, 10, appearance );
			s.setCapability( Sphere.ENABLE_APPEARANCE_MODIFY );
			s.getShape().setCapability( Shape3D.ALLOW_APPEARANCE_WRITE );
			transformGroup.addChild( s );
			
			// add the group to the view branch
			viewBranch.addChild(transformGroup);				
		}				

		// ????
		if ( !storeBeadPosition )
			viewBranch.compile();
				
		// add the view branch to the scene
		parent.addChild( viewBranch );
		
		return viewBranch;
	}

	public static BranchGroup drawNuclei( final Image3DUniverse univ, final Collection<Nucleus> nuclei, final Transform3D globalTransform, 
			final Color3f color, final float nucleusSize, final float transparency )
	{		
		// get the scene
		final BranchGroup parent = univ.getScene();

		// set color and transparency
		final Appearance appearanceNon = new Appearance();
		appearanceNon.setColoringAttributes( new ColoringAttributes( color, ColoringAttributes.SHADE_FLAT ) );
		appearanceNon.setTransparencyAttributes( new TransparencyAttributes( TransparencyAttributes.FASTEST, transparency ) );

		final Appearance appearanceCorr = new Appearance();
		appearanceCorr.setColoringAttributes( new ColoringAttributes( new Color3f( 0, 1, 0 ), ColoringAttributes.SHADE_FLAT ) );
		appearanceCorr.setTransparencyAttributes( new TransparencyAttributes( TransparencyAttributes.FASTEST, transparency/2 ) );

		final Appearance appearanceRef = new Appearance();
		appearanceRef.setColoringAttributes( new ColoringAttributes( new Color3f( 0, 0, 0 ), ColoringAttributes.SHADE_FLAT ) );
		appearanceRef.setTransparencyAttributes( new TransparencyAttributes( TransparencyAttributes.FASTEST, 0 ) );

		final Appearance appearanceNN = new Appearance();
		appearanceNN.setColoringAttributes( new ColoringAttributes( new Color3f( 1, 0, 0 ), ColoringAttributes.SHADE_FLAT ) );
		appearanceNN.setTransparencyAttributes( new TransparencyAttributes( TransparencyAttributes.FASTEST, 0.5f ) );

		// create a new branch group that contains all transform groups which contain one sphere each
		final BranchGroup viewBranch = new BranchGroup();
		viewBranch.setCapability( BranchGroup.ALLOW_CHILDREN_WRITE );
		
		// init the structures needed to code the position of the beads
		final Transform3D transform = new Transform3D();
		final Point3f translation = new Point3f();

		// add all beads
		for ( Iterator<Nucleus> i = nuclei.iterator(); i.hasNext(); )
		{
			final Nucleus nucleus = i.next();
			
			//if ( !drawNonRANSACBeads && bead.getRANSACCorrespondence().size() < 1 )
				//continue;
			
			// set the bead coordinates
			translation.set( nucleus.getW() );
			
			// transform the bead coordinates into the position of the view
			globalTransform.transform( translation );
			
			// create new TransformGroup with the altered coordinates 
			transform.setTranslation( new Vector3f( translation ) );			
			final TransformGroup transformGroup;
			
			//if ( storeBeadPosition )
				//transformGroup = new BeadTransformGroup( transform, bead );
			//else
				transformGroup = new TransformGroup( transform );
			
			transformGroup.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
			transformGroup.setCapability( TransformGroup.ALLOW_CHILDREN_WRITE );

			// add the sphere
			final Sphere s;
			
			/*
			if ( nucleus.isReference )
				s = new Sphere( nucleusSize, Sphere.BODY, 10, appearanceRef );
			else if ( nucleus.isCoordinate )
				s = new Sphere( nucleusSize, Sphere.BODY, 10, appearanceNN );
			else if ( nucleus.isCorrespondence )
				s = new Sphere( nucleusSize, Sphere.BODY, 10, appearanceNon );
			else
				continue;
			*/
			
			if ( nucleus.isReference )
				s = new Sphere( nucleusSize, Sphere.BODY, 10, appearanceRef );
			else if ( nucleus.isCoordinate )
				s = new Sphere( nucleusSize, Sphere.BODY, 10, appearanceNN );
			else if ( nucleus.isCorrespondence )
				s = new Sphere( nucleusSize, Sphere.BODY, 10, appearanceCorr );
			else
				s = new Sphere( nucleusSize, Sphere.BODY, 10, appearanceNon );
			
			
			s.setCapability( Sphere.ENABLE_APPEARANCE_MODIFY );
			s.getShape().setCapability( Shape3D.ALLOW_APPEARANCE_WRITE );
			transformGroup.addChild( s );
			
			// add the group to the view branch
			viewBranch.addChild(transformGroup);				
		}				

		// ????
		if ( !storeBeadPosition )
			viewBranch.compile();
				
		// add the view branch to the scene
		parent.addChild( viewBranch );
		
		return viewBranch;
	}

	public static BranchGroup drawPoints( final Image3DUniverse univ, final Collection<Point> points, final Transform3D globalTransform, 
			final Color3f color, final float pointSize, final float transparency )
	{		
		// get the scene
		final BranchGroup parent = univ.getScene();

		// set color and transparency
		final Appearance appearance = new Appearance();
		appearance.setColoringAttributes( new ColoringAttributes( color, ColoringAttributes.SHADE_GOURAUD ) );
		appearance.setTransparencyAttributes( new TransparencyAttributes( TransparencyAttributes.NICEST, transparency ) );

		// create a new branch group that contains all transform groups which contain one sphere each
		final BranchGroup viewBranch = new BranchGroup();
		viewBranch.setCapability( BranchGroup.ALLOW_CHILDREN_WRITE );
		
		// init the structures needed to code the position of the beads
		final Transform3D transform = new Transform3D();
		final Point3f translation = new Point3f();

		// add all beads
		for ( Iterator<Point> i = points.iterator(); i.hasNext(); )
		{
			final Point point = i.next();
			
			//if ( !drawNonRANSACBeads && bead.getRANSACCorrespondence().size() < 1 )
				//continue;
			
			// set the bead coordinates
			translation.set( point.getW() );
			
			// transform the bead coordinates into the position of the view
			globalTransform.transform( translation );
			
			// create new TransformGroup with the altered coordinates 
			transform.setTranslation( new Vector3f( translation ) );			
			final TransformGroup transformGroup;
			
			//if ( storeBeadPosition )
				//transformGroup = new BeadTransformGroup( transform, bead );
			//else
				transformGroup = new TransformGroup( transform );
			
			transformGroup.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
			transformGroup.setCapability( TransformGroup.ALLOW_CHILDREN_WRITE );

			// add the sphere
			final Sphere s = new Sphere( pointSize, Sphere.BODY, 100, appearance );			
			
			s.setCapability( Sphere.ENABLE_APPEARANCE_MODIFY );
			s.getShape().setCapability( Shape3D.ALLOW_APPEARANCE_WRITE );
			transformGroup.addChild( s );
			
			// add the group to the view branch
			viewBranch.addChild(transformGroup);				
		}				

		// ????
		if ( !storeBeadPosition )
			viewBranch.compile();
				
		// add the view branch to the scene
		parent.addChild( viewBranch );
		
		return viewBranch;
	}

	public static ArrayList<Point3f> getTransformedBoundingBox( final ViewDataBeads view )
	{
		final ArrayList<Point3f> boundingBox = new ArrayList<Point3f>();
		
		final Transform3D transformation = view.getTransform3D();
		final int[] imageSize = view.getImageSize();
		
		Point3f from, to;
		
		from = new Point3f(0,0,0);
		to = new Point3f(view.getImageSize()[0], 0, 0);
		view.getTransform3D().transform( from );
		transformation.transform( to );		
		boundingBox.add( from );
		boundingBox.add( to );
		
		from = new Point3f(imageSize[0], 0, 0);
		to = new Point3f(imageSize[0], imageSize[1], 0);
		transformation.transform( from );
		transformation.transform( to );		
		boundingBox.add( from );
		boundingBox.add( to );

		from = new Point3f(imageSize[0], imageSize[1], 0);
		to = new Point3f(0, imageSize[1], 0);
		transformation.transform( from );
		transformation.transform( to );		
		boundingBox.add( from );
		boundingBox.add( to );
		
		from = new Point3f(0, imageSize[1], 0);
		to = new Point3f(0, 0, 0);
		transformation.transform( from );
		transformation.transform( to );		
		boundingBox.add( from );
		boundingBox.add( to );


		from = new Point3f(0, 0, imageSize[2]);
		to = new Point3f(imageSize[0], 0, imageSize[2]);
		transformation.transform( from );
		transformation.transform( to );		
		boundingBox.add( from );
		boundingBox.add( to );

		from = new Point3f(imageSize[0], 0,  imageSize[2]);
		to = new Point3f(imageSize[0], imageSize[1],  imageSize[2]);
		transformation.transform( from );
		transformation.transform( to );		
		boundingBox.add( from );
		boundingBox.add( to );

		from = new Point3f(imageSize[0], imageSize[1],  imageSize[2]);
		to = new Point3f(0, imageSize[1], imageSize[2]);
		transformation.transform( from );
		transformation.transform( to );		
		boundingBox.add( from );
		boundingBox.add( to );
		
		from = new Point3f(0, imageSize[1],  imageSize[2]);
		to = new Point3f(0, 0, imageSize[2]);
		transformation.transform( from );
		transformation.transform( to );		
		boundingBox.add( from );
		boundingBox.add( to );

		
		from = new Point3f(0, 0, 0);
		to = new Point3f(0, 0, imageSize[2]);
		transformation.transform( from );
		transformation.transform( to );		
		boundingBox.add( from );
		boundingBox.add( to );

		from = new Point3f(imageSize[0], 0, 0);
		to = new Point3f(imageSize[0], 0, imageSize[2]);
		transformation.transform( from );
		transformation.transform( to );		
		boundingBox.add( from );
		boundingBox.add( to );

		from = new Point3f(imageSize[0], imageSize[1], 0);
		to = new Point3f(imageSize[0], imageSize[1], imageSize[2]);
		transformation.transform( from );
		transformation.transform( to );		
		boundingBox.add( from );
		boundingBox.add( to );

		from = new Point3f(0, imageSize[1], 0);
		to = new Point3f(0, imageSize[1], imageSize[2]);
		transformation.transform( from );
		transformation.transform( to );		
		boundingBox.add( from );
		boundingBox.add( to );
		
		return boundingBox;
	}

	public static ArrayList<Point3f> getBoundingBox( final ViewDataBeads view )
	{
		final ArrayList<Point3f> boundingBox = new ArrayList<Point3f>();
		final int[] imageSize = view.getImageSize();
		
		boundingBox.add( new Point3f(0,0,0) );
		boundingBox.add( new Point3f(imageSize[0], 0, 0) );
		
		boundingBox.add( new Point3f(imageSize[0], 0, 0) );
		boundingBox.add( new Point3f(imageSize[0], imageSize[1], 0) );

		boundingBox.add( new Point3f(imageSize[0], imageSize[1], 0) );
		boundingBox.add( new Point3f(0, imageSize[1], 0) );
		
		boundingBox.add( new Point3f(0, imageSize[1], 0) );
		boundingBox.add( new Point3f(0, 0, 0) );

		boundingBox.add( new Point3f(0, 0, imageSize[2]) );
		boundingBox.add( new Point3f(imageSize[0], 0, imageSize[2]) );

		boundingBox.add( new Point3f(imageSize[0], 0,  imageSize[2]) );
		boundingBox.add( new Point3f(imageSize[0], imageSize[1],  imageSize[2]) );

		boundingBox.add( new Point3f(imageSize[0], imageSize[1],  imageSize[2]) );
		boundingBox.add( new Point3f(0, imageSize[1], imageSize[2]) );

		boundingBox.add( new Point3f(0, imageSize[1],  imageSize[2]) );
		boundingBox.add( new Point3f(0, 0, imageSize[2]) );

		boundingBox.add( new Point3f(0, 0, 0) );
		boundingBox.add( new Point3f(0, 0, imageSize[2]) );

		boundingBox.add( new Point3f(imageSize[0], 0, 0) );
		boundingBox.add( new Point3f(imageSize[0], 0, imageSize[2]) );

		boundingBox.add( new Point3f(imageSize[0], imageSize[1], 0) );
		boundingBox.add( new Point3f(imageSize[0], imageSize[1], imageSize[2]) );

		boundingBox.add( new Point3f(0, imageSize[1], 0) );
		boundingBox.add( new Point3f(0, imageSize[1], imageSize[2]) );
		
		return boundingBox;
	}

	public static ArrayList<Point3f> getBoundingBox( final float minX, final float maxX, final float minY, final float maxY, final float minZ, final float maxZ )
	{
		final ArrayList<Point3f> boundingBox = new ArrayList<Point3f>();
		
		boundingBox.add( new Point3f(minX, minY , minZ) );
		boundingBox.add( new Point3f(maxX, minY , minZ) );
		
		boundingBox.add( new Point3f(maxX, minY, minZ) );
		boundingBox.add( new Point3f(maxX, maxY, minZ) );

		boundingBox.add( new Point3f(maxX, maxY, minZ) );
		boundingBox.add( new Point3f(minX, maxY, minZ) );
		
		boundingBox.add( new Point3f(minX, maxY, minZ) );
		boundingBox.add( new Point3f(minX, minY, minZ) );

		boundingBox.add( new Point3f(minX, minY, maxZ) );
		boundingBox.add( new Point3f(maxX, minY, maxZ) );

		boundingBox.add( new Point3f(maxX, minY,  maxZ) );
		boundingBox.add( new Point3f(maxX, maxY,  maxZ) );

		boundingBox.add( new Point3f(maxX, maxY,  maxZ) );
		boundingBox.add( new Point3f(minX, maxY, maxZ) );

		boundingBox.add( new Point3f(minX, maxY,  maxZ) );
		boundingBox.add( new Point3f(minX, minY, maxZ) );

		boundingBox.add( new Point3f(minX, minY, minZ) );
		boundingBox.add( new Point3f(minX, minY, maxZ) );

		boundingBox.add( new Point3f(maxX, minY, minZ) );
		boundingBox.add( new Point3f(maxX, minY, maxZ) );

		boundingBox.add( new Point3f(maxX, maxY, minZ) );
		boundingBox.add( new Point3f(maxX, maxY, maxZ) );

		boundingBox.add( new Point3f(minX, maxY, minZ) );
		boundingBox.add( new Point3f(minX, maxY, maxZ) );
		
		return boundingBox;
	}
	
	public static Content drawBoundingBox( final Image3DUniverse univ, 
										   final float minX, final float maxX, final float minY, final float maxY, final float minZ, 
										   final float maxZ, final Color3f color, final LineAttributes lineAttributes )
	{
		final Content content = univ.addLineMesh( getBoundingBox( minX, maxX, minY, maxY, minZ, maxZ ), color, "BoundingBox", false );//, 0, false, lineAttributes);
		//Motion3D.replaceTransformBranchGroup( content, transform );
		content.showCoordinateSystem(false);
		
		return content;
	}

	public static Content drawView( final Image3DUniverse univ, final ViewDataBeads view, final Color3f color, final LineAttributes lineAttributes, final Transform3D transform )
	{
		final Content content = univ.addLineMesh( getBoundingBox( view ), color, "BoundingBox" + view.getName(), false ); //, 0, false, lineAttributes);
		Motion3D.replaceTransformBranchGroup( content, transform );
		content.showCoordinateSystem(false);
		
		return content;
	}

	public static Content drawView( final Image3DUniverse univ, final ViewDataBeads view, final Color3f color, final LineAttributes lineAttributes )
	{
		final Content content = univ.addLineMesh( getBoundingBox( view ), color, "BoundingBox" + view.getName(), false); //, 0, false, lineAttributes);
		Motion3D.replaceTransformBranchGroup( content, view.getTransform3D() );
		content.showCoordinateSystem(false);
		
		return content;
	}
	
	public static Content[] drawViews( final Image3DUniverse univ, final ViewDataBeads[] views, final Color3f color, final LineAttributes lineAttributes )
	{
		Content boundingBox[] = new Content[ views.length ];		
		for (int i = 0; i < views.length; i++)
			boundingBox[i] = VisualizationFunctions.drawView( univ, views[i], color, lineAttributes );	
		
		return boundingBox;
	}
}
