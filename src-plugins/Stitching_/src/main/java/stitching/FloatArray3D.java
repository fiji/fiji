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

public class FloatArray3D extends FloatArray
{
	// public float data[] = null;
	final public int width;
	final public int height;
	final public int depth;

	public FloatArray3D(float[] data, int width, int height, int depth)
	{
		this.data = data;
		this.width = width;
		this.height = height;
		this.depth = depth;
	}

	public FloatArray3D(int width, int height, int depth)
	{
		data = new float[width * height * depth];
		this.width = width;
		this.height = height;
		this.depth = depth;
	}

	public FloatArray3D clone()
	{
		FloatArray3D clone = new FloatArray3D(width, height, depth);
		System.arraycopy(this.data, 0, clone.data, 0, this.data.length);
		return clone;
	}

	public int getPos(int x, int y, int z)
	{
		return x + width * (y + z * height);
	}

	public float get(int x, int y, int z)
	{
		return data[getPos(x, y, z)];
	}

	public float getMirror(int x, int y, int z)
	{
		if (x >= width) x = width - (x - width + 2);

		if (y >= height) y = height - (y - height + 2);

		if (z >= depth) z = depth - (z - depth + 2);

		if (x < 0)
		{
			int tmp = 0;
			int dir = 1;

			while (x < 0)
			{
				tmp += dir;
				if (tmp == width - 1 || tmp == 0) dir *= -1;
				x++;
			}
			x = tmp;
		}

		if (y < 0)
		{
			int tmp = 0;
			int dir = 1;

			while (y < 0)
			{
				tmp += dir;
				if (tmp == height - 1 || tmp == 0) dir *= -1;
				y++;
			}
			y = tmp;
		}

		if (z < 0)
		{
			int tmp = 0;
			int dir = 1;

			while (z < 0)
			{
				tmp += dir;
				if (tmp == depth - 1 || tmp == 0) dir *= -1;
				z++;
			}
			z = tmp;
		}

		return data[getPos(x, y, z)];
	}

	public float getZero(int x, int y, int z)
	{
		if (x >= width) return 0;

		if (y >= height) return 0;

		if (z >= depth) return 0;

		if (x < 0) return 0;

		if (y < 0) return 0;

		if (z < 0) return 0;

		return data[getPos(x, y, z)];
	}

	public void set(float value, int x, int y, int z)
	{
		data[getPos(x, y, z)] = value;
	}
}
