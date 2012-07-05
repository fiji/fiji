package ai;
/**
 *
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Authors: Ignacio Arganda-Carreras (iarganda@mit.edu), 
 * 			Albert Cardona (acardona@ini.phys.ethz.ch)
 */

import java.io.Serializable;
import java.util.ArrayList;

import weka.core.Instances;

public class Splitter implements Serializable
{
	
	/** Serial version ID */
	private static final long serialVersionUID = 52652189902462L;
	/** split function template */
	private SplitFunction template;
	/**
	 * Construct a split function producer
	 * 
	 * @param sfn split function template
	 */
	public Splitter(SplitFunction sfn)
	{
		this.template = sfn;
	}
	
	/**
	 * Calculate split function based on the input data
	 * @param data original data
	 * @param indices indices of the samples to use
	 * @return split function
	 */
	public SplitFunction getSplitFunction(
			final Instances data,
			final ArrayList<Integer> indices)
	{
		try {
			SplitFunction sf = template.newInstance();
			sf.init(data, indices);
			return sf;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
