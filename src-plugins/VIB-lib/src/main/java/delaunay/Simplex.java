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

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A Simplex is an immutable set of vertices (usually Pnts).
 *
 * @author Paul Chew
 *
 * Created July 2005. Derived from an earlier, messier version.
 */
public class Simplex extends AbstractSet<Pnt> implements Set<Pnt> {

    private List<Pnt> vertices;                  // The simplex's vertices
    private long idNumber;                  // The id number
    private static long idGenerator = 0;    // Used to create id numbers
    public static boolean moreInfo = false; // True iff more info in toString

    /**
     * Constructor.
     * @param collection a Collection holding the Simplex vertices
     * @throws IllegalArgumentException if there are duplicate vertices
     */
    public Simplex(Collection<Pnt> collection) {
        this.vertices = Collections.unmodifiableList(new ArrayList<Pnt>(collection));
        this.idNumber = idGenerator++;
        Set<Pnt> noDups = new HashSet<Pnt>(this);
        if (noDups.size() != this.vertices.size())
            throw new IllegalArgumentException("Duplicate vertices in Simplex");
    }

    /**
     * Constructor.
     * @param vertices the vertices of the Simplex.
     * @throws IllegalArgumentException if there are duplicate vertices
     */
    public Simplex(Pnt[] vertices) {
        this(Arrays.asList(vertices));
    }
    
    /**
     * Constructor.
     * @param vertices the vertices of the Simplex.
     * @throws IllegalArgumentException if there are duplicate vertices
     * 
     * @deprecated Use {@link #Simplex(String[])} instead.
     */
    @Deprecated
    public Simplex(Object[] vertices) {
        this(Arrays.asList((Pnt[])vertices));
    }

    /**
     * String representation.
     * @return the String representation of this Simplex
     */
    public String toString() {
        if (!moreInfo) return "Simplex" + idNumber;
        return "Simplex" + idNumber + super.toString();
    }

    /**
     * Dimension of the Simplex.
     * @return dimension of Simplex (one less than number of vertices)
     */
    public int dimension() {
        return this.vertices.size() - 1;
    }

    /**
     * True iff simplices are neighbors.
     * Two simplices are neighbors if they are the same dimension and they share
     * a facet.
     * @param simplex the other Simplex
     * @return true iff this Simplex is a neighbor of simplex
     */
    public boolean isNeighbor(Simplex simplex) {
        HashSet<Pnt> h = new HashSet<Pnt>(this);
        h.removeAll(simplex);
        return (this.size() == simplex.size()) && (h.size() == 1);
    }

    /**
     * Report the facets of this Simplex.
     * Each facet is a set of vertices.
     * @return an Iterable for the facets of this Simplex
     */
    public List<Set<Pnt>> facets() {
        List<Set<Pnt>> theFacets = new LinkedList<Set<Pnt>>();
        for (Iterator<Pnt> it = this.iterator(); it.hasNext();) {
            Object v = it.next();
            Set<Pnt> facet = new HashSet<Pnt>(this);
            facet.remove(v);
            theFacets.add(facet);
        }
        return theFacets;
    }

    /**
     * Report the boundary of a Set of Simplices.
     * The boundary is a Set of facets where each facet is a Set of vertices.
     * @return an Iterator for the facets that make up the boundary
     */
    public static Set<Set<Pnt>> boundary(Set<Simplex> simplexSet) {
        Set<Set<Pnt>> theBoundary = new HashSet<Set<Pnt>>();
        for (Iterator<Simplex> it = simplexSet.iterator(); it.hasNext();) {
            Simplex simplex = it.next();
            for (Iterator<Set<Pnt>> otherIt = simplex.facets().iterator(); otherIt.hasNext();) {
                Set<Pnt> facet = otherIt.next();
                if (theBoundary.contains(facet)) theBoundary.remove(facet);
                else theBoundary.add(facet);
            }
        }
        return theBoundary;
    }

    /* Remaining methods are those required by AbstractSet */

    /**
     * @return Iterator for Simplex's vertices.
     */
    public Iterator<Pnt> iterator() {
        return this.vertices.iterator();
    }

    /**
     * @return the size (# of vertices) of this Simplex
     */
    public int size() {
        return this.vertices.size();
    }

    /**
     * @return the hashCode of this Simplex
     */
    public int hashCode() {
        return (int)(idNumber^(idNumber>>>32));
    }

    /**
     * We want to allow for different simplices that share the same vertex set.
     * @return true for equal Simplices
     */
    public boolean equals(Object o) {
        return (this == o);
    }
}
