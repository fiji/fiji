import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import ij.io.*;
import ij.*;
import ij.gui.*;
import ij.util.StringSorter;
import ij.plugin.PlugIn;
import ij.plugin.*;
import ij.process.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
/* Christopher Philip Mauer. Copyright (c) 2003.
Permission to use, copy, modify, and distribute this software for any purpose 
without fee is hereby granted, provided that this entire notice is included in 
all copies of any software which is or includes a copy or modification of this 
software and in all copies of the supporting documentation for such software.
Any for profit use of this software is expressly forbidden without first
obtaining the explicit consent of the author. 
THIS SOFTWARE IS BEING PROVIDED "AS IS", WITHOUT ANY EXPRESS OR IMPLIED WARRANTY. 
IN PARTICULAR, THE AUTHOR DOES NOT MAKE ANY REPRESENTATION OR WARRANTY 
OF ANY KIND CONCERNING THE MERCHANTABILITY OF THIS SOFTWARE OR ITS FITNESS FOR ANY 
PARTICULAR PURPOSE. 
*/
/* This PlugIn implements a recursive prediction/correction 
algorithm based on the Kalman Filter. The application for which
it was designed is cleaning up timelapse image streams. It operates
in constant space by opening the image files as needed, and closing
them when the computation has been completed.
																							Christopher Philip Mauer 
																							cpmauer@northwestern.edu
*/
public class Kalman_Filter implements PlugIn{
	
	protected static ImagePlus imp;
	protected static ImagePlus imp2; 
	protected static ImageStack stack;
	protected static ImageStack stack2;
	protected static ImageProcessor ip = null;
	protected static ImageProcessor ip2 = null;
	private String fileName = null;
	private String directory = null;
	private String savedirectory = null;
	private String savename = null;
	private String abspath = null;
	private double  numimages = 0;
	private double firstimage = 0;
	private String namewith = null;
	private String prefixname = null;
	private String fileList[] = null;
	private String fullDirectory[] = null;
	private static String filteredDirectory[] = null;
	private String newfileName = null;
	protected settingBar sbar = null; 
	private double gain = 0.80;
	private double percentvar = 0.05;/*spec this at runtime*/
	private double fps = 30;
	private boolean stopit = false;
	private boolean stop = true;
	private boolean save = false;
	private boolean test = true;
	
	public void run (String argv){
		boolean g = true;
		boolean f = getFiles();
		if(f==true){
			sbar = new settingBar();
			g = getStartSpecs();
			if(g==true){
				imp = new ImagePlus(filteredDirectory[0]);
				imp2 = new ImagePlus(filteredDirectory[0]);
			}
		}
		if(f==true&&g==true) Kalmanizer();
	}
	public boolean getFiles(){
		String directory = "";
		JFileChooser jfc = new JFileChooser();
		jfc.setCurrentDirectory(new File("."));
		int jfcval = jfc.showDialog(sbar, "Choose File");
		if (jfcval == JFileChooser.APPROVE_OPTION){  
			File thefile = jfc.getSelectedFile();
			fileName = thefile.getName();
			directory = thefile.getAbsolutePath();
			directory = directory.substring(0,directory.length()-fileName.length());
			prefixname = fileName.substring(0,fileName.length()-4);
		}
		if(fileName==null) return false;
		GenericDialog fname = new GenericDialog("Pick the naming convention..", IJ.getInstance());
		fname.addStringField("Enter the naming convention which is common to the files you wish to use", fileName, 40);
		fname.addStringField("Enter the naming convention for the save (will be appended with K######)", prefixname, 40);
		fname.addCheckbox("Specify save directory (the default is to save to the raw image directory)", false);
		fname.showDialog();
		if(fname.wasCanceled()) return false;
		namewith = fname.getNextString();
		savename = fname.getNextString();
		/*spec the save directory*/
		if(fname.getNextBoolean()==true){
			JFileChooser dirchooser = new JFileChooser();
			dirchooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int r = dirchooser.showDialog(sbar, "Choose Directory");
			try{
				savedirectory = dirchooser.getSelectedFile().getAbsolutePath();
			}
			catch(Exception e){return false;}
		}
		else savedirectory = directory;
		namewith = directory+namewith;
		/*strcat the absolutepath to the filename and order it*/  
		fullDirectory = new File(directory).list();
		for(int i=0;i<fullDirectory.length;++i)
			fullDirectory[i]=directory+fullDirectory[i];
		StringSorter.sort(fullDirectory);
		/*filter the directory based on the naming convention*/
		int j = 0;	
		for(int i=0;i<fullDirectory.length;++i)if(fullDirectory[i].startsWith(namewith))++j;
		filteredDirectory = new String[j];	
		j = 0;
		for(int i=0;i<fullDirectory.length;++i){
			if(fullDirectory[i].startsWith(namewith)){
				filteredDirectory[j] = fullDirectory[i];
				++j;
			}
		}
		newfileName = savedirectory+"\\"+savename+"K";/*for naming the saved files*/
		return true;
	}
	public boolean getStartSpecs(){
		numimages = filteredDirectory.length;
		if(numimages==0){
			IJ.showMessage("Improper Selection of Files");
			return false;
		}
		String ErrorMessage = 
		new String("One of your values was not properly formatted.\nThe default values will be used.");
		GenericDialog finfo = new GenericDialog("Pick the files..", IJ.getInstance());
		finfo.addNumericField("Enter the number of images (default is all)", numimages, 0);
		finfo.addNumericField("Enter the prediction bias", gain, 2);
		finfo.addNumericField("Enter the system noise estimate", percentvar, 2);
		finfo.addNumericField("Enter the frame per second rate", fps, 0);
		finfo.addCheckbox("Save the results?", save);
		finfo.showDialog();
		if(finfo.wasCanceled()) return false;
		numimages = finfo.getNextNumber();
		if(finfo.invalidNumber()) {
			IJ.showMessage("Error", "Invalid image number - Default employed");
			numimages = filteredDirectory.length;
		}
		gain = finfo.getNextNumber();
		if(finfo.invalidNumber()) {
			IJ.showMessage("Error", "Invalid gain number - Default employed");
			gain = 0.80;
		}
		percentvar = finfo.getNextNumber();
		if(finfo.invalidNumber()) {
			IJ.showMessage("Error", "Invalid variance - Default employed");
			percentvar = 0.05;
		}
		fps = finfo.getNextNumber();
		if(finfo.invalidNumber()) {
			IJ.showMessage("Error", "Invalid fps value - Default employed");
			fps = 30;
		}
		if(numimages> filteredDirectory.length){
			IJ.showMessage("Error", "Invalid image number. Default employed.");
			numimages = filteredDirectory.length;
		}
		if(percentvar>1.0||gain>1.0||percentvar<=0.0||gain<0.0){
			IJ.showMessage(ErrorMessage);
			percentvar = 0.05;
			gain = 0.80;
		}
		save = finfo.getNextBoolean();	
		sbar.gainval.setText(String.valueOf(gain));
		sbar.fpsval.setText(String.valueOf((int)fps));
		sbar.gslider.setValue((int)(gain*100));
		sbar.fpslider.setValue((int)fps);
		return true;
	}
	public void Kalmanizer(){
		int m = imp.getWidth();
		int n = imp.getHeight();
		int dimension = m*n;
		short stackslice[] = new short[dimension];
		short filteredslice[] = new short[dimension];
		double noisevar[] = new double[dimension];
		double average[] = new double[dimension];
		double predicted[] = new double[dimension];
		double predictedvar[] = new double[dimension];
		double observed[] = new double[dimension];
		double Kalman[] = new double[dimension];
		double corrected[] = new double[dimension];
		double correctedvar[] = new double[dimension];	
		ImagePlus img = null;
		Opener o = new Opener();
		try{
		stack = imp.createEmptyStack();
		}
		catch(Exception e){
			IJ.showMessage("Improper File Type: "+fileName);
			return;
		}
		stack.addSlice("raw",imp.getProcessor()); 
		stack2 = imp.createEmptyStack();
		stack2.addSlice("Kalman Filter",imp.getProcessor()); 
		//stack = new ImageStack(m,n,imp.getProcessor().getColorModel());
		imp.setStack("raw", stack);
		imp.show();
		imp.getWindow().setLocation(20,150);
		imp2.setStack("Kalman Filter", stack2);
		imp2.show();
		imp2.getWindow().setLocation(m+50,150);
		try{
		stackslice = (short[])stack.getPixels(1);
		}
		catch(Exception e){
			IJ.showMessage("KalmanFilter uses 16bit images");
			return;
		}
		stack.deleteLastSlice();
		stack2.deleteLastSlice();
		
		for (int i=0;i<dimension;++i) noisevar[i] = percentvar;
		while(stop!=false){
			predicted = short2double(stackslice);
			predictedvar = noisevar;
			long millis = 0;
			stopit = false;
			for(int i=0;i<numimages;++i){
				millis = (long)(1000/fps);/*fps spec at runtime*/
				imp.setTitle("raw "+String.valueOf(i+1)+"/"+String.valueOf((int)numimages));
				imp2.setTitle("Kalman Filter "+String.valueOf(i+1)+"/"+String.valueOf((int)numimages));
				if(stopit==true) break;
				try{
					Thread.sleep(millis);
				}
				catch(Exception e){}
				img = o.openImage(filteredDirectory[i]);
				ip = img.getProcessor();
				stackslice = (short[])ip.getPixels();
				ip = imp.getProcessor();
				ip.setPixels(stackslice);
				stack.addSlice(String.valueOf(i), ip);
				stackslice = (short[])stack.getPixels(1);
				imp.setStack(String.valueOf(i),stack);	
				imp.setSlice(i);
				stack.deleteLastSlice();
				imp.setProcessor(null,ip);
				observed = short2double(stackslice);
				for(int k=0;k<Kalman.length;++k){/*calculate the KalmanGain*/
					Kalman[k] = predictedvar[k]/(predictedvar[k]+noisevar[k]);
				}
				for(int k=0;k<corrected.length;++k){/*allow for the gain*/
					corrected[k] = gain*predicted[k]+(1.0-gain)*observed[k]
					+Kalman[k]*(observed[k] - predicted[k]);
				}
				for(int k=0;k<correctedvar.length;++k){
					correctedvar[k] = predictedvar[k]*(1.0 - Kalman[k]);
				}
				predictedvar = correctedvar;
				predicted = corrected;
				filteredslice = double2short(corrected);
				ip2 = imp2.getProcessor();
				ip2.setPixels(filteredslice);
				stack2.addSlice(String.valueOf(i), ip2);
				imp2.setStack(String.valueOf(i),stack2);	
				imp2.setSlice(i);
				stack2.deleteLastSlice();
				/*write the data to file*/
				if(save==true){
					String x = String.valueOf(i);
					String s = "00000"+i;
					String ss = s.substring(s.length()-6);
					new FileSaver(imp2).saveAsTiff(newfileName+ss+".tif"); 
				}
			}
			Object[] options = {"Run again","Run again and save","Exit"};
			int choice = JOptionPane.showOptionDialog(sbar,"How would you like to proceed?","Kalman Filter",			
			JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE,null,options,
			options[0]);
			if(choice==0){
				stop = true;
				save = false;
			}
			if(choice==1){
				stop = true;
				save = true;
			}
			if(choice==2){
				stop = false;
				save = false;
			}			
		}
	}
	public short[] double2short(double array[]){
		short shortarray[] = new short[array.length];
		for(int j=0;j<array.length;++j){
			shortarray[j] = (short)((array[j]*65535)-32768);
		}
		return shortarray;
	}
	public double[] short2double(short array[]){
		double doublearray[] = new double[array.length];
		for(int j=0;j<array.length;++j){
			doublearray[j] = (((double)array[j])+32768)/65535;
		}
		return doublearray;
	}
	class settingBar extends JFrame implements ActionListener{
		private JFrame frame = null;
		private JPanel panel1 = null;
		private JPanel panel2 = null;
		private Container cp = null;
		protected JLabel gainval = null;
		protected JLabel fpsval = null;
		private JButton stopper = null;
		protected JSlider gslider = null;
		protected JSlider fpslider = null;
		settingBar(){
			frame = new JFrame();
			panel1 = new JPanel();
			panel2 = new JPanel();
			frame.setTitle("Kalman Filter");
			frame.setSize(400, 120);
			frame.setLocation(20,20);
			cp = frame.getContentPane();
			cp.setLayout(new FlowLayout(FlowLayout.LEFT));
			stopper = new JButton("stop filter");
			stopper.addActionListener(this);
			gainval = new JLabel(String.valueOf(gain),4);	
			fpsval = new JLabel(String.valueOf(fps),4);	
			fpslider = new JSlider(1,60,(int)fps);
			fpslider.setPaintTicks(true);
			fpslider.setMajorTickSpacing(6);
			fpslider.setMinorTickSpacing(3);
			fpslider.addChangeListener(new fpsListener());
			panel1 = new JPanel();
			panel1.add(fpslider);
			panel1.add(new JLabel("fps"));
			panel1.add(fpsval);
			cp.add(panel1, BorderLayout.NORTH);
			Hashtable labelTable = new Hashtable();
			for(int i=0;i<=100;i=i+20)labelTable.put(new Integer(i),
			new JLabel(String.valueOf((double)i/100.0)));
			gslider = new JSlider(0,100,(int)(gain*100));
			gslider.setLabelTable(labelTable);
			gslider.setPaintTicks(true);
			gslider.setPaintLabels(true);
			gslider.setMajorTickSpacing(20);
			gslider.setMinorTickSpacing(05);
			gslider.addChangeListener(new gainListener());
			panel2 = new JPanel();
			panel2.add(gslider);
			panel2.add(new JLabel("gain"));
			panel2.add(gainval);
			panel2.add(stopper);
			cp.add(panel2, BorderLayout.SOUTH);
			frame.show();
		}
		class gainListener implements ChangeListener{
			JSlider source = null;
			public void stateChanged(ChangeEvent e) {
				source = (JSlider)e.getSource();
				double gn = (double)source.getValue();
				gn = gn/100;
				gainval.setText(String.valueOf(gn));
				gain = gn;
			}
		}	
		class fpsListener implements ChangeListener{
			JSlider source = null;
			public void stateChanged(ChangeEvent e) {
				source = (JSlider)e.getSource();
				fps = (double)source.getValue();
				fpsval.setText(String.valueOf((int)fps));
			}
		}	
		public synchronized void actionPerformed(ActionEvent ae){
			if(ae.getSource()==stopper){
				stopit = true;
			}
		}
	}
}

