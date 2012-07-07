package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.visualization.threedviewer.SpotGroupNode;
import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.Image3DUniverse;

import java.awt.Color;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeMap;

import javax.vecmath.Color4f;
import javax.vecmath.Point4f;

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
		HashMap<fiji.plugin.trackmate.SpotImp, Point4f>  centers = new HashMap<fiji.plugin.trackmate.SpotImp, Point4f>(N_BLOBS);
		HashMap<fiji.plugin.trackmate.SpotImp, Color4f> colors = new HashMap<fiji.plugin.trackmate.SpotImp, Color4f>(N_BLOBS);
		Point4f center;
		Color4f color;
		fiji.plugin.trackmate.SpotImp spot;
		float[] coords = new float[3];
		for (int i = 0; i < N_BLOBS; i++) {
			coords[0] = WIDTH * ran.nextFloat();
			coords[1] = HEIGHT * ran.nextFloat();
			coords[2] = DEPTH * ran.nextFloat();
			
			center = new Point4f(coords[0], coords[1], coords[2], (float) (RADIUS + ran.nextGaussian()));
			color = new Color4f(new Color(Color.HSBtoRGB(ran.nextFloat(), 1, 1)));
			color.w = ran.nextFloat();
			spot = new fiji.plugin.trackmate.SpotImp(coords);
			centers.put(spot, center);
			colors.put(spot, color);
		}
		
		SpotGroupNode<fiji.plugin.trackmate.SpotImp> sg = new SpotGroupNode<fiji.plugin.trackmate.SpotImp>(centers, colors);
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
		
		for (fiji.plugin.trackmate.SpotImp key : centers.keySet()) {
			sg.setVisible(key, false);
			Thread.sleep(2000/N_BLOBS);
		}
		
		for (fiji.plugin.trackmate.SpotImp key : centers.keySet()) {
			sg.setVisible(key, true);
			Thread.sleep(2000/N_BLOBS);
		}
		
		fiji.plugin.trackmate.SpotImp thisSpot = centers.keySet().iterator().next();
		
		for (int i = 1; i < WIDTH; i++) {
			sg.setRadius(thisSpot, i);
			Thread.sleep(2000/WIDTH);
		}
		
		Point4f p = centers.get(thisSpot);
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
