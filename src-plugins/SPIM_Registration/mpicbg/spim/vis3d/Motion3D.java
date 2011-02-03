package mpicbg.spim.vis3d;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Group;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import mpicbg.spim.io.ConfigurationParserGeneral;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.ProgramConfiguration;

import com.sun.j3d.utils.geometry.Sphere;

import ij.ImagePlus;
import ij3d.Content;
import ij3d.Image3DUniverse;

public class Motion3D
{
	final public static ImagePlus getScreenShot()
	{
		//Get the screen size  
		Toolkit toolkit = Toolkit.getDefaultToolkit();  
		Dimension screenSize = toolkit.getScreenSize();  
		Rectangle rect = new Rectangle(0, 0, screenSize.width, screenSize.height);
		
		Robot robot = null;
		try
		{
			robot = new Robot();
		}
		catch (AWTException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  

		return new ImagePlus("fdf", robot.createScreenCapture(rect));
	}

	final public static void saveScreenShot( String fileName )
	{
		//Get the screen size  
		Toolkit toolkit = Toolkit.getDefaultToolkit();  
		Dimension screenSize = toolkit.getScreenSize();  
		Rectangle rect = new Rectangle(0, 0, screenSize.width, screenSize.height);
		
		Robot robot = null;
		try
		{
			robot = new Robot();
		}
		catch (AWTException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  

		BufferedImage image = robot.createScreenCapture(rect);
		
		//Save the screenshot as a png  
	    File file = new File( fileName );  
	    try
		{
			ImageIO.write(image, "png", file);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  		
	}
	
	final protected Color3f foregroundColor = new Color3f( 0f, 0f, 0f );
	final protected LineAttributes boundingBoxLineAttributes = new LineAttributes();
	public Motion3D()
	{
		Image3DUniverse univ = VisualizationFunctions.initStandardUniverse();
		
		ArrayList<Point3f> list = new ArrayList<Point3f>();
		list.add( new Point3f(0,0,0) );
		list.add( new Point3f(10,0,0) );
		list.add( new Point3f(0,20,0) );
		list.add( new Point3f(0,0,30) );
		
		Transform3D trans = new Transform3D();		
		
		BranchGroup branchGroup = drawBeads(univ, list, trans, foregroundColor, 10, 0.5f);
		
		final Content content = univ.addLineMesh( list, foregroundColor, "BoundingBox", false );// 0, false, boundingBoxLineAttributes);
		
		for (int i = 0; i < 200; i++)
		{
			try
			{
				Thread.sleep(100);
			}
			catch (final InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Vector3f p = new Vector3f(0.4f, 0, 0);
			Transform3D inc = new Transform3D();
			inc.setTranslation(p);
				
			updateTransformBranchGroup(branchGroup, inc);
			updateTransformBranchGroup(content, inc);

		}
	}
	
	public static void updateTransformBranchGroup( final BranchGroup branchGroup, final Transform3D update )
	{
		Enumeration<TransformGroup> en = branchGroup.getAllChildren();
		
		while (en.hasMoreElements())
		{
			TransformGroup tg = en.nextElement();
			Transform3D tmp = new Transform3D();
			
			tg.getTransform( tmp );
			tmp.mul( update );
			
			tg.setTransform( tmp );
		}		
	}

	public static void replaceTransformBranchGroup( final BranchGroup branchGroup, final Transform3D replacement )
	{
		replaceTransformBranchGroup((Group)branchGroup, replacement);
	}
	
	public static void replaceTransformBranchGroup( final Group branchGroup, final Transform3D replacement )
	{
		Enumeration<?> en = branchGroup.getAllChildren();
		
		while (en.hasMoreElements())
		{
			Object o = en.nextElement();
			
			if ( o instanceof TransformGroup )
			{
				TransformGroup tg = (TransformGroup)o;
				tg.setTransform( new Transform3D( replacement ) );
			}
			else if ( o instanceof Switch )
			{
				replaceTransformBranchGroup( (Switch)o, replacement );
			}
		}		
	}
	
	public BranchGroup drawBeads( final Image3DUniverse univ, final Collection<Point3f> points, final Transform3D globalTransform, final Color3f color, final float beadSize, final float transparency )
	{		
		// get the scene
		final BranchGroup parent = univ.getScene();

		// set color and transparency
		final Appearance appearance = new Appearance();
		appearance.setColoringAttributes( new ColoringAttributes( color, ColoringAttributes.SHADE_FLAT ) );
		appearance.setTransparencyAttributes( new TransparencyAttributes( TransparencyAttributes.FASTEST, transparency ) );

		// create a new branch group that contains all transform groups which contain one sphere each
		final BranchGroup viewBranch = new BranchGroup();
		
		// init the structures needed to code the position of the beads
		final Transform3D transform = new Transform3D();		
		final Point3f translation = new Point3f();

		// add all beads
		for ( Iterator<Point3f> i = points.iterator(); i.hasNext(); )
		{
			final Point3f bead = i.next();
			
			// set the bead coordinates
			translation.set( bead );
			
			// transform the bead coordinates into the position of the view
			globalTransform.transform( translation );
			
			// create new TransformGroup with the altered coordinates 
			transform.setTranslation( new Vector3f( translation ) );			
			final TransformGroup transformGroup = new TransformGroup( transform );
			transformGroup.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );

			// add the sphere
			final Sphere s = new Sphere( beadSize, Sphere.BODY, 10, appearance );
			transformGroup.addChild( s );
			
			// add the group to the view branch
			viewBranch.addChild(transformGroup);				
		}				

		// ????
		viewBranch.compile();
		
		// add the view branch to the scene
		parent.addChild( viewBranch );
		
		return viewBranch;
	}
	
	public static void main(String[] args) 
	{
		// read&parse configuration file
		ProgramConfiguration conf = null;
		try
		{
			conf = ConfigurationParserGeneral.parseFile("config/configuration.txt");
		} 
		catch (final Exception e)
		{
			IOFunctions.printErr("Cannot open configuration file: " + e);
			e.printStackTrace();
			return;
		}

		// open imageJ window
		System.getProperties().setProperty("plugins.dir", conf.pluginsDir);
		final String params[] = {"-ijpath " + conf.pluginsDir};
		ij.ImageJ.main(params);
		
		new Motion3D();
	}
}
