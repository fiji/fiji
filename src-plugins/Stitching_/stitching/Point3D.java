/**
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
 * An execption is the FFT implementation of Dave Hale which we use as a library,
 * wich is released under the terms of the Common Public License - v1.0, which is 
 * available at http://www.eclipse.org/legal/cpl-v10.html  
 *
 * @author Stephan Preibisch
 */
package stitching;

public class Point3D
{
	public int x = 0, y = 0, z = 0;
	public float value;
	public boolean printValue = false;

	public Point3D(int x, int y, int z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Point3D(int x, int y, int z, float value)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.value = value;
	}

	public String toString()
	{
		if (printValue)
			return "x: " + x + " y: " + y  + " z: " + z + " value: " + value;
		else
			return "x: " + x + " y: " + y  + " z: " + z;
	}
	
	public Point3D clone()
	{
		return new Point3D(x, y, z, value);
	}
}
