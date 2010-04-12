/*
 * Copyright (c) 2005 by L. Paul Chew.
 * 
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, subject to the following 
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */

package delaunay;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A 2D Delaunay Triangulation (DT) with incremental site insertion.
 * This is not the fastest way to build a DT, but it's a reasonable way
 * to build the DT incrementally and it makes a nice interactive display.
 * There are several O(n log n) methods, but they require that either (1) 
 * the sites are all known initially or (2) the sites are inserted in random
 * order.
 * 
 * @author Paul Chew
 * 
 * Created July 2005.  Derived from an earlier, messier version.
 */
public class DelaunayTriangulation extends Triangulation {
    
    private Simplex mostRecent = null;       // Most recently inserted triangle
    public boolean debug = false;            // Used for debugging
    
    /**
     * Constructor.
     * All sites must fall within the initial triangle.
     * @param triangle the initial triangle
     */
    public DelaunayTriangulation (Simplex triangle) {
        super(triangle);
        mostRecent = triangle;
    }
    
    /**
     * Locate the triangle with point (a Pnt) inside (or on) it.
     * @param point the Pnt to locate
     * @return triangle (Simplex<Pnt>) that holds the point; null if no such triangle
     */
    public Simplex locate (Pnt point) {
        Simplex triangle = mostRecent;
        if (!this.contains(triangle)) triangle = null;
        
        // Try a directed walk (this works fine in 2D, but can fail in 3D)
        Set visited = new HashSet();
        while (triangle != null) {
            if (visited.contains(triangle)) { // This should never happen
                System.out.println("Warning: Caught in a locate loop");
                break;
            }
            visited.add(triangle);
            // Corner opposite point
            Pnt corner = point.isOutside((Pnt[]) triangle.toArray(new Pnt[0]));
            if (corner == null) return triangle;
            triangle = this.neighborOpposite(corner, triangle);
        }
        // No luck; try brute force
        System.out.println("Warning: Checking all triangles for " + point);
        for (Iterator it = this.iterator(); it.hasNext();) {
            Simplex tri = (Simplex) it.next();
            if (point.isOutside((Pnt[]) tri.toArray(new Pnt[0])) == null) return tri;
        }
        // No such triangle
        System.out.println("Warning: No triangle holds " + point);
        return null;
    }
    
    /**
     * Place a new point site into the DT.
     * @param site the new Pnt
     * @return set of all new triangles created
     */
    public Set delaunayPlace (Pnt site) {
        Set newTriangles = new HashSet();
        Set oldTriangles = new HashSet();
        Set doneSet = new HashSet();
        LinkedList waitingQ = new LinkedList();
        
        // Locate containing triangle
        if (debug) System.out.println("Locate");
        Simplex triangle = locate(site);
        
        // Give up if no containing triangle or if site is already in DT
        if (triangle == null || triangle.contains(site)) return newTriangles;
        
        // Find Delaunay cavity (those triangles with site in their circumcircles)
        if (debug) System.out.println("Cavity");
        waitingQ.add(triangle);
        while (!waitingQ.isEmpty()) {
            triangle = (Simplex) waitingQ.removeFirst();      
            if (site.vsCircumcircle((Pnt[]) triangle.toArray(new Pnt[0])) == 1) continue;
            oldTriangles.add(triangle);
            Iterator it = this.neighbors(triangle).iterator();
            for (; it.hasNext();) {
                Simplex tri = (Simplex) it.next();
                if (doneSet.contains(tri)) continue;
                doneSet.add(tri);
                waitingQ.add(tri);
            }
        }
        // Create the new triangles
        if (debug) System.out.println("Create");
        for (Iterator it = Simplex.boundary(oldTriangles).iterator(); it.hasNext();) {
            Set facet = (Set) it.next();
            facet.add(site);
            newTriangles.add(new Simplex(facet));
        }
        // Replace old triangles with new triangles
        if (debug) System.out.println("Update");
        this.update(oldTriangles, newTriangles);
        
        // Update mostRecent triangle
        if (!newTriangles.isEmpty()) mostRecent = (Simplex) newTriangles.iterator().next();
        return newTriangles;
    }
    
    /**
     * Main program; used for testing.
     */
    public static void main (String[] args) {
        Simplex tri = new Simplex(new Pnt[] {new Pnt(-10,10), new Pnt(10,10), new Pnt(0,-10)});
        System.out.println("Triangle created: " + tri);
        DelaunayTriangulation dt = new DelaunayTriangulation(tri);
        System.out.println("DelaunayTriangulation created: " + dt);
        dt.delaunayPlace(new Pnt(0,0));
        dt.delaunayPlace(new Pnt(1,0));
        dt.delaunayPlace(new Pnt(0,1));
        System.out.println("After adding 3 points, the DelaunayTriangulation is a " + dt);
        dt.printStuff();
    }
}
