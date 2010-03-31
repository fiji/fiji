//package mmorpho;
import ij.*;
import ij.process.*;
import ij.gui.*;

import java.awt.*;
import java.awt.image.*;
import ij.plugin.filter.*;
import java.util.*;
import java.lang.reflect.*;
import ij.util.*;
import mmorpho.*;

/**
 * @version  2.3.1 13 June 2006
 * 			 2.3   25 May 2006
 * 			 2.1;  09 Dec 2004
             2.0;  13 Nov 2004
 * @author Dimiter Prodanov
 * 		   Catholic University of Louvaion; University of Leiden
 *
 *
 * @contents       This plugin performs the basic morphologic operations on grayscale images
 *      erosion, dilation, opening and closing with several types of structuring elements.
 *      It is build upon the StructureElement class
 *
 *      The develpoment of this alogorithm was inspired by the book of Jean Serra
 *      "Image Analysis and Mathematical Morphology"
 *
 * @license      This library is free software; you can redistribute it and/or
 *      modify it under the terms of the GNU Lesser General Public
 *      License as published by the Free Software Foundation; either
 *      version 2.1 of the License, or (at your option) any later version.
 *
 *      This library is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *       Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public
 *      License along with this library; if not, write to the Free Software
 *      Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


/**
 * @author Prodanov
 *
 */
/**
 * @author Prodanov
 *
 */
public class GrayMorphology_ implements PlugInFilter, Constants {
    ImagePlus imp;
    public String kernelText = " 0 0 0 0 0\n 0 0 255 0 0\n 0 255 255 255 0\n 0 0 255 0 0\n 0 0 0 0 0\n";
    private static final String R="SE_r", SHOW="show_SE", SETYPE="SE_type", OPER="MOper";
    boolean canceled = true;
    private static final int[] offset=OFFSET0;
    public StructureElement se, minus_se, plus_se, down_se, up_se;
    MorphoProcessor mp;
    ImageWindow win;
    private static float radius=(float)Prefs. getDouble(R,1);
    private static int options=Prefs.getInt(SETYPE,0);
    private static boolean showoptions=Prefs.getBoolean(SHOW,false);
    private static int morphoptions=Prefs.getInt(OPER,0);
    public final static String[] strelitems={"circle","diamond","square","hor line","ver line","2p h","2p v","free form"};
    public final static int[] constitems={CIRCLE,DIAMOND,SQARE,HLINE,VLINE,HPOINTS,VPOINTS,FREE};
    public final static String[] morphitems={"erode","dilate","open","close",
    "fast erode","fast dilate","fast open","fast close"};
    
    public final static int ERODE=0, DILATE=1,OPEN=2,CLOSE=3,
    FERODE=4,FDILATE=5,FOPEN=6,FCLOSE=7;
    private Roi roi;
    boolean isLineRoi;
    int slice=0;
    
    public int setup(String arg, ImagePlus imp) {
        this.imp=imp;
        
        IJ.register(GrayMorphology_.class);
        if (arg.equals("about")){
            showAbout();
            return DONE;
        }
        else {
            if (imp!=null) {
                win = imp.getWindow();
                win.running = true;
               
                roi = imp.getRoi();
                isLineRoi=(roi!=null && roi.getType()==Roi.LINE);
            }

            
            if(IJ.versionLessThan("1.35") || !showDialog(imp)) {
                return DONE;
            }
            else {
                return DOES_8G+DOES_STACKS;
            }
        }
    }
    
    boolean showDialog(ImagePlus imp)   {
        
        if (imp==null) return true;
        GenericDialog gd=new GenericDialog("Parameters");
        
        // Dialog box for user input
        gd.addMessage("This plugin performs morphology operators on graylevel images\n");
        
        gd.addNumericField("Radius of the structure element (pixels):", radius, 1);
        
        gd.addChoice("Type of structure element", strelitems, strelitems[options]);
        gd.addCheckbox("Show mask", showoptions);
        gd.addChoice("Operator", morphitems, morphitems[morphoptions]);

        gd.showDialog();
        radius=(float)gd.getNextNumber();
        options=gd.getNextChoiceIndex();
        
        showoptions=gd.getNextBoolean();
        morphoptions=gd.getNextChoiceIndex();

        
        if (gd.wasCanceled())
            return false;
        if (!validate(radius,2)){
            IJ.showMessage("Invalid Numbers!\n" +
            "Enter floats 0.5 or 1");
            return false;
        }
               
        return true;
    }
    
    /* Now we can run the plugin as an application
     * */
    public static void main(String[] args) {
    	new ImageJ();
    }
    
    private boolean seshown=false;
    
    /** displays the StructureElement */
    
    /**
     * @param strel
     * @param Title
     */
    public void showStrEl(StructureElement strel, String Title) {
        int wh=strel.getWidth();
        int hh=strel.getHeight();
        //IJ.log("width: "+wh+" height: "+hh);
        //Log(strel.getMask());
        //Log(se.getVect());
        ImageProcessor fp= new FloatProcessor(wh,hh,strel.getMask()).convertToByte(false);
        new ImagePlus(Title, fp).show();
        seshown=true;
    }
    
    /**
     * Calculates the absolute value of an array
     * @param arr array of int[]
     * @return array of int[]
     */ 
    public int[] Abs(int[] arr){
        int[] warr=new int[arr.length];
        for (int i=0; i<arr.length;i++)
            warr[i]=Math.abs(arr[i]);
        return warr;
    }
   
    /* Extracts the ByteProcessor within a rectangular roi
     * 
     * @param ip
     * @param r
     * @return ByteProcessor
     */
    public ByteProcessor getMask(ByteProcessor ip, Rectangle r) {
     //Rectangle r=ip.getRoi();
     int width = ip.getWidth();
   	 //int height = ip.getHeight();
   	 byte[] pixels = (byte[])ip.getPixels();
   	 int xloc=(int)r.getX(); int yloc=(int)r.getY();
        int w=(int)r.getWidth();
        int h=(int)r.getHeight();
      	 byte[] mask=new byte[w*h];
        for (int cnt=0; cnt<mask.length;cnt++) {
       	 int index=xloc+cnt%w + (cnt/w)*width +yloc*width;
       	 mask[cnt]=(byte)(pixels[index] & 0xFF);  
        }
        return new ByteProcessor(w, h, mask,ip.getColorModel());
       }
    
    /** principal method of the plugin
     *  does the actual job
     * @param ip the ImageProcessor
     */
    public void run(ImageProcessor ip) {
        int shift=1;
        // width=(int)(2*radius+2*shift);
        if (IJ.escapePressed())
        {IJ.beep(); return;}
        //IJ.log( "options SE "+strelitems[options]+ " "+ constitems[options]);
        int eltype=constitems[options];
        if (eltype==FREE) {
        	 se=inputSE();
        } else {
            shift=1;
            
            se=new StructureElement(eltype,  shift,  radius, offset);
        }
        if (se!=null) {
             mp=new MorphoProcessor(se);
             if ((showoptions) && (!seshown)){
                minus_se =mp.getSE(-1); 
                plus_se=mp.getSE(1);
                showStrEl(se, "SE r=" +radius);
                showStrEl(minus_se, "minus SE r=" +radius);
                showStrEl(plus_se,"plus SE r=" +radius);
             }
	         slice++;   
	         //IJ.showStatus("Doing slice " + slice);
	         if (slice>1)
	             IJ.showStatus(imp.getTitle()+" : "+slice+"/"+imp.getStackSize());
	           // ip.snapshot();
	
	         Rectangle r=ip.getRoi();
	         if (r==null){
	        	doOptions(ip, mp, morphoptions);
	         } // end if
	         else if (!isLineRoi) {
            	ImageProcessor ipmask = getMask((ByteProcessor)ip,r);
            	doOptions(ipmask, mp, morphoptions);
            	ip.insert(ipmask,r.x,r.y);
	         } // end if
	        
            if (slice== imp.getImageStackSize())
        	   imp.updateAndDraw();
        }
    }

    private void doOptions(ImageProcessor ip, MorphoProcessor mp, int morphoptions){
        switch (morphoptions) {
        
        case ERODE: {
            mp.erode(ip);
            break;}
        case DILATE:{
            mp.dilate(ip);
            break;}
        case OPEN:{
            mp.open(ip);
            break;
        }
        case CLOSE:{
            mp.close(ip);
            break;
        }
        
        case FERODE: {
        	if ((se.getType()==HLINE) || (se.getType()==VLINE)) {
        		mp.LineErode(ip);}
        	else {
        		mp.fastErode(ip);
        	}
            break;
        }
        case FDILATE:{
        	if ((se.getType()==HLINE) || (se.getType()==VLINE)) {
            	mp.LineDilate(ip);}
        	else {
        		mp.fastDilate(ip);
            	}
            break;
        }
        case FOPEN:{
        	if ((se.getType()==HLINE) || (se.getType()==VLINE)) {
        		mp.LineErode(ip);
        		mp.LineDilate(ip);
            }
        	else {
        		mp.fopen(ip);
        	}
            break;
        }
        case FCLOSE:{
        	if ((se.getType()==HLINE) || (se.getType()==VLINE)) {
            	mp.LineDilate(ip);
            	mp.LineErode(ip);
            	}
        	else {
        		mp.fclose(ip);
        	}
            break;
        }
 
    } // switch
    	
    }
    
        /* Creates a StructureElement
         * from text input; must be delimited
         *
         * @return StructureElement
         */
        private StructureElement inputSE() {
            GenericDialog gd = new GenericDialog("Input Mask", IJ.getInstance());
            gd.addTextAreas(kernelText, null, 10, 30);
            gd.showDialog();
            if (gd.wasCanceled()) {
                canceled = true;
                return null;
            }
            kernelText = gd.getNextText();
            
            return new StructureElement(kernelText);
        }
        
        /* validates the input value
         * only n/2 floats are accepted
         */
        private boolean validate( float var, int k){
            float a=k*var;
            int b=(int) (k* var);
            // IJ.log(IJ.d2s(a-b));
            if ((a-b==0)||(var<0))
                return true;
            else return false;
        }
     /* Saves the current setings of the plugin for further use
      * 
      *
     * @param prefs
     */
    public static void savePreferences(Properties prefs) {
            prefs.put(R, Double.toString(radius));
            prefs.put(SHOW,Boolean.toString(showoptions));
            prefs.put(SETYPE, Integer.toString(options));
            prefs.put(OPER, Integer.toString(morphoptions));
  
    }
     
        
        /** Logs array
         * @param arr the array
         */
        public void Log(int[][] a){
            String aStr="";
            // int w=(int) Math.sqrt(a.length);
            int h=a.length;
            for (int i=0;i<h;i++) {
                for (int j=0; j<a[i].length;j++){
                    aStr+=a[i][j]+"  ";
                }
                IJ.log(aStr);
                aStr="";
            }
            
        }
        /*------------------------------------------------------------------*/
        void showAbout() {
            IJ.showMessage("Gray Morphology version  2.3",
            "This plugin performs the basic morphologic operations on grayscale images \n  "+
            "erosion, dilation, opening and closing with several types of structuring elements.\n" +
            "It is build upon the StructureElement class. \n"+
            "The develpoment of this alogorithm was inspired by the book of Jean Serra \n" +
            "\"Image Analysis and Mathematical Morphology\""
            );
            
        } /* showAbout */
    }
