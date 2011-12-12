package amira;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.Menus;
import ij.WindowManager;

import ij.gui.GenericDialog;

import ij.macro.Interpreter;

import ij.measure.Calibration;

import java.awt.Frame;
import java.awt.Menu;
import java.awt.MenuBar;

import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * This class parses and writes the Parameters in Amira Files
 */

public class AmiraParameters {
    public static final String INFO = "Info";

    public AmiraParameters(ImagePlus imp) {
		parameters=new Properties();
		if (isAmiraMesh(imp) || (imp.getProperty(INFO)!=null)) {
			line = "Parameters {\n"
				+ (String)imp.getProperty(INFO)
				+ "}\n";
			parseParameters(parameters);
		}
		initializeMaterials();
		initDefaults(imp);
	}

	public AmiraParameters(Properties properties) {
		parameters = new Properties();
		parameters.putAll(properties);
	}

	public AmiraParameters(String text) {
		line=text;
		parameters=new Properties();
		parseParameters(parameters);
		initializeMaterials();
		if(parameters.get("Parameters")==null)
			initDefaults(1,1,1);
	}

	private int width,height,depth;

	private void initDefaults(int width,int height,int depth) {
		initDefaults(width,height,depth,1,1,1);
	}
	private void initDefaults(int width,int height,int depth,double voxelWidth,double voxelHeight,double voxelDepth) {
		this.width = width;
		this.height = height;
		this.depth = depth;
		Hashtable table=(Hashtable)parameters.get("Parameters");
		if(table==null) {
			table=new Hashtable();
			parameters.put("Parameters",table);
		}
		table.put("BoundingBox","0.0 "+(width*voxelWidth)+" 0.0 "+(height*voxelHeight)+" 0.0 "+(depth*voxelDepth));
		table.put("CoordType","\"uniform\"");
		table.put("Content","\""+width+"x"+height+"x"+depth+" byte, uniform coordinates\"");
	}
	public void initDefaults(ImagePlus imp) {
		ImageStack stack=imp.getStack();
		if(stack==null)
			initDefaults(imp.getWidth(),imp.getHeight(),1);
		else
			initDefaults(stack.getWidth(),stack.getHeight(),stack.getSize());
		Calibration cal = imp.getCalibration();
		if(cal != null) {
			// Amira's bounding box is actually the range of the CENTRES
			// of the outer voxels.
			// Of course this means that the bounding box is defined if any of x,y,z are 1
			// This is ususally only going to be a problem in z
			int zPosToGet = (stack==null?1:stack.getSize())-1;
			// investigating what amira does in these circs suggests that
			// it actually pretends that a single slice image does have a z thickness
			// equivalent to that of a 2 slice image
			if (zPosToGet==0) zPosToGet=1;
			// None of this determines what to do with the origin
			// it could reasonably be suggested that if an Amira image has origin 0,0,0
			// then the imagej origin should be displaced by dx/2,dy/2,dz/2 
			// ie half a voxel dimension in each axis
			// But since nearly everyone expects the default origin to be 0,0,0 
			// in both programs we are sligtly stuck.
			put("BoundingBox", cal.getX(0)+" "+
					cal.getX(imp.getWidth()-1)+" "+
					cal.getY(0)+" "+
					cal.getY(imp.getHeight()-1)+" "+
					cal.getZ(0)+" "+
					cal.getZ(zPosToGet));
		}
	}
	public static boolean isAmiraMesh(ImagePlus imp) {
		Object info1 = imp.getProperty("Info");
		if (info1 == null || !(info1 instanceof String))
			return false;
		String info = (String)info1;
		return (info.indexOf("CoordType") >= 0);
	}

	final public static boolean isAmiraLabelfield(ImagePlus imp) {
		Object info1 = imp.getProperty("Info");
		if (info1 == null || !(info1 instanceof String))
			return false;
		String info = (String)info1;
		return (info.indexOf("CoordType") >= 0
				&& info.indexOf("Materials") >= 0);
	}

	public static boolean isAmiraLabelfield(Properties p) {
		return (p!=null && p.get("Parameters")!=null
			&& ((Hashtable)p.get("Parameters")).get("Materials")!=null);
	}

	private static Pattern parameterKeyPattern;
	private static Pattern parameterStringValuePattern;
	private static Pattern parameterStringValuePattern2;
	private static Pattern parameterGroupValuePattern;
	private static Pattern parameterGroupEndPattern;
	private static Pattern colorPattern;

	private Properties parameters;
	private Vector materials;

	private void assertPatternsInitialized() {
		if(parameterKeyPattern==null) {
			parameterKeyPattern=Pattern.compile("\\A[ \t\n]*([-A-Za-z0-9_]+)[ \t]*(.*)\\z",Pattern.DOTALL);
			parameterStringValuePattern=Pattern.compile("\\A([^\n]*?)([,}]?)\n(.*)\\z",Pattern.DOTALL);
			parameterStringValuePattern2=Pattern.compile("\\A\"([^\"]*?)(\")\n(.*)\\z",Pattern.DOTALL);
			parameterGroupValuePattern=Pattern.compile("\\A\\{(.*)\\z",Pattern.DOTALL);
			parameterGroupEndPattern=Pattern.compile("\\A[ \t\n]*}(.*)\\z",Pattern.DOTALL);
			colorPattern=Pattern.compile("^([0-9]*(\\.[0-9]*e?-?[0-9]*)?)[ \t]+([0-9]*(\\.[0-9]*e?-?[0-9]*)?)[ \t]+([0-9]*(\\.[0-9]*e?-?[0-9]*)?)");
		}
	}

	String line;

	private boolean parseParameters(Hashtable map) {
		return parseParameters(map,false);
	}

	/* returns false, if read key/value pair was last of that level */
	private boolean parseParameters(Hashtable map,boolean isMaterial) {
		assertPatternsInitialized();
		Matcher m=parameterKeyPattern.matcher(line);
		if(m.matches()) {
			String key=m.group(1);
			line=m.group(2);
			Matcher m3=parameterGroupValuePattern.matcher(line);
			if(m3.matches()) {
				if(isMaterial) {
					if(materials==null)
						materials=new Vector();
					materials.add(key);
				}
				line=m3.group(1);
				Hashtable subMap=new Hashtable();
				map.put(key,subMap);
				while(parseParameters(subMap,key.equals("Materials")));
				return true;
			}
			Matcher m2=parameterStringValuePattern2.matcher(line);
			if (!m2.matches())
				m2=parameterStringValuePattern.matcher(line);
			if(m2.matches()) {
				String value=m2.group(1);
				String end=m2.group(2);
				line=m2.group(3);
				map.put(key,value);
				return !end.equals("}");
			}
			System.err.println("Warning: empty value for key "+key);
			map.put(key,"");
			return true;
		}
		Matcher m4=parameterGroupEndPattern.matcher(line);
		if(m4.matches()) {
			line=m4.group(1);
			return false;
		}
		System.err.println("This shouldn't happen: no key, and no '}': "+line);
		return false;
	}

	public Object get(String key) {
		Hashtable table = (Hashtable)parameters.get("Parameters");
		return table.get(key);
	}

	public void put(String key,Object value) {
		Hashtable table = (Hashtable)parameters.get("Parameters");
		table.put(key,value);
	}

	void initializeMaterials() {
		if(materials==null)
			materials=new Vector();
		String list=(String)parameters.get("MaterialList");
		if(list==null) {
			list="";
			for(int i=0;i<materials.size();i++)
				list+=(i>0?",":"")+(String)materials.get(i);
			parameters.put("MaterialList",list);
		} else {
			materials.clear();
			StringTokenizer t=new StringTokenizer(list,",");
			while(t.hasMoreTokens())
				materials.add(t.nextToken());
		}
	}

	/*
	public String[] getProperties() {
		if(!(parameters.get("Parameters") instanceof Hashtable))
			return null;
		return getProperties((Hashtable)parameters.get("Parameters"));
	}

	public String[] getProperties(Hashtable map) {
		String[] list=new String[map.size()];
		int i=0;
		for(Enumeration e=map.keys(); e.hasMoreElements();) {
			list[i++]=(String)e.nextElement();
		}
		return list;
	}
	*/

	public String toString() {
		String list="",prefix="\t";
		Hashtable t=(Hashtable)parameters.get("Parameters");
		if (t == null)
			return list;
		for(Enumeration e=t.keys();e.hasMoreElements();) {
			String key=(String)e.nextElement();
			list+=prefix+key+" "+getProperty(t,key,prefix)+"\n";
		}
		return list;
	}

	public String getProperty(String key) {
		if(key==null)
			return "";
		return getProperty((Hashtable)parameters.get("Parameters"),key,"");
	}

	public String getProperty(Hashtable map,String key,String prefix) {
		Object value=map.get(key);
		if(value==null)
			return "\"\"";
		if(value instanceof String)
			return (String)value;

		String end=prefix+"}";
		prefix+="\t";
		String result="{",sep="\n";
		// treat special case "Materials": preserve order
		Hashtable subMap=(Hashtable)value;
		Enumeration e=(key.equals("Materials")?materials.elements():subMap.keys());
		while(e.hasMoreElements()) {
			String subKey=(String)e.nextElement();
			String subValue=getProperty(subMap,subKey,prefix);
			result+=sep+prefix+subKey+" "+subValue;
			if(!subValue.equals("") && !subValue.endsWith("}"))
				sep=",\n";
			else
				sep="\n";
		}
		result+="\n"+end;
		return result;
	}

	public ColorModel getColorModel() {
		if(materials==null || materials.size()==0)
			return null;
		byte[] r=new byte[256];
		byte[] g=new byte[256];
		byte[] b=new byte[256];

		for(int i=0;i<getMaterialCount();i++) {
			double[] color=getMaterialColor(i);
			r[i]=(byte)Math.round(color[0]*255);
			g[i]=(byte)Math.round(color[1]*255);
			b[i]=(byte)Math.round(color[2]*255);
		}
		return new IndexColorModel(8,256,r,g,b);
	}

	public void setParameters(ImagePlus ip) {
		setParameters(ip, true);
	}

	public void setParameters(ImagePlus ip, boolean setCalib) {
		ip.setProperty("Info", toString());
		if (setCalib)
			setCalibration(ip);
		ColorModel c = getColorModel();
		if (c == null) {
			byte[] rLUT = new byte[256];
			byte[] gLUT = new byte[256];
			byte[] bLUT = new byte[256];
			for(int i=0; i<256; i++) {
				rLUT[i]=(byte)i;
				gLUT[i]=(byte)i;
				bLUT[i]=(byte)i;
			}
			c = new IndexColorModel(8, 256, rLUT, gLUT, bLUT);
		}
		ip.getProcessor().setColorModel(c);
		if (ip.getStackSize() > 1)
			ip.getStack().setColorModel(c);
	}

	public void setParameters(Properties prop) {
		for(Enumeration e=parameters.keys();e.hasMoreElements();) {
			String key=(String)e.nextElement();
			prop.put(key,parameters.get(key));
		}
	}

	public boolean changeLabelfieldToGray() {
		Hashtable m = (Hashtable)parameters.get("Parameters");
		if (parameters.get("MaterialList") == null &&
				materials == null &&
				(m == null || m.get("Materials") == null))
			return false;
		if (m != null)
			m.remove("Materials");
		materials = null;
		parameters.remove("MaterialList");
		return true;
	}

	public int addMaterial(String name,double red,double green,double blue) {
		Hashtable m=getMaterials();
		if(m==null) {
			m=new Hashtable();
			((Hashtable)parameters.get("Parameters")).put("Materials",m);
		}
		Hashtable color=new Hashtable();
		color.put("Color",red+" "+green+" "+blue);
		m.put(name,color);

		if(materials==null)
			materials=new Vector();
		materials.add(name);

		String list=(String)parameters.get("MaterialList");
		if(list==null)
			initializeMaterials();
		else
			parameters.put("MaterialList",list+","+name);

		return materials.size()-1;
	}

	public String[] getMaterialList() {
		int count = getMaterialCount();
		String[] materialList = new String[count];
		for (int i = 0; i < count; i++)
			materialList[i] = getMaterialName(i);
		return materialList;
	}

	public Hashtable getMaterials() {
		return (Hashtable)get("Materials");
	}

	public int getMaterialCount() {
		return materials.size();
	}

	public String getMaterialName(int id) {
		return (String)materials.get(id);
	}

	public int getMaterialID(String name) {
		for (int i = 0; i < materials.size(); i++)
			if (name.equals(materials.get(i)))
				return i;
		return -1;
	}

	public double[] getMaterialColor(int id) {
		double[] result=new double[3];
		assertPatternsInitialized();
		Hashtable m2=(Hashtable)getMaterials().get(materials.get(id));
		Object value=m2.get("Color");
		if(value==null) {
			result[0]=result[1]=result[2]=0;
		} else {
			Matcher matcher=colorPattern.matcher((String)value);
			if(matcher.matches()) {
				result[0]=Double.parseDouble(matcher.group(1));
				result[1]=Double.parseDouble(matcher.group(3));
				result[2]=Double.parseDouble(matcher.group(5));
			} else {
				result[0]=result[1]=result[2]=0;
			}
		}
		return result;
	}

	public boolean editMaterial(int id,String name,double red,double green,double blue) {
		if(id<0 || materials==null || id>=materials.size())
			return false;
		if(name==null)
			name=getMaterialName(id);
		double[] color=getMaterialColor(id);
		if(red<0||red>1)
			red=color[0];
		if(green<0||green>1)
			green=color[1];
		if(blue<0||blue>1)
			blue=color[2];

		materials.set(id,name);
		Hashtable c=new Hashtable();
		c.put("Color",red+" "+green+" "+blue);
		getMaterials().put(name,c);

		return true;
	}

	public void setCalibration(ImagePlus image) {
		width = image.getWidth();
		height = image.getHeight();
		ImageStack stack = image.getStack();
		depth = (stack==null?1:stack.getSize());

		Calibration cal = image.getCalibration();
		if(cal == null) {
			cal = new Calibration();
			image.setCalibration(cal);
		}
		setCalibration(cal);
	}
	public void setCalibration(Calibration cal) {
		String boundingBoxString = (String)get("BoundingBox");
		if (boundingBoxString == null)
			return;

		StringTokenizer boundingBox = new StringTokenizer(boundingBoxString);
		if(boundingBox.hasMoreTokens())
			cal.xOrigin = -Double.parseDouble(boundingBox.nextToken());
		if(boundingBox.hasMoreTokens()) {
			cal.pixelWidth = (Double.parseDouble(boundingBox.nextToken())+cal.xOrigin)/width;
			if(cal.pixelWidth!=0)
				cal.xOrigin/=cal.pixelWidth;
		}
		if(boundingBox.hasMoreTokens())
			cal.yOrigin = -Double.parseDouble(boundingBox.nextToken());
		if(boundingBox.hasMoreTokens()) {
			cal.pixelHeight = (Double.parseDouble(boundingBox.nextToken())+cal.yOrigin)/height;
			if(cal.pixelHeight!=0)
				cal.yOrigin/=cal.pixelHeight;
		}
		if(boundingBox.hasMoreTokens())
			cal.zOrigin = -Double.parseDouble(boundingBox.nextToken());
		if(boundingBox.hasMoreTokens()) {
			cal.pixelDepth = (Double.parseDouble(boundingBox.nextToken())+cal.zOrigin)/depth;
			if(cal.pixelDepth!=0)
				cal.zOrigin/=cal.pixelDepth;
		}
	}

	// GUI helpers
	public interface IsA {
		public boolean isA(Object o);
	}

	public static boolean addImageList(GenericDialog g, String title,
			String type, IsA isA) {
		Vector vector = new Vector();
		ImagePlus im;
		for(int i = 1; (im = WindowManager.getImage(i)) != null; i++)
			if (isA.isA(im))
				vector.add(im.getTitle());
		if (vector.size() < 1) {
			IJ.error("No " + type + " available");
			return false;
		}
		addChoice(g, title, vector);
		return true;
	}

	public static boolean addAmiraMeshList(GenericDialog g, String title) {
		return addImageList(g, title, "AmiraMesh",
			new IsA() {
				public boolean isA(Object o) {
					ImagePlus im = (ImagePlus)o;
					return isAmiraMesh(im);
				}
			}
		);
	}

	public static boolean addAmiraLabelsList(GenericDialog g, String t) {
		return addImageList(g, t, "AmiraLabel",
			new IsA() {
				public boolean isA(Object o) {
					ImagePlus im = (ImagePlus)o;
					return isAmiraLabelfield(im);
				}
			}
		);
	}

	public static boolean addAmiraTableList(GenericDialog g, String t) {
		Vector vector = new Vector();
		MenuBar mbar = Menus.getMenuBar();
		Menu menu = null;
		for (int i = 0; i < mbar.getMenuCount(); i++)
			if (mbar.getMenu(i).getLabel().equals("Window")) {
				menu = mbar.getMenu(i);
				break;
			}
		if (menu == null)
			throw new RuntimeException("no Window menu?");
		for (int i = 0; i < menu.getItemCount(); i++) {
			String title = menu.getItem(i).getLabel();
			Frame frame = WindowManager.getFrame(title);
			if (frame != null && frame instanceof AmiraTable)
				vector.add(title);
		}
		if (vector.size() < 1) {
			IJ.error("No AmiraTable available");
			return false;
		}
		addChoice(g, t, vector);
		return true;
	}

	public static int addWindowList(GenericDialog g, String title,
			boolean onlyWithAmiraParameters) {
		Vector v = new Vector();
		if (Interpreter.isBatchMode())
			v.add(Macro.getValue(Macro.getOptions(),
						"window", "(null)"));
		else {
			int count = WindowManager.getWindowCount();
			for (int i = 0; i < count; i++) {
				ImagePlus img = WindowManager.getImage(i + 1);
				if (onlyWithAmiraParameters &&
						!isAmiraMesh(img))
					continue;
				v.add(img.getTitle());
			}
		}
		ImagePlus image = WindowManager.getCurrentImage();
		if (image != null)
			return addChoice(g, title, v, image.getTitle());
		else
			return addChoice(g, title, v);
	}

	public static int addChoice(GenericDialog g, String title, Vector v) {
		return addChoice(g, title, v,
				v.size() > 0 ? (String)v.get(0) : "");
	}

	public static int addChoice(GenericDialog g, String title, Vector v,
			String defaultValue) {
		String[] list = new String[v.size()];
		boolean hasDefault = false;
		for (int i = 0; i < list.length; i++) {
			list[i] = (String)v.get(i);
			if (list[i].equals(defaultValue))
				hasDefault = true;
		}
		if (!hasDefault) {
			String[] newList = new String[list.length + 1];
			System.arraycopy(list, 0, newList, 1, list.length);
			list = newList;
			list[0] = defaultValue;
		}
		g.addChoice(title, list, defaultValue);
		return list.length;
	}

	public Material getMaterial(int id){
		return  new Material(getMaterialName(id),id,
				getMaterialColor(id));
	}


	public static class Material{
		public Material(String name, int id, double[] colors) {
			this.name = name;
			this.id = id;
			this.colors = colors;
		}

		public final String name;
		public final int id;
		public final double[] colors;

		public String toString(){return name;}
	}

	public static String defaultMaterialsString=
		"    Materials {\n"+
		"        Exterior {\n"+
		"            Id 1\n"+
		"        }\n"+
		"        medulla_r {\n"+
		"            Id -1,\n"+
		"            Color 1 0 0,\n"+
		"            Name \"outer_medulla_r\",\n"+
		"            Group \"OL_r\"\n"+
		"        }\n"+
		"        medulla_l {\n"+
		"            Id -1,\n"+
		"            Color 1 0 0,\n"+
		"            Name \"outer_medulla_l\",\n"+
		"            Group \"OL_l\"\n"+
		"        }\n"+
		"        lobula_r {\n"+
		"            Group \"OL_r\",\n"+
		"            Color 1 0.552326 0\n"+
		"        }\n"+
		"        lobula_l {\n"+
		"            Id -1,\n"+
		"            Color 1 0.552326 0,\n"+
		"            Group \"OL_l\"\n"+
		"        }\n"+
		"        lobula_plate_r {\n"+
		"            Id -1,\n"+
		"            Color 1 0.796512 0,\n"+
		"            Group \"OL_r\"\n"+
		"        }\n"+
		"        lobula_plate_l {\n"+
		"            Id -1,\n"+
		"            Color 1 0.802326 0,\n"+
		"            Group \"OL_l\"\n"+
		"        }\n"+
		"        mushroom_body_r {\n"+
		"            Id -1,\n"+
		"            Color 0.401163 0.0988372 0\n"+
		"        }\n"+
		"        mushroom_body_l {\n"+
		"            Id -1,\n"+
		"            Color 0.401163 0.104651 0\n"+
		"        }\n"+
		"        ellipsoid_body {\n"+
		"            Id -1,\n"+
		"            Color 0 0.619 0,\n"+
		"            Group \"CC\"\n"+
		"        }\n"+
		"        noduli {\n"+
		"            Id -1,\n"+
		"            Color 0.598837 1 0,\n"+
		"            Group \"CC\"\n"+
		"        }\n"+
		"        fan_shaped_body {\n"+
		"            Id -1,\n"+
		"            Color 0.110465 1 0.0404624,\n"+
		"            Group \"CC\"\n"+
		"        }\n"+
		"        protocerebral_bridge {\n"+
		"            Id -1,\n"+
		"            Color 0 0.373 0,\n"+
		"            Name \"protocebral_bridge\",\n"+
		"            Group \"CC\"\n"+
		"        }\n"+
		"        antennal_lobe_r {\n"+
		"            Id 18,\n"+
		"            Color 0.156863 0.45098 0.8\n"+
		"        }\n"+
		"        antennal_lobe_l {\n"+
		"            Id 19,\n"+
		"            Color 0.156863 0.45098 0.8\n"+
		"        }\n"+
		"        lateral_horn_r {\n"+
		"            Id 18,\n"+
		"            Color 0.57 0.3534 0.171,\n"+
		"            Name \"lateral horn\"\n"+
		"        }\n"+
		"        lateral_horn_l {\n"+
		"            Id 19,\n"+
		"            Color 0.57 0.352944 0.171\n"+
		"        }\n"+
		"    }\n";

	public static AmiraParameters defaultMaterials() {
		return new AmiraParameters("Parameters {\n"+
					   defaultMaterialsString+
					   "}\n");
	}

	public static String[] getWindowList() {
		Vector v = new Vector();
		MenuBar mbar = Menus.getMenuBar();
		Menu menu = null;
		for (int i = 0; i < mbar.getMenuCount(); i++)
			if (mbar.getMenu(i).getLabel().equals("Window")) {
				menu = mbar.getMenu(i);
				break;
			}
		if (menu == null)
			throw new RuntimeException("no Window menu?");
		for (int i = 0; i < WindowManager.getWindowCount(); i++) {
			ImagePlus img = WindowManager.getImage(i + 1);
			v.add(img.getTitle());
		}
		for (int i = 0; i < menu.getItemCount(); i++) {
			String title = menu.getItem(i).getLabel();
			if (WindowManager.getFrame(title) != null)
				v.add(title);
		}

		String[] result = new String[v.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = (String)v.get(i);
		return result;
	}

}

