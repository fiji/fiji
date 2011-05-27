package fiji.plugin.trackmate.visualization.test;

import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.Image3DUniverse;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javax.vecmath.Color4f;
import javax.vecmath.Point4f;

import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.visualization.TrackMateModelView.TrackDisplayMode;
import fiji.plugin.trackmate.visualization.SpotGroupNode;
import fiji.plugin.trackmate.visualization.TrackDisplayNode;

public class TrackDisplayNodeTestDrive {

	public static void main(String[] args) {
		
		final int N_BLOBS = 50;
		final int WIDTH = 200;
		final int DEPTH = 50;
		final int RADIUS = 10;

		float x, y, z, r, theta;
		float[] coords;
		Spot spot;
		Spot[] spots = new Spot[N_BLOBS];
		ArrayList<Spot>	 alSpot;
		SpotCollection timeSpots = new SpotCollection();
		SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph = new SimpleWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		TreeMap<Integer, ContentInstant> spotInstants = new TreeMap<Integer, ContentInstant>();
		Color4f color;
		Point4f center;
		
		for (int i = 0; i < N_BLOBS; i++) {
			
			theta =  (float) (4 * 2 * Math.PI * i / N_BLOBS);
			r = (float) ((0.1 + 0.9 * i / N_BLOBS ) * WIDTH / 2);
			x = (float) (r * Math.cos(theta));
			y = (float) (r * Math.sin(theta));
			z = (float) i / N_BLOBS * DEPTH;
			coords = new float[] { x, y, z };
			spot = new SpotImp(coords);
			spot.putFeature(Feature.POSITION_T, i);
			spot.putFeature(Feature.RADIUS, r);
			
			spots[i] = spot;
			
			HashMap<Spot, Point4f> centers = new HashMap<Spot, Point4f>(N_BLOBS);
			HashMap<Spot, Color4f> spotColors = new HashMap<Spot, Color4f>(N_BLOBS);
			
			center = new Point4f(coords[0], coords[1], coords[2], (float)RADIUS);
			color = new Color4f(Color.MAGENTA);
			color.w = 0;
			
			centers.put(spot, center);
			spotColors.put(spot, color);
			
			alSpot = new ArrayList<Spot>();
			alSpot.add(spot);
			timeSpots.put(i, alSpot);
			
			graph.addVertex(spot);
			if (i > 0) 
				graph.addEdge(spots[i-1], spot);
			
			SpotGroupNode<Spot> sg = new SpotGroupNode<Spot>(centers, spotColors);
			ContentInstant ci = new ContentInstant("t"+i);
			ci.display(sg);
			spotInstants.put(i, ci);
			
		}
		
		// Spot content
		Content spotContent = new Content("Spots", spotInstants);
		
		ij.ImageJ.main(args);
		Image3DUniverse universe = new Image3DUniverse();
		universe.show();
		universe.addContent(spotContent);

		
		// Track content
		List<Set<Spot>> tracks = new ConnectivityInspector<Spot, DefaultWeightedEdge>(graph).connectedSets();
		int ntracks = tracks.size();
		HashMap<Set<Spot>, Color> trackColors = new HashMap<Set<Spot>, Color>(tracks.size());

		int index = 0;
		float value;
		for(Set<Spot> track : tracks) {
			value = (float) index / ntracks;
			trackColors.put(track, InterpolatePaintScale.Jet.getPaint(value));
			index++;
		}

		TrackDisplayNode tdn = new TrackDisplayNode(graph, timeSpots, tracks, trackColors);
		ContentInstant ciTracks = new ContentInstant("TracksInstant");
		ciTracks.display(tdn);
		TreeMap<Integer, ContentInstant> trackInstants = new TreeMap<Integer, ContentInstant>();
		trackInstants.put(0, ciTracks);
		Content trackContent = new Content("Tracks", trackInstants);
		trackContent.setShowAllTimepoints(true);
		universe.addContentLater(trackContent);
		tdn.setDisplayTrackMode(TrackDisplayMode.LOCAL_WHOLE_TRACKS, 10);
		universe.addTimelapseListener(tdn);
		
	}
	
}
