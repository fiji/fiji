/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/*
 * NamedPointSet.java
 *
 * Created on 28-Sep-2007, 11:49:37
 */

package landmarks;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.measure.Calibration;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.StringReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.Set;
import math3d.Point3d;
import vib.transforms.FastMatrixTransform;
import vib.transforms.OrderedTransformations;
import tracing.PathAndFillManager;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;
import util.BatchOpener;
import util.opencsv.CSVReader;

public class NamedPointSet {

	private static class Handler extends DefaultHandler {
		protected NamedPointSet nps;
		public Handler( NamedPointSet nps ) {
			super();
			this.nps = nps;
		}
		String version = "";
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if( qName.equals("namedpointset") ) {
				version = attributes.getValue( "version" );
				if( version == null ) {
					throw new SAXException( "No 'version' attribute in <namedpointset>" );
				}
			}
			if( qName.equals("pointworld") ) {
				String setString = attributes.getValue("set");
				String nameString = attributes.getValue("name");
				String xString = attributes.getValue("x");
				String yString = attributes.getValue("y");
				String zString = attributes.getValue("z");
				if( setString == null )
					throw new SAXException( "No 'set' attribute in <pointworld>" );
				boolean set;
				if( setString.equals("true") )
					set = true;
				else if( setString.equals("false") )
					set = false;
				else
					throw new SAXException( "The 'set' attribute must be 'true' or 'false'" );
				if( nameString == null )
					throw new SAXException( "No 'name' attribute in <pointworld" );
				String name = nameString;
				if( set && (xString == null || yString == null || zString == null) )
					throw new SAXException( "If 'set' is true then all of 'x', 'y' and 'z' must be specified." );
				if( ! set && (xString != null || yString != null || zString != null ) )
					throw new SAXException( "If 'set' is false then none of 'x', 'y' or 'z' may be specified." );
				double x = -1;
				double y = -1;
				double z = -1;
				if( set ) {
					try {
						x = Double.parseDouble( xString );
						y = Double.parseDouble( yString );
						z = Double.parseDouble( zString );
						nps.add( new NamedPointWorld( name, x, y, z ) );
					} catch( NumberFormatException e ) {
						throw new SAXException( "One of 'x', 'y' and 'z' couldn't be parsed as a number" );
					}
				} else {
					nps.add( new NamedPointWorld( name ) );
				}
			}
		}
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
		}
	}

	public static class PointsFileException extends Exception {
		PointsFileException( String message ) {
			super( message );
		}
	}

	ArrayList<NamedPointWorld> pointsWorld;

	public int size() {
		return pointsWorld.size();
	}

	public NamedPointSet() {
		pointsWorld = new ArrayList<NamedPointWorld>();
	}

	public ListIterator listIterator() {
		return pointsWorld.listIterator();
	}

	public NamedPointSet transformPointsWith( OrderedTransformations o ) {
		NamedPointSet result = new NamedPointSet();
		Iterator i0;
		for( i0 = pointsWorld.listIterator(); i0.hasNext(); ) {
			NamedPointWorld p = (NamedPointWorld)i0.next();
			NamedPointWorld transformed = p.transformWith(o);
			result.add( transformed );
		}
		return result;
	}

	public NamedPointWorld getPoint( String name ) {
		Iterator<NamedPointWorld> i;
		for (i = listIterator(); i.hasNext();) {
			NamedPointWorld p = i.next();
			if( p.name.equals(name) ) {
				return p;
			}
		}
		return null;
	}

	synchronized public NamedPointWorld delete(int i) {
		return (NamedPointWorld)pointsWorld.remove(i);
	}

	public NamedPointWorld get(int i) {
		return pointsWorld.get(i);
	}

	public NamedPointWorld get(String name) {
		Iterator<NamedPointWorld> i0;
		for(i0=pointsWorld.listIterator();i0.hasNext();) {
			NamedPointWorld p=i0.next();
			if( p.getName().equals(name) )
				return p;
		}
		return null;
	}

	public String[] getPointNames(){
		String[] sa= new String[pointsWorld.size()];
		int i=0;
		for (Iterator it = listIterator(); it.hasNext();) {
			NamedPointWorld p = (NamedPointWorld)it.next();
			sa[i++]=p.name;
		}
		return sa;
	}

	public int getIndexOfPoint( String name ) {
		int i = 0;
		for( NamedPointWorld p : pointsWorld ) {
			if( p.getName().equals( name ) )
				return i;
			++i;
		}
		return -1;
	}

	void showAsROI(int i, ImagePlus imp) {
		NamedPointWorld p = pointsWorld.get(i);
		assert p.set;
		double x = p.x;
		double y = p.y;
		double z = p.z;
		Calibration c = imp.getCalibration();
		if( c != null ) {
			x /= c.pixelWidth;
			y /= c.pixelHeight;
			z /= c.pixelDepth;
		}
		int sliceAllChannels = (int)z;
		System.out.println("sliceAllChannels: "+sliceAllChannels);
		int channels = imp.getNChannels();
		int currentChannel = imp.getChannel();
		int slice = sliceAllChannels * channels + currentChannel;
		System.out.println("slice: "+slice);
		imp.setSlice(slice);
		Roi roi = new PointRoi( (int)x, (int)y );
		imp.setRoi(roi);
	}

	public boolean savePointsFile( String savePath ) {

		try {
			FileOutputStream fos = new FileOutputStream(savePath);
			byte [] asBytes = xmlDataAsBytes( );
			fos.write(asBytes);
			fos.close();
			return true;
		} catch( IOException e ) {
			return false;
		}
	}

	public String xmlDataAsString( ) {
		StringBuffer result = new StringBuffer();
		result.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		result.append("<!DOCTYPE namedpointset [\n");
		result.append("  <!ELEMENT namedpointset (pointworld*)>\n");
		result.append("  <!ELEMENT pointworld EMPTY>\n");
		result.append("  <!ATTLIST namedpointset version CDATA #REQUIRED>\n");
		result.append("  <!ATTLIST pointworld set (true|false) #REQUIRED>\n");
		result.append("  <!ATTLIST pointworld name CDATA #REQUIRED>\n");
		result.append("  <!ATTLIST pointworld x CDATA #IMPLIED>\n");
		result.append("  <!ATTLIST pointworld y CDATA #IMPLIED>\n");
		result.append("  <!ATTLIST pointworld z CDATA #IMPLIED>\n");
		result.append("]>\n\n");
		result.append("<namedpointset version=\"1.0\">\n");
		Iterator<NamedPointWorld> i = pointsWorld.iterator();
		while( i.hasNext() ) {
			NamedPointWorld p = i.next();
			result.append("  ");
			result.append(p.toXMLElement());
			result.append("\n");
		}
		result.append("</namedpointset>\n");
		return result.toString();
	}

	public byte [] xmlDataAsBytes( ) {
		try {
			return xmlDataAsString().getBytes("UTF-8");
		} catch( java.io.UnsupportedEncodingException e ) {
			IJ.error("UTF-8 isn't available (!)");
			return null;
		}
	}

	public boolean saveIGSPointsFile( String savePath ) {

		try {
			FileOutputStream fos = new FileOutputStream(savePath);
			StringBuffer sb=new StringBuffer("! TYPEDSTREAM 1.1\n");
			Iterator<NamedPointWorld> i;
			for(i=listIterator();i.hasNext();) {
				NamedPointWorld p = i.next();
				if(p.set) sb.append(p.toIGS()+"\n");
			}
			fos.write(sb.toString().getBytes("UTF-8"));
			fos.close();
			return true;
		} catch( IOException e ) {
			return false;
		}
	}

/*
	public byte [] dataAsBytes( ) {

		int total_bytes = 0;

		ArrayList< byte [] > linesOfBytes = new ArrayList< byte [] >();
		Iterator i;
		for(i=listIterator();i.hasNext();) {
			NamedPoint p = (NamedPoint)i.next();
			if(p.set) {
				String line = p.toYAML() + "\n";
				byte [] line_bytes;
				try {
					line_bytes = line.getBytes("UTF-8");
				} catch( UnsupportedEncodingException e ) {
					IJ.error( "Got an UnsupportedEncodingException - this should never happen: " + e );
					return null;
				}
				linesOfBytes.add( line_bytes );
				total_bytes += line_bytes.length;
			}
		}

		byte [] result = new byte[total_bytes];
		int add_at = 0;

		for( int j = 0; j < linesOfBytes.size(); ++j ) {
			byte [] line_of_bytes = linesOfBytes.get(j);
			System.arraycopy( line_of_bytes, 0, result, add_at, line_of_bytes.length );
			add_at += line_of_bytes.length;
		}

		return result;
	}
*/

	public void add(NamedPointWorld namedPointWorld) {
		synchronized (this) {
			String name = namedPointWorld.getName();
			NamedPointWorld existing = get( name );
			if( existing != null )
				throw new RuntimeException( "Trying to add a point of name '" + name + "', but this NamedPointSet already has one." );
			pointsWorld.add( namedPointWorld );
		}
	}

	/* The file loading here is complicated by the large number of
	   possible filenames and fileformats.

	   If the file is called "test.tif", we try each of the
	   following filenames for the points file in turn:

	     test.points.xml
	     test.tif.points.xml
	     test.points.R
	     test.tif.points.R
	     test.points
	     test.tif.points

	   If the file has no extension, e.g. it's called "test", we
	   try each of the following filenames:

	     test.points.xml
	     test.points.R
	     test.points

	   For each file that exists, we try to parse it as:

	     - An XML file:
		 - The new standard format.
		 - Co-ordinates are in world space (i.e. scaled by
		   calibration).

	     - An "R" file:
		 - This is really a tab separated values file.
		 - Co-ordinates are again in world space.

	     - A pseudo-YAML file:
		 - Co-ordinates are integer indicies of the samples,
		   with Z co-ordinates 0-indexed.
		 - This format should no longer be used, but is
		   supported to load old files.

	     - [FIXME: could add Torsten's IGS files here if I had the specification.]

	   Since the lattermost case doesn't store the calibration
	   data, we pass down an imageFilename and imagePlus parameter
	   to each, since we may discover that we need to fetch the
	   calibration data, and either:

	     - imagePlus non-null: The ImagePlus is already loaded
	       (very efficient to get the calibration)

	     - or imageFilename non-null: the image has to be loaded
	       from that file to get the calibraion (very inefficient)

	     - both are null: return an error if the file is pseudo-YAML

	*/

	public static NamedPointSet forImage( ImagePlus imagePlus ) throws PointsFileException {
		FileInfo info = imagePlus.getOriginalFileInfo();
		if( info == null )
			throw new PointsFileException( "Could not find original file for the image: " + imagePlus.getTitle() );
		String fileName = info.fileName;
		String url = info.url;
		String directory = info.directory;
		File f = new File( directory, fileName );
		return NamedPointSet.forImage( imagePlus, f.getAbsolutePath() );
	}

	static boolean verbose = true;

	public static NamedPointSet forImage( String imageFilename ) throws PointsFileException {
		return forImage( null, imageFilename );
	}

	public static NamedPointSet forImage( ImagePlus imagePlus, String imageFilename ) throws PointsFileException {
		String [] possibleExtensions = { ".points.xml", ".points.R", ".points"  };
		int lastDot = imageFilename.lastIndexOf( "." );
		String withoutExtension = null;
		if( lastDot >= 0 )
			withoutExtension = imageFilename.substring( 0, lastDot );
		for( String extension : possibleExtensions ) {
			for( int i = 0; i < 2; ++i ) {
				String candidateFilename = null;
				if( i == 0 && lastDot >= 0 ) {
					candidateFilename = withoutExtension + extension;
				} else if( i == 1 ) {
					candidateFilename = imageFilename + extension;
				}
				try {
					return fromFile( candidateFilename, imagePlus, imageFilename );
				} catch( PointsFileException e ) { }
			}
		}
		throw new PointsFileException( "None of the points filenames corresponding to '" + imageFilename + "' could be loaded." );
	}

	public static NamedPointSet fromFile( String pointsFilename ) throws PointsFileException {
		return fromFile( pointsFilename, null, null );
	}

	public static NamedPointSet fromFile( String pointsFilename, ImagePlus imagePlus, String imageFilename ) throws PointsFileException {
		File f = new File( pointsFilename );
		if( ! f.exists() ) {
			throw new PointsFileException( "File not found: " + pointsFilename );
		}
		return fromFile( f, imagePlus, imageFilename );
	}

	public static NamedPointSet fromFile( File f ) throws PointsFileException {
		return fromFile( f, null, null );
	}

	public static NamedPointSet fromFile( File f, ImagePlus imagePlus, String imageFilename ) throws PointsFileException {
		try {
			return fromBufferedReader( new BufferedReader( new FileReader(f) ), imagePlus, imageFilename );
		} catch( FileNotFoundException e ) {
			throw new PointsFileException( "Couldn't find the file: "+f.getAbsolutePath() );
		}
	}

	public static NamedPointSet fromString( String s ) {
		return fromString( s );
	}

	// Ultimately, we turn all of these files into a String, and try to load that:
	public static NamedPointSet fromString( String s, ImagePlus imagePlus, String imageFilename ) throws PointsFileException {

		// First try parsing as XML:
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(true);
			SAXParser parser = factory.newSAXParser();
			StringReader reader=new StringReader(s);
			InputSource inputSource=new InputSource(reader);
			NamedPointSet nps = new NamedPointSet();
			Handler h = new Handler(nps);
			parser.parse( inputSource, h );
			reader.close();
			return nps;
		} catch( javax.xml.parsers.ParserConfigurationException e ) {
			throw new PointsFileException( "There was a ParserConfigurationException when trying to parse as XML: " + e );
		} catch( SAXException e ) {
			/* This is most likely a real parsing
			   exception, so just carry on to the next
			   method.  It's probably just not XML. */
		} catch( FileNotFoundException e ) {
			// Should never occur from a StringReader...
			throw new PointsFileException( "BUG: FileNotFoundException while parsing XML from a String: " + e );
		} catch( IOException e ) {
			// Should never occur from a String
			throw new PointsFileException( "BUG: IOException while parsing XML from a String: " + e );
		}

		/* Now try loading as a tab-separated values file,
		   e.g. the points.R files that we have generated in
		   the past: */
		try {
			NamedPointSet nps = new NamedPointSet();
			Pattern emptyPattern = Pattern.compile("^ *$");
			Pattern numberPattern = Pattern.compile( "[eE0-9\\.\\-]+" );
			StringReader sr = new StringReader( s );

			CSVReader tsvReader = new CSVReader( sr, '\t' );
			List< String[] > l = tsvReader.readAll();
			for( String [] a : l ) {
				if( a.length != 4 )
					throw new PointsFileException( "There must be 4 fields per line of the tab-separated values file" );
				String xString = a[0];
				String yString = a[1];
				String zString = a[2];
				String name = a[3];
				Matcher emptyXMatcher = emptyPattern.matcher( xString );
				Matcher emptyYMatcher = emptyPattern.matcher( yString );
				Matcher emptyZMatcher = emptyPattern.matcher( zString );
				Matcher numberXMatcher = numberPattern.matcher( xString );
				Matcher numberYMatcher = numberPattern.matcher( yString );
				Matcher numberZMatcher = numberPattern.matcher( zString );
				boolean set;
				if( emptyXMatcher.matches() && emptyYMatcher.matches() && emptyZMatcher.matches() ) {
					set = false;
				} else if( numberXMatcher.matches() && numberYMatcher.matches() && numberZMatcher.matches() ) {
					set = true;
				} else {
					throw new PointsFileException( "In tab-separated format, the first three columns must all be empty or all be numbers" );
				}
				if( set ) {
					double x = Double.parseDouble( xString );
					double y = Double.parseDouble( yString );
					double z = Double.parseDouble( zString );
					nps.add( new NamedPointWorld( name, x, y, z ) );
				} else {
					nps.add( new NamedPointWorld( name ) );
				}
			}
			return nps;
			/* Any of these exceptions mean that it
			   probably wasn't a valid tab-separated
			   values file, so carry on to the last
			   method */
		} catch( PointsFileException pfe ) {
		} catch( IOException e ) {
		} catch( NumberFormatException e ) {
		}

		// Now try loading it as pseudo-YAML file:
		try {
			NamedPointSet nps = new NamedPointSet();
			Pattern p_data =
				Pattern.compile("^\"(.*)\": *"+
						"\\[ *([eE0-9\\.\\-]+) *, *"+
						"([eE0-9\\.\\-]+) *, *"+
						"([eE0-9\\.\\-]+) *\\] *$");

			Pattern p_comment = Pattern.compile("^ *#.*$");
			Pattern p_name_no_data = Pattern.compile("^\"(.*)\":.*$");

			// Make sure we have the calibration data here:
			Calibration c = null;
			if( imagePlus != null )
				c = imagePlus.getCalibration();
			else if( imageFilename != null ) {
				ImagePlus loadedImagePlus = BatchOpener.openFirstChannel( imageFilename );
				if( loadedImagePlus != null ) {
					c = loadedImagePlus.getCalibration();
					loadedImagePlus.close();
				}
			}
			double xSpacing = 1;
			double ySpacing = 1;
			double zSpacing = 1;
			if( c != null ) {
				xSpacing = c.pixelWidth;
				ySpacing = c.pixelHeight;
				zSpacing = c.pixelDepth;
			}

			String [] lines = s.split("[\\r\\n]+");
			for( String line : lines ) {
				line = line.trim();
				if( line.length() == 0 )
					continue;

				Matcher m_data = p_data.matcher(line);
				Matcher m_comment = p_comment.matcher(line);
				Matcher m_name_no_data = p_name_no_data.matcher(line);

				if (m_data.matches()) {
					nps.add( new NamedPointWorld( m_data.group(1),
								      Double.parseDouble(m_data.group(2)) * xSpacing,
								      Double.parseDouble(m_data.group(3)) * ySpacing,
								      Double.parseDouble(m_data.group(4)) * zSpacing) );
				} else if (m_name_no_data.matches()) {
					nps.add( new NamedPointWorld( m_name_no_data.group(1) ) );
				} else if (m_comment.matches()) {
					continue;
				} else {
					throw new PointsFileException( "Couldn't parse the points file; the problematic line is '" + line + "'" );
				}
			}
			if( c == null )
				IJ.error( "Warning: no calibration data found for a pseudo-YAML points file; assuming that voxel spacing is ( 1, 1, 1 )" );
			return nps;
		} catch( NumberFormatException e ) {
			throw new PointsFileException( "Failed to load the points file by any method; the last error was: " + e );
		} catch( PointsFileException e ) {
			throw new PointsFileException( "Failed to load the points file by any method; the last error was: " + e );
		}
	}

	public static NamedPointSet fromBufferedReader( BufferedReader br, ImagePlus imagePlus, String imageFilename ) throws PointsFileException {
		StringBuffer result = new StringBuffer( "" );
		String line = null;
		try {
			while( true ) {
				line = br.readLine();
				if( line == null )
					return fromString( result.toString(), imagePlus, imageFilename );
				else {
					result.append( line );
					result.append( "\n" );
				}
			}
		} catch( IOException e ) {
			throw new PointsFileException( "There was an IOException while reading points file data: " + e );
		}
	}

	public ArrayList<String> namesSharedWith( NamedPointSet other ) {
		return namesSharedWith( other, false );
	}

	public ArrayList<String> namesSharedWith( NamedPointSet other, boolean onlySetPoints ) {

		ArrayList<String> common = new ArrayList<String>();
		for( NamedPointWorld iNPW : pointsWorld ) {
			String pointName = iNPW.name;
			for( NamedPointWorld jNPQ : other.pointsWorld ) {
				if( (iNPW.set && jNPQ.set) || ! onlySetPoints ) {
					if( pointName.equals(jNPQ.name) ) {
						common.add( new String( pointName ) );
						break;
					}
				}
			}
		}
		return common;
	}

	public Point3d[] getPoint3DArrayForNames( String [] names ) {
		Point3d [] result = new Point3d[names.length];
		for( int i = 0; i < names.length; ++i ) {
			NamedPointWorld np = get(names[i]);
			if( np == null )
				return null;
			else
				result[i] = new Point3d( np.x, np.y, np.z );
		}
		return result;
	}

	public Set< String > getNamesAsSet( ) {
		HashSet< String > namesSet = new HashSet< String >();
		for( NamedPointWorld npw : pointsWorld ) {
			namesSet.add( npw.getName() );
		}
		return namesSet;
	}

	public boolean equals( NamedPointSet other ) {
		Set< String > thisNames = getNamesAsSet();
		Set< String > otherNames = other.getNamesAsSet();
		if( ! thisNames.equals( otherNames ) )
			return false;
		double epsilon = 0.00001;
		for( String name : thisNames ) {
			NamedPointWorld thisNPW = get( name );
			NamedPointWorld otherNPW = other.get( name );
			if( thisNPW.set != otherNPW.set )
				return false;
			if( thisNPW.set ) {
				if( Math.abs( thisNPW.x - otherNPW.x ) > epsilon )
					return false;
				if( Math.abs( thisNPW.y - otherNPW.y ) > epsilon )
					return false;
				if( Math.abs( thisNPW.z - otherNPW.z ) > epsilon )
					return false;
			}
		}
		return true;
	}

	public String toString( ) {
		StringBuffer sb = new StringBuffer();
		for( NamedPointWorld npw : pointsWorld ) {
			sb.append( npw.toString() );
			sb.append( "\n" );
		}
		return sb.toString();
	}

	synchronized public boolean renamePointTo( int i, String newName ) {
		NamedPointWorld existing = get( newName );
		if( existing == null ) {
			pointsWorld.get(i).setName( newName );
			return true;
		} else {
			return false;
		}
	}

	synchronized public NamedPointWorld addNewPoint( ) {
		int i = pointsWorld.size();
		boolean nameTaken = true;
		String newName = null;
		while( nameTaken ) {
			newName = "Named Point (" + i + ")";
			nameTaken = get( newName ) != null;
			++ i;
		}
		NamedPointWorld toAdd = new NamedPointWorld( newName );
		pointsWorld.add( toAdd );
		return toAdd;
	}

	void unset( int i ) {
		NamedPointWorld npw = pointsWorld.get(i);
		npw.unset();
	}
}
