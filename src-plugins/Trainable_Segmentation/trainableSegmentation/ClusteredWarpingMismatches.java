package trainableSegmentation;

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
 * Authors: Ignacio Arganda-Carreras (iarganda@mit.edu)
 */

public class ClusteredWarpingMismatches
{
	public int numOfObjectAdditions = 0;
	public int numOfHoleDeleitions = 0;
	public int numOfMergers = 0;
	public int numOfHoleAdditions = 0;
	public int numOfObjectDeleitions = 0;
	public int numOfSplits = 0;
	
	public ClusteredWarpingMismatches(
			int numOfObjectAdditions,
			int numOfHoleDeleitions,
			int numOfMergers,
			int numOfHoleAdditions,
			int numOfObjectDeletions,
			int numOfSplits
			)
	{
		this.numOfHoleAdditions = numOfHoleAdditions;
		this.numOfHoleDeleitions = numOfHoleDeleitions;
		this.numOfMergers = numOfMergers;
		this.numOfObjectAdditions = numOfObjectAdditions;
		this.numOfObjectDeleitions = numOfObjectDeletions;
		this.numOfSplits = numOfSplits;
	}
}
