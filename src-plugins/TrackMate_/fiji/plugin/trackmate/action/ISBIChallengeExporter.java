package fiji.plugin.trackmate.action;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.util.TMUtils;

public class ISBIChallengeExporter extends AbstractTMAction {

	private static final ImageIcon ICON = new ImageIcon(TrackMateWizard.class.getResource("images/ISBIlogo.png"));


	/*
	 * CONSTRUCTOR
	 */

	public ISBIChallengeExporter() {
		this.icon = ICON;
	}

	/*
	 * METHODS
	 */

	@Override
	public void execute(TrackMate_ plugin) {
		logger.log("Exporting to ISBI 2012 particle tracking challenge format.\n");
		final TrackMateModel model = plugin.getModel();
		int ntracks = model.getNFilteredTracks();
		if (ntracks == 0) {
			logger.log("No visible track found. Aborting.\n");
			return;
		}

		logger.log("  Preparing XML data.\n");
		Element root = marshall(model);

		File file;
		File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
		try {
			String filename = plugin.getModel().getSettings().imageFileName;
			filename = filename.substring(0, filename.indexOf("."));
			file = new File(folder.getPath() + File.separator + filename +"_ISBI.xml");
		} catch (NullPointerException npe) {
			file = new File(folder.getPath() + File.separator + "ISBIChallenge2012Result.xml");
		}
		file = TMUtils.askForFile(file, wizard, logger);

		logger.log("  Writing to file.\n");
		Document document = new Document(root);
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		try {
			outputter.output(document, new FileOutputStream(file));
		} catch (FileNotFoundException e) {
			logger.error("Trouble writing to "+file+":\n" + e.getMessage());
		} catch (IOException e) {
			logger.error("Trouble writing to "+file+":\n" + e.getMessage());
		}
		logger.log("Done.\n");
	}

	@Override
	public String getInfoText() {
		return "<html>" +
				"Export the current model content to an XML file following the " +
				"ISBI 2012 particle tracking challenge format, as specified on " +
				"<a href='http://bioimageanalysis.org/track/'></a>. " +
				"<p> " +
				"Only tracks are exported. If there is not track, this action " +
				"does nothing. " +
				"</html>";
	}

	@Override
	public String toString() {
		return "Export to ISBI challenge format";
	}

	private Element marshall(TrackMateModel model) {
		Element root = new Element("root");
		Element content = new Element(CONTENT_KEY);

		// Extract from file name
		String filename = model.getSettings().imageFileName; // VIRUS snr 7 density mid.tif
		String pattern = "^(\\w+) " + SNR_ATT +" (\\d+) " + DENSITY_ATT + " (\\w+)\\.";	
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(filename);
		String snr_val;
		String density_val;
		String scenario_val;
		if (m.find()) {
			scenario_val 	= m.group(1);
			snr_val 		= m.group(2);
			density_val 	= m.group(3);
		} else {
			scenario_val = filename;
			snr_val = "?";
			density_val = "?";
		}
		content.setAttribute(SNR_ATT, snr_val);
		content.setAttribute(DENSITY_ATT, density_val);
		content.setAttribute(SCENARIO_ATT, scenario_val);
		content.setAttribute(DATE_ATT, new Date().toString());

		logger.setStatus("Marshalling...");
		for (int i = 0 ; i < model.getNFilteredTracks() ; i++) {

			Element trackElement = new Element(TRACK_KEY);
			Set<Spot> track = model.getTrackSpots(i);
			// Sort them by time 
			TreeSet<Spot> sortedTrack = new TreeSet<Spot>(Spot.frameComparator);
			sortedTrack.addAll(track);
			
			for (Spot spot : sortedTrack) {
				float t = spot.getFeature(Spot.POSITION_T);
				float x = spot.getFeature(Spot.POSITION_X);
				float y = spot.getFeature(Spot.POSITION_Y);
				float z = spot.getFeature(Spot.POSITION_Z);

				Element spotElement = new Element(SPOT_KEY);
				spotElement.setAttribute(T_ATT, ""+ (int)t);
				spotElement.setAttribute(X_ATT, ""+x);
				spotElement.setAttribute(Y_ATT, ""+y);
				spotElement.setAttribute(Z_ATT, ""+z);
				trackElement.addContent(spotElement);
			}
			content.addContent(trackElement);
			logger.setProgress(i / (0f + model.getNFilteredTracks()));
		}

		logger.setStatus("");
		logger.setProgress(1);
		root.addContent(content);
		return root;
	}


	/*
	 * XML KEYS
	 */

	private static final String CONTENT_KEY = "TrackContestISBI2012";
	private static final String DATE_ATT = "generationDateTime";
	private static final String SNR_ATT = "snr";
	private static final String DENSITY_ATT = "density";
	private static final String SCENARIO_ATT = "scenario";

	private static final String TRACK_KEY = "particle";
	private static final String SPOT_KEY = "detection";
	private static final String X_ATT = "x";
	private static final String Y_ATT = "y";
	private static final String Z_ATT = "z";
	private static final String T_ATT = "t";

}
