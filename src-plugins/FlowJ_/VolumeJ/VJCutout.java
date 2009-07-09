package VolumeJ;
import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import volume.*;

/**
 * VJCutout. Implements cutouts: slice views in a volume rendering.
 *
 * Copyright (c) 1999-2002, Michael Abramoff. All rights reserved.
 * @author: Michael Abramoff
 *
 * Small print:
 * Permission to use, copy, modify and distribute this version of this software or any parts
 * of it and its documentation or any parts of it ("the software"), for any purpose is
 * hereby granted, provided that the above copyright notice and this permission notice
 * appear intact in all copies of the software and that you do not sell the software,
 * or include the software in a commercial package.
 * The release of this software into the public domain does not imply any obligation
 * on the part of the author to release future versions into the public domain.
 * The author is free to make upgraded or improved versions of the software available
 * for a fee or commercially only.
 * Commercial licensing of the software is available by contacting the author.
 * THE SOFTWARE IS PROVIDED "AS IS" AND WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY
 * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 */
public class VJCutout
{
        private VJMatrix 	        m, mi;
        private Volume 			v;
        private VJPlane 		x; // the cutout plane parallel to the x-axis
        private VJPlane 		y; // the cutout plane parallel to the y-axis
        private VJPlane 		z; // the cutout plane parallel to the z-axis
        private float[]		center; // the crossing of the 3 planes.
        private VJClassifier		classifier;
        private VJShader		shader;
        private VJInterpolator		interpolator;
        private int			xcutout, ycutout,zcutout;

        public VJCutout(Volume v, int xcutout, int ycutout, int zcutout,
                VJInterpolator interpolator, VJShader shader, VJClassifier classifier)
        /*
			Create 3 cutout planes that cross at xcutout, ycutout, zcutout.
			This means that 3 cutaway planes are created along constant x- y- and z-values
        */
        {
                this.m = m;
                this.mi = mi;
                this.v = v;
                this.classifier = classifier;
                this.shader = shader;
                this.interpolator = interpolator;
                this.xcutout = xcutout;
                this.ycutout = ycutout;
                this.zcutout = zcutout;
                if (! classifier.doesCutouts())
                {
                        VJUserInterface.error("Please select another classifier that can classify cutouts.");
                        return;
                }
                VJUserInterface.write("Cutout center at "+xcutout+", "+ycutout+", "+zcutout);
        }
        public void setup(VJMatrix m, VJMatrix mi)
        {
                this.m = m;
		this.mi = mi;
		float [] centeros = new float[4];
		float [] p2 = new float[4];
		float [] p3 = new float[4];
		// the x=c plane.
		centeros[0]  = xcutout; centeros[1] = ycutout; centeros[2] = zcutout; centeros[3] = 1;
		p2[0]  = xcutout; p2[1] = ycutout + 1; p2[2] = zcutout + 1; p2[3] = 1;
		p3[0]  = xcutout; p3[1] = ycutout - 1; p3[2] = zcutout + 1; p3[3] = 1;
		center = m.mul(centeros);
		// determine the plane-equation in viewspace.
		x = new VJPlane(center, m.mul(p2), m.mul(p3));
		// the y=c plane.
		p2[0]  = xcutout + 1; p2[1] = ycutout; p2[2] = zcutout + 1; p2[3] = 1;
		p3[0]  = xcutout - 1; p3[1] = ycutout; p3[2] = zcutout + 1; p3[3] = 1;
		// determine the plane-equation in viewspace.
		y = new VJPlane(center, m.mul(p2), m.mul(p3));
		// the z=c plane.
		p2[0]  = xcutout + 1; p2[1] = ycutout + 1; p2[2] = zcutout; p2[3] = 1;
		p3[0]  = xcutout - 1; p3[1] = ycutout + 1; p3[2] = zcutout; p3[3] = 1;
		// determine the plane-equation in viewspace.
		z = new VJPlane(center, m.mul(p2), m.mul(p3));
        }
        /**
         * This routine decides whether to show a cutout slice or a rendered surface
         * at pixel i,j. The routine is as follows:
         * Check whether ac contains a surface near vl (the opacity of ac > 0.5).
         * If there is a surface at or near vl,
         * the 3 intersections of the ray from i,j into the volume with
         * the x,y,z cutout-planes are determined.
         * -the surface is rendered if the closest intersection is beyond
         * the crossing of the cutout planes (as seen from the viewplane),
         * or if the the ray is parallel to any of the planes.
         * -the value at the intersection is interpolated, classified
         * and composed if the closest intersection is on this side
         * of the crossing.
         * You can optimize this by only calculating the intersection if ijk is closer
         * to the viewplane than the crossing of the cutout planes.
         */
        public void cutout(VJAlphaColor ac, int i, int j, int k)
        {
                if (ac.getAlpha() > 0.5)
                {
                        // first find the intersections.
                        float [] xis = new float[3];
                        xis[0] = (float) i;
                        xis[1] = (float) j;
                        xis[2] = x.intersectRay(i, j);
                        float [] yis = new float[3];
                        yis[0] = (float) i;
                        yis[1] = (float) j;
                        yis[2] = y.intersectRay(i, j);
                        float [] zis = new float[3];
                        zis[0] = (float) i;
                        zis[1] = (float) j;
                        zis[2] = z.intersectRay(i, j);
                        float [] nearest = findNearestIntersection(xis, yis, zis);
                        // Not parallel to any plane?
                        if (nearest != null)
                        {
                                // are we currently in front of or behind the nearest plane.
                                if (k < nearest[2] && nearest[2] <= center[2])
                                {
                                        if (IJ.debugMode)
                                                ij.IJ.write("nearest intersection "+
                                                        nearest[0]+","+nearest[1]+","+nearest[2]);
                                        // in front of the plane: show the value at the intersection.
                                        // determine the voxellocation of the intersection:
                                        float [] vos = mi.mul(nearest);
                                        VJVoxelLoc vl = new VJVoxelLoc(vos);
					VJValue value = new VJValue();
                                        // Get the interpolated voxel value or values (vector).
                                        interpolator.value(value, v, vl);
                                        // classify the value as a slice.
                                        VJAlphaColor nac = classifier.alphacolor(value);
                                        // shade the plane.
                                        VJGradient g;
                                        if (nearest == xis)
                                                g = x.getGradient();
                                        else if (nearest == yis)
                                                g = y.getGradient();
                                        else
                                                g = z.getGradient();
                                        nac.attenuate(shader.shade(g));
                                        // Make the color definitive.
                                        ac.copy(nac);
                                }
                                // behind the plane: continue rendering the surface.
                        }
                        // parallel to any plane: render surface.
                }
                // not yet a surface.
        }
        private float[] findNearestIntersection(float [] is1, float [] is2, float []is3)
        {
                float [] nearest = is1;
                if (nearest == null || (is2 != null && is2[2] < nearest[2]))
                        nearest = is2;
                if (nearest == null || (is3 != null && is3[2] < nearest[2]))
                        nearest = is3;
                return nearest;
        }
}

