package register_virtual_stack;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFileChooser;

import mpicbg.trakem2.transform.CoordinateTransform;

import ij.IJ;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;

public class Transform_Virtual_Stack_MT implements PlugIn 
{

	/** working directory path */
	public static String currentDirectory = (OpenDialog.getLastDirectory() == null) ? 
			OpenDialog.getDefaultDirectory() : OpenDialog.getLastDirectory();

	//---------------------------------------------------------------------------------
	/**
	 * Plug-in run method
	 * 
	 * @param arg plug-in arguments
	 */
	public void run(String arg) 
	{
		// Choose source image folder
		JFileChooser chooser = new JFileChooser();
		if(currentDirectory != null)
			chooser.setCurrentDirectory(new java.io.File(currentDirectory));
		else
			chooser.setCurrentDirectory(new java.io.File("."));
		chooser.setDialogTitle("Choose directory with Source images");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);
		if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
			return;

		String source_dir = chooser.getSelectedFile().toString();
		if (null == source_dir) 
			return;
		source_dir = source_dir.replace('\\', '/');
		if (!source_dir.endsWith("/")) source_dir += "/";

		// Choose target folder to save images into
		chooser.setDialogTitle("Choose directory to store Output images");
		if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
			return;

		String target_dir = chooser.getSelectedFile().toString();
		if (null == target_dir) 
			return;
		target_dir = target_dir.replace('\\', '/');
		if (!target_dir.endsWith("/")) target_dir += "/";


		// Choose input folder where transform files are stored
		chooser.setDialogTitle("Choose directory with Transform files");
		if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
			return;

		String transf_dir = chooser.getSelectedFile().toString();
		if (null == transf_dir) 
			return;
		transf_dir = transf_dir.replace('\\', '/');
		if (!transf_dir.endsWith("/")) transf_dir += "/";

		// Execute transformation
		exec(source_dir, target_dir, transf_dir);

	}
	//---------------------------------------------------------------------------------
	/**
	 * Transform images in the source directory applying transform files from a specific directory.
	 * 
	 * @param source_dir folder with input (source) images.
	 * @param target_dir folder to store output (transformed) images.
	 * @param transf_dir folder with transform files.
	 * @return true for correct execution, false otherwise.
	 */
	public boolean exec(
			final String source_dir, 
			final String target_dir, 
			final String transf_dir) 
	{
		// Get source file listing
		final String exts = ".tif.jpg.png.gif.tiff.jpeg.bmp.pgm";
		final String[] src_names = new File(source_dir).list(new FilenameFilter() 
		{
			public boolean accept(File dir, String name) 
			{
				int idot = name.lastIndexOf('.');
				if (-1 == idot) return false;
				return exts.contains(name.substring(idot).toLowerCase());
			}
		});
		Arrays.sort(src_names);
		
		// Get transform file listing
		final String ext_xml = ".xml";
		final String[] transf_names = new File(transf_dir).list(new FilenameFilter() 
		{
			public boolean accept(File dir, String name) 
			{
				int idot = name.lastIndexOf('.');
				if (-1 == idot) return false;
				return ext_xml.contains(name.substring(idot).toLowerCase());
			}
		});
		Arrays.sort(transf_names);
		
		// Check the number of input (source) files and transforms.
		if(transf_names.length != src_names.length)
		{
			IJ.error("The number of source and transform files must be equal!");
			return false;
		}
		
		// Read transforms
		CoordinateTransform[] transform = new CoordinateTransform[transf_names.length];
		for(int i = 0; i < transf_names.length; i ++)
		{
			transform[i] = readCoordinateTransform(transf_dir + transf_names[i]);
			if(transform[i] == null)
			{
				IJ.error("Error when reading transform from file: " + transf_dir + transf_names[i]);
				return false;
			}
		}
		
		// Apply transforms
		
		// Executor service to run concurrent tasks
		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		// Create transformed images
		IJ.showStatus("Calculating transformed images...");
		if(Register_Virtual_Stack_MT.createResults(source_dir, src_names, target_dir, null, exe, transform) == false)
		{
			IJ.log("Error when creating transformed images");
			exe.shutdownNow();
			return false;
		}
		
		exe.shutdownNow();
		
		
		return true;
	}
	
	//---------------------------------------------------------------------------------
	/**
	 * Read coordinate transform from file (generated in Register_Virtual_Stack)
	 * @param filename complete file name (including path)
	 * @return true if the coordinate transform was properly read, false otherwise.
	 */
	public CoordinateTransform readCoordinateTransform(String filename) 
	{
		
		try 
		{
			final FileReader fr = new FileReader(filename);
			final BufferedReader br = new BufferedReader(fr);
			String line = null;
			while ((line = br.readLine()) != null) 
			{
				int index = -1;
				if( (index = line.indexOf("=")) != -1)
				{
					final int index2 = line.indexOf("\"", index+2); 
					final String ct_class = line.substring(index+2, index2);
					final CoordinateTransform ct = (CoordinateTransform) Class.forName(ct_class).newInstance();
					final int index3 = line.indexOf("=", index2+1);
					final int index4 = line.indexOf("\"", index3+2); 
					final String data = line.substring(index3+2, index4);
					ct.init(data);
					return ct;
				}
			}
		
		} catch (FileNotFoundException e) {
			IJ.error("File not found exception" + e);
			
		} catch (IOException e) {
			IJ.error("IOException exception" + e);
			
		} catch (NumberFormatException e) {
			IJ.error("Number format exception" + e);
			
		} catch (InstantiationException e) {
			IJ.error("Instantiation exception" + e);
			
		} catch (IllegalAccessException e) {
			IJ.error("Illegal access exception" + e);
			
		} catch (ClassNotFoundException e) {
			IJ.error("Class not found exception" + e);
			
		}
		return null;
	}

}// end class Register_Virtual_Stack_MT
