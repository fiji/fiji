package fiji.plugin.trackmate;

import java.util.List;

/**
 * Simple interface for factory that can return a list of the instance they hold 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Mar 31, 2011
 *
 */
public interface Listable<K> {

	public List<K> getList();
	
}
