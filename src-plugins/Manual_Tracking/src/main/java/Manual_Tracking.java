/*Manual tracking v2.0, 15/06/05
    Fabrice P Cordelières, fabrice.cordelieres at curie.u-psud.fr
New features:
2D centring correction added
Directionality check added
Previous track files may be reloaded
3D features added (retrieve z coordinates, quantification and 3D representation as VRML file)
 */

import adapter.Image5DAdapter;
import adapter.ImageAdapter;

import java.awt.*;
import java.awt.event.*;
import java.awt.SystemColor;
import java.io.*;
import java.lang.*;
import java.util.StringTokenizer;
import ij.*;
import ij.gui.*;
import ij.io.*;
import ij.measure.*;
import ij.plugin.Converter;
import ij.plugin.frame.*;
import ij.plugin.filter.*;
import ij.plugin.filter.Duplicater;
import ij.process.*;
import ij.process.StackConverter;
import ij.util.*;
import ij.util.Tools.*;




public class Manual_Tracking extends PlugInFrame implements ActionListener, ItemListener, MouseListener {
    
    
    //Calibration related variables---------------------------------------------
    double calxy=0.129; //This value may be changed to meet your camera caracteristics
    double calz=0.3; //This value may be changed to meet your piezo/Z-stepper caracteristics
    double calt=2; //This value may be changed to meet your timelapse settings
    int cent=5; //Default side size for the square where the center is searched
    int dotsize=5; // Drawing parameter: default dot size
    double linewidth=1; // Drawing parameter: default line width
    int fontsize=12; // Drawing parameter: default font size
    Color[] col={Color.blue,Color.green,Color.red,Color.cyan,Color.magenta,Color.yellow,Color.white}; //Values for color in the drawing options
    
    
    //Universal variables-------------------------------------------------------
    int i;
    int j;
    int k;
    int l;
    int m;
    int n;
    String txt;
    
    
    //Interface related variables-----------------------------------------------
    static Frame instance;
    Font bold = new Font("",3,12);
    Panel panel;
    //Tracking
    Button butAdd;
    Button butDlp;
    Button butEnd;
    Button butDel;
    Choice trackdel;
    Button butDelAll;
    Checkbox checkPath;
    //Centring
    Choice choicecent;
    Checkbox checkCent;
    Label labelCorr;
    //Directionality
    Button butAddRef;
    Button butDelRef;
    Label titleRef;
    Checkbox checkShowRef;
    Checkbox checkRef;
    //Drawing
    Button butOvd;
    Button butOvl;
    Button butOvdl;
    Button butOverdots;
    Button butOverlines;
    Button butOverboth;
    Checkbox checkText;
    //Load/Param/Retrieve
    Button butLoad;
    Checkbox checkParam;
    Button butRetrieveZ;
    //Parameters
    Label labelEmpty1;
    Label labelEmpty2;
    Label labelEmpty3;
    Label labelEmpty4;
    Label labelParam;
    Label labelEmpty5;
    Label labelTime;
    TextField caltfield;
    Choice choicecalt;
    Label labelxy;
    TextField calxyfield;
    Choice choicecalxy;
    Label labelz;
    TextField calzfield;
    Label labelEmpty6;
    Label labelCent;
    Label labelPix;
    TextField centsize;
    Label labelDot;
    TextField dotsizefield;
    Label labelEmpty7;
    Label labelLine;
    TextField linewidthfield;
    Label labelEmpty8;
    Label labelFont;
    TextField fontsizefield;
    Label labelEmpty9;
    
    
    //Image related variables---------------------------------------------------
    ImagePlus img;
    String imgtitle;
    int Width;
    int Height;
    int Depth;
    int Slice;
    String SliceTitle;
    ImageCanvas canvas;
    ImagePlus ip;
    ImageStack stack;
    ImageWindow win;
    StackConverter sc;
    Duplicater dp;
    
    
    //Tracking related variables------------------------------------------------
    boolean islistening=false; //True as long as the user is tracking
    
    int[] xRoi; //Defines the ROI to be shown using the 'Show path' option - x coordinates
    int[] yRoi; //Defines the ROI to be shown using the 'Show path' option - y coordinates
    Roi roi; //ROI
    int Nbtrack=1; // Number of tracks
    int NbPoint=1; // Number of tracked points in the current track
    int ox; //x coordinate of the current tracked point
    int oy; //y coordinate of the current tracked point
    int PixVal; //intensity of the current tracked point
    int prevx; //x coordinate of the previous tracked point
    int prevy; //y coordinate of the previous tracked point
    double Distance; //Distance between (ox,oy) and (prevx, prevy)
    double Velocity; //Distance/calt
    int pprevx; //x coordinate of the antepenultimate tracked point
    int pprevy; //y coordinate of the antepenultimate tracked point
    boolean trackZ; // whether Z or time is tracked
    
    
    //Centring correction related variables--------------------------------------
    String commentCorr; //Stores the tracked point coordinates and the corrected point coordinates
    
    
    //Reference related variables-----------------------------------------------
    boolean islisteningRef=false; // True when the add reference button has been clicked.
    boolean RefSet=false; // True if a reference has already been set
    int DirIndex=1; //1 for anterograde movement, -1 for retrograde movement
    int refx=0; // x coordinate of the reference pixel
    int refy=0; // y coordinate of the reference pixel
    Roi roiRef; // Circular region drawn around the reference
    
    
    //Dialog boxes--------------------------------------------------------------
    GenericDialog gd;
    GenericDialog gd1;
    GenericDialog gd2;
    GenericDialog VRMLgd;
    OpenDialog od;
    SaveDialog sd;
    String FileName; // Filename with extension
    String File; // Filename without extension
    String dir; // Directory
    
    
    //Results tables------------------------------------------------------------
    ResultsTable rt; //2D results table
    ResultsTable rtmp; // Temporary results table
    String[] head={"Track n°","Slice n°","X","Y","Distance","Velocity","Pixel Value"}; //2D results table's headings
    ResultsTable rt3D; //3D results table
    
    //Load Previous Track File related variables--------------------------------
    BufferedReader in; //Input file
    String line; //Input line from the input file
    StringTokenizer Token; //used to separate tab delimited values in the imported file
     
    
    //Retrieve z coordinates dialog box & variables-----------------------------
    String[] CentringArray={"No centring correction", "Barycentre in signal box", "Max intensity in signal box"}; //List of options in the centring correction choicelist
    int Centring; //3D centring option n°
    int sglBoxx; //Width of the signal box
    int sglBoxy; //Height of the signal box
    int sglBoxz; //Depth of the signal box
    int bkgdBoxx; //Width of the background box
    int bkgdBoxy; //Height of the background box
    int bkgdBoxz; //Depth of the background box
    String[] QuantificationArray={"No background correction", "Bkgd box centred on sgl box", "Bkgd box on top left" , "Bkgd box on top right" , "Bkgd box on bottom left", "Bkgd box on bottom right"}; //List of options in the quantification settings choicelist
    int Quantification; //3D quantification option n°
    boolean DoQuantification; //True if the Do quantification checkbox is checked
    boolean DoBleachCorr; //True if the Do bleaching correction checkbox is checked
    boolean DoVRML; //True if the Export 3D+t data as a VRML file checkbox is checked
            
    
    //3D centring correction related variables----------------------------------
    int tmpx; //Temporary x value
    int tmpy; //Temporary y value
    int tmpz; //Temporary z value
    int tmpttl; //Temporary sum of all pixels' values in the signal box
    int tmppixval; //Intensity of the current pixel
    
    
    //Quantification related variables------------------------------------------
    int limsx1; //Left limit of the signal box
    int limsx2; //Right limit of the signal box
    int limsy1; //Upper limit of the signal box
    int limsy2; //Lower limit of the signal box
    int limsz1; //Top limit of the signal box
    int limsz2; //Bottom limit of the signal box
    double sizeSgl; //Number of voxel in the signal box
    int limbx1; //Left limit of the background box
    int limbx2; //Right limit of the background box
    int limby1; //Upper limit of the background box
    int limby2; //Lower limit of the background box
    int limbz1; //Top limit of the background box
    int limbz2; //Bottom limit of the background box
    double sizeBkgd; //Number of voxel in the background box
    double Qsgl; //Summed intensities of the voxels inside the signal box
    double Qbkgd; //Summed intensities of the voxels inside the background box
    double Qttl; //Summed intensities of the whole pixels in the stack at current time
    double Qttl0; //Summed intensities of the whole pixels in the stack at the first time of the current track
    double QSglBkgdCorr; //QSgl background corrected
    double QSglBkgdBleachCorr; //QSgl background and bleaching corrected
    
    
    //VRML export related variables---------------------------------------------
    String[] StaticArray={"None", "Trajectories", "Objects"}; //List of options in the static view choicelist
    String[] DynamicArray={"None", "Objects", "Objects & Static Trajectories", "Objects & Progressive Trajectories"}; //List of options in the dynamic view choicelist
    String Static; //Designation of the static view selected, to be added to the destination filename
    String Dynamic; //Designation of the dynamic view selected, to be added to the destination filename
    boolean StaticView; //True if a static view has to be generated
    boolean StaticViewObj; //True if a static view of the objects has to be generated
    boolean StaticViewTraj; //True if a static view of the trajectories has to be generated
    boolean DynamicView; //True if a dynamic view has to be generated
    boolean DynamicViewStaticTraj; //True if a dynamic view of the objects overlayed to a static view of trajectories has to be generated
    boolean DynamicViewDynamicTraj; //True if a dynamic view of the objects overlayed to a dynamic view of trajectories has to be generated
    String dirVRMLstat; //Path to save the static VRML view
    OutputStreamWriter oswStat; //Output file for VRML static view
    String dirVRMLdynam; //Path to save the dynamic VRML view
    OutputStreamWriter oswDynam; //Output file for VRML dynamic view
    String[] vrmlCol={"0 0 1", "0 1 0", "1 0 0", "0.5 1 1", "1 0.5 1","1 1 0","1 1 1"}; //Values for colors in the VRML file
    int x; //Variable to store x coordinate read from the 3D results table
    int y; //Variable to store y coordinate read from the 3D results table
    int z; //Variable to store z coordinate read from the 3D results table
    int xOld; //Variable to store previous x coordinate read from the 3D results table
    int yOld; //Variable to store previous y coordinate read from the 3D results table
    int zOld; //Variable to store previous z coordinate read from the 3D results table
    int [][] VRMLarray; //1st dimension: line n° from the 3D results table; 2nd dimension: 0-Tag (track n°/color); 1-time; 2-x, 3-y; 4-z
    int vrmlCount; //Number of tracks modulo 6: will define the color applied to the track
    double DistOfView; //Distance between the object and the camera in the VRML view
    double minTime; //Minimum timepoint where a track is started
    double maxTime; //Maximum timepoint where a track is ended
    int countBefore; //Difference between the current track startpoint and minTime
    int countAfter;//Difference between the current track endpoint and maxTime
    int countTtl; //Difference between countBefore and countAfter
    String key; //Defines the animation's keyframes
    int Tag; //Track number
    int TagOld; //Previous track number
    String point; //Stores the xyz coordinates (modified by calibration) of the current point from the current track
    int pointNb; //Number of points in the current track
    String pointKey; //Stores the xyz coordinates (modified by calibration) of each point from the current track
    String lastPoint; //Stores the xyz coordinates (modified by calibration) of the last point from the current track
    
    protected ImageAdapter adapter;
    
    public Manual_Tracking() {
        super("Tracking");
        instance=this;

        try {
		Class.forName("i5d.gui.Image5DWindow");
		adapter = new Image5DAdapter();
        } catch (ClassNotFoundException e) {
		adapter = new ImageAdapter();
        }

        //Interface setup ------------------------------------------------------
        panel = new Panel();
        panel.setLayout(new GridLayout(0,3));
        panel.setBackground(SystemColor.control);
        
        
        //---------------------------------Tracking
        panel.add(new Label());
        Label title=new Label();
        title.setText("Tracking :");
        title.setFont(bold);
        panel.add(title);
        panel.add(new Label());
        
        butAdd = new Button("Add track");
        butAdd.addActionListener(this);
        panel.add(butAdd);
        
        butDlp = new Button("Delete last point");
        butDlp.addActionListener(this);
        panel.add(butDlp);
        
        butEnd = new Button("End track");
        butEnd.addActionListener(this);
        panel.add(butEnd);
        
        //***
        butDel = new Button("Delete track n°");
        butDel.addActionListener(this);
        panel.add(butDel);
        trackdel = new Choice();
        panel.add(trackdel);
        
        butDelAll = new Button("Delete all tracks");
        butDelAll.addActionListener(this);
        panel.add(butDelAll);
        
        //***
        panel.add(new Label());
        checkPath=new Checkbox("Show path ?", false);
        checkPath.addItemListener(this);
        panel.add(checkPath);
        panel.add(new Label());
        
        //***
        panel.add(new Label());
        panel.add(new Label());
        panel.add(new Label());
        
        
        ///---------------------------------Centring
        panel.add(new Label());
        Label title3=new Label();
        title3.setText("Centring Correction:");
        title3.setFont(bold);
        panel.add(title3);
        panel.add(new Label());
        
        //***
        panel.add(new Label("Centring option :"));
        choicecent = new Choice();
        choicecent.add("Local maximum");
        choicecent.add("Local minimum");
        choicecent.add("Local barycentre");
        panel.add(choicecent);
        labelCorr=new Label();
        panel.add(labelCorr);
        
        //***
        panel.add(new Label());
        checkCent=new Checkbox("Use centring correction ?", false);
        checkCent.addItemListener(this);
        panel.add(checkCent);
        panel.add(new Label());
        
        //***
        panel.add(new Label());
        panel.add(new Label());
        panel.add(new Label());
        
        
        //---------------------------------Directionality
        panel.add(new Label());
        Label title4=new Label();
        title4.setText("Directionality :");
        title4.setFont(bold);
        panel.add(title4);
        panel.add(new Label());
        
        //***
        butAddRef = new Button("Add reference");
        butAddRef.addActionListener(this);
        panel.add(butAddRef);
        
        butDelRef = new Button("Delete reference");
        butDelRef.addActionListener(this);
        panel.add(butDelRef);
        
        titleRef=new Label();
        titleRef.setText("No reference set");
        panel.add(titleRef);
        
        //***
        panel.add(new Label());
        checkShowRef=new Checkbox("Show reference ?", false);
        checkShowRef.addItemListener(this);
        panel.add(checkShowRef);
        checkRef=new Checkbox("Use directionality ?", false);
        checkRef.addItemListener(this);
        panel.add(checkRef);
        
        //***
        panel.add(new Label());
        panel.add(new Label());
        panel.add(new Label());
        
        
        //---------------------------------Drawing
        panel.add(new Label());
        Label title2=new Label();
        title2.setText("Drawing :");
        title2.setFont(bold);
        panel.add(title2);
        panel.add(new Label());
        
        //***
        butOvd = new Button("Dots");
        butOvd.addActionListener(this);
        panel.add(butOvd);
        butOvl = new Button("Progressive Lines");
        butOvl.addActionListener(this);
        panel.add(butOvl);
        butOvdl = new Button("Dots & Lines");
        butOvdl.addActionListener(this);
        panel.add(butOvdl);
        
        //***
        butOverdots = new Button("Overlay Dots");
        butOverdots.addActionListener(this);
        panel.add(butOverdots);
        butOverlines = new Button("Overlay Lines");
        butOverlines.addActionListener(this);
        panel.add(butOverlines);
        butOverboth = new Button("Overlay Dots & Lines");
        butOverboth.addActionListener(this);
        panel.add(butOverboth);
        
        //***
        panel.add(new Label());
        checkText=new Checkbox("Show text ?", false);
        checkText.addItemListener(this);
        panel.add(checkText);
        panel.add(new Label());
        
        //***
        panel.add(new Label());
        panel.add(new Label());
        panel.add(new Label());
        
        
        //---------------------------------Load Previous Table/Parameters ?/Retrieve z
        butLoad = new Button("Load Previous Track File");
        butLoad.addActionListener(this);
        panel.add(butLoad);
        checkParam=new Checkbox("Show parameters ?", true);
        checkParam.addItemListener(this);
        panel.add(checkParam);
        butRetrieveZ = new Button("Retrieve Z Coordinates");
        butRetrieveZ.addActionListener(this);
        panel.add(butRetrieveZ);
        
        
        //---------------------------------Setup of the hiddeable paramters menu
        labelEmpty1=new Label();
        labelEmpty2=new Label();
        labelEmpty3=new Label();
        labelEmpty4=new Label();
        labelEmpty5=new Label();
        labelEmpty6=new Label();
        labelEmpty7=new Label();
        labelEmpty8=new Label();
        labelEmpty9=new Label();
        
        
        //---------------------------------Parameters
        panel.add(labelEmpty1);
        panel.add(labelEmpty2);
        panel.add(labelEmpty3);
        
        //***
        panel.add(labelEmpty4);
        labelParam=new Label("Parameters :");
        labelParam.setFont(bold);
        panel.add(labelParam);
        panel.add(labelEmpty5);
        
        //***
        labelTime=new Label("Time Interval :");
        panel.add(labelTime);
        caltfield = new TextField(Double.toString(calt));
        panel.add(caltfield);
        choicecalt = new Choice();
        choicecalt.add("sec");
        choicecalt.add("min");
        choicecalt.add("unit");
        panel.add(choicecalt);
        
        //***
        labelxy=new Label("x/y calibration :");
        panel.add(labelxy);
        calxyfield = new TextField(Double.toString(calxy));
        panel.add(calxyfield);
        choicecalxy = new Choice();
        choicecalxy.add("nm");
        choicecalxy.add("µm");
        choicecalxy.add("unit");
        choicecalxy.select("µm");
        panel.add(choicecalxy);
        
        //***
        labelz=new Label("z calibration :");
        panel.add(labelz);
        calzfield = new TextField(Double.toString(calz));
        panel.add(calzfield);
        panel.add(labelEmpty6);
        
        //***
        labelCent=new Label("Search square size for centring:");
        panel.add(labelCent);
        centsize = new TextField(Double.toString(cent));
        panel.add(centsize);
        labelPix=new Label(" pixels");
        panel.add(labelPix);
        
        //***
        labelDot=new Label("Dot size :");
        panel.add(labelDot);
        dotsizefield = new TextField(Double.toString(dotsize));
        panel.add(dotsizefield);
        panel.add(labelEmpty7);
        
        //***
        labelLine=new Label("Line width :");
        panel.add(labelLine);
        linewidthfield = new TextField(Double.toString(linewidth));
        panel.add(linewidthfield);
        panel.add(labelEmpty8);
        
        //***
        labelFont=new Label("Font size :");
        panel.add(labelFont);
        fontsizefield = new TextField(Double.toString(fontsize));
        panel.add(fontsizefield);
        panel.add(labelEmpty9);
        
        
        
        add(panel,BorderLayout.CENTER);
        pack();
        show();
        IJ.showProgress(2,1);
        rt=new ResultsTable();
        
    }

    /**
     * Moves to a given position in either time or z in the given
     * image, depending on what is tracked.
     */
    protected void moveTo(ImagePlus img, int position) {
        if (trackZ)
            adapter.setSlice(img, position);
        else
            adapter.setFrame(img, position);
    }

    /**
     * Gets either the current slice or the current frame, depending
     * on what is tracked.
     */
    protected int getPosition(ImagePlus img) {
        if (trackZ)
            return adapter.getSlice(img);
        else
            return adapter.getFrame(img);
    }

    /**
     * Gets the number of slices or frames available in the image,
     * depending on whit is tracked.
     */
    protected int getMaxPosition(ImagePlus img) {
        if (trackZ)
            return img.getNSlices();
        else
            return img.getNFrames();
    }

    public void itemStateChanged(ItemEvent e) {
        // Show/Hide the current path-------------------------------------------
        if (e.getSource() == checkPath) {
            if (checkPath.getState()) {img.setRoi(roi);
            } else {
                img.killRoi();
            }
            checkShowRef.setState(false);
        }
        
        // Enable/Disable the centring correction-------------------------------
        if (e.getSource() == checkCent) {
            if (!checkCent.getState()) commentCorr="";
            labelCorr.setText(commentCorr);
        }
        
        // Show/Hide reference position-----------------------------------------
        if (e.getSource() == checkShowRef) {
            if (checkShowRef.getState()) {
                if (!RefSet) {
                    IJ.showMessage("!!! Warning !!!", " No reference set:\nClick on 'Add reference' first !!!");
                    checkShowRef.setState(false);
                    return;
                }
                dotsize=(int) Tools.parseDouble(dotsizefield.getText());
                roiRef= new OvalRoi(refx-dotsize, refy-dotsize, 2*dotsize, 2*dotsize);
                img.setRoi(roiRef);
            } else {
                img.killRoi();
            }
            checkPath.setState(false);
        }
        
        // Show/Hide Parameters-------------------------------------------------
        if (e.getSource() == checkParam) {
            if (!checkParam.getState()){
                HideParam();
            }else{
                ShowParam();
            }
        }
    }
    
    
    
    public void actionPerformed(ActionEvent e) {
        // Button Add Track pressed---------------------------------------------
        if (e.getSource() == butAdd) {
            if (islistening){
                IJ.showMessage("This operation can't be completed:\na track is already being followed...");
                return;
            }
            HideParam();
            img=WindowManager.getCurrentImage();
            imgtitle = img.getTitle();
            if (imgtitle.indexOf(".")!=-1) imgtitle=imgtitle.substring(0,imgtitle.indexOf("."));
            calt=Tools.parseDouble(caltfield.getText());
            calxy=Tools.parseDouble(calxyfield.getText());
            if (calt==0 || calxy==0) {
                IJ.showMessage("Error", "Calibration values\n"+"should not be equal to zero !!!");
                ShowParam();
                return;
            }
            IJ.setTool(7);

            // assume time tracking when more than one frame is present
            trackZ = img.getNSlices() > 1 && img.getNFrames() == 1;
            
            xRoi=new int[getMaxPosition(img)];
            yRoi=new int[getMaxPosition(img)];
            
            if (img==null){
                IJ.showMessage("Error", "Man,\n"+"You're in deep troubles:\n"+"no opened stack...");
                return;
            }
            
            win = img.getWindow();
            canvas=win.getCanvas();
            moveTo(img, 1);
            
            NbPoint=1;
            IJ.showProgress(2,1);
            canvas.addMouseListener(this);
            islistening=true;
            return;
        }
        
        // Button Delete last point pressed-------------------------------------
        if (e.getSource() == butDlp) {
            gd = new GenericDialog("Delete last point");
            gd.addMessage("Are you sure you want to \n" + "delete last point ?");
            gd.showDialog();
            if (gd.wasCanceled()) return;
            
            //Create a temporary ResultTable and copy only the non deleted data
            rtmp=new ResultsTable();
            for (i=0; i<(rt.getCounter()); i++) {
                rtmp.incrementCounter();
                for (j=0; j<7; j++) rtmp.addValue(j, rt.getValue(j,i));
            }
            
            rt.reset();
            
            //Copy data back to original table except last point
            
            for (i=0; i<head.length; i++) rt.setHeading(i,head[i]);
            
            for (i=0; i<((rtmp.getCounter())-1); i++) {
                rt.incrementCounter();
                for (j=0; j<7; j++) rt.addValue(j, rtmp.getValue(j,i));
            }
            rt.show("Results from "+imgtitle+" in "+choicecalxy.getItem(choicecalxy.getSelectedIndex())+" per "+choicecalt.getItem(choicecalt.getSelectedIndex()));
            
            //Manage case where the deleted point is the last of a serie
            if (islistening==false) {
                Nbtrack--;
                trackdel.remove(""+(int) rt.getValue(0,rt.getCounter()-1));
                canvas.addMouseListener(this);
                islistening=true;
            }
            
            prevx=(int) rt.getValue(2, rt.getCounter()-1);
            prevy=(int) rt.getValue(3, rt.getCounter()-1);
            moveTo(img, ((int) rt.getValue(1, rt.getCounter()-1))+1);
            IJ.showStatus("Last Point Deleted !");
        }
        
        // Button End Track pressed---------------------------------------------
        if (e.getSource() == butEnd) {
            trackdel.add(""+Nbtrack);
            Nbtrack++;
            canvas.removeMouseListener(this);
            islistening=false;
            IJ.showStatus("Tracking is over");
            IJ.showProgress(2,1);
            return;
        }
        
        // Button Del Track pressed---------------------------------------------
        if (e.getSource() == butDel) {
            canvas.removeMouseListener(this);
            islistening=false;
            int tracktodelete= (int) Tools.parseDouble(trackdel.getItem(trackdel.getSelectedIndex()));
            gd = new GenericDialog("Delete Track n°" + tracktodelete);
            gd.addMessage("Do you want to \n" + "delete track n°" + tracktodelete + " ?");
            gd.showDialog();
            if (gd.wasCanceled()) return;
            
            //Create a temporary ResultTable and copy only the non deleted data
            rtmp=new ResultsTable();
            for (i=0; i<(rt.getCounter()); i++) {
                int nbtrack=(int) rt.getValue(0,i);
                if(nbtrack!=tracktodelete){
                    rtmp.incrementCounter();
                    for (j=0; j<7; j++) rtmp.addValue(j, rt.getValue(j,i));
                }
            }
            
            rt.reset();
            
            //Copy data back to original table
            
            for (i=0; i<head.length; i++) rt.setHeading(i,head[i]);
            
            for (i=0; i<(rtmp.getCounter()); i++) {
                rt.incrementCounter();
                for (j=0; j<7; j++){
                    if (j==0 & rtmp.getValue(0,i)>tracktodelete){
                        rt.addValue(j, rtmp.getValue(j,i)-1);
                    } else {
                        rt.addValue(j, rtmp.getValue(j,i));
                    }
                }
            }
            
            rt.show("Results from "+imgtitle+" in "+choicecalxy.getItem(choicecalxy.getSelectedIndex())+" per "+choicecalt.getItem(choicecalt.getSelectedIndex()));
            trackdel.removeAll();
            for (i=1;i<(rt.getValue(0,rt.getCounter()-1))+1;i++){
                trackdel.add(""+i);
            }
            IJ.showStatus("Track n°"+tracktodelete +" Deleted !");
            Nbtrack=((int) rt.getValue(0,rt.getCounter()-1))+1;
        }
        
        // Button Del All Tracks pressed----------------------------------------
        if (e.getSource() == butDelAll) {
            canvas.removeMouseListener(this);
            islistening=false;
            IJ.showProgress(2,1);
            IJ.showStatus("Tracking is over");
            gd = new GenericDialog("Delete All Tracks");
            gd.addMessage("Do you want to \n" + "delete all measurements ?");
            gd.showDialog();
            if (gd.wasCanceled()) return;
            rt.reset();
            rt.show("Results from "+imgtitle+" in "+choicecalxy.getItem(choicecalxy.getSelectedIndex())+" per "+choicecalt.getItem(choicecalt.getSelectedIndex()));
            trackdel.removeAll();
            IJ.showStatus("All Tracks Deleted !");
            Nbtrack=1;
            return;
        }
        
        // Button Add Reference pressed-----------------------------------------
        if (e.getSource() == butAddRef) {
            if (islistening) {
                gd= new GenericDialog("!!! Warning !!!");
                gd.addMessage("Adding an origin will end your current track.\nDo you want to continue ?");
                gd.showDialog();
                if (gd.wasCanceled()) return;
                
                trackdel.add(""+Nbtrack);
                Nbtrack++;
                canvas.removeMouseListener(this);
                islistening=false;
                IJ.showStatus("Tracking is over");
                IJ.showProgress(2,1);
            }
            
            img=WindowManager.getCurrentImage();
            if (img==null){
                IJ.showMessage("Error", "Man,\n"+"You're in deep troubles:\n"+"no opened stack...");
                return;
            }
            
            if (RefSet) {
                gd1= new GenericDialog("!!! Warning !!!");
                gd1.addMessage("An origin has already been defined.\nAre you sure you want to delete it ?");
                gd1.showDialog();
                if (gd1.wasCanceled()) return;
            }
            
            gd2 = new GenericDialog("Define reference");
            gd2.addMessage("Click on the pixel \nto be considered as reference");
            gd2.showDialog();
            if (gd2.wasCanceled()) return;
            
            ImageWindow win = img.getWindow();
            canvas=win.getCanvas();
            NbPoint=1;
            
            islisteningRef=true;
            canvas.addMouseListener(this);
            islistening=true;
            return;
        }
        
        // Button Delete reference pressed--------------------------------------
        if (e.getSource() == butDelRef) {
            gd= new GenericDialog("!!! Warning !!!");
            gd.addMessage("Are you sure you want to delete the reference ?");
            gd.showDialog();
            if (gd.wasCanceled()) return;
            refx=0;
            refy=0;
            RefSet=false;
            checkCent.setState(false);
            IJ.showStatus("Reference deleted!");
            titleRef.setText("No reference set");
            return;
        }
        
        // Buttons Dots, Lines or Dots & Lines pressed--------------------------------------------------
        if (e.getSource() == butOvd || e.getSource() == butOvl ||e.getSource() == butOvdl) {
            img.killRoi();
            canvas.removeMouseListener(this);
            islistening=false;
            if (e.getSource()==butOvd) txt="Dots ";
            if (e.getSource()==butOvl) txt="Progressive Lines ";
            if (e.getSource()==butOvdl) txt="Dots & Lines ";
            ip=NewImage.createRGBImage(txt+imgtitle,img.getWidth(),img.getHeight(),img.getStackSize(),1);
            ip.show();
            stack=ip.getStack();
            if (e.getSource()==butOvd || e.getSource()==butOvdl) Dots();
            if (e.getSource()==butOvl || e.getSource()==butOvdl) ProLines();
            IJ.showStatus(txt+imgtitle+" Created !");
            return;
        }
                
        // Button Overlay Dots, Overlay Lines or Overlay Dots & Lines pressed------------------------------------------
        if (e.getSource() == butOverdots || e.getSource() == butOverlines || e.getSource() == butOverboth) {
            img.killRoi();
            canvas.removeMouseListener(this);
            islistening=false;
            if (e.getSource()==butOverdots) txt="Overlay Dots ";
            if (e.getSource()==butOverlines) txt="Overlay Progressive Lines ";
            if (e.getSource()==butOverboth) txt="Overlay Dots & Lines ";
            ip=(new Duplicater()).duplicateStack(img, txt+imgtitle);
            ip.show();
            (new StackConverter(ip)).convertToRGB();
            stack=ip.getStack();
            if (e.getSource()==butOverdots || e.getSource()==butOverboth) Dots();
            if (e.getSource()==butOverlines || e.getSource()==butOverboth) ProLines();
            IJ.showStatus(txt+imgtitle+" Created !");
            return;
        }
                    
        // Button Load Previous Track File pressed------------------------------
        if (e.getSource() == butLoad) {
            gd = new GenericDialog("Load Previous Track File");
            gd.addMessage("Are you sure you want to \nload a previous Track file ?\nAll non saved data will be lost");
            gd.showDialog();
            if (gd.wasCanceled()) return;
            img=WindowManager.getCurrentImage();
            Width=img.getWidth();
            Height=img.getHeight();
            imgtitle = img.getTitle();
            if (imgtitle.indexOf(".")!=-1) imgtitle=imgtitle.substring(0,imgtitle.indexOf("."));
            
            od = new OpenDialog("Select the track file", "");
            if (od.getFileName()==null) return;
            File=od.getFileName();
            FileName=File.substring(0,File.indexOf("."));
            dir=od.getDirectory();
            i=0;
            rt.reset();
            
            try{
                if (i==0) in = new BufferedReader(new FileReader(dir+File));
                in.readLine();
                while ((line=in.readLine())!=null){
                    i=0;
                    Token=new StringTokenizer(line);
                    rt.incrementCounter();
                    while (Token.hasMoreTokens()){
                        if (i!=0){
                            rt.addValue(i-1, Tools.parseDouble(Token.nextToken()));
                        } else {
                            Token.nextToken();
                        }
                        i++;
                    }
                }
            } catch ( IOException f ) {
                IJ.error("Error...");
            }
            
            
            for (i=0; i<head.length; i++) rt.setHeading(i,head[i]);
            trackdel.removeAll();
            Nbtrack=(int) (rt.getValue(0,rt.getCounter()-1))+1;
            for (i=1;i<Nbtrack;i++) trackdel.add(""+i);
            rt.show("Results from "+imgtitle+" in "+choicecalxy.getItem(choicecalxy.getSelectedIndex())+" per "+choicecalt.getItem(choicecalt.getSelectedIndex()));
            
            ImageWindow win = img.getWindow();
            canvas=win.getCanvas();
            
            
        }
        
        // Button Retrieve Z Coordinates pressed--------------------------------
        if (e.getSource() == butRetrieveZ) {
            if (IJ.versionLessThan("1.34k")){
                IJ.showMessage("To use this functionality, ImageJ version must be 1.34k or higher.\nSave your track file, update ImageJ, reload the stack and track file,\nand try again.");
                return;
            }
        
            img=WindowManager.getCurrentImage();
            img=WindowManager.getCurrentImage();
            imgtitle = img.getTitle();
            if (imgtitle.indexOf(".")!=-1) imgtitle=imgtitle.substring(0,imgtitle.indexOf("."));
            calt=Tools.parseDouble(caltfield.getText());
            calxy=Tools.parseDouble(calxyfield.getText());
            calz=Tools.parseDouble(calzfield.getText());
            if (calt==0 || calxy==0 || calz==0) {
                IJ.showMessage("Error", "Calibration values\n"+"should not be equal to zero !!!");
                return;
            }
            
            if (img==null || rt.getCounter()==0){
                IJ.showMessage("Error", "No opened stack or No tracking data");
                return;
            }
            
            gd= new GenericDialog("Retrieve Z Coordinate");
            gd.addChoice("Centring Correction", CentringArray, CentringArray[0]);
            gd.addMessage("Size of signal box (pixels):");
            gd.addNumericField("Width", 5,0);
            gd.addNumericField("Height", 5,0);
            gd.addNumericField("Depth", 3,0);
            gd.addMessage("Size of background box (pixels):");
            gd.addNumericField("Width", 7,0);
            gd.addNumericField("Height", 7,0);
            gd.addNumericField("Depth", 5,0);
            gd.addMessage("");
            gd.addCheckbox("Do quantification", true);
            gd.addChoice("Quantification settings", QuantificationArray, QuantificationArray[1]);
            gd.addMessage("");
            gd.addCheckbox("Do bleaching correction", true);
            gd.addMessage("");
            gd.addCheckbox("Export 3D+t data as a VRML file", true);
            gd.showDialog();
            if (gd.wasCanceled()) return;
            
            Centring=gd.getNextChoiceIndex();
            sglBoxx=(int) gd.getNextNumber()/2;
            sglBoxy=(int) gd.getNextNumber()/2;
            sglBoxz=(int) gd.getNextNumber()/2;
            bkgdBoxx=(int) gd.getNextNumber()/2;
            bkgdBoxy=(int) gd.getNextNumber()/2;
            bkgdBoxz=(int) gd.getNextNumber()/2;
            DoQuantification=gd.getNextBoolean();
            Quantification=gd.getNextChoiceIndex();
            DoBleachCorr=gd.getNextBoolean();
            DoVRML=gd.getNextBoolean();
            
            od = new OpenDialog("Select the stacks' source folder", "" , "---Source Folder---");
            if (od.getDirectory()==null) return;
            dir=od.getDirectory();
            
            ResultsTable rt3D=new ResultsTable();
            
            TagOld=0;
            
            if (DoVRML){
                Static="";
                StaticView=true;
                StaticViewTraj=false;
                StaticViewObj=false;
                Dynamic="";
                DynamicView=true;
                DynamicViewStaticTraj=false;
                VRMLgd= new GenericDialog("VRML file");
                VRMLgd.addChoice("Static view",StaticArray,StaticArray[1]);
                VRMLgd.addMessage("");
                VRMLgd.addChoice("Dynamic view",DynamicArray,DynamicArray[1]);;
                
                VRMLgd.showDialog();
                if (VRMLgd.wasCanceled()) return;
                
                switch(VRMLgd.getNextChoiceIndex()){
                    case 0: StaticView=false; break;
                    case 1: StaticViewTraj=true; Static="_static-Trajectories.wrl"; break;
                    case 2: StaticViewObj=true; Static="_static-Objects.wrl";break;
                }
                
                switch(VRMLgd.getNextChoiceIndex()){
                    case 0: DynamicView=false; break;
                    case 1: Dynamic="_dynamic-Objects.wrl"; break;
                    case 2: DynamicViewStaticTraj=true; Dynamic="_dynamic-Objects & Static Trajectories.wrl"; break;
                    case 3: DynamicViewDynamicTraj=true; Dynamic="_dynamic-Objects & Dynamic Trajectories.wrl"; break;
                }
                
                if (!StaticView && !DynamicView) DoVRML=false;
            }
            
            
            if (DoVRML){
                sd = new SaveDialog("Select destination folder for the VRML file", "---VRML Files Destination Folder---", "");
                FileName=imgtitle;
                if (FileName.indexOf(".")!=-1) FileName=FileName.substring(0,FileName.indexOf("."));
                
                if (StaticView) dirVRMLstat = sd.getDirectory()+FileName+Static;
                if (DynamicView) dirVRMLdynam = sd.getDirectory()+FileName+Dynamic;
                VRMLarray=new int[rt.getCounter()][5];
            }
            
            String[] head3D={"Track n°","Timepoint ("+choicecalt.getItem(choicecalt.getSelectedIndex())+")","X","Y","Z","Distance","Velocity","Quantif sgl","Nb voxels sgl ("+(sglBoxx*2+1)+"x"+(sglBoxy*2+1)+"x"+(sglBoxz*2+1)+" px)","Quantif bkgd","Nb voxels bkgd ("+(bkgdBoxx*2+1)+"x"+(bkgdBoxy*2+1)+"x"+(bkgdBoxz*2+1)+" px)","Sgl bkgd corr","Sgl bkgd bleach corr","Quantif ttl"};
            
            for (i=0; i<head3D.length; i++) rt3D.setHeading(i,head3D[i]);
            
            for (i=0; i<rt.getCounter(); i++){
                IJ.showProgress(i,rt.getCounter()-1);
                rt3D.incrementCounter();
                
                Tag=(int) rt.getValue(0,i);
                Slice=(int) rt.getValue(1, i);
                x=(int) rt.getValue(2, i);
                y=(int)rt.getValue(3, i);
                z=0;
                PixVal=(int)rt.getValue(6, i);
                
                SliceTitle=img.getStack().getSliceLabel(Slice);
                if (SliceTitle==null){
                    IJ.showMessage("Error", "Each z projection (slice) must carry\nthe corresponding filename as a label");
                    IJ.showProgress(2,1);
                    return;
                }
                
                File f = new File(dir+"/"+SliceTitle);
                if (!f.isDirectory()) {
                    ip = new Opener().openImage(dir, SliceTitle);
                    if (ip!=null){
                        Width=ip.getWidth();
                        Height=ip.getHeight();
                        Depth=ip.getStackSize();
                        
                        for (j=1;j<ip.getStackSize()+1;j++){
                            ip.setSlice(j);
                            if (PixVal==ip.getProcessor().getPixel(x,y))z=j;
                        }
                        if (Centring!=0) Center3D();
                        rt3D.addValue(0, Tag);
                        rt3D.addValue(1, (Slice-1)*calt);
                        rt3D.addValue(2, x);
                        rt3D.addValue(3, y);
                        rt3D.addValue(4, z);
                        
                        if (DoVRML){
                            VRMLarray[i][0]=Tag;
                            VRMLarray[i][1]=(int) Math.round((Slice-1)*calt);
                            VRMLarray[i][2]=x;
                            VRMLarray[i][3]=y;
                            VRMLarray[i][4]=z;
                        }
                        
                        if (Tag!=TagOld || rt3D.getCounter()==1) {
                            if (minTime>(Slice-1)*calt) minTime=(Slice-1)*calt;
                            rt3D.addValue(5, -1);
                            rt3D.addValue(6, -1);
                        } else {
                            if (maxTime<(Slice-1)*calt) maxTime=(Slice-1)*calt;
                            Distance=Math.sqrt(Math.pow(calxy*(x-xOld),2)+Math.pow(calxy*(y-yOld),2)+Math.pow(calz*(z-zOld),2));
                            Velocity=Distance/calt;
                            rt3D.addValue(5, Distance);
                            rt3D.addValue(6, Velocity);
                        }
                        
                        
                        if (DoQuantification){
                            quantify();
                            if (Tag!=TagOld)Qttl0=Qttl;
                            rt3D.addValue(7,Qsgl);
                            rt3D.addValue(8,sizeSgl);
                            rt3D.addValue(9,Qbkgd);
                            rt3D.addValue(10,sizeBkgd);
                            rt3D.addValue(11,QSglBkgdCorr);
                            if (DoBleachCorr){
                                QSglBkgdBleachCorr=QSglBkgdCorr*Qttl0/Qttl;
                                rt3D.addValue(12,QSglBkgdBleachCorr);
                                rt3D.addValue(13,Qttl);
                                
                            }
                        }
                    }
                }
                TagOld=Tag;
                xOld=x;
                yOld=y;
                zOld=z;
                ip.flush();
            }
            if (DoQuantification){
                rt3D.show("3D Results from "+imgtitle+" in "+choicecalxy.getItem(choicecalxy.getSelectedIndex())+" per "+choicecalt.getItem(choicecalt.getSelectedIndex())+", "+CentringArray[Centring]+", "+QuantificationArray[Quantification]);
            }else{
                rt3D.show("3D Results from "+imgtitle+" in "+choicecalxy.getItem(choicecalxy.getSelectedIndex())+" per "+choicecalt.getItem(choicecalt.getSelectedIndex())+", "+CentringArray[Centring]);
            }
            if (DoVRML) DoVRML();
            IJ.showProgress(2,1);
        }
        
    }
    
    // Click on image-----------------------------------------------------------
    public void mouseReleased(MouseEvent m) {
        if (!islisteningRef){
            IJ.showProgress(getPosition(img), getMaxPosition(img));
            if (trackZ) {
                IJ.showStatus("Tracking slice " + adapter.getSlice(img) + " of " + img.getNSlices());
            } else {
                IJ.showStatus("Tracking frame " + adapter.getFrame(img) + " of " + img.getNFrames());
            }
            if (Nbtrack==1 && NbPoint==1){
                for (i=0; i<head.length; i++) rt.setHeading(i,head[i]);
            }
        }
        
        img.killRoi();
        checkShowRef.setState(false);
        
        int x=m.getX();
        int y=m.getY();
        ox=canvas.offScreenX(x);
        oy=canvas.offScreenY(y);
        if (checkCent.getState()) Center2D();
        
        if (islisteningRef){
            canvas.removeMouseListener(this);
            islistening=false;
            islisteningRef=false;
            refx=ox;
            refy=oy;
            IJ.showStatus("Reference set to ("+refx+","+refy+")");
            titleRef.setText("Reference set to ("+refx+","+refy+")");
            RefSet=true;
            checkRef.setState(true);
            dotsize=(int) Tools.parseDouble(dotsizefield.getText());
            roiRef= new OvalRoi(refx-dotsize, refy-dotsize, 2*dotsize, 2*dotsize);
            img.setRoi(roiRef);
            checkShowRef.setState(true);
            return;
        }
        
        xRoi[NbPoint-1]=ox;
        yRoi[NbPoint-1]=oy;
        
        
        if (NbPoint==1){
            Distance=-1;
            Velocity=-1;
        } else {
            Distance=calxy*Math.sqrt(Math.pow((ox-prevx),2)+Math.pow((oy-prevy),2));
            Velocity=Distance/calt;
        }
        
        if (checkRef.getState()) Directionnality();
        
        PixVal=img.getProcessor().getPixel(ox,oy);
        
        rt.incrementCounter();
        double[] doub={Nbtrack,(img.getCurrentSlice()),ox,oy,Distance,Velocity,PixVal};
        for (i=0; i<doub.length; i++) rt.addValue(i,doub[i]);
        rt.show("Results from "+imgtitle+" in "+choicecalxy.getItem(choicecalxy.getSelectedIndex())+" per "+choicecalt.getItem(choicecalt.getSelectedIndex()));
        
        if (getPosition(img) < getMaxPosition(img)) {
            NbPoint++;
            moveTo(img, getPosition(img) + 1);
            if (Distance!=0) {
                pprevx=prevx;
                pprevy=prevy;
            }
            prevx=ox;
            prevy=oy;
            roi=new PolygonRoi(xRoi,yRoi,NbPoint-1,Roi.POLYLINE);
            if(checkPath.getState()) img.setRoi(roi);
        } else {
            trackdel.add(""+Nbtrack);
            Nbtrack++;
            img.setRoi(roi);
            canvas.removeMouseListener(this);
            islistening=false;
            checkCent.setState(false);
            IJ.showStatus("Tracking is over");
            return;
        }
        
        
        
        
    }
    
    public void mousePressed(MouseEvent m) {}
    public void mouseExited(MouseEvent m) {}
    public void mouseClicked(MouseEvent m) {}
    public void mouseEntered(MouseEvent m) {}
    
    void HideParam(){
        checkParam.setState(false);
        panel.remove(labelEmpty1);
        panel.remove(labelEmpty2);
        panel.remove(labelEmpty3);
        panel.remove(labelEmpty4);
        panel.remove(labelParam);
        panel.remove(labelEmpty5);
        panel.remove(labelTime);
        panel.remove(caltfield);
        panel.remove(choicecalt);
        panel.remove(labelxy);
        panel.remove(calxyfield);
        panel.remove(choicecalxy);
        panel.remove(labelz);
        panel.remove(calzfield);
        panel.remove(labelEmpty6);
        panel.remove(labelCent);
        panel.remove(centsize);
        panel.remove(labelPix);
        panel.remove(labelDot);
        panel.remove(dotsizefield);
        panel.remove(labelEmpty7);
        panel.remove(labelLine);
        panel.remove(linewidthfield);
        panel.remove(labelEmpty8);
        panel.remove(labelFont);
        panel.remove(fontsizefield);
        panel.remove(labelEmpty9);
        pack();
        show();
    }
    
    void ShowParam(){
        checkParam.setState(true);
        panel.add(labelEmpty1);
        panel.add(labelEmpty2);
        panel.add(labelEmpty3);
        panel.add(labelEmpty4);
        panel.add(labelParam);
        panel.add(labelEmpty5);
        panel.add(labelTime);
        panel.add(caltfield);
        panel.add(choicecalt);
        panel.add(labelxy);
        panel.add(calxyfield);
        panel.add(choicecalxy);
        panel.add(labelz);
        panel.add(calzfield);
        panel.add(labelEmpty6);
        panel.add(labelCent);
        panel.add(centsize);
        panel.add(labelPix);
        panel.add(labelDot);
        panel.add(dotsizefield);
        panel.add(labelEmpty7);
        panel.add(labelLine);
        panel.add(linewidthfield);
        panel.add(labelEmpty8);
        panel.add(labelFont);
        panel.add(fontsizefield);
        panel.add(labelEmpty9);
        pack();
        show();
    }
    
    void Center2D(){
        int lim=(int)((Tools.parseDouble(centsize.getText()))/2);
        int pixval=img.getProcessor().getPixel(ox,oy);
        double xb=0;
        double yb=0;
        double sum=0;
        commentCorr="("+ox+","+oy+") > (";
        for (i=ox-lim; i<ox+lim+1; i++){
            for (j=oy-lim; j<oy+lim+1; j++){
                if (img.getProcessor().getPixel(i,j)>pixval && choicecent.getSelectedIndex()==0){
                    ox=i;
                    oy=j;
                    pixval=img.getProcessor().getPixel(ox,oy);
                }
                if (img.getProcessor().getPixel(i,j)<pixval && choicecent.getSelectedIndex()==1){
                    ox=i;
                    oy=j;
                    pixval=img.getProcessor().getPixel(ox,oy);
                }
                xb=xb+i*img.getProcessor().getPixel(i,j);
                yb=yb+j*img.getProcessor().getPixel(i,j);
                sum=sum+img.getProcessor().getPixel(i,j);
            }
        }
        xb=xb/sum;
        yb=yb/sum;
        if (choicecent.getSelectedIndex()==2){
            ox=(int)xb;
            oy=(int)yb;
        }
        commentCorr=commentCorr+ox+","+oy+")";
        labelCorr.setText(commentCorr);
    }
    
    void Directionnality(){
        if (!RefSet) {
            IJ.showMessage("!!! Warning !!!", " No reference set:\nClick on 'Add reference' first !!!");
            checkRef.setState(false);
            return;
        }
        
        if (NbPoint==2){
            DirIndex=1;
            pprevx=refx;
            pprevy=refy;
        }
        
        if (NbPoint>1){
            
            double angle1 = roi.getAngle(pprevx, pprevy, prevx, prevy);
            double angle2 = roi.getAngle(prevx, prevy, ox, oy);
            double angle = Math.abs(180-Math.abs(angle1-angle2));
            if (angle>180.0) angle = 360.0-angle;
            if (angle<90) DirIndex=-DirIndex;
            
            Distance=Distance*DirIndex;
            Velocity=Velocity*DirIndex;
        }
        
    }
    
    void Dots(){
        
        dotsize=(int) Tools.parseDouble(dotsizefield.getText());
        j=0;
        int nbtrackold=1;
        for (i=0; i<(rt.getCounter()); i++) {
            int nbtrack=(int) rt.getValue(0,i);
            int nbslices=(int) rt.getValue(1,i);
            int cx=(int) rt.getValue(2,i);
            int cy=(int) rt.getValue(3,i);
            if ((nbtrack != nbtrackold)) j++;
            if (j>6) j=0;
            ImageProcessor ip= stack.getProcessor(nbslices);
            ip.setColor(col[j]);
            ip.setLineWidth(dotsize);
            ip.drawDot(cx, cy);
            if (checkText.getState()){
                Font font = new Font("SansSerif", Font.PLAIN, (int) Tools.parseDouble(fontsizefield.getText()));
                ip.setFont(font);
                ip.drawString(""+nbtrack, cx+(dotsize-5)/2, cy-(dotsize-5)/2);
            }
            nbtrackold=nbtrack;
        }
        
    }
    
    void ProLines(){
        
        linewidth=Tools.parseDouble(linewidthfield.getText());
        j=0;
        k=1;
        int cxold=0;
        int cyold=0;
        int nbtrackold=1;
        
        for (i=0; i<(rt.getCounter()); i++) {
            int nbtrack=(int) rt.getValue(0,i);
            int nbslices=(int) rt.getValue(1,i);
            int cx=(int) rt.getValue(2,i);
            int cy=(int) rt.getValue(3,i);
            int lim=img.getStackSize()+1;
            if ((nbtrack != nbtrackold)) {
                j++;
                k=1;
            }
            for (int n=nbtrack; n<(rt.getCounter());n++) {
                if ((int) (rt.getValue(0,n)) == nbtrack) lim=(int) rt.getValue(1,n);
            }
            
            if (j>6) j=0;
            for (int m=nbslices; m<lim+1;m++) {
                if (k==1){
                    cxold=cx;
                    cyold=cy;
                }
                
                ImageProcessor ip= stack.getProcessor(m);
                ip.setColor(col[j]);
                ip.setLineWidth((int) linewidth);
                ip.drawLine(cxold, cyold, cx, cy);
                nbtrackold=nbtrack;
                k++;
            }
            cxold=cx;
            cyold=cy;
        }
    }
    
    void Center3D(){
        tmpx=0;
        tmpy=0;
        tmpz=0;
        PixVal=0;
        tmpttl=0;
        Qttl=0;
        
        
        limsx1=x-sglBoxx;
        if (limsx1<0) limsx1=0;
        limsx2=x+sglBoxx;
        if (limsx2>Width-1) limsx2=Width-1;
        
        limsy1=y-sglBoxy;
        if (limsy1<0) limsy1=0;
        limsy2=y+sglBoxy;
        if (limsy2>Height-1) limsy2=Height-1;
        
        limsz1=z-sglBoxz;
        if (limsz1<1) limsz1=1;
        limsz2=z+sglBoxz;
        if (limsz2>ip.getNSlices()) limsz2=ip.getNSlices();
        
        
        
        for (l=limsz1; l<limsz2+1; l++){
            moveTo(ip, l);
            for (m=limsy1; m<limsy2+1; m++){
                for (n=limsx1; n<limsx2+1; n++){
                    tmppixval=ip.getProcessor().getPixel(n,m);
                    
                    //Case centring option is barycenter
                    if (Centring==1) {
                        tmpx=tmpx+n*tmppixval;
                        tmpy=tmpy+m*tmppixval;
                        tmpz=tmpz+l*tmppixval;
                        tmpttl=tmpttl+tmppixval;
                    }
                    
                    //Case centring option is max intensity
                    if (Centring==2) {
                        if (tmppixval>PixVal){
                            PixVal=tmppixval;
                            tmpx=n;
                            tmpy=m;
                            tmpz=l;
                            tmpttl=1;
                        }
                    }
                }
            }
        }
        x=(int) tmpx/tmpttl;
        y=(int) tmpy/tmpttl;
        z=(int) tmpz/tmpttl;
    }
    
    void quantify() {
        Qsgl=0;
        sizeSgl=0;
        Qbkgd=0;
        sizeBkgd=0;
        Qttl=0;
        
        
        limsx1=x-sglBoxx;
        if (limsx1<0) limsx1=0;
        limsx2=x+sglBoxx;
        if (limsx2>Width-1) limsx2=Width-1;
        
        limsy1=y-sglBoxy;
        if (limsy1<0) limsy1=0;
        limsy2=y+sglBoxy;
        if (limsy2>Height-1) limsy2=Height-1;
        
        limsz1=z-sglBoxz;
        if (limsz1<1) limsz1=1;
        limsz2=z+sglBoxz;
        if (limsz2>getMaxPosition(ip)) limsz2=getMaxPosition(ip);
        
        if (Quantification==1) {
            limbx1=x-bkgdBoxx;
            limbx2=x+bkgdBoxx;
            limby1=y-bkgdBoxy;
            limby2=y+bkgdBoxy;
        }
        
        if (Quantification==2){
            limbx1=0;
            limbx2=bkgdBoxx*2;
            limby1=0;
            limby2=bkgdBoxy*2;
        }
        
        if (Quantification==3){
            limbx1=Width-2*bkgdBoxx;
            limbx2=Width;
            limby1=0;
            limby2=bkgdBoxy*2;
        }
        
        if (Quantification==4){
            limbx1=0;
            limbx2=bkgdBoxx*2;
            limby1=Height-2*bkgdBoxy;
            limby2=Height;
        }
        if (Quantification==5){
            limbx1=Width-2*bkgdBoxx;
            limbx2=Width;
            limby1=Height-2*bkgdBoxy;
            limby2=Height;
        }
        
        if (limbx1<0) limbx1=0;
        if (limbx2>Width-1) limbx2=Width-1;
        if (limby1<0) limby1=0;
        if (limby2>Height-1) limby2=Height-1;
        limbz1=z-bkgdBoxz;
        if (limbz1<1) limbz1=1;
        limbz2=z+bkgdBoxz;
        if (limbz2>getMaxPosition(ip)) limbz2=getMaxPosition(ip);
        
        for (l=limsz1; l<limsz2+1; l++){
            moveTo(ip, l);
            for (m=limsy1; m<limsy2+1; m++){
                for (n=limsx1; n<limsx2+1; n++){
                    Qsgl=Qsgl+ip.getProcessor().getPixel(n,m);
                    sizeSgl++;
                }
            }
        }
        
        if (Quantification>0){
            for (l=limbz1; l<limbz2+1; l++){
                moveTo(ip, l);
                for (m=limby1; m<limby2+1; m++){
                    for (n=limbx1; n<limbx2+1; n++){
                        Qbkgd=Qbkgd+ip.getProcessor().getPixel(n,m);
                        sizeBkgd++;
                    }
                }
            }
        }else{
            sizeBkgd=1; //Prevents division by zero
        }
        
        QSglBkgdCorr=Qsgl-Qbkgd*sizeSgl/sizeBkgd;
        
        if (DoBleachCorr){
            for (l=1; l<ip.getStackSize()+1; l++){
                ip.setSlice(l);
                for (m=0; m<Height; m++){
                    for (n=0; n<Width; n++){
                        Qttl=Qttl+ip.getProcessor().getPixel(n,m);
                    }
                }
            }
        }
    }
    void DoVRML(){
        try {
            if (StaticView) oswStat=new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(dirVRMLstat)));
            if (DynamicView) oswDynam=new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(dirVRMLdynam)));
            
            
            vrmlCount=-1;
            TagOld=0;
            
            DistOfView= Math.max(Math.sqrt(Math.pow(Width*calxy/2,2)+Math.pow(Depth*calz/2,2)), Math.sqrt(Math.pow(Height*calxy/2,2)+Math.pow(Depth*calz/2,2)));
            DistOfView= Math.max(Math.sqrt(Math.pow(Width*calxy/2,2)+Math.pow(Height*calxy/2,2)), DistOfView);
            
            if (DynamicView){
                oswDynam.write("#VRML V2.0 utf8\n");
                oswDynam.write("\n");
                oswDynam.write("Viewpoint{\n");
                oswDynam.write("position "+(Width*calxy/2)+" "+(Height*calxy/2)+" "+(-3*DistOfView)+"\n");
                oswDynam.write("orientation 1 0 0 3.1415926535897932384626433832795\n");
                oswDynam.write("description \"XY view\"}\n");
                
                oswDynam.write("Viewpoint{\n");
                oswDynam.write("position "+(Width*calxy/2)+" "+3*DistOfView+" "+(Depth*calz/2)+"\n");
                oswDynam.write("orientation -1 0 0 1.5707963267948966192313216916398\n");
                oswDynam.write("description \"XZ view\"}\n");
                
                oswDynam.write("Viewpoint{");
                oswDynam.write("position "+3*DistOfView+" "+(Height*calxy/2)+" "+(Depth*calz/2)+"\n");
                oswDynam.write("orientation 0.57735026918962576450914878050196 0.57735026918962576450914878050196 0.57735026918962576450914878050196 2.0943951023931954923084289221863\n");
                oswDynam.write("description \"YZ view\"}\n");
                
                oswDynam.write("Transform{\n");
                oswDynam.write("translation "+(Width*calxy/2)+" "+(Height*calxy/2)+" "+(ip.getStackSize()*calz/2)+"\n");
                oswDynam.write("children[\n");
                oswDynam.write("Shape{\n");
                oswDynam.write("appearance Appearance{\n");
                oswDynam.write("material Material{diffuseColor 1 1 1\n");
                oswDynam.write("transparency 0.75}}\n");
                oswDynam.write("geometry Box{size "+(Width*calxy)+" "+(Height*calxy)+" "+(ip.getStackSize()*calz)+"}}]}\n");
                oswDynam.write("\n");
            }
            
            
            if (StaticView){
                oswStat.write("#VRML V2.0 utf8\n");
                oswStat.write("\n");
                oswStat.write("Viewpoint{\n");
                oswStat.write("position "+(Width*calxy/2)+" "+(Height*calxy/2)+" "+(-3*DistOfView)+"\n");
                oswStat.write("orientation 1 0 0 3.1415926535897932384626433832795\n");
                oswStat.write("description \"XY view\"}\n");
                
                oswStat.write("Viewpoint{\n");
                oswStat.write("position "+(Width*calxy/2)+" "+3*DistOfView+" "+(Depth*calz/2)+"\n");
                oswStat.write("orientation -1 0 0 1.5707963267948966192313216916398\n");
                oswStat.write("description \"XZ view\"}\n");
                
                oswStat.write("Viewpoint{");
                oswStat.write("position "+3*DistOfView+" "+(Height*calxy/2)+" "+(Depth*calz/2)+"\n");
                oswStat.write("orientation 0.57735026918962576450914878050196 0.57735026918962576450914878050196 0.57735026918962576450914878050196 2.0943951023931954923084289221863\n");
                oswStat.write("description \"YZ view\"}\n");
                
                oswStat.write("Transform{\n");
                oswStat.write("translation "+(Width*calxy/2)+" "+(Height*calxy/2)+" "+(ip.getStackSize()*calz/2)+"\n");
                oswStat.write("children[\n");
                oswStat.write("Shape{\n");
                oswStat.write("appearance Appearance{\n");
                oswStat.write("material Material{diffuseColor 1 1 1\n");
                oswStat.write("transparency 0.75}}\n");
                oswStat.write("geometry Box{size "+(Width*calxy)+" "+(Height*calxy)+" "+(ip.getStackSize()*calz)+"}}]}\n");
                oswStat.write("\n");
            }
            
            key="key [\n";
            for (double tmp=minTime; tmp<maxTime+calt; tmp=tmp+calt){
                key=key+" "+((tmp-minTime)/(maxTime-minTime));
                countTtl++ ;
            }
            
            key=key+"]\n";
            
            for (i=0; i<rt.getCounter();i++){
                Tag=(int) VRMLarray[i][0];
                if (Tag!=TagOld){
                    vrmlCount++;
                    point="";
                    pointNb=0;
                    pointKey="";
                    lastPoint="";
                    countBefore=0;
                    countAfter=0;
                    
                    
                    if (vrmlCount>6) vrmlCount=0;
                    if (DynamicView){
                        oswDynam.write("DEF TRACK"+VRMLarray[i][0]+" Transform{\n");
                        oswDynam.write("children[\n");
                        oswDynam.write("Shape{\n");
                        oswDynam.write("appearance Appearance{\n");
                        oswDynam.write("material Material{diffuseColor "+vrmlCol[vrmlCount]+"}}\n");
                        oswDynam.write("geometry Sphere {radius "+(Tools.parseDouble(dotsizefield.getText())*calxy)+"}}]}\n");
                        oswDynam.write("\n");
                        oswDynam.write("DEF TRACK"+VRMLarray[i][0]+"_clock TimeSensor{\n");
                        oswDynam.write("cycleInterval 5\n");
                        oswDynam.write("loop TRUE\n");
                        oswDynam.write("stopTime -1}\n");
                        oswDynam.write("\n");
                        oswDynam.write("DEF TRACK"+VRMLarray[i][0]+"_positions PositionInterpolator{\n");
                        oswDynam.write(key+"\n");
                        oswDynam.write("keyValue [\n");
                    }
                    for (j=0;j<(VRMLarray[i][1])/calt;j++){
                        if (DynamicView) oswDynam.write(calxy*VRMLarray[i][2]+" "+calxy*VRMLarray[i][3]+" "+calz*VRMLarray[i][4]+",\n");
                        point=point+calxy*VRMLarray[i][2]+" "+calxy*VRMLarray[i][3]+" "+calz*VRMLarray[i][4]+", ";
                        //----
                        pointKey=pointKey+point;
                        for (k=pointNb; k<countTtl; k++) pointKey=pointKey+calxy*VRMLarray[i][2]+" "+calxy*VRMLarray[i][3]+" "+calz*VRMLarray[i][4]+", ";
                        pointKey=pointKey+"\n";
                        //----
                        pointNb++;
                        countBefore++ ;
                    }
                    
                }
                
                if (StaticView && StaticViewObj){
                    oswStat.write("Transform{\n");
                    oswStat.write("translation "+calxy*VRMLarray[i][2]+" "+calxy*VRMLarray[i][3]+" "+calz*VRMLarray[i][4]+",\n");
                    oswStat.write("children[\n");
                    oswStat.write("Shape{\n");
                    oswStat.write("appearance Appearance{\n");
                    oswStat.write("material Material{diffuseColor "+vrmlCol[vrmlCount]+"}}\n");
                    oswStat.write("geometry Sphere {radius "+(Tools.parseDouble(dotsizefield.getText())*calxy)+"}}]\n}");
                    oswStat.write("\n");
                }
                
                if (DynamicView) oswDynam.write(calxy*VRMLarray[i][2]+" "+calxy*VRMLarray[i][3]+" "+calz*VRMLarray[i][4]+",\n");
                point=point+calxy*VRMLarray[i][2]+" "+calxy*VRMLarray[i][3]+" "+calz*VRMLarray[i][4]+", ";
                //----
                pointKey=pointKey+point;
                for (k=pointNb+1; k<countTtl; k++) pointKey=pointKey+calxy*VRMLarray[i][2]+" "+calxy*VRMLarray[i][3]+" "+calz*VRMLarray[i][4]+", ";
                pointKey=pointKey+"\n";
                //----
                countAfter++ ;
                pointNb++;
                
                if ((i!=rt.getCounter()-1 && VRMLarray[i][0]!=VRMLarray[i+1][0]) || i==rt.getCounter()-1){
                    for (k=0; k<countTtl; k++) lastPoint=lastPoint+calxy*VRMLarray[i][2]+" "+calxy*VRMLarray[i][3]+" "+calz*VRMLarray[i][4]+", ";
                    
                    if (DynamicView){
                        for (j=0;j<(countTtl-(countBefore+countAfter));j++){
                            oswDynam.write(calxy*VRMLarray[i][2]+" "+calxy*VRMLarray[i][3]+" "+calz*VRMLarray[i][4]+",\n");
                            pointKey=pointKey+lastPoint+"\n";
                        }
                        oswDynam.write("]}\n");
                        oswDynam.write("\n");
                        oswDynam.write("ROUTE TRACK"+VRMLarray[i][0]+"_clock.fraction_changed TO TRACK"+VRMLarray[i][0]+"_positions.set_fraction\n");
                        oswDynam.write("ROUTE TRACK"+VRMLarray[i][0]+"_positions.value_changed TO TRACK"+VRMLarray[i][0]+".translation\n");
                        oswDynam.write("\n");
                        oswDynam.write("DEF TRACK"+VRMLarray[i][0]+"_scale PositionInterpolator{\n");
                        oswDynam.write(key+"\n");
                        oswDynam.write("keyValue [\n");
                        for (j=0;j<countBefore;j++) oswDynam.write("0 0 0,\n");
                        for (j=0;j<countAfter;j++) oswDynam.write("1 1 1"+",\n");
                        for (j=0;j<(countTtl-(countBefore+countAfter));j++) oswDynam.write("0 0 0,\n");
                        oswDynam.write("]}\n");
                        oswDynam.write("\n");
                        oswDynam.write("ROUTE TRACK"+VRMLarray[i][0]+"_clock.fraction_changed TO TRACK"+VRMLarray[i][0]+"_scale.set_fraction\n");
                        oswDynam.write("ROUTE TRACK"+VRMLarray[i][0]+"_scale.value_changed TO TRACK"+VRMLarray[i][0]+".scale\n");
                        oswDynam.write("\n");
                        if (DynamicViewStaticTraj || DynamicViewDynamicTraj){
                            oswDynam.write("Shape{\n");
                            oswDynam.write("geometry IndexedLineSet{\n");
                            oswDynam.write("coord DEF TRAJ"+VRMLarray[i][0]+" Coordinate{\n");
                            if (DynamicViewStaticTraj) oswDynam.write("point[\n"+point+"]");
                            oswDynam.write("}\ncoordIndex[");
                        }
                    }
                    
                    if (StaticView && StaticViewTraj){
                        oswStat.write("Shape{\n");
                        oswStat.write("geometry IndexedLineSet{\n");
                        oswStat.write("coord Coordinate{\n");
                        oswStat.write("point[\n");
                        
                        oswStat.write(point+"]}\n");
                        oswStat.write("coordIndex[");
                    }
                    
                    
                    
                    for (j=0;j<countTtl;j++){
                        if (DynamicView && DynamicViewDynamicTraj) oswDynam.write(j+" ");
                        if (DynamicView && DynamicViewStaticTraj && j<pointNb) oswDynam.write(j+" ");
                        if (StaticView && StaticViewTraj && j<pointNb) oswStat.write(j+" ");
                    }
                    
                    if (DynamicView && (DynamicViewStaticTraj || DynamicViewDynamicTraj)){
                        oswDynam.write("-1]\n");
                        oswDynam.write("color Color{color["+vrmlCol[vrmlCount]+"]}\n");
                        oswDynam.write("colorPerVertex FALSE}}\n");
                        oswDynam.write("\n");
                    }
                    
                    if (DynamicView && DynamicViewDynamicTraj){
                        oswDynam.write("DEF TRAJ"+VRMLarray[i][0]+"_coord CoordinateInterpolator{\n");
                        oswDynam.write(key);
                        oswDynam.write("keyValue[\n"+pointKey+"]}\n");
                        oswDynam.write("ROUTE TRACK"+VRMLarray[i][0]+"_clock.fraction_changed TO TRAJ"+VRMLarray[i][0]+"_coord.set_fraction\n");
                        oswDynam.write("ROUTE TRAJ"+VRMLarray[i][0]+"_coord.value_changed TO TRAJ"+VRMLarray[i][0]+".point\n\n");
                        
                    }
                    
                    if (StaticView && StaticViewTraj){
                        oswStat.write("-1]\n");
                        oswStat.write("color Color{color["+vrmlCol[vrmlCount]+"]}\n");
                        oswStat.write("colorPerVertex FALSE}}\n");
                        oswStat.write("\n");
                    }
                    
                }
                TagOld=Tag;
            }
            oswStat.close();
            oswDynam.close();
            
            
        } catch (IOException e) {
            IJ.error("Error writing VRML file");
        }
        
    }
}

