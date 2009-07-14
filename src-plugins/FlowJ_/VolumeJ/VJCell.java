package VolumeJ;
import volume.*;

/**
 * VJCell.
 * For patenting and copyrighting reasons all Javadoc comments have been removed.
 *
 * Copyright (c) 2001-2003, Michael Abramoff. All rights reserved.
 * Patent pending.
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
public class VJCell
{
        /** Position of this cell in objectspace coordinates. */
        public int ix, iy, iz;
        /**
         * The faces (in the form of plane equations) for the base coordinates of this cell,
         * sorted by magnitude of their normal vector. Parallel to the viewing ray have been eliminated.
         */
        private VJPlane [] vsface;
        // The shift in D of the plane equation for a shift in x,y,z in objectspace.
        private float [] planeShiftD;
        // The coordinate along which the face plane lies for all planes in vsface.
        private int [] planeNormalAxis;
        // The anterior and posterior (kmin + kdelta) extent of the basis of this cell in the k-dimension
        private float kminbase, kdelta, jminbase, jdelta, iminbase, idelta;
        // Increases in viewspace coordinates (for i,j,k) on increase in objectspace coordinates (x,y,z).
        private float [] iinc; float [] jinc; float [] kinc;
        // Error boundary in float.      //012345678901234567890
        //static final private float ERR = 0.0000000001;
        // Error boundary in float.              //012345678901234567890
        static final private float ERR = (float) 0.00001;
        // VJMatrix only needed for debugging.
        private VJMatrix m, mi;
        /**
        * A cell is the unit of a volume in isosurface rendering
        * A cell consists of eight voxels,
        * and has the same position as its lowerleftanterior voxel.
        * This class defines methods to convert voxel positions to cell positions and vv.
        */
        public VJCell(VJMatrix m, VJMatrix mi)
        {
                /**
                 * Define a cell, including its faces in the coordinate system defined by m.
                 * A cell itself is always in objectspace coordinates.
                 * @param m the transformation matrix to go from objectspace to viewspace coordinates.
                 * @param mi the inverse of the transformation matrix.
                 */
                ix = 0; iy = 0; iz = 0;// Compute faces on instantiation.
                this.m = m;
                this.mi = mi;
                precomputeFaces(m);
        }
        public void move(float ox, float oy, float oz)
        {
                /**
                * Move this cell to enclose osv.
                * Correct for slight floating point inaccuracies (ERR).
                * The position of a cell is always in objectspace coordinates,
                * and always an integer position on the grid.
                * @param osv the location to move the cell to.
                */
                ix = (int) (ox >= 0 ? (ox + ERR) : (ox - 1 - ERR));
                iy = (int) (oy >= 0 ? (oy + ERR) : (oy - 1 - ERR));
                iz = (int) (oz >= 0 ? (oz + ERR) : (oz - 1 - ERR));
        }
        /**
         * Compute the plane equations for the (six) faces of this cell.
         * @param m the transformation matrix to convert objectspace into viewspace coordinates.
         */
        private void precomputeFaces(VJMatrix m)
        {
                try{
                /**
                 * These computations are highly factored to increase the efficiency of shifting
                * a plane if the cell is moved. This way, the computation of intersections with the ray is
                * sped up (this is the most time consuming part of the rendering process).
                * Therefore this code is quite complex at first sight.
                * The faces are defined in viewspace (because the ray is defined in viewspace),
                * and can be shifted easily, based on a shift in objectspace, which is what happens
                * when you traverse the interesting cells.
                * @param m the transformation matrix (from object to viewspace).
                */
                float [][] vsvertex = transformVertices(m, vertices(0, 0, 0));
                VJPlane [] allfaces = faces(vsvertex);
                // Increase in viewspace coordinates (i,j,k) on step in objectspace coordinates (x=0,y=1,z=2).
                iinc = new float[3];
                jinc = new float[3];
                kinc = new float[3];
                iinc[0] = vsvertex[1][0] - vsvertex[0][0];
                iinc[1] = vsvertex[2][0] - vsvertex[0][0];
                iinc[2] = vsvertex[4][0] - vsvertex[0][0];
                jinc[0] = vsvertex[1][1] - vsvertex[0][1];
                jinc[1] = vsvertex[2][1] - vsvertex[0][1];
                jinc[2] = vsvertex[4][1] - vsvertex[0][1];
                kinc[0] = vsvertex[1][2] - vsvertex[0][2];
                kinc[1] = vsvertex[2][2] - vsvertex[0][2];
                kinc[2] = vsvertex[4][2] - vsvertex[0][2];

                // Find the extents of the 0,0,0 cell in viewspace along the k (2nd) -axis.
                float [] kextents = extents(vsvertex, 2);
                // The cell vertex with the lowest k value in viewspace. This is the anterior limit on k.
                kminbase = kextents[0];
                // The distance to the highest k value in viewspace in this cell. This is the posterior limit on k.
                kdelta = kextents[1] - kminbase;
                // Find the extents of the 0,0,0 cell in viewspace along the j-axis.
                float [] jextents = extents(vsvertex, 1);
                // The cell vertex with the lowest k value in viewspace. This is the anterior limit on j.
                jminbase = jextents[0];
                // The distance to the highest k value in viewspace in this cell. This is the posterior limit on j.
                jdelta = jextents[1] - jminbase;
                // Find the extents of the 0,0,0 cell in viewspace along the i-axis.
                float [] iextents = extents(vsvertex, 0);
                // The cell vertex with the lowest i value in viewspace. This is the anterior limit on i.
                iminbase = iextents[0];
                // The distance to the highest i value in viewspace in this cell. This is the posterior limit on i.
                idelta = iextents[1] - iminbase;

                // Determine the effect a move of a single cell in x, y and z directions has
                // on the plane equations.
                // So, you can rapidly shift the planes of a cell that has been moved to
                // any location in objectspace.

                // Get the magnitude of the change in the D's of the plane
                // equations corresponding to a one cell shift in x (left face), y (bottom)
                // and z (front) direction, of the position of the cell in OBJECT space.
                float [] dD = new float[3];
                // The difference in D between the left and right face (aka a x shift in OS)
                dD[0] = allfaces[1].getD() - allfaces[0].getD();
                // The difference in D between the bottom and top face (aka a y shift in OS)
                dD[1] = allfaces[3].getD() - allfaces[2].getD();
                // The difference in D between the front and back face (aka a z shift in OS)
                dD[2] = allfaces[5].getD() - allfaces[4].getD();
                // The plane equations for the left, bottom and
                // front faces suffice, since you can compute the right, top and back planes
                // easily by adding dD[0], dD[1] and dD[2] to them.
                // Sort the planes by length of their normal vector in the k-dimension, largest normal first.
                VJPlane [] vsfaceSorted = new VJPlane[3];
                float [] planeShiftDSorted = new float[3];
                int [] planeNormalAxisSorted = new int[3];
                if (Math.abs(allfaces[0].getC()) > Math.abs(allfaces[2].getC()) &&
                        Math.abs(allfaces[2].getC()) > Math.abs(allfaces[4].getC()))
                {
                        vsfaceSorted[0] = allfaces[0]; planeShiftDSorted[0] = dD[0]; planeNormalAxisSorted[0] = 0; //x
                        vsfaceSorted[1] = allfaces[2]; planeShiftDSorted[1] = dD[1]; planeNormalAxisSorted[1] = 1; //y
                        vsfaceSorted[2] = allfaces[4]; planeShiftDSorted[2] = dD[2]; planeNormalAxisSorted[2] = 2; //z
                }
                else if (Math.abs(allfaces[0].getC()) > Math.abs(allfaces[2].getC()) &&
                        Math.abs(allfaces[2].getC()) < Math.abs(allfaces[4].getC()))
                {
                        vsfaceSorted[0] = allfaces[4]; planeShiftDSorted[0] = dD[2]; planeNormalAxisSorted[0] = 2; //z
                        vsfaceSorted[1] = allfaces[0]; planeShiftDSorted[1] = dD[0]; planeNormalAxisSorted[1] = 0; //x
                        vsfaceSorted[2] = allfaces[2]; planeShiftDSorted[2] = dD[1]; planeNormalAxisSorted[2] = 1; //y
                }
                else
                {
                        vsfaceSorted[0] = allfaces[4]; planeShiftDSorted[0] = dD[2]; planeNormalAxisSorted[0] = 2; //z
                        vsfaceSorted[1] = allfaces[2]; planeShiftDSorted[1] = dD[1]; planeNormalAxisSorted[1] = 1; //y
                        vsfaceSorted[2] = allfaces[0]; planeShiftDSorted[2] = dD[0]; planeNormalAxisSorted[2] = 0; //x
                }
                // Cull all planes parallel to the coordinate system.
                // This code may seem redundant but is not, because of floating point rounding.
                // Determine number of non-parallel planes.
                int n = 0;
                for (int i = 0; i < vsfaceSorted.length; i++)
                        if (vsfaceSorted[i].getC() != 0)
                                n++;
                // Now create arrays of the right size.
                vsface = new VJPlane[n];
                planeShiftD = new float[n];
                planeNormalAxis = new int[n];
                // Select all non-parallel planes.
                for (int i = 0, j = 0; i < vsfaceSorted.length && j < n; i++)
                        if (vsfaceSorted[i].getC() != 0)
                        {
                                planeShiftD[j] = planeShiftDSorted[i];
                                planeNormalAxis[j] = planeNormalAxisSorted[i];
                                vsface[j++] = vsfaceSorted[i];
                        }
                }
                catch (Exception e) { VJUserInterface.write("error: "+e); }
        }
        public float [] intersect(int i, int j, int kdummy)
        {
                /**
                 * Intersect a ray through i,j with the faces of this cell.
                 * The ray always has derivative 0,0,1 (i.e. leads off into k space)
                 * In some viewing directions, there may be more than 2 intersections because of floating
                 * point inaccuracies at corners and edges. Simply take the first two then.
                 * Since the faces are sorted most perpendicular to ray first, you will find the
                 * intersections with the most perpendicular faces.
                 * The ERR value is important. If it is too small, floating point inaccuracies will deteriorate rendering at
                 * small scales. If it is too large, rendering speed will decrease.
                 * @param i, j, k a point on the ray (which has derivative 0,0,1). k is not used.
                 * @return a float[2] with two different intersection points on the ray in viewspace coordinates (k=space),
                 * null if no intersection or only one intersections were found within this cell.
                 *
                 */
                float [] intersection = new float[2];
                int intersections = 0;
                // First check whether the ray falls within the bounds of this cell.
                // Determine range for i for this cell.
                /*
                float imin = (float) (iminbase + iinc[0] * ix + iinc[1] * iy + iinc[2] * iz);
                float imax = imin + (float) idelta;
                // Sanity check for ray traversal algorithm.
                if (! between(i, imin, imax, ERR))
                {
                        VJUserInterface.write("i fails");
                        return null;
                }
                // Got through i coordinate sanity check, now determine range for j for this cell.
                float jmin = (float) (jminbase + jinc[0] * ix + jinc[1] * iy + jinc[2] * iz);
                float jmax = jmin + (float) jdelta;
                if (! between(j, jmin, jmax, ERR))
                {
                        VJUserInterface.write("j fails");
                        return null;
                }
                */
                // Set p to this cell's coordinates, with coordinates sorted by face plane order.
                int [] p = new int[3];
                p[0] = ix; p[1] = iy; p[2] = iz;
                // Compute true k coordinate.
                float kmin = (float) (kminbase + kinc[0] * ix + kinc[1] * iy + kinc[2] * iz);
                float kmax = kmin + (float) kdelta;
                for (int f = 0; f < vsface.length && intersections < 2; f++)
                {
                        float ai = (float) vsface[f].getA() * (float) i;
                        float bj = (float) vsface[f].getB() * (float) j;
                        float c = (float) vsface[f].getC();
                        // Compute the D of the plane equation for the first face.
                        // The increase in D by the shift of this cell from base along the axes of this plane.
                        float d = (float) vsface[f].getD() + (float) p[planeNormalAxis[f]] * (float) planeShiftD[f];
                        // Intersect face-plane with a line from i,j,0 with derivative 0,0,1 (in viewspace).
                        float k = - (ai + bj + d) / c;
                        // Check whether the intersection at i0,j0,k lies within this cell but is different from the other.
                        boolean isInside = between(k, kmin, kmax, ERR);
                        boolean isNotSame = (intersections > 0 && ! between(k, intersection[0], intersection[0], ERR)) || (intersections == 0);
                        if (isInside && isNotSame)
                                // Yes, keep the intersection.
                                intersection[intersections++] = k;
                        // Now for the plane parallel to the first plane (a distance of one planeShiftD).
                        // Compute the intersection of plane ax+by+cz+D = 0 with ray vsi,vsj,1 to 0,0,1
                        if (intersections < 2)
                        {
                                // Shift the D to the other side of this cell.
                                d += planeShiftD[f];
                                k = - (ai + bj + d) / c;
                                // Check whether the intersection at i0,j0,k lies within this cell.
                                isInside = between(k, kmin, kmax, ERR);
                                isNotSame = (intersections > 0 && ! between(k, intersection[0], intersection[0], ERR)) || (intersections == 0);
                                if (isInside && isNotSame)
                                        // Yes, keep the intersection.
                                        intersection[intersections++] = k;
                        }
                }
                if (intersections == 2)
                        return intersection;
                else
                        return null;
        }
        String s;
        public float [] intersectTracing(int i, int j, int kdummy)
        {
                /**
                 * The ray has derivative 0,0,1.
                 * In some viewing directions, there may be more than 2 intersections because of floating
                 * point inaccuracies at corners and edges. Simply take the first two then.
                 * Since the faces are sorted most perpendicular to ray first, you will find the
                 * intersections with the most perpendicular faces.
                 */
                float [] intersection = new float[2];
                int intersections = 0;
                // First check whether the ray falls within the bounds of this cell.
                // Determine range for i for this cell.
                float imin = (float) (iminbase + iinc[0] * ix + iinc[1] * iy + iinc[2] * iz);
                float imax = imin + (float) idelta;
                s="_____\n";
                // Sanity check for ray traversal algorithm.
                if (! between(i, imin, imax, ERR))
                {
                        s="i not within cell: "+i+","+j+" imin, imax "+imin+","+imax+" iinc[] "+iinc[0]+","+iinc[1]+","+iinc[2]+" iminbase "+iminbase+" idelta "+idelta;
                        /*
                        VJMatrix mi = m.inverse();
                        float [] v = VJMatrix.newVector(ix, iy, iz);
                        float [] vt = m.mul(v);
                        s+="\nposition of this cell in viewspace: "+vt[0]+","+vt[1]+","+vt[2];
                        v = VJMatrix.newVector(vsi, vsj, kmin);
                        vt = mi.mul(v);
                        s+="\ncurrent pos (i,j,kmin) in object space: "+vt[0]+","+vt[1]+","+vt[2];
                        v = VJMatrix.newVector(vsi, vsj, kmax);
                        vt = mi.mul(v);
                        s+="\nup to (i,j,kmax) in object space: "+vt[0]+","+vt[1]+","+vt[2];
                        v = VJMatrix.newVector(imin, jmin, kmin);
                        vt = mi.mul(v);
                        s+="\n(imin,jmin,kmin) in object space: "+vt[0]+","+vt[1]+","+vt[2];
                        v = VJMatrix.newVector(imin, jmin, kmax);
                        vt = mi.mul(v);
                        s+="\n(imin,jmin,kmax) in object space: "+vt[0]+","+vt[1]+","+vt[2];
                        v = VJMatrix.newVector(imax, jmin, kmin);
                        vt = mi.mul(v);
                        s+="\n(imax,jmin,kmin) in object space: "+vt[0]+","+vt[1]+","+vt[2];
                        v = VJMatrix.newVector(imax, jmin, kmax);
                        vt = mi.mul(v);
                        s+="\n(imax,jmin,kmax) in object space: "+vt[0]+","+vt[1]+","+vt[2];
                        */
                        return null;
                }
                // Got through i coordinate sanity check, now determine range for j for this cell.
                float jmin = (float) (jminbase + jinc[0] * ix + jinc[1] * iy + jinc[2] * iz);
                float jmax = jmin + (float) jdelta;
                if (! between(j, jmin, jmax, ERR))
                {
                        s="j not within cell: "+i+","+j+" jmin, jmax "+jmin+","+jmax+" iinc[] "+iinc[0]+","+iinc[1]+","+iinc[2]+" iminbase "+iminbase+" idelta "+idelta;
                        return null;
                }
                // Set p to this cell's coordinates, with coordinates sorted by face plane order.
                int [] p = new int[3];
                p[0] = ix; p[1] = iy; p[2] = iz;
                // Got through i and j sanity checks, now compute k coordinate.
                float kmin = (float) (kminbase + kinc[0] * ix + kinc[1] * iy + kinc[2] * iz);
                float kmax = kmin + (float) kdelta;
                for (int f = 0; f < vsface.length && intersections < 2; f++)
                {
                        float ai = (float) vsface[f].getA() * (float) i;
                        float bj = (float) vsface[f].getB() * (float) j;
                        float c = (float) vsface[f].getC();
                        // Compute the D of the plane equation for the first face.
                        // The increase in D by the shift of this cell from base along the axes of this plane.
                        float d = (float) vsface[f].getD() + (float) p[planeNormalAxis[f]] * (float) planeShiftD[f];
                        // Intersect face-plane with a line from i,j,0 with derivative 0,0,1 (in viewspace).
                        float k = - (ai + bj + d) / c;
                        // Check whether the intersection at i0,j0,k lies within this cell.
                        boolean isInside = between(k, kmin, kmax, ERR);
                        boolean isNotSame = (intersections > 0 && ! between(k, intersection[0], intersection[0], ERR)) || (intersections == 0);
                        if (isInside && isNotSame)
                        {
                                // Yes, keep the intersection.
                                intersection[intersections++] = k;
                                s+= "\nIntersection found ("+intersections+"): k="+k+" kmin,kmax: {"+kmin+"-"+kmax+"}"+" ai,bj,c,d: "+ai+" "+bj+" "+c+" "+d+" ray: "+i+","+j;
                                //VJMatrix mi = m.inverse();
                                //float [] v = VJMatrix.newVector(vsi, vsj, k);
                                //float [] vt = mi.mul(v);
                                //s+="\ninters object space: "+vt[0]+","+vt[1]+","+vt[2];
                                //s += toStringFaces();
                        }
                        else
                        {
                                s+="\nFailed intersection ("+intersections+"): k="+k+" not within cell "+ix+","+iy+","+iz+", ray="+i+","+j+", k="+k+" {"+kmin+" - "+kmax+"} kinc[] "+kinc[0]+","+kinc[1]+","+kinc[2]+" kminbase "+kminbase+" kdelta "+kdelta;
                                s+=" f="+f+" planeNormalAxis[f] "+planeNormalAxis[f]+", p[planeNormalAxis[f]]="+p[planeNormalAxis[f]]+" planeShiftD[f]="+planeShiftD[f]+"\n";
                                //s+="a="+a+", b="+b+", c="+c+", vsface[f].getD()="+vsface[f].getD()+", d="+d+"\n";
                                float [] vt = m.inverse().mul(VJMatrix.newVector(i, j, k));
                                s+="k="+k+", in object space: "+vt[0]+","+vt[1]+","+vt[2]+"\n";
                                //VJPlane [] face = faces(transformVertices(m, vertices(ix, iy, iz)));
                                //s += "face planes computed directly from cell coords:\n"+face[0]+"\n"+face[1]+"\n"+face[2]+"\n"+face[3]+"\n"+face[4]+"\n"+face[5]+"\n";
                                s += toStringFaces();
                        }
                        // Now for the second plane, parallel to the first plane (a distance of one planeShiftD).
                        // Compute the intersection of plane ax+by+cz+D = 0 with ray ray.i,ray.j,1 to 0,0,1
                        if (intersections < 2)
                        {
                                // Shift the D to the other side of this cell.
                                d += planeShiftD[f];
                                k = - (ai + bj + d) / c;
                                // Check whether the intersection at i0,j0,k lies within this cell.
                                isInside = between(k, kmin, kmax, ERR);
                                isNotSame = (intersections > 0 && ! between(k, intersection[0], intersection[0], ERR)) || (intersections == 0);
                                if (isInside && isNotSame)
                                {
                                        // Yes, keep the intersection.
                                        intersection[intersections++] = k;
                                        s+= "\nIntersection found ("+intersections+"): k="+k+" kmin,kmax: {"+kmin+"-"+kmax+"}"+" ai,bj,c,d: "+ai+" "+bj+" "+c+" "+d+" ray: "+i+","+j;
                                        //VJMatrix mi = m.inverse();
                                        //float [] v = VJMatrix.newVector(vsi, vsj, k);
                                        //float [] vt = mi.mul(v);
                                        //s+="\ninters object space: "+vt[0]+","+vt[1]+","+vt[2];
                                        //s += toStringFaces();
                                }
                                else
                                {
                                        s+="\nFailed intersection ("+intersections+"): k="+k+" not within cell "+ix+","+iy+","+iz+", ray="+i+","+j+", k="+k+" {"+kmin+" - "+kmax+"} kinc[] "+kinc[0]+","+kinc[1]+","+kinc[2]+" kminbase "+kminbase+" kdelta "+kdelta;
                                        s+=" f="+f+" planeNormalAxis[f] "+planeNormalAxis[f]+", p[planeNormalAxis[f]]="+p[planeNormalAxis[f]]+" planeShiftD[f]="+planeShiftD[f]+"\n";
                                        //s+="a="+a+", b="+b+", c="+c+", vsface[f].getD()="+vsface[f].getD()+", d="+d+"\n";
                                        float [] vt = m.inverse().mul(VJMatrix.newVector(i, j, k));
                                        s+="k="+k+", in object space: "+vt[0]+","+vt[1]+","+vt[2]+"\n";
                                        //VJPlane [] face = faces(transformVertices(m, vertices(ix, iy, iz)));
                                        //s += "face planes computed directly from cell coords:\n"+face[0]+"\n"+face[1]+"\n"+face[2]+"\n"+face[3]+"\n"+face[4]+"\n"+face[5]+"\n";
                                        s += toStringFaces();
                                }
                        }
                }
                if (intersections == 1)
                        intersection[intersections++] = intersection[0];
                if (intersections == 2)
                        return intersection;
                else
                        return null;
        }
        /**
         * Transform the vertex coordinates of a cell into a different coordinate system.
         * The matrix of coordinates is transformed using transformation matrix m.
         * @param m the transformation matrix to convert coordinates from one system to another.
         * @param vertex [8][4] float the homogenuous coordinates of all vertices of a cell.
         * @return a float[8][4] containing the transformed coordinates of all vertices (same order as param vertex).
         */
        public static float [][] transformVertices(VJMatrix m, float [][] vertex)
        {
                // Cell vertices in viewspace.
                float [][] tvertex = new float[vertex.length][vertex[0].length];
                for (int i = 0; i < vertex.length; i++)
                        tvertex[i] = m.mul(vertex[i]);       // lbf
                //VJUserInterface.write(" Transformed vertices for "+x+","+y+","+z);
                return tvertex;

        }
        /**
         * Compute the vertex coordinates of a cell at x, y, z.
         * @param x, y, z the position of a cell in objectspace.
         */
        public static float [][] vertices(int x, int y, int z)
        {
                // Cell vertices in homogenous objectspace coordinates.
                float [][] vertex = new float[8][4];
                vertex[0][0]=x+0; vertex[0][1]=y+0; vertex[0][2]=z+0; vertex[0][3]=1; // lbf
                vertex[1][0]=x+1; vertex[1][1]=y+0; vertex[1][2]=z+0; vertex[1][3]=1; // rbf
                vertex[2][0]=x+0; vertex[2][1]=y+1; vertex[2][2]=z+0; vertex[2][3]=1; // ltf
                vertex[3][0]=x+1; vertex[3][1]=y+1; vertex[3][2]=z+0; vertex[3][3]=1; // rtf
                vertex[4][0]=x+0; vertex[4][1]=y+0; vertex[4][2]=z+1; vertex[4][3]=1; // lbb
                vertex[5][0]=x+1; vertex[5][1]=y+0; vertex[5][2]=z+1; vertex[5][3]=1; // rbb
                vertex[6][0]=x+0; vertex[6][1]=y+1; vertex[6][2]=z+1; vertex[6][3]=1; // ltb
                vertex[7][0]=x+1; vertex[7][1]=y+1; vertex[7][2]=z+1; vertex[7][3]=1; // rtb
                return vertex;
        }
        /**
         * Compute the face plane equations of all planes bounding a cell from its vertices.
         * @param vertex a [][]float containing the vertices of the cell.
         * The order of the vertices must be as in vertices().
         * @return an array of 6 VJPlane plane equations, ordered left,right,bottom,top,front,back.
         */
        private VJPlane [] faces(float [][] vertex)
        {
                // The face planes of this cell in viewspace coordinates.
                VJPlane [] face = new VJPlane[6];
                // left face.
                face[0] =  new VJPlane(vertex[0], vertex[2], vertex[4]);
                // right face.
                face[1] = new VJPlane(face[0], vertex[1]);
                // bottom face.
                face[2] = new VJPlane(vertex[0], vertex[1], vertex[4]);
                // top face.
                face[3] = new VJPlane(face[2], vertex[2]);
                // front face.
                face[4] = new VJPlane(vertex[0], vertex[1], vertex[2]);
                // back face.
                face[5] = new VJPlane(face[4], vertex[4]);
                return face;
        }
        /**
         * Find the minimum and maximum of the extents of a cell represented by its vertices.
         * @param vertex a [][]float with the vertices in the order as in vertices().
         * @param dim an int that is the dimension in which you want the extents.
         * @return a float[2] with the minimum resp. maximum extent for dim.
         */
        private static float [] extents(float [][] vertex, int dim)
        {
                float [] extents = new float[2];
                // The vertex with the lowest k value in viewspace.
                float [] minvertex = VJMatrix.getMin(vertex, dim);
                extents[0] = minvertex[dim];
                float [] maxvertex = VJMatrix.getMax(vertex, dim);
                extents[1] = maxvertex[dim];
                //VJUserInterface.write("cell ("+l+") limitations:\n"+"{"+extents[0]+" - "+extents[1]+"}");
                return extents;
        }
        public String toString()
        {
                String s=" cell "+ix+","+iy+","+iz;
                return s;
        }
        /**
         * Format a String from the equations of all faces in this cell.
         */
        public String toStringFaces()
        {
                String s = "Cell face plane eqs in viewspace coordinates:\n";
                int [] p = new int[3];
                p[planeNormalAxis[0]] = ix;
                if (planeNormalAxis.length > 1)
                        p[planeNormalAxis[1]] = iy;
                if (planeNormalAxis.length > 2)
                        p[planeNormalAxis[2]] = iz;
                for (int f = 0; f < vsface.length; f++)
                {
                        float a = vsface[f].getA();
                        float b = vsface[f].getB();
                        float c = vsface[f].getC();
                        float d = vsface[f].getD() + p[planeNormalAxis[f]] * planeShiftD[f];
                        s += ""+a+"x +"+b+"y +"+c+"z +"+d+" = 0\n";
                        d += planeShiftD[f];
                        s += ""+a+"x +"+b+"y +"+c+"z +"+d+" = 0\n";
                }
                if (vsface.length < 3)
                        s += " remaining "+((3-vsface.length)*2)+" faces are parallel to viewing vector\n";
                return s;
        }
        /**
         * Format a String from the extents of this cell.
         * @return a String with the extents of this cell in standard order.
         */
        public String toStringVertices(float [][] vertex)
        {
                String s = "extents of this cell in chosen coordinate system:\n";
                for (int i = 0; i < vertex.length; i++)
                {
                        s+=i+": ";
                        for (int j = 0; j < 3; j++)
                                s += vertex[i][j] + ",";
                        s+=" - ";
                }
                return s;
        }
        /**
         * Format a String of the contents of this cell, als include the volume values at the vertex of this cell.
         * Are printed in order:
         *
         *      tlf     trf     tlb     trb
         *      blf     brf     blb     brb
         */
        public String toString(Volume v)
        {
                String s = toString();
                s += "\nvertex voxels:\n";
                for (int y = 1; y >= 0; y--)
                {
                        for (int z = 0; z < 2 ; z++)
                        {
                                for (int x = 0; x < 2; x++)
                                {
                                        Object o = v.get(ix+x, iy+y, iz+z);
                                        s += o.toString();
                                        s += " ";
                                }
                                s+= "   ";
                        }
                        s+="\n";
                }
                return s;
        }
        /**
         * Return whether d is within an interval around dlower and dupper.
         * "Around" is expressed by an error boundary epsilon.
         * @param d the value
         * @param dlower the lower bound
         * @param dupper the upper bound
         * @param epsilon the error margin of the interval
         */
        private boolean between(float d, float dlower, float dupper, float epsilon)
        { return (d >= (dlower - epsilon) && d <= (dupper + epsilon)); }
}
