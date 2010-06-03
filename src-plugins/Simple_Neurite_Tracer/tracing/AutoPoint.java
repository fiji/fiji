/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009 Mark Longair */

/*
  This file is part of the ImageJ plugin "Auto Tracer".

  The ImageJ plugin "Auto Tracer" is free software; you can
  redistribute it and/or modify it under the terms of the GNU General
  Public License as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  The ImageJ plugin "Auto Tracer" is distributed in the hope that it
  will be useful, but WITHOUT ANY WARRANTY; without even the implied
  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU General Public License for more details.

  In addition, as a special exception, the copyright holders give
  you permission to combine this program with free software programs or
  libraries that are released under the Apache Public License.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package tracing;

public class AutoPoint {
	public short x;
	public short y;
	public short z;
	public AutoPoint(int x,int y,int z) {
		this.x = (short)x;
		this.y = (short)y;
		this.z = (short)z;
	}
	@Override
	public String toString() {
		return "("+x+","+y+","+z+")";
	}
	@Override
	public boolean equals(Object o) {
		AutoPoint op=(AutoPoint)o;
		// System.out.println("Testing equality between "+this+" and "+op);
		boolean result = (this.x == op.x) && (this.y == op.y) && (this.z == op.z);
		return result;
	}
	@Override
	public int hashCode() {
		return (int)x + (int)y * (1 << 11) + (int)z * (1 << 22);
	}
}
