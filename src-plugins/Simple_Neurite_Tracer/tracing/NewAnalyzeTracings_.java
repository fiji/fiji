/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

import java.awt.*;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.io.*;
import java.util.Enumeration;

import amira.AmiraParameters;

import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;

import vib.transforms.OrderedTransformations;

import util.FileAndChannel;
import landmarks.Bookstein_From_Landmarks;

import util.BatchOpener;
import vib.oldregistration.RegistrationAlgorithm;

class ImagesFromLine {

	String lineName;
	ArrayList<String> baseNames = new ArrayList<String>();

}

public class NewAnalyzeTracings_ implements PlugIn, TraceLoaderListener {

	int [] labelIndices =  { 7,     // mushroom_body_r
				 8,     // mushroom_body_l
				 9,     // ellipsoid_body
				 11,    // noduli
				 10,    // fan_shaped_body
				 12,    // protocerebral_bridge
				 15,    // antennal_lobe_r
				 16 };  // antennal_lobe_l

	class NewGraphNode implements Comparable {
		public NewGraphNode() { }
		public NewGraphNode(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		int x = -1;
		int y = -1;
		int z = -1;
		NewGraphNode linkedTo [] = null;
		@Override
		public boolean equals(Object other) {
			NewGraphNode o=(NewGraphNode)other;
			return x == o.x && y == o.y && z == o.z;
		}
		@Override
		public int hashCode( ) {
			return x + y * (1 << 11) + z * (1 << 22);
		}
		@Override
		public String toString( ) {
			return "("+x+","+y+","+z+")";
		}
		// These members are only used for the search:
		float g = Float.MIN_VALUE;
		float h = Float.MIN_VALUE;
		NewGraphNode previous = null;
		public float distanceTo( NewGraphNode o ) {
			float xdiff = (x - o.x) * spacing_x;
			float ydiff = (y - o.y) * spacing_y;
			float zdiff = (z - o.z) * spacing_y;
			float distSq = xdiff*xdiff + ydiff*ydiff + zdiff*zdiff;
			return (float) Math.sqrt( distSq );
		}
		public void setFrom( NewGraphNode o ) {
			this.x = o.x;
			this.y = o.y;
			this.z = o.z;
			this.linkedTo = o.linkedTo;
		}
		double f() {
			return g + h;
		}
		//@Override
		public int compareTo( Object other ) {
			NewGraphNode n = (NewGraphNode)other;
			return Double.compare( f(), n.f() );
		}
	}

	ArrayList< NewGraphNode > makePath( NewGraphNode lastNode ) {

		// System.out.println( "Trying to return result" );

		ArrayList< NewGraphNode > resultReversed = new ArrayList< NewGraphNode >();
		NewGraphNode p = lastNode;
		do {
			resultReversed.add(p);
			// System.out.println(  "adding "+p.toDotName());
		} while( null != (p = p.previous) );

		ArrayList< NewGraphNode > realResult = new ArrayList< NewGraphNode >();

		for( int i = resultReversed.size() - 1; i >= 0; --i )
			realResult.add( resultReversed.get(i) );

		return realResult;
	}

	class PathWithLength {

		int startNeuropilRegion;
		int endNeuropilRegion;

		public double length;
		public ArrayList< NewGraphNode > path;

		public Path toPath() {
			Path p = new Path( spacing_x, spacing_y, spacing_z, "" /* FIXME: get the real spacing_units */, path.size() );
			for( int i = 0; i < path.size(); ++i ) {
				NewGraphNode n = path.get(i);
				p.addPointDouble( n.x * spacing_x,
						  n.y * spacing_y,
						  n.z * spacing_z );
			}
			return p;
		}
	}

	PathWithLength findPath( NewGraphNode start, int endMaterial ) {

		// System.out.println("Starting path finding:");

		int startMaterial = label_data[start.z][start.y*width+start.x];

		// First reset all the search parameters:
		{
			Collection<NewGraphNode> c = positionToNode.values();
			for( NewGraphNode n : c ) {
				n.g = 0;
				n.h = 0;
				n.previous = null;
			}
		}

		PriorityQueue< NewGraphNode > closed_from_start = new PriorityQueue< NewGraphNode >();
		PriorityQueue< NewGraphNode > open_from_start = new PriorityQueue< NewGraphNode >();

		Hashtable< NewGraphNode, NewGraphNode > open_from_start_hash = new Hashtable< NewGraphNode, NewGraphNode >();
		Hashtable< NewGraphNode, NewGraphNode > closed_from_start_hash = new Hashtable< NewGraphNode, NewGraphNode >();

		start.g = 0;
		start.h = 0;
		start.previous = null;

		// add_node( open_from_start, open_from_start_hash, start );
		open_from_start.add( start );
		open_from_start_hash.put( start, start );

		while( open_from_start.size() > 0 ) {

			// NewGraphNode p = get_highest_priority( open_from_start, open_from_start_hash );

			// System.out.println("Before poll: "+open_from_start_hash.size()+"/"+open_from_start.size());
			NewGraphNode p = open_from_start.poll();
			open_from_start_hash.remove( p );
			// System.out.println("After poll: "+open_from_start_hash.size()+"/"+open_from_start.size());

			// System.out.println( " Got node "+p.toDotName()+" from the queue" );

			int pointMaterial = label_data[p.z][p.y*width+p.x];

			// Has the route from the start found the goal?

			if( pointMaterial == endMaterial ) {
				// System.out.println( "Found the goal! (from start to end)" );
				ArrayList< NewGraphNode > path = makePath(p);
				if( path == null )
					return null;
				else {
					PathWithLength result = new PathWithLength();
					result.path = path;
					result.length = p.g;
					return result;
				}
			}

			// add_node( closed_from_start, closed_from_start_hash, p );
			closed_from_start.add( p );
			closed_from_start_hash.put( p, p );

			// Now look at all the neighbours...

			// System.out.println("linkedTo "+p.linkedTo.length+" neigbours");
			for( int i = 0; i < p.linkedTo.length; ++i ) {

				NewGraphNode neighbour = p.linkedTo[i];
				float distance = p.distanceTo(neighbour);
				if( neighbour.z == 118 || (neighbour.y*width + neighbour.x) == 118 )
					System.out.println("neighbour is: ("+neighbour.x+","+neighbour.y+","+neighbour.z+" and width "+width+" height "+height+" depth "+depth);
				int neighbourMaterial = label_data[neighbour.z][neighbour.y*width+neighbour.x];

				// Ignore this neighbour if it's it's not of the exterior or end material

				if( ! (neighbourMaterial == 0 || neighbourMaterial == endMaterial) )
					continue;

				NewGraphNode newNode = new NewGraphNode();
				newNode.setFrom( neighbour );
				newNode.g = p.g + distance;
				newNode.h = 0;
				newNode.previous = p;

				NewGraphNode foundInClosed = closed_from_start_hash.get(neighbour);

				NewGraphNode foundInOpen = open_from_start_hash.get(neighbour);

				// Is there an exisiting route which is
				// better?  If so, discard this new candidate...

				if( (foundInClosed != null) && (foundInClosed.f() <= newNode.f()) ) {
					// System.out.println( "  Found in closed, but no better.");
					continue;
				}

				if( (foundInOpen != null) && (foundInOpen.f() <= newNode.f()) ) {
					// System.out.println( "  Found in open, but no better.");
					continue;
				}

				if( foundInClosed != null ) {

					// System.out.println("Found in closed and better");

					// remove( closed_from_start, closed_from_start_hash, foundInClosed );
					closed_from_start.remove( foundInClosed );
					closed_from_start_hash.remove( foundInClosed );

					foundInClosed.setFrom( newNode );

					// add_node( open_from_start, open_from_start_hash, foundInClosed );
					open_from_start.add( foundInClosed );
					open_from_start_hash.put( foundInClosed, foundInClosed );

					continue;
				}

				if( foundInOpen != null ) {

					// System.out.println("Found in open and better");

					// remove( open_from_start, open_from_start_hash, foundInOpen );
					open_from_start.remove( foundInOpen );
					open_from_start_hash.remove( foundInOpen );

					foundInOpen.setFrom( newNode );

					// add_node( open_from_start, open_from_start_hash, foundInOpen );
					open_from_start.add( foundInOpen );
					open_from_start_hash.put( foundInOpen, foundInOpen );

					continue;
				}

				// Otherwise we add a new node:

				// System.out.println("  Adding new node to open " + newNode.toDotName() );

				// add_node( open_from_start, open_from_start_hash, newNode );
				open_from_start.add( newNode );
				open_from_start_hash.put( newNode, newNode );
			}
		}

		/* If we get to here then we haven't found a route to the
		   end point. */

		return null;
	}

	public int positionToKey( int x, int y, int z ) {
		return x + y * width + z * width * height;
	}

	int width = -1, height = -1, depth = -1;
	float spacing_x = Float.MIN_VALUE;
	float spacing_y = Float.MIN_VALUE;
	float spacing_z = Float.MIN_VALUE;
	String spacing_units = "";

	ArrayList<NewGraphNode> verticesInObjOrder;
	Hashtable<Integer,NewGraphNode> positionToNode;
	int numberOfVertices = -1;
	ArrayList<ArrayList<NewGraphNode>> links;

	public void gotVertex( int vertexIndex,
			       float x_scaled, float y_scaled, float z_scaled,
			       int x_image, int y_image, int z_image ) {

		if( width < 0 ||
		    height < 0 ||
		    depth < 0 ||
		    spacing_x == Float.MIN_VALUE ||
		    spacing_y == Float.MIN_VALUE ||
		    spacing_z == Float.MIN_VALUE ) {

			throw new RuntimeException("Some metadata was missing from the comments before the first vertex.");
		}

		if( z_image >= depth ) {
			System.out.println("z is too deep:");
			System.out.println("x_scaled: "+x_scaled);
			System.out.println("y_scaled: "+y_scaled);
			System.out.println("z_scaled: "+z_scaled);
			System.out.println("x_image: "+x_image);
			System.out.println("y_image: "+y_image);
			System.out.println("z_image: "+z_image);
			System.out.println("spacing_x: "+spacing_x);
			System.out.println("spacing_y: "+spacing_y);
			System.out.println("spacing_z: "+spacing_z);
		}

		verticesInObjOrder.add( new NewGraphNode( x_image, y_image, z_image ) );
	}

	public void gotLine( int fromVertexIndex, int toVertexIndex ) {
		if( links == null ) {
			numberOfVertices = verticesInObjOrder.size() - 1;
			links = new ArrayList<ArrayList<NewGraphNode>>(numberOfVertices);
			for( int i = 0; i <= numberOfVertices; ++i )
				links.add(new ArrayList<NewGraphNode>());
		}

		ArrayList<NewGraphNode> fromLinks = links.get(fromVertexIndex);
		ArrayList<NewGraphNode> toLinks = links.get(toVertexIndex);

		NewGraphNode toVertex=verticesInObjOrder.get(toVertexIndex);
		NewGraphNode fromVertex=verticesInObjOrder.get(fromVertexIndex);

		if( ! fromLinks.contains( toVertex ) )
			fromLinks.add( toVertex );

		if( ! toLinks.contains( fromVertex ) )
			toLinks.add( fromVertex );
	}

	public void gotWidth( int width ) {
		this.width = width;
	}

	public void gotHeight( int height ) {
		this.height = height;
	}

	public void gotDepth( int depth ) {
		this.depth = depth;
	}

	public void gotSpacingX( float spacing_x ) {
		this.spacing_x = spacing_x;
	}

	public void gotSpacingY( float spacing_y ) {
		this.spacing_y = spacing_y;
	}

	public void gotSpacingZ( float spacing_z ) {
		this.spacing_z = spacing_z;
	}

	// FIXME: call from somewhere to set:
	public void gotSpacingUnits( String spacing_units ) {
		this.spacing_units = spacing_units;
	}

	byte[][] label_data;
	byte[][] registered_label_data;
	String [] materialNames;

	int [] redValues;
	int [] greenValues;
	int [] blueValues;

	int materials;
	AmiraParameters parameters;

	public byte[][] loadRegisteredLabels( String basename ) {

		String originalImageFileName = basename + ".lsm";

		ImagePlus labels = BatchOpener.openFirstChannel(standardBrainLabelsFileName);

		ImageStack labelStack=labels.getStack();
		int templateWidth=labelStack.getWidth();
		int templateHeight=labelStack.getHeight();
		int templateDepth=labelStack.getSize();
		byte[][] new_label_data=new byte[templateDepth][];
		for( int z = 0; z < templateDepth; ++z )
			new_label_data[z] = (byte[])labelStack.getPixels( z + 1 );

		parameters = new AmiraParameters(labels);
		materials = parameters.getMaterialCount();
		materialNames = new String[256];
		materialNameToIndex = new Hashtable< String, Integer >();
		for( int i = 0; i < materials; ++i ) {
			materialNames[i] = parameters.getMaterialName(i);
			materialNameToIndex.put(materialNames[i],new Integer(i));
			System.out.println("Material: "+i+" is "+materialNames[i]);
		}

		redValues = new int[materials];
		greenValues = new int[materials];
		blueValues = new int[materials];

		for( int i=0; i < materials; i++ ) {

			double[] c = parameters.getMaterialColor(i);

			redValues[i] = (int)(255*c[0]);
			greenValues[i] = (int)(255*c[1]);
			blueValues[i] = (int)(255*c[2]);
		}

		byte [][] transformed_label_data = null;

		String registeredLabelFileName = basename + ".registered.labels";

		File registeredLabelFile = new File(registeredLabelFileName);
		if( registeredLabelFile.exists() ) {

			// Then just load it:

			ImagePlus registeredLabels = BatchOpener.openFirstChannel(registeredLabelFileName);
			ImageStack tmpStack = registeredLabels.getStack();

			transformed_label_data = new byte[registeredLabels.getStackSize()][];

			for( int z = 0; z < registeredLabels.getStackSize(); ++z ) {
				transformed_label_data[z] = (byte[]) tmpStack.getPixels(z+1);
			}

			labels.close();

		} else {

			// We have to calculate the registration:

			FileAndChannel fc=new FileAndChannel(originalImageFileName, 0 );
			FileAndChannel standardBrainFC=new FileAndChannel(standardBrainFileName,0);

			Bookstein_From_Landmarks matcher=new Bookstein_From_Landmarks();
			matcher.loadImages(standardBrainFC,fc);
			matcher.generateTransformation();

			transformed_label_data = new byte[depth][width*height];
			RegistrationAlgorithm.ImagePoint imagePoint = new RegistrationAlgorithm.ImagePoint();

			for( int z = 0; z < depth; ++z ) {
				System.out.println("doing slice: "+z);
				for( int y = 0; y < height; ++y ) {
					for( int x = 0; x < width; ++x ) {

						matcher.transformDomainToTemplate( x, y, z, imagePoint );

						int x_in_template=imagePoint.x;
						int y_in_template=imagePoint.y;
						int z_in_template=imagePoint.z;

						int label_value = 0;

						if( z_in_template >= 0 && z_in_template < templateDepth &&
						    y_in_template >= 0 && y_in_template < templateHeight &&
						    x_in_template >= 0 && x_in_template < templateWidth ) {

							label_value = new_label_data[z_in_template][y_in_template*templateWidth+x_in_template]&0xFF;
						}

						transformed_label_data[z][y*width+x] = (byte)label_value;

						if( label_value >= materials ) {
							throw new RuntimeException( "A label value of " + label_value + " was found, which is not a valid material (max " + (materials - 1) + ")" );
						}
					}
				}
			}

			ImageStack stack = new ImageStack(width,height);

			for( int z = 0; z < depth; ++z ) {
				ByteProcessor bp = new ByteProcessor(width,height);
				bp.setPixels(transformed_label_data[z]);
				stack.addSlice("",bp);
			}

			ImagePlus t = new ImagePlus("transformed labels",stack);
			t.getProcessor().setColorModel( labels.getProcessor().getColorModel() );
			boolean saved=new FileSaver(t).saveAsTiffStack(registeredLabelFileName);
			if( ! saved ) {
				throw new RuntimeException("Failed to save registered labels to: "+registeredLabelFileName);
			}
		}

		labels.close();

		return transformed_label_data;
	}

      	public ArrayList<PathWithLength> buildGraph( File tracesObjFile, File labelsFile, File writePathsTo, File writeDotTo ) {

		boolean usePointRegisteredLabels = true;

		String tracesObjFileName = tracesObjFile.getAbsolutePath();
		String labelsFileName = labelsFile.getAbsolutePath();

		/* First load the traces file: */

		// The indices in the .obj begin at 1, so put in a dummy node at 0:

		verticesInObjOrder = new ArrayList<NewGraphNode>();
		verticesInObjOrder.add( new NewGraphNode() );

		System.out.println("Loading traces file: "+tracesObjFileName);

		boolean success = SinglePathsGraph.loadWithListener( tracesObjFileName, this );

		if( ! success ) {
			throw new RuntimeException("Failed to load traces");
		}

		System.out.println("Finished loading: "+(verticesInObjOrder.size()-1)+" vertices found");

		System.out.println("  traces width:"+width);
		System.out.println("  traces height:"+height);
		System.out.println("  traces depth:"+depth);


		long linksBothWays = 0;
		for( int i = 1; i < verticesInObjOrder.size(); ++i ) {
			NewGraphNode v = verticesInObjOrder.get(i);
			ArrayList<NewGraphNode> linkedTo = links.get(i);
			int l = linkedTo.size();
			linksBothWays += l;
			NewGraphNode [] a=new NewGraphNode[l];
			v.linkedTo = linkedTo.toArray(a);
		}

		links = null;

		System.out.println("And set the links in the NewGraphNodes: "+linksBothWays);

		positionToNode = new Hashtable<Integer,NewGraphNode>();

		// Now we want to index by position rather than vertex index:
		boolean first = true;
		for( NewGraphNode n : verticesInObjOrder ) {
			if( first ) { first = false; continue; }
			int k = positionToKey( n.x, n.y, n.z );
			positionToNode.put( k, n );
		}

		verticesInObjOrder = null;

		System.out.println("Added vertices to the hash, now has: "+positionToNode.size()+" entries");

		/* And now the real labels file: */

		ImagePlus labels = BatchOpener.openFirstChannel(labelsFileName);
		if( labels == null )
			throw new RuntimeException("Couldn't open labels file "+labelsFileName);
		ImageStack labelStack=labels.getStack();

		System.out.println("label file width:" +labels.getWidth());
		System.out.println("label file height:" +labels.getHeight());
		System.out.println("label file depth:" +labels.getStackSize());

		label_data=new byte[depth][];
		for( int z = 0; z < depth; ++z )
			label_data[z] = (byte[])labelStack.getPixels( z + 1 );


		if( usePointRegisteredLabels ) {

			/* Now load the registered labels file.  This
			 * registration will be very approximate, so take this
			 * with a pinch of salt... */

			String basename = labelsFileName.substring(0,labelsFileName.lastIndexOf("."));

			registered_label_data = loadRegisteredLabels( basename );

			/* For the moment, only include the mushroom
			   bodies (7 & 8) and the antennal lobes (15 &
			   16) */

			for( int z = 0; z < depth; ++z ) {
				for( int y = 0; y < height; ++y ) {
					for( int x = 0; x < width; ++x ) {

						// Don't copy over anything if there's a real value here:
						if( label_data[z][y*width+x] == 0 ) {
							byte registered_value = registered_label_data[z][y*width+x];
							if( registered_value == 7 ||
							    registered_value == 8 ||
							    registered_value == 15 ||
							    registered_value == 16 ) {
								label_data[z][y*width+x] = registered_label_data[z][y*width+x];
							}
						}
					}
				}
			}

		} else {

			parameters = new AmiraParameters(labels);
			materials = parameters.getMaterialCount();
			materialNames = new String[256];
			materialNameToIndex = new Hashtable< String, Integer >();
			for( int i = 0; i < materials; ++i ) {
				materialNames[i] = parameters.getMaterialName(i);
				materialNameToIndex.put(materialNames[i],new Integer(i));
				System.out.println("Material: "+i+" is "+materialNames[i]);
			}

			redValues = new int[materials];
			greenValues = new int[materials];
			blueValues = new int[materials];

		}

		ArrayList<ArrayList<NewGraphNode>> allEdges = new ArrayList<ArrayList<NewGraphNode>>();

		for( int i=0; i < materials; i++ ) {
			allEdges.add(new ArrayList<NewGraphNode>());
			double[] c = parameters.getMaterialColor(i);
			redValues[i] = (int)(255*c[0]);
			greenValues[i] = (int)(255*c[1]);
			blueValues[i] = (int)(255*c[2]);
		}

/* Find all the points on the edge of a neuropil regions: */

		for( int a = 0; a < labelIndices.length; ++a ) {
			int labelIndex = labelIndices[a];
			String labelName = materialNames[labelIndex];

			System.out.println("   Dealing with label index "+labelIndex+", name: "+labelName);

			ArrayList<NewGraphNode> neuropilEdgePoints = allEdges.get(labelIndex);

			for( int z = 0; z < depth; ++z )
				for( int y = 0; y < height; ++y )
					for( int x = 0; x < width; ++x ) {
						if( label_data[z][y*width+x] != labelIndex )
							continue;
						int k = positionToKey(x,y,z);
						NewGraphNode n = positionToNode.get(k);
						if( n == null )
							continue;
						/* So now we have a traced point in the right neuropil region.
						   We only care about edge points, though, so check that it has
						   a neighbour that's in the exterior. */
						NewGraphNode [] linkedNodes = n.linkedTo;
						for( int i = 0; i < linkedNodes.length; ++i ) {
							NewGraphNode l = linkedNodes[i];
							if( label_data[l.z][l.y*width+l.x] == 0 ) {
								neuropilEdgePoints.add(l);
								break;
							}
						}
					}

			System.out.println("   Found "+neuropilEdgePoints.size()+" points on the edge of the "+labelName);
		}

		// We'll store copies of these in a PathAndFillManager
		// so that we can write out something that will be
		// loadable by the manual tracer afterwards:

		PathAndFillManager manager=new PathAndFillManager( width, height, depth, spacing_x, spacing_y, spacing_z, null );

		ArrayList<PathWithLength> paths=new ArrayList<PathWithLength>();

		// Now start a search from each of these points trying
		// to find an end point at one of the edge points from
		// the other neuropil regions:

		for( int a = 0; a < labelIndices.length; ++a ) {

			int labelIndex = labelIndices[a];
			String labelName = materialNames[labelIndex];
			System.out.println("Starting searches from "+labelIndex+", name: "+labelName);

			ArrayList<NewGraphNode> startPoints = allEdges.get(labelIndex);

			for( int endM = labelIndex + 1; endM < materials; ++endM ) {

				ArrayList<NewGraphNode> potentialEndPoints = allEdges.get(endM);
				if( potentialEndPoints.size() == 0 )
					continue;

				for( NewGraphNode startPoint : startPoints ) {

					System.out.println("  Starting from point "+startPoint+" ("+labelName+" looking for material: "+materialNames[endM]);

					PathWithLength route = findPath( startPoint, endM );
					if( route == null ) {
						// System.out.println("No route found.");
						continue;
					}

					route.startNeuropilRegion = labelIndex;
					route.endNeuropilRegion = endM;

					paths.add(route);
					Path newPath=route.toPath();
					newPath.setName(materialNames[labelIndex]+" to " +materialNames[endM]);
					manager.addPath(newPath);

					// Add that path to the fill manager as well:

					System.out.println("  Found a route!");
				}
			}
		}

		if( writePathsTo != null ) {
			try {
				manager.writeXML( writePathsTo.getAbsolutePath(),
						  true );
			} catch( IOException e ) {
				System.out.println("Writing to: "+writePathsTo+" failed");
			}
		}

		if( writeDotTo != null ) {
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(writeDotTo.getAbsolutePath(),false));

				out.write( "graph G {\n" );
				out.write( "        graph [overlap=scale,splines=true];\n");
				out.write( "        node [fontname=\"DejaVuSans\",style=filled];\n");

				HashSet<Integer> materialsInGraph = new HashSet<Integer>();
				for( PathWithLength p : paths ) {
					materialsInGraph.add(p.startNeuropilRegion);
					materialsInGraph.add(p.endNeuropilRegion);
				}

				for( int m : materialsInGraph ) {
					String name = materialNames[m];
					out.write( "        \"" + name + "\" [fillcolor=\"" + colorString(name) + "\"];\n" );
				}

				HashSet<String> connectionsDone=new HashSet<String>();

				for( PathWithLength p : paths ) {
					String dotLine = "        \"" + materialNames[p.startNeuropilRegion] + "\" -- \"" + materialNames[p.endNeuropilRegion] + "\";\n";
					if( ! connectionsDone.contains(dotLine) ) {
						out.write(dotLine);
						connectionsDone.add(dotLine);
					}
				}
				out.write( "}" );
				out.close();

			} catch( IOException ioe ) {
				IJ.error( "Exception while writing the file" );
			}
		}

		return paths;
	}

	String standardBrainFileName = "/home/mark/arnim-brain/CantonF41c.grey";
	String standardBrainLabelsFileName = "/home/mark/arnim-brain/CantonF41c.labels";

	public void run( String argument ) {

		String baseDirectory = "/media/WD Passport/corpus/central-complex/";
		// String baseDirectory = "/media/WD USB 2/corpus/central-complex/";
		// String baseDirectory = "/home/mark/tmp-corpus/";
		ArrayList<ImagesFromLine> lines = new ArrayList<ImagesFromLine>();

		ImagesFromLine linec5 = new ImagesFromLine();
		linec5.lineName = "c005";
		linec5.baseNames.add("c005BA");
		linec5.baseNames.add("c005BB");
		linec5.baseNames.add("c005BC");
		linec5.baseNames.add("c005BE");
		linec5.baseNames.add("c005BF");
		linec5.baseNames.add("c5xUAS-CD8GFP-40x-central-complex-BF");
		linec5.baseNames.add("c5xUAS-lacZ-40x-cc-BB");
		linec5.baseNames.add("c5xUAS-lacZ-40x-cc-BC");
		lines.add(linec5);

		ImagesFromLine line210y = new ImagesFromLine();
		line210y.lineName = "210y";
		line210y.baseNames.add("210y-40x-central-complex-CA");
		line210y.baseNames.add("210y-40x-central-complex-CB");
		line210y.baseNames.add("210y-40x-central-complex-CD");
		line210y.baseNames.add("210y-40x-central-complex-CE");
		line210y.baseNames.add("210yAC");
		line210y.baseNames.add("210yAD");
		line210y.baseNames.add("210yAE");
		line210y.baseNames.add("210yAO");
		line210y.baseNames.add("210yAP");
		lines.add(line210y);

		ImagesFromLine line71y = new ImagesFromLine();
		line71y.lineName = "71y";
		line71y.baseNames.add("71yAAeastmost");
		line71y.baseNames.add("71yABwestmost");
		line71y.baseNames.add("71yAF");
		line71y.baseNames.add("71yAM");
		line71y.baseNames.add("71yAN");
		line71y.baseNames.add("71yAQ");
		line71y.baseNames.add("71yAR");
		lines.add(line71y);

		ImagesFromLine linec61 = new ImagesFromLine();
		linec61.lineName = "c61";
		linec61.baseNames.add("c061AG");
		linec61.baseNames.add("c061AH");
		linec61.baseNames.add("c061AI()");
		linec61.baseNames.add("c061AK");
		linec61.baseNames.add("c061AL");
		linec61.baseNames.add("c061AU");
		lines.add(linec61);

		for( ImagesFromLine line : lines ) {

			System.out.println("Looking at line: "+line.lineName);

			for( String baseName : line.baseNames ) {

				System.out.println("  Image basename: "+baseName );

				File lsmFile = new File( baseDirectory, baseName + ".lsm" );
				/*
				if( ! lsmFile.exists() )
					continue;
				*/

				File tracesObjFile = new File( baseDirectory, baseName + ".traces.obj" );
				if( ! lsmFile.exists() )
					continue;

				File labelsFile = new File( baseDirectory, baseName + ".labels" );
				if( ! lsmFile.exists() )
					continue;

				System.out.println("!!!");

				// Load labels and traces.obj ...

				ArrayList<PathWithLength> foundPaths = buildGraph(
					tracesObjFile,
					labelsFile,
					new File( baseDirectory, baseName + ".neuropil-connections.traces"),
					new File( baseDirectory, baseName + ".dot" ) );
			}
		}
	}

	public Hashtable< String, Integer > materialNameToIndex;

	public String colorString( String materialName ) {

		if( materialName.equals("Exterior") )
			return "#DDDDDD";
		else {

			Integer material_id_integer = materialNameToIndex.get(materialName);
			int material_id = material_id_integer.intValue();

			double scaling = 1.4;

			int r = (int)( redValues[material_id] * scaling );
			int g = (int)( greenValues[material_id] * scaling );
			int b = (int)( blueValues[material_id] * scaling );

			if( r > 255 )
				r = 255;
			if( g > 255 )
				g = 255;
			if( b > 255 )
				b = 255;

			String redValueString = Integer.toHexString(r);
			if( redValueString.length() <= 1 )
				redValueString = "0" + redValueString;

			String greenValueString = Integer.toHexString(g);
			if( greenValueString.length() <= 1 )
				greenValueString = "0" + greenValueString;

			String blueValueString = Integer.toHexString(b);
			if( blueValueString.length() <= 1 )
				blueValueString = "0" + blueValueString;

			return "#" + redValueString + greenValueString + blueValueString;
		}
	}

}
