package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.threedviewer.SpotGroupNode;
import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.Image3DUniverse;

import java.awt.Color;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeMap;

import javax.vecmath.Color4f;
import javax.vecmath.Point4d;

public class SpotGroupNodeTestDrive {

	/*
	 * STATIC METHODS
	 */

	public static void main(String args[]) throws InterruptedException {
		final int N_BLOBS = 100;
		final int WIDTH = 200;
		final int HEIGHT = 200;
		final int DEPTH = 50;
		final int RADIUS = 10;
		
		Random ran = new Random();
		HashMap<Spot, Point4d>  centers = new HashMap<Spot, Point4d>(N_BLOBS);
		HashMap<Spot, Color4f> colors = new HashMap<Spot, Color4f>(N_BLOBS);
		Point4d center;
		Color4f color;
		Spot spot;
		double[] coords = new double[3];
		for (int i = 0; i < N_BLOBS; i++) {
			coords[0] = WIDTH * ran.nextDouble();
			coords[1] = HEIGHT * ran.nextDouble();
			coords[2] = DEPTH * ran.nextDouble();
			
			center = new Point4d(coords[0], coords[1], coords[2], RADIUS + ran.nextGaussian());
			color = new Color4f(new Color(Color.HSBtoRGB(ran.nextFloat(), 1, 1)));
			color.w = ran.nextFloat();
			spot = new fiji.plugin.trackmate.Spot(coords);
			centers.put(spot, center);
			colors.put(spot, color);
		}
		
		SpotGroupNode<Spot> sg = new SpotGroupNode<Spot>(centers, colors);
		//sg.setName("spots");
		ContentInstant ci = new ContentInstant("t0");
		ci.display(sg);
		TreeMap<Integer, ContentInstant> instants = new TreeMap<Integer, ContentInstant>();
		instants.put(0, ci);
		Content c = new Content("instants", instants);
		
		ij.ImageJ.main(args);
		Image3DUniverse universe = new Image3DUniverse();
		universe.show();
		universe.addContentLater(c);
		
		for (Spot key : centers.keySet()) {
			sg.setVisible(key, false);
			Thread.sleep(2000/N_BLOBS);
		}
		
		for (Spot key : centers.keySet()) {
			sg.setVisible(key, true);
			Thread.sleep(2000/N_BLOBS);
		}
		
		Spot thisSpot = centers.keySet().iterator().next();
		
		for (int i = 1; i < WIDTH; i++) {
			sg.setRadius(thisSpot, i);
			Thread.sleep(2000/WIDTH);
		}
		
		Point4d p = centers.get(thisSpot);
		for (int i = 0; i < WIDTH; i++) {
			p.x = i;
			p.y = i;
			sg.setCenter(thisSpot, p);
			Thread.sleep(2000/WIDTH);
		}
		
		for (int i = 1; i <= 100; i++) {
			sg.setTransparency(thisSpot, (float)i/100);
			Thread.sleep(2000/100);
		}
		
		Color4f col = colors.get(thisSpot);
		for (int i = 100; i >= 1; i--) {
			col.w =  (float)i/100;
			col.x =  (float)i/100;
			sg.setColor(thisSpot, col);
			Thread.sleep(2000/100);
		}
	}

}
