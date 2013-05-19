import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.awt.datatransfer.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.PlugInFrame;
import ij.measure.*;
import ij.plugin.filter.Analyzer;


/** Bob Dougherty.  Plugin to reorder slides in a stack
Some code borrowed from Wayne Rasband's ROI Manager, Filler, and StackEditor.
Version 0 3/14/2004
Version 1 3/15/2004  Added Insert command.
Version 2 3/16/2004  Updated (by Wayne) for new type of masks in ImageJ 1.32c
Version 3 3/17/2004  Added Duplicate commands
Version 4 3/20/2004  Improved Insert type conversion
Version 5 3/22/2004  Added image scaling for insert.   Added message on insert
                     when multiple windows have the same name.  Simplified button labels.
                     Added Delete n.
Version 6 4/2/2004   Added Insert File.
Version 7 4/2/2004   Added Paste (system) and Insert URL.
Version 8 4/5/2005   Fixed bug in duplicate and revised delete and delete n.
Version 9 11/18/2005 Added Sort and Reverse commands
Version 10 11/18/2005 Added Sort by mean, Label Slices
*/
/*	License:
	Copyright (c) 2004, 2005, OptiNav, Inc.
	All rights reserved.

	Redistribution and use in source and binary forms, with or without
	modification, are permitted provided that the following conditions
	are met:

		Redistributions of source code must retain the above copyright
	notice, this list of conditions and the following disclaimer.
		Redistributions in binary form must reproduce the above copyright
	notice, this list of conditions and the following disclaimer in the
	documentation and/or other materials provided with the distribution.
		Neither the name of OptiNav, Inc. nor the names of its contributors
	may be used to endorse or promote products derived from this software
	without specific prior written permission.

	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
	"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
	LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
	A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
	EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
	PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
	PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
	LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
	NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

public class Stack_Sorter implements PlugIn {
	StackSorter ss;

	public void run(String arg) {
		ss = new StackSorter();
	}
}
/**
*/
class StackSorter extends PlugInFrame implements ActionListener, Measurements {

	Panel panel;
	static Frame instance;
	ImagePlus imp = null;
	ImageStack stack = null;
	int numSlices,slice;
	protected Label label1;
	boolean doScaling = true;
	static int QUIT = -1;
	static int CROP = 0;
	static int SHRINK_SOURCE = 1;
	static int EXPAND_DEST_BORDER = 2;
	static int RESIZE_DEST = 3;

	public StackSorter() {
		super("Stack Sorter");
		if (instance!=null) {
			instance.toFront();
			return;
		}
		if (IJ.versionLessThan("1.32c")){
			IJ.error("This version of Stack Sorter requires ImageJ 1.32c or later.");
			return;
		}
		instance = this;
		setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
		panel = new Panel();
		panel.setLayout(new GridLayout(18,1,5,5));
		addLabel("Operate on slice");
		addButton("<< First");
		addButton("<");
		addButton(">");
		addButton(">> Last");
		addButton("Duplicate");
		addButton("Duplicate n");
		addButton("Insert");
		addButton("Insert File");
		addButton("Insert URL");
		addButton("Paste (system)");
		addButton("Delete");
		addButton("Delete n");
		addLabel("Whole stack");
		addButton("Label Slices");
		addButton("Sort by Label");
		addButton("Sort by Mean");
		addButton("Reverse");
		add(panel);
		pack();
 		show();
	}
	void addButton(String label) {
		Button b = new Button(label);
		b.addActionListener(this);
		panel.add(b);
	}
	void addLabel(String label) {
		Label l = new Label(label,Label.CENTER);
		panel.add(l);
	}

	public void actionPerformed(ActionEvent e) {
		String label = e.getActionCommand();
		if (label==null)
			return;
		imp = WindowManager.getCurrentImage();
		if (imp == null){
			if(label.equals("Insert File")){
				String path = getFileName();
				if(path != null){
					ImagePlus imp = new Opener().openImage(path);
					if (imp!=null) imp.show();
				}
			} else if(label.equals("Insert URL")){
				ImagePlus imp = getImagePlusFromURL();
				if (imp!=null) imp.show();
			} else if(label.equals("Paste (system)")){
				ImagePlus imp = getSystemClipboardImage();
				if (imp!=null) imp.show();
			} else
				IJ.noImage();
		} else {
			numSlices = imp.getStackSize();
			slice = imp.getCurrentSlice();
			if ((numSlices < 2)&&(!label.equals("Duplicate"))&&
				(!label.equals("Duplicate n"))&&(!label.equals("Insert"))&&
				(!label.equals("Insert File"))&&(!label.equals("Paste (system)"))&&
				(!label.equals("Insert URL"))){
					IJ.showMessage("Stack required");
			} else {
				stack = imp.getStack();
				if (label.equals("<< First"))
					first();
				else if (label.equals("<"))
					fwd();
				else if (label.equals(">"))
					bkwd();
				else if (label.equals(">> Last"))
					last();
				else if (label.equals("Duplicate"))
					dup();
				else if (label.equals("Duplicate n"))
					dupN();
				else if (label.equals("Insert"))
					ins();
				else if (label.equals("Insert File"))
					insf();
				else if (label.equals("Insert URL"))
					insURL();
				else if (label.equals("Paste (system)"))
					pasteSys();
				else if (label.equals("Delete"))
					del();
				else if (label.equals("Delete n"))
					delN();
				else if (label.equals("Sort by Label"))
					sortByLabel();
				else if (label.equals("Sort by Mean"))
					sortByMean();
				else if (label.equals("Label Slices"))
					labelSlices();
				else if (label.equals("Reverse"))
					reverse();
			}
		}
	}
	void labelSlices(){
		for(int iSlice = 1; iSlice <= numSlices; iSlice++){
			String title = ""+iSlice;
			if(iSlice < 10){
				title = "00000"+title;
			}else if(iSlice < 100){
				title = "0000"+title;
			}else if(iSlice < 1000){
				title = "000"+title;
			}else if(iSlice < 10000){
				title = "00"+title;
			}else if(iSlice < 100000){
				title = "0"+title;
			}
			stack.setSliceLabel(title,iSlice);
		}
		imp.setStack(null,stack);
		imp.updateAndDraw();
	}
	void reverse(){
		for(int iSlice = 1; iSlice < numSlices; iSlice++){
			stack.addSlice(stack.getSliceLabel(1), stack.getProcessor(1),numSlices - iSlice + 1);
			stack.deleteSlice(1);
		}
		imp.setStack(null,stack);
		imp.updateAndDraw();
	}
	void sortByLabel(){
		boolean swapped = false;
		for(int pass = 0; pass < numSlices; pass++){
			for(int iSlice = 1; iSlice < (numSlices-pass); iSlice++){
				int comp = stack.getSliceLabel(iSlice).compareTo(stack.getSliceLabel(iSlice+1));
				if(comp > 0){
					swapped = true;
					stack.addSlice(stack.getSliceLabel(iSlice), stack.getProcessor(iSlice),iSlice+1);
					stack.deleteSlice(iSlice);
				}
			}
			if(!swapped)break;
		}
		imp.setStack(null,stack);
		imp.updateAndDraw();
	}
	void sortByMean(){
		float[] y = getZAxisProfile();
		if(y == null)return;
		boolean swapped = false;
		for(int pass = 0; pass < numSlices; pass++){
			for(int iSlice = 1; iSlice < (numSlices-pass); iSlice++){
				float comp = y[iSlice - 1] - y[iSlice];
				if(comp > 0){
					swapped = true;
					stack.addSlice(stack.getSliceLabel(iSlice), stack.getProcessor(iSlice),iSlice+1);
					stack.deleteSlice(iSlice);
					float temp = y[iSlice - 1];
					y[iSlice - 1] = y[iSlice];
					y[iSlice] = temp;
				}
			}
			if(!swapped)break;
		}
		imp.setStack(null,stack);
		imp.updateAndDraw();
	}
	void first(){
		if(slice!=1){
			stack.addSlice(stack.getSliceLabel(slice), stack.getProcessor(slice),0);
			stack.deleteSlice(slice+1);
			imp.setStack(null,stack);
			imp.setSlice(1);
			imp.updateAndDraw();
		}
	}
	void bkwd(){
		if(slice!=numSlices){
			stack.addSlice(stack.getSliceLabel(slice), stack.getProcessor(slice),slice+1);
			stack.deleteSlice(slice);
			imp.setStack(null,stack);
			imp.setSlice(slice+1);
			imp.updateAndDraw();
		}
	}
	void fwd(){
		if(slice!=1){
			stack.addSlice(stack.getSliceLabel(slice), stack.getProcessor(slice),slice-2);
			stack.deleteSlice(slice+1);
			imp.setStack(null,stack);
			imp.setSlice(slice-1);
			imp.updateAndDraw();
		}
	}
	void last(){
		if(slice!=numSlices){
			stack.addSlice(stack.getSliceLabel(slice), stack.getProcessor(slice),numSlices);
			stack.deleteSlice(slice);
			imp.setStack(null,stack);
			imp.setSlice(numSlices);
			imp.updateAndDraw();
		}
	}
	void dup(){
		stack.addSlice(stack.getSliceLabel(slice), stack.getProcessor(slice).duplicate(),slice);
		imp.setStack(null,stack);
		imp.setSlice(slice+1);
		imp.updateAndDraw();
	}
	void dupN(){
		int n = (int)IJ.getNumber("Number of times to duplicate the slice",1);
		for (int i = 0; i < n;i++)dup();
	}
	void ins(){
		ImagePlus imp1 = showDialog(imp.getTitle());
		insImp1(imp1);
	}
	void insf(){
		String path = getFileName();
		if(path != null){
			ImagePlus imp1 = new Opener().openImage(path);
			insImp1(imp1);
		}
	}
	void insURL(){
		ImagePlus imp1 = getImagePlusFromURL();
		insImp1(imp1);
	}
	void pasteSys(){
		ImagePlus imp1 = getSystemClipboardImage();
				insImp1(imp1);
	}
	ImagePlus getImagePlusFromURL(){
		ImagePlus imp1 = null;
		String url = IJ.getString("URL (or path) for an image","http://rsb.info.nih.gov/ij/images/clown.gif");
		if (!url.equals(""))
			imp1 = new ImagePlus(url);
		return imp1;
	}
	ImagePlus getSystemClipboardImage(){
		ImagePlus imp1 = null;
		try {
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			Clipboard cb = toolkit.getSystemClipboard();
			Transferable trns = cb.getContents(this);
			String name = cb.getName();
				if(trns==null){
					IJ.showMessage("The system clipboard is empty.");
					return null;
				}
				if(!trns.isDataFlavorSupported(DataFlavor.imageFlavor)){
					IJ.showMessage("The system clipboard has no usable image.");
					return null;
				}
				Image img =	(Image)trns.getTransferData(DataFlavor.imageFlavor);
				if (img!=null){
					imp1 =new ImagePlus(name, img);
				}
			} catch(Exception e) {
				IJ.showMessage("Exception with system clipboard.");
		}
		return imp1;
	}
	String getFileName(){
		Frame f = new Frame();
		FileDialog fd = new FileDialog(f, "Image file", FileDialog.LOAD);
		fd.setVisible(true);
		String path = fd.getDirectory();
		String filename = fd.getFile();
		if ((path == null) || (filename == null))
			return null;
		return path+filename;
	}
	void insImp1(ImagePlus imp1){
		if(imp1==null){
			return;
		}
		ImageStack stack1 = imp1.getStack();
		int numSlices1 = imp1.getStackSize();
		int slice1 = imp1.getCurrentSlice();
		int maxInsert = numSlices1 - slice1 + 1;
		int nInsert = 1;
		if(maxInsert>1){
			nInsert = maxInsert+1;
			while((nInsert > maxInsert)||(nInsert < 0 )){
				nInsert = (int)IJ.getNumber("Number of slices to insert (max "+maxInsert+")",1);
				if(nInsert==IJ.CANCELED)return;
				if((nInsert > maxInsert)||(nInsert < 0 ))IJ.beep();
			}
		}
		if(nInsert==0)return;
		int w = imp.getWidth();
		int h = imp.getHeight();
      	Rectangle r = imp1.getProcessor().getRoi();
      	int w1 = r.width;
      	int h1 = r.height;
      	int option = CROP;
      	if((w1>w)||(h1>h)){
			option = showOptionDialog(w1, h1, w, h);
			if(option == QUIT)
				return;
			else if(option == EXPAND_DEST_BORDER){
				expandDestBorder(w1, h1, w, h);
			}
			else if(option == RESIZE_DEST){
				resizeDest(w1, h1, w, h);
			}
		}
		for (int i = 0; i < nInsert; i++){
			ImageProcessor ip0 = stack1.getProcessor(slice1+i).duplicate();
			ImageProcessor ip1temp = null;
			if(option == SHRINK_SOURCE)
				ip1temp = shrink(ip0,w1,h1,w,h);
			else
				ip1temp = ip0;
			insert(ip1temp,imp1);
		}
	}
	void expandDestBorder(int w1,int h1,int w,int h){
		int wFinal,hFinal;
		if(w1 > w)
			wFinal = w1;
		else
			wFinal = w;
		if(h1 > h)
			hFinal = h1;
		else
			hFinal = h;

		ImageStack newStack = new ImageStack(wFinal,hFinal);
		for (int i = 1; i <= numSlices; i++){
			ImageProcessor ipi2 = stack.getProcessor(i).duplicate();
			ImageProcessor ipi = ipi2.resize(wFinal,hFinal);
        	ipi.setColor(Toolbar.getBackgroundColor());
        	ipi.fill();
			ipi.copyBits(ipi2,(wFinal - w)/2,(hFinal-h)/2,Blitter.COPY);
			ipi.setColor(Toolbar.getForegroundColor());
			newStack.addSlice(stack.getSliceLabel(i),ipi);
		}
		String title = imp.getShortTitle();
		stack = newStack;
		Calibration cal = imp.getCalibration();
		imp.hide();
		imp = new ImagePlus(title,stack);
		imp.setSlice(slice);
		imp.setCalibration(cal);
		imp.show();
	}
	void resizeDest(int w1,int h1,int w,int h){
		int wFinal,hFinal;
		if((((double)w1)/w) > (((double)h1)/h)){
			//scale factor = w1/w
			wFinal = w1;
			hFinal = h*w1/w;
		}else{
			//scale factor = h1/h
			wFinal = w*h1/h;
			hFinal = h1;
		}
		IJ.run("Size...", "width="+wFinal+" height="+hFinal+" constrain interpolate");
		imp = WindowManager.getCurrentImage();
	}
	ImageProcessor shrink(ImageProcessor ip1, int w1, int h1, int w, int h){
		if(ip1==null)return null;
		int wFinal,hFinal;
		if((((double)w)/w1) < (((double)h)/h1)){
			//scale factor = w/w1
			wFinal = w;
			hFinal = w*h1/w1;
		}else{
			//scale factor = h/h1
			wFinal = w1*h/h1;
			hFinal = h;
		}
		return ip1.resize(wFinal,hFinal);
	}
	void insert(ImageProcessor ip1temp, ImagePlus imp1){
		IJ.run("Add Slice");
		imp.updateAndDraw();
		stack = imp.getStack();
		ImageProcessor ip = stack.getProcessor(slice+1);
        Roi roi = imp1.getRoi();
		ImageProcessor ip1 = convertType(ip1temp,ip,doScaling);
		ip.setColor(Toolbar.getBackgroundColor());
		ip.fill();
        ip1.setRoi(roi);
      	Rectangle r = ip1.getRoi();
		clearOutside(ip1,imp1, roi, r);
		if((ip.getWidth()==ip1.getWidth())&&(ip.getHeight()==ip1.getHeight()))
			ip.copyBits(ip1,0,0,Blitter.COPY);
		else{
			int x1Cent = r.x + r.width/2;
			int y1Cent = r.y + r.height/2;
			int xCent = ip.getWidth()/2;
			int yCent = ip.getHeight()/2;
			ip.copyBits(ip1,xCent-x1Cent,yCent-y1Cent,Blitter.COPY);
		}
		ip.setColor(Toolbar.getForegroundColor());
		imp.setStack(null,stack);
		imp.setSlice(++slice);
		imp.updateAndDraw();
	}
	//Convert ip1 to be the same type as ip
	ImageProcessor convertType(ImageProcessor ip1,ImageProcessor ip,boolean doScaling){
		ImageProcessor result = null;
			TypeConverter tc = new TypeConverter(ip1,doScaling);
		if(ip instanceof ByteProcessor)
			result = tc.convertToByte();
		else if(ip instanceof FloatProcessor)
			result = ip1.convertToFloat();
		else if(ip instanceof ColorProcessor)
			result = tc.convertToRGB();
		else if(ip instanceof ShortProcessor)
			result = tc.convertToShort();
		else
			result = ip1;
		return result;
	}
	void del() {
		if (!imp.lock())
			return;
		ImageStack stack = imp.getStack();
		int n = imp.getCurrentSlice();
		stack.deleteSlice(n);
		if (stack.getSize()==1) {
			imp.setProcessor(null, stack.getProcessor(1));
 			new ImageWindow(imp);
		}
		imp.setStack(null, stack);
 		numSlices--;
		if (n>numSlices)
			imp.setSlice(numSlices);
		else
			imp.setSlice(n);
        imp.unlock();
    }
	void delN(){
		int maxDelete;
		if(slice == 1)
			maxDelete = numSlices - 1;
		else
			maxDelete = numSlices - slice + 1;
		int nDelete = 1;
		if(maxDelete>1){
			nDelete = maxDelete+1;
			while((nDelete > maxDelete)||(nDelete < 0 )){
				nDelete = (int)IJ.getNumber("Number of slices to delete (max "+maxDelete+")",1);
				if(nDelete==IJ.CANCELED)return;
				if((nDelete > maxDelete)||(nDelete < 0 ))IJ.beep();
			}
		}
		if(nDelete==0)return;
		for(int i = 0; i < nDelete; i++){
			del();
		}

	}
    public void clearOutside(ImageProcessor ip,ImagePlus imp, Roi roi, Rectangle r) {
        if (isLineSelection(roi)) {
            return;
        }
        ImageProcessor mask = makeMask(ip, r, imp);
        ip.setColor(Toolbar.getBackgroundColor());
        ip.snapshot();
        ip.fill();
        ip.reset(mask);
        int width = ip.getWidth();
        int height = ip.getHeight();
        ip.setRoi(0, 0, r.x, height);
        ip.fill();
        ip.setRoi(r.x, 0, r.width, r.y);
        ip.fill();
        ip.setRoi(r.x, r.y+r.height, r.width, height-(r.y+r.height));
        ip.fill();
        ip.setRoi(r.x+r.width, 0, width-(r.x+r.width), height);
        ip.fill();
        ip.resetRoi();
    }
    boolean isLineSelection(Roi roi) {
        return roi!=null && roi.getType()>=Roi.LINE && roi.getType()<=Roi.FREELINE;
    }

    ImageProcessor makeMask(ImageProcessor ip, Rectangle r, ImagePlus imp) {
 		ImageProcessor mask = imp.getMask();
 		if (mask==null) {
 			mask = new ByteProcessor(r.width, r.height);
 			mask.invert();
 		} else {
 			// duplicate mask (needed because getMask caches masks)
 			mask = mask.duplicate();
 		}
 		mask.invert();
		return mask;
 	}

	ImagePlus showDialog(String title) {
		int[] wList = WindowManager.getIDList();
		if (wList==null) {
			IJ.noImage();
			return null;
		}

		String[] titles = new String[wList.length];
		int n = titles.length;
		if (n == 1) {
			IJ.showMessage("No image to insert.  Open a second image first, or use Insert File or Paste.");
			return null;
		}

		for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			titles[i] = imp!=null?imp.getTitle():"";
		}
		String[] titles1 = new String[n-1];
		int[] wList1 = new int[n-1];
		int ind = 0;
		for (int i = 0; i < n; i++){
			if(!title.equals(titles[i])){
				titles1[ind] = titles[i];
				wList1[ind++] = wList[i];
			}
		}
		if(ind != (n-1)){
			IJ.showMessage("The window titles need to be unique for the insert operation to work.");
			return null;
		}
		if(titles1.length==1)return WindowManager.getImage(wList1[0]);
		GenericDialog gd = new GenericDialog("Choose Image");
		gd.addChoice("Image to insert in stack:", titles1, titles1[0]);
		gd.showDialog();
		if (gd.wasCanceled())
			return null;
		int index = gd.getNextChoiceIndex();
		return WindowManager.getImage(wList1[index]);
	}
	int showOptionDialog(int w1, int h1,int w, int h) {
		String[] options = new String[]{"inserting a cropped image",
										"inserting a reduced size image",
										"resizing the destination stack by adding border pixels",
										"resizing the destination stack by scaling it up"};
		GenericDialog gd = new GenericDialog("Options");
		gd.addMessage("The image does not fit. Source: "+w1+"x"+h1+" pixels. Dest.: "+w+"x"+h+" pixels.");
		gd.addChoice("Adjust by", options, options[0]);
		gd.showDialog();
		if (gd.wasCanceled())
			return QUIT;
		return gd.getNextChoiceIndex();
	}
	public void windowClosing(WindowEvent e) {
		super.windowClosing(e);
	}
	public void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID()==WindowEvent.WINDOW_CLOSING) {
			instance = null;
		}
	}
	//Adapted from ImageJ code by Wayne Rasband
	float[] getZAxisProfile(){
		Roi roi = imp.getRoi();
		if (roi!=null && roi.isLine()) {
    		IJ.error("ZAxisProfiler", "This command does not work with line selections.");
    		return null;
		}
		ImageProcessor ip = imp.getProcessor();
		double minThreshold = ip.getMinThreshold();
		double maxThreshold = ip.getMaxThreshold();
		return getZAxisProfile(roi, minThreshold, maxThreshold);
	}
	//Adapted from ImageJ code by Wayne Rasband
    float[] getZAxisProfile(Roi roi, double minThreshold, double maxThreshold) {
        ImageStack stack = imp.getStack();
        int size = stack.getSize();
        float[] values = new float[size];
        Calibration cal = imp.getCalibration();
        Analyzer analyzer = new Analyzer(imp);
        int measurements = analyzer.getMeasurements();
        boolean showResults = measurements!=0 && measurements!=LIMIT;
        boolean showingLabels = (measurements&LABELS)!=0;
        measurements |= MEAN;
        if (showResults) {
            if (!analyzer.resetCounter())
                return null;
        }
        int current = imp.getCurrentSlice();
        for (int i=1; i<=size; i++) {
            if (showingLabels) imp.setSlice(i);
            ImageProcessor ip = stack.getProcessor(i);
            if (minThreshold!=ImageProcessor.NO_THRESHOLD)
                ip.setThreshold(minThreshold,maxThreshold,ImageProcessor.NO_LUT_UPDATE);
            ip.setRoi(roi);
            ImageStatistics stats = ImageStatistics.getStatistics(ip, measurements, cal);
            analyzer.saveResults(stats, roi);
            if (showResults)
                analyzer.displayResults();
            values[i-1] = (float)stats.mean;
        }
        if (showingLabels) imp.setSlice(current);
        return values;
    }
}