package org.imagearchive.lsm.toolbox;

import ij.ImagePlus;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.imagearchive.lsm.reader.info.ImageDirectory;
import org.imagearchive.lsm.reader.info.LSMFileInfo;
import org.imagearchive.lsm.toolbox.info.CZLSMInfoExtended;
import org.imagearchive.lsm.toolbox.info.scaninfo.DataType;
import org.imagearchive.lsm.toolbox.info.scaninfo.Recording;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DomXmlExporter {
	private Document doc;
	private static String comment = "Created with LSM_Toolbox v"
			+ MasterModel.VERSION + " (C) Patrick Pirrotte 2008";

	public String getXML(String filename, boolean filter){
		Reader reader = ServiceMediator.getReader();
		ImagePlus imp = reader.open(filename, false);
		reader.updateMetadata(imp);
		LSMFileInfo lsm = (LSMFileInfo) imp.getOriginalFileInfo();
		ImageDirectory imDir = (ImageDirectory) lsm.imageDirectories.get(0);
		CZLSMInfoExtended cz = (CZLSMInfoExtended) imDir.TIF_CZ_LSMINFO;
		return buildTree(cz,filter);
	}

	public String buildTree(CZLSMInfoExtended cz, boolean filter) {

		try {
			ArrayList<Recording> recordings = cz.scanInfo.recordings;
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = factory.newDocumentBuilder();
			doc = docBuilder.newDocument();
			doc.appendChild(doc.createComment(comment));
			Element root = doc.createElement("org.imagearchive.lsm.toolbox");

			Element child = doc.createElement("CarlZeiss");
			addNode(child, "DimensionX", DataType.LONG, cz.DimensionX);
			addNode(child, "DimensionY", DataType.LONG, cz.DimensionY);
			addNode(child, "DimensionZ", DataType.LONG, cz.DimensionZ);
			addNode(child, "DimensionChannels", DataType.LONG,
					cz.DimensionChannels);
			addNode(child, "DimensionTime", DataType.LONG, cz.DimensionTime);
			addNode(child, "IntensityDataType", DataType.LONG,
					cz.IntensityDataType);
			addNode(child, "ThumbnailX", DataType.LONG, cz.ThumbnailX);
			addNode(child, "ThumbnailY", DataType.LONG, cz.ThumbnailY);
			addNode(child, "VoxelSizeX", DataType.DOUBLE, cz.VoxelSizeX);
			addNode(child, "VoxelSizeY", DataType.DOUBLE, cz.VoxelSizeY);
			addNode(child, "VoxelSizeZ", DataType.DOUBLE, cz.VoxelSizeZ);
			addNode(child, "OriginX", DataType.DOUBLE, cz.OriginX);
			addNode(child, "OriginY", DataType.DOUBLE, cz.OriginY);
			addNode(child, "OriginZ", DataType.DOUBLE, cz.OriginZ);
			addNode(child, "ScanType", DataType.INTEGER, cz.OriginZ);
			addNode(child, "SpectralScan", DataType.INTEGER, cz.SpectralScan);
			addNode(child, "DataType", DataType.LONG, cz.DataType);
			addNode(child, "TimeIntervall", DataType.DOUBLE, cz.TimeIntervall);
			addNode(child, "DisplayAspectX", DataType.DOUBLE, cz.DisplayAspectX);
			addNode(child, "DisplayAspectY", DataType.DOUBLE, cz.DisplayAspectY);
			addNode(child, "DisplayAspectZ", DataType.DOUBLE, cz.DisplayAspectZ);
			addNode(child, "DisplayAspectTime", DataType.DOUBLE,
					cz.DisplayAspectTime);
			addNode(child, "ToolbarFlags", DataType.DOUBLE, cz.OriginZ);

			root.appendChild(child);

			for (int i = 0; i < recordings.size(); i++) {
				Recording recording = (Recording) recordings.get(i);
				Element rec = doc.createElement("Recording");
				rec = populateNode(rec, Recording.data, recording.records);
				if (recording.lasers != null) {
					Element las = doc.createElement("Lasers");
					for (int j = 0; j < recording.lasers.length; j++) {

						LinkedHashMap<String, Object> records = recording.lasers[j].records;
						LinkedHashMap<String, Object> m = records;
						Object o = m.get("LASER_ACQUIRE");
						if (!filter || (o == null)
								|| (o != null & o.toString().equals("-1"))) {
							Element lasNode = doc.createElement("Laser");
							lasNode = populateNode(lasNode,
									recording.lasers[j].data, records);
							las.appendChild(lasNode);
						}
					}
					rec.appendChild(las);
				}

				if (recording.tracks != null) {

					Element tra = doc.createElement("Tracks");
					for (int j = 0; j < recording.tracks.length; j++) {

						Element traNode = doc.createElement("Track");
						traNode = populateNode(traNode,
								recording.tracks[j].data,
								recording.tracks[j].records);
						if (recording.tracks[j].detectionChannels != null) {
							if (recording.tracks[j].detectionChannels.length > 0) {

								Element cha = doc
										.createElement("DetectionChannels");
								for (int k = 0; k < recording.tracks[j].detectionChannels.length; k++) {
									LinkedHashMap<String, Object> m = recording.tracks[j].detectionChannels[k].records;
									Object o = m.get("ACQUIRE");
									if (!filter
											|| (o == null)
											|| (o != null & o.toString()
													.equals("-1"))) {
										Element chaNode = doc
												.createElement("DetectionChannel");
										chaNode = populateNode(
												chaNode,
												recording.tracks[j].detectionChannels[k].data,
												recording.tracks[j].detectionChannels[k].records);
										cha.appendChild(chaNode);
									}
								}
								traNode.appendChild(cha);
							}
						}

						if (recording.tracks[j].illuminationChannels != null) {
							if (recording.tracks[j].illuminationChannels.length > 0) {
								Element ill = doc
										.createElement("IlluminationChannels");
								for (int k = 0; k < recording.tracks[j].illuminationChannels.length; k++) {
									LinkedHashMap<String, Object> m = recording.tracks[j].illuminationChannels[k].records;
									Object o = m.get("ACQUIRE");
									if (!filter
											|| (o == null)
											|| (o != null & o.toString()
													.equals("-1"))) {
										Element illNode = doc
												.createElement("IlluminationChannel");
										illNode = populateNode(
												illNode,
												recording.tracks[j].illuminationChannels[k].data,
												recording.tracks[j].illuminationChannels[k].records);
										ill.appendChild(illNode);
									}
								}
								traNode.appendChild(ill);
							}
						}

						if (recording.tracks[j].beamSplitters != null) {
							if (recording.tracks[j].beamSplitters.length > 0) {
								Element bsp = doc
										.createElement("BeamSplitters");
								for (int k = 0; k < recording.tracks[j].beamSplitters.length; k++) {

									Element bspNode = doc
											.createElement("BeamSplitter");
									bspNode = populateNode(
											bspNode,
											recording.tracks[j].beamSplitters[k].data,
											recording.tracks[j].beamSplitters[k].records);
									bsp.appendChild(bspNode);
								}
								traNode.appendChild(bsp);
							}
						}

						if (recording.tracks[j].dataChannels != null) {
							if (recording.tracks[j].dataChannels.length > 0) {
								Element dch = doc.createElement("DataChannels");
								for (int k = 0; k < recording.tracks[j].dataChannels.length; k++) {
									LinkedHashMap<String, Object> m = recording.tracks[j].dataChannels[k].records;
									Object o = m.get("ACQUIRE");
									if (!filter
											|| (o == null)
											|| (o != null & o.toString()
													.equals("-1"))) {
										Element dchNode = doc
												.createElement("DataChannel");
										dchNode = populateNode(
												dchNode,
												recording.tracks[j].dataChannels[k].data,
												recording.tracks[j].dataChannels[k].records);
										dch.appendChild(dchNode);
									}
								}
								traNode.appendChild(dch);
							}
						}
						if (traNode.hasChildNodes())
							tra.appendChild(traNode);
					}
					rec.appendChild(tra);
				}

				if (recording.markers != null) {
					Element mar = doc.createElement("Markers");
					for (int j = 0; j < recording.markers.length; j++) {
						Element marNode = doc.createElement("Marker");
						marNode = populateNode(marNode,
								recording.markers[j].data,
								recording.markers[j].records);
						mar.appendChild(marNode);
					}
					rec.appendChild(mar);
				}
				if (recording.timers != null) {
					Element tim = doc.createElement("Timers");
					for (int j = 0; j < recording.timers.length; j++) {
						Element timNode = doc.createElement("Timer");
						timNode = populateNode(timNode,
								recording.markers[j].data,
								recording.markers[j].records);
						tim.appendChild(timNode);
					}
					rec.appendChild(tim);
				}
				root.appendChild(rec);
			}

			doc.appendChild(root);

			TransformerFactory transfac = TransformerFactory.newInstance();
			Transformer trans = transfac.newTransformer();
			trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			trans.setOutputProperty(OutputKeys.INDENT, "yes");

			StringWriter sw = new StringWriter();
			StreamResult result = new StreamResult(sw);
			DOMSource source = new DOMSource(doc);
			trans.transform(source, result);
			String xmlString = sw.toString();
			return xmlString;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * private Element populate(Element lasNode, Object[][] data, LinkedHashMap
	 * records) { for (int i = 0; i < data.length; i++) { String s =
	 * records.get(data[i][2]).toString(); lasNode.setAttribute((String)
	 * data[i][2], s); } return lasNode; }
	 */
	private Element populateNode(Element pNode, Object[][] data,
			LinkedHashMap<String, Object> records) {
		for (int i = 0; i < data.length; i++) {
			// String s = records.get(data[i][2]).toString();
			Object o = records.get(data[i][2]);
			if (o != null) {
				String s = "";
				if ((Enum) data[i][1] == DataType.STRING)
					s = o.toString();
				if ((Enum) data[i][1] == DataType.INTEGER)
					s = Integer.toString((Integer) o);
				if ((Enum) data[i][1] == DataType.LONG)
					s = Long.toString((Long) o);
				if ((Enum) data[i][1] == DataType.DOUBLE)
					s = Double.toString((Double) o);

				String id = Long.toString((Long) data[i][0]);
				Element node = doc.createElement(((String) data[i][2]));
				// node.setAttribute("name", (String) data[i][2]);
				node.setAttribute("id", id);
				node.setAttribute("type", ((Enum) data[i][1]).toString());
				node.appendChild(doc.createTextNode(s));
				pNode.appendChild(node);
			}
		}
		return pNode;
	}

	private Element addNode(Element pNode, String name, Enum e, Object value) {
		Element node = doc.createElement(name);
		// node.setAttribute("name", name);
		node.setAttribute("type", e.toString());
		// Comment comment = doc.createComment(value.toString());
		node.appendChild(doc.createTextNode(value.toString()));
		pNode.appendChild(node);
		return pNode;
	}

	private String getDataType(Enum e) {
		return e.toString();
	}
}
