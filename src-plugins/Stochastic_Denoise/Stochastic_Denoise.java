
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;

import ij.plugin.PlugIn;

import ij.process.ImageProcessor;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;

import mpicbg.imglib.type.numeric.RealType;

/**
 * Stochastic Image Denoising Plugin
 *
 * Implementation of the stochastic image denoising algotithm as proposed by:
 *
 *   Francisco Estrada, David Fleet, Allan Jepson
 *   Stochastic Image Denoising
 *   British Mashine Vision Conference 2009
 *
 * @author Jan Funke <jan.funke@inf.tu-dresden.de>
 */



/**
 * Plugin interface to the stochastic image denoising algorithm.
 *
 * @author Jan Funke <jan.funke@inf.tu-dresden.de>
 * @version 0.1
 */
public class Stochastic_Denoise<T extends RealType<T>> implements PlugIn {

	// the image to process
	private Image<T> image;

	// the ImagePlus version of it
	private ImagePlus imp;

	// the denoised image
	private Image<T> denoised;

	// the ImagePlus version of it
	private ImagePlus dns;

	// image dimensions
	private int[] dimensions;

	// the algorithm implementation
	private StochasticDenoise<T> stochasticDenoise;

	private final int SIGMA_MIN  = 0;
	private final int SIGMA_MAX  = 50;
	private final int SIGMA_INIT = 15;

	private final int PATHS_MIN  = 0;
	private final int PATHS_MAX  = 100;
	private final int PATHS_INIT = 20;

	// number of random walks per pixel
	private int numSamples = PATHS_INIT;

	// minimal probability for a path
	private float minProb = 1e-6f;

	// parameter of the probability function
	private float sigma = SIGMA_INIT;

	private JPanel       configPanel;

	private JSlider sigmaSlider;
	private JPanel  sigmaPanel;
	private JSlider pathsSlider;
	private JPanel  pathsPanel;
	private JButton applyButton;

	@SuppressWarnings("serial")
	private class ConfigWindow extends JFrame {

		// action listener
		private ActionListener actionListener = new ActionListener() {
	
			public void actionPerformed(final ActionEvent e) {

				if(e.getSource() == applyButton) {
						try {
							applyButton.setEnabled(false);

							// perform the denoising
							stochasticDenoise.process(image, denoised);

							// show the result
							dns.show();
							dns.updateAndDraw();

						} catch(Exception ex) {
							ex.printStackTrace();
						} finally {
							applyButton.setEnabled(true);
						}
				}
			}
		};
	
		// change listener for sliders
		private ChangeListener changeListener = new ChangeListener() {
	
			public void stateChanged(final ChangeEvent e) {
				JSlider source = (JSlider)e.getSource();
				if(e.getSource() == sigmaSlider)
					sigma = source.getValue();
				if(e.getSource() == pathsSlider)
					numSamples = source.getValue();
				stochasticDenoise.setParameters(numSamples, minProb, sigma);
				IJ.log("Parameters: numPaths=" + numSamples + ", sigma=" + sigma);
			}
		};

		ConfigWindow() {

			configPanel = new JPanel();

			applyButton  = new JButton("Denoise");

			//GridBagLayout sigmaLayout = new GridBagLayout();
			//GridBagConstraints sigmaConstraints = new GridBagConstraints();
			//sigmaConstraints.anchor = GridBagConstraints.NORTHWEST;
			//sigmaConstraints.fill = GridBagConstraints.HORIZONTAL;
			//sigmaConstraints.gridwidth = 1;
			//sigmaConstraints.gridheight = 1;
			//sigmaConstraints.gridx = 0;
			//sigmaConstraints.gridy = 0;
			//sigmaConstraints.insets = new Insets(5, 5, 6, 6);
			//sigmaPanel.setLayout(sigmaLayout);
			
			//applyConstraints.gridy++;
			//applyPanel.add(overlayButton, applyConstraints);
			//applyConstraints.gridy++;

			sigmaSlider = new JSlider(JSlider.HORIZONTAL, SIGMA_MIN, SIGMA_MAX, SIGMA_INIT);
			sigmaSlider.setToolTipText("Set the noise standard deviation");
			sigmaSlider.setMajorTickSpacing(10);
			sigmaSlider.setMinorTickSpacing(5);
			sigmaSlider.setPaintTicks(true);
			sigmaSlider.setPaintLabels(true);

			sigmaPanel = new JPanel();
			sigmaPanel.setBorder(BorderFactory.createTitledBorder("Noise Standard Deviation"));
			sigmaPanel.add(sigmaSlider);

			pathsSlider = new JSlider(JSlider.HORIZONTAL, PATHS_MIN, PATHS_MAX, PATHS_INIT);
			pathsSlider.setToolTipText("Set the number of samples per pixel");
			pathsSlider.setMajorTickSpacing(20);
			pathsSlider.setMinorTickSpacing(10);
			pathsSlider.setPaintTicks(true);
			pathsSlider.setPaintLabels(true);

			pathsPanel = new JPanel();
			pathsPanel.setBorder(BorderFactory.createTitledBorder("Samples"));
			pathsPanel.add(pathsSlider);

			applyButton.addActionListener(actionListener);
			sigmaSlider.addChangeListener(changeListener);
			pathsSlider.addChangeListener(changeListener);

			GridBagLayout configLayout = new GridBagLayout();
			GridBagConstraints configConstraints = new GridBagConstraints();
			configConstraints.anchor = GridBagConstraints.NORTHWEST;
			configConstraints.fill = GridBagConstraints.HORIZONTAL;
			configConstraints.gridwidth = 1;
			configConstraints.gridheight = 1;
			configConstraints.gridx = 0;
			configConstraints.gridy = 0;
			configConstraints.insets = new Insets(5, 5, 6, 6);
			configPanel.setLayout(configLayout);
			
			configPanel.add(sigmaPanel, configConstraints);
			configConstraints.gridy++;
			configPanel.add(pathsPanel, configConstraints);
			configConstraints.gridy++;
			configPanel.add(applyButton, configConstraints);
			configConstraints.gridy++;

			add(configPanel);

			setDefaultCloseOperation(DISPOSE_ON_CLOSE);

			pack();
			setVisible(true);
		}
	}

	public void run(String arg) {

		IJ.log("Starting plugin Stochastic Denoise");

		// read image
		imp   = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.showMessage("Please open an image first.");
			return;
		}

		image      = ImagePlusAdapter.wrap(imp);
		dimensions = image.getDimensions();
		int width  = dimensions[0];
		int height = dimensions[1];
		int slices = 1;
		if (dimensions.length > 2)
			slices = dimensions[2];

		// prepare segmentation image
		dns = imp.createImagePlus();
		ImageStack stack = new ImageStack(width, height);
		for (int s = 1; s <= slices; s++) {
			ImageProcessor duplProcessor = imp.getStack().getProcessor(s).duplicate();
			stack.addSlice("", duplProcessor);
		}
		dns.setStack(stack);
		dns.setDimensions(1, slices, 1);
		if (slices > 1)
			dns.setOpenAsHyperStack(true);

		dns.setTitle("denoised " + imp.getTitle());

		denoised = ImagePlusAdapter.wrap(dns);

		// set up algorithm
		stochasticDenoise = new StochasticDenoise<T>();
		stochasticDenoise.setParameters(PATHS_INIT, minProb, SIGMA_INIT);

		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						IJ.log("Creating window...");
						new ConfigWindow();
					}
				});
	}
}
