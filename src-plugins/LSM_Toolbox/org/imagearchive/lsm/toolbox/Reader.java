package org.imagearchive.lsm.toolbox;

import ij.IJ;
import ij.ImagePlus;
import ij.io.OpenDialog;
import ij.io.RandomAccessStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.JFileChooser;

import org.imagearchive.lsm.reader.info.ChannelNamesAndColors;
import org.imagearchive.lsm.reader.info.ImageDirectory;
import org.imagearchive.lsm.reader.info.LSMFileInfo;
import org.imagearchive.lsm.toolbox.gui.AllKnownFilter;
import org.imagearchive.lsm.toolbox.gui.BatchFilter;
import org.imagearchive.lsm.toolbox.gui.ImageFilter;
import org.imagearchive.lsm.toolbox.gui.ImagePreview;
import org.imagearchive.lsm.toolbox.info.CZLSMInfoExtended;
import org.imagearchive.lsm.toolbox.info.ChannelWavelengthRange;
import org.imagearchive.lsm.toolbox.info.Event;
import org.imagearchive.lsm.toolbox.info.EventList;
import org.imagearchive.lsm.toolbox.info.TimeStamps;
import org.imagearchive.lsm.toolbox.info.scaninfo.BeamSplitter;
import org.imagearchive.lsm.toolbox.info.scaninfo.DataChannel;
import org.imagearchive.lsm.toolbox.info.scaninfo.DetectionChannel;
import org.imagearchive.lsm.toolbox.info.scaninfo.IlluminationChannel;
import org.imagearchive.lsm.toolbox.info.scaninfo.Laser;
import org.imagearchive.lsm.toolbox.info.scaninfo.Marker;
import org.imagearchive.lsm.toolbox.info.scaninfo.Recording;
import org.imagearchive.lsm.toolbox.info.scaninfo.ScanInfo;
import org.imagearchive.lsm.toolbox.info.scaninfo.Timer;
import org.imagearchive.lsm.toolbox.info.scaninfo.Track;

public class Reader {

	private MasterModel masterModel;

	public Reader() {
		masterModel = MasterModel.getMasterModel();
	}

	public static CZLSMInfoExtended getCZ(String filename) {
		Reader reader = ServiceMediator.getReader();
		ImagePlus imp = reader.open(filename, false);
		if (imp == null) return null;
		reader.updateMetadata(imp);
		LSMFileInfo lsm = (LSMFileInfo) imp.getOriginalFileInfo();
		ImageDirectory imDir = (ImageDirectory) lsm.imageDirectories.get(0);
		CZLSMInfoExtended cz = (CZLSMInfoExtended) imDir.TIF_CZ_LSMINFO;
		return cz;
	}

	public ImagePlus open(String arg, boolean verbose) {
		File file = null;
		ImagePlus imp = null;
		if (arg.equals("")) {
			JFileChooser fc = new JFileChooser();
			fc.addChoosableFileFilter(new BatchFilter());
			fc.addChoosableFileFilter(new ImageFilter());
			fc.addChoosableFileFilter(new AllKnownFilter());
			fc.setAcceptAllFileFilterUsed(false);
			fc.setAccessory(new ImagePreview(fc));
			fc.setName("Open Zeiss LSM image");
			String directory = OpenDialog.getDefaultDirectory();
			if (directory != null) {
				File directoryHandler = new File(directory);
				if (directoryHandler != null && directoryHandler.isDirectory())
					fc.setCurrentDirectory(directoryHandler);
			}
			int returnVal = fc.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				file = fc.getSelectedFile();
				if (file == null) {
					IJ.error("no file selected");
					return null;
				}
				if (file.getAbsolutePath().endsWith(".csv")) {
					BatchConverter converter = new BatchConverter(masterModel);
					converter.convertBatchFile(file.getAbsolutePath());
					return null;
				}
			}
		} else
			file = new File(arg);
		if (file != null) {
			imp = open(file.getParent(), file.getName(), verbose, false);
			updateMetadata(imp);
			LSMFileInfo openLSM = (LSMFileInfo) imp.getOriginalFileInfo();
			// printImDirData(openLSM);
			OpenDialog.setDefaultDirectory(file.getParent());
		}
		return imp;
	}

	public ImagePlus open(String directory, String filename, boolean verbose,
			boolean thumb) {
		ImagePlus imp = null;
		org.imagearchive.lsm.reader.Reader r = new org.imagearchive.lsm.reader.Reader();
		imp = r.open(directory, filename, false, false);
		return imp;
	}

	private void printImDirData(LSMFileInfo lsmFi) {
		for (int i = 0; i < lsmFi.imageDirectories.size(); i++) {
			System.err.println("Imdir " + i);
			System.err.println("=============\n");
			ImageDirectory imDir = (ImageDirectory) lsmFi.imageDirectories
					.get(i);
			System.err.println("ImDir data:\n" + imDir.toString());
			if (imDir.TIF_CZ_LSMINFO != null)
				System.err.println("CZ-Info data:\n"
						+ imDir.TIF_CZ_LSMINFO.toString());
			else
				System.err.println("CZ-Info data is null (not set)");
			System.err
					.println("=================================================");
		}

	}

	public void readMetadata(RandomAccessStream stream, ImagePlus imp) {
		if (imp.getOriginalFileInfo() instanceof LSMFileInfo) {
			LSMFileInfo lsm = (LSMFileInfo) imp.getOriginalFileInfo();
			if (lsm.fullyRead == true)
				return;
			ImageDirectory imDir = (ImageDirectory) lsm.imageDirectories.get(0);
			if (imDir == null)
				return;
			long offset = imDir.TIF_CZ_LSMINFO_OFFSET;
			imDir.TIF_CZ_LSMINFO = getCZ_LSMINFO(stream, offset, false);
			lsm.imageDirectories.set(0, imDir);
			imp.setFileInfo(lsm);
		}
	}

	public void updateMetadata(ImagePlus imp) {
		if (imp == null)
			return;
		if (imp.getOriginalFileInfo() instanceof LSMFileInfo) {
			LSMFileInfo lsm = (LSMFileInfo) imp.getOriginalFileInfo();
			if (lsm.fullyRead == true)
				return;
			RandomAccessFile file;
			try {
				file = new RandomAccessFile(new File(lsm.directory
						+ System.getProperty("file.separator") + lsm.fileName),
						"r");
				RandomAccessStream stream = new RandomAccessStream(file);
				ImageDirectory imDir = (ImageDirectory) lsm.imageDirectories
						.get(0);
				if (imDir == null)
					return;
				long offset = imDir.TIF_CZ_LSMINFO_OFFSET;
				imDir.TIF_CZ_LSMINFO = getCZ_LSMINFO(stream, offset, false);
				lsm.fullyRead = true;
				lsm.imageDirectories.set(0, imDir);
				imp.setFileInfo(lsm);
			} catch (FileNotFoundException e) {
				IJ.error("Could not update metadata.");
			}
		}
	}

	public boolean isLSMfile(RandomAccessStream stream) {
		boolean identifier = false;
		long ID = 0;
		try {
			stream.seek(2);
			ID = ReaderToolkit.swap(stream.readShort());
			if (ID == 42)
				identifier = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return identifier;
	}

	private CZLSMInfoExtended getCZ_LSMINFO(RandomAccessStream stream,
			long position, boolean thumb) {
		CZLSMInfoExtended cz = new CZLSMInfoExtended();
		try {
			if (position == 0)
				return cz;
			stream.seek((int) position + 8);

			cz.DimensionX = ReaderToolkit.swap(stream.readInt());
			cz.DimensionY = ReaderToolkit.swap(stream.readInt());
			cz.DimensionZ = ReaderToolkit.swap(stream.readInt());

			// number of channels
			cz.DimensionChannels = ReaderToolkit.swap(stream.readInt());
			// Timestack size
			cz.DimensionTime = ReaderToolkit.swap(stream.readInt());

			cz.IntensityDataType = ReaderToolkit.swap(stream.readInt());

			cz.ThumbnailX = ReaderToolkit.swap(stream.readInt());

			cz.ThumbnailY = ReaderToolkit.swap(stream.readInt());

			cz.VoxelSizeX = ReaderToolkit.swap(stream.readDouble());
			cz.VoxelSizeY = ReaderToolkit.swap(stream.readDouble());
			cz.VoxelSizeZ = ReaderToolkit.swap(stream.readDouble());

			cz.OriginX = ReaderToolkit.swap(stream.readDouble());
			cz.OriginY = ReaderToolkit.swap(stream.readDouble());
			cz.OriginZ = ReaderToolkit.swap(stream.readDouble());

			cz.ScanType = ReaderToolkit.swap(stream.readShort());
			cz.SpectralScan = ReaderToolkit.swap(stream.readShort());
			cz.DataType = ReaderToolkit.swap(stream.readInt());
			cz.OffsetVectorOverlay = ReaderToolkit.swap(stream.readInt());
			cz.OffsetInputLut = ReaderToolkit.swap(stream.readInt());
			cz.OffsetOutputLut = ReaderToolkit.swap(stream.readInt());
			cz.OffsetChannelColors = ReaderToolkit.swap(stream.readInt());
			cz.TimeIntervall = ReaderToolkit.swap(stream.readDouble());

			cz.OffsetChannelDataTypes = ReaderToolkit.swap(stream.readInt());

			cz.OffsetScanInformation = ReaderToolkit.swap(stream.readInt());
			cz.OffsetKsData = ReaderToolkit.swap(stream.readInt());
			cz.OffsetTimeStamps = ReaderToolkit.swap(stream.readInt());
			cz.OffsetEventList = ReaderToolkit.swap(stream.readInt());
			cz.OffsetRoi = ReaderToolkit.swap(stream.readInt());
			cz.OffsetBleachRoi = ReaderToolkit.swap(stream.readInt());
			cz.OffsetNextRecording = ReaderToolkit.swap(stream.readInt());

			cz.DisplayAspectX = ReaderToolkit.swap(stream.readDouble());
			cz.DisplayAspectY = ReaderToolkit.swap(stream.readDouble());
			cz.DisplayAspectZ = ReaderToolkit.swap(stream.readDouble());
			cz.DisplayAspectTime = ReaderToolkit.swap(stream.readDouble());

			cz.OffsetMeanOfRoisOverlay = ReaderToolkit.swap(stream.readInt());
			cz.OffsetTopoIsolineOverlay = ReaderToolkit.swap(stream.readInt());
			cz.OffsetTopoProfileOverlay = ReaderToolkit.swap(stream.readInt());
			cz.OffsetLinescanOverlay = ReaderToolkit.swap(stream.readInt());

			cz.ToolbarFlags = ReaderToolkit.swap(stream.readInt());
			cz.OffsetChannelWavelength = ReaderToolkit.swap(stream.readInt());
			cz.OffsetChannelFactors = ReaderToolkit.swap(stream.readInt());
			cz.ObjectiveSphereCorrection = ReaderToolkit.swap(stream.readInt());
			cz.OffsetUnmixParameters = ReaderToolkit.swap(stream.readInt());
			// not reading reserved ... should be 72 words
			if (cz.OffsetChannelDataTypes != 0) {
				cz.OffsetChannelDataTypesValues = getOffsetChannelDataTypesValues(
						stream, cz.OffsetChannelDataTypes, cz.DimensionChannels);
			}
			if (cz.OffsetChannelColors != 0) {
				ChannelNamesAndColors channelNamesAndColors = getChannelNamesAndColors(
						stream, cz.OffsetChannelColors, cz.DimensionChannels);
				cz.channelNamesAndColors = channelNamesAndColors;
			}
			if (cz.OffsetChannelWavelength != 0) {
				cz.channelWavelength = getLambdaStamps(stream,
						cz.OffsetChannelWavelength);
			}

			if (cz.OffsetTimeStamps != 0) {
				cz.timeStamps = getTimeStamps(stream, cz.OffsetTimeStamps);
				if ((cz.ScanType == 3) || (cz.ScanType == 4)
						|| (cz.ScanType == 5) || (cz.ScanType == 6)
						|| (cz.ScanType == 9) || (cz.ScanType == 10)) {
					if (cz.OffsetEventList != 0)
						cz.eventList = getEventList(stream, cz.OffsetEventList,
								cz.timeStamps.FirstTimeStamp);
				}
			}
			if (cz.OffsetScanInformation != 0) {
				if (!thumb)
					cz.scanInfo = getScanInfo(stream, cz.OffsetScanInformation);
			}
		} catch (IOException getCZ_LSMINFO_exception) {
			getCZ_LSMINFO_exception.printStackTrace();
		}
		return cz;
	}

	private int[] getOffsetChannelDataTypesValues(RandomAccessStream stream,
			long position, long channelCount) {
		int[] OffsetChannelDataTypesValues = new int[(int) channelCount];
		try {
			stream.seek((int) position);

			for (int i = 0; i < channelCount; i++) {
				OffsetChannelDataTypesValues[i] = ReaderToolkit.swap(stream
						.readInt());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return OffsetChannelDataTypesValues;
	}

	private ChannelNamesAndColors getChannelNamesAndColors(
			RandomAccessStream stream, long position, long channelCount) {
		ChannelNamesAndColors channelNamesAndColors = new ChannelNamesAndColors();
		try {
			stream.seek((int) position);
			channelNamesAndColors.BlockSize = ReaderToolkit.swap(stream
					.readInt());
			channelNamesAndColors.NumberColors = ReaderToolkit.swap(stream
					.readInt());
			channelNamesAndColors.NumberNames = ReaderToolkit.swap(stream
					.readInt());
			channelNamesAndColors.ColorsOffset = ReaderToolkit.swap(stream
					.readInt());
			channelNamesAndColors.NamesOffset = ReaderToolkit.swap(stream
					.readInt());
			channelNamesAndColors.Mono = ReaderToolkit.swap(stream.readInt());
			// reserved 4 words
			stream.seek((int) channelNamesAndColors.NamesOffset
					+ (int) position);
			channelNamesAndColors.ChannelNames = new String[(int) channelCount];
			// long Namesize = channelNamesAndColors.BlockSize-
			// channelNamesAndColors.NamesOffset;
			for (int j = 0; j < channelCount; j++) {
				long size = ReaderToolkit.swap(stream.readInt());
				channelNamesAndColors.ChannelNames[j] = ReaderToolkit
						.readSizedNULLASCII(stream, size);
			}
			stream.seek((int) channelNamesAndColors.ColorsOffset
					+ (int) position);
			channelNamesAndColors.Colors = new int[(int) (channelNamesAndColors.NumberColors)];

			for (int j = 0; j < (int) (channelNamesAndColors.NumberColors); j++) {
				channelNamesAndColors.Colors[j] = ReaderToolkit.swap(stream
						.readInt());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return channelNamesAndColors;
	}

	private TimeStamps getTimeStamps(RandomAccessStream stream, long position) {
		TimeStamps timeStamps = new TimeStamps();
		try {
			stream.seek((int) position);
			timeStamps.Size = ReaderToolkit.swap(stream.readInt());
			timeStamps.NumberTimeStamps = ReaderToolkit.swap(stream.readInt());
			timeStamps.Stamps = new double[(int) timeStamps.NumberTimeStamps];
			timeStamps.TimeStamps = new double[(int) timeStamps.NumberTimeStamps];

			for (int i = 0; i < timeStamps.NumberTimeStamps; i++) {
				timeStamps.Stamps[i] = ReaderToolkit.swap(stream.readDouble());
			}
			for (int i = 1; i < timeStamps.NumberTimeStamps; i++) {
				timeStamps.TimeStamps[i] = timeStamps.Stamps[i]
						- timeStamps.Stamps[0];
			}
			timeStamps.FirstTimeStamp = timeStamps.Stamps[0];
			timeStamps.TimeStamps[0] = 0;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return timeStamps;
	}

	private ChannelWavelengthRange getLambdaStamps(RandomAccessStream stream,
			long position) {
		ChannelWavelengthRange channelWavelength = new ChannelWavelengthRange();
		try {
			stream.seek((int) position);
			channelWavelength.Channels = ReaderToolkit.swap(stream.readInt());
			channelWavelength.StartWavelength = new double[(int) channelWavelength.Channels];
			channelWavelength.EndWavelength = new double[(int) channelWavelength.Channels];
			channelWavelength.LambdaStamps = new double[(int) channelWavelength.Channels];
			for (int i = 0; i < channelWavelength.Channels; i++) {
				channelWavelength.StartWavelength[i] = ReaderToolkit
						.swap(stream.readDouble());
				channelWavelength.EndWavelength[i] = ReaderToolkit.swap(stream
						.readDouble());
				channelWavelength.LambdaStamps[i] = (channelWavelength.StartWavelength[i] + channelWavelength.EndWavelength[i]) / 2;
			}
		} catch (IOException getLAMBDASTAMPS_exception) {
			getLAMBDASTAMPS_exception.printStackTrace();
		}
		return channelWavelength;
	}

	private EventList getEventList(RandomAccessStream stream, long position,
			double firstTimeStamp) {
		EventList eventList = new EventList();
		String EventType = "";
		String EventDescription = "";
		String EventNote = "";
		int pointer = 0;
		try {
			stream.seek((int) position);
			eventList.Size = ReaderToolkit.swap(stream.readInt());
			eventList.NumberEvents = ReaderToolkit.swap(stream.readInt());
			eventList.events = new Event[(int) eventList.NumberEvents];
			pointer = stream.getFilePointer();
			if (eventList.NumberEvents > 0)
				for (int i = 0; i < eventList.NumberEvents; i++) {
					eventList.events[i] = new Event();
					eventList.events[i].SizeEventListEntry = ReaderToolkit
							.swap(stream.readInt());
					eventList.events[i].Time = ReaderToolkit.swap(stream
							.readDouble());
					eventList.events[i].EventType = ReaderToolkit.swap(stream
							.readInt());
					switch ((int) eventList.events[i].EventType) {
					case (0):
						EventType = "Marker";
						break;
					case (1):
						EventType = "Timer Change";
						break;
					case (2):
						EventType = "Bleach Start";
						break;
					case (3):
						EventType = "Bleach Stop";
						break;
					case (4):
						EventType = "Trigger";
						break;
					default:
						EventType = "Unknown";
						break;
					}
					ReaderToolkit.swap(stream.readInt());
					EventDescription += IJ.d2s(eventList.events[i].Time
							- firstTimeStamp)
							+ "\t" + EventType + "\t";
					EventNote = ReaderToolkit.readNULLASCII2(stream,
							eventList.events[i].SizeEventListEntry - 16);
					pointer += (int) eventList.events[i].SizeEventListEntry;
					stream.seek(pointer);
					EventDescription += EventNote + "\n";
				}
			eventList.Description = EventDescription;
		} catch (IOException getEVENTLIST_exception) {
			IJ.log("IOException \n" + "Last Offset: " + IJ.d2s(position, 0));
			getEVENTLIST_exception.printStackTrace();
		}
		return eventList;
	}

	private ScanInfo getScanInfo(RandomAccessStream stream, long position) {
		ScanInfo scanInfo = new ScanInfo();
		ScanInfoTag tag = new ScanInfoTag();
		try {
			stream.seek((int) position);
			while (tag.entry != 0x0FFFFFFFF) {
				tag = getScanInfoTag(stream);
				if (Recording.isRecording(tag.entry)) {
					Recording recording = new Recording();
					while (tag.entry != 0x0FFFFFFFF) {
						tag = getScanInfoTag(stream);
						if (Laser.isLasers(tag.entry)) {
							recording.lasers = getLaserBlock(stream);
							tag.entry = 0;
						}
						if (Track.isTracks(tag.entry)) {
							recording.tracks = getTrackBlock(stream);
							tag.entry = 0;
						}
						if (Marker.isMarkers((tag.entry))) {
							recording.markers = getMarkerBlock(stream);
							tag.entry = 0;
						}
						if (Timer.isTimers(tag.entry)) {
							recording.timers = getTimerBlock(stream);
							tag.entry = 0;
						}
						recording.records = getRecords(stream, tag,
								Recording.data, recording.records);
					}
					scanInfo.recordings.add(recording);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return scanInfo;
	}

	private Laser[] getLaserBlock(RandomAccessStream stream) {
		ScanInfoTag tag = new ScanInfoTag();
		if (IJ.debugMode)
			IJ.log("Lasers");
		Vector<Laser> v = new Vector<Laser>();
		while (tag.entry != 0x0FFFFFFFF) {
			tag = getScanInfoTag(stream);
			if (Laser.isLaser(tag.entry)) {
				Laser l = new Laser();
				while (tag.entry != 0x0FFFFFFFF) {
					tag = getScanInfoTag(stream);
					if (IJ.debugMode)
						IJ.log("Lasertag: " + Long.toHexString(tag.entry));
					if (tag.entry != 0x0FFFFFFFF)
						l.records = getRecords(stream, tag, l.data, l.records);
				}
				v.add(l);
				tag.entry = 0;

			}

		}
		return (Laser[]) v.toArray(new Laser[v.size()]);

	}

	private IlluminationChannel[] getIlluminationBlock(RandomAccessStream stream) {
		ScanInfoTag tag = new ScanInfoTag();
		Vector<IlluminationChannel> vdc = new Vector<IlluminationChannel>();
		while (tag.entry != 0x0FFFFFFFF) {
			tag = getScanInfoTag(stream);

			if (IlluminationChannel.isIlluminationChannel(tag.entry)) {
				IlluminationChannel ic = new IlluminationChannel();

				while (tag.entry != 0x0FFFFFFFF) {
					tag = getScanInfoTag(stream);
					ic.records = getRecords(stream, tag, ic.data, ic.records);
				}
				vdc.add(ic);
				tag.entry = 0;
			}

		}
		return (IlluminationChannel[]) vdc.toArray(new IlluminationChannel[vdc
				.size()]);
	}

	private Track[] getTrackBlock(RandomAccessStream stream) {
		ScanInfoTag tag = new ScanInfoTag();
		Vector<Track> v = new Vector<Track>();
		int tracksnum = 0;
		while (tag.entry != 0x0FFFFFFFF) {
			tag = getScanInfoTag(stream);
			if (Track.isTrack(tag.entry)) {
				tracksnum++;
				Track t = new Track();
				while (tag.entry != 0x0FFFFFFFF) {
					tag = getScanInfoTag(stream);

					if (IJ.debugMode)
						IJ.log("Tracktag: " + Long.toHexString(tag.entry));

					if (IlluminationChannel.isIlluminationChannels(tag.entry)) {
						t.illuminationChannels = getIlluminationBlock(stream);
						tag.entry = 0;
					}
					if (DetectionChannel.isDetectionChannels(tag.entry)) {
						t.detectionChannels = getDetectionChannelBlock(stream);
						tag.entry = 0;
					}
					if (BeamSplitter.isBeamSplitters(tag.entry)) {
						t.beamSplitters = getBeamSplitterBlock(stream);
						tag.entry = 0;
					}
					if (DataChannel.isDataChannels(tag.entry)) {
						t.dataChannels = getDataChannelBlock(stream);
						tag.entry = 0;
					}

					if (tag.entry != 0x0FFFFFFFF)
						t.records = getRecords(stream, tag, t.data, t.records);

				}
				v.add(t);
				tag.entry = 0;
			}
		}
		return (Track[]) v.toArray(new Track[v.size()]);
	}

	private DetectionChannel[] getDetectionChannelBlock(
			RandomAccessStream stream) {
		ScanInfoTag tag = new ScanInfoTag();
		Vector<DetectionChannel> vdc = new Vector<DetectionChannel>();
		while (tag.entry != 0x0FFFFFFFF) {
			tag = getScanInfoTag(stream);
			if (DetectionChannel.isDetectionChannel(tag.entry)) {
				DetectionChannel ic = new DetectionChannel();
				while (tag.entry != 0x0FFFFFFFF) {
					tag = getScanInfoTag(stream);
					ic.records = getRecords(stream, tag, ic.data, ic.records);
				}
				vdc.add(ic);
				tag.entry = 0;
			}
		}
		DetectionChannel[] dc = new DetectionChannel[vdc.size()];
		for (int i = 0; i < vdc.size(); i++) {
			dc[i] = (DetectionChannel) vdc.get(i);
		}
		return dc;
	}

	private BeamSplitter[] getBeamSplitterBlock(RandomAccessStream stream) {
		ScanInfoTag tag = new ScanInfoTag();
		Vector<BeamSplitter> vdc = new Vector<BeamSplitter>();
		while (tag.entry != 0x0FFFFFFFF) {
			tag = getScanInfoTag(stream);
			if (BeamSplitter.isBeamSplitter(tag.entry)) {
				BeamSplitter ic = new BeamSplitter();
				while (tag.entry != 0x0FFFFFFFF) {
					tag = getScanInfoTag(stream);
					ic.records = getRecords(stream, tag, ic.data, ic.records);
				}
				vdc.add(ic);
				tag.entry = 0;
			}

		}
		return (BeamSplitter[]) vdc.toArray(new BeamSplitter[vdc.size()]);
	}

	private Marker[] getMarkerBlock(RandomAccessStream stream) {
		ScanInfoTag tag = new ScanInfoTag();
		Vector<Marker> v = new Vector<Marker>();
		while (tag.entry != 0x0FFFFFFFF) {
			tag = getScanInfoTag(stream);
			if (Marker.isMarker(tag.entry)) {
				Marker m = new Marker();
				while (tag.entry != 0x0FFFFFFFF) {
					tag = getScanInfoTag(stream);
					m.records = getRecords(stream, tag, m.data, m.records);
				}
				v.add(m);
				tag.entry = 0;
			}
		}
		return (Marker[]) v.toArray(new Marker[v.size()]);
	}

	private Timer[] getTimerBlock(RandomAccessStream stream) {
		ScanInfoTag tag = new ScanInfoTag();
		Vector<Timer> v = new Vector<Timer>();
		while (tag.entry != 0x0FFFFFFFF) {
			tag = getScanInfoTag(stream);
			if (Timer.isTimer(tag.entry)) {
				Timer t = new Timer();

				while (tag.entry != 0x0FFFFFFFF) {
					tag = getScanInfoTag(stream);
					t.records = getRecords(stream, tag, t.data, t.records);
				}
				v.add(t);
				tag.entry = 0;
			}
		}
		return (Timer[]) v.toArray(new Timer[v.size()]);
	}

	private DataChannel[] getDataChannelBlock(RandomAccessStream stream) {
		ScanInfoTag tag = new ScanInfoTag();
		Vector<DataChannel> vdc = new Vector<DataChannel>();

		while (tag.entry != 0x0FFFFFFFF) {
			tag = getScanInfoTag(stream);
			if (DataChannel.isDataChannel(tag.entry)) {
				DataChannel ic = new DataChannel();
				while (tag.entry != 0x0FFFFFFFF) {

					tag = getScanInfoTag(stream);
					ic.records = getRecords(stream, tag, ic.data, ic.records);
				}
				vdc.add(ic);
				tag.entry = 0;
			}

		}
		return (DataChannel[]) vdc.toArray(new DataChannel[vdc.size()]);
	}

	private LinkedHashMap<String, Object> getRecords(RandomAccessStream stream,
			ScanInfoTag tag, Object[][] data, LinkedHashMap<String, Object> lhm) {
		try {
			String value = "";
			long l = 0;
			double d = 0;
			long position = stream.getFilePointer();
			if (tag.type == 2)
				value = ReaderToolkit.readSizedNULLASCII(stream, tag.size);
			if (tag.type == 4)
				l = ReaderToolkit.swap(stream.readInt());
			if (tag.type == 5)
				d = ReaderToolkit.swap(stream.readDouble());
			for (int i = 0; i < data.length; i++) {
				if (((Long) data[i][0]).longValue() == tag.entry) {
					if (tag.type == 2) {
						if (IJ.debugMode)
							IJ.log("Tag recognized: ["
									+ Long.toHexString(tag.entry) + "] -->"
									+ (String) data[i][2]);
						lhm.put((String) data[i][2], value);
						return lhm;
					}
					if (tag.type == 4) {
						lhm.put((String) data[i][2], new Long(l));
						if (IJ.debugMode)
							IJ.log("Tag recognized: ["
									+ Long.toHexString(tag.entry) + "] -->"
									+ (String) data[i][2]);
						return lhm;
					}
					if (tag.type == 5) {
						lhm.put((String) data[i][2], new Double(d));
						if (IJ.debugMode)
							IJ.log("Tag recognized: ["
									+ Long.toHexString(tag.entry) + "] -->"
									+ (String) data[i][2]);
						return lhm;
					}
				}
			}
			if (tag.type == 2)
				lhm.put("<UNKNOWN@" + (position - 12) + ">", value);
			if (tag.type == 4)
				lhm.put("<UNKNOWN@" + (position - 12) + ">", new Long(l));
			if (tag.type == 5)
				lhm.put("<UNKNOWN@" + (position - 12) + ">", new Double(d));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lhm;
	}

	private ScanInfoTag getScanInfoTag(RandomAccessStream stream) {
		ScanInfoTag sit = new ScanInfoTag();
		try {
			int s1 = stream.read();
			int s2 = stream.read();
			int s3 = stream.read();
			int s4 = stream.read();
			sit.entry = (long) ((s4 << 24) + (s2 << 16) + (s3 << 8) + s1);
			sit.type = ReaderToolkit.swap(stream.readInt());
			sit.size = ReaderToolkit.swap(stream.readInt());
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (IJ.debugMode)
			IJ.log("Tag read: [" + Long.toHexString(sit.entry) + "]");
		return sit;
	}

	/**
	 * *************************************************************************
	 * ******************
	 */
	private class ScanInfoTag {
		public long entry = 0;

		public long type = 0;

		public long size = 0;
	}

}
