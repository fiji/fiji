/*
 * At the moment, this class works on double's. To change that,
 * s/double \/\*dtype\*\//int/g
 */

package math3d;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class Point3d {
	public double /*dtype*/ x, y, z;

	public Point3d() {
	}

	public Point3d(double /*dtype*/ x, double /*dtype*/ y, double /*dtype*/ z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

    public double /*dtype*/ [] toArray() {
        double /*dtype*/ [] result={x,y,z};
        return result;
    }

    public float[] toArrayFloat() {
        float[] result={(float)x,(float)y,(float)z};
        return result;
    }	

    public double[] toArrayDouble() {
        double[] result={(double)x,(double)y,(double)z};
        return result;
    }	

	public Point3d minus(Point3d other) {
		return new Point3d(x - other.x,
				y - other.y,
				z - other.z);
	}

	public Point3d plus(Point3d other) {
		return new Point3d(x + other.x,
				y + other.y,
				z + other.z);
	}

	public double /*dtype*/ scalar(Point3d other) {
		return x * other.x + y * other.y + z * other.z;
	}

	public Point3d times(double /*dtype*/ factor) {
		return new Point3d(x * factor,
				y * factor,
				z * factor);
	}

	public Point3d vector(Point3d other) {
		return new Point3d(y * other.z - z * other.y,
				z * other.x - x * other.z,
				x * other.y - y * other.x);
	}

	public double length() {
		return Math.sqrt(scalar(this));
	}

	public double /*dtype*/ distance2(Point3d other) {
		double /*dtype*/ x1 = x - other.x;
		double /*dtype*/ y1 = y - other.y;
		double /*dtype*/ z1 = z - other.z;
		return x1 * x1 + y1 * y1 + z1 * z1;
	}

	public double distanceTo(Point3d other) {
		return Math.sqrt(distance2(other));
	}

	public static Point3d average(Point3d[] list) {
		Point3d result = new Point3d();
		for (int i = 0; i < list.length; i++)
			result = result.plus(list[i]);
		return result.times(1.0 / list.length);
	}

	static Point3d random() {
		return new Point3d(Math.random() * 400 + 50,
				Math.random() * 400 + 50,
				Math.random() * 400 + 50);
	}

	public String toString() {
		return x + " " + y + " " + z;
	}
	
	public static Point3d parsePoint(String s){
		StringTokenizer st = new StringTokenizer(s," ");
		Point3d p = new Point3d();
		p.x = Double.parseDouble(st.nextToken());
		p.y = Double.parseDouble(st.nextToken());
		p.z = Double.parseDouble(st.nextToken());
		return p;
	}
	
	public static Point3d[] parsePoints(String s){
		ArrayList list = new ArrayList();
		StringTokenizer st = new StringTokenizer(s,",");
		while(st.hasMoreTokens())
			list.add(parsePoint(st.nextToken().trim()));

		Point3d[] result = new Point3d[list.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = (Point3d)list.get(i);
		return result;
	}
	
	public static void print(Point3d[] points){
		for (int i = 0; i < points.length; i++)
			System.out.println((i > 0 ? "," : "") + points[i]);
	}
	
	public static void main(String[] args){
		String s = "127.46979200950274 127.5047385083133 28.033169558193062,153.0 123.5 0.0";
		Point3d[] p = Point3d.parsePoints(s);
		print(p);
	}
}


