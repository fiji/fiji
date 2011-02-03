package util;

import java.util.ArrayList;

/* This is a Java version of the public domain function found here:
 ftp://ftp.isc.org/pub/usenet/comp.sources.unix/volume26/line3d
 */

public class Bresenham3D {

	public static int sign(int a) {
		return (a < 0) ? -1 : ((a > 0 ? 1 : 0));
	}

	public static class IntegerPoint {
		public IntegerPoint(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		public int x, y, z;
		public boolean diagonallyAdjacentOrEqual(IntegerPoint other) {
			int xdiff = Math.abs(other.x - x);
			int ydiff = Math.abs(other.y - y);
			int zdiff = Math.abs(other.z - z);
			return (xdiff < 1 && ydiff < 1 && zdiff < 1);
		}
		@Override
		public String toString() {
			return "("+x+","+y+","+z+")";
		}
	}

	public static ArrayList<IntegerPoint> bresenham3D(IntegerPoint p1, IntegerPoint p2) {
		return bresenham3D(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z);
	}

	public static ArrayList<IntegerPoint> bresenham3D(int x1, int y1,
			int z1, int x2, int y2, int z2) {
		ArrayList<IntegerPoint> result = new ArrayList<IntegerPoint>();

		int xd, yd, zd;
		int x, y, z;
		int ax, ay, az;
		int sx, sy, sz;
		int dx, dy, dz;

		dx = x2 - x1;
		dy = y2 - y1;
		dz = z2 - z1;

		ax = Math.abs(dx) << 1;
		ay = Math.abs(dy) << 1;
		az = Math.abs(dz) << 1;

		sx = sign(dx);
		sy = sign(dy);
		sz = sign(dz);

		x = x1;
		y = y1;
		z = z1;

		if (ax >= Math.max(ay, az)) { /* x dominant */
			yd = ay - (ax >> 1);
			zd = az - (ax >> 1);
			while (true) {
				result.add(new IntegerPoint(x,y,z));
				if (x == x2) {
					return result;
				}

				if (yd >= 0) {
					y += sy;
					yd -= ax;
				}

				if (zd >= 0) {
					z += sz;
					zd -= ax;
				}

				x += sx;
				yd += ay;
				zd += az;
			}
		} else if (ay >= Math.max(ax, az)) { /* y dominant */

			xd = ax - (ay >> 1);
			zd = az - (ay >> 1);
			while (true) {
				result.add(new IntegerPoint(x,y,z));
				if (y == y2) {
					return result;
				}

				if (xd >= 0) {
					x += sx;
					xd -= ay;
				}

				if (zd >= 0) {
					z += sz;
					zd -= ay;
				}

				y += sy;
				xd += ax;
				zd += az;
			}
		} else if (az >= Math.max(ax, ay)) { /* z dominant */
			xd = ax - (az >> 1);
			yd = ay - (az >> 1);
			while (true) {
				result.add(new IntegerPoint(x,y,z));
				if (z == z2) {
					return result;
				}

				if (xd >= 0) {
					x += sx;
					xd -= az;
				}

				if (yd >= 0) {
					y += sy;
					yd -= az;
				}

				z += sz;
				xd += ax;
				yd += ay;
			}
		}
		return null;
	}
}
