package fiji.plugin.trackmate.action;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ImageIcon;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.util.TMUtils;

public class ExportTracksToXML extends AbstractTMAction {

	public static final ImageIcon ICON = new ImageIcon(TrackMateWizard.class.getResource("images/page_save.png"));
	public static final String NAME = "Export tracks to XML file";
	public static final String INFO_TEXT = "<html>" +
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
		int ntracks = model.getTrackModel().getNFilteredTracks();
		if (ntracks == 0) {
			logger.log("No visible track found. Aborting.\n");
			return;
		}

		logger.log("  Preparing XML data.\n");
		Element root = marshall(model);

		File folder; 
		try {
			folder = new File(plugin.getModel().getSettings().imp.getOriginalFileInfo().directory);
		} catch (NullPointerException npe) {
			folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
		}
		
		File file;
		try {
			String filename = plugin.getModel().getSettings().imageFileName;
			filename = filename.substring(0, filename.indexOf("."));
			file = new File(folder.getPath() + File.separator + filename +"_Tracks.xml");
		} catch (NullPointerException npe) {
			file = new File(folder.getPath() + File.separator + "Tracks.xml");
		}
		file = IOUtils.askForFile(file, wizard, logger);
		if (null == file) {
			return;
		}

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
		return INFO_TEXT;
	}

	@Override
	public String toString() {
		return NAME;
	}

	private Element marshall(TrackMateModel model) {
		logger.setStatus("Marshalling...");
		Element content = new Element(CONTENT_KEY);
		
		content.setAttribute(NTRACKS_ATT, ""+model.getTrackModel().getNFilteredTracks());
		content.setAttribute(PHYSUNIT_ATT, model.getSettings().spaceUnits);
		content.setAttribute(FRAMEINTERVAL_ATT, ""+model.getSettings().dt);
		content.setAttribute(FRAMEINTERVALUNIT_ATT, ""+model.getSettings().timeUnits);
		content.setAttribute(DATE_ATT, TMUtils.getCurrentTimeString());
		content.setAttribute(FROM_ATT, TrackMate_.PLUGIN_NAME_STR + " v" + TrackMate_.PLUGIN_NAME_VERSION);

		Set<Integer> trackIDs = model.getTrackModel().getFilteredTrackIDs();
		int i = 0;
		for (Integer trackID : trackIDs) {

			Set<Spot> track = model.getTrackModel().getTrackSpots(trackID);
			
			Element trackElement = new Element(TRACK_KEY);
			trackElement.setAttribute(NSPOTS_ATT, ""+track.size());

			// Sort them by time 
			TreeSet<Spot> sortedTrack = new TreeSet<Spot>(Spot.timeComparator);
			sortedTrack.addAll(track);

			for (Spot spot : sortedTrack) {
				int frame = model.getFilteredSpots().getFrame(spot);
				double x = spot.getFeature(Spot.POSITION_X);
				double y = spot.getFeature(Spot.POSITION_Y);
				double z = spot.getFeature(Spot.POSITION_Z);

				Element spotElement = new Element(SPOT_KEY);
				spotElement.setAttribute(T_ATT, ""+frame);
				spotElement.setAttribute(X_ATT, ""+x);
				spotElement.setAttribute(Y_ATT, ""+y);
				spotElement.setAttribute(Z_ATT, ""+z);
				trackElement.addContent(spotElement);
			}
			content.addContent(trackElement);
			logger.setProgress(i++ / (0d + model.getTrackModel().getNFilteredTracks()));
		}

		logger.setStatus("");
		logger.setProgress(1);
		return content;
	}


	/*
	 * XML KEYS
	 */

	private static final String CONTENT_KEY 	= "Tracks";
	private static final String DATE_ATT 		= "generationDateTime";
	private static final String PHYSUNIT_ATT 	= "spaceUnits";
	private static final String FRAMEINTERVAL_ATT 	= "frameInterval";
	private static final String FRAMEINTERVALUNIT_ATT 	= "timeUnits";
	private static final String FROM_ATT 		= "from";
	private static final String NTRACKS_ATT		= "nTracks";
	private static final String NSPOTS_ATT		= "nSpots";
	
	
	private static final String TRACK_KEY = "particle";
	private static final String SPOT_KEY = "detection";
	private static final String X_ATT = "x";
	private static final String Y_ATT = "y";
	private static final String Z_ATT = "z";
	private static final String T_ATT = "t";


}
