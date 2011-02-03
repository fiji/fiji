package mpicbg.spim.io;

import java.util.ArrayList;

import mpicbg.spim.io.ConfigurationParserException;

/**
 * <p>
 * Title:
 * </p>
 * 
 * <p>
 * Description:
 * </p>
 * 
 * <p>
 * Copyright: Copyright (c) 2006
 * </p>
 * 
 * <p>
 * Company: Diplomarbeit - IZBI Leipzig/TU Dresden
 * </p>
 * 
 * @author Stephan Preibisch
 * @version 1.0
 */
public class ProgramConfiguration 
{
	public String baseFolder;
	public String binariesFolder;
	public String librariesFolder;
	public String jobFolder;
	public String configFolder;
	public String configFileTemplate;
	
	public String javaExecutable;
	public String javaArguments;
	
	public String timepointPattern;	
	public int timepoints[];	
	
	public String pluginsDir;
	
    public void parseTimePoints() throws ConfigurationParserException
    {
    	ArrayList<Integer> tmp = SPIMConfiguration.parseIntegerString(timepointPattern);

    	timepoints = new int[tmp.size()];
    	
    	for (int i = 0; i < tmp.size(); i++)
    		timepoints[i] = tmp.get(i);
    }
	
    public void printParameters()
    {
    	IOFunctions.println("baseFolder: " + baseFolder);
    	IOFunctions.println("binariesFolder: " + binariesFolder);
    	IOFunctions.println("librariesFolder: " + librariesFolder);
    	IOFunctions.println("jobFolder: " + jobFolder);
    	IOFunctions.println("configFolder: " + configFolder);
    	IOFunctions.println("configFileTemplate: " + configFileTemplate);
    	
    	IOFunctions.println("javaExecutable: " + javaExecutable);
    	IOFunctions.println("javaArguments: " + javaArguments);
    	
    	IOFunctions.println("timepointPattern: " + timepointPattern);
    	if (timepoints != null)
    	{
    		System.out.print("Time Points: ");
    		for (int tp : timepoints)
    			System.out.print(tp + " ");
    		
    		IOFunctions.println();
    	}
    	
    	IOFunctions.println("pluginsDir: " + pluginsDir);
    	
    }
}
