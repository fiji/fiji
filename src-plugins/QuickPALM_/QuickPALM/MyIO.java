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

class MyIO
{    
    void loadTransformation(String filename, ResultsTable res)
    {
        try {
               String line;
               FileReader fr = new FileReader(filename);
               BufferedReader br = new BufferedReader(fr);
               if (!br.readLine().equals(" 	Z-Step	Raw Width minus Heigh	Calibration Width minus Height"))
               {
                   IJ.error("File does not seam to be an Astigmatism calibration file");
                   return;
               }
               //java.lang.String [] elements = new java.lang.String [3];
               java.lang.String [] elements;
               int counter = 1;
               res.reset();
               while ((line = br.readLine()) != null)
               {
                    IJ.showStatus("Loading element "+counter+"... sit back and relax.");
                    counter++;
                    line.trim();
                    elements = line.split("\t");
                    res.incrementCounter();
                    res.addValue("Z-Step", Double.parseDouble(elements[1]));
                    res.addValue("Raw Width minus Heigh", Double.parseDouble(elements[2]));
                    res.addValue("Calibration Width minus Height", Double.parseDouble(elements[3]));
               }
               fr.close();
        }   
        catch (FileNotFoundException e) {
            IJ.error("File not found exception" + e);
            return;
        } catch (IOException e) {
            IJ.error("IOException exception" + e);
            return;
        } catch (NumberFormatException e) {
            IJ.error("Number format exception" + e);
            return;
        }
    }
    
    void loadParticleResults(String filename, ResultsTable res)
    {
        try {
               String line;
               FileReader fr = new FileReader(filename);
               BufferedReader br = new BufferedReader(fr);
               java.lang.String header = " 	Intensity	X (px)	Y (px)	X (nm)	Y (nm)	Z (nm)	Left-Width(px)	Right-Width (px)	Up-Height (px)	Down-Height (px)	X Symmetry (%)	Y Symmetry (%)	Width minus Height (px)	Frame Number";
               java.lang.String firstline = br.readLine();
               if (!firstline.contains("X (px)	Y (px)	X (nm)	Y (nm)	Z (nm)"))
               {
                   IJ.error("File does not seam to be a Particles Table file");
                   IJ.log("Found header: "+firstline);
                   IJ.log("Expecting: "+header);
                   return;
               }
               res.reset();
               int counter = 1;
               java.util.concurrent.locks.Lock lock = new java.util.concurrent.locks.ReentrantLock();
               ThreadedLoader tloader  = new ThreadedLoader();
               //java.lang.String txt = fr.read();
               while ((line = br.readLine()) != null)
               {
                    tloader = new ThreadedLoader();
                    tloader.mysetup(res, lock, line);
                    tloader.start();
                    IJ.showStatus("Loading particle "+counter+"... sit back and relax.");
                    counter++;
               }
               try {tloader.join();}
               catch (Exception e) {IJ.error(""+e);}
               if (res.getCounter()<5000000)
               {
                    IJ.showStatus("Creating particle table, this should take a few seconds...");
                    res.show("Results");
               }
                else
                    IJ.showMessage("Warning", "Results table has too many particles, they will not be shown but the data still exists within it\nyou can still use all the plugin functionality or save table changes though the 'Save Particle Table' command.");
               fr.close();
               IJ.showStatus("Done loading table...");
        }   
        catch (FileNotFoundException e) {
            IJ.error("File not found exception" + e);
            return;
        } catch (IOException e) {
            IJ.error("IOException exception" + e);
            return;
        } catch (NumberFormatException e) {
            IJ.error("Number format exception" + e);
            return;
        }
    }
}
    
class ThreadedLoader extends Thread
{
    java.util.concurrent.locks.Lock lock;
    ResultsTable res;
	java.lang.String line;
    java.lang.String [] elements;
    
	public void mysetup(ResultsTable res_, java.util.concurrent.locks.Lock lock_, java.lang.String line_)
	{
        res=res_;
        lock=lock_;
        line=line_;
	}
	
	public void run()
	{
        line.trim();
        elements = line.split("\t");
        if (elements.length<14) return;
        
        lock.lock();
        res.incrementCounter();
        res.addValue("Intensity",            Double.parseDouble(elements[1]));
        res.addValue("X (px)",               Double.parseDouble(elements[2]));
        res.addValue("Y (px)",               Double.parseDouble(elements[3]));
        res.addValue("X (nm)",               Double.parseDouble(elements[4]));
        res.addValue("Y (nm)",               Double.parseDouble(elements[5]));
        res.addValue("Z (nm)",               Double.parseDouble(elements[6]));
        res.addValue("Left-StdDev (px)",     Double.parseDouble(elements[7]));
        res.addValue("Right-StdDev (px)",    Double.parseDouble(elements[8]));
        res.addValue("Up-StdDev (px)",       Double.parseDouble(elements[9]));
        res.addValue("Down-StdDev (px)",     Double.parseDouble(elements[10]));			
        res.addValue("X Symmetry (%)",       Double.parseDouble(elements[11]));
        res.addValue("Y Symmetry (%)",       Double.parseDouble(elements[12]));
        res.addValue("Width minus Height (px)", Double.parseDouble(elements[13]));
        res.addValue("Frame Number",         Double.parseDouble(elements[14]));
        lock.unlock();
	}
}

