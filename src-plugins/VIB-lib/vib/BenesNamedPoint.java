package vib;
import ij.IJ;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import math3d.Point3d;

public class BenesNamedPoint extends Point3d {

	String name;
	boolean set;
	static Pattern p_data = Pattern.compile("^\"(.*)\": *"+
									"\\[ *([eE0-9\\.\\-]+) *, *"+
									"([eE0-9\\.\\-]+) *, *"+
									"([eE0-9\\.\\-]+) *\\] *$");
	static Pattern p_empty = Pattern.compile("^ *$");

	public BenesNamedPoint(String name,
			  double x,
			  double y,
			  double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.name = name;
		this.set = true;
	}

	public BenesNamedPoint(String name) {
		this.name = name;
		this.set = false;
	}
	
	public void set(double x, double y, double z){
		this.x = x; this.y = y; this.z = z;
		this.set = true;
	}

	public static BenesNamedPoint fromLine(String line){
		Matcher m_data = p_data.matcher(line);
		Matcher m_empty = p_empty.matcher(line);

		if (m_data.matches()) {
			return
				new BenesNamedPoint(m_data.group(1),
					       Double.parseDouble(m_data.group(2)),
					       Double.parseDouble(m_data.group(3)),
					       Double.parseDouble(m_data.group(4)));
				
		} else if (m_empty.matches()) {
			return null;
		} else {
			IJ.log("There was a points file, but this line was malformed:\n"+
				 line);
			return null;
		}
	}

	public String getName() {
		return name;
	}

	public boolean isSet() {
		return set;
	}
	
	public static String escape(String s) {
		String result = s.replaceAll("\\\\","\\\\\\\\");
		result = result.replaceAll("\\\"","\\\\\"");
		return result;
	}

	public static String unescape(String s) {
		// FIXME: actually write the unescaping code...
		return s;
	}
	
	public String coordinatesAsString(){
		return "[ "+
		x+", "+
		y+", "+
		z+" ]";
	}

	public String toYAML() {
		String line = "\""+
			escape(name)+
			"\": " + coordinatesAsString();
		return line;
	}
	
	public String toString(){
		return name + ": [" + x + ", " + y + ", " + z + "]";
	}
}
