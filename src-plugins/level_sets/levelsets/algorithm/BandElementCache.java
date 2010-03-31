// $Revision$, $Date$, $Author$

package levelsets.algorithm;

import java.util.Stack;

/**
 * A cache for BandElement objects - this avoids having to construct/destruct
 * objects in algorithms that do frequent allocations/deallocations of this
 * type (which is straining for the Java virtual machine).
 */
public class BandElementCache
{
   final private Stack<BandElement> element_cache = new Stack<BandElement>();
   
   /** Creates a new instance of BandElementCache */
   public BandElementCache(final int capacity)
   {
      element_cache.ensureCapacity(capacity);
   }
   
   final public void recycleBandElement(final BandElement elem)
   {
      if (element_cache.size() < element_cache.capacity())
      {
         element_cache.push(elem);
      }
   }
   
   final public BandElement getRecycledBandElement(final int x, final int y, final int z, final double value)
   {
      if (element_cache.size() == 0)
      {
         return new BandElement(x, y, z, value);
      }
      else
      {
         final BandElement elem = element_cache.pop();
         elem.setX(x);
         elem.setY(y);
         elem.setZ(z);
         elem.setValue(value);
         return elem;
      }
   }
   
   final public BandElement getRecycledBandElement()
   {
      return getRecycledBandElement(0, 0, 0, 0);
   }
}
