/*
* Human Visible Tookit
*  Author: Nathaniel Gonzalez Santiago: CIAR- RCMI Puerto Rico Medical Sciences Campus
* 
*  This plugin is designed to improve and reduce the preprocessing process of the images
*  provided by the Human Visible Project: http://www.nlm.nih.gov/research/visible/visible_human.html. The features are:
*  1. Raw image batch conversion to JPEG,BMP and TIFF format. Many of the available programs do not work, or do not
*     allowed total control over raw image file format.
*     This is done taking into account each of the specified values for the anatomy,ct,mri Images
*  2. Batch image resize. The original size of the Anatomy images are 7.2 mb the size can be significantly reduced to do
*      more image proccesing like 3d rendering. This results in less memory demand. 
*  3. You have the ability of making you own macro with ImageJ and applied to each of the images you are preprocessing.
*  4. AutoAlign Feature. In the Images of the male set some misaligned were detected and I tried to automatically fix this problem.
*  5. Blue Gel Removal. The anatomy images are encased in some type of blue gel in order to eliminate that gel a filter was created
*     the value of sensibility can be adjusted. The default value (0.9)should be quite good for initial testing.
*/
/*
 1. Raw image batch conversion to JPEG,BMP,RAW and TIFF format. Many of the available programs do not work, or do not
     allowed total control over raw image file format.
     This is done taking into account each of the specified values for the anatomy,ct,mri Images
  2. Batch image resize. The original size of the Anatomy images are 7.2 mb the size can be significantly reduced to do
      more image proccesing like 3d rendering. This results in less memory demand. 
  3. You have the ability of making you own macro with ImageJ and applied to each of the images you are preprocessing.
  4. AutoAlign Feature. In the Images of the male set some misaligned were detected and I tried to automatically fix this problem.
  5. Blue Gel Removal. The anatomy images are encased in some type of blue gel in order to eliminate that gel a filter was created
     the value of sensibility can be adjusted. The default value (0.9)should be quite good for initial testing.

    Copyright (C) 2005  Nathaniel Gonzalez Santiago


This library is free software; you can redistribute it and/or modify it under the terms of
the GNU Lesser General Public License as published by the Free Software Foundation; either 
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
See the GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License along with this library; 
if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 MA 02111-1307 USA 


    
Also add information on how to contact you by electronic and paper mail.

Nathaniel Gonzalez Santiago
ngonzalez@rcm.upr.edu.
University of Puerto Rico, Medical Sciences Campus.

RCMI Program
Room 621-A,6th floor
Main Building Medical Sciences Campus
GPO Box 365067
San Juan, PR 00936-5067

*/


import ij.plugin.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import ij.*;
import ij.io.*;
import ij.process.*;
import ij.gui.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import ij.plugin.filter.*;


public class MultipleImageProcessor implements PlugIn{ 
   private JButton inputFolder, outputFolder,plain2Button,MacroRunner;
   private JTextField inputFolderField,outputFolderField,resizeBarField,MacroRunnerFileField,BlueSettingField;
   private JLabel inputFolderLabel,outputFolderLabel,resizeBarLabel, MacroRunnerFileLabel,BlueSettingLabel;
   private JComboBox saveChoicesBox,yesOrNoAligmentBox,yesOrNoBlueRemovalBox,origianlTypeBox,vhDataSetBox;  
   private static JSlider resizeBar;
   private static String[] saveChoices = {"Tiff", "8-bit Tiff", "Jpeg","Bmp"};
   private static String[] vhDataSet = {"Male", "Female"};
   private static String[] origianlType = {"Anatomy", "CT", "MRI"};
   private static String[] aligmentOpts = {"No","Yes"};
   private static String[] yesOrNoBlueRemoval = {"No","Yes"};
   private static String dtdefault = "Anatomy";
   private static String vhdsdefault = "Male";
   private static String aa = "No";
   private static String br = "No";
   private static String format = "Tiff";
   private static String title="Example";
   static String DirIn = "";
   static String DirOut = "";
   static String FileMacro = "";
   static double[] bcf = new double[1];
   static {bcf[0] = .9; }
   boolean sameSettings = false;
   boolean openImage = false;
   static boolean runmacro = false;	
   Hashtable maleTable; 
  
ImagePlus imp;
static boolean canceled;
int slice;






public void run(String arg)
   {
      
      GenericDialog od = new GenericDialog("Multiple Image Processor");
      vhDataSetBox = new JComboBox(vhDataSet);
      /*vhDataSetBox.addItemListener(
	new ItemListener() {
	public void itemStateChanged(ItemEvent e){

	if(  vhDataSetBox.getSelectedItem() == "Female" && !(origianlTypeBox.getSelectedItem() == "CT" || origianlTypeBox.getSelectedItem() == "MRI") ){
	yesOrNoAligmentBox.setEnabled( false );
        yesOrNoAligmentBox.setSelectedIndex( 0 );
	yesOrNoBlueRemovalBox.setEnabled( true );
	BlueSettingField.setEditable( true );
	}
	else if(vhDataSetBox.getSelectedItem() == "Female" && origianlTypeBox.getSelectedItem() == "Anatomy"){
	yesOrNoAligmentBox.setEnabled( false );
        yesOrNoAligmentBox.setSelectedIndex( 0 );

	}else if( vhDataSetBox.getSelectedItem() == "Male" && !(origianlTypeBox.getSelectedItem() == "CT" || origianlTypeBox.getSelectedItem() == "MRI") ){
	yesOrNoAligmentBox.setEnabled( true );
	yesOrNoAligmentBox.setSelectedIndex( 0 );
	yesOrNoBlueRemovalBox.setEnabled( true );
	BlueSettingField.setEditable( true );

	}

	}	
	}
      );*/

      







      saveChoicesBox =  new JComboBox(saveChoices);
      yesOrNoAligmentBox = new JComboBox(aligmentOpts);
      yesOrNoBlueRemovalBox = new JComboBox(yesOrNoBlueRemoval);

      origianlTypeBox = new JComboBox(origianlType);
      origianlTypeBox.addItemListener(
	new ItemListener() {
	public void itemStateChanged(ItemEvent e){

	if( origianlTypeBox.getSelectedItem() == "CT" || origianlTypeBox.getSelectedItem() == "MRI"){
	//JOptionPane.showMessageDialog(null,"hello");
	BlueSettingField.setEditable( false );
	yesOrNoAligmentBox.setEnabled( false );
 	yesOrNoAligmentBox.setSelectedIndex( 0 );
	yesOrNoBlueRemovalBox.setEnabled( false );
	yesOrNoBlueRemovalBox.setSelectedIndex( 0 );

	BlueSettingField.repaint();
	}else if(vhDataSetBox.getSelectedItem() == "Male"){
	BlueSettingField.setEditable( true );
	yesOrNoAligmentBox.setEnabled( true );
	yesOrNoBlueRemovalBox.setEnabled( true );
	BlueSettingField.repaint();
	
	}
	else if(vhDataSetBox.getSelectedItem() == "Female"){
	BlueSettingField.setEditable( true );
	yesOrNoAligmentBox.setSelectedIndex( 0 );
	yesOrNoAligmentBox.setEnabled( false );
	yesOrNoBlueRemovalBox.setEnabled( true );
	BlueSettingField.repaint();
	}


	}	
	}
      );



           
      JLabel outputFormatedType = new JLabel("Output Image Format Type :       ");
      JLabel autoAlignLabel = new JLabel("Anatomy Images Auto Align :     ");
      JLabel blueRemovalLabel = new JLabel("Apply Blue Gel Removal Filter:   ");
      JLabel vhDataSetLabel = new JLabel("Visible Human Data Set :           ");
      JLabel dataTypeLabel = new JLabel("Data Type :                                ");
      if(IJ.isWindows()){
         dataTypeLabel = new JLabel("Data Type :                                    ");
      }      
      JLabel dataLocationLabel = new JLabel("Data Location:                                ");
      JLabel BlueSettingLabel = new JLabel("Blue Threshold:");	






      Panel blueRemovalPanel,inputPanel,dataTypePanel,dataSetPanel,outputFolderPanel,outputDataTypePanel,autoAlignPanel,resizeSetPanel,MacroRunnerPanel,AboutPanel;
      
      AboutPanel = new Panel();
//      MenuBar menuBar = new MenuBar();
//      Menu menu = new Menu("About");
//      MenuItem ciarItem = new MenuItem("CIAR...");
//      MenuItem vhpItem = new MenuItem("VHP Preprocecing Program..");
      
      JMenuBar menuBar = new JMenuBar();
      JMenu menu = new JMenu("About");
      JMenuItem ciarItem = new JMenuItem("CIAR...");
      ActionListener lst = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        
        BrowserLauncher hb = new BrowserLauncher(); 
        
		try{
		hb.openURL("http://rcmi.rcm.upr.edu/research/inform.html");
      		}catch( Exception f ){
        	JOptionPane.showMessageDialog(null,"http://rcmi.rcm.upr.edu/research/inform.html");
		}

		}
      };
      ciarItem.addActionListener(lst);
    
	



      JMenuItem vhpItem = new JMenuItem("MIP Program..");
      ActionListener snd = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String AboutMessage = new String();
	AboutMessage = "Multiple Image Processor, CIAR, Nathaniel Gonzalez 2005";
 	/*AboutMessage = "Visible Human Preprocessing Toolkit Version 1.07 \n" +
 	"Author: Nathaniel Gonzalez Santiago: CIAR- RCMI Puerto Rico Medical Sciences Campus \n" +
	"\tWhen we started working with images from the Visible Human Project, we became \n " +
	"\taware that the pre-processing of the anatomical images (eliminating the blue gel, \n" +
	"\tcentering the images, eliminating labels) presented a set of unique challenges. \n" +
	"\tThe Visible Human Preprocessing Toolkit was created to provide a fast, easy, \n" +
	"\tcross-platform set of tools for pre-processing Visible Human Project images using ImageJ.\n" +
	"\tWe hope this approach will facilitate access to Visible Human images by non-programmers. \n\n" +
	
	"\tThe Toolkit allows the user to read and process both male and female data sets, which\n" +
	"\tconsist of anatomical, CT and MRI images in raw format.  Once the files are read, \n" +
	"\tthey can be processed in multiple ways. The Toolkit specifically includes a Blue Gel \n" +
	"\tRemoval tool, applicable to both male and female anatomical datasets, and an Align \n" +
	"\tFeature which will align images in the male anatomical  dataset. With the Toolkit, \n" +
	"\tthe user can also apply macros generated with ImageJ macro functions to the set of \n" +
	"\toriginal raw images. After doing the pre-processing, this application allows you to \n" +
	"\tsave images in any of a variety of formats and sizes.\n\n"  + 

	"\tThe Center for Information Architecture in Research is supported by a \n" + 
	"\tRCMI grant  (G12RR03051) from the National Center for Research Resources, NIH.\n";
*/

        BrowserLauncher hb = new BrowserLauncher(); 
	 JOptionPane.showMessageDialog(null,AboutMessage,"Multiple Image Processor",JOptionPane.INFORMATION_MESSAGE); 




	}




      };
      vhpItem.addActionListener(snd);


	
      menu.add(ciarItem);
      menu.add(vhpItem);

      menuBar.add(menu);
      AboutPanel.add(menuBar);

/*
      dataSetPanel = new Panel();
      dataSetPanel.setLayout(new GridLayout(1, 2,5,5));
      dataSetPanel.add(vhDataSetLabel);
      dataSetPanel.add(vhDataSetBox);
      
	
      dataTypePanel = new Panel();
      dataTypePanel.setLayout(new GridLayout(1, 2,5,5));
      dataTypePanel.add(dataTypeLabel);
      dataTypePanel.add(origianlTypeBox);
	*/
      outputDataTypePanel = new Panel();
      outputDataTypePanel.setLayout(new GridLayout(1, 2,5,5));
      outputDataTypePanel.add(outputFormatedType);
      outputDataTypePanel.add(saveChoicesBox);

/*
      autoAlignPanel = new Panel();
      autoAlignPanel.setLayout(new GridLayout(1, 2,5,5));
      autoAlignPanel.add(autoAlignLabel);
      autoAlignPanel.add(yesOrNoAligmentBox);

      blueRemovalPanel = new Panel();
      blueRemovalPanel.setLayout(new GridLayout(2, 2,5,5));
      BlueSettingField = new JTextField(5);




      blueRemovalPanel.add(blueRemovalLabel);
      blueRemovalPanel.add(yesOrNoBlueRemovalBox);
      blueRemovalPanel.add(BlueSettingLabel);
      BlueSettingField.setText(bcf[0]+"");
     
      blueRemovalPanel.add(BlueSettingField);

	
*/	




      // Input Folder Panel	
      inputFolderLabel = new JLabel("Input Folder:     ");
      inputFolderField = new JTextField(DirIn,11);
      inputPanel = new Panel();
      inputPanel.setLayout(new GridLayout(1, 3));
      inputFolder = new JButton( "SELECT" );
      inputPanel.add(inputFolderLabel);
      inputPanel.add(inputFolderField); 
      inputPanel.add(inputFolder);

      // Output Folder
      outputFolderLabel = new JLabel("Output Folder:     ");
      outputFolderField = new JTextField(DirOut,11);
      outputFolderPanel = new Panel();
      outputFolderPanel.setLayout(new GridLayout(1, 3));
      outputFolder = new JButton( "SELECT" );
      outputFolderPanel.add(outputFolderLabel);
      outputFolderPanel.add(outputFolderField); 
      outputFolderPanel.add(outputFolder);

      // Resize Bar
      

      resizeBarLabel = new JLabel("Resize Percent:        ");
      resizeBarField = new JTextField(4);
      resizeBarField.setText("100");
      resizeSetPanel = new Panel();
     // resizeSetPanel.setLayout(new GridLayout(1, 2));

      resizeBar = new JSlider(SwingConstants.HORIZONTAL,1,100,100);
      resizeBar.setMajorTickSpacing( 10 );
      resizeBar.setPaintTicks ( true );
      resizeSetPanel.add(resizeBarLabel);
      resizeSetPanel.add(resizeBarField);
      resizeSetPanel.add(resizeBar);

	// Macro Runner
      MacroRunnerFileLabel = new JLabel("Macro File:     ");
      MacroRunnerFileField = new JTextField(FileMacro,11);
      MacroRunnerPanel = new Panel();
      MacroRunnerPanel.setLayout(new GridLayout(1, 3));
      MacroRunner = new JButton( "SELECT" );
      MacroRunnerPanel.add(MacroRunnerFileLabel);
      MacroRunnerPanel.add(MacroRunnerFileField); 
      MacroRunnerPanel.add(MacroRunner);
	

	
      od.addPanel(AboutPanel,GridBagConstraints.EAST,new Insets(5, 0, 0, 25));
      //od.addPanel(dataSetPanel);
      //od.addPanel(dataTypePanel);
      //od.addMessage("Data Location:");
      od.addPanel(inputPanel);
      od.addPanel(outputFolderPanel);
      od.addPanel(resizeSetPanel);
      od.addPanel(MacroRunnerPanel);
      od.addPanel(outputDataTypePanel);
      //od.addPanel(autoAlignPanel);
      //od.addPanel(blueRemovalPanel);
     

           
 // create buttons
      	
     

     String ot = origianlTypeBox.getSelectedItem()+"";
     String ds = (String)vhDataSetBox.getSelectedItem();
     String br = (String)yesOrNoBlueRemovalBox.getSelectedItem();
     String al = (String)yesOrNoAligmentBox.getSelectedItem(); 
     String sc = (String)saveChoicesBox.getSelectedItem();
		
	

      // create an instance of inner class ButtonHandler
      // to use for button event handling 
      
      ButtonHandler handler = new ButtonHandler();
      outputFolder.addActionListener( handler );
      inputFolder.addActionListener( handler );
      MacroRunner.addActionListener( handler );
      ciarItem.addActionListener( handler ); 
     resizeBar.addChangeListener( new ChangeListener() {
      public void stateChanged( ChangeEvent e)
	{
	resizeBarField.setText(resizeBar.getValue() + "");
	resizeBarField.repaint(); 
	}
	}
 	);
      


      od.showDialog();
	
	if (od.wasCanceled()) {
		return;
	}
	else{
		if(((String)origianlTypeBox.getSelectedItem() == "CT" && (String)saveChoicesBox.getSelectedItem() == "Tiff")
			|| ((String)origianlTypeBox.getSelectedItem() == "MRI" && (String)saveChoicesBox.getSelectedItem() == "Tiff")){
			JOptionPane.showMessageDialog(null,"Image Cannot be Saved as a regular Tiff it will be instead be\n"
			+"saved as 8bit Tiff");
			saveChoicesBox.setSelectedItem("8-bit Tiff");
		}





	convert(DirIn, DirOut, (String)origianlTypeBox.getSelectedItem(), (String)saveChoicesBox.getSelectedItem(),
        (String)yesOrNoBlueRemovalBox.getSelectedItem(),(String)yesOrNoAligmentBox.getSelectedItem(),(String)vhDataSetBox.getSelectedItem());
 
	}




   }



/*-----------------------------------------------------------------------------------------------------------------*/ 
public void convert(String dir1, String dir2,String original,String format, String blueremoval, String autoalign,String vhDataSet) {
		IJ.resetEscape();
                IJ.log("");
		IJ.log("Converting to "+format);
		if (!dir2.endsWith(File.separator))
			dir2 += File.separator;
		IJ.log("dir1: "+dir1);
		IJ.log("dir2: "+dir2);
		String[] list = new File(dir1).list();
		if (list==null) return;
		
		openImage = false;
		if(runmacro ){
		openImage = true;
		}
			
		IJ.run("Colors...", "foreground=white background=black selection=yellow");
		


		for (int i=0; i<list.length; i++) {
			

			if(IJ.escapePressed()){
			break;
			} 
			IJ.log((i+1)+": "+list[i]);
			IJ.showStatus(i+"/"+list.length);
			File f = new File(dir1+list[i]);
			//if (!f.isDirectory() && (list[i].endsWith("raw" ) || list[i].endsWith("RAW" )) ) {
			if (!f.isDirectory() && (list[i].endsWith("jpeg" ) || list[i].endsWith("JPEG") || list[i].endsWith("jpg" ) || list[i].endsWith("JPG") || list[i].endsWith("bmp" ) || list[i].endsWith("BMP") || list[i].endsWith("gif" ) || list[i].endsWith("GIF") || list[i].endsWith("tiff" ) || list[i].endsWith("TIFF") || list[i].endsWith("tif" ) || list[i].endsWith("TIF") || list[i].endsWith("pgm") || list[i].endsWith("PGM") ) ) {

							
				ImagePlus img = new ImagePlus(dir1+"/"+list[i]);
										
				String parameter = new String();
				String parameter2 = new String();
				String parameter3= new String();
				parameter = "name=" + (String)list[i] + " type=RGB fill_with=black width=2148 height=1316 slices=1";
				parameter2 = "name=" + (String)list[i] + " type=RGB fill_with=black width=2048 height=1216 slices=1";
				parameter3 = "width=2048 height=1500 position=Top-Center zero";
				
											
			
				if(runmacro && FileMacro != null ){
				img.show();

				IJ.run("Select None");
				if(IJ.isWindows()){
				
         			IJ.runMacroFile(FileMacro,"");
			   	}else{      
				IJ.run("Run... ", "run=" + FileMacro);
				}
				IJ.run("Select None");
				}
								
			
						
				
				if (img!=null) {
										
					if (img!=null)
						save(img, dir2, format, autoalign,blueremoval);
	
				}
			}
		}
		IJ.showProgress(1.0);
		IJ.showStatus("");
}

public void save(ImagePlus img, String dir, String format, String autoalign,String blueremoval) {
		String name = img.getTitle();
		int dotIndex = name.lastIndexOf(".");
		if (dotIndex>=0)
			name = name.substring(0, dotIndex);
		String path = dir + name;
		double scale;
		if(resizeBar.getValue() != 100){ 
		scale = resizeBar.getValue() / 100.0 ;
		ImageProcessor ip = img.getProcessor();
		int width = ip.getWidth();
		int height = ip.getHeight();
		ip.setInterpolate(true);
		ip = ip.resize((int)(width*scale),(int)(height*scale));
		img.setProcessor(null,ip);
		}
		

		if (format.equals("Tiff"))
			new FileSaver(img).saveAsTiff(path+".tif");
		else if (format.equals("8-bit Tiff"))
			saveAs8bitTiff(img, path+".tif");
		else if (format.equals("Jpeg"))
			new FileSaver(img).saveAsJpeg(path+".jpg");
		else if (format.equals("Bmp"))
			new FileSaver(img).saveAsBmp(path+".bmp");
		else if (format.equals("Raw"))
			new FileSaver(img).saveAsRaw(path+".raw");

		
		if(runmacro ){
				IJ.run("Close");
		
		}
				
		}

public ImagePlus process(ImagePlus img) {
		//double xscale = 1.6;
		//double yscale = 1.1;
		//int width = img.getWidth();
		//int height = img.getHeight();
		//ImageProcessor ip = img.getProcessor();
		//ip.setInterpolate(true);
		//ip = ip.resize((int)(width*xscale), (int)(height*yscale));
		//img.setProcessor(null, ip);
		return img;
	}

public ImagePlus AlignImage(ImagePlus img, ImageProcessor ip) {
			
		return img;
	}





void saveAs8bitTiff(ImagePlus img, String path) {
		ImageProcessor ip = img.getProcessor();
		if (ip instanceof ColorProcessor)
			{ip = reduceColors(ip); img.setProcessor(null, ip);}
		else if ((ip instanceof ShortProcessor) || (ip instanceof FloatProcessor))
			{ip = ip.convertToByte(true); img.setProcessor(null, ip);}
		new FileSaver(img).saveAsTiff(path);
	}

ImageProcessor reduceColors(ImageProcessor ip) {
		MedianCut mc = new MedianCut((int[])ip.getPixels(), ip.getWidth(), ip.getHeight());
		Image img = mc.convert(256);
		return(new ByteProcessor(img));
	}

/*----------------------------------------------------------------------------------------------------------------------------*/








   // inner class for button event handling
   private class ButtonHandler implements ActionListener {
      public void actionPerformed( ActionEvent e )
      {
        


	String s = "";
	if( e.getSource() == inputFolder){
	JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Input Folder");
	fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);


	int result = fileChooser.showOpenDialog(null);
	DirIn = fileChooser.getSelectedFile()+"";  
	
	if(DirIn != null + ""){
	inputFolderField.setText(fileChooser.getSelectedFile() + "");
	inputFolderField.repaint(); 
	}
	}

	else if(e.getSource() == outputFolder){
	JFileChooser fileChooserOutput = new JFileChooser();
        fileChooserOutput.setDialogTitle("Output Folder");
	fileChooserOutput.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);


	int result = fileChooserOutput.showOpenDialog(null);
        DirOut = fileChooserOutput.getSelectedFile() + "";
	if(DirOut != null + ""){
	outputFolderField.setText(fileChooserOutput.getSelectedFile() + "");
	outputFolderField.repaint(); 
	}
	}
	else if(e.getSource() == MacroRunner){
	JFileChooser fileChooserOutput = new JFileChooser();
        fileChooserOutput.setDialogTitle("Macro Run File");
	fileChooserOutput.setFileSelectionMode(JFileChooser.FILES_ONLY);


	int result = fileChooserOutput.showOpenDialog(null);
        FileMacro = fileChooserOutput.getSelectedFile() + "";
	if(FileMacro != null + ""){
	MacroRunnerFileField.setText(fileChooserOutput.getSelectedFile() + "");
	MacroRunnerFileField.repaint(); 
	runmacro = true;
	}else{
	MacroRunnerFileField.setText("");
	runmacro = false;
	}
	


	}	
	
	

		

		}
   }










}



