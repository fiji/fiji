package fiji.plugin.trackmate.io;

import ij.IJ;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jdom2.Attribute;
import org.jdom2.DataConversionException;
import org.jdom2.Element;

import fiji.plugin.trackmate.Logger;

/**
 * A collection of static utilities for the input/output of xml files.
 * @author Jean-Yves Tinevez
 *
 */
public class IOUtils {
	



	/**
	 * Prompt the user for a target xml file.
	 *  
	 * @param file  a default file, will be used to display a default choice in the file chooser.
	 * @param parent  the {@link Frame} to lock on this dialog.
	 * @param logger  a {@link Logger} to report what is happening.
	 * @return  the selected file, or <code>null</code> if the user pressed the "cancel" button.
	 */
	public static File askForFile(File file, Frame parent, Logger logger) {

		if(IJ.isMacintosh()) {
			// use the native file dialog on the mac
			FileDialog dialog =	new FileDialog(parent, "Save to a XML file", FileDialog.SAVE);
			dialog.setDirectory(file.getParent());
			dialog.setFile(file.getName());
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".xml");
				}
			};
			dialog.setFilenameFilter(filter);
			dialog.setVisible(true);
			String selectedFile = dialog.getFile();
			if (null == selectedFile) {
				logger.log("Save data aborted.\n");
				return null;
			}
			if (!selectedFile.endsWith(".xml"))
				selectedFile += ".xml";
			file = new File(dialog.getDirectory(), selectedFile);
		} else {
			JFileChooser fileChooser = new JFileChooser(file.getParent());
			fileChooser.setSelectedFile(file);
			FileNameExtensionFilter filter = new FileNameExtensionFilter("XML files", "xml");
			fileChooser.setFileFilter(filter);

			int returnVal = fileChooser.showSaveDialog(parent);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				file = fileChooser.getSelectedFile();
			} else {
				logger.log("Save data aborted.\n");
				return null;  	    		
			}
		}
		return file;
	}




	/** 
	 * Read and return an integer attribute from a JDom {@link Element}, and substitute a default value of 0
	 * if the attribute is not found or of the wrong type.
	 */
	public static final int readIntAttribute(Element element, String name, Logger logger) {
		return readIntAttribute(element, name, logger, 0);
	}

	public static final int readIntAttribute(Element element, String name, Logger logger, int defaultValue) {
		int val = defaultValue;
		Attribute att = element.getAttribute(name);
		if (null == att) {
			logger.error("Could not find attribute "+name+" for element "+element.getName()+", substituting default value: "+defaultValue+".\n");
			return val;
		}
		try {
			val = att.getIntValue();
		} catch (DataConversionException e) {	
			logger.error("Cannot read the attribute "+name+" of the element "+element.getName()+", substituting default value: "+defaultValue+".\n");
		}
		return val;
	}

	public static final double readFloatAttribute(Element element, String name, Logger logger) {
		double val = 0;
		Attribute att = element.getAttribute(name);
		if (null == att) {
			logger.error("Could not find attribute "+name+" for element "+element.getName()+", substituting default value.\n");
			return val;
		}
		try {
			val = att.getFloatValue();
		} catch (DataConversionException e) {	
			logger.error("Cannot read the attribute "+name+" of the element "+element.getName()+", substituting default value.\n"); 
		}
		return val;
	}

	public static final double readDoubleAttribute(Element element, String name, Logger logger) {
		double val = 0;
		Attribute att = element.getAttribute(name);
		if (null == att) {
			logger.error("Could not find attribute "+name+" for element "+element.getName()+", substituting default value.\n");
			return val;
		}
		try {
			val = att.getDoubleValue();
		} catch (DataConversionException e) {	
			logger.error("Cannot read the attribute "+name+" of the element "+element.getName()+", substituting default value.\n"); 
		}
		return val;
	}

	public static final boolean readBooleanAttribute(Element element, String name, Logger logger) {
		boolean val = false;
		Attribute att = element.getAttribute(name);
		if (null == att) {
			logger.error("Could not find attribute "+name+" for element "+element.getName()+", substituting default value.\n");
			return val;
		}
		try {
			val = att.getBooleanValue();
		} catch (DataConversionException e) {	
			logger.error("Cannot read the attribute "+name+" of the element "+element.getName()+", substituting default value.\n"); 
		}
		return val;
	}

}
