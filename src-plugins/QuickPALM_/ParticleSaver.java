package QuickPALM;

import ij.*;
import ij.measure.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.plugin.frame.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.CurveFitter.*;
import java.awt.*;
import ij.io.*;

import java.lang.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ParticleSaver
{
    public java.lang.String filename;
    private FileWriter fw;
    private java.util.concurrent.locks.Lock wlock = new java.util.concurrent.locks.ReentrantLock();
    private java.util.concurrent.locks.Lock lock = new java.util.concurrent.locks.ReentrantLock();
    private int counter = 1;
    private ThreadedSaver tsaver;
    private java.lang.String msg="";
    
    public void setup()
    {

        String path = "";
        final SaveDialog od = new SaveDialog("File to save particles into", "Particles Table", ".xls");
        path = od.getDirectory();
        this.filename = od.getFileName();
        this.filename = path+filename;
        try
        {
            this.fw = new FileWriter(this.filename);
            this.fw.write(" 	Intensity	X (px)	Y (px)	X (nm)	Y (nm)	Z (nm)	Left-Width(px)	Right-Width (px)	Up-Height (px)	Down-Height (px)	X Symmetry (%)	Y Symmetry (%)	Width minus Height (px)	Frame Number\n");
        }
        catch (Exception e)
        {
            IJ.error(""+e);
        }
    }
    
    public void saveParticle(double s, double x, double y, double x_, double y_, double z_, double left, double right, double up, double down, double xsym, double ysym, double wmh, double frame)
    {
        if (this.fw == null) return;    
        lock.lock();
        msg+=this.counter+"\t"+s+"\t"+x+"\t"+y+"\t"+x_+"\t"+y_+"\t"+z_+"\t"+left+"\t"+right+"\t"+up+"\t"+down+"\t"+xsym+"\t"+ysym+"\t"+wmh+"\t"+frame+"\n";
        lock.unlock();
        
        if (counter%1000==0)
        {
			lock.lock();
            this.tsaver = new ThreadedSaver();
            this.tsaver.mysetup(this.fw, this.wlock, msg);
            msg="";
			this.tsaver.start();
            lock.unlock();
        }
        this.counter++;
    }
    
    public void close()
    {
        if (this.fw == null) return;
        wlock.lock();
        try{this.fw.write(msg);}
        catch (Exception e) {IJ.error(""+e);}
        msg="";
        wlock.unlock();
        try {this.fw.close();}
        catch (Exception e) {IJ.error(""+e);}
    }    
}

class ThreadedSaver extends Thread
{
	private ReconstructionViewer viewer;
    FileWriter fw;
    java.util.concurrent.locks.Lock wlock;
    java.lang.String msg;
	
	public void mysetup(FileWriter fw_, java.util.concurrent.locks.Lock wlock_, java.lang.String msg_)
	{
        fw=fw_;
        wlock=wlock_;
        msg=msg_;
	}
	
	public void run()
	{
        wlock.lock();
        try
        {
            fw.write(msg);
        }
        catch (Exception e)
        {
            IJ.error(""+e);
        }
        wlock.unlock();
	}
}