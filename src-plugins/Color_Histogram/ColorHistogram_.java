/*
 * ColorHistogram.java
 *
 * Created on 09 May 2004, 21:23
 */

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.awt.datatransfer.*;
import java.awt.datatransfer.Clipboard;

import ij.util.*;
import ij.measure.*;
import ij.text.*;

/**
 *     @version 2.0.5 date 18 Jun 2007
 *     				- fixed a bug in ROI handling
 *     				- fixed a bug in mode calculation
 *     
 *     			2.0  28 Apr 2007
 *     				- fixed a bug in mask handling
 *     				- added new class for statistics calculation
 *	
 *      		1.1 31 Oct 2005
 *					- added numerical output to the Results window
 *					- fixed a bug in ROI handling
 *					- fixed a bug in standard deviation calculation
 *
 *     			1.0.1
 *     				- added support of ROIs
 *     
 *     @author  Dimiter Prodanov
 *     Copyright (C) 2004 - 2007 Dimiter Prodanov
 *      This library is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *       Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public
 *      License along with this library; if not, write to the Free Software
 *      Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/** This plugin is based on the Histogram plugin, 
 the HistogramWindow class and the LookUpTable class. */

public  class ColorHistogram_   implements PlugInFilter  {
    ImagePlus imp;
    
    
    
    /*------------------------------------------------------------------*/
    /**
     *  Overloaded method of PlugInFilter.
     *
     * @param  arg  Optional argument, not used by this plugin.
     * @param  imp  Optional argument, not used by this plugin.
     * @return   Flag word that specifies the filters capabilities.
     */
    public int setup(String arg, ImagePlus imp){
        this.imp=imp;
        //IJ.register(ColorHistogram_.class);
        if (arg.equals("about")){
            showAbout();
            return DONE;
        }
        
        
        if(IJ.versionLessThan("1.32c")) {
            return DONE;
        }
        else {
            return DOES_RGB+NO_CHANGES+NO_UNDO;
        }
    } /* setup */
    
    public void run(ImageProcessor ip) {
        
        new ColorHistogramWindow(imp);
        
    }
    
    public static void main (String[] args) {
    	
    }
    
    
    /*---------------------------------------------------------------
     
        if (imp==null) return true;
        GenericDialog gd=new GenericDialog("Explain here");
     
     
        gd.showDialog();
     
        if (gd.wasCanceled())
            return false;
     
     
     
     
        return true;
    } /* showDialog */
    
    /*------------------------------------------------------------------*/
    void showAbout() {
        IJ.showMessage("About Color Histogram...",
        "Displays histograms of a RGB image\n version 2.0"
        );
    }    /* showAbout */
    
    public void textValueChanged(java.awt.event.TextEvent textEvent) {
    }
    
    
    
}


// @SuppressWarnings("serial")
class ColorHistogramWindow extends ImageWindow implements Measurements, ActionListener, ClipboardOwner  {
    static final int WIN_WIDTH = 300;
    static final int WIN_HEIGHT = 410;
    static final int HIST_WIDTH = 256;
    static final int HIST_HEIGHT = 80;
    static final int BAR_HEIGHT = 12;
    static final int XMARGIN = 20;
    static final int YMARGIN = 10;
    
    //protected ImageStatistics stats;
    protected int[][] histogram;
    //protected LookUpTable lut;
    protected Rectangle[] frame = new Rectangle[3] ;
    protected Button list, save, copy,log;
    protected Label value, count;
    //protected static String defaultDirectory = null;
    protected int decimalPlaces;
    protected int digits;
    //protected int newMaxCount;
    protected int plotScale = 1;
    protected boolean logScale;
    //protected Calibration cal;
    public static int nBins = 256;
    private byte[] mask;
   // private int area=1;
    private int width, height=1;
    protected int[] histMin=new int[3];
    protected int[] histMax=new int[3];
    protected double[] histMean=new double[3];
    protected int[] histMode=new int[3];
    protected int[] histModeCnt=new int[3];
    protected double[] histStdev=new double[3];
    protected ColorStats stats;
    /*
    public ColorProcessor getMask(ColorProcessor ip, Rectangle r) {
    	
        int width = ip.getWidth();
      	 //int height = ip.getHeight();
      	int[] pixels = (int[])ip.getPixels();
      	int xloc=(int)r.getX(); int yloc=(int)r.getY();
        int w=(int)r.getWidth();
        int h=(int)r.getHeight();
        //IJ.log("roi length " +w*h);
        int[] mask=new int[w*h];
    
        for (int cnt=0; cnt<mask.length;cnt++) {
           int index=xloc+cnt%w + (cnt/w)*width +yloc*width;
           mask[cnt]=pixels[index];  
                 	
        }

        return new ColorProcessor(w, h, mask);

       }
    */
    /** Displays a histogram using the title "Histogram of ImageName". */
    
    public ColorHistogramWindow(ImagePlus imp) {
        super(NewImage.createRGBImage("Histogram of "+imp.getShortTitle(), WIN_WIDTH, WIN_HEIGHT, 1, NewImage.FILL_WHITE));
		ImageProcessor ipmask = imp.getMask();
		
		boolean isnull=(ipmask==null);
		mask=(!isnull)?(byte[])ipmask.getPixels():null;
        width=imp.getWidth();
        height=imp.getHeight();
        
        showHistogram(imp, nBins, 0.0, 0.0);
        PrintResults();
    }
    
    public void setup() {
        Panel buttons = new Panel();
        buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
        list = new Button("List");
        list.addActionListener(this);
        buttons.add(list);
        copy = new Button("Copy");
        copy.addActionListener(this);
        buttons.add(copy);
     //   log = new Button("Log");
     //   log.addActionListener(this);
      //  buttons.add(log);
        Panel valueAndCount = new Panel();
        valueAndCount.setLayout(new GridLayout(2, 1));
        value = new Label("                  "); //21
        value.setFont(new Font("Monospaced", Font.PLAIN, 12));
        valueAndCount.add(value);
        count = new Label("                  ");
        count.setFont(new Font("Monospaced", Font.PLAIN, 12));
        valueAndCount.add(count);
        buttons.add(valueAndCount);
        add(buttons);
        pack();
    }
    
    
    public void showHistogram(ImagePlus imp, int bins, double histMin, double histMax) {
        setup();
        //cal = imp.getCalibration();
        //boolean limitToThreshold =false; //(Analyzer.getMeasurements()&LIMIT)!=0;
        
        Rectangle rect;
        try {
            rect=imp.getRoi(). getBoundingRect();
            //area=rect.width*rect.height;
            //IJ.log("b width "+rect.width+" b height "+rect.height);
        }
        catch (NullPointerException e) {
            rect=new Rectangle(0,0,width,height);
            //area=width*height;
        }
        ColorProcessor cp=(ColorProcessor) imp.getProcessor();
        //int [] pixels=(int[])cp.getPixels();
        
        if (mask!=null) {
            //histogram= getHistogram(width, pixels,mask, rect);
            //ImageProcessor ipmask = imp.getMask();
            //new ImagePlus("ROI",ipmask).show();
            stats= new ColorStats(cp,mask,rect);
        }
        else  {
            //histogram = getHistogram(width,pixels, rect);
            stats= new ColorStats(cp,rect);
        }
        //calculateStatistics(histogram);
        
        histMean=stats.getMean();
        histMode=stats.getMode();
        histModeCnt=stats.getModeCnt();
        histStdev=stats.getStdev();
        histogram=stats.getHistogram();
       // histMin=stats.getMin();
        //histMin=stats.getMax();
        
        //boolean fixedRange =true; //type==ImagePlus.GRAY8 || type==ImagePlus.COLOR_256 || type==ImagePlus.COLOR_RGB;
        ImageProcessor ip = this.imp.getProcessor();
        //boolean color = true;//!(imp.getProcessor() instanceof ColorProcessor) && !lut.isGrayscale();
        //IJ.log("color: "+color);
        //if (color)
        //ip = ip.convertToRGB();
        
        drawHistogram(ip);
        //	if (color)
        //	this.imp.setProcessor(null, ip);
        //else
        this.imp.updateAndDraw();
    }
    
    protected void drawHistogram(ImageProcessor ip) {
        int x, y;
//        int maxCount2 = 0;
//        int mode2 = 0;
//        int saveModalCount;
        decimalPlaces = Analyzer.getPrecision();
        
        
        ip.setColor(Color.black);
        ip.setLineWidth(1);
        int offset=0;
        
        //red
        int delta=YMARGIN;
        drawPlot(0, offset, ip);
        x = XMARGIN + 1;
        y = YMARGIN + HIST_HEIGHT + 2;
        //IJ.log("col :"+lut.getMapSize());
        drawUnscaledColorBar(0,ip, x-1, y, 256, BAR_HEIGHT);
        y += BAR_HEIGHT;
        int cheight=BAR_HEIGHT + HIST_HEIGHT + 2+delta;
        // IJ.log("ch :"+ cheight);
        offset+=cheight;
        //green
        drawPlot(1, offset, ip);
        x = XMARGIN + 1;
        y = YMARGIN + HIST_HEIGHT + 2;
        drawUnscaledColorBar(1,ip, x-1, y+offset, 256, BAR_HEIGHT);
        
        y += BAR_HEIGHT;
        
        offset+=cheight;
        //blue
        drawPlot(2, offset, ip);
        x = XMARGIN + 1;
        y = YMARGIN + HIST_HEIGHT + 2;
        drawUnscaledColorBar(2,ip, x-1, y+offset, 256, BAR_HEIGHT);
        y += BAR_HEIGHT+15;
        
        drawText(ip, x, y+offset);
    }
    
    protected void drawUnscaledColorBar(int color, ImageProcessor ip, int x, int y, int width, int height) {
        ImageProcessor bar = null;
        if (ip instanceof ColorProcessor) {
            bar = new ColorProcessor(width, height);
            for (int i = 0; i<256; i++) {
                switch (color) {
                    case 0: //red
                        bar.setColor(new Color(i&0xff, 0, 0));
                        break;
                    case 1: //green
                        bar.setColor(new Color(0, i&0xff, 0));
                        break;
                    case 2: //blue
                        bar.setColor(new Color(0, 0,i&0xff));
                        break;
                }
                bar.moveTo(i, 0); bar.lineTo(i, height); 
            }
            
            ip.insert(bar, x,y);
            ip.setColor(Color.black);
            ip.drawRect(x-1, y, width+2, height);
        }
    }
    
    public void PrintResults(){
        IJ.setColumnHeadings("channel\tmean\tmode\tstd.dev.");
        IJ.write("red\t"+ IJ.d2s(histMean[0],2) + "\t" + IJ.d2s(histModeCnt[0],0) + "\t " 
        		+IJ.d2s(histStdev[0],2) );
   
         IJ.write("green\t" + IJ.d2s(histMean[1],2)+"\t" + IJ.d2s(histModeCnt[1],0) + "\t " 
        		 +IJ.d2s(histStdev[1],2) );
         
         IJ.write("blue\t" + IJ.d2s(histMean[2],2)+"\t" + IJ.d2s(histModeCnt[2],0) + "\t " 
        		 +IJ.d2s(histStdev[2],2));
 }
    
  /*  
    protected void calculateStatistics(int[][]histogram) {
        double[] h2=new double[256];
        for (int col=0;col<3;col++) {
            histMode[col]= getMinMax(histogram[col])[1];
            
            int min=0;
            while ((histogram[col][min] == 0) && (min < 256))
                min++;
            histMin[col] = min;
            
            int max = 255;
            while ((histogram[col][max] == 0) && (max > 0))
                max--;
            histMax[col] = max;
            
            double cumsum=0;
            double cumsum2=0;
            for (int i=0; i<256;i++){
                cumsum+=i*histogram[col][i];
               // cumsum2+=i*i*histogram[col][i];
                h2[i]=i*i*histogram[col][i];
            }
            //IJ.log("cumsum: "+cumsum);
           
                  
            cumsum2=IntTrapz(h2, 1.0);
            //IJ.log("cumsum2: "+cumsum2);
            histMean[col]=cumsum/area;
            double histVar = (cumsum2/area-histMean[col]*histMean[col])*area/(area-1.0);
           // IJ.log("histVar: "+histVar);
            histStdev[col] = Math.sqrt(histVar);
            
        }
        
    }
    
    *//*------------------------------------------------------------------*/
    /*  Simpson's rule of integration
  *
    
    private double IntSimp(double[]y, double dx ) {
        double s=0;
        int m=y.length/2;
        for (int i=0;i<m-1;i++) {
            s+=y[2*i]+4*y[2*i+1]+y[2*i+2];
        }
        s=dx/3*s;
        return s;
    }
  */
    
    /*------------------------------------------------------------------*/
    /*  Trapezoid rule of integration

        private double IntTrapz(double[]y, double dx ) {
        double s=0;
        int m=y.length;
        for (int i=0;i<m-1;i++) {
            s+=y[i]+y[i+1];
        }
        s=dx/2*s;
        return s;
    }
    */
    
    public void drawPlot(int what, int offset, ImageProcessor ip) {
        //if (maxCount==0) maxCount = 1;
        int mode=histMode[what];
        int YM=YMARGIN+offset;
        frame[what] = new Rectangle(XMARGIN, YM, HIST_WIDTH, HIST_HEIGHT);
        ip.drawRect(frame[what].x-1, frame[what].y, frame[what].width+2, frame[what].height+1);
        int index, y;
        for (int i = 0; i<HIST_WIDTH; i++) {
            y = (int)(HIST_HEIGHT*histogram[what][i]/mode);
            if (y>HIST_HEIGHT)
                y = HIST_HEIGHT;
            ip.drawLine(i+XMARGIN, YM+HIST_HEIGHT, i+XMARGIN, YM+HIST_HEIGHT-y);
        }
    }
    /*
    public static int[] getMinMax(int[] a) {
        int min = a[0];
        int max =a[0];
        int value;
        for (int i=0; i<a.length; i++) {
            value = a[i];
            if (value<min)
                min = value;
            if (value>max)
                max = value;
        }
        int[] minAndMax = new int[2];
        minAndMax[0] = min;
        minAndMax[1] = max;
        return minAndMax;
    }
    */
    int getWidth(double d, ImageProcessor ip) {
        return ip.getStringWidth(IJ.d2s(d,0));
    }
    
    void drawText(ImageProcessor ip, int x, int y) {
        ip.setFont(new Font("SansSerif",Font.PLAIN,12));
        ip.setAntialiasedText(true);
        ip.drawString(IJ.d2s(0,0), x - 4, y);
        ip.drawString(IJ.d2s(255,0), x + HIST_WIDTH - getWidth(256, ip) + 10, y);
        
        
        int col1 = XMARGIN;
        int col2 = XMARGIN + HIST_WIDTH/3+10;
        int col3=col2+ HIST_WIDTH/3;
        int row1 = y+25;
        
        int row2 = row1 + 15;
        int row3 = row2 + 15;
        int row4 = row3 + 15;
       // int row5 = row4 + 15;
       // int row6 = row5 + 15;
       // int row7 = row6 + 15;
        ip.drawString("Count: " + stats.getHistcount(), col1, row1);
        ip.drawString("rMean: " + IJ.d2s(histMean[0],2), col1, row2);
        ip.drawString("rSD: " + IJ.d2s(histStdev[0],2), col2, row2);
        ip.drawString("gMean: " + IJ.d2s(histMean[1],2), col1, row3);
        ip.drawString("gSD: " + IJ.d2s(histStdev[1],2), col2, row3);
        ip.drawString("bMean: " + IJ.d2s(histMean[2],2), col1, row4);
        ip.drawString("bSD: " + IJ.d2s(histStdev[2],2), col2, row4);
        ip.drawString("rMode: " + IJ.d2s(histModeCnt[0],0), col3, row2);
        ip.drawString("gMode: " +IJ. d2s(histModeCnt[1],0), col3, row3);
        ip.drawString("bMode: " +IJ. d2s(histModeCnt[2],0), col3, row4);
        
    }
    
    void showList() {
        //IJ.log("list");
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<256; i++)
            sb.append(i+"\t"+histogram[0][i]+"\t"+histogram[1][i]+"\t"+histogram[2][i]+"\n");
        TextWindow tw = new TextWindow(getTitle(), "bin\tred\tgreen\tblue", sb.toString(), 200, 400);
        
    }
    

 /*   public int[][] getHistogram(int width, int[] pixels, Rectangle roi) {
        
        int c, r, g, b, v;
        int roiY=roi.y;
        int roiX=roi.y;
        int roiWidth=roi.width;
        int roiHeight=roi.height;
        int[][] histogram = new int[3][256];
        for (int y=roiY; y<(roiY+roiHeight); y++) {
            int i = y * width + roiX;
            for (int x=roiX; x<(roiX+roiWidth); x++) {
                c = pixels[i++];
                r = (c&0xff0000)>>16;
                g = (c&0xff00)>>8;
                b = c&0xff;
                //v = (int)(r*0.299 + g*0.587 + b*0.114 + 0.5);
                histogram[0][r]++;
                histogram[1][g]++;
                histogram[2][b]++;
            }
            //if (y%20==0)
            //showProgress((double)(y-roiY)/roiHeight);
        }
        //hideProgress();
        return histogram;
    }
    
    *//** Value of pixels included in masks. *//*
    public static final int WHITE = 0xFF;
    
    public int[][] getHistogram(int width, int[]pixels, byte[] mask, Rectangle roi) {
        int c, r, g, b;
        int[][] histogram = new int[3][256];
        int roiY=roi.y;
        int roiX=roi.y;
        int roiWidth=roi.width;
        int roiHeight=roi.height;
        int v=0;
        for (int y=roiY, my=0; y<(roiY+roiHeight); y++, my++) {
            int i = y * width + roiX;
            int mi = my * roiWidth;
            //v=0;
            for (int x=roiX, mx=0; x<(roiX+roiWidth); x++, mx++) {
            	int cnt=mi+mx;
            	//int w=mask[cnt]&0xff;
            	//IJ.log("cnt "+ cnt+" m "+w+ " mask "+ mask[cnt]);
            	//if(w!=0) v++;
                if ((mask[cnt]&0xff)==WHITE) {
                    c = pixels[i];
                    r = (c&0xff0000)>>16;
                    g = (c&0xff00)>>8;
                    b = c&0xff;               
                    histogram[0][r]++;
                    histogram[1][g]++;
                    histogram[2][b]++;
                    v++;
                }
                i++;
            }
            //if (y%20==0)
            //showProgress((double)(y-roiY)/roiHeight);
        }
        //hideProgress();
         IJ.log("pixels in mask "+v);
        return histogram;
    }*/
    
   void copyToClipboard() {
		Clipboard systemClipboard = null;
		try {systemClipboard = getToolkit().getSystemClipboard();}
		catch (Exception e) {systemClipboard = null; }
		if (systemClipboard==null)
			{IJ.error("Unable to copy to Clipboard."); return;}
		IJ.showStatus("Copying histogram values...");
		CharArrayWriter aw = new CharArrayWriter(256*4);
		PrintWriter pw = new PrintWriter(aw);
		for (int i=0; i<256; i++)
			pw.print(i+"\t"+histogram[0][i]+"\t"+histogram[1][i]+"\t"+histogram[2][i]+"\n");
		String text = aw.toString();
		pw.close();
		StringSelection contents = new StringSelection(text);
		systemClipboard.setContents(contents, this);
		IJ.showStatus(text.length() + " characters copied to Clipboard");
	}
	
    
    public void actionPerformed(java.awt.event.ActionEvent e) {
        Object b = e.getSource();
        if (b==list)
            showList();
        else if (b==copy)
        copyToClipboard();
        //else if (b==log)
        //replot();
    }
    
    
    public void mouseMoved(int x, int y) {
        if (value==null || count==null)
            return;

        if (frame!=null)  {
            if ( frame[0].contains(new Point(x,y))) {
                x = x - frame[0].x;
                if (x>255) x = 255;
                 int index = (int)(x*((double)histogram[0].length)/HIST_WIDTH);
                value.setText("  Value: " + index);
                count.setText("  Count: " + histogram[0][index]);
            } else if ( frame[1].contains(new Point(x,y))) {
                x = x - frame[1].x;
                if (x>255) x = 255;
                int index = (int)(x*((double)histogram[1].length)/HIST_WIDTH);
                
                value.setText("  Value: " + index);
                count.setText("  Count: " + histogram[1][index]);
            } else if ( frame[2].contains(new Point(x,y))) {
                x = x - frame[2].x;
                if (x>255) x = 255;
                int index = (int)(x*((double)histogram[2].length)/HIST_WIDTH);
                
                value.setText("  Value: " + index);
                count.setText("  Count: " + histogram[2][index]);
            } else  {
                value.setText("");
                count.setText("");
            }
        }
    }
    
    public void lostOwnership(java.awt.datatransfer.Clipboard clipboard, java.awt.datatransfer.Transferable transferable) {
    }
    
    
    
}

class ColorStats {
	int[][] histogram=new int[3][256] ;
	double [] hmean=new double[3];
	double [] hmin=new double[3];
	double [] hmax=new double[3];
	int [] hmode=new int[3];
	int [] hmodecnt=new int[3];
	double [] hstdev=new double[3];
	int histcount=0;
	
	public ColorStats(ColorProcessor cp, Rectangle rect) {
        int [] pixels=(int[])cp.getPixels();
        int width=cp.getWidth();
        histogram = getHistogram(width,pixels, rect);
        calculateStatistics(histogram);
	}
	
	public ColorStats(ColorProcessor cp, byte[] mask, Rectangle rect) {
        int [] pixels=(int[])cp.getPixels();
        int width=cp.getWidth();
        

        histogram= getHistogram(width, pixels,mask, rect);
        //ImageProcessor ipmask = imp.getMask();
        //new ImagePlus("ROI",ipmask).show();
 
        calculateStatistics(histogram);
	}
	
	public int[][] getHistogram(int width, int[] pixels, Rectangle roi) {
        
        int c, r, g, b;//, v;
        int roiY=roi.y;
        int roiX=roi.x;
        int roiWidth=roi.width;
        int roiHeight=roi.height;
        histcount=roi.width*roi.height;
       // IJ.log(" x " +roiX+" y " +roiY+ " width " +roiWidth+" hight " +roiHeight+" hc: "+ histcount);
        int[][] histogram = new int[3][256];
        for (int y=roiY; y<(roiY+roiHeight); y++) {
            int i = y * width + roiX;
           // for (int x=roiX; x<(roiX+roiWidth); x++) {
            for (int x=0; x<roiWidth; x++) {
                c = pixels[i];
                r = (c&0xff0000)>>16;
                g = (c&0xff00)>>8;
                b = c&0xff;
                
                histogram[0][r]++;
                histogram[1][g]++;
                histogram[2][b]++;
                i++;
                //IJ.log(""+i+" "+x);
            }
            
            //if (y%20==0)
            //showProgress((double)(y-roiY)/roiHeight);
        }
        //hideProgress();
        
        return histogram;
    }
    
    /** Value of pixels included in masks. */
    public static final int WHITE = 0xFF;

	 public int[][] getHistogram(int width, int[]pixels, byte[] mask, Rectangle roi) {
	        int c, r, g, b;
	        int[][] histogram = new int[3][256];
	        int roiY=roi.y;
	        int roiX=roi.x;
	        int roiWidth=roi.width;
	        int roiHeight=roi.height;
	        int v=0;
	        for (int y=roiY, my=0; y<(roiY+roiHeight); y++, my++) {
	            int i = y * width + roiX;
	            int mi = my * roiWidth;
	            //v=0;
	            for (int x=roiX, mx=0; x<(roiX+roiWidth); x++, mx++) {
	            	int cnt=mi+mx;
	            	//int w=mask[cnt]&0xff;
	            	//IJ.log("cnt "+ cnt+" m "+w+ " mask "+ mask[cnt]);
	            	//if(w!=0) v++;
	            	//IJ.log(" "+w);
	                if ((mask[cnt]&0xff)==WHITE) {
	                    c = pixels[i];
	                    r = (c&0xff0000)>>16;
	                    g = (c&0xff00)>>8;
	                    b = c&0xff;               
	                    histogram[0][r]++;
	                    histogram[1][g]++;
	                    histogram[2][b]++;
	                    v++;
	                }
	                i++;
	            }
	            //if (y%20==0)
	            //showProgress((double)(y-roiY)/roiHeight);
	        }
	        //hideProgress();
	         //IJ.log("pixels in mask "+v);
	         histcount=v;
	        return histogram;
	    }
	public int getHistcount() {
		return histcount;
	}

	public int[][] getHistogram() {
		return histogram;
	}

	public double[] getMean() {
		return hmean;
	}
	
	public int[] getMode() {
		return hmode;
	}
	
	public int[] getModeCnt() {
		return hmodecnt;
	}

	public double[] getStdev() {
		return hstdev;
	}
	
	protected void calculateStatistics(int[][]histogram) {
        double[] h2=new double[256];
        for (int col=0;col<3;col++) {
            int [][] tmp=getMinMax(histogram[col]);
            hmode[col]= tmp[1][0];
            hmodecnt[col]= tmp[1][1];
            int min=0;
            while ((histogram[col][min] == 0) && (min < 256))
                min++;
            hmin[col] = min;
            
            int max = 255;
            while ((histogram[col][max] == 0) && (max > 0))
                max--;
            hmax[col] = max;
            
            double sum=0;
            double sum2=0;

            for (int i=0; i<256;i++){
                sum+=i*histogram[col][i];
                sum2+=i*i*histogram[col][i];
                //z+=i;
            }
           
            
            //cumsum2=IntTrapz(h2, 1.0);
            //IJ.log("cumsum2: "+cumsum2);
            hmean[col]=sum/histcount;
            double histVar = (sum2-sum*sum/histcount)/(histcount-1.0);
           // IJ.log("histVar: "+histVar);
            hstdev[col] = Math.sqrt(histVar);
            
        }
        
    }
    
    public int[][] getMinMax(int[] a) {
        int min = a[0];
        int max =a[0];
        int value;
        int cntmin=-1, cntmax=-1;
        for (int i=0; i<a.length; i++) {
            value = a[i];
            if (value<=min) {
                min = value;
                cntmin=i;
            }
            if (value>=max) {
                max = value;
                cntmax=i;
            }
        }
        int[][] minAndMax = new int[2][2];
        //if (what==0) {
        	minAndMax[0][0] = min;
        	minAndMax[1][0]= max;
        //}
        //if (what==0) {
            minAndMax[0][1] = cntmin;
            minAndMax[1][1]= cntmax;
          //  }
        return minAndMax;
    }

	public double[] getMax() {
		return hmax;
	}

	public double[] getMin() {
		return hmin;
	}
}
