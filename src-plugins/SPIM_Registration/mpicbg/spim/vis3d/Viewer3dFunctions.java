package mpicbg.spim.vis3d;

import ij3d.Content;
import ij3d.Image3DUniverse;
import ij3d.ImageCanvas3D;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Label;
import java.awt.Panel;
import java.util.ArrayList;

import javax.media.j3d.Background;
import javax.media.j3d.LineAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

public class Viewer3dFunctions
{

	protected Color3f backgroundColor = new Color3f(1, 1, 1);
	protected Color3f foregroundColor = new Color3f(0.5f, 0.5f, 0.6f);
	protected Font statusbarFont = new Font("Cambria", Font.PLAIN, 12);

	public Viewer3dFunctions()
	{
		final ArrayList<Point3f> list1 = new ArrayList<Point3f>();
		list1.add(new Point3f(0,0,0));
		list1.add(new Point3f(100,100,100));
		list1.add(new Point3f(100,50,100));
		list1.add(new Point3f(100,100,50));
		list1.add(new Point3f(10,100,100));

		final ArrayList<Point3f> listb = VisualizationFunctions.makeArrow(new Point3f(0,0,0), new Point3f(0, 50, 50), (float)Math.toRadians(90), 5);

		final Image3DUniverse uni = new Image3DUniverse(800, 600);
		uni.show();

		setBackgroundColor( uni, backgroundColor );
		setStatusBarLayout( uni, foregroundColor, statusbarFont );

		final LineAttributes attrs = new LineAttributes();
		attrs.setLineWidth(10);

		//final Content c1 = uni.addLineMesh(list1, new Color3f(0, 0, 255), "sdgf", 0, true, attrs);

		final LineAttributes attrs2 = new LineAttributes();
		attrs2.setLineWidth(2);
		uni.addLineMesh(listb, new Color3f(0, 0, 0), "sf2", false); //0, false, attrs2);
		setStatusBar(uni, "sdjgsdjgsdjgsd;jg;sd;jgs;dg");

		//final Random rnd = new Random(35325235);

		for (int i = 0; i < 20; i++)
		{
			final ArrayList<Point3f> list2 = VisualizationFunctions.makeArrow(new Point3f(0,0,0), new Point3f(75, 100, i*5), (float)Math.toRadians(20), 5);
			final Content c = uni.addLineMesh(list2, new Color3f(0, 0, 0), "sdgf"+i, false ); //0, false);
			c.showCoordinateSystem(false);

			try
			{
				Thread.sleep(200);
			}
			catch (final InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		final Panel p = new Panel();
		p.setPreferredSize(new Dimension(80, 20));
		final Button status = new Button("View Details");
		p.add(status);
		uni.getWindow().add(p, BorderLayout.EAST, -1);
		uni.getWindow().pack();


		try
		{
			Thread.sleep(2000);
		}
		catch (final InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		setStatusBarLayout( uni, new Color3f(255, 0, 0), statusbarFont );
	}

	public static void setStatusBarLayout( final Image3DUniverse universe, final Color3f color, final Font font )
	{
		final Label status = getStatusBar( universe );
		status.setForeground( new Color((int)(color.x*255), (int)(color.y*255), (int)(color.z*255)) );
		status.setFont( font );
	}

	public static void setStatusBar( final Image3DUniverse universe, final String text )
	{
		final Label status = getStatusBar(universe);
		status.setText( text );

	}

	public static void setBackgroundColor( final Image3DUniverse universe, final Color3f color )
	{
		final Background background = ((ImageCanvas3D)universe.getCanvas()).getBG();
		background.setColor( color );

		final Label status = getStatusBar(universe);
		status.setBackground(new Color((int)(color.x*255), (int)(color.y*255), (int)(color.z*255)));
	}

	public static Label getStatusBar( final Image3DUniverse universe )
	{
		return universe.getWindow().getStatusLabel();
	}
}
