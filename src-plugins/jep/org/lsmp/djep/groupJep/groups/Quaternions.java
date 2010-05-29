/* @author rich
 * Created on 16-May-2004
 */
package org.lsmp.djep.groupJep.groups;

import org.nfunk.jep.JEP;
import org.lsmp.djep.groupJep.interfaces.*;
/**
 * Possibly the Quaternions, completely untested.
 * 
 * @author Rich Morris
 * Created on 16-May-2004
 */
public class Quaternions extends Group implements RingI {

	public static class Quaternion extends Number {
		double x,y,z,w;
		public Quaternion(double x,double y,double z,double w){
			this.w = x; this.y = y; this.z=z; this.w=w;
		}
		public double doubleValue() {return x;}
		public float floatValue() {return (float) x;}
		public int intValue() {return (int) x;}
		public long longValue() {return (long) x;}
		// TODO pretty print so 0 + 0 i + 0 j + 1 k printed as k
		public String toString() {//return ""+x+"+"+y+" i +"+z+" j +"+w+" k";
			StringBuffer sb = new StringBuffer();
			boolean flag=false;
			if(x!=0.0) { sb.append(x); flag = true; }
			if(y!=0.0) { 
				if(flag && y>0.0) sb.append("+");
				if(y==1.0) {}
				else if(y==-1.0) { sb.append("-"); }
				else sb.append(y); 
				sb.append("i");
				flag=true;
			}
			
			if(z!=0.0) { 
				if(flag && z>0.0) sb.append("+");
				if(z==1.0) {}
				else if(z==-1.0) { sb.append("-"); }
				else sb.append(z); 
				sb.append("j");
				flag=true;
			}
			if(w!=0.0) { 
				if(flag && w>0.0) sb.append("+");
				if(w==1.0) {}
				else if(w==-1.0) { sb.append("-"); }
				else sb.append(w); 
				sb.append("k");
				flag=true;
			}
			if(!flag)
				sb.append("0");
			return sb.toString();
		}
	}
	private Quaternion ZERO = new Quaternion(0,0,0,0);
	private Quaternion ONE = new Quaternion(1,0,0,0);
	private Quaternion I = new Quaternion(0,1,0,0);
	private Quaternion J = new Quaternion(0,0,1,0);
	private Quaternion K = new Quaternion(0,0,0,1);

	public Number getZERO() {return ZERO;}
	public Number getONE() {return ONE;	}

	public Number getInverse(Number num) {
		Quaternion q = (Quaternion) num;
		return new Quaternion(-q.x,-q.y,-q.z,-q.w);
	}

	public Number add(Number a, Number b) {
		Quaternion p = (Quaternion) a;
		Quaternion q = (Quaternion) b;
		return new Quaternion(p.x+q.x,p.y+q.y,p.z+q.z,p.w+q.w);
	}

	public Number sub(Number a, Number b) {
		Quaternion p = (Quaternion) a;
		Quaternion q = (Quaternion) b;
		return new Quaternion(p.x-q.x,p.y-q.y,p.z-q.z,p.w-q.w);
	}


	public Number mul(Number a, Number b) {
		Quaternion p = (Quaternion) a;
		Quaternion q = (Quaternion) b;
		return new Quaternion(
			p.x*q.x - p.y*q.y - p.z*q.z - p.w*q.w,
			p.x*q.y - p.y*q.x + p.z*q.w - p.w*q.z,
			p.x*q.z - p.y*q.w + p.z*q.x + p.w*q.y,
			p.x*q.w - p.y*q.z - p.z*q.y + p.w*q.x
			);
	}

	public boolean equals(Number a, Number b) {
		Quaternion p = (Quaternion) a;
		Quaternion q = (Quaternion) b;
		return (p.x==q.x)&&(p.y==q.y)&&(p.z==q.z)&&(p.w==q.w);
	}

	public Number valueOf(String s) {
		return new Quaternion(Double.parseDouble(s),0,0,0);
	}

	public void addStandardConstants(JEP j) {
		super.addStandardConstants(j);
		j.addConstant("i",I);
		j.addConstant("j",J);
		j.addConstant("k",K);
	}
	public String toString() {return "Quaternions";}

}
