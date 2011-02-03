/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* This is a cut-down version of the TrakEM2 Pipe class, which is here
   so that we can use the "makeTube" function to construct meshes from
   a series of points with radiuses without having to include the
   complete TrakEM2_.jar as a dependency.

   It's not very sensible repeating all this code, of course - ideally
   most of this functionality should be in the 3D viewer, but one can
   work from this starting point to do that.

   As an example, you might use this like:

		double [][][] allPoints = Pipe.makeTube(x_points_d,
							y_points_d,
							z_points_d,
							radiuses,
							4,       // resample - 1 means just "use mean distance between points", 3 is three times that, etc.
							12);     // "parallels" (12 means cross-sections are dodecagons)

		java.util.List triangles = Pipe.generateTriangles(allPoints,
								  1); // scale

		String title = "helloooo";

		univ.resetView();

		univ.addMesh(triangles,
			     new Color3f(Color.green),
			     title,
			     1); // threshold, ignored for meshes, I think

   (Mark Longair 2008-06-05) */

/**

TrakEM2 plugin for ImageJ(C).
Copyright (C) 2005, 2006 Albert Cardona and Rodney Douglas.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

In addition, as a special exception, the copyright holders give
you permission to combine this program with free software programs or
libraries that are released under the Apache Public License.

You may contact Albert Cardona at acardona at ini.phys.ethz.ch
Institute of Neuroinformatics, University of Zurich / ETH, Switzerland.
**/

package ij3d;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import javax.vecmath.Point3f;
import javax.vecmath.Color3f;

import Jama.Matrix;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.*;
import ij.measure.Calibration;

import java.awt.Color;
import java.awt.image.IndexColorModel;
import java.awt.image.ColorModel;

public class Pipe {

	static public List generateTriangles(final double[][][] all_points, final double scale) {
		return generateTriangles(all_points,scale,null,null);
	}

	/** Accepts an arrays as that returned from methods
	    generateJoints and makeTube: first dimension is the list of
	    points, second dimension is the number of vertices defining
	    the circular cross section of the tube, and third dimension
	    is the x,y,z of each vertex.

	    pointColorList is input, telling this method what the
	    colour of each point along the path is, while
	    vertexColorList should be an empty list used for output -
	    the color of each vertex in the triangulation will be
	    returned in there. */

	static public List<Point3f> generateTriangles(final double[][][] all_points, final double scale, List<Color3f> pointColorList, List<Color3f> vertexColorList ) {
		boolean outputColors = (pointColorList != null) && (vertexColorList != null);
		if( outputColors && vertexColorList.size() > 0 )
			throw new RuntimeException("vertexColorList in Pipe.generateTriangles() is only for output: should be empty");

		int n = all_points.length;
		final int parallels = all_points[0].length -1;
		List<Point3f> list = new ArrayList<Point3f>();
		for (int i=0; i<n-1; i++) { //minus one since last is made with previous
			for (int j=0; j<parallels; j++) { //there are 12+12 triangles for each joint //it's up to 12+1 because first point is repeated at the end
				// first triangle in the quad
				list.add(new Point3f((float)(all_points[i][j][0] * scale), (float)(all_points[i][j][1] * scale), (float)(all_points[i][j][2] * scale)));
				list.add(new Point3f((float)(all_points[i][j+1][0] * scale), (float)(all_points[i][j+1][1] * scale), (float)(all_points[i][j+1][2] * scale)));
				list.add(new Point3f((float)(all_points[i+1][j][0] * scale), (float)(all_points[i+1][j][1] * scale), (float)(all_points[i+1][j][2] * scale)));

				// second triangle in the quad
				list.add(new Point3f((float)(all_points[i+1][j][0] * scale), (float)(all_points[i+1][j][1] * scale), (float)(all_points[i+1][j][2] * scale)));
				list.add(new Point3f((float)(all_points[i][j+1][0] * scale), (float)(all_points[i][j+1][1] * scale), (float)(all_points[i][j+1][2] * scale)));
				list.add(new Point3f((float)(all_points[i+1][j+1][0] * scale), (float)(all_points[i+1][j+1][1] * scale), (float)(all_points[i+1][j+1][2] * scale)));

				if( outputColors ) {
					vertexColorList.add(pointColorList.get(i));
					vertexColorList.add(pointColorList.get(i));
					vertexColorList.add(pointColorList.get(i+1));

					vertexColorList.add(pointColorList.get(i+1));
					vertexColorList.add(pointColorList.get(i));
					vertexColorList.add(pointColorList.get(i+1));
				}
			}
		}
		return list;
	}

	static public double[][][] makeTube(double[] px, double[] py, double[] pz, double[] p_width_i, final int resample, final int parallels, final boolean do_resample, Color3f flatColor, ImagePlus colorImage, List<Color3f> outputColors) {

		boolean colorsSpecified = flatColor != null || colorImage != null;
		if( colorsSpecified ) {
			if( outputColors == null )
				throw new RuntimeException("If you specify flatColor or colorImage in a call to makeTube(), then outputColors must be non-null");
			if( outputColors.size() > 0 )
				throw new RuntimeException("The outputColors list in makeTube should be empty; it's for output, not input");
		}

		int n = px.length;

		assert(n >= 2);

		// Resampling to get a smoother pipe
		if (do_resample) {
			try {
				VectorString3D vs = new VectorString3D(px, py, pz);
				vs.addDependent(p_width_i);
				vs.resample(vs.getAverageDelta() * resample);
				px = vs.getPoints(0);
				py = vs.getPoints(1);
				pz = vs.getPoints(2);
				p_width_i = vs.getDependent(0);
				//Utils.log("lengths:  " + px.length + ", " + py.length + ", " + pz.length + ", " + p_width_i.length);
				n = vs.length();
			} catch (Exception e) {
				IJ.error(""+e);
			}
			assert(n >= 2);
		}

		if( colorsSpecified ) {
			Color3f [] finalColors = getPointColors(px,py,pz,flatColor,colorImage);
			/* This method adds an extra point at the
			   beginning and end, so duplicate the colours
			   at each: */
			outputColors.add(finalColors[0]);
			for( int i = 0; i < n; ++i )
				outputColors.add(finalColors[i]);
			outputColors.add(finalColors[n-1]);
		}

		double[][][] all_points = new double[n+2][parallels+1][3];
		int extra = 1; // this was zero when not doing capping
		for (int cap=0; cap<parallels+1; cap++) {
			all_points[0][cap][0] = px[0];//p_i[0][0]; //x
			all_points[0][cap][1] = py[0]; //p_i[1][0]; //y
			all_points[0][cap][2] = pz[0]; //z_values[0];
			all_points[all_points.length-1][cap][0] = px[n-1]; //p_i[0][p_i[0].length-1];
			all_points[all_points.length-1][cap][1] = py[n-1]; //p_i[1][p_i[0].length-1];
			all_points[all_points.length-1][cap][2] = pz[n-1]; //z_values[z_values.length-1];
		}
		double angle = 2*Math.PI/parallels; //Math.toRadians(30);

		Vector3 betweenPoints = new Vector3();
		Vector3 lastFirstSpoke = null;
		Vector3[] circle = new Vector3[parallels+1];
		Vector3 intersection = new Vector3();
		Vector3 crossForOffset = new Vector3();

		// Pre-compute the sin and cos of each angle:
		double [] sinn = new double[parallels];
		double [] cosn = new double[parallels];
		for (int q=0; q<parallels; q++) {
			sinn[q] = Math.sin(angle*q);
			cosn[q] = Math.cos(angle*q);
		}

		final double epsilon = 0.0000000001;

		Vector3 unit_y = new Vector3(0,1,0);
		Vector3 unit_z = new Vector3(0,0,1);

		for (int i=0; i<n; i++) {

			// The vector from point i to the next one:
			if( i < n - 1 )
				betweenPoints.set(px[i+1] - px[i], py[i+1] - py[i], pz[i+1] - pz[i]);
			else {
				// Just reuse the previous vector...
			}

			/* Two adjacent points can't be the same - FIXME: this should be ensured
			 * by callers - check that this is actually the case, and that the resampling
			 * can't produce two identical points. */
			if (betweenPoints.isZero(epsilon))
				throw new RuntimeException("Two points on the path were the same");

			/* If this is the first time through, there will be no lastFirstSpoke,
			 * so pick an arbitrary vector in the plane orthogonal to betweenPoints:
			 */
			if (lastFirstSpoke == null) {
				lastFirstSpoke = new Vector3();
				betweenPoints.crossWith(unit_z, lastFirstSpoke);
				if( lastFirstSpoke.isZero(epsilon) ) {
					betweenPoints.crossWith(unit_y, lastFirstSpoke);
				}
			}

			lastFirstSpoke.normalize(lastFirstSpoke);

			/* tangent1 and tangent2 are the tangents to the path at point i and at point
			 * i + 1.  Make these the vectors between the points on either side, if possible.
			 */
			int t1_a = Math.max( 0,   i-1 );
			int t1_b = Math.min( n-1, i+1 );
			int t2_a = Math.min( n-2,   i );
			int t2_b = Math.min( n-1, i+2 );

			Vector3 tangent1 = new Vector3( px[t1_b] - px[t1_a], py[t1_b] - py[t1_a], pz[t1_b] - pz[t1_a] );
			Vector3 tangent2 = new Vector3( px[t2_b] - px[t2_a], py[t2_b] - py[t2_a], pz[t2_b] - pz[t2_a] );

			/* Now if we imagine the two planes defined by those tangents are overlaid
			 * then we want the vector of their line of intersection, which will be
			 * the cross product of the two tangents.  This intersection vector will
			 * be the vector of a spoke which is in both discs:
			 */
			tangent1.crossWith(tangent2,intersection);

			/* We're trying to make sure that the intersection vector for each pair of
			 * disks is matched up.  This amounts to finding out the offset angle to start at.
			 * If the intersection vector is 0, that means the two tangents are parallel,
			 * so make the offset 0 and pretend the intersection vector is the lastFirstSpoke
			 */

			int offset;
			if( intersection.isZero(epsilon) ) {
				offset = 0;
				intersection.setFrom(lastFirstSpoke);
			} else {
				/* Otherwise, we should find out the angle between the intersection
				 * and the last first spoke.  Then we start at the same offset. */
				intersection.normalize(intersection);

				/* Both intersection and lastFirstSpoke are normalized, so the dot
				 * product gives us the cosine of the angle between them */
				double offsetAngle = Math.acos( intersection.dotWith(lastFirstSpoke) );

				/* But we don't know if the angle from intersection to lastFirstSpoke
				 * is clockwise or anti-clockwise in the disk.  Take the cross product
				 * and test whether it is parallel or anti-parallel to tangent1 to find out:
				 */
				intersection.crossWith(lastFirstSpoke, crossForOffset);
				if( crossForOffset.dotWith(tangent1) < 0 ) {
					offsetAngle = 2 * Math.PI - offsetAngle;
				}

				/* Now turn that angle into an offset: */
				offset = (int)Math.round(offsetAngle / angle);
				offset = offset % parallels;
			}

			intersection.scale(p_width_i[i], intersection);

			for( int q = 0; q < parallels; ++q ) {
				int a = (q + offset) % parallels;
				circle[q] = rotate_v_around_axis(intersection,tangent1,sinn[a],cosn[a]);
			}
			circle[parallels] = circle[0];
			lastFirstSpoke.setFrom(circle[0]);

			// Adding points to main array
			for (int j=0; j<parallels+1; j++) {
				all_points[i+extra][j][0] = /*p_i[0][i]*/ px[i] + circle[j].x;
				all_points[i+extra][j][1] = /*p_i[1][i]*/ py[i] + circle[j].y;
				all_points[i+extra][j][2] = /*z_values[i]*/ pz[i] + circle[j].z;
			}
		}

		return all_points;
	}

	/** From my former program, A_3D_Editing.java and Pipe.java */
	static private Vector3 rotate_v_around_axis(final Vector3 v, final Vector3 axis, final double sin, final double cos) {

		final Vector3 result = new Vector3();
		final Vector3 r = axis.normalize(axis);

		result.set((cos + (1-cos) * r.x * r.x) * v.x + ((1-cos) * r.x * r.y - r.z * sin) * v.y + ((1-cos) * r.x * r.z + r.y * sin) * v.z,
			   ((1-cos) * r.x * r.y + r.z * sin) * v.x + (cos + (1-cos) * r.y * r.y) * v.y + ((1-cos) * r.y * r.z - r.x * sin) * v.z,
			   ((1-cos) * r.y * r.z - r.y * sin) * v.x + ((1-cos) * r.y * r.z + r.x * sin) * v.y + (cos + (1-cos) * r.z * r.z) * v.z);

		/*
		result.x += (cos + (1-cos) * r.x * r.x) * v.x;
		result.x += ((1-cos) * r.x * r.y - r.z * sin) * v.y;
		result.x += ((1-cos) * r.x * r.z + r.y * sin) * v.z;

		result.y += ((1-cos) * r.x * r.y + r.z * sin) * v.x;
		result.y += (cos + (1-cos) * r.y * r.y) * v.y;
		result.y += ((1-cos) * r.y * r.z - r.x * sin) * v.z;

		result.z += ((1-cos) * r.y * r.z - r.y * sin) * v.x;
		result.z += ((1-cos) * r.y * r.z + r.x * sin) * v.y;
		result.z += (cos + (1-cos) * r.z * r.z) * v.z;
		*/
		return result;
	}

	public static final int enrangeInteger( float value, int mininum, int maximum ) {
		return Math.max( mininum, Math.min( maximum, (int) Math.round( value ) ) );
	}

	public static final int enrangeInteger( double value, int mininum, int maximum ) {
		return Math.max( mininum, Math.min( maximum, (int) Math.round( value ) ) );
	}

	public static Color3f [] getPointColors( double [] x_points,
						 double [] y_points,
						 double [] z_points,
						 Color3f flatColor,
						 ImagePlus colorImage ) {
		int n = x_points.length;
		Color3f [] result = new Color3f[x_points.length];
		Arrays.fill( result, flatColor );
		if( colorImage != null ) {
			ImageStack stack = colorImage.getStack();
			int width = colorImage.getWidth();
			int height = colorImage.getHeight();
			int depth = colorImage.getStackSize();
			int type = colorImage.getType();

			double x_spacing = 1;
			double y_spacing = 1;
			double z_spacing = 1;
			Calibration calibration = colorImage.getCalibration();
			if( calibration != null ) {
				x_spacing = calibration.pixelWidth;
				y_spacing = calibration.pixelHeight;
				z_spacing = calibration.pixelDepth;
			}

			java.awt.image.ColorModel cm = colorImage.getProcessor().getCurrentColorModel();

			byte [] reds = null;
			byte [] greens = null;
			byte [] blues = null;

			int mapSize = -1;

			if( cm != null && cm instanceof IndexColorModel ) {
				java.awt.image.IndexColorModel icm = (IndexColorModel)cm;
				mapSize = icm.getMapSize();
				reds = new byte[mapSize];
				greens = new byte[mapSize];
				blues = new byte[mapSize];
				icm.getReds(reds);
				icm.getGreens(greens);
				icm.getBlues(blues);
			}

			for( int i = 0; i < n; ++i ) {
				int x = (int)Math.round(x_points[i] / x_spacing);
				int y = (int)Math.round(y_points[i] / y_spacing);
				int z = (int)Math.round(z_points[i] / z_spacing);
				if( x < 0 )
					x = 0;
				else if( x >= width )
					x = width - 1;
				if( y < 0 )
					y = 0;
				else if( y >= height )
					y = height - 1;
				if( z < 0 )
					z = 0;
				else if( z >= depth )
					z = depth - 1;
				switch(type) {
				case ImagePlus.COLOR_RGB:
				{
					ColorProcessor cp = (ColorProcessor)stack.getProcessor(z+1);
					Color c = cp.getColor(x,y);
					result[i] = new Color3f(c);
				}
				break;
				case ImagePlus.GRAY8:
				{
					ByteProcessor bp = (ByteProcessor)stack.getProcessor(z+1);
					int v = bp.getPixel(x,y);
					if (cm == null) {
						float fv = v / 255.0f;
						result[i] = new Color3f(fv,fv,fv);
					} else {
						result[i] = new Color3f( (reds[v] & 0xFF) / 255.0f,
									 (greens[v] & 0xFF) / 255.0f,
									 (blues[v] & 0xFF) / 255.0f );
					}
				}
				break;
				case ImagePlus.COLOR_256:
				{
					ByteProcessor bp = (ByteProcessor)stack.getProcessor(z+1);
					int v = bp.getPixel(x,y);
					result [i] = new Color3f( (reds[v] & 0xFF) / 255.0f,
								  (greens[v] & 0xFF) / 255.0f,
								  (blues[v] & 0xFF) / 255.0f );
				}
				break;
				case ImagePlus.GRAY16:
				{
					ShortProcessor sp = (ShortProcessor)stack.getProcessor(z+1);
					int s = sp.getPixel(x,y);
					int v = enrangeInteger( (s - sp.getMin()) * 255.0f / (sp.getMax() - sp.getMin()),
								0,
								255 );
					result[i] = new Color3f( (reds[v] & 0xFF) / 255.0f,
								 (greens[v] & 0xFF) / 255.0f,
								 (blues[v] & 0xFF) / 255.0f );
				}
				break;
				case ImagePlus.GRAY32:
				{
					FloatProcessor fp = (FloatProcessor)stack.getProcessor(z+1);
					float f = fp.getf(x,y);
					int v = enrangeInteger( (f - fp.getMin()) * 255 / (fp.getMax() - fp.getMin()),
								0,
								255 );
					result[i] = new Color3f( (reds[v] & 0xFF) / 255.0f,
								 (greens[v] & 0xFF) / 255.0f,
								 (blues[v] & 0xFF) / 255.0f );
				}
				break;

				default:
					throw new RuntimeException("colorImage type: "+type+" is not supported yet");
				}
			}
		}
		return result;
	}
}
