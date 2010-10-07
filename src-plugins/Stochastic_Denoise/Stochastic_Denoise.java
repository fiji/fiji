
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.Dictionary;
import java.util.Hashtable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
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

	private final float SIGMA_MIN  = 0.0f;
	private final float SIGMA_MAX  = 1.0f;
	private final float SIGMA_INIT = 0.15f;
	private final float SIGMA_PRECISION = 1000f; // for the JSlider (that only supports ints)

	private final int PATHS_MIN    = 0;
	private final int PATHS_MAX    = 100;
	private final int PATHS_INIT   = 20;

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

		// executor service to launch threads for the plugin methods and events
		final ExecutorService exec = Executors.newFixedThreadPool(1);
	
		// action listener
		private ActionListener actionListener = new ActionListener() {
	
			public void actionPerformed(final ActionEvent e) {

				// listen to the buttons on separate threads not to block
				// the event dispatch thread
				exec.submit(new Runnable() {
					public void run() 
					{
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
				});
			}
		};
	
		// change listener for sliders
		private ChangeListener changeListener = new ChangeListener() {
	
			public void stateChanged(final ChangeEvent e) {
				JSlider source = (JSlider)e.getSource();
				if(e.getSource() == sigmaSlider)
					sigma = source.getValue()/SIGMA_PRECISION;
				if(e.getSource() == pathsSlider)
					numSamples = source.getValue();
				stochasticDenoise.setParameters(numSamples, minProb, sigma);
				IJ.log("Parameters: numPaths=" + numSamples + ", sigma=" + sigma);
			}
		};

		ConfigWindow() {

			configPanel = new JPanel();

			applyButton  = new JButton("Denoise");

			Dictionary<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
			for (int i = 0; i <= (int)(SIGMA_MAX*SIGMA_PRECISION); i += (int)(SIGMA_PRECISION/5))
				labelTable.put(i, new JLabel("" + (i/SIGMA_PRECISION)));
			sigmaSlider = new JSlider(JSlider.HORIZONTAL, (int)(SIGMA_MIN*SIGMA_PRECISION),
			                                              (int)(SIGMA_MAX*SIGMA_PRECISION),
			                                              (int)(SIGMA_INIT*SIGMA_PRECISION));
			sigmaSlider.setToolTipText("Set the noise standard deviation");
			sigmaSlider.setMajorTickSpacing((int)(SIGMA_PRECISION/5));
			sigmaSlider.setLabelTable(labelTable);
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

			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					exec.shutdownNow();
					applyButton.removeActionListener(actionListener);
					sigmaSlider.removeChangeListener(changeListener);
					pathsSlider.removeChangeListener(changeListener);
				}
			});
	
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
		stochasticDenoise.setParameters(numSamples, minProb, sigma);

		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						IJ.log("Creating window...");
						new ConfigWindow();
					}
				});
	}
}
