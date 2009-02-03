package org.imagearchive.lsm.toolbox;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.LookUpTable;
import ij.io.FileInfo;
import ij.io.ImageReader;
import ij.io.OpenDialog;
import ij.io.RandomAccessStream;
import ij.measure.Calibration;
import ij.text.TextWindow;

import java.awt.Color;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.JFileChooser;

import org.imagearchive.lsm.toolbox.gui.AllKnownFilter;
import org.imagearchive.lsm.toolbox.gui.BatchFilter;
import org.imagearchive.lsm.toolbox.gui.ImageFilter;
import org.imagearchive.lsm.toolbox.gui.ImagePreview;
import org.imagearchive.lsm.toolbox.info.CZ_LSMInfo;
import org.imagearchive.lsm.toolbox.info.ChannelNamesAndColors;
import org.imagearchive.lsm.toolbox.info.ChannelWavelengthRange;
import org.imagearchive.lsm.toolbox.info.Event;
import org.imagearchive.lsm.toolbox.info.ImageDirectory;
import org.imagearchive.lsm.toolbox.info.LsmFileInfo;
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

	public Reader(MasterModel masterModel) {
		this.masterModel = masterModel;
	}

	public Reader() {
		this.masterModel = new MasterModel();
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
			fc.setAccessory(new ImagePreview(masterModel, fc));
			fc.setName("Open Zeiss LSM image");
			String dds = OpenDialog.getDefaultDirectory();
			if (dds != null) {
				File dd = new File(dds);
				if (dd != null && dd.isDirectory())
					fc.setCurrentDirectory(dd);
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
			imp = open(file.getParent(), file.getName(), true, verbose, false);
			OpenDialog.setDefaultDirectory(file.getParent());
		}
		return imp;
	}

	private void showEventList(LsmFileInfo info) {
		ArrayList imDirs = info.imageDirectories;
		ImageDirectory imDir = (ImageDirectory) imDirs.get(0);
		CZ_LSMInfo cz = imDir.TIF_CZ_LSMINFO;
		EventList events = cz.eventList;
		if (events != null) {
			String header = new String(
					"Time (sec) \tEvent Type \tEvent Description");
			TextWindow tw = new TextWindow("Time Events for " + info.fileName,
					header, null, 400, 200);
			tw.append(events.Description);
		}
	}

	public ImagePlus open(String directory, String filename,
			boolean showInfoFrames, boolean verbose, boolean thumb) {
		ImagePlus imp = null;
		RandomAccessFile file;
		LsmFileInfo lsm;
		try {
			file = new RandomAccessFile(new File(directory, filename), "r");
			RandomAccessStream stream = new RandomAccessStream(file);
			lsm = new LsmFileInfo(masterModel);
			lsm.fileName = filename;
			lsm.directory = directory;
			if (isLSMfile(stream)) {
				// read first image directory
				ImageDirectory imDir = readImageDirectoy(stream, 8, thumb);
				lsm.imageDirectories.add(imDir);
				int i = 0;
				while (imDir.OFFSET_NEXT_DIRECTORY != 0) {
					imDir = readImageDirectoy(stream,
							imDir.OFFSET_NEXT_DIRECTORY, thumb);
					lsm.imageDirectories.add(imDir);
					i++;
				}
				// printImDirData(lsm);
				imp = open(stream, lsm, verbose, thumb);
				stream.close();
				if (showInfoFrames)
					showEventList(lsm);
			} else
				IJ.error("Not a valid lsm file");
		} catch (FileNotFoundException e) {
			IJ.error("File not found");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return imp;
	}

	private void printImDirData(LsmFileInfo lsmFi) {
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

	private long getTagCount(RandomAccessStream stream, long position) {
		long tags = 0;
		try {
			stream.seek((int) position);
			tags = ReaderToolkit.swap(stream.readShort());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tags;
	}

	private ImageDirectory readImageDirectoy(RandomAccessStream stream,
			long startPosition, boolean thumb) {
		ImageDirectory imDir = new ImageDirectory();
		long tags = getTagCount(stream, startPosition);
		byte[] tag;

		int tagtype = 0;
		int MASK = 0x00ff;
		long MASK2 = 0x000000ff;
		long currentTagPosition = 0;
		// needed because sometimes offset do not fit in
		// the imDir structure and are placed elsewhere
		long stripOffset = 0, stripByteOffset = 0;

		for (int i = 0; i < tags; i++) {
			currentTagPosition = startPosition + 2 + i * 12;
			tag = readTag(stream, (int) currentTagPosition);
			tagtype = ((tag[1] & MASK) << 8) | ((tag[0] & MASK) << 0);

			switch (tagtype) {
			case 254:
				imDir.TIF_NEWSUBFILETYPE = ((tag[11] & MASK2) << 24)
						| ((tag[10] & MASK2) << 16) | ((tag[9] & MASK2) << 8)
						| (tag[8] & MASK2);
				break;
			case 256:
				imDir.TIF_IMAGEWIDTH = ((tag[11] & MASK2) << 24)
						| ((tag[10] & MASK2) << 16) | ((tag[9] & MASK2) << 8)
						| (tag[8] & MASK2);
				break;
			case 257:
				imDir.TIF_IMAGELENGTH = ((tag[11] & MASK2) << 24)
						| ((tag[10] & MASK2) << 16) | ((tag[9] & MASK2) << 8)
						| (tag[8] & MASK2);
				break;
			case 258:
				imDir.TIF_BITSPERSAMPLE_LENGTH = ((tag[7] & MASK2) << 24)
						| ((tag[6] & MASK2) << 16) | ((tag[5] & MASK2) << 8)
						| (tag[4] & MASK2);
				imDir.TIF_BITSPERSAMPLE_CHANNEL[0] = ((tag[8] & MASK2) << 0);
				imDir.TIF_BITSPERSAMPLE_CHANNEL[1] = ((tag[9] & MASK2) << 0);
				imDir.TIF_BITSPERSAMPLE_CHANNEL[2] = ((tag[10] & MASK2) << 0);
				break;
			case 259:
				imDir.TIF_COMPRESSION = ((tag[8] & MASK2) << 0);
				break;
			case 262:
				imDir.TIF_PHOTOMETRICINTERPRETATION = ((tag[8] & MASK2) << 0);
				break;
			case 273:
				imDir.TIF_STRIPOFFSETS_LENGTH = ((tag[7] & MASK2) << 24)
						| ((tag[6] & MASK2) << 16) | ((tag[5] & MASK2) << 8)
						| (tag[4] & MASK2);
				stripOffset = ((tag[11] & MASK2) << 24)
						| ((tag[10] & MASK2) << 16) | ((tag[9] & MASK2) << 8)
						| (tag[8] & MASK2);
				break;
			case 277:
				imDir.TIF_SAMPLESPERPIXEL = ((tag[8] & MASK2) << 0);
				break;
			case 279:
				imDir.TIF_STRIPBYTECOUNTS_LENGTH = ((tag[7] & MASK2) << 24)
						| ((tag[6] & MASK2) << 16) | ((tag[5] & MASK2) << 8)
						| (tag[4] & MASK2);
				stripByteOffset = ((tag[11] & MASK2) << 24)
						| ((tag[10] & MASK2) << 16) | ((tag[9] & MASK2) << 8)
						| (tag[8] & MASK2);
				break;
			case 317:
				imDir.TIF_PREDICTOR = ((tag[8] & MASK2) << 0);
				break;
			case 320:
				imDir.TIF_COLORMAP = readColorMap(stream);
				break;
			case 34412:
				imDir.TIF_CZ_LSMINFO_OFFSET = ((tag[11] & MASK2) << 24)
						| ((tag[10] & MASK2) << 16) | ((tag[9] & MASK2) << 8)
						| (tag[8] & MASK2);
				break;
			default:
				break;
			}
		}
		imDir.TIF_STRIPOFFSETS = new long[(int) imDir.TIF_STRIPOFFSETS_LENGTH];
		if (imDir.TIF_STRIPOFFSETS_LENGTH == 1)
			imDir.TIF_STRIPOFFSETS[0] = stripOffset;
		else
			imDir.TIF_STRIPOFFSETS = getIntTable(stream, stripOffset,
					(int) imDir.TIF_STRIPOFFSETS_LENGTH);
		imDir.TIF_STRIPBYTECOUNTS = new long[(int) imDir.TIF_STRIPBYTECOUNTS_LENGTH];
		if (imDir.TIF_STRIPBYTECOUNTS_LENGTH == 1)
			imDir.TIF_STRIPBYTECOUNTS[0] = stripByteOffset;
		else
			imDir.TIF_STRIPBYTECOUNTS = getIntTable(stream, stripByteOffset,
					(int) imDir.TIF_STRIPBYTECOUNTS_LENGTH);

		try {
			stream.seek((int) (currentTagPosition + 12));
			int offset_next_directory = ReaderToolkit.swap(stream.readInt());
			imDir.OFFSET_NEXT_DIRECTORY = offset_next_directory;
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (imDir.TIF_CZ_LSMINFO_OFFSET != 0) {
			imDir.TIF_CZ_LSMINFO = getCZ_LSMINFO(stream,
					imDir.TIF_CZ_LSMINFO_OFFSET, thumb);
		}
		return imDir;
	}

	private byte[][] readColorMap(RandomAccessStream stream) {
		byte[][] buffer = new byte[3][256];
		try {
			for (int i = 0; i < 3; i++)
				for (int j = 0; j < 256; j++)
					buffer[i][j] = (byte) ReaderToolkit.swap(stream.readInt());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return buffer;
	}

	private byte[] readTag(RandomAccessStream stream, int position) {
		byte[] tag = new byte[12];
		try {
			stream.seek(position);
			stream.readFully(tag);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tag;
	}

	private long[] getIntTable(RandomAccessStream stream, long position,
			int count) {
		long[] offsets = new long[count];
		try {
			stream.seek((int) position);
			for (int i = 0; i < count; i++)
				offsets[i] = ReaderToolkit.swap(stream.readInt());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return offsets;
	}

	private CZ_LSMInfo getCZ_LSMINFO(RandomAccessStream stream, long position,
			boolean thumb) {
		CZ_LSMInfo cz = new CZ_LSMInfo();
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
						|| (cz.ScanType == 9)) {
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
		Vector v = new Vector();
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
		Vector vdc = new Vector();
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
		Vector v = new Vector();
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
		Vector vdc = new Vector();
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
		Vector vdc = new Vector();
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
		Vector v = new Vector();
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
		Vector v = new Vector();
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
		Vector vdc = new Vector();

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

	private LinkedHashMap getRecords(RandomAccessStream stream,
			ScanInfoTag tag, Object[][] data, LinkedHashMap lhm) {
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

	public ImagePlus open(RandomAccessStream stream, LsmFileInfo lsmFi,
			boolean verbose, boolean thumb) {
		ImageDirectory firstImDir = (ImageDirectory) lsmFi.imageDirectories
				.get(0);
		if (firstImDir == null) {
			if (verbose)
				IJ.error("LSM ImageDir null.");
			return null;
		} // should not be if it is a true LSM file

		CZ_LSMInfo cz = firstImDir.TIF_CZ_LSMINFO;
		if (cz == null) {
			if (verbose)
				IJ.error("LSM ImageDir null.");
			return null;
		} // should not be, first Directory should have a CZ...

		ImagePlus imp = null;
		switch (cz.ScanType) {
		case 0:
			imp = readStack(stream, lsmFi, cz, thumb);
			return imp;
		case 1:
			imp = readStack(stream, lsmFi, cz, thumb);
			return imp;
		case 2:
			imp = readStack(stream, lsmFi, cz, thumb);
			return imp;
		case 3:
			imp = readStack(stream, lsmFi, cz, thumb);
			return imp;
		case 4:
			imp = readStack(stream, lsmFi, cz, thumb);
			return imp;
		case 5:
			imp = readStack(stream, lsmFi, cz, thumb);
			return imp;
		case 6:
			imp = readStack(stream, lsmFi, cz, thumb);
			return imp;
		default:
			if (verbose)
				IJ.error("Unsupported LSM scantype: " + cz.ScanType);
			break;
		}
		return imp;
	}

	private ImagePlus readStack(RandomAccessStream stream, LsmFileInfo lsmFi,
			CZ_LSMInfo cz, boolean thumb) {
		ImageDirectory firstImDir = (ImageDirectory) lsmFi.imageDirectories
				.get(0);
		lsmFi.url = "";
		lsmFi.fileFormat = FileInfo.TIFF;
		lsmFi.pixelDepth = cz.VoxelSizeZ * 1000000;
		lsmFi.pixelHeight = cz.VoxelSizeY * 1000000;
		lsmFi.pixelWidth = cz.VoxelSizeX * 1000000;
		lsmFi.unit = MasterModel.micrometer;
		lsmFi.valueUnit = MasterModel.micrometer;
		lsmFi.nImages = 1;
		lsmFi.intelByteOrder = true;

		ImageStack st = null;
		int datatype = (int) cz.IntensityDataType;
		if (datatype == 0)
			datatype = cz.OffsetChannelDataTypesValues[0];
		switch (datatype) {
		case 1:
			lsmFi.fileType = FileInfo.GRAY8;
			break;
		case 2:
			lsmFi.fileType = FileInfo.GRAY16_UNSIGNED;
			break;
		case 5:
			lsmFi.fileType = FileInfo.GRAY32_FLOAT;
			break;
		default:
			lsmFi.fileType = FileInfo.GRAY8;
			break;
		}
		ColorModel cm = null;

		if (lsmFi.fileType == FileInfo.COLOR8 && lsmFi.lutSize > 0)
			cm = new IndexColorModel(8, lsmFi.lutSize, lsmFi.reds,
					lsmFi.greens, lsmFi.blues);
		else
			cm = LookUpTable.createGrayscaleColorModel(lsmFi.whiteIsZero);

		if (!thumb)
			st = new ImageStack((int) firstImDir.TIF_IMAGEWIDTH,
					(int) firstImDir.TIF_IMAGELENGTH, cm);
		else
			st = new ImageStack((int) cz.ThumbnailX, (int) cz.ThumbnailY, cm);

		firstImDir = null;
		ImageReader reader = null;
		int flength = 0;
		lsmFi.stripOffsets = new int[1];
		lsmFi.stripLengths = new int[1];
		for (int imageCounter = 0; imageCounter < lsmFi.imageDirectories.size(); imageCounter++) {
			ImageDirectory imDir = (ImageDirectory) lsmFi.imageDirectories
					.get(imageCounter);
			for (int i = 0; i < imDir.TIF_STRIPBYTECOUNTS.length; i++)

				if (imDir.TIF_COMPRESSION == 5) {
					lsmFi.compression = FileInfo.LZW;
					flength = (int) new File(lsmFi.directory
							+ System.getProperty("file.separator")
							+ lsmFi.fileName).length();
					if (imDir.TIF_PREDICTOR == 2)
						lsmFi.compression = FileInfo.LZW_WITH_DIFFERENCING;
				} else
					lsmFi.compression = 0;

			if (!thumb && imDir.TIF_NEWSUBFILETYPE == 0) {
				lsmFi.width = (int) imDir.TIF_IMAGEWIDTH;
				lsmFi.height = (int) imDir.TIF_IMAGELENGTH;
				Object pixels;
				for (int channelCount = 0; channelCount < (int) (cz.DimensionChannels); channelCount++) {
					datatype = (int) cz.IntensityDataType;
					if (datatype == 0)
						datatype = cz.OffsetChannelDataTypesValues[channelCount];
					switch (datatype) {
					case 1:
						lsmFi.fileType = FileInfo.GRAY8;
						break;
					case 2:
						lsmFi.fileType = FileInfo.GRAY16_UNSIGNED;
						break;
					case 5:
						lsmFi.fileType = FileInfo.GRAY32_FLOAT;
						break;
					default:
						lsmFi.fileType = FileInfo.GRAY8;
						break;
					}
					lsmFi.stripLengths[0] = (int) imDir.TIF_STRIPBYTECOUNTS[channelCount];
					lsmFi.stripOffsets[0] = (int) imDir.TIF_STRIPOFFSETS[channelCount];
					reader = new ImageReader(lsmFi);
					if (channelCount < imDir.TIF_STRIPOFFSETS_LENGTH) {

						if (lsmFi.stripLengths[0] + lsmFi.stripOffsets[0] > flength) {
							lsmFi.stripLengths[0] = flength
									- lsmFi.stripOffsets[0];
						}
						try {
							stream.seek(lsmFi.stripOffsets[0]);
						} catch (IOException e) {

							e.printStackTrace();
						}
						pixels = reader.readPixels((InputStream) stream);
						st.addSlice("", pixels);
					}
				}
			} else if (thumb && imDir.TIF_NEWSUBFILETYPE == 1) {
				// ONLY IF THUMBS
				lsmFi.width = (int) imDir.TIF_IMAGEWIDTH;
				lsmFi.height = (int) imDir.TIF_IMAGELENGTH;
				lsmFi.fileType = FileInfo.COLOR8;
				reader = new ImageReader(lsmFi);
				Object pixels;
			/*	byte[][] b = imDir.TIF_COLORMAP;
				lsmFi.reds = b[0];
				lsmFi.greens = b[1];
				lsmFi.blues = b[2];*/
				int channels = (int) (cz.DimensionChannels);
				channels = 1; // only read the first channel for the thumbs.
								// --> speed!
				for (int channelCount = 0; channelCount < channels; channelCount++) {
					lsmFi.stripLengths[0] = (int) imDir.TIF_STRIPBYTECOUNTS[channelCount];
					lsmFi.stripOffsets[0] = (int) imDir.TIF_STRIPOFFSETS[channelCount];
					if (channelCount < imDir.TIF_STRIPOFFSETS_LENGTH) {
						try {
							stream.seek(lsmFi.stripOffsets[0]);
						} catch (IOException e) {

							e.printStackTrace();
						}
						pixels = reader.readPixels((InputStream) stream);
						st.addSlice("", pixels);
					}
				}
				// break out of for loop, speed
				// imageCounter = lsmFi.imageDirectories.size();
			}
		}
		IJ.showProgress(1.0);
		ImagePlus imp = new ImagePlus(lsmFi.fileName, st);
		imp.setDimensions((int) cz.DimensionChannels, (int) cz.DimensionZ,
				(int) cz.DimensionTime);
		if (cz.DimensionChannels >= 2
				&& (imp.getStackSize() % cz.DimensionChannels) == 0) {
			imp = new CompositeImage(imp, CompositeImage.COLOR);
		}
		imp.setFileInfo(lsmFi);
		Calibration cal = new Calibration();
		cal.setUnit(lsmFi.unit);
		cal.pixelDepth = lsmFi.pixelDepth;
		cal.pixelHeight = lsmFi.pixelHeight;
		cal.pixelWidth = lsmFi.pixelWidth;
		imp.setCalibration(cal);
		Color[] color = new Color[2];
		color[0] = new Color(0, 0, 0);
		if (!thumb) {
			for (int channel = 0; channel < (int) cz.DimensionChannels; channel++) {
				int r = (int) (cz.channelNamesAndColors.Colors[channel] & 255);
				int g = (int) ((cz.channelNamesAndColors.Colors[channel] >> 8) & 255);
				int b = (int) ((cz.channelNamesAndColors.Colors[channel] >> 16) & 255);
				color[1] = new Color(r, g, b);
				if (r == 0 && g == 0 && b == 0)
					color[1] = Color.white;
				if (!thumb)
					ReaderToolkit.applyColors(imp, channel, color, 2);
			}
		}
		if (thumb){
			color[1] = Color.white;
			ReaderToolkit.applyColors(imp, 1, color, 2);
			//ReaderToolkit.showLut(imp, 1, lsmFi, true);
		}
		if (imp.getOriginalFileInfo().fileType == FileInfo.GRAY16_UNSIGNED) {
			double min = imp.getProcessor().getMin();
			double max = imp.getProcessor().getMax();
			imp.getProcessor().setMinAndMax(min, max);
		}

		int stackPosition = 1;
		for (int i = 1; i <= cz.DimensionTime; i++)
			for (int j = 1; j <= cz.DimensionZ; j++)
				for (int k = 1; k <= cz.DimensionChannels; k++) {
					// imp.setPosition(k, j, i);
					// int stackPosition = imp.getCurrentSlice();
					if (stackPosition <= imp.getStackSize()) {
						String label = cz.channelNamesAndColors.ChannelNames[k - 1];
						st.setSliceLabel(label, stackPosition++);
					}
				}
		// setInfo(imp, lsmFi);
		// imp.show();
		return imp;
	}

	/** ******************************************************************************************* */
	private class ScanInfoTag {
		public long entry = 0;

		public long type = 0;

		public long size = 0;
	}

	public CZ_LSMInfo readCz(File f) {
		RandomAccessFile file;
		LsmFileInfo lsm;
		try {
			file = new RandomAccessFile(f, "r");
			RandomAccessStream stream = new RandomAccessStream(file);
			lsm = new LsmFileInfo(masterModel);
			lsm.fileName = f.getName();
			lsm.directory = f.getParent();
			if (isLSMfile(stream)) {
				ImageDirectory imDir = readImageDirectoy(stream, 8, false);
				file.close();
				return imDir.TIF_CZ_LSMINFO;
			} else {
				IJ.error("Not an LSM file.");
				file.close();
				return null;
			}
		} catch (IOException e) {
			IJ.error("IOException when trying to read "+f);
			return null;
		}
	}
}
