/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

import java.awt.*;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.*;
import java.util.Enumeration;

import amira.AmiraParameters;

import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.plugin.PlugIn;

import landmarks.Bookstein_From_Landmarks;

import vib.transforms.OrderedTransformations;
import vib.oldregistration.RegistrationAlgorithm;

import util.FileAndChannel;
import util.BatchOpener;

class PointInPath {

	public PointInPath() {
	}

	private double x;
	private double y;
	private double z;

	private String neuropilRegion;

	public GraphNode node;

	@Override
	public String toString() {
		return "(" + x + "," + y + "," + z + ") [" + neuropilRegion + "]" + (start ? " start" : "") + (end ? " end" : "") + ((node == null) ? " no GraphNode attached" : " GraphNode attached (" + node.id + ")"  );
	}

	void setPosition( double x, double y, double z ) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	void setNeuropilRegion( String neuropilRegion ) {
		this.neuropilRegion = new String( neuropilRegion );
	}

	String getNeuropilRegion( ) {
		return neuropilRegion;
	}

	double getX() {
		return x;
	}

	double getY() {
		return y;
	}

	double getZ() {
		return z;
	}

	boolean start = false;
	boolean end = false;

	void setStart( boolean start ) {
		this.start = start;
	}

	void setEnd( boolean end ) {
		this.end = end;
	}

	boolean start() {
		return start;
	}

	boolean end() {
		return end;
	}

	int path_id = -1;

	void setPathID( int i ) {
		this.path_id = i;
	}

	int getPathID( ) {
		return path_id;
	}

	boolean nearTo( double within, double other_x, double other_y, double other_z ) {
		double xdiff = other_x - x;
		double ydiff = other_y - y;
		double zdiff = other_z - z;
		double distance_squared = xdiff * xdiff + ydiff * ydiff + zdiff * zdiff;
		double within_squared = within * within;
		return distance_squared <= within_squared;
	}
}

public class AnalyzeTracings_ implements PlugIn {

	static public Connectivity buildGraph( String imageFileName, ArrayList< Path > allPaths ) {

		Connectivity result = new Connectivity();

		int x, y, z;

		FileAndChannel fc=new FileAndChannel( imageFileName, 0 );

                String standardBrainFileName="/media/WD USB 2/standard-brain/data/vib-drosophila/CantonM43c.grey";
                String standardBrainLabelsFileName="/media/WD USB 2/standard-brain/data/vib-drosophila/CantonM43c.labels";
                FileAndChannel standardBrainFC=new FileAndChannel(standardBrainFileName,0);

		Bookstein_From_Landmarks matcher=new Bookstein_From_Landmarks();
		matcher.loadImages(standardBrainFC,fc);
		matcher.generateTransformation();

		ImagePlus labels;
		{
			ImagePlus[] tmp=BatchOpener.open(standardBrainLabelsFileName);
			labels=tmp[0];
		}
		System.out.println("   labels were: "+labels);
		ImageStack labelStack=labels.getStack();
		int templateWidth=labelStack.getWidth();
		int templateHeight=labelStack.getHeight();
		int templateDepth=labelStack.getSize();
		byte[][] label_data=new byte[templateDepth][];
		for( z = 0; z < templateDepth; ++z )
			label_data[z] = (byte[])labelStack.getPixels( z + 1 );

		AmiraParameters parameters = new AmiraParameters(labels);
		int materials = parameters.getMaterialCount();
		result.materialNames = new String[256];
		result.materialNameToIndex = new Hashtable< String, Integer >();
		for( int i = 0; i < materials; ++i ) {
			result.materialNames[i] = parameters.getMaterialName(i);
			result.materialNameToIndex.put(result.materialNames[i],new Integer(i));
			System.out.println("Material: "+i+" is "+result.materialNames[i]);
		}

		result.redValues = new int[materials];
		result.greenValues = new int[materials];
		result.blueValues = new int[materials];

		for( int i=0; i < materials; i++ ) {

			double[] c = parameters.getMaterialColor(i);

			result.redValues[i] = (int)(255*c[0]);
			result.greenValues[i] = (int)(255*c[1]);
			result.blueValues[i] = (int)(255*c[2]);
		}

		// First transform all the points into transformedPoints:

		ArrayList< PointInPath > transformedPoints = new ArrayList< PointInPath >();

		double [] transformedPoint = new double[3];

		ArrayList< GraphNode > endPoints = new ArrayList< GraphNode >();
		ArrayList< GraphNode > allNodes = new ArrayList< GraphNode >();

		RegistrationAlgorithm.ImagePoint imagePoint = new RegistrationAlgorithm.ImagePoint();

		int paths = allPaths.size();
		// System.out.println("Paths to draw: "+paths);
		for( int i = 0; i < paths; ++i ) {
			Path path = (Path)allPaths.get(i);

			for( int k = 0; k < path.size(); ++k ) {

				int x_in_domain = path.getXUnscaled(k);
				int y_in_domain = path.getYUnscaled(k);
				int z_in_domain = path.getZUnscaled(k);

				matcher.transformDomainToTemplate( x_in_domain, y_in_domain, z_in_domain, imagePoint );

				int x_in_template=imagePoint.x;
				int y_in_template=imagePoint.y;
				int z_in_template=imagePoint.z;

				int label_value=label_data[z_in_template][y_in_template*templateWidth+x_in_template]&0xFF;

				if( label_value >= materials ) {
					IJ.error( "A label value of " + label_value + " was found, which is not a valid material (max " + (materials - 1) + ")" );
					return null;
				}

				PointInPath p = new PointInPath();

				p.setPosition( x_in_template, y_in_template, z_in_template );
				p.setNeuropilRegion( result.materialNames[label_value] );
				p.setPathID( i );

				if( k == 0 )
					p.setStart( true );
				if( k == (path.size() - 1) )
					p.setEnd( true );

				// System.out.println( p.getPathID() + "|" + i + " - " + p.getNeuropilRegion() + ": at (" + x_in_template + "," + y_in_template + "," + z_in_template + ")" );

				transformedPoints.add(p);
			}
		}

		// Now create a node for each endpoint (and
		// this defines the unique endpoints (these
		// may also be midpoint nodes in other paths)...

		int limit = 5; // Some if within 5 pixels...

		int e = 0;

		// This is n squared, so All Really Bad.

		System.out.println("Finding which endpoints are really the same.");

		for( int i = 0; i < transformedPoints.size(); ++i ) {

			PointInPath p = (PointInPath)transformedPoints.get(i);
			if( p.start() || p.end() ) {

				boolean already_recorded = false;

				for( int j = 0; j < endPoints.size(); ++j ) {
					GraphNode q = (GraphNode)endPoints.get(j);
					if( p.nearTo(limit,q.x,q.y,q.z) ) {
						// System.out.println( "Two near points" );
						// System.out.println( "materials: " + p.getNeuropilRegion() + " and " + q.material_name );
						if (p.getNeuropilRegion().equals(q.material_name)) {
							// System.out.println( "  same material name" );
							// Then we assume they're the same, but set that information.
							p.node = q;
							already_recorded = true;
							break;
						}
					}
				}

				if( ! already_recorded ) {

					// Create the new node.

					GraphNode g = new GraphNode();

					g.x = (int)p.getX();
					g.y = (int)p.getY();
					g.z = (int)p.getZ();

					g.id = e;

					g.material_name = p.getNeuropilRegion();

					p.node = g;

					endPoints.add( g );
					allNodes.add( g );

					++e;
				}

			}

		}

		System.out.println("Done finding which endpoints are really the same.");

		// Now we're going to go through all the points, path by path.

		ArrayList< PointInPath > inThisPath = null;
		for( int i = 0; i < transformedPoints.size(); ++i  ) {
			PointInPath p = (PointInPath)transformedPoints.get(i);
			if( p.start() ) {
				// System.out.println( "Found start point: " + p );
				// Set up a new path to add to, and add this point.
				inThisPath = new ArrayList< PointInPath >();
				inThisPath.add(p);
			} else if( p.end() ) {
				// System.out.println( "Found end point: " + p );
				inThisPath.add(p);
				{
					// Now analyze that path.
					int start_id = ((PointInPath)inThisPath.get(0)).node.id;
					int end_id = ((PointInPath)inThisPath.get(inThisPath.size()-1)).node.id;

					System.out.println("Path from ID "+start_id+" to "+end_id);

					double nearestDistanceSq[] = new double[endPoints.size()];
					for( int n = 0; n < nearestDistanceSq.length; ++n )
						nearestDistanceSq[n] = -1;
					PointInPath nearestPointInPath[] = new PointInPath[endPoints.size()];

					for( int pinpath = 0; pinpath < inThisPath.size(); ++pinpath ) {

						PointInPath pi = (PointInPath)inThisPath.get(pinpath);
						for( int j = 0; j < endPoints.size(); ++j ) {
							GraphNode q = (GraphNode)endPoints.get(j);
							if( pi.nearTo(limit,q.x,q.y,q.z) ) {
								// System.out.println( "  point in path is near to node ID " + q.id );
								if( pi.getNeuropilRegion().equals(q.material_name) &&
								    (q.id != start_id) &&
								    (q.id != end_id) ) {

									// Then we assume they're the same, and set that information.

									double xdiff = q.x - pi.getX();
									double ydiff = q.y - pi.getY();
									double zdiff = q.z - pi.getZ();
									double distancesq = xdiff*xdiff + ydiff*ydiff + zdiff*zdiff;
									System.out.println( "  on path between " + start_id + " and " + end_id +
											    "  lies the node " + q.id + " (distancesq " + distancesq );

									if( (nearestDistanceSq[j] < 0) || (distancesq < nearestDistanceSq[j]) ) {
										nearestDistanceSq[j] = distancesq;
										nearestPointInPath[j] = pi;
									}

									break;
								}
							}
						}
					}

					for( int n = 0; n < nearestDistanceSq.length; ++n ) {
						double ds= nearestDistanceSq[n];
						if( ds >= 0 ) {
							GraphNode g = (GraphNode)endPoints.get(n);
							PointInPath pi = nearestPointInPath[n];
							pi.node = g;
							System.out.println( "--- nearest point to node " + n + " (distancesq: " + ds + ") was point " + pi );
						}
					}


				}
				inThisPath = null;
			} else {
				// System.out.println( "Found intermediate point: " + p );
				inThisPath.add(p);
			}
		}

		System.out.println("Number of end points is: "+endPoints.size());

		int max_nodes = 4096;
		// Limit the number of possible nodes to 1024 for the time being...
		double [][] distances = new double[max_nodes][max_nodes];
		for( int i = 0; i < max_nodes; ++i )
			for( int j = 0; j < max_nodes; ++j )
				distances[i][j] = -1;

		double distance = 0;

		double last_x = -1;
		double last_y = -1;
		double last_z = -1;
		String last_material_name = null;
		boolean change_into_material_not_registered = false;

		GraphNode lastNode = null;

		// ------------------------------------------------------------------------

		// So, when we look at each point, one of the following must be true:
		//
		//    This is a start point
		//    This material is different from the last, and we're now in the interior

		// If there's a change of material

		for( int i = 0; i < transformedPoints.size(); ++i ) {

			PointInPath p = (PointInPath)transformedPoints.get(i);

			double p_x = p.getX();
			double p_y = p.getY();
			double p_z = p.getZ();
			String material_name = p.getNeuropilRegion();

			if( p.start() ) {
				System.out.println( "=====================" );
				System.out.println( "Path starting at id " + p.node.id );
				distance = 0;
				lastNode = p.node;
				last_x = -1;
				last_y = -1;
				last_z = -1;
				last_material_name = material_name;
			} else {

				if( material_name.equals("Exterior") ) {
					double xdiff = p_x - last_x;
					double ydiff = p_y - last_y;
					double zdiff = p_z - last_z;
					distance += Math.sqrt( xdiff*xdiff + ydiff*ydiff + zdiff*zdiff );
				}

			}

			System.out.println( "point " + p );

			boolean pathEnded = false;

			if( p.node == null ) {

				// Check that no endpoint is actually really close to this point:
				for( int j = 0; j < endPoints.size(); ++j ) {
					GraphNode g = (GraphNode)endPoints.get(j);
					if( g.nearTo(limit,(int)p_x,(int)p_y,(int)p_z) && (g.material_name.equals(p.getNeuropilRegion())) && (g.id != lastNode.id) ) {
						pathEnded = true;
						change_into_material_not_registered = false;
						System.out.println( "A: distance " + distance + " from " + lastNode.id + " to " + g.id );
						distances[lastNode.id][g.id] = distance;
						distances[g.id][lastNode.id] = distance;
						lastNode = g;
						distance = 0;
						break;
					}
				}

			} else {

				// Then this is an existing endpoint; set the distances from the last node.
				GraphNode g = p.node;

				if( p.node.id != lastNode.id ) {
					pathEnded = true;
					change_into_material_not_registered = false;
					System.out.println( "B: distance " + distance + " from " + lastNode.id + " to " + g.id );
					distances[lastNode.id][g.id] = distance;
					distances[g.id][lastNode.id] = distance;
					lastNode = g;
					distance = 0;
				}
			}

			// So now we've dealt with all situations where an endpoint might end the path.
			// The only other situation where we might need to create a node because we've
			// moved out of a region when the change into the region wasn't registered.

			if( ! pathEnded ) {
				if( ! last_material_name.equals(material_name) ) {

					System.out.println( "changing from material " + last_material_name + " to " + material_name );

					if( material_name.equals("Exterior") && change_into_material_not_registered ) {

						GraphNode g = new GraphNode();
						// It's a bit arbitrary where we mark the position of this one, so just make it the last point in the region.
						g.x = (int)last_x;
						g.y = (int)last_y;
						g.z = (int)last_z;
						g.material_name = last_material_name;
						g.id = e++;
						allNodes.add(g);

						System.out.println( "C: distance " + distance + " from " + lastNode.id + " to " + g.id );
						distances[lastNode.id][g.id] = distance;
						distances[g.id][lastNode.id] = distance;
						lastNode = g;
						distance = 0;

						lastNode = g;
						change_into_material_not_registered = false;

					} else {

						change_into_material_not_registered = true;

					}

				}
			}

			last_x = p_x;
			last_y = p_y;
			last_z = p_z;

			last_material_name = material_name;

		}

		result.distances = distances;
		result.allNodes = allNodes;

		return result;
	}

	static public Connectivity buildGraph( String imageFileName ) {

		String tracesFileName = imageFileName + ".traces";
		PathAndFillManager manager=new PathAndFillManager();
		manager.loadGuessingType(tracesFileName);
		return buildGraph( imageFileName, manager.getAllPaths() );
	}

	public void run(String arg) {

		FileAndChannel[] c061FilesWithTraces={
			new FileAndChannel("/media/WD USB 2/corpus/central-complex/c061AG.lsm",0),
			new FileAndChannel("/media/WD USB 2/corpus/central-complex/c061AH.lsm",0),
			new FileAndChannel("/media/WD USB 2/corpus/central-complex/c061AI().lsm",0),
			new FileAndChannel("/media/WD USB 2/corpus/central-complex/c061AJ.lsm",0),
			new FileAndChannel("/media/WD USB 2/corpus/central-complex/c061AK.lsm",0)
		};

		Hashtable< String, Integer > connectionCounts = new Hashtable< String, Integer >();
		Hashtable< String, double [] > connectionDistances = new Hashtable< String, double [] >();
		int filesConsidered = c061FilesWithTraces.length;

		Connectivity lastConnectivity = null;

		HashSet< String > nonExteriorMaterials = new HashSet< String >();


		String [] outputPrefixes = new String[20];
		int dotFiles = 0;

		for( int fnumber = 0; fnumber < c061FilesWithTraces.length; ++fnumber ) {

			int x, y, z;

			FileAndChannel fc = c061FilesWithTraces[fnumber];

			String fileName = fc.getPath();

			int lastDotIndex = fileName.lastIndexOf( '.' );
			System.out.println("lastDotIndex " + lastDotIndex );
			int lastSeparatorIndex = fileName.lastIndexOf( File.separatorChar );
			System.out.println("lastSeparatorIndex " + lastSeparatorIndex );


			String namePrefix = fileName.substring( lastSeparatorIndex + 1, lastDotIndex );

			Connectivity connectivity = buildGraph( fileName );
			ArrayList allNodes = connectivity.allNodes;
			double [][] distances = connectivity.distances;

			lastConnectivity = connectivity;

			// Now dump this network to some format that dot can parse....

			try {

				String outputPrefix = "test-" + namePrefix;
				outputPrefixes[dotFiles++] = outputPrefix;

				BufferedWriter out = new BufferedWriter(new FileWriter(outputPrefix+".dot",false));

				out.write( "graph G {\n" );
				out.write( "        graph [overlap=scale,splines=true];\n");
				out.write( "        node [fontname=\"DejaVuSans\",style=filled];\n");

				for( int i = 0; i < allNodes.size(); ++i ) {
					GraphNode node = (GraphNode)allNodes.get(i);
					String material_name = node.material_name;

					out.write( "        \"" + node.toDotName() + "\" [fillcolor=\"" + connectivity.colorString(material_name) + "\"];\n" );
				}

				HashSet< String > reflexive = new HashSet< String >();
				for( int i = 0; i < allNodes.size(); ++i ) {
					GraphNode start_node = (GraphNode)allNodes.get(i);
					for( int j = 0; j < allNodes.size(); ++j ) {
						if( distances[i][j] >= 0 ) {
							GraphNode end_node = (GraphNode)allNodes.get(j);
							out.write( "        \"" +  start_node.toDotName() + "\" -- \"" + end_node.toDotName() + "\";\n" );
						}
					}
				}
				for( String node_name : reflexive )
					out.write( "        \"" +  node_name + "\" -- \"" + node_name + "\";\n" );

				out.write( "}" );

				out.close();

			} catch( IOException ioe ) {

				IJ.error( "Exception while writing the file" );

			}



			// Now dump this network to some format that dot can parse....

			try {

				String outputPrefix = "test-collapsed-" + namePrefix;
				outputPrefixes[dotFiles++] = outputPrefix;

				BufferedWriter out = new BufferedWriter(new FileWriter(outputPrefix+".dot",false));

				out.write( "graph G {\n" );
				out.write( "        graph [overlap=scale,splines=true];\n");
				out.write( "        node [fontname=\"DejaVuSans\",style=filled];\n");

				for( int i = 0; i < allNodes.size(); ++i ) {
					GraphNode node = (GraphNode)allNodes.get(i);
					String material_name = node.material_name;

					out.write( "        \"" + node.toCollapsedDotName() + "\" [fillcolor=\"" + connectivity.colorString(material_name) + "\"];\n" );
				}


				HashSet< String > reflexive = new HashSet< String >();
				for( int i = 0; i < allNodes.size(); ++i ) {
					GraphNode start_node = (GraphNode)allNodes.get(i);
					for( int j = 0; j < allNodes.size(); ++j ) {
						if( distances[i][j] >= 0 ) {
							GraphNode end_node = (GraphNode)allNodes.get(j);

							if( start_node.toCollapsedDotName().equals(end_node.toCollapsedDotName()) ) {

								// if( start_node.material_name.equals("Exterior") )
								// 	;
								// else
								reflexive.add(start_node.toCollapsedDotName());

							} else {
								out.write( "        \"" +  start_node.toCollapsedDotName() + "\" -- \"" + end_node.toCollapsedDotName() + "\";\n" );
							}
						}
					}
				}
				for( String node_name : reflexive )
					out.write( "        \"" +  node_name + "\" -- \"" + node_name + "\";\n" );

				out.write( "}" );

				out.close();

			} catch( IOException ioe ) {

				IJ.error( "Exception while writing the file" );

			}

			// Ultimately, what we want to do is to find - for
			// each pair of neuropil regions, can we find a path
			// between those two in the graph of nodes?

			// Each node is either and endpoint or a neuropil
			// region.


			// Now dump this network to some format that dot can parse....

			try {

				String outputPrefix = "test-verycollapsed-" + namePrefix;
				outputPrefixes[dotFiles++] = outputPrefix;

				BufferedWriter out = new BufferedWriter(new FileWriter(outputPrefix+".dot",false));

				out.write( "graph G {\n" );
				out.write( "        graph [overlap=scale,splines=true];\n");
				out.write( "        node [fontname=\"DejaVuSans\",style=filled];\n");

				for( int i = 0; i < allNodes.size(); ++i ) {
					GraphNode node = (GraphNode)allNodes.get(i);
					String material_name = node.material_name;

					out.write( "        \"" + node.material_name + "\" [fillcolor=\"" + connectivity.colorString(material_name) + "\"];\n" );
				}

				for( int i = 1; i < connectivity.materialNames.length; ++i ) {
					for( int j = i+1; j < connectivity.materialNames.length; ++j ) {

						String from_material = connectivity.materialNames[i];
						String to_material = connectivity.materialNames[j];

						// Find the path between the two...

						// But there will be multiple
						// nodes in that part, and in
						// the destination.

						if( from_material == null )
							continue;
						if( to_material == null )
							continue;

						boolean foundConnection = false;
						double shortestDistance = Double.MAX_VALUE;

						System.out.println( "from: " + from_material + " -> " + to_material );

						String hashKey = "\"" + from_material + "\" -- \"" + to_material+ "\"";

						for( int start_node_index = 0;
						     start_node_index < allNodes.size();
						     ++start_node_index )
							for( int end_node_index = 0;
							     end_node_index < allNodes.size();
							     ++end_node_index ) {

								GraphNode start_node = (GraphNode)allNodes.get(start_node_index);
								GraphNode end_node = (GraphNode)allNodes.get(end_node_index);

								if( start_node.material_name.equals(from_material) &&
								    end_node.material_name.equals(to_material) ) {

									System.out.println("== Trying to find path between "+start_node.toDotName()+" and "+end_node.toDotName());

									PathWithLength pathWithLength =
										connectivity.pathBetween(
											start_node,
											end_node );

									if( pathWithLength != null ) {

										foundConnection = true;

										// System.out.println("Path is of length " + pathBetween.size());

										for( int index_in_path = 0;
										     index_in_path < pathWithLength.path.size();
										     ++index_in_path ) {
											GraphNode g = (GraphNode)pathWithLength.path.get(index_in_path);
											System.out.print( g.toDotName() + "-" );
										}
										System.out.println("");

										if( pathWithLength.length < shortestDistance ) {
											shortestDistance = pathWithLength.length;
										}

									}


									// If there is one, then output that:
									//
									// ---
									//




								}
							}

						if( foundConnection ) {

							if( ! from_material.equals("Exterior") )
								nonExteriorMaterials.add(from_material);
							if( ! to_material.equals("Exterior") )
								nonExteriorMaterials.add(to_material);

							out.write( "        \"" + from_material + "\" -- \"" + to_material + "\";\n" );
							System.out.println("C: "+from_material+"-"+to_material);
							if( ! connectionDistances.containsKey(hashKey) )
								connectionDistances.put(hashKey,new double[filesConsidered]);
							if( ! connectionCounts.containsKey(hashKey) )
								connectionCounts.put(hashKey,new Integer(0));
							double [] ds = connectionDistances.get(hashKey);
							int connectionsSoFar = connectionCounts.get(hashKey).intValue();
							ds[connectionsSoFar] = shortestDistance * 1.16;
							connectionCounts.put(hashKey,new Integer(connectionsSoFar+1));
						}

					}
				}
				out.write( "}" );

				out.close();

			} catch( IOException ioe ) {

				IJ.error( "Exception while writing the file" );

			}

			// break;

		}



		// ------------------------------------------------------------------------


		try {

			String outputPrefix = "overall-";
			outputPrefixes[dotFiles++] = outputPrefix;

			BufferedWriter out = new BufferedWriter(new FileWriter(outputPrefix+".dot",false));

			out.write( "graph G {\n" );
			out.write( "        graph [overlap=scale,splines=true];\n");
			out.write( "        node [fontname=\"DejaVuSans\",style=filled];\n");

			for( String material_name : nonExteriorMaterials )
				out.write( "        \"" + material_name + "\" [fillcolor=\"" + lastConnectivity.colorString(material_name) + "\"];\n" );

			for( Enumeration e = connectionCounts.keys(); e.hasMoreElements(); ) {
				String connection_string = (String)e.nextElement();
				int counts = ((Integer)connectionCounts.get(connection_string)).intValue();
				double sum = 0;
				double sum_squared = 0;
				double [] ds = (double [])connectionDistances.get(connection_string);
				for( int i = 0; i < counts; ++i ) {
					sum += ds[i];
					sum_squared += ds[i] * ds[i];
				}
				double mean = sum / counts;
				double sd = Math.sqrt( (sum_squared / counts) - (mean * mean) );

				System.out.println( connection_string + (counts / filesConsidered) + " mean distance " + mean + " [sd " + sd + "]" );

				String label = "p: " + (counts / (double)filesConsidered) + "\\nmean d: " + mean + ( sd > 0 ? "\\nsd d: " + sd : "" );

				// label = "";

				out.write( "        " + connection_string + " [style=\"setlinewidth("+counts+")\",label=\"" + label + "\",fontsize=11]\n" );

			}

			out.write( "}" );

			out.close();

		} catch( IOException ioe ) {

			IJ.error( "Exception while writing the file" );

		}







		// ------------------------------------------------------------------------

		for( int i = 0; i < dotFiles; ++i ) {

			String outputPrefix = outputPrefixes[i];
			String dotFile = outputPrefix + ".dot";
			String svgFile = outputPrefix + ".svg";

			System.out.println( "Generating " + svgFile + " from " + dotFile );

			try {
				Process p = Runtime.getRuntime().exec( "neato -Tsvg -o"+ svgFile +" < " + dotFile );
				p.waitFor();
			} catch( IOException ioe ) {
				System.out.println("Got IOException: "+ioe);
			} catch( InterruptedException ie ) {
				System.out.println("Got InterruptedException: "+ie);
			}


		}

		System.exit(0);

	}
}

