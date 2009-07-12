/*
 * _3D_objects_counter.java
 *
 * Created on 7 novembre 2007, 11:54
 *
 * Copyright (C) 2007 Fabrice P. Cordelières
 *  
 * License:
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 
 */


import ij.*;
import ij.ImagePlus.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import ij.util.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;

import Utilities.*;

/**
 *
 * @author Fabrice P. Cordelières, fabrice.cordelieres@gmail.com
 * @version 1.0, 7/11/07
 */
public class _3D_objects_counter implements PlugIn, AdjustmentListener, TextListener {
    ImagePlus imp;
    ImageProcessor ip;
    int width, height, nbSlices, length;
    double min, max;
    String title, redirectTo;
    int thr, minSize, maxSize, dotSize, fontSize;
    boolean excludeOnEdges, showObj, showSurf, showCentro, showCOM, showNb, whiteNb, newRT, showStat, showMaskedImg, closeImg, showSummary, redirect;
    Vector sliders, values;
    
    public void run(String arg) {
        if (IJ.versionLessThan("1.39i")) return;
        
        imp=WindowManager.getCurrentImage();
        
        if (imp==null){
            IJ.error("Man,\n"+"How can I work\n"+"without an image ?!!!");
            return;
        }
        
        if (imp.getBitDepth()>16){
            IJ.error("3D objects counter only works on 8- or 16-bits images...");
            return;
        }
        
        width=imp.getWidth();
        height=imp.getHeight();
        nbSlices=imp.getStackSize();
        length=height*width*nbSlices;
        title=imp.getTitle();
        
        min=Math.pow(2, imp.getBitDepth());
        max=0;
        
        for (int i=1; i<=nbSlices; i++){
            imp.setSlice(i);
            ip=imp.getProcessor();
            min=Math.min(min, imp.getStatistics().min);
            max=Math.max(max, imp.getStatistics().max);
        }
        
        imp.setSlice((int)nbSlices/2);
        imp.resetDisplayRange();
        thr=ip.getAutoThreshold();
        ip.setThreshold(thr, max,ImageProcessor.RED_LUT);
        imp.updateAndDraw();
                
        minSize=(int) Prefs.get("3D-OC_minSize.double", 10);
        maxSize=length;
        excludeOnEdges=Prefs.get("3D-OC_excludeOnEdges.boolean", true);
        showObj=Prefs.get("3D-OC_showObj.boolean", true);
        showSurf=Prefs.get("3D-OC_showSurf.boolean", true);
        showCentro=Prefs.get("3D-OC_showCentro.boolean", true);
        showCOM=Prefs.get("3D-OC_showCOM.boolean", true);
        showStat=Prefs.get("3D-OC_showStat.boolean", true);
        showSummary=Prefs.get("3D-OC_summary.boolean", true);
        
        showMaskedImg=Prefs.get("3D-OC-Options_showMaskedImg.boolean", true);
        closeImg=Prefs.get("3D-OC-Options_closeImg.boolean", false);
    
        
        redirectTo=Prefs.get("3D-OC-Options_redirectTo.string", "none");
        redirect=!this.redirectTo.equals("none") && WindowManager.getImage(this.redirectTo)!=null;
        
        if (redirect){
            ImagePlus imgRedir=WindowManager.getImage(this.redirectTo);
            if (!(imgRedir.getWidth()==this.width && imgRedir.getHeight()==this.height && imgRedir.getNSlices()==this.nbSlices) || imgRedir.getBitDepth()>16){
                redirect=false;
                showMaskedImg=false;
                IJ.log("Redirection canceled: images should have the same size and a depth of 8- or 16-bits.");
            }
            if (imgRedir.getTitle().equals(this.title)){
                redirect=false;
                showMaskedImg=false;
                IJ.log("Redirection canceled: both images have the same title.");
            }
        }
        
        if (!redirect){
            Prefs.set("3D-OC-Options_redirectTo.string", "none");
            Prefs.set("3D-OC-Options_showMaskedImg.boolean", false);
        }
        
        GenericDialog gd=new GenericDialog("3D Object Counter v2.0");
        
        gd.addSlider("Threshold", min, max, thr);
        gd.addSlider("Slice", 1, nbSlices, nbSlices/2);
        
        sliders=gd.getSliders();
        ((Scrollbar)sliders.elementAt(0)).addAdjustmentListener(this);
        ((Scrollbar)sliders.elementAt(1)).addAdjustmentListener(this);
        values = gd.getNumericFields();
        ((TextField)values.elementAt(0)).addTextListener(this);
        ((TextField)values.elementAt(1)).addTextListener(this);
        
        gd.addMessage("Size filter: ");
        gd.addNumericField("Min.",minSize, 0);
        gd.addNumericField("Max.", maxSize, 0);
        gd.addCheckbox("Exclude_objects_on_edges", excludeOnEdges);
        gd.addMessage("Maps to show: ");
        gd.addCheckbox("Objects", showObj);
        gd.addCheckbox("Surfaces", showSurf);
        gd.addCheckbox("Centroids", showCentro);
        gd.addCheckbox("Centres_of_masses", showCOM);
        gd.addMessage("Results tables to show: ");
        gd.addCheckbox("Statistics", showStat);
        gd.addCheckbox("Summary", showSummary);
        
        if (redirect) gd.addMessage("\nRedirection:\nImage used as a mask: "+this.title+"\nMeasures will be done on: "+this.redirectTo+(showMaskedImg?"\nMasked image will be shown":"")+".");
        if (closeImg) gd.addMessage("\nCaution:\nImage(s) will be closed during the processing\n(see 3D-OC options to change this setting).");
        
        gd.showDialog();
        
        if (gd.wasCanceled()){
            ip.resetThreshold();
            imp.updateAndDraw();
            return;
        }
        
        thr=(int) gd.getNextNumber();
        gd.getNextNumber();
        minSize=(int) gd.getNextNumber();
        maxSize=(int) gd.getNextNumber();
        excludeOnEdges=gd.getNextBoolean();
        showObj=gd.getNextBoolean();
        showSurf=gd.getNextBoolean();
        showCentro=gd.getNextBoolean();
        showCOM=gd.getNextBoolean();
        showStat=gd.getNextBoolean();
        showSummary=gd.getNextBoolean();

        Prefs.set("3D-OC_minSize.double", minSize);
        Prefs.set("3D-OC_excludeOnEdges.boolean", excludeOnEdges);
        Prefs.set("3D-OC_showObj.boolean", showObj);
        Prefs.set("3D-OC_showSurf.boolean", showSurf);
        Prefs.set("3D-OC_showCentro.boolean", showCentro);
        Prefs.set("3D-OC_showCOM.boolean", showCOM);
        Prefs.set("3D-OC_showStat.boolean", showStat);
        Prefs.set("3D-OC_summary.boolean", showSummary);
        if (!redirect) Prefs.set("3D-OC-Options_redirectTo.string", "none");
        
        ip.resetThreshold();
        imp.updateAndDraw();
        
        Counter3D OC=new Counter3D(imp, thr, minSize, maxSize, excludeOnEdges, redirect);
        
        dotSize=(int) Prefs.get("3D-OC-Options_dotSize.double", 5);
        fontSize=(int) Prefs.get("3D-OC-Options_fontSize.double", 10);
        showNb=Prefs.get("3D-OC-Options_showNb.boolean", true);
        whiteNb=Prefs.get("3D-OC-Options_whiteNb.boolean", true);
    
        if (showObj){OC.getObjMap(showNb, fontSize).show(); IJ.run("Fire");}
        if (showSurf){OC.getSurfPixMap(showNb, whiteNb, fontSize).show(); IJ.run("Fire");}
        if (showCentro){OC.getCentroidMap(showNb, whiteNb, dotSize, fontSize).show(); IJ.run("Fire");}
        if (showCOM){OC.getCentreOfMassMap(showNb, whiteNb, dotSize, fontSize).show(); IJ.run("Fire");}
        
        newRT=Prefs.get("3D-OC-Options_newRT.boolean", true);
            
        if (showStat) OC.showStatistics(newRT);
        
        if (showSummary) OC.showSummary();
}

    public void adjustmentValueChanged(AdjustmentEvent e) {
        thr=((Scrollbar)sliders.elementAt(0)).getValue();
        imp.setSlice(((Scrollbar)sliders.elementAt(1)).getValue());
        imp.resetDisplayRange();
        ip.setThreshold(thr, max,ImageProcessor.RED_LUT);
    }

    public void textValueChanged(TextEvent e) {
        if ((int) Tools.parseDouble(((TextField)values.elementAt(0)).getText())>max){
            ((Scrollbar)sliders.elementAt(0)).setValue((int) max);
            ((TextField)values.elementAt(0)).setText(""+((int) max));
        }
        if ((int) Tools.parseDouble(((TextField)values.elementAt(0)).getText())<min){
                ((Scrollbar)sliders.elementAt(0)).setValue((int) min);
                ((TextField)values.elementAt(0)).setText(""+((int)min));
        }
        
        if ((int) Tools.parseDouble(((TextField)values.elementAt(1)).getText())>nbSlices){
            ((Scrollbar)sliders.elementAt(1)).setValue(nbSlices);
            ((TextField)values.elementAt(1)).setText(""+nbSlices);
        }
        if ((int) Tools.parseDouble(((TextField)values.elementAt(1)).getText())<1){
                ((Scrollbar)sliders.elementAt(1)).setValue(1);
                ((TextField)values.elementAt(1)).setText("1");
        }
        thr=((Scrollbar)sliders.elementAt(0)).getValue();
        imp.setSlice(((Scrollbar)sliders.elementAt(1)).getValue());
        imp.resetDisplayRange();
        ip.setThreshold(thr, max, ImageProcessor.RED_LUT);
    }
}