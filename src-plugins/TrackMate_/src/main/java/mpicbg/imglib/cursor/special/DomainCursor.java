/**
 * 
 */
package mpicbg.imglib.cursor.special;

import mpicbg.imglib.type.Type;

/**
 * Abstract class for {@link CoordsCursor}s that iterate over a domain which size can be
 * determined by a single parameter. For instance: cubes, spheres, circles.
 * <p>
 * This abstract layer add a single method: {@link #setSize(float)} that reconfigures 
 * the domain to iterate on.
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Sep 9, 2010
 *
 */
public abstract class DomainCursor <T extends Type<T>> extends CoordsCursor<T> {

	/**
	 * The domain size, in physical units.
	 */
	protected float size; 
	
	/**
	 * Change the size of the domain this cursor iterates on.  This <b>resets</b> this cursor.
	 */
	public abstract void setSize(float size);
}
