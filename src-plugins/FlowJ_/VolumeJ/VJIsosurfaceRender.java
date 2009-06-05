package VolumeJ;
import java.awt.*;
import volume.*;
import ij.process.*;
import ij.ImagePlus;

/**
 * VJIsosurfaceRender.
 *
 * For patenting and copyrighting reasons all informative Javadoc comments have been removed.
 *
 * Copyright (c) 2001-2003, Michael Abramoff. All rights reserved.
 * @author: Michael Abramoff
 *
 * Note: this is not open source software!
 * These algorithms, source code, documentation or any derived programs ('the software')
 * are the intellectual property of Michael Abramoff.
 * Michael Abramoff asserts his right as the sole owner of the rights
 * to this software.
 * You and/or any person(s) acting with or for you may not:
 * - directly or indirectly copy, sell, lease, rent, license,
 * sublicense, redistribute, lend, give, transfer or otherwise distribute or
 * use the software
 * - modify, translate, or create derivative works from the software, assign or
 * otherwise transfer rights to the Software or use the Software for timesharing
 * or service bureau purposes
 * - reverse engineer, decompile, disassemble or otherwise attempt to discover the
 * source code or underlying ideas or algorithms of the Software or any subsequent
 * version thereof or any part thereof.
 * Commercial licensing of the software is available by contacting the author.
 * THE SOFTWARE IS PROVIDED "AS IS" AND WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY
 * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 */
public class VJIsosurfaceRender extends VJRenderer
{
	private static final float     EPSILON = 0.0001f;
	// The binary shell.
	private VJBinaryShell           shell;

	/**
	 * Create a new renderer with specified methods.
	 * @param interpolator the interpolator that will interpolate VJValues from the volume to be rendered.
	 * @param shader the VJShader that will be used for shading.
	 * @param classifier the VJClassifier that will be used for classifying.
	 * @throws IllegalArgumentException if parameters not properly defined.
	 */
	public VJIsosurfaceRender(VJInterpolator interpolator, VJShader shader, VJClassifier classifier)
	throws IllegalArgumentException
	{
		super(interpolator, shader, classifier);
	}
	/**
	 * The volume has changed. Reset the volume size and the binary shell.
	 * @param volume the new volume.
	 */
	public void setVolume(Volume v)
	{
		// The binary shell is computed to which cells are interesting. All other cells
		// are disregarded.
		this.v = v;
		// Create the binary shell containing the surface.
		VJThresholdedVolume tv = new VJThresholdedVolume(v, classifier);
		shell = new VJBinaryShell(tv);
		//(new ImagePlus("binary shell", shell.getImageStack())).show();
	}
	/**
	 *  Variables are all inherited from VJRenderer
	*/
	public synchronized void run()
	{
		/** Isosurface volume rendering is a rendering technique whereby the surfaces at a specified iso value
		* are rendered. Isosurface uses the concept of <code>cells</code>. Cells are cubes with vertices of 8
		* adjacent voxels.<br><br>
		* Implemented as follows: first, all cells containing a surface are selected (in a VJBinaryShell).
		* Then rays are cast from the viewplane into all cells in the shell. The intersections of the cell faces
		* with the ray are computed. If the ray intersects the surface (i.e. within the cell it has values both
		* over and under the isovalue), the precise position of the iso surface (i.e. where the sample has the same
		* value as the iso value) is searched (in this case, by bisection). The gradient of the interpolated voxel
		* on this point on the ray is the surface normal at this point and is rendered.<br>
		*
		* Renders v into pixels, the viewpane array.
		*
		*  Traverse the viewpane, pass rays through the volume, inspect cells whether they are in the shell or not,
		*  compute the intersection of the ray with the surface in the shell using bisection and
		*  regula falsi, check whether the intersection is in the cell,
		*  determine the gradient at the intersection point.
		*  Use the gradient to compute the shade for the ray and store in a 2-D z-buffer.
		*
		* Bug status:
		* - (Resolved) Not all views are free of vertical error lines. Not sure where that originates. kbuffer shosw them,
		* intersections are nout found at those lines. Occurs only in scale=1, at 60degree and 150degree rotations.
		* Must have to do with an error in computing the intersection with the cell faces.
		* Maybe rounding errors in cell faces? There are more errors like this if you increase ERR to 0.01. If you set
		* ERR to a very small value, that doesnt help, though.
		* Is okay in kbuffer, in kintersectbuffer the problem shows up.
		* - (Resolved) some vl.z are beyond volume boundaries. Checking for wrong values after bisection, helps,
		* but should not be necessary. Problem is somewhere in bisection or intersection.
		* - (Resolved) Rays can only be parallel to y-axes. Code is there for y-axis traversal but doesnt work.
				* - (Resolved) RGB volume rendering interpolation gives errors. The voxel hue and saturation should derive
				* from the surface that is being rendered. Proposal: use only over-threshold voxels to interpolate,
				* ignore all under threshold voxels.
		*/
		running = true;
		float iso = (float) classifier.getThreshold();
		//VJZBuffer kbuffer = new VJZBuffer(width, height);
		//VJZBuffer kintersectbuffer = new VJZBuffer(width, height);
		// The final rendering buffer.
		newViewportBuffer();
		// Create a place to store extra interpolated voxel information for color rendering.
		VJValue sample0 = null;
		if (outputType == COLORINT && v instanceof VolumeRGB)
		   sample0 = (VJValue) (new VJValueHSB());
		else
		   sample0 = new VJValue();
		VJValue sample1 = new VJValue();
		// Get the deltas to step along ray along i-axis, j-axis, k-axis, respectively, in objectspace.
		float [] istep = mi.getColumn(0);
		float [] jstep = mi.getColumn(1);
		float [] kstep = mi.getColumn(2);
		// Get the constants for stepping along ray.
		float [] cstep = mi.getColumn(3);
		// Compute vector orientation of ray.
		float dx = (depth-1)*kstep[0];
		float dy = (depth-1)*kstep[1];
		float dz = (depth-1)*kstep[2];
		// Initialize the current cell for this coordinate system.
		VJCell cell = new VJCell(m, mi);
		// Starting position of first ray in viewspace.
		float [] rayvs = VJMatrix.newVector(ioffset, joffset, koffset);
		// Find starting position of first ray in objectspace.
		float [] rayos = mi.mul(rayvs);
		// Set ray start constants, k position and offsets.
		float ckx = koffset * kstep[0] + joffset * jstep[0] + ioffset * istep[0] + cstep[0];
		float cky = koffset * kstep[1] + joffset * jstep[1] + ioffset * istep[1] + cstep[1];
		float ckz = koffset * kstep[2] + joffset * jstep[2] + ioffset * istep[2] + cstep[2];
		// Prepare the shell for casting rays with the direction of the ray (stays constant for this rendering view).
		shell.advancePrepare(dx, dy, dz);
		// Keep amount of time spent in loop for each viewspacepixel.
		long start = System.currentTimeMillis();
		// Traverse the viewpane and cast rays into the cells in the binary shell.
		for (int j = 0; j < height && running; j++) // step in j direction.
		{
			// Set ray start j position.
			float ox = ckx + j * jstep[0];
			float oy = cky + j * jstep[1];
			float oz = ckz + j * jstep[2];
			//if (j % 10 == 0) VJUserInterface.progress((float)j/(float) height);
			for (int i = 0; i < width; i++) // step in i direction
			{
				// Move cell to start of ray in object space.
				cell.move(ox, oy, oz);
				// Initialize the search for a surface along this ray starting at cell.
				int max = shell.advanceInit(cell, ox, oy, oz);
				// Advance the cell to the next surface, if any.
				while (true)
				{
					int k;
					if (onTrace(i, j)) { k = shell.advanceToSurfaceTracing(cell); trace("("+sequenceNumber+")"+i+","+j+": "+shell.s); } else
					k = shell.advanceToSurface(cell);
					if (k >= max)
						break;
					// Surface in this cell. Is cell available for interpolation?
					else if (interpolator.isValidGradient(cell, v))
					{
						//kbuffer.insert(k, i, j);
						// Find the two intersections of the ray (goes trough i,j)
						// with the faces of the cell.
						float [] kintersect;
						if (onTrace(i, j)) { kintersect = cell.intersectTracing(ioffset+i, joffset+j, koffset+k); trace("("+sequenceNumber+")"+i+","+j+": "+cell.s); } else
						kintersect = cell.intersect(ioffset+i, joffset+j, koffset+k);
						// If there are 2 intersections, locate surface between them
						if (kintersect != null)
						{
							//kintersectbuffer.insert(kintersect[0], i, j);
							// Take samples at kintersect[0] and kintersect[1].
							// Convert intersection (viewspace) to an objectspace VJVoxelLoc.
							VJVoxelLoc vlk0 = new VJVoxelLoc(ox + (kintersect[0] - rayvs[2])*kstep[0],
								oy + (kintersect[0] - rayvs[2])*kstep[1],
								oz + (kintersect[0] - rayvs[2])*kstep[2]);
							VJVoxelLoc vlk1 = new VJVoxelLoc(ox + (kintersect[1] - rayvs[2])*kstep[0],
								oy + (kintersect[1] - rayvs[2])*kstep[1],
								oz + (kintersect[1] - rayvs[2])*kstep[2]);
							// Saves about 50%.
							// Optimize: these calculations can be bilinear instead of trilinear.
							float k0sample = interpolator.value(sample0, v, vlk0).floatvalue;
							float k1sample = interpolator.value(sample1, v, vlk1).floatvalue;
							// Only estimate a surface if 2 samples bracket iso value.
							if (iso >= k0sample && iso <= k1sample || iso >= k1sample && iso <= k0sample)
							{
								// Determine the iso point cordinate
								// in object space, i.e. the root of the
								// surface equation between intersections k0 and k1.
								VJVoxelLoc vl = bisection(sample0, vlk0, iso, kintersect[0],
									kintersect[1],  k0sample, k1sample, kstep, 3);
								// Phong shading. Compute the interpolated gradient.
								VJGradient g = interpolator.gradient(v, vl);
								// Normalize the gradient.
								g.normalize();
								// Calculate the shade.
								VJShade shade = shader.shade(g);
								// Voxel brightness known.
								// For vector (RGB) rendering, also need actual color of voxel.
								int pixel;
								if (sample0 instanceof VJValueHSB)
								{
									VJValueHSB samplehsb = (VJValueHSB) sample0;
									// Get the interpolated hue and saturation voxel values at vl.
									interpolator.valueHS(samplehsb, (VolumeRGB) v, iso, vl);
									// Shade the hue and saturation to obtain final pixel.
									pixel = java.awt.Color.HSBtoRGB(samplehsb.getHue(),
										samplehsb.getSaturation(), shade.get());
								}
								else
									// Shade a white pixel.
									pixel = (int) (shade.get() * 255.0);
								setPixel(pixel, i, j);
								break;
							}
							else if (onTrace(i, j)) trace("("+sequenceNumber+") failed iso bracket at "+vlk0+","+vlk1+" samples "+k0sample+", "+k1sample);
						} // kintersect != null
					} // while. Continue looking for a surface.
				}  // k step.
				ox += istep[0];
				oy += istep[1];
				oz += istep[2];
			}	// i step
			yield();
		}	// j step
		VJUserInterface.progress(1f);
		traceWrite();
		pixelms = (float) (System.currentTimeMillis() - start) / (float) (width * height);
		running = false;
		//ImageProcessor imprk = new FloatProcessor(width, height, kbuffer.getBuffer(), null); (new ImagePlus("k values (cells with surfaces)"+description, imprk)).show();
		//ImageProcessor imprki = new FloatProcessor(width, height, kintersectbuffer.getBuffer(), null); (new ImagePlus("kintersect values (cells with 2 intersections)"+description, imprki)).show();
	}
	/**
	 * Do bisection for a number of steps.
	 * @param vlk0 the ray position at k0 in objectspace coordinates.
	 * @param iso the value of the isosurface
	 * @param k0, k1 the positions on the ray you want to bisect between.
	 * @param k1sample, k2sample the volume sample values at k1 and k2.
	 * @param kstep a float[4] with for x,y,z the changes in objectspace coordinates for a step in k-direction
	 * in viewspace.
	 * @param steps the number of bisection steps.
	 * @return a VJVoxelLoc postioned on the ray at the final bisection. This is the position
	 * on the ray where the volume sample is closest to iso.
	 */
	private VJVoxelLoc bisection(VJValue sample, VJVoxelLoc vlk0, float iso, float k0, float k1, float k0sample,
		float k1sample, float [] kstep, int steps)
	{
		// The direction from k0sample to k1sample.
		float basek0 = k0;
		float basek1 = k1;
		float base0sample = k0sample; float base1sample = k1sample;
		VJVoxelLoc vl = null;
		for (int s = 0; s < steps; s++)
		{
			// Precisize the sample by bisection.
			// Numerical Recipes in C, second edition.
			// pp 353-354.
			// Find midpoint.
			float m = k0 + (k1-k0)/2;
			// Determine the location in objectspace corresponding to m.
			float mdiff = m - basek0;
			vl = new VJVoxelLoc(vlk0.x + mdiff*(float)kstep[0],
				vlk0.y + mdiff*(float)kstep[1],
				vlk0.z + mdiff*(float)kstep[2]);
			// Sample at mdiff.
			float samplef = interpolator.value(sample, v, vl).floatvalue;
			// Find out which half is closer to iso.
			float dk0 = k0sample - iso;
			float dk1 = k1sample - iso;
			float diso = samplef - iso;
			if (diso < EPSILON) return vl; // Too close to iso to matter.
			if (dk0 * diso > 0.0) { k0sample = samplef; k0 = m; } // Does not bracket iso at k0 side.
			else if (dk1 * diso > 0.0) { k1sample = samplef; k1 = m; } // Does not bracket iso at k1 side.
			else if (Math.abs(dk0) >= Math.abs(dk1)) { k0sample = samplef; k0 = m; } // Bracketed, k1 is closer.
			else { k1sample = samplef; k1 = m; } // bracketed, k0 is closer.
		}
		float sampleDiff = (k1sample - k0sample);
		// Beware of underflow.
		if (Math.abs(sampleDiff) > 0.0)
		{
			// Precize the sample further by regula-falsi.
			// Numerical Recipes in C, second edition.
			// pp 358-359.
			float f = (iso - k0sample) / sampleDiff;
			float t = k0 + (k1 - k0) * f;
			float tshift = t - basek0;
			vl = new VJVoxelLoc(vlk0.x + tshift*(float)kstep[0],
				vlk0.y + tshift*(float)kstep[1],
				vlk0.z + tshift*(float)kstep[2]);
		}
		return vl;
	}
	public static String desc() { return "Isosurface"; }
}

