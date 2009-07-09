package VolumeJ;
import volume.*;
// Should be removed, still needs ImageStack and ImageProcessor.
import ij.*;
import ij.process.*;

/**
 * VJBInaryShell.
 * For patenting and copyrighting reasons all Javadoc comments have been removed.
 * This class implements a binary shell (created from a VJThresholdedVolume).
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
public class VJBinaryShell extends Volume
{
        /**
         * During instantiation, every cell (of 8 voxels) is checked to see whether this cell contains both a true and
         * a false thresholded voxel. If so, it is part of the binary shell of an object. If not, it is not interesting.
         * It is used as the shell volume input to a iso-surface renderer.
         * Also implements methods to traverse the shell.
        */
        // Cells, not voxels!
        public boolean []      	        v;

        public VJBinaryShell() {  }
        /**
         * Create a binary shell out of a a thresholded volume
         * @param vt the thresholded volume to be processed
         */
        public VJBinaryShell(VJThresholdedVolume vt)
        {
                depth = vt.getDepth()-1;
                height = vt.getHeight()-1;
                width = vt.getWidth()-1;
                int vtdepth = vt.getDepth();
                int vtheight = vt.getHeight();
                int vtwidth = vt.getWidth();
                v = new boolean[depth*height*width];
                for (int z = 0; z < depth; z++)
                for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                {
                        // Inspect the cell who has x,y,z as its lowerleftanterior corner.
                        boolean origin = vt.v[z*vtheight*vtwidth+y*vtwidth+x];
                        // Stack problems with cleaner expressions.
                        if (origin == vt.v[z*vtheight*vtwidth+y*vtwidth+(x+1)] &&
                                origin == vt.v[z*vtheight*vtwidth+(y+1)*vtwidth+(x)] &&
								origin == vt.v[z*vtheight*vtwidth+(y+1)*vtwidth+(x+1)] &&
    							origin == vt.v[(z+1)*vtheight*vtwidth+(y)*vtwidth+(x)] &&
    							origin == vt.v[(z+1)*vtheight*vtwidth+(y)*vtwidth+(x+1)] &&
    							origin == vt.v[(z+1)*vtheight*vtwidth+(y+1)*vtwidth+(x)] &&
    							origin == vt.v[(z+1)*vtheight*vtwidth+(y+1)*vtwidth+(x+1)])
                                 v[z*height*width+y*width+x] = false;
                        else
                                v[z*height*width+y*width+x] = true;
                }
        }
        /**
         * Get the shell value as a boolean.
         * @param x the x position of the voxel
         * @param y the y position of the voxel
         * @param z the z position of the voxel
         * @value a Number with 0 for false and 1 for true
         */
        public Object get(int x, int y, int z) { return new Boolean(v[z*height*width+y*width+x]); }
        /**
         * Set the shell value to a boolean value.
         * @param value a boolean
         * @param x the x position of the voxel
         * @param y the y position of the voxel
         * @param z the z position of the voxel
         */
        public void set(Object value, int x, int y, int z) { v[z*height*width+y*width+x] = ((Boolean) value).booleanValue(); }
        /**
         * Determine whether there is a surface in the cell that vl is in.
         * @param x,y,z the lowerleftanterior corner of the cell
         * @return true if the cell corresponding to x,y,z is part of a surface, false if not.
         */
        public boolean surface(VJVoxelLoc vl)
        {
                return surface(vl.iz, vl.iy, vl.ix);
        }
        /**
         * Determine whether cell contains a surface.
         * @param cell the cell to examine.
         * @return true if the cell is part of a surface, false if not, or is not in the shell.
         */
        public boolean surface(VJCell cell)
        {
                return surface(cell.ix, cell.iy, cell.iz);
        }
        /**
         * Determine whether cell contains a surface.
         * @param cell the cell to examine.
         * @return true if the cell is part of a surface, false if not, or is not in the shell.
         */
        public boolean surface(int ix, int iy, int iz)
        {
                if (ix >= 0 && ix < width &&
                  iy >= 0 && iy < height &&
                  iz >= 0 && iz < depth)
                        return v[iz*height*width+iy*width+ix];
                else
                        return false;
        }
        /**
         * Make an ImageJ imagestack from this shell. For debugging purposes only.
         */
        public ImageStack getImageStack()
        {
                ImageStack stack = new ImageStack(width, height);
                for (int z = 0; z < depth; z++)
                {
                        ImageProcessor ip = new ByteProcessor(width, height);
                        byte [] plane = (byte []) ip.getPixels();
                        for (int y = 0; y < height; y++)
                        for (int x = 0; x < width; x++)
							plane[y*width+x] = (byte) (v[z*height*width+y*width+x] ? 255 : 0);
                        stack.addSlice(""+(z+1), ip);
                }
                return stack;
        }
        /**
         * All highly factored values for cell advance algorithm.
         */
        private int sx, sy, sz;
        private int n, total;
        private int ix, iy, iz;
        private double Axz, Bxz, Ayz, Byz;
        private int Cxzai, Cxzbi, Cyzai, Cyzbi;
        private int Cxzas, Cxzbs, Cyzas, Cyzbs;
        private double Cxzc1, Cxzc2, Cyzc1, Cyzc2;
        private double dx, dy, dz;
        private double inflater;
        private int distanceDeltaxzi, distanceDeltayzi;
        String s;
        /**
         * Prepare surface search for a ray with a specific direction vector.
         * Highly factored out.
         * Here you only know the direction of the ray.
         * @param dx, dy, dz the vector orientation of the ray.
         */
        public void advancePrepare(double dx, double dy, double dz)
        {
                /**
                 * Comments to code here to avoid showing up in Javadoc API.
                 *
                 * Fast 3D ray traversal for floating point rays, my own design.
                 * Still floating point.
                 * The ray is guaranteed to visit all voxels on the line from ox,oy,oz to dx,dy,dz.<br>
                 * It is sort of Bresenham for floating point grids.
                 * The algorithm uses an accumulator, distanceDelta, to accumulate decision information.
                 * Depending on whether distanceDelta is positive or negative, the cell is advanced to either of the two possible
                 * directions (the two neighboring cells in the direction of the ray.
                 * See advanceInit for more details.
                 */
                this.dx = dx; this.dy = dy; this.dz = dz;
                // Compute number of cells to be traversed.
                total = (int) Math.round(Math.abs(dx)+Math.abs(dy)+Math.abs(dz));
                // Steps in x, y and z direction.
                sx = sign(dx);
                sy = sign(dy);
                sz = sign(dz);
                /**
                 * Equation of a ray (line) is:
                 * Ax + By + C = 0
                 * If the vector of the line is vx, vy and a point on the line is ox, oy then
                 * A = vy, B = -vx and C = -A ox -B oy
                 * Also refer to searchSurfaceInit below
                 */
                Axz = dz;
                Bxz = -dx;
                Ayz = dz;
                Byz = -dy;
                /**
                 * The distance of a point px,py to a line Ax+By+C is
                 * ABS(A px + B py + C / SQRT(A^2 + B^2))
                 *
                 * If you want to compute the difference in distances of two points (ox+sx, oy) and (ox, oy+sy)
                 * to a line Ax+By+C, then by taking the square of each distance and subtracting them,
                 * and doing constant removal, you get equation:
                 * eq = ox Ca - oy Cb + Cc
                 * with
                 * Ca = 2 A^2 sx - 2AB sy, Cb = 2 B^2 sy - 2AB sx, Cc = A^2sx^2 - B^2 sy^2 + C (2A sx - 2B sy).
                 * If this equation is > 0, (ox+sx,oy) is farther from the line than (ox, oy+sy),
                 * otherwise it is closer.
                 *
                 */
                double Cxza = 2 * Axz * Axz * sx - 2 * Axz * Bxz * sz;
                double Cxzb = 2 * Bxz * Bxz * sz - 2 * Axz * Bxz * sx;
                double Cyza = 2 * Ayz * Ayz * sy - 2 * Ayz * Byz * sz;
                double Cyzb = 2 * Byz * Byz * sz - 2 * Ayz * Byz * sy;
                /**
                 * Compute intermediate factors for Cc, since C is only known if you have a point for a specific ray.
                 * You currently only know the direction.
                 * Add 0.5 (Ca - Cb) to Cc, since you are comparing the distances from the center of the
                 * cell, not the grid position of the cell.
                 * cell center = grid position + 0.5
                 */
                // Factor of Cc not dependent on C.
                Cxzc1 = Axz * Axz - Bxz * Bxz + 0.5 * (Cxza - Cxzb);
                Cyzc1 = Ayz * Ayz - Byz * Byz + 0.5 * (Cyza - Cyzb);
                // Factor of Cc dependent on C
                Cxzc2 = 2 * Axz * sx  - 2 * Bxz * sz;
                Cyzc2 = 2 * Ayz * sy  - 2 * Byz * sz;
                /**
                 * Divide all three parts of the factored out computation of distanceDelta by inflater.
                 * <code>inflater</code> is chosen so that the largest of Cas, Cbs, just fits
                 * within an integer (to be sure, divided by 2).
                 * This way, the maximum precision within 32 bits is taken for Cas, Cbs, which is also the maximum
                 * that distanceDelta can have during the iteration.
                 */
                 /*
                if (Math.abs(Cxzbs) > Math.abs(Cxzas))
                        inflaterxz = Integer.MAX_VALUE / (2.0 * Math.abs(Cxzbs));
                else
                        inflaterxz = Integer.MAX_VALUE / (2.0 * Math.abs(Cxzas));
                */
                // Find maximum of four factors as denominator.
                double denom = Math.max(Math.abs(Cxza), Math.max(Math.abs(Cxzb), Math.max(Math.abs(Cyza), Math.abs(Cyzb))));
                inflater = Integer.MAX_VALUE / (2.0 * denom);
                // Inflate.
                Cxza *= inflater;
                Cxzb *= inflater;
                Cyza *= inflater;
                Cyzb *= inflater;
                // Convert to 32 bits.
                Cxzai = (int) Math.round(Cxza);
                Cxzbi = (int) Math.round(Cxzb);
                Cyzai = (int) Math.round(Cyza);
                Cyzbi = (int) Math.round(Cyzb);
                /**
                 * Since the loop was refactored to use addition instead of multiplication to iterate distanceDelta,
                 * you need Ca * direction in y-axis and Cb * direction in x-axis, called Cas and Cbs.
                 */
                Cxzas = Cxzai * sx;
                Cxzbs = Cxzbi * sz;
                Cyzas = Cyzai * sy;
                Cyzbs = Cyzbi * sz;
                //VJUserInterface.write("inflaterxz: "+inflaterxz+" Cxzasi= "+Cxzasi+" Cxzas= "+Cxzas+" Cxzbsi= "+Cxzbsi+" Cxzbs= "+Cxzbs);

        }
        /**
         * Initialization of depth search routine.
         * Here you know the starting point on  the ray.
         * @param cell the starting position of the cell.
         * @param ox, oy, oz the origin (starting point) of the ray
         * @return the number of cells to be traversed.
         */
        public int advanceInit(VJCell cell, double ox, double oy, double oz)
        {
                // Now you can compute C, and Cc.
                double Cxz = -Axz * ox - Bxz * oz;
                double Cxzc = Cxzc1 + Cxz * Cxzc2;
                double Cyz = -Ayz * oy - Byz * oz;
                double Cyzc = Cyzc1 + Cyz * Cyzc2;
                ix = cell.ix;
                iy = cell.iy;
                iz = cell.iz;
                /**
                 * The equation for deciding which direction the next point on the line takes is (see above):
                 * distanceDelta = ox Ca - oy Cb + Cc
                 * with
                 * Ca = 2 A^2 sx - 2AB sy, Cb = 2 B^2 sy - 2AB sx, Cc = A^2sx^2 - B^2 sy^2 + C (2A sx - 2B sy).
                 *
                 * If you factor out multiplication within the loop and replace it with addition,
                 * the above set of equations is refactored to:
                 * Starting position at (x, y, z): distanceDelta = x * Ca - z * Cb + Cc
                 * Within loop:
                 * if (distanceDelta >= 0), distanceDelta -= Cb * direction along y axis, else distanceDelta += Ca * * direction along x axis.
                 *
                 * Compute initial value of distanceDelta (the difference of the square of the two distances, (ox+sx, oy) and
                 * (ox, oy+sy).
                 */
                double distanceDeltaxz = (double) ix * (double) Cxzai - (double) iz * (double) Cxzbi + Cxzc * inflater;
                double distanceDeltayz = (double) iy * (double) Cyzai - (double) iz * (double) Cyzbi + Cyzc * inflater;
                //distanceDeltaxz *= inflater;
                //distanceDeltayz *= inflater;
                // Convert to 32 bits.
                distanceDeltaxzi = (int) Math.round(distanceDeltaxz);
                // Convert to 32 bits.
                distanceDeltayzi = (int) Math.round(distanceDeltayz);
                n = total;
                //s=("Starting ray at "+ox+","+oy+","+oz+", to "+(dx+ox)+","+(dy+oy)+","+(dz+oz));
                //s+=("\nSome constants Cxzai="+Cxzai+" Cxzbi="+Cxzbi+" Cxzc="+Cxzc+" Cyzai="+Cyzai+" Cyzbi="+Cyzbi+" Cyzc="+Cyzc);
                //s+=("\nEqs: distanceDeltaxzi="+distanceDeltaxzi+" distanceDeltayzi="+distanceDeltayzi+" Cxzas="+Cxzas+" Cxzbs="+Cxzbs+" Cyzas="+Cyzas+" Cyzbs="+Cyzbs);
                //s+=("\nExpect to traverse "+total+" cells.\nRay equation: "+Axz+"x + "+Bxz+"z + "+Cxz+" = 0. s()="+sx+","+sy+","+sz+"\n");
                return n;
        }
        /**
         * Advance a cell to a surface along the ray.
         * See advanceInit() for details.
         * At return the cell will be at a surface.
         * @param cell the cell from which to proceed along the ray.
         * @return the number of cells visited since the call to advanceInit,
         * max if no cell containing a surface can be found anymore.
         */
        public int advanceToSurface(VJCell cell)
        {
                /**
                 * Integer arithmetic for finding the next point on a straight line between to floating point positions.
                 * This is my version of Bresenham's algorithm for finding the next point on a straight line.
                 */
                while (n-- > 0)
                {
                        boolean found = surface(ix, iy, iz);
                        // Has a surface, advance cell position to ix, iy, iz.
                        if (found)
                        {
                                cell.ix = ix;
                                cell.iy = iy;
                                cell.iz = iz;
                        }
                        // Advance ix,iy,iz to cell with center closest to ray (tested with distanceDeltaxz).
                        if (distanceDeltaxzi >= 0 && distanceDeltayzi >= 0)
                        {
                                // Distance in x and y direction both larger than in z direction.
                                iz += sz;
                                distanceDeltaxzi -= Cxzbs;
                                distanceDeltayzi -= Cyzbs;
                        }
                        else if (distanceDeltaxzi < distanceDeltayzi)
                        {
                                // x and y smaller than z, x smaller than y
                                ix += sx;
                                distanceDeltaxzi += Cxzas;
                        }
                        else
                        {
                                // x and y smaller than z, y smaller than x
                                iy += sy;
                                distanceDeltayzi += Cyzas;
                        }
                        // Keep the new position.
                        if (found)
                                return total-n;
                }
                return total;
        }
        /**
         * Advance a cell to a surface along the ray. Set string s to the trace of the cells visited.
         * @param cell the cell from which to proceed along the ray.
         * @return the number of cells visited since the call to advanceInit,
         * max if no cell containing a surface can be found anymore.
         */
        public int advanceToSurfaceTracing(VJCell cell)
        {
                while (n-- > 0)
                {
                        boolean found = surface(ix, iy, iz);
                        if (found)
                        {
                                cell.ix = ix;
                                cell.iy = iy;
                                cell.iz = iz;
                        }
                        //s+=("; "+ix+","+iy+","+iz+"\ndistanceDeltaxzi="+distanceDeltaxzi+"distanceDeltayzi="+distanceDeltayzi+"\n");
                        s+=("; "+ix+","+iy+","+iz);
                        // Keep the new position.
                        if (distanceDeltaxzi >= 0 && distanceDeltayzi >= 0)
                        {
                                // Distance in x and y direction both larger than in z direction.
                                iz += sz;
                                distanceDeltaxzi -= Cxzbs;
                                distanceDeltayzi -= Cyzbs;
                        }
                        else if (distanceDeltaxzi < distanceDeltayzi)
                        {
                                // At least one of x and y closer than z, and x closer than y
                                ix += sx;
                                distanceDeltaxzi += Cxzas;
                        }
                        else
                        {
                                // At least one of x and y closer than z, and y closer than x
                                iy += sy;
                                distanceDeltayzi += Cyzas;
                        }
                        if (found)
                        {
                                s="surface at: "+cell.ix+","+cell.iy+","+cell.iz+" ("+(total-n)+"): "+s+"\n";
                                return total-n;
                        }
                }
                s="no surface "+cell.ix+","+cell.iy+","+cell.iz+" visited ("+(total-n)+"): "+s+"\n";
                return total;
        }
        private int sign(double d) { if (d >= 0) return 1; else return -1; }
}

/*
 * C code from the article
 * "Voxel Traversal along a 3D Line"
 * by Daniel Cohen, danny@bengus.bgu.ac.il
 * in "Graphics Gems IV", Academic Press, 1994
 */

/* The following C subroutine visits all voxels along the line
segment from (x, y, z) and (x + dx, y + dy, z + dz) */

/*
Line ( x, y, z, dx, dy, dz )
int x, y, z, dx, dy, dz;
{
    int n, sx, sy, sz, exy, exz, ezy, ax, ay, az, bx, by, bz;

    sx = sgn(dx);  sy = sgn(dy);  sz = sgn(dz);
    ax = abs(dx);  ay = abs(dy);  az = abs(dz);
    bx = 2*ax;	   by = 2*ay;	  bz = 2*az;
    exy = ay-ax;   exz = az-ax;	  ezy = ay-az;
    n = ax+ay+az;
    while ( n-- ) {
	VisitVoxel ( x, y, z );
	if ( exy < 0 ) {
	    if ( exz < 0 ) {
		x += sx;
		exy += by; exz += bz;
	    }
	    else  {
		z += sz;
		exz -= bx; ezy += by;
	    }
	}
	else {
	    if ( ezy < 0 ) {
		z += sz;
		exz -= bx; ezy += by;
	    }
	    else  {
		y += sy;
		exy -= bx; ezy -= bz;
	    }
	}
    }
}
*/