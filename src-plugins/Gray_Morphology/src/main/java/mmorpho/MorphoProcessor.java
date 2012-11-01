/*
 * MorphoProcessor.java
 *  @version 1.0.0; 13 November 2004
 * Created on 13 November 2004, 20:19
 */

package mmorpho;
import ij.*;
import ij.process.*;
import ij.plugin.filter.*;
import java.util.*;
import java.lang.reflect.*;
import ij.util.*;
import mmorpho.*;
/**
 *
 * @author  Dimiter Prodanov
 * @version  1.5   05 June 2006
 * 			 1.0;  09 Dec 2004
 */
public class MorphoProcessor implements Constants {
	private final static String version="1.5";
    private StructureElement se, minus_se, plus_se; //, down_se, up_se;
    private LocalHistogram bh,p_h,m_h;
    private int[][]pg,pg_plus,pg_minus;
    int width, height;

    /** Creates a new instance of MorphoProcessor */
    public MorphoProcessor(StructureElement se) {
    	this.se=se;
        width=se.getWidth();
        height=se.getHeight();
        minus_se = new StructureElement(se.H(se.Delta(SGRAD),HMINUS),width);
        plus_se=new StructureElement(se.H(se.Delta(NGRAD),HMINUS),width);
        bh=new LocalHistogram();
        p_h=new LocalHistogram();
        m_h=new LocalHistogram();
        pg=se.getVect();
        pg_plus=plus_se.getVect();
        pg_minus=minus_se.getVect();
    }
    
    
    private final static int ORIG=0,PLUS=1,MINUS=-1;
    
    public StructureElement getSE(int options) {
        switch (options) {
            case ORIG: {
                return se;
            }
            case PLUS:{
                return  plus_se;
            }
            case MINUS:
                return minus_se;
                
        }
        return se;
    }
    
    
    /*
     * @param ip the ImageProcessor
     * @param se the StructureElement
     */
    
    
    /** Performs gray level erosion */
    public void fastErode(ImageProcessor ip) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        int  min = -32767;//,k=0,x=0,y=0;
        
        //int pgzise=pg.length;
        byte[] pixels=(byte[])ip.getPixels();
        byte[] newpix= new byte[pixels.length];
       // String s="", s2="";
        
        int row=0, z=0;
        int index=0;
        
        
       //boolean changed=false;
        for (row=1;row<=height;row++){
            z=(row-1)*width;
            //      IJ.log("odd index  "+ z);
            bh.init(z,   width, height, pixels, pg, 1) ;
            
            // bh.Log();
            //bh.doMinimum();
            min=bh.getMinimum();
            newpix[z]=(byte)(min&0xFF);
            for  (int col=1; col<width;col++){
                index=z+col;
                //          s2+=" "+index+"\r\n";
                //  StringBuffer sb=new StringBuffer(100);
                try {
                    p_h.init(index, width, height, pixels, pg_plus, 1) ;
                    m_h.init(index-1, width, height, pixels, pg_minus, 1);
                    bh.sub(m_h);
                    bh.add(p_h);
                    
                    bh.doMinimum();
                    
                    min=bh.getMinimum();
                    newpix[index]=(byte)(min&0xFF);
                }
                catch  ( ArrayIndexOutOfBoundsException aiob) {
                    IJ.log(" out index: "+index+" min "+min);
                    //IJ.log(sb.toString());
                }
            } //odd loop
            
            
        }
        //IJ.log("add " +cnt + " sub " +cnt2);
        //         IJ.log(" " +index+" height "+height );
        //         IJ.log(s2);
        //         IJ.log(s);
        
        
        System.arraycopy(newpix, 0, pixels, 0, pixels.length);
    }
    
    /** Performs gray level dilation */
    public void fastDilate(ImageProcessor ip) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        int  max = 32767;//,k=0,x=0,y=0;
       
        // int pgzise=pg.length;
        byte[] pixels=(byte[])ip.getPixels();
        byte[] newpix= new byte[pixels.length];
        //String s="", s2="";
        
        int row=0,z=0;
        int index=0;
               
        for ( row=1;row<=height;row++){
            
            z=(row-1)*width;
            //    IJ.log("odd index  "+ z);
            bh.init(z,   width, height, pixels, pg, 0) ;
            //  bh.doMaximum();
            max=bh.getMaximum();
            newpix[z]=(byte)(max&0xFF);
            for  (int col=1; col<width;col++){
                index=z+col;
                //          s2+=" "+index+"\r\n";
                try {
                    p_h.init(index, width, height, pixels, pg_plus, 0) ;
                    m_h.init(index-1, width, height, pixels, pg_minus, 0) ;
                    bh.sub(m_h);
                    bh.add(p_h);
                    bh.doMaximum();
                    max=bh.getMaximum();
                    newpix[index]=(byte)(max&0xFF);
                }
                catch  ( ArrayIndexOutOfBoundsException aiob) {
                    IJ.log(" out index: "+index);
                }
            } //odd loop
            
        }
        
        //         IJ.log(" " +index+" height "+height );
        //         IJ.log(s2);
        //         IJ.log(s);
         System.arraycopy(newpix, 0, pixels, 0, pixels.length);
    }
    
    /*
     * @param ip the ImageProcessor
     * @param se the StructureElement
     */
    
    /** Performs gray level erosion */
    public void  erode(ImageProcessor ip){
        
        int width = ip.getWidth();
        int height = ip.getHeight();
        int min = -32767; //,k=0,x=0,y=0;
       
        
        int sz=pg.length;//se.getWidth()*se.getHeight();
        // byte[] p=(byte[])ip.convertToByte(false).getPixels();
        byte[] pixels=(byte[])ip.getPixels();
        
        int[] wnd=new int[sz];
        
        byte[] newpix= new byte[pixels.length];
        
        //int i,j=0;
        for (int c=0;c<pixels.length;c++) {
            // i=c/width;
            // j=c%width;
            wnd=getMinMax(c, width, height, pixels, pg, ERODE);
            min=wnd[0]+255;
            newpix[c]=(byte)(min&0xFF);
            
        }
        
        
        System.arraycopy(newpix, 0, pixels, 0, pixels.length);
    }
    
    
    /** Performs gray level dilation
     * @param ip the ImageProcessor
     * @param se the StructureElement
     */
    public void dilate(ImageProcessor ip){
        
        int width = ip.getWidth();
        int height = ip.getHeight();
        int  max = 32768;//,k=0,x=0,y=0;
      
        //int[][]pg=se.getVect();
        //  IJ.log("pg: "+pg.length);
        int sz=pg.length; //se.getWidth()*se.getHeight();
       
        byte[] pixels=(byte[])ip.getPixels();
        int[] wnd=new int[sz];
               
        byte[] newpix= new byte[pixels.length];
        //int i,j=0;
        for (int c=0;c<pixels.length;c++) {
            
            //i=c/width;
            //j=c%width;
            wnd=getMinMax(c, width, height, pixels, pg,DILATE);
   
            max=wnd[1]-255;
            newpix[c]=(byte)(max&0xFF);
            
        }
        
        System.arraycopy(newpix, 0, pixels, 0, pixels.length);
    }
    
    public final static int BINF=-256;
    
    private int[] getMinMax(int index, int width, int height, byte[] pixels, int[][] pg,  int type) {
        //  int[][]pg=se.getVectTransform(mType);
        int pgzise=pg.length;
        
        int[] wnd=new int[2];
        int i,j,k=0;
        int x,y=0;
        int min=255;
        int max=0;

        i=index/width;
        j=index%width;
        for (int g=0;g<pgzise;g++){
            y=i+pg[g][0];
            x=j+pg[g][1];
            try {
                if  ((x>=width) || (y>=height) || (x<0) || (y<0) ) {
                    if (type==DILATE)  k=0;
                    if (type==ERODE)  k=255;
                }
                else {
                    k=pixels[x+width*y]&0xFF;
                }
            }
            catch (ArrayIndexOutOfBoundsException ex) {
                
                k=x+width*y;
                IJ.log("AIOB x: "+x+" y: "+y+" index: "+k);
            }
            
            if (type==DILATE)    k=k+pg[g][2];
            if (type==ERODE)   k=k-pg[g][2];
            if (k<min) min=k; 
            if (k>max) max=k;
     
            
        }
        wnd[0]=min&0xFF;
        wnd[1]=max&0xFF;
        return wnd;
    }
    
    private int[] getRanks(int index, int width, int height, byte[] pixels,int[][] pg,  int type) {
        //  int[][]pg=se.getVectTransform(mType);
        int pgzise=pg.length;
        
        int[] wnd=new int[pgzise];
        int i,j,k=0;
        int x,y=0;
        
        i=index/width;
        j=index%width;
        for (int g=0;g<pgzise;g++){
            y=i+pg[g][0];
            x=j+pg[g][1];
            try {
                if  ((x>=width) || (y>height-1) || (x<0) || (y<0) ) {
                    if (type==DILATE)  k=0;
                    if (type==ERODE)  k=255;
                }
                else {
                    k=pixels[x+width*y]&0xFF;
                }
            }
            catch (ArrayIndexOutOfBoundsException ex) {
                
                k=x+width*y;
                IJ.log("AIOB x: "+x+" y: "+y+" index: "+k);
            }
            
            //  if (type==DILATE)    wnd[g]=k+pg[g][2];
            // if (type==ERODE)    wnd[g]=k-pg[g][2];
            
            if (type==ERODE)   {
                k=k-pg[g][2];
                
                int v=0;
                while (v<=g) {
                    if (k>=wnd[v]) {
                        v++;
                    }
                    else {
                        wnd[v]=k;
                        v++;
                        break;
                    }
                }
                
            }
            if (type==DILATE)  {
                k=k+pg[g][2];
                //int swp=0;
                int v=pgzise-1;
                
                while (v>=g) {
                    if (k<=wnd[v]) {
                        v--;
                    }
                    else {
                        wnd[v]=k;
                        v--;
                        break;
                    }
                }
            }
        }
        
        return wnd;
    }
    
    
    /** Performs graylevel erosion followed by graylevel dilation
     *  with arbitrary structural element se
     * @param ip the ImageProcessor
     * @param se the StructureElement
     */
    
    public void open(ImageProcessor ip){
        int width = ip.getWidth();
        int height = ip.getHeight();
        int min = -32767;//,k=0,x=0,y=0;
        int  max = 32768;
        int w=this.width;//se.getWidth();
        int h=this.height;//se.getHeight();
         // int[][] pg=se.getVect();
        
        int sz=pg.length;
        
        byte[] pixels=(byte[])ip.getPixels();
        byte[] newpix= new byte[pixels.length];
        byte[] newpix2= new byte[pixels.length];  
        int[] wnd=new int[sz];
        //  int i,j=0;
        for (int row=1; row<=height; row++) {
            for (int col=0; col<width; col++){
                int index=(row-1)*width+col; //erosion step
                 if (index< pixels.length) {
                     wnd=getMinMax(index, width, height, pixels, pg, ERODE);
                     min=wnd[0]+255;
                     newpix[index]=(byte)(min&0xFF);
                  }
                int index2=(row-h-1)*width+col-w; //dilation step
                if ((index2>=0) && (index2< pixels.length)) {
                     wnd=getMinMax(index2, width, height, newpix, pg,DILATE);
                     max=wnd[1]-255;
                     newpix2[index2]=(byte)(max&0xFF);
                }
            }
        }
            for (int row=height; row<=height+h; row++){
                for (int col=0; col<width+w; col++){
                      int index2=(row-h-1)*width+col-w; //dilation step
                      if ((index2>=0) && (index2< pixels.length)) {
                         wnd=getMinMax(index2, width, height, newpix, pg,DILATE);
                         max=wnd[1]-255;
                         newpix2[index2]=(byte)(max&0xFF);
                      }
                }
            }
                
        
        /*
        for (int c=0;c<pixels.length;c++) {
            // i=c/width;
            // j=c%width;
            wnd=getMinMax(c, width, height, pixels, pg, ERODE);
       
            min=wnd[0]+255;
            newpix[c]=(byte)(min&0xFF);
            
        }
        
        
        int  max = 32768;
        k=x=y=0;
        //wnd=new int[sz];
        
        
     
        i=j=0;
        for (int c=0;c<pixels.length;c++) {
            
            //i=c/width;
            //j=c%width;
            wnd=getMinMax(c, width, height, newpix, pg,DILATE);
            
            max=wnd[1]-255;
            newpix2[c]=(byte)(max&0xFF);
            
        }
        */
        System.arraycopy(newpix2, 0, pixels, 0, pixels.length);
        
        
        
    }
    
    /** Performs fast graylevel erosion followed by fast graylevel dilation
     *  with arbitrary structural element se
     * @param ip the ImageProcessor
     * @param se the StructureElement
     */
    
    
    public void fopen(ImageProcessor ip){
        int width = ip.getWidth();
        int height = ip.getHeight();
        int  min = -32767;//,k=0,x=0,y=0;
        //int pgzise=pg.length;
        byte[] pixels=(byte[])ip.getPixels();
        byte[] newpix= new byte[pixels.length];
        //String s="", s2="";
        
        int row=0, z=0;
        int index=0;
        
        // Erosion
        
        //boolean changed=false;
        for ( row=1;row<=height;row++){
            z=(row-1)*width;
            //   IJ.log("odd index  "+ z);
            bh.init(z,   width, height, pixels, pg, 1) ;
            // bh.Log();
            //bh.doMinimum();
            min=bh.getMinimum();
            newpix[z]=(byte)(min&0xFF);
            for  (int col=1; col<width;col++){
                index=z+col;
                //          s2+=" "+index+"\r\n";
                //  StringBuffer sb=new StringBuffer(100);
                try {
                    p_h.init(index, width, height, pixels, pg_plus, 1) ;
                    m_h.init(index-1, width, height, pixels, pg_minus, 1);
                    bh.sub(m_h);
                    bh.add(p_h);
                    
                    bh.doMinimum();
                    
                    min=bh.getMinimum();
                    newpix[index]=(byte)(min&0xFF);
                }
                catch  ( ArrayIndexOutOfBoundsException aiob) {
                    IJ.log(" out index: "+index+" min "+min);
                    //IJ.log(sb.toString());
                }
            } //odd loop
            
            
        }
        
        // Dilation
        int  max = 32767;//,k=0,x=0,y=0;
        
        byte[] newpix2= new byte[pixels.length];
        
        for (row=1;row<=height;row++){
            
            z=(row-1)*width;
            //    IJ.log("odd index  "+ z);
            bh.init(z,   width, height, newpix, pg, 0) ;
            //  bh.doMaximum();
            max=bh.getMaximum();
            newpix2[z]=(byte)(max&0xFF);
            for  (int col=1; col<width;col++){
                index=z+col;
                //          s2+=" "+index+"\r\n";
                try {
                    p_h.init(index, width, height, newpix, pg_plus, 0) ;
                    m_h.init(index-1, width, height, newpix, pg_minus, 0) ;
                    bh.sub(m_h);
                    bh.add(p_h);
                    bh.doMaximum();
                    max=bh.getMaximum();
                    newpix2[index]=(byte)(max&0xFF);
                }
                catch  ( ArrayIndexOutOfBoundsException aiob) {
                    IJ.log(" out index: "+index);
                }
            } //odd loop
            
            
        }
        
        
        System.arraycopy(newpix2, 0, pixels, 0, pixels.length);
        
    }
    
    
    
    
    /**
     *  Performs graylevel dilation followed by graylevel erosion
     *  with arbitrary structural element
     * @param ip the ImageProcessor
     * @param se the StructureElement
     *
     **/
    public void close(ImageProcessor ip){
        int width = ip.getWidth();
        int height = ip.getHeight();
        int w=this.width;//se.getWidth();
        int h=this.height;//se.getHeight();
        int  min=0,max = 255;//,k=0,x=0,y=0;

        //  IJ.log("pg: "+pg.length);
        int sz=pg.length;//se.getWidth()*se.getHeight();
        
        byte[] pixels=(byte[])ip.getPixels();
        byte[] newpix= new byte[pixels.length];
        byte[] newpix2= new byte[pixels.length];
        int[] wnd=new int[sz];
        
        for (int row=1; row<=height; row++) {
            for (int col=0; col<width; col++){
                int index=(row-1)*width+col; //dilation step
                 if (index< pixels.length) {
                     wnd=getMinMax(index, width, height, pixels, pg, DILATE);
                     max=wnd[1]-255; 
                     newpix[index]=(byte)(max&0xFF);
                  }
                int index2=(row-h-1)*width+col-w; //erosion step
                if ((index2>=0) && (index2< pixels.length)) {
                     wnd=getMinMax(index2, width, height, newpix, pg,ERODE);
                     min=wnd[0]+255;
                     newpix2[index2]=(byte)(min&0xFF);
                }
            }
        }
            for (int row=height; row<=height+h; row++){
                for (int col=0; col<width+w; col++){
                      int index2=(row-h-1)*width+col-w; //erosion step
                      if ((index2>=0) && (index2< pixels.length)) {
                         wnd=getMinMax(index2, width, height, newpix, pg,ERODE);
                          min=wnd[0]+255;
                         newpix2[index2]=(byte)(min&0xFF);
                      }
                }
            }
        /*
        
    
        int i,j=0;
        for (int c=0;c<pixels.length;c++) {
            
            //i=c/width;
            //j=c%width;
            wnd=getMinMax(c, width, height, pixels, pg,DILATE);
            //max=getMinMax(wnd)[1]-255;
            max=wnd[1]-255;
            newpix[c]=(byte)(max&0xFF);
            
        }
        
        int min = -32767;
        k=x=y=0;
        
        i=j=0;
        for (int c=0;c<pixels.length;c++) {
            // i=c/width;
            // j=c%width;
            wnd=getMinMax(c, width, height, newpix, pg, ERODE);
            //min=getMinMax(wnd)[0]+255;
            min=wnd[0]+255;
            newpix2[c]=(byte)(min&0xFF);
            
        }
        */
        
        System.arraycopy(newpix2, 0, pixels, 0, pixels.length);
        
        
    }
    
    /**
     *  Performs fast graylevel dilation followed by fast graylevel erosion
     *  with arbitrary structural element
     *  @param ip the ImageProcessor
     * @param se the StructureElement
     */
    
    public void fclose(ImageProcessor ip){
        
        //fastDilate(ip,se);
        //fastErode(ip,se);
        int width = ip.getWidth();
        int height = ip.getHeight();
        int  max = 32767;//,k=0,x=0,y=0;
         
        //int pgzise=pg.length;
        byte[] pixels=(byte[])ip.getPixels();
        byte[] newpix= new byte[pixels.length];
        //String s="", s2="";
        
        int row=0,z=0;
        int index=0;
        
        // Dilation loop
        
        for (row=1;row<=height;row++){
            
            z=(row-1)*width;
            //    IJ.log("odd index  "+ z);
            bh.init(z,   width, height, pixels, pg, 0) ;
            //  bh.doMaximum();
            max=bh.getMaximum();
            newpix[z]=(byte)(max&0xFF);
            for  (int col=1; col<width;col++){
                index=z+col;
                //          s2+=" "+index+"\r\n";
                try {
                    p_h.init(index, width, height, pixels, pg_plus, 0) ;
                    m_h.init(index-1, width, height, pixels, pg_minus, 0) ;
                    bh.sub(m_h);
                    bh.add(p_h);
                    bh.doMaximum();
                    max=bh.getMaximum();
                    newpix[index]=(byte)(max&0xFF);
                }
                catch  ( ArrayIndexOutOfBoundsException aiob) {
                    IJ.log(" out index: "+index);
                }
            } //odd loop
            
            
        }
        
        int  min = -32767;//,k=0,x=0,y=0;
                
        byte[] newpix2= new byte[pixels.length];

        
        // Erosion loop
        
        //boolean changed=false;
        for (row=1;row<=height;row++){
            z=(row-1)*width;
            //                                //    IJ.log("odd index  "+ z);
            bh.init(z,   width, height, newpix, pg, 1) ;
            
            // bh.Log();
            //bh.doMinimum();
            min=bh.getMinimum();
            newpix2[z]=(byte)(min&0xFF);
            for  (int col=1; col<width;col++){
                index=z+col;
                //          s2+=" "+index+"\r\n";
                //  StringBuffer sb=new StringBuffer(100);
                try {
                    p_h.init(index, width, height, newpix, pg_plus, 1) ;
                    m_h.init(index-1, width, height, newpix, pg_minus, 1);
                    bh.sub(m_h);
                    bh.add(p_h);
                    
                    bh.doMinimum();
                    
                    min=bh.getMinimum();
                    newpix2[index]=(byte)(min&0xFF);
                }
                catch  ( ArrayIndexOutOfBoundsException aiob) {
                    IJ.log(" out index: "+index+" min "+min);
                    //IJ.log(sb.toString());
                }
            } //odd loop
            
            
        }
        
        
        
        System.arraycopy(newpix2, 0, pixels, 0, pixels.length);
        
    }
    
//  implementation of the DAA algorithm for fast linear erosion
    public void LineErode(ImageProcessor ip) {
        
        int width = ip.getWidth();
        int height = ip.getHeight();
        int  min = -32767;//,k=0,x=0,y=0;
        //Log(pg);
        int w=Math.max(this.width,this.height);
       
        int shift=se.getShift();
        int p=w-2*shift;
        
        int[] wnd=new int[2*p];
        int[] R=new int[p];
        int[] S=new int[p];
        //int r=(int)se.getR();
        int type=se.getType();
        // IJ.log("r: "+r+ " l: "+p+" sh: "+shift);
        
        byte[] pixels=(byte[])ip.getPixels();
        byte[] newpix= new byte[pixels.length];
        
        int z=0, index=0;
        //IJ.log("type "+type);
        if (type==HLINE) {
            
            for (int row=0;row<height;row++){
                z=row*width;

                for  (int col=0; col<=width+p;col+=p){
                    int k=0;
                    //wnd population
                    for (int i=-p; i<p; i++) {
                        int x=col+i;
                        index=z+x;
                        //  edge effects
                        try {
                            if  ((x>width) || (x<0) || (index>=width*height)) {
                                k=255;
                           }
                            else {
                                k=pixels[index]&0xFF;
                            }
                        }
                        catch (ArrayIndexOutOfBoundsException ex) {
                            IJ.log("AIOB row: "+row+" col: "+col+" index: "+index+"x: "+x);
                        }
                        wnd[i+p]=k;
                } // wnd

                
                R[0]=wnd[p]; //center
                S[0]=wnd[p]; //center

                for (int j=1; j<p; j++){
                    R[j]=Math.min(R[j-1],wnd[p-j]); //backward
                    S[j]=Math.min(S[j-1],wnd[j+p]);//forward
                }
                int offset=0;
                 
                for (int j=0; j<p;j++){
                    
                    try {
                        min=Math.min(R[j],S[p-j-1]);
                        offset=-j+p/2;
                        if ( (col+offset>=0) && (col+offset<width))
                            newpix[z+col+offset]=(byte)(min&0xFF);
                          //newpix[z+col+offset]=(byte)(0xA0);
                        }
                        catch  ( ArrayIndexOutOfBoundsException aiob) {
                            IJ.log("row: "+row+" col: "+col+" off: "+offset);
                        }
                    }
                } //odd loop
            }
        }
    
        
        if (type==VLINE) {
          for  (int col=0; col<width;col++){
       
            //z=(col-1);
           for (int row=0;row<height+p;row+=p){
               // z=(row-1)*width;
                int k=0;
                //wnd population
                for (int i=-p; i<p; i++) {
                    int y=row+i;
                    index=col+y*width;
                    //  edge effects
                    try {
                        if  ((y>height) || (y<0) || (index>width*height-1)) {
                            k=255;
                       }
                        else {
                            k=pixels[index]&0xFF;
                        }
                    }
                    catch (ArrayIndexOutOfBoundsException ex) {
        
                        IJ.log("AIOB row: "+row+" col: "+col+" index: "+index+"y: "+y);
                    }
                    wnd[i+p]=k;

                } // wnd

                
                R[0]=wnd[p]; //center
                S[0]=wnd[p]; //center

                for (int j=1; j<p; j++){
                    R[j]=Math.min(R[j-1],wnd[p-j]); //backward
                    S[j]=Math.min(S[j-1],wnd[j+p]);//forward
                }
                 int y=0;
                 
                for (int j=0; j<p;j++){
                     try {
                        min=Math.min(R[j],S[p-j-1]);
                        y=p/2-j;
                        if ( (row+y>=0) && (col+(row+y)*width<height*width))
                            newpix[col+(row+y)*width]=(byte)(min&0xFF);
                      //  newpix[col+(row+y)*width]=(byte)(0xA0);
                    }
                    catch  ( ArrayIndexOutOfBoundsException aiob) {
                        IJ.log("row: "+row+" col: "+col+" off: "+y);
         
                    }
                }
            } //odd loop
            
            
        }
        }
         
        System.arraycopy(newpix, 0, pixels, 0, pixels.length);
        
        //  }
        //    else IJ.log("wrong type");
    }
    
//  implementation of the DAA algorithm for fast linear dilation
    public void LineDilate(ImageProcessor ip) {
        
        int width = ip.getWidth();
        int height = ip.getHeight();
        int  min = -32767;//,k=0,x=0,y=0;
        
        int w=Math.max(this.width,this.height);
       
        int shift=se.getShift();
        int p=w-2*shift;
        
        int[] wnd=new int[2*p];
        int[] R=new int[p];
        int[] S=new int[p];
       // int r=(int)se.getR();
        int type=se.getType();
       //  IJ.log("r: "+r+ " l: "+p+" sh: "+shift);
        
        byte[] pixels=(byte[])ip.getPixels();
        byte[] newpix= new byte[pixels.length];
        
        int z=0, index=0;
        //IJ.log("type "+type);
        if (type==HLINE) {
            
            for (int row=0;row<height;row++){
                z=row*width;

                for  (int col=0; col<=width+p;col+=p){
                    int k=0;
                    //wnd population
                    for (int i=-p; i<p; i++) {
                        int x=col+i;
                        index=z+x;
                        //  edge effects
                        try {
                            if  ((x>width) || (x<0) || (index>=width*height)) {
                                k=0;
                           }
                            else {
                                k=pixels[index]&0xFF;
                            }
                        }
                        catch (ArrayIndexOutOfBoundsException ex) {
                            IJ.log("AIOB row: "+row+" col: "+col+" index: "+index+"x: "+x);
                        }
                        wnd[i+p]=k;
                } // wnd

                
                R[0]=wnd[p]; //center
                S[0]=wnd[p]; //center

                for (int j=1; j<p; j++){
                    R[j]=Math.max(R[j-1],wnd[p-j]); //backward
                    S[j]=Math.max(S[j-1],wnd[j+p]);//forward
                }
                int offset=0;
                 
                for (int j=0; j<p;j++){
                    
                    try {
                        min=Math.max(R[j],S[p-j-1]);
                        offset=-j+p/2;
                        if ( (col+offset>=0) && (col+offset<width))
                            newpix[z+col+offset]=(byte)(min&0xFF);
                          
                        }
                        catch  ( ArrayIndexOutOfBoundsException aiob) {
                            IJ.log("row: "+row+" col: "+col+" off: "+offset);
                        }
                    }
                } //odd loop
            }
        }
    
        
        if (type==VLINE) {
          for  (int col=0; col<width;col++){
       
            //z=(col-1);
           for (int row=0;row<height+p;row+=p){
               // z=(row-1)*width;
                int k=0;
                //wnd population
                for (int i=-p; i<p; i++) {
                    int y=row+i;
                    index=col+y*width;
                    //  edge effects
                    try {
                        if  ((y>height) || (y<0) || (index>width*height-1)) {
                            k=0;
                       }
                        else {
                            k=pixels[index]&0xFF;
                        }
                    }
                    catch (ArrayIndexOutOfBoundsException ex) {
        
                        IJ.log("AIOB row: "+row+" col: "+col+" index: "+index+"y: "+y);
                    }
                    wnd[i+p]=k;

                } // wnd

                
                R[0]=wnd[p]; //center
                S[0]=wnd[p]; //center

                for (int j=1; j<p; j++){
                    R[j]=Math.max(R[j-1],wnd[p-j]); //backward
                    S[j]=Math.max(S[j-1],wnd[j+p]);//forward
                }
                 int y=0;
                 
                for (int j=0; j<p;j++){
                     try {
                        min=Math.max(R[j],S[p-j-1]);
                        y=p/2-j;
                        if ( (row+y>=0) && (col+(row+y)*width<height*width))
                            newpix[col+(row+y)*width]=(byte)(min&0xFF);
                      //  newpix[col+(row+y)*width]=(byte)(0xA0);
                    }
                    catch  ( ArrayIndexOutOfBoundsException aiob) {
                        IJ.log("row: "+row+" col: "+col+" off: "+y);
         
                    }
                }
            } //odd loop
            
            
        }
        }
         
        System.arraycopy(newpix, 0, pixels, 0, pixels.length);
        
        //  }
        //    else IJ.log("wrong type");
    }
    
    /** Calculates Min and Max element of an array
     * @return [0] - min [1] - max
     * @param a the array
     *
     * public static int[] getMinMax(int[] a) {
     * int min = a[0];
     * int max =a[0];
     * int value;
     * for (int i=0; i<a.length; i++) {
     * value = a[i];
     * if (value<min)
     * min = value;
     * if (value>max)
     * max = value;
     * }
     * int[] minAndMax = new int[2];
     * minAndMax[0] = min;
     * minAndMax[1] = max;
     * return minAndMax;
     * }
     */
    private final float findMean(float[] values) {
        float sum = values[0];
        for (int i=1; i<values.length; i++)
            sum += values[i];
        return (float)(sum/values.length);
    }
    
    /** Logs
     * @param arr the array
     */
    private void Log(int[][] a){
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
    
}
