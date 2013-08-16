package CLI;
/**
 *
 * Command Line Interface plugin for ImageJ(C).
 * Copyright (C) 2004 Albert Cardona.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
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
 * You may contact Albert Cardona at albert at pensament.net, and at http://www.lallum.org/java/index.html
 *
 * **/


/* VERSION: 1.0
 * RELEASE DATE: 2004-09-27
 * AUTHOR: Albert Cardona at albert at pensament.net
 */

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.Image;
import java.io.File;
import java.awt.Component;
import javax.swing.JTextArea;

public class Screenshot implements Runnable {

	Component component;
	int seconds;
	String dir;
	String file_name;
	
	String report = "";
	JTextArea out = null;

	String getReport() {
		return report;
	}

	public void setOut(JTextArea jta) {
		this.out = jta;
	}
	
	Screenshot(Component component, int seconds, String dir, String file_name) {
		this.component = component;
		this.seconds = seconds;
		this.dir = dir;
		this.file_name = file_name;
	}

	public void run() {
		
        	if(!IJ.isJava2()) {
            		report += "\n-->  Screen Grabber - Java 1.3 or later required.\n";
            		return;
        	}
		
		if (null == dir) {
			dir = System.getProperty("user.dir");
		}
		if (null == file_name) {
			file_name = "screenshot.jpg";
		}
		String path = dir + System.getProperty("file.separator") + file_name;

		File f = new File(path);
		int i = 1;
		while(f.exists()) {
			f = new File(path.substring(0, path.length()-4) + "_" + i + ".jpg");
			i++;
		}

        	try {
            		Robot robot = new Robot();
			Rectangle r;
			
			if(null == component) {
				Toolkit toolkit = Toolkit.getDefaultToolkit();
            			Dimension dimension = toolkit.getScreenSize();
            			r = new Rectangle(dimension);
			} else {
				//get component coordinates:
				r = new Rectangle(component.getX(), component.getY(), component.getWidth(), component.getHeight());
			}
			robot.delay(seconds * 1000);
			Image img = robot.createScreenCapture(r);
			
			if(img != null) {
				new FileSaver(new ImagePlus("s", img)).saveAsJpeg(f.getAbsolutePath());
			}
        	} catch(Exception e) {
			IJ.write("An error ocurred: " + e);
		}
		
		report += "\n-->  Screenshot saved at:\n-->  " + f.getAbsolutePath() + "\n";

		if (null != out) {
			out.append(report);
		}
    	}
}
