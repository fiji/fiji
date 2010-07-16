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

	static public List generateTriangles(final double[][][] all_points, final double scale, List pointColorList, List vertexColorList ) {
		boolean outputColors = (pointColorList != null) && (vertexColorList != null);
		if( outputColors && vertexColorList.size() > 0 )
			throw new RuntimeException("vertexColorList in Pipe.generateTriangles() is only for output: should be empty");

		int n = all_points.length;
		final int parallels = all_points[0].length -1;
		List list = new ArrayList();
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

	static public double[][][] makeTube(double[] px, double[] py, double[] pz, double[] p_width_i, final int resample, final int parallels, final boolean do_resample, Color3f flatColor, ImagePlus colorImage, List outputColors) {

		boolean colorsSpecified = flatColor != null || colorImage != null;
		if( colorsSpecified ) {
			if( outputColors == null )
				throw new RuntimeException("If you specify flatColor or colorImage in a call to makeTube(), then outputColors must be non-null");
			if( outputColors.size() > 0 )
				throw new RuntimeException("The outputColors list in makeTube should be empty; it's for output, not input");
		}

		int n = px.length;

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

		Vector3 v3_P12;
		Vector3 v3_PR;
		Vector3[] circle = new Vector3[parallels+1];
		double sinn, coss;
		int half_parallels = parallels/2;
		for (int i=0; i<n-1; i++) {
			//System.out.println(i + " : " + px[i] + ", " + py[i] + ", " + pz[i]);
			//First vector: from one realpoint to the next
			//v3_P12 = new Vector3(p_i[0][i+1] - p_i[0][i], p_i[1][i+1] - p_i[1][i], z_values[i+1] - z_values[i]);
			v3_P12 = new Vector3(px[i+1] - px[i], py[i+1] - py[i], pz[i+1] - pz[i]);

			//Second vector: ortogonal to v3_P12, made by cross product between v3_P12 and a modifies v3_P12 (either y or z set to 0)

			//checking that v3_P12 has not z set to 0, in which case it woundn´t be different and then the cross product not give an ortogonal vector as output

			//chosen random vector: the same vector, but with x = 0;
			/* matrix:
				1 1 1		1 1 1				1 1 1				1 1 1
				v1 v2 v3	P12[0] P12[1] P12[2]		P12[0] P12[1] P12[2]		P12[0] P12[1] P12[2]
				w1 w2 w3	P12[0]+1 P12[1] P12[2]		P12[0]+1 P12[1]+1 P12[2]+1	P12[0] P12[1] P12[2]+1

			   cross product: v ^ w = (v2*w3 - w2*v3, v3*w1 - v1*w3, v1*w2 - w1*v2);

			   cross product of second: v ^ w = (b*(c+1) - c*(b+1), c*(a+1) - a*(c+1) , a*(b+1) - b*(a+1))
							  = ( b - c           , c - a             , a - b            )

			   cross product of third: v ^ w = (b*(c+1) - b*c, c*a - a*(c+1), a*b - b*a)
							   (b		 ,-a            , 0);
							   (v3_P12.y	 ,-v3_P12.x     , 0);


			Reasons why I use the third:
				-Using the first one modifies the x coord, so it generates a plane the ortogonal of which will be a few degrees different when z != 0 and when z =0,
				 thus responsible for soft shiftings at joints where z values change
				-Adding 1 to the z value will produce the same plane whatever the z value, thus avoiding soft shiftings at joints where z values are different
				-Then, the third allows for very fine control of the direction that the ortogonal vector takes: simply manipulating the x coord of v3_PR, voilà.

			*/

			// BELOW if-else statements needed to correct the orientation of vectors, so there's no discontinuity
			if (v3_P12.y < 0) {
				v3_PR = new Vector3(v3_P12.y, -v3_P12.x, 0);
				v3_PR = v3_PR.normalize(v3_PR);
				v3_PR = v3_PR.scale(p_width_i[i], v3_PR);

				//vectors are perfectly normalized and scaled
				//The problem then must be that they are not properly ortogonal and so appear to have a smaller width.
				//   -not only not ortogonal but actually messed up in some way, i.e. bad coords.

				circle[half_parallels] = v3_PR;
				for (int q=half_parallels+1; q<parallels+1; q++) {
					sinn = Math.sin(angle*(q-half_parallels));
					coss = Math.cos(angle*(q-half_parallels));
					circle[q] = rotate_v_around_axis(v3_PR, v3_P12, sinn, coss);
				}
				circle[0] = circle[parallels];
				for (int qq=1; qq<half_parallels; qq++) {
					sinn = Math.sin(angle*(qq+half_parallels));
					coss = Math.cos(angle*(qq+half_parallels));
					circle[qq] = rotate_v_around_axis(v3_PR, v3_P12, sinn, coss);
				}
			} else {
				v3_PR = new Vector3(-v3_P12.y, v3_P12.x, 0);           //thining problems disappear when both types of y coord are equal, but then shifting appears
				/*
				Observations:
					-if y coord shifted, then no thinnings but yes shiftings
					-if x coord shifted, THEN PERFECT
					-if both shifted, then both thinnings and shiftings
					-if none shifted, then no shiftings but yes thinnings
				*/

				v3_PR = v3_PR.normalize(v3_PR);
				if (null == v3_PR) {
					System.out.println("vp_3r is null: most likely a point was repeated in the list, and thus the vector has length zero.");
					return null;
				}
				v3_PR = v3_PR.scale(
						p_width_i[i],
						v3_PR);

				circle[0] = v3_PR;
				for (int q=1; q<parallels; q++) {
					sinn = Math.sin(angle*q);
					coss = Math.cos(angle*q);
					circle[q] = rotate_v_around_axis(v3_PR, v3_P12, sinn, coss);
				}
				circle[parallels] = v3_PR;
			}
			// Adding points to main array
			for (int j=0; j<parallels+1; j++) {
				all_points[i+extra][j][0] = /*p_i[0][i]*/ px[i] + circle[j].x;
				all_points[i+extra][j][1] = /*p_i[1][i]*/ py[i] + circle[j].y;
				all_points[i+extra][j][2] = /*z_values[i]*/ pz[i] + circle[j].z;
			}
		}
		for (int k=0; k<parallels+1; k++) {
			all_points[n-1+extra][k][0] = /*p_i[0][n-1]*/ px[n-1] + circle[k].x;
			all_points[n-1+extra][k][1] = /*p_i[1][n-1]*/ py[n-1] + circle[k].y;
			all_points[n-1+extra][k][2] = /*z_values[n-1]*/ pz[n-1] + circle[k].z;
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
				System.out.println("setting reds, greens and blues with map size: "+mapSize);
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
