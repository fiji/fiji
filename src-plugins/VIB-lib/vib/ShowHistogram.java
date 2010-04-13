package vib;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.awt.datatransfer.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.*;
import ij.plugin.filter.Analyzer;
import ij.text.TextWindow;

/** This class is an extended ImageWindow that displays histograms. */
public class ShowHistogram extends ImageWindow implements Measurements, ActionListener, ClipboardOwner {
	static final int WIN_WIDTH = 300;
	static final int WIN_HEIGHT = 240;
	static final int HIST_WIDTH = 256;
	static final int HIST_HEIGHT = 128;
	static final int BAR_HEIGHT = 12;
	static final int XMARGIN = 20;
	static final int YMARGIN = 10;

	protected double[] histogram;
	protected double histMin,binSize;
	protected Rectangle frame = null;
	protected Button list, save, copy,log;
	protected Label value, count;
	protected static String defaultDirectory = null;
	protected int decimalPlaces;
	protected int digits;
	protected int newMinCount;
	protected int newMaxCount;
	protected int plotScale = 1;
	protected boolean logScale;
	protected int yMax;
	public static int nBins = 256;

	/** Displays a histogram using the title "Histogram of ImageName". */
	public ShowHistogram(double[] histogram, double histMin, double binSize) {
		super(NewImage.createByteImage("Histogram", WIN_WIDTH, WIN_HEIGHT, 1, NewImage.FILL_WHITE));
		this.histogram = histogram;
		this.histMin = histMin;
		this.binSize = binSize;
		showHistogram(0.0, 0.0);
	}

	public ShowHistogram(long[] histogram, int histMin) {
		super(NewImage.createByteImage("Histogram", WIN_WIDTH, WIN_HEIGHT, 1, NewImage.FILL_WHITE));
		this.histogram = new double[histogram.length];
		for(int i=0;i<histogram.length;i++)
			this.histogram[i] = histogram[i];
		this.histMin = histMin;
		this.binSize = 1;
		showHistogram(0.0, 0.0);
	}

	/** Draws the histogram using the specified title and ImageStatistics. */
	public void showHistogram(double min, double max) {
		setup();
		drawHistogram(this.imp.getProcessor());
		this.imp.updateAndDraw();
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
		log = new Button("Log");
		log.addActionListener(this);
		buttons.add(log);
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

	public void mouseMoved(int x, int y) {
		if (value==null || count==null)
			return;
		if ((frame!=null)  && x>=frame.x && x<=(frame.x+frame.width)) {
			x = x - frame.x;
			if (x>255) x = 255;
			int index = (int)(x*((double)histogram.length)/HIST_WIDTH);
			value.setText("  Value: " + IJ.d2s(histMin+index*binSize, digits));
			count.setText("  Count: " + histogram[index]);
		} else {
			value.setText("");
			count.setText("");
		}
	}

	protected void drawHistogram(ImageProcessor ip) {
		int x, y;
		double minCount2 = histogram[0];
		double maxCount2 = histogram[0];
		int mode2 = 0;
		int saveModalCount;

		ip.setColor(Color.black);
		ip.setLineWidth(1);
		decimalPlaces = Analyzer.getPrecision();
		digits = binSize!=1.0?decimalPlaces:0;
		for (int i = 1; i<histogram.length; i++)
			if (histogram[i] > maxCount2) {
				maxCount2 = histogram[i];
				mode2 = i;
			} else if (histogram[i] < minCount2)
				minCount2 = histogram[i];
		drawPlot(minCount2, maxCount2, ip);
		x = XMARGIN + 1;
		y = YMARGIN + HIST_HEIGHT + 2;
		y += BAR_HEIGHT+15;
		drawText(ip, x, y);
	}


	void drawPlot(double minCount, double maxCount, ImageProcessor ip) {
		if (maxCount <= minCount)
			maxCount = minCount + 1;
		newMinCount = (int)minCount;
		newMaxCount = (int)maxCount;
		System.err.println("min/max: "+minCount+"/"+maxCount);
		frame = new Rectangle(XMARGIN, YMARGIN, HIST_WIDTH, HIST_HEIGHT);
		ip.drawRect(frame.x-1, frame.y, frame.width+2, frame.height+1);
		int index, y;
		for (int i = 0; i<HIST_WIDTH; i++) {
			index = (int)(i*(double)histogram.length/HIST_WIDTH);
			y = (int)(HIST_HEIGHT * (histogram[index] - minCount)
					/ (maxCount - minCount));
			if (y>HIST_HEIGHT)
				y = HIST_HEIGHT;
			ip.drawLine(i+XMARGIN, YMARGIN+HIST_HEIGHT, i+XMARGIN, YMARGIN+HIST_HEIGHT-y);
		}
	}

	void drawLogPlot (int maxCount, ImageProcessor ip) {
		frame = new Rectangle(XMARGIN, YMARGIN, HIST_WIDTH, HIST_HEIGHT);
		ip.drawRect(frame.x-1, frame.y, frame.width+2, frame.height+1);
		double max = Math.log(maxCount);
		ip.setColor(Color.gray);
		int index, y;
		for (int i = 0; i<HIST_WIDTH; i++) {
			index = (int)(i*(double)histogram.length/HIST_WIDTH);
			y = histogram[index]==0?0:(int)(HIST_HEIGHT*Math.log(histogram[index])/max);
			if (y>HIST_HEIGHT)
				y = HIST_HEIGHT;
			ip.drawLine(i+XMARGIN, YMARGIN+HIST_HEIGHT, i+XMARGIN, YMARGIN+HIST_HEIGHT-y);
		}
		ip.setColor(Color.black);
	}

	void drawText(ImageProcessor ip, int x, int y) {
		ip.setFont(new Font("SansSerif",Font.PLAIN,12));
		ip.setAntialiasedText(true);
		double hmin = histMin;
		double hmax = histMin+binSize*(histogram.length-1);
		ip.drawString(d2s(hmin), x - 4, y);
		ip.drawString(d2s(hmax), x + HIST_WIDTH - getWidth(hmax, ip) + 10, y);

		binSize = Math.abs(binSize);
		boolean showBins = binSize!=1.0;
		int col1 = XMARGIN + 5;
		int col2 = XMARGIN + HIST_WIDTH/2;
		int row1 = y+25;
		if (showBins) row1 -= 8;
		int row2 = row1 + 15;
		int row3 = row2 + 15;
		int row4 = row3 + 15;
		ip.drawString("Count: " + histogram.length, col1, row1);

		double total=0,mean=0,var=0,min=Double.MAX_VALUE,max=Double.MIN_VALUE;
		for(int i=0;i<histogram.length;i++) {
			mean+=i*histogram[i];
			var+=i*i*histogram[i];
			total+=histogram[i];
			if(min>histogram[i]) min=histogram[i];
			if(max<histogram[i]) max=histogram[i];
		}
		if(total>0) {
			mean/=total;
			var/=total;
		}
		double stdDev=Math.sqrt(var-mean*mean)*binSize;
		mean=histMin+mean*binSize;
		ip.drawString("Mean: " + d2s(mean), col1, row2);
		ip.drawString("StdDev: " + d2s(stdDev), col1, row3);
		ip.drawString("Min: " + d2s(min), col2, row1);
		ip.drawString("Max: " + d2s(max), col2, row2);

		if (showBins) {
			ip.drawString("Bins: " + d2s(histogram.length), col1, row4);
			ip.drawString("Bin Width: " + d2s(binSize), col2, row4);
		}
	}

	String d2s(double d) {
		if (d==Double.MAX_VALUE||d==-Double.MAX_VALUE)
			return "0";
		else if (Double.isNaN(d))
			return("NaN");
		else if (Double.isInfinite(d))
			return("Infinity");
		else if ((int)d==d)
			return IJ.d2s(d,0);
		else
			return IJ.d2s(d,decimalPlaces);
	}

	int getWidth(double d, ImageProcessor ip) {
		return ip.getStringWidth(d2s(d));
	}

	protected void showList() {
		StringBuffer sb = new StringBuffer();
		String vheading = binSize==1.0?"value":"bin start";
		for (int i=0; i<histogram.length; i++)
			sb.append(IJ.d2s(histMin+i*binSize, digits)+"\t"+histogram[i]+"\n");
		TextWindow tw = new TextWindow(getTitle(), vheading+"\tcount", sb.toString(), 200, 400);
	}

	protected void copyToClipboard() {
		Clipboard systemClipboard = null;
		try {systemClipboard = getToolkit().getSystemClipboard();}
		catch (Exception e) {systemClipboard = null; }
		if (systemClipboard==null)
			{IJ.error("Unable to copy to Clipboard."); return;}
		IJ.showStatus("Copying histogram values...");
		CharArrayWriter aw = new CharArrayWriter(histogram.length*4);
		PrintWriter pw = new PrintWriter(aw);
		for (int i=0; i<histogram.length; i++)
			pw.print(IJ.d2s(histMin+i*binSize, digits)+"\t"+histogram[i]+"\n");
		String text = aw.toString();
		pw.close();
		StringSelection contents = new StringSelection(text);
		systemClipboard.setContents(contents, this);
		IJ.showStatus(text.length() + " characters copied to Clipboard");
	}

	void replot() {
		logScale = !logScale;
		ImageProcessor ip = this.imp.getProcessor();
		frame = new Rectangle(XMARGIN, YMARGIN, HIST_WIDTH, HIST_HEIGHT);
		ip.setColor(Color.white);
		ip.setRoi(frame.x-1, frame.y, frame.width+2, frame.height);
		ip.fill();
		ip.resetRoi();
		ip.setColor(Color.black);
		if (logScale) {
			drawLogPlot(newMaxCount, ip);
			drawPlot(newMinCount, newMaxCount, ip);
		} else
			drawPlot(newMinCount, newMaxCount, ip);
		this.imp.updateAndDraw();
	}

	/*
	void rescale() {
		Graphics g = img.getGraphics();
		plotScale *= 2;
		if ((newMaxCount/plotScale)<50) {
			plotScale = 1;
			frame = new Rectangle(XMARGIN, YMARGIN, HIST_WIDTH, HIST_HEIGHT);
			g.setColor(Color.white);
			g.fillRect(frame.x, frame.y, frame.width, frame.height);
			g.setColor(Color.black);
		}
		drawPlot(newMaxCount/plotScale, g);
		//ImageProcessor ip = new ColorProcessor(img);
		//this.imp.setProcessor(null, ip);
		this.imp.setImage(img);
	}
	*/

	public void actionPerformed(ActionEvent e) {
		Object b = e.getSource();
		if (b==list)
			showList();
		else if (b==copy)
			copyToClipboard();
		else if (b==log)
			replot();
	}

	public void lostOwnership(Clipboard clipboard, Transferable contents) {}

	public double[] getHistogram() {
		return histogram;
	}

}
