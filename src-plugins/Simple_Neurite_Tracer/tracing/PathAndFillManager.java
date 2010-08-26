/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009, 2010 Mark Longair */

/*
  This file is part of the ImageJ plugin "Simple Neurite Tracer".

  The ImageJ plugin "Simple Neurite Tracer" is free software; you
  can redistribute it and/or modify it under the terms of the GNU
  General Public License as published by the Free Software
  Foundation; either version 3 of the License, or (at your option)
  any later version.

  The ImageJ plugin "Simple Neurite Tracer" is distributed in the
  hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the GNU General Public License for more
  details.

  In addition, as a special exception, the copyright holders give
  you permission to combine this program with free software programs or
  libraries that are released under the Apache Public License.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package tracing;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.PriorityQueue;
import java.util.LinkedList;

import java.io.*;

import ij.*;

import ij.measure.Calibration;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;

import javax.media.j3d.View;
import ij3d.Content;
import ij3d.UniverseListener;

import util.Bresenham3D;
import util.XMLFunctions;

@SuppressWarnings("serial")
class TracesFileFormatException extends SAXException {
	public TracesFileFormatException(String message) {
		super(message);
	}
}

@SuppressWarnings("serial")
class SWCExportException extends Exception {
	public SWCExportException(String message) {
		super(message);
	}
}

public class PathAndFillManager extends DefaultHandler implements UniverseListener {

	static final boolean verbose = SimpleNeuriteTracer.verbose;

	SimpleNeuriteTracer plugin;
	ImagePlus imagePlus;

	int maxUsedID = -1;

	boolean needImageDataFromTracesFile;

	public PathAndFillManager( ) {
		allPaths = new ArrayList< Path >();
		allFills = new ArrayList< Fill >();
		listeners = new ArrayList< PathAndFillListener >();
		selectedPathsSet = new HashSet<Path>();
		needImageDataFromTracesFile = true;
		this.imagePlus = null;
		this.x_spacing = Double.MIN_VALUE;
		this.y_spacing = Double.MIN_VALUE;
		this.z_spacing = Double.MIN_VALUE;
		this.spacing_units = null;
		this.width = Integer.MIN_VALUE;
		this.height = Integer.MIN_VALUE;
		this.depth = Integer.MIN_VALUE;
	}

	public PathAndFillManager( ImagePlus imagePlus ) {
		this();
		this.imagePlus = imagePlus;
		Calibration c = imagePlus.getCalibration();
		this.x_spacing = c.pixelWidth;
		this.y_spacing = c.pixelHeight;
		this.z_spacing = c.pixelDepth;
		this.spacing_units = c.getUnit();
		if( this.spacing_units == null || this.spacing_units.length() == 0 )
			this.spacing_units = "" + c.getUnit();
		this.width = imagePlus.getWidth();
		this.height = imagePlus.getHeight();
		this.depth = imagePlus.getStackSize();
		needImageDataFromTracesFile = false;
	}

	public PathAndFillManager( SimpleNeuriteTracer plugin ) {
		this();
		this.plugin = plugin;
		this.x_spacing = plugin.x_spacing;
		this.y_spacing = plugin.y_spacing;
		this.z_spacing = plugin.z_spacing;
		this.spacing_units = plugin.spacing_units;
		this.width = plugin.width;
		this.height = plugin.height;
		this.depth = plugin.depth;
		needImageDataFromTracesFile = false;
	}

	public PathAndFillManager( int width, int height, int depth, float x_spacing, float y_spacing, float z_spacing, String spacing_units ) {
		this();
		this.x_spacing = x_spacing;
		this.y_spacing = y_spacing;
		this.z_spacing = z_spacing;
		this.width = width;
		this.height = height;
		this.depth = depth;
		if( spacing_units == null )
			this.spacing_units = "unknown";
		needImageDataFromTracesFile = false;
	}

	int width;
	int height;
	int depth;

	double x_spacing;
	double y_spacing;
	double z_spacing;
	String spacing_units;

	ArrayList< Path > allPaths;
	ArrayList< Fill > allFills;

	ArrayList< PathAndFillListener > listeners;

	HashSet< Path > selectedPathsSet;

	public int size() {
		return allPaths.size();
	}

	/* This is used by the interface to have changes in the path
	   manager reported so that they can be reflected in the UI. */

	public synchronized void addPathAndFillListener( PathAndFillListener listener ) {
		listeners.add(listener);
	}

	public synchronized Path getPath( int i ) {
		return allPaths.get(i);
	}

	public synchronized Path getPathFromName( String name ) {
		return getPathFromName( name, true );
	}
	public synchronized Path getPathFromName( String name, boolean caseSensitive ) {
		for( Path p : allPaths ) {
			if( caseSensitive ) {
				if( name.equals(p.getName()) )
					return p;
			} else {
				if( name.equalsIgnoreCase(p.getName()) )
					return p;
			}
		}
		return null;
	}

	public synchronized Path getPathFromID( int id ) {
		for( Path p : allPaths ) {
			if( id == p.getID() ) {
				return p;
			}
		}
		return null;
	}

	/* This is called to update the PathAndFillManager's idea of
	   which paths are currently selected.  This is also
	   propagated to:

	       (a) Each Path object (so that the 3D viewer can reflect
	       the change, for instance.)

	       (b) All the registered PathAndFillListener objects.
	*/
	public synchronized void setSelected( Path [] selectedPaths, Object sourceOfMessage ) {
		selectedPathsSet.clear();
		for( int i = 0; i < selectedPaths.length; ++i )
			selectedPathsSet.add( selectedPaths[i] );
		for( PathAndFillListener pafl : listeners ) {
			if( pafl != sourceOfMessage )
				// The source of the message already knows the states:
				pafl.setSelectedPaths( selectedPathsSet, this );
		}
		if( plugin != null ) {
			plugin.repaintAllPanes();
			plugin.update3DViewerContents();
		}
	}

	public synchronized boolean isSelected( Path path ) {
		return selectedPathsSet.contains(path);
	}

	public boolean anySelected( ) {
		return selectedPathsSet.size() > 0;
	}

	/* This method returns an array of the "primary paths", which
	   should be displayed at the top of a tree-like hierarchy.

	   The paths actually form a graph, of course, but most UIs
	   will want to display the graph as a tree. */

	public synchronized Path [] getPathsStructured() {

		ArrayList<Path> primaryPaths=new ArrayList<Path>();

		/* Some paths may be explicitly marked as primary, so
		   extract those and everything connected to them
		   first.  If you encounter another path marked as
		   primary when exploring from these then that's an
		   error... */

		TreeSet<Path> pathsLeft = new TreeSet<Path>();

		for( int i = 0; i < allPaths.size(); ++i ) {
			pathsLeft.add(allPaths.get(i));
		}

		int markedAsPrimary = 0;

		/* This is horrendously inefficent but with the number
		   of paths that anyone might reasonably add by hand
		   (I hope!) it's acceptable. */

		Iterator<Path> pi = pathsLeft.iterator();
		Path primaryPath = null;
		while( pi.hasNext() ) {
			Path p = pi.next();
			if( p.getPrimary() ) {
				pi.remove();
				primaryPaths.add(p);
				++ markedAsPrimary;
			}
		}

		for( int i = 0; i < primaryPaths.size(); ++i ) {
			primaryPath = primaryPaths.get(i);
			primaryPath.setChildren(pathsLeft);
		}

		// Start with each one left that doesn't start on another:
		boolean foundOne = true;
		while( foundOne ) {
			foundOne = false;
			pi = pathsLeft.iterator();
			while( pi.hasNext() ) {
				Path p = pi.next();
				if( p.startJoins == null ) {
					foundOne = true;
					pi.remove();
					primaryPaths.add(p);
					p.setChildren(pathsLeft);
					break;
				}
			}
		}

		// If there's anything left, start with that:
		while( pathsLeft.size() > 0 ) {
			pi = pathsLeft.iterator();
			Path p = pi.next();
			pi.remove();
			primaryPaths.add(p);
			p.setChildren(pathsLeft);
		}

		return primaryPaths.toArray(new Path[]{});
	}

	public synchronized ArrayList<SWCPoint> getSWCFor( Set<Path> selectedPaths ) throws SWCExportException {

		/* Turn the primary paths into a Set.  This call also
		   ensures that the Path.children and
		   Path.somehowJoins relationships are set up
		   correctly: */
		Set<Path> structuredPathSet = new HashSet<Path>(Arrays.asList(getPathsStructured()));

		/* Check that there's only one primary path in
		   selectedPaths by taking the intersection and
		   checking there's exactly one element in it: */

		structuredPathSet.retainAll(selectedPaths);

		if( structuredPathSet.size() == 0 )
			throw new SWCExportException("The paths you select for SWC export must include a primary path\n(i.e. one at the top level in the Path Window's tree)");
		if( structuredPathSet.size() > 1 )
			throw new SWCExportException("You can only select one connected set of paths for SWC export");

		/* So now we definitely only have one primary path.
		   All the connected paths must also be selected, but
		   we'll check that as we go along: */

		ArrayList<SWCPoint> result = new ArrayList<SWCPoint>();

		int currentPointID = 1;

		/* nextPathsToAdd is the queue of Paths to add points
		   from, and pathsAlreadyDone is the set of Paths that
		   have already had their points added */

		LinkedList<Path> nextPathsToAdd = new LinkedList<Path>();
		Set<Path> pathsAlreadyDone = new HashSet<Path>();

		Path firstPath = (Path)structuredPathSet.iterator().next();
		if( firstPath.size() == 0 )
			throw new SWCExportException("The primary path contained no points!");
		nextPathsToAdd.add( firstPath );

		while( nextPathsToAdd.size() > 0 ) {

			Path currentPath = nextPathsToAdd.removeFirst();

			if( ! selectedPaths.contains(currentPath) )
				throw new SWCExportException("The path \""+currentPath+"\" is connected to other selected paths, but wasn't itself selected");

			/* The paths we're dealing with specify
			   connectivity, but we might be using the
			   fitted versions - take them for the point
			   positions: */

			Path pathToUse = currentPath;
			if( currentPath.getUseFitted() ) {
				pathToUse = currentPath.fitted;
			}

			Path parent = null;

			for( Path possibleParent : currentPath.somehowJoins ) {
				if( pathsAlreadyDone.contains( possibleParent ) ) {
					parent = possibleParent;
					break;
				}
			}

			int indexToStartAt = 0;
			int nearestParentSWCPointID = -1;
			PointInImage connectingPoint = null;
			if( parent != null ) {
				if( currentPath.startJoins != null &&
				    currentPath.startJoins == parent )
					connectingPoint = currentPath.startJoinsPoint;
				else if( currentPath.endJoins != null &&
					 currentPath.endJoins == parent )
					connectingPoint = currentPath.endJoinsPoint;
				else if( parent.startJoins != null &&
					 parent.startJoins == currentPath )
					connectingPoint = parent.startJoinsPoint;
				else if( parent.endJoins != null &&
					 parent.endJoins == currentPath )
					connectingPoint = parent.endJoinsPoint;
				else
					throw new SWCExportException("Couldn't find the link between parent \""+parent+"\"\nand child \""+currentPath+"\" which are somehow joined");

				/* Find the SWC point ID on the parent which is nearest: */

				double distanceSquaredToNearestParentPoint = Double.MAX_VALUE;
				for( SWCPoint s : result ) {
					if( s.fromPath != parent )
						continue;
					double distanceSquared = connectingPoint.distanceSquaredTo(s.x, s.y, s.z);
					if( distanceSquared < distanceSquaredToNearestParentPoint ) {
						nearestParentSWCPointID = s.id;
						distanceSquaredToNearestParentPoint = distanceSquared;
					}
				}

				/* Now find the index of the point on this path which is nearest */
				indexToStartAt = pathToUse.indexNearestTo( connectingPoint.x,
									   connectingPoint.y,
									   connectingPoint.z );
			}

			SWCPoint firstSWCPoint = null;

			boolean realRadius = pathToUse.hasCircles();
			for( int i = indexToStartAt; i < pathToUse.points; ++i ) {
				double radius = 0;
				if( realRadius )
					radius = pathToUse.radiuses[i];
				SWCPoint swcPoint = new SWCPoint(currentPointID,
								 Path.SWC_UNDEFINED,
								 pathToUse.precise_x_positions[i],
								 pathToUse.precise_y_positions[i],
								 pathToUse.precise_z_positions[i],
								 radius,
								 firstSWCPoint == null ?  nearestParentSWCPointID : currentPointID - 1);
				swcPoint.fromPath = currentPath;
				result.add(swcPoint);
				++ currentPointID;
				if( firstSWCPoint == null )
					firstSWCPoint = swcPoint;
			}

			boolean firstOfOtherBranch = true;
			for( int i = indexToStartAt - 1; i >= 0; --i ) {
				int previousPointID = currentPointID - 1;
				if( firstOfOtherBranch ) {
					firstOfOtherBranch = false;
					previousPointID = firstSWCPoint.id;
				}
				double radius = 0;
				if( realRadius )
					radius = pathToUse.radiuses[i];
				SWCPoint swcPoint = new SWCPoint(currentPointID,
								 Path.SWC_UNDEFINED,
								 pathToUse.precise_x_positions[i],
								 pathToUse.precise_y_positions[i],
								 pathToUse.precise_z_positions[i],
								 radius,
								 previousPointID);
				swcPoint.fromPath = currentPath;
				result.add(swcPoint);
				++ currentPointID;
			}

			pathsAlreadyDone.add( currentPath );

			/* Add all the connected paths that aren't already in pathsAlreadyDone */

			for( Path connectedPath : currentPath.somehowJoins ) {
				if( ! pathsAlreadyDone.contains( connectedPath ) ) {
					nextPathsToAdd.add( connectedPath );
				}
			}
		}

		// Now check that all selectedPaths are in pathsAlreadyDone, otherwise give an error:

		Path disconnectedExample = null;
		int selectedAndNotConnected = 0;
		for( Path selectedPath : selectedPaths ) {
			if( ! pathsAlreadyDone.contains(selectedPath) ) {
				++ selectedAndNotConnected;
				if( disconnectedExample == null )
					disconnectedExample = selectedPath;
			}
		}
		if( selectedAndNotConnected > 0 )
			throw new SWCExportException("You must select all the connected paths\n("+selectedAndNotConnected+" paths (e.g. \""+disconnectedExample+"\") were not connected.)");

		return result;
	}

	public synchronized void resetListeners( Path justAdded ) {
		resetListeners( justAdded, false );
	}

	public synchronized void resetListeners( Path justAdded, boolean expandAll ) {

		ArrayList<String> pathListEntries = new ArrayList<String>();

		for( Path p : allPaths ) {
			int pathID = p.getID();
			if( p == null ) {
				throw new RuntimeException("BUG: A path in allPaths was null!");
			}
			String pathName;
			String name = p.getName();
			if( name == null )
				name = "Path [" + pathID + "]";
			if( p.startJoins != null ) {
				name += ", starts on " + p.startJoins.getName();
			}
			if( p.endJoins != null ) {
				name += ", ends on " + p.endJoins.getName();
			}
			name += " [" + p.getRealLengthString() + " " + spacing_units + "]";
			pathListEntries.add( name );
		}

		for( PathAndFillListener listener : listeners )
			listener.setPathList( pathListEntries.toArray( new String[]{} ), justAdded, expandAll );

		int fills = allFills.size();

		String [] fillListEntries = new String[fills];

		for( int i = 0; i < fills; ++i ) {

			Fill f = allFills.get(i);
			if( f == null ) {
				if (verbose) System.out.println("fill was null with i "+i+" out of "+fills );
				continue;
			}

			String name = "Fill (" + i + ")";

			if( (f.sourcePaths != null) && (f.sourcePaths.size() > 0) ) {
				name += " from paths: " + f.getSourcePathsStringHuman();
			}
			fillListEntries[i] = name;
		}

		for( PathAndFillListener pafl : listeners )
			pafl.setFillList( fillListEntries );

	}

	public void addPath( Path p ) {
		addPath(p,false);
	}

	public synchronized void addPath( Path p, boolean forceNewName ) {
		if( getPathFromID( p.getID() ) != null )
			throw new RuntimeException("Attempted to add a path with an ID that was already added");
		if( p.getID() < 0 ) {
			p.setID(++maxUsedID);
		}
		if( maxUsedID < p.getID() )
			maxUsedID = p.getID();
		if(p.name == null || forceNewName) {
			String suggestedName = getDefaultName(p);
			p.setName(suggestedName);
		}
		// Now check if there's already a path with this name.
		// If so, try adding numbered suffixes:
		String originalName = p.getName();
		String candidateName = originalName;
		int numberSuffix = 2;
		while( getPathFromName( candidateName ) != null ) {
			candidateName = originalName + " (" + numberSuffix + ")";
			++ numberSuffix;
		}
		p.setName( candidateName );
		/* Generate a new content3D, since it matters that the
		   path is added with the right name via
		   update3DViewerContents: */
		if( plugin != null && plugin.use3DViewer ) {
			p.removeFrom3DViewer( plugin.univ );
			p.addTo3DViewer( plugin.univ, plugin.deselectedColor3f, plugin.colorImage );
		}
		allPaths.add(p);
		resetListeners( p );
	}

	/* Find the default name for a new path, making sure it
	   doesn't collide with any of the existing names: */

	protected String getDefaultName(Path p) {
		if( p.getID() < 0 )
			throw new RuntimeException("A path's ID should never be negative");
		return "Path ("+p.getID()+")";
	}

	public synchronized void deletePath( int index ) {
		deletePath( index, true );
	}

	public synchronized void deletePath( Path p ) {
		int i = getPathIndex( p );
		if( i < 0 )
			throw new RuntimeException("Trying to delete a non-existent path: "+p);
		deletePath( i );
	}

	public synchronized int getPathIndex( Path p ) {
		int i = 0;
		for( i = 0; i < allPaths.size(); ++i ) {
			if( p == allPaths.get( i ) )
				return i;
		}
		return -1;
	}

	private synchronized void deletePath( int index, boolean updateInterface ) {

		Path originalPathToDelete = allPaths.get(index);

		Path unfittedPathToDelete = null;
		Path fittedPathToDelete = null;

		if( originalPathToDelete.fittedVersionOf == null ) {
			unfittedPathToDelete = originalPathToDelete;
			fittedPathToDelete = originalPathToDelete.fitted;
		} else {
			unfittedPathToDelete = originalPathToDelete.fittedVersionOf;
			fittedPathToDelete = originalPathToDelete;
		}

		allPaths.remove(unfittedPathToDelete);
		if( fittedPathToDelete != null )
			allPaths.remove(fittedPathToDelete);

		// We don't just delete; have to fix up the references
		// in other paths (for start and end joins):

		for( Path p : allPaths ) {
			if( p.startJoins == unfittedPathToDelete ) {
				p.startJoins = null;
				p.startJoinsPoint = null;
			}
			if( p.endJoins == unfittedPathToDelete ) {
				p.endJoins = null;
				p.endJoinsPoint = null;
			}
		}

		selectedPathsSet.remove(fittedPathToDelete);
		selectedPathsSet.remove(unfittedPathToDelete);

		if( plugin != null && plugin.use3DViewer ) {
			if( fittedPathToDelete != null && fittedPathToDelete.content3D != null )
				fittedPathToDelete.removeFrom3DViewer(plugin.univ);
			if( unfittedPathToDelete.content3D != null )
				unfittedPathToDelete.removeFrom3DViewer(plugin.univ);
		}

		if( updateInterface )
			resetListeners( null );
	}

	public void deletePaths( int [] indices ) {

		Arrays.sort( indices );

		for( int i = indices.length - 1; i >= 0; --i ) {
			deletePath( indices[i], false );
		}

		resetListeners( null );
	}

	public void addFill( Fill fill ) {

		allFills.add(fill);
		resetListeners( null );
	}

	public void deleteFills( int [] indices ) {

		Arrays.sort( indices );

		for( int i = indices.length - 1; i >= 0; --i ) {
			deleteFill( indices[i], false );
		}

		resetListeners( null );
	}

	public void deleteFill( int index ) {
		deleteFill( index, true );
	}

	private synchronized void deleteFill( int index, boolean updateInterface ) {

		allFills.remove( index );

		if( updateInterface )
			resetListeners( null );
	}

	public void reloadFill( int index ) {

		Fill toReload = allFills.get(index);

		plugin.startFillerThread( FillerThread.fromFill( plugin.getImagePlus(),
								 plugin.stackMin,
								 plugin.stackMax,
								 true,
								 toReload ) );

	}

	// FIXME: should probably use XMLStreamWriter instead of this ad-hoc approach:
	synchronized public void writeXML( String fileName,
					   boolean compress ) throws IOException {

		PrintWriter pw = null;

		try {
			if( compress )
				pw = new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(fileName)),"UTF-8"));
			else
				pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName),"UTF-8"));

			pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			pw.println("<!DOCTYPE tracings [");
			pw.println("  <!ELEMENT tracings       (samplespacing,imagesize,path*,fill*)>");
			pw.println("  <!ELEMENT imagesize      EMPTY>");
			pw.println("  <!ELEMENT samplespacing  EMPTY>");
			pw.println("  <!ELEMENT path           (point+)>");
			pw.println("  <!ELEMENT point          EMPTY>");
			pw.println("  <!ELEMENT fill           (node*)>");
			pw.println("  <!ELEMENT node           EMPTY>");
			pw.println("  <!ATTLIST samplespacing  x                 CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST samplespacing  y                 CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST samplespacing  z                 CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST samplespacing  units             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST imagesize      width             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST imagesize      height            CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST imagesize      depth             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST path           id                CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST path           primary           CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           name              CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           startson          CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           startsindex       CDATA           #IMPLIED>"); // deprecated
			pw.println("  <!ATTLIST path           startsx           CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           startsy           CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           startsz           CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           endson            CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           endsindex         CDATA           #IMPLIED>"); // deprecated
			pw.println("  <!ATTLIST path           endsx             CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           endsy             CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           endsz             CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           reallength        CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           usefitted         (true|false)    #IMPLIED>");
			pw.println("  <!ATTLIST path           fitted            CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           fittedversionof   CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST point          x                 CDATA           #REQUIRED>"); // deprecated
			pw.println("  <!ATTLIST point          y                 CDATA           #REQUIRED>"); // deprecated
			pw.println("  <!ATTLIST point          z                 CDATA           #REQUIRED>"); // deprecated
			pw.println("  <!ATTLIST point          xd                CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST point          yd                CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST point          zd                CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST point          tx                CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST point          ty                CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST point          tz                CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST point          r                 CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST fill           id                CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST fill           frompaths         CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST fill           metric            CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST fill           threshold         CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST fill           volume            CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST node           id                CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           x                 CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           y                 CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           z                 CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           previousid        CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST node           distance          CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           status            (open|closed)   #REQUIRED>");
			pw.println("]>");
			pw.println("");

			pw.println("<tracings>");

			pw.println("  <samplespacing x=\"" + x_spacing + "\" " +
				   "y=\"" + y_spacing + "\" " +
				   "z=\"" + z_spacing + "\" " +
				   "units=\"" + spacing_units + "\"/>" );

			pw.println("  <imagesize width=\"" + width + "\" height=\"" + height + "\" depth=\"" + depth + "\"/>" );

			for( Path p : allPaths ) {
				// This probably should be a String returning
				// method of Path.
				pw.print("  <path id=\"" + p.getID() + "\"" );
				String startsString = "";
				String endsString = "";
				if( p.startJoins != null ) {
					int startPathID = p.startJoins.getID();
					// Find the nearest index for backward compatability:
					int nearestIndexOnStartPath = -1;
					if( p.startJoins.size() > 0 ) {
						nearestIndexOnStartPath = p.startJoins.indexNearestTo(
							p.startJoinsPoint.x,
							p.startJoinsPoint.y,
							p.startJoinsPoint.z );
					}
					startsString = " startson=\"" + startPathID + "\"" +
						" startx=\"" + p.startJoinsPoint.x + "\"" +
						" starty=\"" + p.startJoinsPoint.y + "\"" +
						" startz=\"" + p.startJoinsPoint.z + "\"";
					if( nearestIndexOnStartPath >= 0 )
						startsString += " startsindex=\"" + nearestIndexOnStartPath + "\"";
				}
				if( p.endJoins != null ) {
					int endPathID = p.endJoins.getID();
					// Find the nearest index for backward compatability:
					int nearestIndexOnEndPath = -1;
					if( p.endJoins.size() > 0 ) {
						nearestIndexOnEndPath = p.endJoins.indexNearestTo(
							p.endJoinsPoint.x,
							p.endJoinsPoint.y,
							p.endJoinsPoint.z );
					}
					endsString = " endson=\"" + endPathID + "\"" +
						" endsx=\"" + p.endJoinsPoint.x + "\"" +
						" endsy=\"" + p.endJoinsPoint.y + "\"" +
						" endsz=\"" + p.endJoinsPoint.z + "\"";
					if( nearestIndexOnEndPath >= 0 )
						endsString += " endsindex=\"" + nearestIndexOnEndPath + "\"";
				}
				if( p.getPrimary() )
					pw.print(" primary=\"true\"");
				pw.print(" usefitted=\""+p.getUseFitted()+"\"");
				if( p.fitted != null ) {
					pw.print(" fitted=\""+p.fitted.getID()+"\"");
				}
				if( p.fittedVersionOf != null ) {
					pw.print(" fittedversionof=\""+p.fittedVersionOf.getID()+"\"");
				}
				pw.print(startsString);
				pw.print(endsString);
				if( p.name != null ) {
					pw.print( " name=\""+XMLFunctions.escapeForXMLAttributeValue(p.name)+"\"" );
				}
				pw.print(" reallength=\"" + p.getRealLength( ) + "\"");
				pw.println( ">" );

				for( int i = 0; i < p.size(); ++i ) {
					int px = p.getXUnscaled(i);
					int py = p.getYUnscaled(i);
					int pz = p.getZUnscaled(i);
					double pxd = p.precise_x_positions[i];
					double pyd = p.precise_y_positions[i];
					double pzd = p.precise_z_positions[i];
					String attributes = "x=\"" + px + "\" " + "y=\"" + py + "\" z=\"" + pz + "\" "+
						"xd=\"" + pxd + "\" yd=\"" + pyd + "\" zd=\"" + pzd + "\"";
					if( p.hasCircles() ) {
						attributes += " tx=\""+p.tangents_x[i]+"\"";
						attributes += " ty=\""+p.tangents_y[i]+"\"";
						attributes += " tz=\""+p.tangents_z[i]+"\"";
						attributes += " r=\""+p.radiuses[i]+"\"";
					}
					pw.println("    <point "+attributes+"/>");
				}
				pw.println( "  </path>" );
			}
			// Now output the fills:
			int fillIndex = 0;
			for( Fill f : allFills ) {
				f.writeXML( pw, fillIndex );
				++ fillIndex;
			}
			pw.println("</tracings>");
		} finally {
			if( pw != null )
				pw.close();
		}
	}

	double parsed_x_spacing;
	double parsed_y_spacing;
	double parsed_z_spacing;

	String parsed_units;

	int parsed_width;
	int parsed_height;
	int parsed_depth;

	Fill current_fill;
	Path current_path;

	HashMap< Integer, Integer > startJoins;
	HashMap< Integer, Integer > startJoinsIndices;
	HashMap< Integer, PointInImage > startJoinsPoints;
	HashMap< Integer, Integer > endJoins;
	HashMap< Integer, Integer > endJoinsIndices;
	HashMap< Integer, PointInImage > endJoinsPoints;
	HashMap< Integer, Boolean > useFittedFields;
	HashMap< Integer, Integer > fittedFields;
	HashMap< Integer, Integer > fittedVersionOfFields;

	ArrayList< int [] > sourcePathIDForFills;

	int last_fill_node_id;

	int last_fill_id;

	HashSet< Integer > foundIDs;

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws TracesFileFormatException {

		if( qName.equals("tracings") ) {

			startJoins            = new HashMap< Integer, Integer >();
			startJoinsIndices     = new HashMap< Integer, Integer >();
			startJoinsPoints      = new HashMap< Integer, PointInImage >();
			endJoins              = new HashMap< Integer, Integer >();
			endJoinsIndices       = new HashMap< Integer, Integer >();
			endJoinsPoints        = new HashMap< Integer, PointInImage >();
			useFittedFields       = new HashMap< Integer, Boolean >();
			fittedFields          = new HashMap< Integer, Integer >();
			fittedVersionOfFields = new HashMap< Integer, Integer >();

			sourcePathIDForFills = new ArrayList< int [] >();
			foundIDs = new HashSet< Integer >();

			last_fill_id = -1;

			/* We need to remove the old paths and fills
			 * before loading the ones: */

			if (verbose) System.out.println("Clearing old paths and fills...");

			clearPathsAndFills();

			if (verbose) System.out.println("Now "+allPaths.size()+" paths and "+allFills.size()+" fills");

		} else if( qName.equals("imagesize") ) {

			try {

				String widthString = attributes.getValue("width");
				String heightString = attributes.getValue("height");
				String depthString = attributes.getValue("depth");

				parsed_width = Integer.parseInt(widthString);
				parsed_height = Integer.parseInt(heightString);
				parsed_depth = Integer.parseInt(depthString);

				if( needImageDataFromTracesFile ) {
					this.width = parsed_width;
					this.height = parsed_height;
					this.depth = parsed_depth;
				} else if( ! ((parsed_width == width) &&
					      (parsed_height == height) &&
					      (parsed_depth == depth)) ) {
					throw new TracesFileFormatException("The image size in the traces file didn't match - it's probably for another image");
				}

			} catch( NumberFormatException e ) {
				throw new TracesFileFormatException("There was an invalid attribute to <imagesize/>: "+e);
			}

		} else if( qName.equals("samplespacing") ) {

			try {

				String xString = attributes.getValue("x");
				String yString = attributes.getValue("y");
				String zString = attributes.getValue("z");
				parsed_units = attributes.getValue("units");

				parsed_x_spacing = Double.parseDouble(xString);
				parsed_y_spacing = Double.parseDouble(yString);
				parsed_z_spacing = Double.parseDouble(zString);

				if( needImageDataFromTracesFile ) {
					this.x_spacing = parsed_x_spacing;
					this.y_spacing = parsed_y_spacing;
					this.z_spacing = parsed_z_spacing;
					this.spacing_units = parsed_units;
				}

			} catch( NumberFormatException e ) {
				throw new TracesFileFormatException("There was an invalid attribute to <samplespacing/>: "+e);
			}

		} else if( qName.equals("path") ) {

			String idString = attributes.getValue("id");
			String d = attributes.getValue("d");

			String useFittedString = attributes.getValue("usefitted");
			String fittedIDString = attributes.getValue("fitted");
			String fittedVersionOfIDString = attributes.getValue("fittedversionof");

			String startsonString =  attributes.getValue("startson");
			String startsindexString =  attributes.getValue("startsindex");
			String startsxString = attributes.getValue("startsx");
			String startsyString = attributes.getValue("startsy");
			String startszString = attributes.getValue("startsz");
			String endsonString =  attributes.getValue("endson");
			String endsindexString =  attributes.getValue("endsindex");
			String endsxString = attributes.getValue("endsx");
			String endsyString = attributes.getValue("endsy");
			String endszString = attributes.getValue("endsz");

			String nameString = attributes.getValue("name");

			String primaryString = attributes.getValue("primary");

			if( startsxString == null && startsyString == null && startszString == null ) { }
			else if( startsxString != null && startsyString != null && startszString != null ) { }
			else {
				throw new TracesFileFormatException("If one of starts[xyz] is specified, all of them must be.");
			}

			if( endsxString == null && endsyString == null && endszString == null ) { }
			else if( endsxString != null && endsyString != null && endszString != null ) { }
			else {
				throw new TracesFileFormatException("If one of ends[xyz] is specified, all of them must be.");
			}

			boolean accurateStartProvided = startsxString != null;
			boolean accurateEndProvided = endsxString != null;

			if( startsonString != null && (startsindexString == null && ! accurateStartProvided)  ) {
				throw new TracesFileFormatException("If startson is specified for a path, then startsindex or starts[xyz] must also be specified.");
			}

			if( endsonString != null && (endsindexString == null && ! accurateStartProvided)  ) {
				throw new TracesFileFormatException("If endson is specified for a path, then endsindex or ends[xyz] must also be specified.");
			}

			int startson, startsindex, endson, endsindex;
			double startsx, startsy, startsz;
			double endsx, endsy, endsz;

			current_path = new Path( x_spacing, y_spacing, z_spacing, spacing_units );

			Integer startsOnInteger = null;
			Integer startsIndexInteger = null;
			PointInImage startJoinPoint = null;
			Integer endsOnInteger = null;
			Integer endsIndexInteger = null;
			PointInImage endJoinPoint = null;

			Integer fittedIDInteger = null;
			Integer fittedVersionOfIDInteger = null;

			if( primaryString != null && primaryString.equals("true") )
				current_path.setPrimary(true);

			int id = -1;

			try {

				id = Integer.parseInt(idString);
				if( foundIDs.contains(id) ) {
					throw new TracesFileFormatException("There is more than one path with ID "+id);
				}
				current_path.setID(id);
				if( id > maxUsedID )
					maxUsedID = id;

				if( startsonString == null ) {
					startson = startsindex = -1;
				} else {
					startson = Integer.parseInt(startsonString);
					startsOnInteger = new Integer( startson );

					if( startsxString == null ) {
						// The index (older file format) was supplied:
						startsindex = Integer.parseInt(startsindexString);
						startsIndexInteger = new Integer( startsindexString );
					} else {
						startJoinPoint = new PointInImage( Double.parseDouble( startsxString ),
										   Double.parseDouble( startsyString ),
										   Double.parseDouble( startszString ) );
					}
				}

				if( endsonString == null )
					endson = endsindex = -1;
				else {
					endson = Integer.parseInt(endsonString);
					endsOnInteger = new Integer( endson );

					if( endsxString != null ) {
						endJoinPoint = new PointInImage( Double.parseDouble( endsxString ),
										 Double.parseDouble( endsyString ),
										 Double.parseDouble( endszString ) );
					} else {
						// The index (older file format) was supplied:
						endsindex = Integer.parseInt(endsindexString);
						endsIndexInteger = new Integer( endsindex );
					}
				}

				if( fittedVersionOfIDString != null )
					fittedVersionOfIDInteger = new Integer( Integer.parseInt(fittedVersionOfIDString) );
				if( fittedIDString != null )
					fittedIDInteger = new Integer( Integer.parseInt(fittedIDString) );

			} catch( NumberFormatException e ) {
				e.printStackTrace();
				throw new TracesFileFormatException("There was an invalid attribute in <path/>: "+e);
			}

			if( nameString == null )
				current_path.setDefaultName();
			else
				current_path.setName(nameString);

			if( startsOnInteger != null )
				startJoins.put( id, startsOnInteger );
			if( endsOnInteger != null )
				endJoins.put( id, endsOnInteger );

			if( startJoinPoint != null )
				startJoinsPoints.put( id, startJoinPoint );
			if( endJoinPoint != null )
				endJoinsPoints.put( id, endJoinPoint );

			if( startsIndexInteger != null ) {
				startJoinsIndices.put( id, startsIndexInteger );
			}
			if( endsIndexInteger != null )
				endJoinsIndices.put( id, endsIndexInteger );

			if( useFittedString == null )
				useFittedFields.put( id, false );
			else {
				if( useFittedString.equals("true") )
					useFittedFields.put( id, true );
				else if( useFittedString.equals("false") )
					useFittedFields.put( id, false );
				else
					throw new TracesFileFormatException("Unknown value for 'fitted' attribute: '"+useFittedString+"'");
			}

			if( fittedIDInteger != null )
				fittedFields.put( id, fittedIDInteger );
			if( fittedVersionOfIDInteger != null )
				fittedVersionOfFields.put( id, fittedVersionOfIDInteger );

		} else if( qName.equals("point") ) {

			try {

				double parsed_xd, parsed_yd, parsed_zd;

				String xdString = attributes.getValue("xd");
				String ydString = attributes.getValue("yd");
				String zdString = attributes.getValue("zd");

				String xString = attributes.getValue("x");
				String yString = attributes.getValue("y");
				String zString = attributes.getValue("z");

				if( xdString != null &&
				    ydString != null &&
				    zdString != null ) {
					parsed_xd = Double.parseDouble(xdString);
					parsed_yd = Double.parseDouble(ydString);
					parsed_zd = Double.parseDouble(zdString);
				} else if( xdString != null ||
					   ydString != null ||
					   zdString != null ) {
					throw new TracesFileFormatException("If one of the attributes xd, yd or zd to the point element is specified, they all must be.");
				} else if( xString != null &&
					   yString != null &&
					   zString != null ) {
					parsed_xd = parsed_x_spacing * Integer.parseInt(xString);
					parsed_yd = parsed_y_spacing * Integer.parseInt(yString);
					parsed_zd = parsed_z_spacing * Integer.parseInt(zString);
				} else if( xString != null ||
					   yString != null ||
					   zString != null ) {
					throw new TracesFileFormatException("If one of the attributes x, y or z to the point element is specified, they all must be.");
				} else {
					throw new TracesFileFormatException("Each point element must have at least the attributes (x, y and z) or (xd, yd, zd)");
				}

				current_path.addPointDouble(parsed_xd,parsed_yd,parsed_zd);

				int lastIndex = current_path.size() - 1;
				String radiusString = attributes.getValue("r");
				String tXString = attributes.getValue("tx");
				String tYString = attributes.getValue("ty");
				String tZString = attributes.getValue("tz");

				if( radiusString != null &&
				    tXString != null &&
				    tYString != null &&
				    tZString != null ) {
					if( lastIndex == 0 )
						// Then we've just started, create the arrays in Path:
						current_path.createCircles();
					else if( ! current_path.hasCircles() )
						throw new TracesFileFormatException("The point at index " + lastIndex + " had a fitted circle, but none previously did");
					current_path.tangents_x[lastIndex] = Double.parseDouble( tXString );
					current_path.tangents_y[lastIndex] = Double.parseDouble( tYString );
					current_path.tangents_z[lastIndex] = Double.parseDouble( tZString );
					current_path.radiuses[lastIndex] = Double.parseDouble( radiusString );
				} else if( radiusString != null ||
					   tXString != null ||
					   tYString != null ||
					   tZString != null )
					throw new TracesFileFormatException("If one of the r, tx, ty or tz attributes to the point element is specified, they all must be");
				else {
					// All circle attributes are null:
					if( current_path.hasCircles() )
						throw new TracesFileFormatException("The point at index " + lastIndex + " had no fitted circle, but all previously did");
				}

			} catch( NumberFormatException e ) {
				throw new TracesFileFormatException("There was an invalid attribute to <imagesize/>");
			}

		} else if( qName.equals("fill") ) {

			try {

				String [] sourcePaths = { };
				String fromPathsString = attributes.getValue("frompaths");
				if( fromPathsString != null )
					sourcePaths = fromPathsString.split(", *");

				current_fill = new Fill();

				String metric = attributes.getValue("metric");
				current_fill.setMetric(metric);

				last_fill_node_id = -1;

				String fill_id_string = attributes.getValue("id");

				int fill_id = Integer.parseInt(fill_id_string);

				if( fill_id < 0 ) {
					throw new TracesFileFormatException("Can't have a negative id in <fill>");
				}

				if( fill_id != (last_fill_id + 1) ) {
					IJ.log("Out of order id in <fill> (" + fill_id +
					       " when we were expecting " + (last_fill_id + 1) + ")");
					fill_id = last_fill_id + 1;
				}

				int [] sourcePathIndices = new int[ sourcePaths.length ];

				for( int i = 0; i < sourcePaths.length; ++i )
					sourcePathIndices[i] = Integer.parseInt(sourcePaths[i]);

				sourcePathIDForFills.add( sourcePathIndices );

				last_fill_id = fill_id;

				String thresholdString = attributes.getValue("threshold");
				double fillThreshold = Double.parseDouble(thresholdString);

				current_fill.setThreshold(fillThreshold);

			} catch( NumberFormatException e ) {
				throw new TracesFileFormatException("There was an invalid attribute to <fill>");
			}


		} else if( qName.equals("node") ) {

			try {

				String xString = attributes.getValue("x");
				String yString = attributes.getValue("y");
				String zString = attributes.getValue("z");
				String idString = attributes.getValue("id");
				String distanceString = attributes.getValue("distance");
				String previousString = attributes.getValue("previousid");

				int parsed_x = Integer.parseInt(xString);
				int parsed_y = Integer.parseInt(yString);
				int parsed_z = Integer.parseInt(zString);
				int parsed_id = Integer.parseInt(idString);
				double parsed_distance = Double.parseDouble(distanceString);
				int parsed_previous;
				if( previousString == null )
					parsed_previous = -1;
				else
					parsed_previous = Integer.parseInt(previousString);

				if( parsed_id != (last_fill_node_id + 1) ) {
					throw new TracesFileFormatException("Fill node IDs weren't consecutive integers");
				}

				String openString = attributes.getValue("status");

				current_fill.add( parsed_x,
						  parsed_y,
						  parsed_z,
						  parsed_distance,
						  parsed_previous,
						  openString.equals("open") );

				last_fill_node_id = parsed_id;

			} catch( NumberFormatException e ) {
				throw new TracesFileFormatException("There was an invalid attribute to <node/>: "+e);
			}

		} else {
			throw new TracesFileFormatException("Unknown element: '"+qName+"'");
		}

	}

	public void addTo3DViewer( Path p ) {
		if( plugin != null && plugin.use3DViewer && p.fittedVersionOf == null && p.size() > 1 ) {
			Path pathToAdd;
			if( p.getUseFitted() )
				pathToAdd = p.fitted;
			else
				pathToAdd = p;
			pathToAdd.addTo3DViewer(plugin.univ,plugin.deselectedColor,plugin.colorImage);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws TracesFileFormatException {

		if( qName.equals("path") ) {

			allPaths.add( current_path );

		} else if( qName.equals("fill") ) {

			allFills.add( current_fill );

		} else if( qName.equals("tracings") ) {

			// Then we've finished...

			for( int i = 0; i < allPaths.size(); ++i ) {
				Path p = allPaths.get(i);

				Integer startID = startJoins.get(p.getID());
				Integer startIndexInteger = startJoinsIndices.get(p.getID());
				PointInImage startJoinPoint = startJoinsPoints.get(p.getID());
				Integer endID = endJoins.get(p.getID());
				Integer endIndexInteger = endJoinsIndices.get(p.getID());
				PointInImage endJoinPoint = endJoinsPoints.get(p.getID());
				Integer fittedID = fittedFields.get(p.getID());
				Integer fittedVersionOfID = fittedVersionOfFields.get(p.getID());
				Boolean useFitted = useFittedFields.get(p.getID());

				if( startID != null ) {
					Path startPath = getPathFromID(startID);
					if( startJoinPoint == null ) {
						// Then we have to get it from startIndexInteger:
						startJoinPoint = startPath.getPointInImage(startIndexInteger.intValue());
					}
					p.setStartJoin( startPath, startJoinPoint );
				}
				if( endID != null ) {
					Path endPath = getPathFromID(endID);
					if( endJoinPoint == null ) {
						// Then we have to get it from endIndexInteger:
						endJoinPoint = endPath.getPointInImage(endIndexInteger.intValue());
					}
					p.setEndJoin( endPath, endJoinPoint );
				}
				if( fittedID != null ) {
					Path fitted = getPathFromID(fittedID);
					p.fitted = fitted;
					p.setUseFitted(useFitted.booleanValue());
				}
				if( fittedVersionOfID != null ) {
					Path fittedVersionOf = getPathFromID(fittedVersionOfID);
					p.fittedVersionOf = fittedVersionOf;
				}
			}

			// Do some checks that the fitted and fittedVersionOf fields match up:
			for( int i = 0; i < allPaths.size(); ++i ) {
				Path p = allPaths.get(i);
				if( p.fitted != null ) {
					if( p.fitted.fittedVersionOf == null )
						throw new TracesFileFormatException("Malformed traces file: p.fitted.fittedVersionOf was null");
					else if( p != p.fitted.fittedVersionOf )
						throw new TracesFileFormatException("Malformed traces file: p didn't match p.fitted.fittedVersionOf");
				} else if( p.fittedVersionOf != null ) {
					if( p.fittedVersionOf.fitted == null )
						throw new TracesFileFormatException("Malformed traces file: p.fittedVersionOf.fitted was null");
					else if( p != p.fittedVersionOf.fitted )
						throw new TracesFileFormatException("Malformed traces file: p didn't match p.fittedVersionOf.fitted");
				}
				if( p.useFitted && p.fitted == null ) {
					throw new TracesFileFormatException("Malformed traces file: p.useFitted was true but p.fitted was null");
				}
			}

			// Now we're safe to add them all to the 3D Viewer
			for( int i = 0; i < allPaths.size(); ++i ) {
				Path p = allPaths.get(i);
				addTo3DViewer( p );
			}

			// Now turn the source paths into real paths...
			for( int i = 0; i < allFills.size(); ++i ) {
				Fill f = allFills.get(i);
				Set<Path> realSourcePaths = new HashSet<Path>();
				int [] sourcePathIDs = sourcePathIDForFills.get(i);
				for( int j = 0; j < sourcePathIDs.length; ++j ) {
					Path sourcePath = getPathFromID(sourcePathIDs[j]);
					if( sourcePath != null )
						realSourcePaths.add( sourcePath );
				}
				f.setSourcePaths( realSourcePaths );
			}

			setSelected( new Path[0], this );
			resetListeners( null, true );
			if( plugin != null )
				plugin.repaintAllPanes();
		}

	}

	public static PathAndFillManager createFromTracesFile( String filename ) {
		PathAndFillManager pafm = new PathAndFillManager();
		if( pafm.loadGuessingType(filename) )
			return pafm;
		else
			return null;
	}

	public boolean loadFromString( String tracesFileAsString ) {

		StringReader reader=new StringReader(tracesFileAsString);
		boolean result = load(null,reader);
		reader.close();
		return result;

	}

	public boolean load( InputStream is, Reader reader ) {

		try {

			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(true);
			SAXParser parser = factory.newSAXParser();

			if( is != null )
				parser.parse( is, this );
			else if( reader != null ) {
				InputSource inputSource=new InputSource(reader);
				parser.parse( inputSource, this );
			}

			// We must have got the image data if we've got to this stage...
			needImageDataFromTracesFile = false;

		} catch( javax.xml.parsers.ParserConfigurationException e ) {

			clearPathsAndFills();
			IJ.error("There was a ParserConfigurationException: "+e);
			return false;

		} catch( SAXException e ) {

			clearPathsAndFills();
			IJ.error(e.toString());
			return false;

		} catch( FileNotFoundException e ) {

			clearPathsAndFills();
			IJ.error("File not found: "+e);
			return false;

		} catch( IOException e ) {

			clearPathsAndFills();
			IJ.error("There was an IO exception while reading the file: "+e);
			return false;

		}

		return true;

	}

	void clearPathsAndFills( ) {
		maxUsedID = -1;
		if( plugin != null && plugin.use3DViewer ) {
			for( Path p : allPaths )
				p.removeFrom3DViewer( plugin.univ );
		}
		allPaths.clear();
		allFills.clear();
		resetListeners( null );
	}

	/* The two useful documents about the SWC file formats are:

	   doi:10.1016/S0165-0270(98)00091-0
	   http://linkinghub.elsevier.com/retrieve/pii/S0165027098000910
	   J Neurosci Methods. 1998 Oct 1;84(1-2):49-54.Links
	   "An on-line archive of reconstructed hippocampal neurons."
	   Cannon RC, Turner DA, Pyapali GK, Wheal HV.

	   http://www.personal.soton.ac.uk/dales/morpho/morpho_doc/index.html

	   Annoyingly, some published SWC files use world coordinates
	   in microns (correct as I understand the specification)
	   while some others use image coordinates (incorrect and less
	   useful).  An example of the latter seems to part of the
	   DIADEM Challenge data set.

	   There aren't any really good workarounds for this, since if
	   we try to guess whether the files are broken or not, there
	   are always going to be odd cases where the heuristics fail.
	   In addition, it's not at all clear what the "radius" column
	   is meant to mean in these files.

	   So, the extent to which I'm going to work around these
	   broken files is that there's a flag to this method which
	   says "assume that the coordinates are image coordinates".
	   The broken files also seem to require that you scale the
	   radius by the minimum voxel separation (!) so that flag
	   also turns on that workaround.
	*/

	public boolean importSWC( BufferedReader br, boolean assumeCoordinatesIndexVoxels ) throws IOException {
		return importSWC( br, assumeCoordinatesIndexVoxels, 0, 0, 0, 1, 1, 1, true );
	}

	public boolean importSWC( BufferedReader br, boolean assumeCoordinatesIndexVoxels,
				  double x_offset, double y_offset, double z_offset,
				  double x_scale, double y_scale, double z_scale,
				  boolean replaceAllPaths ) throws IOException {

		if( needImageDataFromTracesFile )
			throw new RuntimeException( "[BUG] Trying to load SWC file while we still need image data information" );

		if( replaceAllPaths )
			clearPathsAndFills( );

		Pattern pEmpty = Pattern.compile("^\\s*$");
		Pattern pComment = Pattern.compile("^([^#]*)#.*$");

		Set< Integer > alreadySeen = new HashSet< Integer >();
		Map< Integer, SWCPoint > idToSWCPoint = new HashMap< Integer, SWCPoint >();

		List<SWCPoint> primaryPoints = new ArrayList<SWCPoint>();

		/* Some SWC files I've tried use world co-ordinates
		   (good) but some seem to have the sign wrong, so
		   calculate what should be the minimum and maximum
		   value in each axis so we can test for this later. */

		double minX = Math.min( 0, width * x_spacing );
		double minY = Math.min( 0, height * y_spacing );
		double minZ = Math.min( 0, depth * z_spacing );

		double maxX = Math.max( 0, width * x_spacing );
		double maxY = Math.max( 0, height * y_spacing );
		double maxZ = Math.max( 0, depth * z_spacing );

		double minimumVoxelSpacing = Math.min(Math.abs(x_spacing),Math.min(Math.abs(y_spacing),Math.abs(z_spacing)));

		int pointsOutsideImageRange = 0;

		String line;
		while( (line = br.readLine()) != null ) {
			Matcher mComment = pComment.matcher(line);
			line = mComment.replaceAll("$1");
			Matcher mEmpty = pEmpty.matcher(line);
			if( mEmpty.matches() )
				continue;
			String [] fields = line.split("\\s+");
			if( fields.length != 7 ) {
				IJ.error("Wrong number of fields ("+fields.length+") in line: "+line);
				return false;
			}
			try {
				int id = Integer.parseInt(fields[0]);
				int type = Integer.parseInt(fields[1]);
				double x = x_scale * Double.parseDouble(fields[2]) + x_offset;
				double y = y_scale * Double.parseDouble(fields[3]) + y_offset;
				double z = z_scale * Double.parseDouble(fields[4]) + z_offset;
				if( assumeCoordinatesIndexVoxels ) {
					x *= x_spacing;
					y *= y_spacing;
					z *= z_spacing;
				}
				double radius = Double.parseDouble(fields[5]);
				if( assumeCoordinatesIndexVoxels ) {
					/* See the comment above; this just seems to be the
					   convention in the broken files that I've come across: */
					radius *= minimumVoxelSpacing;
				}

				/* If the radius is set to near zero,
				   then artificially set it to half of
				   the voxel spacing so that
				   *something* appears in the 3D Viewer */

				if( Math.abs(radius) < 0.0000001 )
					radius = minimumVoxelSpacing / 2;

				int previous = Integer.parseInt(fields[6]);
				if( alreadySeen.contains(id) ) {
					IJ.error("Point with ID "+id+" found more than once");
					return false;
				}
				alreadySeen.add( id );

				if( x < minX || x > maxX )
					++ pointsOutsideImageRange;
				if( y < minY || y > maxY )
					++ pointsOutsideImageRange;
				if( z < minZ || z > maxZ )
					++ pointsOutsideImageRange;

				SWCPoint p = new SWCPoint( id, type, x, y, z, radius, previous );
				idToSWCPoint.put( id, p );
				if( previous == -1 )
					primaryPoints.add( p );
				else {
					SWCPoint previousPoint = idToSWCPoint.get( previous );
					p.previousPoint = previousPoint;
					previousPoint.addNextPoint( p );
				}
			} catch( NumberFormatException nfe ) {
				IJ.error( "There was a malformed number in line: "+line );
				return false;
			}
		}

		if( pointsOutsideImageRange > 0 )
			IJ.log("Warning: "+pointsOutsideImageRange+" points were outside the image volume - you may need to change your SWC import options");

		HashMap< SWCPoint, Path > pointToPath =
			new HashMap< SWCPoint, Path >();

		PriorityQueue< SWCPoint > backtrackTo =
			new PriorityQueue< SWCPoint >();

		for( SWCPoint start : primaryPoints )
			backtrackTo.add( start );

		HashMap< Path, SWCPoint > pathStartsOnSWCPoint =
			new HashMap< Path, SWCPoint >();
		HashMap< Path, PointInImage > pathStartsAtPointInImage =
			new HashMap< Path, PointInImage >();

		SWCPoint start;
		Path currentPath;
		while( (start = backtrackTo.poll()) != null ) {
			currentPath = new Path( x_spacing, y_spacing, z_spacing, spacing_units );
			currentPath.createCircles();
			int added = 0;
			if( start.previousPoint != null ) {
				SWCPoint beforeStart = start.previousPoint;
				pathStartsOnSWCPoint.put( currentPath, beforeStart );
				pathStartsAtPointInImage.put( currentPath, beforeStart.getPointInImage() );
				currentPath.addPointDouble( beforeStart.x,
							    beforeStart.y,
							    beforeStart.z );
				currentPath.radiuses[added] = beforeStart.radius;
				++ added;

			}
			// Now we can start adding points to the path:
			SWCPoint currentPoint = start;
			while( currentPoint != null ) {
				currentPath.addPointDouble( currentPoint.x,
							    currentPoint.y,
							    currentPoint.z );
				currentPath.radiuses[added] = currentPoint.radius;
				++ added;
				pointToPath.put( currentPoint, currentPath );
				/* Remove each one from "alreadySeen"
				   when we add it to a path, just to
				   check that nothing's left at the
				   end, which indicates that the file
				   is malformed. */
				alreadySeen.remove( currentPoint.id );
				if( currentPoint.nextPoints.size() > 0 ) {
					SWCPoint newCurrentPoint = currentPoint.nextPoints.get(0);
					currentPoint.nextPoints.remove(0);
					for( int i = 0; i < currentPoint.nextPoints.size(); ++i ) {
						SWCPoint pointToQueue = currentPoint.nextPoints.get(i);
						backtrackTo.add( pointToQueue );
					}
					currentPoint = newCurrentPoint;
				} else
					currentPoint = null;
			}
			currentPath.setGuessedTangents( 2 );
			addPath( currentPath );
		}

		if( alreadySeen.size() > 0 ) {
			IJ.error( "Malformed file: there are some misconnected points" );
			for( int i : alreadySeen ) {
				SWCPoint p = idToSWCPoint.get( i );
				System.out.println( "  Misconnected: " + p);
			}
			return false;
		}

		// Set the start joins:
		for( Path p : allPaths ) {
			SWCPoint swcPoint = pathStartsOnSWCPoint.get( p );
			if( swcPoint == null )
				continue;
			Path previousPath = pointToPath.get(swcPoint);
			PointInImage pointInImage = pathStartsAtPointInImage.get( p );
			p.setStartJoin( previousPath, pointInImage );
		}

		resetListeners( null, true );
		return true;
	}

	public boolean importSWC( String filename, boolean ignoreCalibration ) {
		return importSWC( filename, ignoreCalibration, 0, 0, 0, 1, 1, 1, true );
	}

	public boolean importSWC( String filename, boolean ignoreCalibration,
				  double x_offset, double y_offset, double z_offset,
				  double x_scale, double y_scale, double z_scale,
				  boolean replaceAllPaths ) {

		File f = new File(filename);
		if( ! f.exists() ) {
			IJ.error("The traces file '"+filename+"' does not exist.");
			return false;
		}

		InputStream is = null;
		boolean result = false;

		try {

			is = new BufferedInputStream(new FileInputStream(filename));
			BufferedReader br = new BufferedReader(new InputStreamReader(is,"UTF-8"));

			result = importSWC(br,ignoreCalibration,x_offset,y_offset,z_offset,x_scale,y_scale,z_scale,replaceAllPaths);

			if( is != null )
				is.close();

		} catch( IOException ioe ) {
			IJ.error("Couldn't open file '"+filename+"' for reading.");
			return false;
		}

		return result;


	}

	public static final int TRACES_FILE_TYPE_COMPRESSED_XML = 1;
	public static final int TRACES_FILE_TYPE_UNCOMPRESSED_XML = 2;
	public static final int TRACES_FILE_TYPE_SWC = 3;

	public static int guessTracesFileType( String filename ) {

		/* Look at the magic bytes at the start of the file:

		   If this looks as if it's gzip compressed, assume
		   it's a compressed traces file - the native format
		   of this plugin.

		   If it begins "<?xml", assume it's an uncompressed
		   traces file.

		   Otherwise, assum it's an SWC file.
		*/

		File f = new File(filename);
		if( ! f.exists() ) {
			IJ.error("The traces file '"+filename+"' does not exist.");
			return -1;
		}

		try {
			InputStream is;
			byte[] buf = new byte[8];
			is = new FileInputStream(filename);
			is.read(buf, 0, 8);
			is.close();
			if (verbose) System.out.println("buf[0]: "+buf[0]+", buf[1]: "+buf[1]);
			if( ((buf[0]&0xFF) == 0x1F) && ((buf[1]&0xFF) == 0x8B) )
				return TRACES_FILE_TYPE_COMPRESSED_XML;
			else if( ((buf[0] == '<') && (buf[1] == '?') &&
				  (buf[2] == 'x') && (buf[3] == 'm') &&
				  (buf[4] == 'l') && (buf[5] == ' ')) )
				return TRACES_FILE_TYPE_UNCOMPRESSED_XML;

		} catch (IOException e) {
			IJ.error("Couldn't read from file: "+filename);
			return -1;
		}

		return TRACES_FILE_TYPE_SWC;
	}

	public boolean loadCompressedXML( String filename ) {
		try {
			if (verbose) System.out.println("Loading gzipped file...");
			return load( new GZIPInputStream(new BufferedInputStream(new FileInputStream(filename))), null );
		} catch( IOException ioe ) {
			IJ.error("Couldn't open file '"+filename+"' for reading\n(n.b. it was expected to be compressed XML)");
			return false;
		}
	}

	public boolean loadUncompressedXML( String filename ) {
		try {
			if (verbose) System.out.println("Loading uncompressed file...");
			return load( new BufferedInputStream(new FileInputStream(filename)), null );
		} catch( IOException ioe ) {
			IJ.error("Couldn't open file '"+filename+"' for reading\n(n.b. it was expected to be XML)");
			return false;
		}
	}

	public boolean loadGuessingType( String filename ) {

		int guessedType = guessTracesFileType( filename );
		switch( guessedType ) {

		case TRACES_FILE_TYPE_COMPRESSED_XML:
			return loadCompressedXML(filename);
		case TRACES_FILE_TYPE_UNCOMPRESSED_XML:
			return loadUncompressedXML(filename);
		case TRACES_FILE_TYPE_SWC:
			return importSWC( filename, false, 0, 0, 0, 1, 1, 1, true );
		default:
			IJ.error("guessTracesFileType() return an unknown type"+guessedType);
			return false;
		}
	}

	/* This method will set all the points in array that
	 * correspond to points on one of the paths to 255, leaving
	 * everything else as it is.  This is useful for creating
	 * stacks that can be used in skeleton analysis plugins that
	 * expect a stack of this kind. */

	synchronized void setPathPointsInVolume( byte [][] slices, int width, int height, int depth ) {
		for( Path topologyPath : allPaths ) {
			Path p = topologyPath;
			if( topologyPath.getUseFitted() ) {
				p = topologyPath.fitted;
			}
			if( topologyPath.fittedVersionOf != null )
				continue;

			int n = p.size();

			ArrayList<Bresenham3D.IntegerPoint> pointsToJoin = new ArrayList<Bresenham3D.IntegerPoint>();

			if( p.startJoins != null ) {
				PointInImage s = p.startJoinsPoint;
				Path sp = p.startJoins;
				int spi = sp.indexNearestTo(s.x, s.y, s.z);
				pointsToJoin.add(new Bresenham3D.IntegerPoint(
						sp.getXUnscaled(spi),
						sp.getYUnscaled(spi),
						sp.getZUnscaled(spi)));
			}

			for( int i = 0; i < n; ++i ) {
				pointsToJoin.add(new Bresenham3D.IntegerPoint(
						p.getXUnscaled(i),
						p.getYUnscaled(i),
						p.getZUnscaled(i)));
			}

			if( p.endJoins != null ) {
				PointInImage s = p.endJoinsPoint;
				Path sp = p.endJoins;
				int spi = sp.indexNearestTo(s.x, s.y, s.z);
				pointsToJoin.add(new Bresenham3D.IntegerPoint(
						sp.getXUnscaled(spi),
						sp.getYUnscaled(spi),
						sp.getZUnscaled(spi)));
			}

			Bresenham3D.IntegerPoint previous = null;
			for( Bresenham3D.IntegerPoint current : pointsToJoin ) {
				if( previous == null ) {
					previous = current;
					continue;
				}

				/* If we don't actually need to draw a line,
				 * just put a point: */
				if( current.diagonallyAdjacentOrEqual(previous) ) {
					slices[current.z][current.y * width + current.x] = (byte)255;
				} else {
					/* Otherwise draw a line with the 3D version
					 * of Bresenham's algorithm:
					 */
					List<Bresenham3D.IntegerPoint> pointsToDraw =
						Bresenham3D.bresenham3D(previous, current);
					for( Bresenham3D.IntegerPoint ip : pointsToDraw ) {
						slices[ip.z][ip.y * width + ip.x] = (byte)255;
					}
				}

				previous = current;
			}
		}
	}

	synchronized PointInImage nearestJoinPointOnSelectedPaths( double x, double y, double z ) {

		PointInImage result = null;

		double minimumDistanceSquared = Double.MAX_VALUE;

		int paths = allPaths.size();

		for( int s = 0; s < paths; ++s ) {

			Path p = allPaths.get(s);

			if( ! selectedPathsSet.contains(p) )
				continue;

			if( 0 == p.size() )
				continue;

			int i = p.indexNearestTo( x * x_spacing,
						  y * y_spacing,
						  z * z_spacing );

			PointInImage nearestOnPath = p.getPointInImage( i );

			double distanceSquared = nearestOnPath.distanceSquaredTo(
				x * x_spacing,
				y * y_spacing,
				z * z_spacing );

			if( distanceSquared < minimumDistanceSquared ) {
				result = nearestOnPath;
				minimumDistanceSquared = distanceSquared;
			}
		}

		return result;
	}

	@Deprecated
	ArrayList<Path> getAllPaths() {
		return allPaths;
	}

	// Methods we need to implement for UniverseListener:
	public void transformationStarted(View view) { }
	public void transformationUpdated(View view) { }
	public void transformationFinished(View view) { }
	public void contentAdded(Content c) { }
	public void contentRemoved(Content c) { }
	public void contentChanged(Content c) { }
	public void contentSelected(Content c) { }
	public void canvasResized() { }
	public void universeClosed() {
		plugin.use3DViewer = false;
	}
	// ... end of methods for UniverseListener

	public NearPoint nearestPointOnAnyPath( double x, double y, double z, double distanceLimit ) {

		/* Order all points in all paths by their euclidean
		   distance to (x,y,z): */

		PriorityQueue< NearPoint > pq = new PriorityQueue< NearPoint >();

		for( Path path : allPaths ) {
			if( ! path.versionInUse() )
				continue;
			for( int j = 0; j < path.size(); ++j ) {
				pq.add( new NearPoint( x, y, z, path, j ) );
			}
		}

		while( true ) {

			NearPoint np = pq.poll();
			if( np == null )
				return null;

			/* Don't bother looking at points that are
			   more than distanceLimit away.  Since we get
			   them in the order closest to furthest away,
			   if we exceed this limit returned: */

			if( np.distanceToPathPointSquared() > (distanceLimit * distanceLimit) )
				return null;

			double distanceToPath = np.distanceToPathNearPoint();
			if( distanceToPath >= 0 )
				return np;
		}
	}

	public AllPointsIterator allPointsIterator() {
		return new AllPointsIterator();
	}

	/* Note that this returns the number of points in th
	   currently in-use version of each path. */

	public int pointsInAllPaths( ) {
		AllPointsIterator a = allPointsIterator();
		int points = 0;
		while( a.hasNext() ) {
			a.next();
			++ points;
		}
		return points;
	}

	public class AllPointsIterator implements Iterator<PointInImage> {

		public AllPointsIterator() {
			numberOfPaths = allPaths.size();
			currentPath = null;
			currentPathIndex = -1;
			currentPointIndex = -1;
		}

		int numberOfPaths;
		// These should all be set to be appropriate to the
		// last point that was returned:
		Path currentPath;
		int currentPathIndex;
		int currentPointIndex;

		public boolean hasNext() {
			if( currentPath == null || currentPointIndex == currentPath.points - 1 ) {
				/* Find out if there is a non-empty
				   path after this: */
				int tmpPathIndex = currentPathIndex + 1;
				while( tmpPathIndex < numberOfPaths ) {
					Path p = allPaths.get( tmpPathIndex );
					if( p.size() > 0 && p.versionInUse() )
						return true;
					++tmpPathIndex;
				}
				return false;
			}
			/* So we know that there's a current path and
			   we're not at the end of it, so there must
			   be another point: */
			return true;
		}

		public PointInImage next() {
			if( currentPath == null || currentPointIndex == currentPath.points - 1 ) {
				currentPointIndex = 0;
				/* Move to the next non-empty path: */
				while( true ) {
					++ currentPathIndex;
					if( currentPathIndex == numberOfPaths )
						throw new java.util.NoSuchElementException();
					currentPath = allPaths.get( currentPathIndex );
					if( currentPath.size() > 0 && currentPath.versionInUse() )
						break;
				}
			} else
				++ currentPointIndex;
			return currentPath.getPointInImage(currentPointIndex);
		}

		public void remove() {
			throw new UnsupportedOperationException("AllPointsIterator does not allow the removal of points");
		}

	}

	/* For each point in *this* PathAndFillManager, find the
	   corresponding point on the other one.  If there's no
	   corresponding one, include a null instead. */

	public ArrayList< NearPoint > getCorrespondences( PathAndFillManager other, double maxDistance ) {

		ArrayList< NearPoint > result = new ArrayList< NearPoint >();

		AllPointsIterator i = allPointsIterator();
		while( i.hasNext() ) {
			PointInImage p = i.next();
			NearPoint np = other.nearestPointOnAnyPath(
				p.x,
				p.y,
				p.z,
				maxDistance );
			result.add(np);
		}
		return result;
	}

	public static String stringForCSV( String s ) {
		boolean quote = false;
		String result = s;
		if( s.indexOf(',') >= 0 )
			quote = true;
		if( s.indexOf('"') >= 0 ) {
			quote = true;
			result = s.replaceAll("\"","\"\"");
		}
		if( quote )
			return "\"" + result + "\"";
		else
			return result;
	}



	public static void csvQuoteAndPrint(PrintWriter pw, Object o) {
		pw.print(PathAndFillManager.stringForCSV(""+o));
	}

	public void exportFillsAsCSV( File outputFile ) throws IOException {

			String [] headers = new String[]{ "FillID",
							  "SourcePaths",
							  "Threshold",
							  "Metric",
							  "Volume",
							  "LengthUnits" };

			PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile.getAbsolutePath()),"UTF-8"));
			int columns = headers.length;
			for( int c = 0; c < columns; ++c ) {
				csvQuoteAndPrint(pw,headers[c]);
				if( c < (columns - 1) )
					pw.print(",");
			}
			pw.print("\r\n");
			for( int i = 0; i < allFills.size(); ++i ) {
				Fill f = allFills.get(i);
				csvQuoteAndPrint(pw,i);
				pw.print(",");
				csvQuoteAndPrint(pw,f.getSourcePathsStringMachine());
				pw.print(",");
				csvQuoteAndPrint(pw,f.getThreshold());
				pw.print(",");
				csvQuoteAndPrint(pw,f.getMetric());
				pw.print(",");
				csvQuoteAndPrint(pw,f.getVolume());
				pw.print(",");
				csvQuoteAndPrint(pw,f.spacing_units);
				pw.print("\r\n");
			}
			pw.close();
	}

	/* Output some potentially useful information about the paths
	   as a CSV (comma separated values) file. */

	public void exportToCSV( File outputFile ) throws IOException {
		// FIXME: also add statistics on volumes of fills and
		// reconstructions...
		String [] headers = { "PathID",
				      "PathName",
				      "PrimaryPath",
				      "PathLength",
				      "PathLengthUnits",
				      "StartsOnPath",
				      "EndsOnPath",
				      "ConnectedPathIDs",
				      "ChildPathIDs",
				      "StartX",
				      "StartY",
				      "StartZ",
				      "EndX",
				      "EndY",
				      "EndZ",
				      "ApproximateFittedVolume" };

		Path [] primaryPaths = getPathsStructured();
		HashSet<Path> h = new HashSet<Path>();
		for( int i = 0; i < primaryPaths.length; ++i )
			h.add(primaryPaths[i]);

		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile.getAbsolutePath()),"UTF-8"));
		int columns = headers.length;
		for( int c = 0; c < columns; ++c ) {
			pw.print(stringForCSV(headers[c]));
			if( c < (columns - 1) )
				pw.print(",");
		}
		pw.print("\r\n");
		for( Path p : allPaths ) {
			Path pForLengthAndName = p;
			if( p.getUseFitted() ) {
				pForLengthAndName = p.fitted;
			}
			if( p.fittedVersionOf != null )
				continue;
			pw.print(stringForCSV(""+p.getID()));
			pw.print(",");
			pw.print(stringForCSV(""+pForLengthAndName.getName()));
			pw.print(",");
			boolean primary = h.contains(p);
			pw.print(stringForCSV(""+primary));
			pw.print(",");
			pw.print(stringForCSV(""+pForLengthAndName.getRealLength()));
			pw.print(",");
			pw.print(stringForCSV(""+p.spacing_units));
			pw.print(",");
			if( p.startJoins != null )
				pw.print(""+p.startJoins.getID());
			pw.print(",");
			if( p.endJoins != null )
				pw.print(""+p.endJoins.getID());
			pw.print(",");
			pw.print(stringForCSV(p.somehowJoinsAsString()));
			pw.print(",");
			pw.print(stringForCSV(p.childrenAsString()));
			pw.print(",");

			double [] startPoint = new double[3];
			double [] endPoint = new double[3];

			pForLengthAndName.getPointDouble(0,startPoint);
			pForLengthAndName.getPointDouble(pForLengthAndName.size()-1,endPoint);

			pw.print(""+startPoint[0]);
			pw.print(",");
			pw.print(""+startPoint[1]);
			pw.print(",");
			pw.print(""+startPoint[2]);
			pw.print(",");
			pw.print(""+endPoint[0]);
			pw.print(",");
			pw.print(""+endPoint[1]);
			pw.print(",");
			pw.print(""+endPoint[2]);

			pw.print(",");
			double fittedVolume = pForLengthAndName.getApproximateFittedVolume();
			if( fittedVolume >= 0 )
				pw.print(fittedVolume);
			else
				pw.print("");

			pw.print("\r\n");
			pw.flush();
		}
		pw.close();
	}

	/* Whatever the state of the paths, update the 3D viewer to
	   make sure that they're the right colour, the right version
	   (fitted or unfitted) is being used, whether the line or
	   surface representation is being used, or whether the path
	   should be displayed at all (it shouldn't if the "Show only
	   selected paths" option is set.) */

	public void update3DViewerContents() {
		if( plugin != null && ! plugin.use3DViewer )
			return;
		boolean showOnlySelectedPaths = plugin.getShowOnlySelectedPaths();
		// Now iterate over all the paths:
		for( Path p : allPaths ) {

			if( p.fittedVersionOf != null )
				continue;

			boolean selected = p.getSelected();

			p.updateContent3D(
				plugin.univ, // The appropriate 3D universe
				(selected || ! showOnlySelectedPaths), // Visible at all?
				plugin.getPaths3DDisplay(), // How to display?
				selected ? plugin.selectedColor3f : plugin.deselectedColor3f,
				plugin.colorImage ); // Colour?
		}
	}

	/** A base class for all the methods we might want to use to
	    transform paths. */

	// Note that this will transform fitted Paths but lose the radiuses

	public PathAndFillManager transformPaths( PathTransformer transformation, ImagePlus templateImage ) {

		double pixelWidth = 1;
		double pixelHeight = 1;
		double pixelDepth = 1;
		String units = "pixels";

		Calibration templateCalibration = templateImage.getCalibration();
		if( templateCalibration != null ) {
			pixelWidth = templateCalibration.pixelWidth;
			pixelHeight = templateCalibration.pixelHeight;
			pixelDepth = templateCalibration.pixelDepth;
			units = templateCalibration.getUnits();
		}

		PathAndFillManager pafmResult = new PathAndFillManager( templateImage.getWidth(),
									templateImage.getHeight(),
									templateImage.getStackSize(),
									(float)pixelWidth,
									(float)pixelHeight,
									(float)pixelDepth,
									units );

		int [] startJoinsIndices = new int[size()];
		int [] endJoinsIndices = new int[size()];

		PointInImage [] startJoinsPoints = new PointInImage[size()];
		PointInImage [] endJoinsPoints = new PointInImage[size()];

		Path [] addedPaths = new Path[size()];

		int i = 0;
		for( Path p : allPaths ) {

			Path startJoin = p.getStartJoins();
			if( startJoin == null ) {
				startJoinsIndices[i] = -1;
				endJoinsPoints[i] = null;
			} else {
				startJoinsIndices[i] = allPaths.indexOf(startJoin);
				PointInImage transformedPoint = p.getStartJoinsPoint().transform( transformation );
				if( transformedPoint.isReal() )
					startJoinsPoints[i] = transformedPoint;
			}

			Path endJoin = p.getEndJoins();
			if( endJoin == null ) {
				endJoinsIndices[i] = -1;
				endJoinsPoints[i] = null;
			} else {
				endJoinsIndices[i] = allPaths.indexOf(endJoin);
				PointInImage transformedPoint = p.getEndJoinsPoint().transform( transformation );
				if( transformedPoint.isReal() )
					endJoinsPoints[i] = transformedPoint;
			}

			Path transformedPath = p.transform( transformation, templateImage, imagePlus );
			if( transformedPath.size() >= 2 ) {
				addedPaths[i] = transformedPath;
				pafmResult.addPath( transformedPath );
			}

			++i;
		}

		for( i = 0; i < size(); ++i ) {
			int si = startJoinsIndices[i];
			int ei = endJoinsIndices[i];
			if( addedPaths[i] != null ) {
				if( si >= 0 && addedPaths[si] != null && startJoinsPoints[i] != null )
					addedPaths[i].setStartJoin( addedPaths[si], startJoinsPoints[i] );
				if( ei >= 0 && addedPaths[ei] != null && endJoinsPoints[i] != null )
					addedPaths[i].setEndJoin( addedPaths[ei], endJoinsPoints[i] );
			}
		}

		return pafmResult;
	}
}
