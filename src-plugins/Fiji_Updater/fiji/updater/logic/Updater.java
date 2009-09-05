package fiji.updater.logic;

import com.jcraft.jsch.JSchException;

import fiji.updater.logic.FileUploader.SourceFile;
import fiji.updater.logic.FileUploader.UploadListener;

import fiji.updater.util.Compressor;
import fiji.updater.util.DependencyAnalyzer;
import fiji.updater.util.Util;

import ij.IJ;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/*
 * This class is responsible for writing updates to server, upon given the
 * updated plugin records.
 *
 * Note: Plugins are uploaded differently
 * - Non-Fiji plugins & new versions of Fiji Plugins will have files AND
 *   details uploaded
 * - Uninstalled & up-to-date plugins will ONLY have their details uploaded
 *   (i.e.: XML file)
 */
public class Updater {
	protected long xmlLastModified;
	protected FileUploader uploader;

	public Updater(long xmlLastModified) {
		// TODO: use lastModified() of lock file as timestamp for new
		// plugins
		this.xmlLastModified = xmlLastModified;
	}

	public void setUploader(FileUploader uploader) {
		this.uploader = uploader;
	}

	public synchronized boolean setLogin(String username, String password) {
		try {
			uploader = new SSHFileUploader(username, password,
				PluginManager.UPDATE_DIRECTORY);
			return true;
		} catch (JSchException e) {
			IJ.error("Failed to login");
			return false;
		}
	}

	public void upload(UploadListener uploadListener)
			throws Exception  {
		if (uploadListener != null)
			uploader.addListener(uploadListener);

		String backup = Util.prefix(PluginManager.XML_BACKUP);
		String compressed = Util.prefix(PluginManager.XML_COMPRESSED);
		String txt = Util.prefix(PluginManager.TXT_FILENAME);
		generateAndValidateXML(backup);
		// TODO: only save _compressed_ backup, and not as db.bak!
		compress(backup, compressed);
		// TODO: do no save text file at all!
		saveTextFile(txt);

		// TODO: rename "UpdateSource" to "Transferable", reuse!
		List<SourceFile> files = new ArrayList<SourceFile>();
		files.add(new UpdateSource(compressed,
					PluginManager.XML_LOCK, "C0444"));
		files.add(new UpdateSource(txt,
					PluginManager.TXT_FILENAME, "C0644"));
		for (PluginObject plugin :
				PluginCollection.getInstance().toUpload())
			files.add(new UpdateSource(plugin));
		uploader.upload(xmlLastModified, files);

		// No errors thrown -> just remove temporary files
		new File(backup).delete();
		new File(Util.prefix(PluginManager.TXT_FILENAME)).delete();
	}

	protected void compress(String uncompressed, String compressed)
			throws IOException {
		FileOutputStream out = new FileOutputStream(compressed);
		FileInputStream in = new FileInputStream(uncompressed);
		Compressor.compressAndSave(Compressor.readStream(in), out);
		out.close();
		in.close();
	}

	// TODO: in-memory only, please
	protected void saveTextFile(String path) throws FileNotFoundException {
		PrintStream out = new PrintStream(path);
		for (PluginObject plugin :
				PluginCollection.getInstance().fijiPlugins())
			out.println(plugin.getFilename() + " "
					+ plugin.getTimestamp() + " "
					+ plugin.getChecksum());
		out.close();
	}

	// TODO: this _must_ go to XMLFileWriter (together with the validating stuff)
	protected TransformerHandler handler;
	protected void generateAndValidateXML(String path) throws SAXException,
			TransformerConfigurationException, IOException,
			ParserConfigurationException {
		FileOutputStream out = new FileOutputStream(path);
		XMLFileHandler xmlHandler = new XMLFileHandler(out);
		handler = xmlHandler.getXMLHandler();

		handler.startDocument();
		AttributesImpl attr = new AttributesImpl();
		handler.startElement("", "", "pluginRecords", attr);
		for (PluginObject plugin :
				PluginCollection.getInstance().fijiPlugins()) {
			attr.clear();
			setAttribute(attr, "filename", plugin.filename);
			handler.startElement("", "", "plugin", attr);

			attr.clear();
			setAttribute(attr, "checksum", plugin.getChecksum());
			setAttribute(attr, "timestamp", plugin.getTimestamp());
			setAttribute(attr, "filesize", plugin.filesize);
			handler.startElement("", "", "version", attr);
			if (plugin.description != null)
				writeSimpleTag("description",
						plugin.description);

			for (Dependency dependency : plugin.getDependencies()) {
				attr.clear();
				setAttribute(attr, "filename",
						dependency.filename);
				setAttribute(attr, "timestamp",
						dependency.timestamp);
				if (dependency.relation !=
						Dependency.Relation.AT_LEAST)
					setAttribute(attr, "relation",
						dependency.relation.toString());
				writeSimpleTag("dependency", null, attr);
			}

			writeSimpleTags("link", plugin.getLinks());
			writeSimpleTags("author", plugin.getAuthors());
			handler.endElement("", "", "version");

			for (PluginObject.Version version :
					plugin.getPrevious()) {
				attr.clear();
				setAttribute(attr, "timestamp",
						version.timestamp);
				setAttribute(attr, "checksum",
						version.checksum);
				writeSimpleTag("previous-version", null, attr);
			}
			handler.endElement("", "", "plugin");
		}
		handler.endElement("", "", "pluginRecords");
		handler.endDocument();
		out.close();

		FileInputStream in = new FileInputStream(path);
		xmlHandler.validateXMLContents(in);
		in.close();
	}

	protected void setAttribute(AttributesImpl attributes,
			String key, long value) {
		setAttribute(attributes, key, "" + value);
	}

	protected void setAttribute(AttributesImpl attributes,
			String key, String value) {
		attributes.addAttribute("", "", key, "CDATA", value);
	}

	protected void writeSimpleTags(String tagName, Iterable<String> values)
			throws SAXException {
		for (String value : values)
			writeSimpleTag(tagName, value);
	}

	protected void writeSimpleTag(String tagName, String value)
			throws SAXException {
		writeSimpleTag(tagName, value, new AttributesImpl());
	}

	protected void writeSimpleTag(String tagName, String value,
				AttributesImpl attributes)
			throws SAXException {
		handler.startElement("", "", tagName, attributes);
		if (value != null)
			handler.characters(value.toCharArray(),
					0, value.length());
		handler.endElement("", "", tagName);
	}
}
