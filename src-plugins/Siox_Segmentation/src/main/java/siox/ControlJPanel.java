/**
 * Siox_Segmentation plug-in for ImageJ and Fiji.
 * 2009 Ignacio Arganda-Carreras, Johannes Schindelin, Stephan Saalfeld 
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

package siox;

import ij.ImagePlus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.siox.SioxSegmentator;
/**
 * This class implements the interactive buttons for the Siox segmentation GUI.
 *  
 * @author Ignacio Arganda-Carreras, Johannes Schindelin, Stephan Saalfeld
 *
 */
public class ControlJPanel extends JPanel
{
	/** Generated serial version UID */
	private static final long serialVersionUID = -1037100741242680537L;
	// GUI components
	final JPanel segJPanel=new JPanel(new GridBagLayout());
	final JPanel drbJPanel=new JPanel(new GridBagLayout());
	final JPanel resetJPanel = new JPanel(new GridBagLayout());
	
	final JLabel fgOrBgJLabel=new JLabel("Add Known ");
	final JButton segmentJButton = new JButton("Segment");
	
	final JRadioButton fgJRadioButton = new JRadioButton("Foreground");
	final JRadioButton bgJRadioButton = new JRadioButton("Background");
	
	final JSlider smoothness = new JSlider(0, 10, 6);
	
	final JCheckBox multipart=new JCheckBox("Allow multiple foreground components", false);
	final JLabel smoothJLabel = new JLabel("Smoothing:");
	
	final JRadioButton addJRadioButton=	new JRadioButton(SioxSegmentator.ADD_EDGE);
	final JRadioButton subJRadioButton=new JRadioButton(SioxSegmentator.SUB_EDGE);
	final JButton refineJButton = new JButton("Refine");
	
	
	final JSlider addThreshold=new JSlider(0, 100, 100);
	final JSlider subThreshold=new JSlider(0, 100, 0);
	
	final JButton resetJButton=new JButton("Reset");	
	final JButton createMaskJButton=new JButton("Create mask");
	final JButton saveSegmentatorJButton=new JButton("Save segmentator");
	
	/** Denotes some foreground being added. More foreground/background can be added or segmentation started. */
	final static int FG_ADDED_STATUS = 5;
	/** Denotes basic segmentation finished.  Allows detail refinement. */
	final static int SEGMENTED_STATUS = 6;
	
	/** One of the status constants, denotes current processing step. */
	int status = FG_ADDED_STATUS;
	
	
	//-----------------------------------------------------------------
	/**
	 * Constructs a control panel for interactive SIOX segmentation on given image.
	 */
	public ControlJPanel(ImagePlus imp)
	{
		super(new BorderLayout());
						
		final JPanel controlsBox=new JPanel(new GridBagLayout());
		
		segJPanel.setBorder(BorderFactory.createTitledBorder("1. Initial Segmentation"));
		drbJPanel.setBorder(BorderFactory.createTitledBorder("2. Detail Refinement Brush"));
		
		final ButtonGroup fgOrBgButtonGroup=new ButtonGroup();
		fgOrBgButtonGroup.add(fgJRadioButton);
		fgOrBgButtonGroup.add(bgJRadioButton);
		fgJRadioButton.setSelected(true);
		final String fgOrBgTooltip=
		  "Add Selection as Known Foreground/Background.";
		fgOrBgJLabel.setToolTipText(fgOrBgTooltip);
		fgJRadioButton.setToolTipText(fgOrBgTooltip);
		bgJRadioButton.setToolTipText(fgOrBgTooltip);
		segJPanel.add(fgOrBgJLabel, getGbc(0, 0, 1, false, false));
		segJPanel.add(fgJRadioButton, getGbc(1, 0, 1, false, false));
		segJPanel.add(bgJRadioButton, getGbc(2, 0, 1, false, false));
		
		multipart.setToolTipText("Use All Foreground Components of at Least a Fourth of the Biggest Connected Component.");
		smoothness.setToolTipText("Number of Smoothing Cycles in Postprocessing.");
		smoothness.setPaintTicks(true);
		smoothness.setMinorTickSpacing(1);
		smoothness.putClientProperty("JSlider.isFilled", Boolean.TRUE);
		segJPanel.add(multipart, getGbc(0, 1, 3, false, true));
		segJPanel.add(smoothJLabel, getGbc(0, 2, 3, false, true));
		segJPanel.add(smoothness, getGbc(1, 2, 2, false, true));
		final GridBagConstraints segGc = getGbc(0, 3, 3, false, false);
		segGc.anchor = GridBagConstraints.CENTER;
		segJPanel.add(segmentJButton, segGc);
						
		final ButtonGroup drbButtonGroup=new ButtonGroup();
		drbButtonGroup.add(addJRadioButton);
		drbButtonGroup.add(subJRadioButton);
		final ActionListener drbModeListener = new ActionListener()
		  {
			  public void actionPerformed(ActionEvent e)
			  {
				  addThreshold.setEnabled(addJRadioButton.isSelected());
				  subThreshold.setEnabled(subJRadioButton.isSelected());
			  }
		  };
		addJRadioButton.addActionListener(drbModeListener);
		subJRadioButton.addActionListener(drbModeListener);
		subJRadioButton.setSelected(true);		
		final String drbTooltip=
			"Additive or Subtractive Alpha Brush to Improve Edges or Highly Detailed Regions.";
		addJRadioButton.setToolTipText(drbTooltip);
		subJRadioButton.setToolTipText(drbTooltip);
		
		addThreshold.setToolTipText("Threshold Defining Subpixel Granularity for Additive Refinement Brush.");
		subThreshold.setToolTipText("Threshold Defining Subpixel Granularity for Substractive Refinement Brush.");
		addThreshold.setPaintTicks(true);
		addThreshold.setMinorTickSpacing(5);
		addThreshold.putClientProperty("JSlider.isFilled", Boolean.TRUE);
		subThreshold.setPaintTicks(true);
		subThreshold.setMinorTickSpacing(5);
		subThreshold.putClientProperty("JSlider.isFilled", Boolean.TRUE);
		drbJPanel.add(subJRadioButton, getGbc(0, 1, 1, false, false));
		drbJPanel.add(subThreshold, getGbc(1, 1, 2, false, true));
		drbJPanel.add(addJRadioButton, getGbc(0, 2, 1, false, false));
		drbJPanel.add(addThreshold, getGbc(1, 2, 2, false, true));
		drbJPanel.add(Box.createVerticalStrut(6), getGbc(0, 3, 1, false, false)); 
		drbJPanel.add(refineJButton, segGc);
				
		final String resetTooltip = "Reset displayed image";
		resetJButton.setToolTipText(resetTooltip);		
		resetJPanel.add(resetJButton, getGbc(0, 0, 1, false, false));				
		resetJPanel.add(createMaskJButton, getGbc(1, 0, 1, false, false));
		resetJPanel.add(saveSegmentatorJButton, getGbc(2, 0, 1, false, false));
		
		controlsBox.add(segJPanel, getGbc(0, 0, 1, false, true));
		controlsBox.add(drbJPanel, getGbc(0, 1, 1, false, true));
		controlsBox.add(resetJPanel, getGbc(0, 2, 1, false, true));

		add(controlsBox, BorderLayout.EAST);
		
		updateComponentEnabling();
		
	}// end ControlJPanel constructor
	
	/**
	 * Returns a gridbag constraint with the given parameters, standard
	 * L&amp;F insets and a west anchor.
	 */
	private static GridBagConstraints getGbc(int x, int y, int width,
											 boolean vFill, boolean hFill)
	{
		final GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(6, 6, 5, 5);
		c.anchor = GridBagConstraints.WEST;
		c.gridx = x;
		c.gridy = y;
		c.gridwidth = width;
		if (vFill) { // position may grow vertical
			c.fill = GridBagConstraints.VERTICAL;
			c.weighty = 1.0;
		}
		if (hFill) { // position may grow horizontally
			c.fill = hFill
			  ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
	  }
	  return c;
	}
	
	
	/** Enables/disables GUI components according to current status. */
	void updateComponentEnabling()
	{
		final Color onColor = UIManager.getColor("TitledBorder.titleColor");
		final Color offColor = UIManager.getColor("Label.disabledForeground");
		
		// panel for the SIOX segmentation step:
		final boolean addPhase = status==FG_ADDED_STATUS;
		((TitledBorder) segJPanel.getBorder()).setTitleColor(addPhase ? onColor : offColor);		
		
		smoothness.setEnabled(addPhase);
		smoothJLabel.setEnabled(addPhase);
		fgOrBgJLabel.setEnabled(addPhase);
		fgJRadioButton.setEnabled(addPhase);
		bgJRadioButton.setEnabled(addPhase);
		multipart.setEnabled(addPhase);
		segmentJButton.setEnabled(addPhase);
				
		segJPanel.repaint(); // update new border title color on screen
		
		// panel for the detail refinement step:
		final boolean drbPhase = (status == SEGMENTED_STATUS);
		((TitledBorder) drbJPanel.getBorder()).setTitleColor(drbPhase? onColor : offColor);
		addJRadioButton.setEnabled(drbPhase);
		subJRadioButton.setEnabled(drbPhase);
		if(addJRadioButton.isEnabled() == false)
			subJRadioButton.setSelected(true);
		refineJButton.setEnabled(drbPhase);
		addThreshold.setEnabled(drbPhase && addJRadioButton.isSelected());
		subThreshold.setEnabled(drbPhase && subJRadioButton.isSelected());
		drbJPanel.repaint(); // update new border title color on screen
		

	}// end updateComponentEnabling method
	

	
	
}// end class ControlJPanel
