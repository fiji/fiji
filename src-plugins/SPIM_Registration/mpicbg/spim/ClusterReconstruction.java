package mpicbg.spim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;

import mpicbg.spim.io.ConfigurationParserGeneral;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.ProgramConfiguration;
import mpicbg.spim.io.TextFileAccess;

public class ClusterReconstruction 
{
	public static void main(String[] args) 
	{
        // read&parse configuration file
		ProgramConfiguration conf = null;
        try
        {
            conf = ConfigurationParserGeneral.parseFile( "config/configuration.txt" );
        } catch (Exception e)
        {
            IOFunctions.printErr("Cannot open configuration file: " + e);
            e.printStackTrace();
            return;
        }
        
        conf.printParameters();
        
        for (int timePointIndex = 0; timePointIndex < conf.timepoints.length; timePointIndex++)
        {
        	//
        	// create spim configuration file
        	//
        	String spimConfigFile = conf.baseFolder + conf.configFolder + conf.configFileTemplate + "_" + timePointIndex + ".txt";
        	String spimConfigTemplate = conf.baseFolder + conf.configFolder + conf.configFileTemplate;
        	try
        	{
	        	BufferedReader in = TextFileAccess.openFileRead(spimConfigTemplate);
	        	PrintWriter out = TextFileAccess.openFileWrite(spimConfigFile);
	        	
	        	while (in.ready())
	        	{
	        		String line = in.readLine();
	        		
	        		if (line.startsWith("Time Point Pattern = "))
	        			line = "Time Point Pattern = \"" + conf.timepoints[timePointIndex] + "\"";
	        		
	        		out.println(line);
	        	}
	        	
	        	in.close();
	        	out.close();
        	}
        	catch (Exception e)
        	{
        		IOFunctions.printErr("Cannot read spim configuration template from: '" + spimConfigTemplate + "'");
        		IOFunctions.printErr("Cannot write spim configuration to: '" + spimConfigFile + "'");
        		IOFunctions.printErr(e);
        		e.printStackTrace();
        		System.exit(0);
        	}
        	
        	//
        	// write job files
        	//
        	
    		String jobFile = conf.baseFolder + conf.jobFolder + "jobspim_" + timePointIndex;
        	try
        	{
	        	PrintWriter job = TextFileAccess.openFileWrite(jobFile);
	        	
	        	job.println("#!/bin/sh");
	        	job.println("#$ -S /bin/sh");
	        	job.println("cd " + conf.baseFolder);
	        	job.println(conf.javaExecutable + " " + conf.javaArguments + " -cp " + conf.binariesFolder + 
	        			    getLibraries(conf.baseFolder + conf.librariesFolder) + ":/home/tomancak/ImageJ/ij.jar" +  
	        			    " mpi.fruitfly.spim.Reconstruction " + spimConfigFile);
	        	
	        	job.close();
        	}
        	catch (Exception e)
        	{
        		IOFunctions.printErr("Cannot write spim job file: '" + jobFile + "'");
        		IOFunctions.printErr(e);
        		e.printStackTrace();
        		System.exit(0);
        	}   
        }
        
    	//
    	// write submission script
    	//
		String submissionFile = conf.baseFolder + "submitjobs";
    	try
    	{
        	PrintWriter submissionScript = TextFileAccess.openFileWrite(submissionFile);
        	
        	submissionScript.println("#!/bin/sh");	   
        	submissionScript.println("");
        	
        	for (int timePointIndex = 0; timePointIndex < conf.timepoints.length; timePointIndex++)
        		submissionScript.println("qsub -P spim " + conf.baseFolder + conf.jobFolder + "jobspim_" + timePointIndex);

        	submissionScript.close();
    	}
    	catch (Exception e)
    	{
    		IOFunctions.printErr("Cannot write submission file: '" + submissionFile + "'");
    		IOFunctions.printErr(e);
    		e.printStackTrace();
    		System.exit(0);
    	}
	}

	protected static String getLibraries(String directory)
	{
		String libString = "";
		String[] libraries = ClusterReconstruction.getDirectoryListingOfFiles(directory, ".jar");
		
		for (String lib : libraries)
			libString = libString + ":" + lib;
		
		return libString;
	}
	
	private static String[] getDirectoryListingOfFiles(String directory, final String filterFileName)
	{
		FilenameFilter filter = new FilenameFilter()
		{
			public boolean accept( File dir, String name )
			{
				if (name.toUpperCase().endsWith(filterFileName.toUpperCase()))
				{
					File f = new File(dir, name);
					
					if (f.isFile())
						return true;
					else
						return false;
				}
				else
				{
					return false;
				}
			}
		};

		File dir = new File(directory);
		String[] children = dir.list(filter);	
		java.util.Arrays.sort(children);

		for (int i = 0; i < children.length; i++)
		{
			children[i] = dir + "/" + children[i];
			children[i] = children[i].replace('\\', '/');
		}		
		return children;
	}

	public static void getMovements(String directory, String extension)
	{
		String[] regFiles = getDirectoryListingOfFiles(directory, extension);
		IOFunctions.println("Timepoint" + "\t" + "x" + "\t" + "y" + "\t" + "z");

		String lasttimepoint = "";
		for (String file : regFiles)
		{
			float x = 0, y = 0, z = 0;
			
			String timepoint = file.substring(file.indexOf("TL")+2, file.indexOf("Angle") - 1);
			
			try
			{
				BufferedReader in = TextFileAccess.openFileRead(file);
				
				while (in.ready())
				{
					String line = in.readLine().trim();
					
					if (line.startsWith("m03:"))
						x = Float.parseFloat(line.substring(line.indexOf(":") + 1, line.length()));
					else if (line.startsWith("m13:"))
						y = Float.parseFloat(line.substring(line.indexOf(":") + 1, line.length()));
					else if (line.startsWith("m23:"))
						z = Float.parseFloat(line.substring(line.indexOf(":") + 1, line.length()));
				}
				in.close();
				
				if  (!timepoint.equals(lasttimepoint))
					IOFunctions.println(timepoint + "\t" + x + "\t" + y + "\t" + z);
				
				lasttimepoint = timepoint;
			}
			catch (Exception e)
			{
				IOFunctions.printErr("Cannot read file: " + file);
				e.printStackTrace();
				System.exit(0);
			}
		}
	}
	
	public static void getDisplacements(String directory)
	{
		String[] regFiles = getDirectoryListingOfFiles(directory, ".registration");
		IOFunctions.println("Timepoint" + "\t" + "avg" + "\t" + "min" + "\t" + "max");

		String lasttimepoint = "";
		for (String file : regFiles)
		{
			float min = 0, avg = 0, max = 0;
			
			String timepoint = file.substring(file.indexOf("TL")+2, file.indexOf("Angle") - 1);
			
			try
			{
				BufferedReader in = TextFileAccess.openFileRead(file);
				
				while (in.ready())
				{
					String line = in.readLine().trim();
					
					if (line.startsWith("minError:"))
						min = Float.parseFloat(line.substring(line.indexOf(":") + 1, line.length()));
					else if (line.startsWith("maxError:"))
						max = Float.parseFloat(line.substring(line.indexOf(":") + 1, line.length()));
					else if (line.startsWith("avgError:"))
						avg = Float.parseFloat(line.substring(line.indexOf(":") + 1, line.length()));
				}
				in.close();
				
				if  (!timepoint.equals(lasttimepoint))
					IOFunctions.println(timepoint + "\t" + avg + "\t" + min + "\t" + max);
				
				lasttimepoint = timepoint;
			}
			catch (Exception e)
			{
				IOFunctions.printErr("Cannot read file: " + file);
				e.printStackTrace();
				System.exit(0);
			}
		}
	}
	
}
