import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import mpicbg.imglib.image.display.imagej.ImageJFunctions;

import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.NewImage;
import ij.measure.ResultsTable;

/**
 * This class displays the container contents in one single window
 * and offers features like the use of different LUTs.
 *
 */
public class SingleWindowDisplay extends ImageWindow implements Display, ItemListener {
	static final int WIN_WIDTH = 300;
	static final int WIN_HEIGHT = 240;
	static final int HIST_WIDTH = 256;
	static final int HIST_HEIGHT = 128;
	static final int BAR_HEIGHT = 12;
	static final int XMARGIN = 20;
	static final int YMARGIN = 10;

	protected Rectangle frame = new Rectangle(XMARGIN, YMARGIN, HIST_WIDTH, HIST_HEIGHT);
	protected List<Result.ImageResult> listOfImageResults = new ArrayList<Result.ImageResult>();

	// GUI elements
	JButton listButton, copyButton, logButton;
	JLabel valueLabel, countLabel;

	SingleWindowDisplay(){
		super(NewImage.createFloatImage("Single Window Display", WIN_WIDTH, WIN_HEIGHT, 1, NewImage.FILL_WHITE));
	}

	public void setup() {
		Panel imageSelectionPanel = new Panel();
		imageSelectionPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		JComboBox dropDownList = new JComboBox();
		for(Result.ImageResult r : listOfImageResults) {
			dropDownList.addItem(r);
		}
		dropDownList.addItemListener(this);
		imageSelectionPanel.add(dropDownList);

		Panel buttons = new Panel();
		buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));

		listButton = new JButton("List");
		//listButton.addActionListener(this);
		buttons.add(listButton);

		copyButton = new JButton("Copy");
		//copyButton.addActionListener(this);
		buttons.add(copyButton);

		logButton = new JButton("Log");
		//logButton.addActionListener(this);
		buttons.add(logButton);

		Panel valueAndCount = new Panel();
		valueAndCount.setLayout(new GridLayout(2, 1));
		valueLabel = new JLabel("                  "); //21
		valueLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
		valueAndCount.add(valueLabel);
		countLabel = new JLabel("                  ");
		countLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
		valueAndCount.add(countLabel);
		buttons.add(valueAndCount);

		remove(ic);
		add(imageSelectionPanel);
		add(ic);
		add(buttons);
		pack();
    }

	public void display(DataContainer container) {
		Iterator<Result> iterator = container.iterator();
		while (iterator.hasNext()){
			Result r = iterator.next();
			if (r instanceof Result.SimpleValueResult){
				Result.SimpleValueResult result = (Result.SimpleValueResult)r;
			} else if ( r instanceof Result.ImageResult) {
				Result.ImageResult result = (Result.ImageResult)r;
				listOfImageResults.add(result);
			}
		}

		setup();
		this.show();
	}

	public void mouseMoved(int x, int y) {
		if (valueLabel==null || countLabel==null)
			return;
		if ((frame!=null) && x >= frame.x && x <= (frame.x + frame.width)) {
			x = x - frame.x;
			if (x>255) x = 255;
			//int index = (int)(x*((double)histogram.length)/HIST_WIDTH);
			//valueLabel.setText("  Value: " + ResultsTable.d2s(cal.getCValue(stats.histMin+index*stats.binSize), digits));
			//countLabel.setText("  Count: " + histogram[index]);
			valueLabel.setText("  Value: ...");
			countLabel.setText("  Count: ...");
		} else {
			valueLabel.setText("");
			countLabel.setText("");
		}
	}

	public void itemStateChanged(ItemEvent e) {
		Result.ImageResult result = (Result.ImageResult)(e.getItem());
		ImagePlus imp = ImageJFunctions.displayAsVirtualStack( result.getData() );
		if (this.imp != imp){
			this.imp = imp;
			System.out.println("ouch!");
		}
	}
}