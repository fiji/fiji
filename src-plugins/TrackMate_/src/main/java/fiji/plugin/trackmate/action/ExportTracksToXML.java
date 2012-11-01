package fiji.plugin.trackmate.action;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

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

public class ExportTracksToXML extends AbstractTMAction {

	private static final ImageIcon ICON = new ImageIcon(TrackMateWizard.class.getResource("images/page_save.png"));
	
	/*
	 * CONSTRUCTOR
	 */

	public ExportTracksToXML() {
		this.icon = ICON;
	}

	/*
	 * METHODS
	 */

	@Override
	public void execute(TrackMate_ plugin) {

		logger.log("Exporting tracks to simple XML format.\n");
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
			file = new File(folder.getPath() + File.separator + filename +"_Tracks.xml");
		} catch (NullPointerException npe) {
			file = new File(folder.getPath() + File.separator + "Tracks.xml");
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
				"Export the tracks in the current model content to a XML " +
				"file in a simple format. " +
				"<p> " +
				"The file will have one element per track, and each track " +
				"contains several spot elements. These spots are " +
				"sorted by frame number, and have 4 numerical attributes: " +
				"the frame number this spot is in, and its X, Y, Z position in " +
				"physical units as specified in the image properties. " +
				"<p>" +
				"As such, this format <u>cannot</u> handle track merging and " +
				"splitting properly, and is suited only for non-branching tracks." +
				"</html>";
	}

	@Override
	public String toString() {
		return "Export tracks to XML file";
	}

	private Element marshall(TrackMateModel model) {
		Element root = new Element("root");
		Element content = new Element(CONTENT_KEY);
		
		content.setAttribute(PHYSUNIT_ATT, model.getSettings().spaceUnits);
		content.setAttribute(DATE_ATT, new Date().toString());

		logger.setStatus("Marshalling...");
		Integer[] visibleTracks = model.getVisibleTrackIndices().toArray(new Integer[] {});
		for (int i = 0 ; i < model.getNFilteredTracks() ; i++) {

			Element trackElement = new Element(TRACK_KEY);
			int trackindex = visibleTracks[i];
			
			Set<Spot> track = model.getTrackSpots(trackindex);
			// Sort them by time 
			TreeSet<Spot> sortedTrack = new TreeSet<Spot>(Spot.frameComparator);
			sortedTrack.addAll(track);

			for (Spot spot : sortedTrack) {
				int frame = model.getFilteredSpots().getFrame(spot);
				float x = spot.getFeature(Spot.POSITION_X);
				float y = spot.getFeature(Spot.POSITION_Y);
				float z = spot.getFeature(Spot.POSITION_Z);

				Element spotElement = new Element(SPOT_KEY);
				spotElement.setAttribute(T_ATT, ""+frame);
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

	private static final String CONTENT_KEY 	= "Tracks";
	private static final String DATE_ATT 		= "generationDateTime";
	private static final String PHYSUNIT_ATT 	= "spaceUnits";

	private static final String TRACK_KEY = "particle";
	private static final String SPOT_KEY = "detection";
	private static final String X_ATT = "x";
	private static final String Y_ATT = "y";
	private static final String Z_ATT = "z";
	private static final String T_ATT = "t";

}
