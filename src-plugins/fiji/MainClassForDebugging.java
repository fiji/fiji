package fiji;

import ij.IJ;
import ij.Menus;
import ij.ImageJ;
import java.io.File;

public class MainClassForDebugging {

	static int foo;
	static String className;

	public static void main(String args[]) {
		if(IJ.getInstance()==null) {
			new ImageJ();
		}
		String path="";
		int i=0;
		while(i<args.length-1) {

			path+=args[i];
			i++;
			path+=" ";
		}
		path+=args[i];
		className=findClassName(path);
		try {
			IJ.runPlugIn(className,"");
		} catch(Exception e) { e.printStackTrace(); }
		IJ.getInstance().dispose();

	}

	public static String findClassName(String path) {
		String c = path;
		String c1= path;
        if (c.endsWith(".java")) {
			c = c.substring(0, c.length() - 5);
		}
		String pluginsPath = Menus.getPlugInsPath();
		File f1=new File(pluginsPath);
		if (!pluginsPath.endsWith(File.separator))
			pluginsPath += File.separator;
		boolean check=false;
		while(c1.lastIndexOf(File.separator)>0) {
			File f2=new File(c1);
			if(f2.compareTo(f1)==0) {
				check=true;
				break;
			}else
				c1=c1.substring(0,c1.lastIndexOf(File.separator));
		}
		if(check) {
			c=c.substring(c1.length());
			while(c.startsWith(File.separator))
				c=c.substring(1);
		}
		return(c.replace('/','.'));
	}

}

