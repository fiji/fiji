package mpicbg.spim.io;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: Diplomarbeit - IZBI Leipzig/TU Dresden</p>
 *
 * @author Stephan Preibisch
 * @version 1.0
 */

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.container.cell.CellContainerFactory;
import mpicbg.imglib.interpolation.linear.LinearInterpolatorFactory;
import mpicbg.imglib.interpolation.nearestneighbor.NearestNeighborInterpolatorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.spim.registration.ViewStructure;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.lang.reflect.Field;

public class ConfigurationParserSPIM
{
	public static SPIMConfiguration parseFile( String confFileName ) throws ConfigurationParserException
	{
		SPIMConfiguration conf = new SPIMConfiguration();

		String knownDatatypes[] = { "int", "double", "String", "boolean", "float", "ContainerFactory", "InterpolatorFactory", "OutsideStrategyFactory" };

		// convert to unix-style
		confFileName = confFileName.replace('\\', '/');
		confFileName = confFileName.replaceAll( "//", "/" );
		
		// get path
		String confFilePath;

		try
		{
			if (confFileName.indexOf("/") != -1)
				confFilePath = confFileName.substring(0, confFileName.lastIndexOf("/") + 1);
			else
				confFilePath = "";	
						
		} catch (Exception e)
		{
			throw new ConfigurationParserException("Error parsing confFileName-String: '" + e.getMessage() + "'");
		}		

		// open configfile
		BufferedReader assignFile = null;
		BufferedReader confFile;
		
		try
		{
			confFile = TextFileAccess.openFileReadEx(confFileName);
		}
		catch ( IOException e  )
		{
			throw new ConfigurationParserException("Configuration file not found: '" + confFileName + "'");	
		}
			

		// open assignment file
		try
		{
			while (confFile.ready() && assignFile == null)
			{
				String line = confFile.readLine().trim();

				if (line.startsWith("<") && line.endsWith(">"))
				{
					assignFile = TextFileAccess.openFileRead(confFilePath + line.substring(1, line.length() - 1));
					if (assignFile == null)
						throw new ConfigurationParserException("Variables Assignment file not found: '" + confFilePath + (line.substring(1, line.length() - 1)) + "'");
				}
			}
		} catch (Exception e)
		{
			throw new ConfigurationParserException("Error finding assignment file entry: '" + e.getMessage() + "'");
		}

		// load assignments
		ArrayList<ArrayList<?>> assignments = new ArrayList<ArrayList<?>>();
		Field[] fields = conf.getClass().getDeclaredFields();

		int lineCount = 0;
		try
		{
			while (assignFile.ready())
			{
				lineCount++;
				String line = assignFile.readLine().trim();

				if (!line.startsWith("#") && line.length() > 0)
				{
					String words[] = line.split("=");

					if (words.length != 2)
						throw new ConfigurationParserException("Wrong format in assignment file, should be 'entry = datatype name'");

					// entry name
					ArrayList temp = new ArrayList();
					temp.add(words[0].trim());

					words = words[1].trim().split(" ");
					if (words.length != 2)
						throw new ConfigurationParserException("Wrong format in assignment file, datatype and name on right side MUST have no spaces");

					words[0] = words[0].trim();
					words[1] = words[1].trim();

					// datatype
					boolean positiveMatch = false;

					for (int i = 0; i < knownDatatypes.length; i++)
					{
						if (words[0].compareTo(knownDatatypes[i]) == 0)
							positiveMatch = true;
					}

					if (!positiveMatch)
					{
						String datatypes = "";
						for (int i = 0; i < knownDatatypes.length; i++)
							datatypes += knownDatatypes[i] + " ";

						throw new ConfigurationParserException("Unknown datatype '" + words[0] + "', available datatypes are: " + datatypes);
					}

					temp.add(words[0]);

					// variable name
					int variablesPosition = -1;

					for (int i = 0; i < fields.length; i++)
					{
						if (words[1].compareTo(fields[i].getName()) == 0)
							variablesPosition = i;
					}

					if (variablesPosition == -1)
					{
						String variables = "";
						for (int i = 0; i < fields.length; i++)
							variables += fields[i].getName() + "\n";

						throw new ConfigurationParserException("Unknown variable '" + words[1] + "', available variables are:\n" + variables);
					}

					temp.add(words[1]);
					temp.add(new Integer(variablesPosition));

					assignments.add(temp);
				}
			}
		} catch (Exception e)
		{
			throw new ConfigurationParserException("Error reading/parsing assignment file at line " + lineCount + ":\n" + e.getMessage());
		}

		// read configuration file and assign entry values to assigned variables
		lineCount = 0;

		try
		{
			while (confFile.ready())
			{
				lineCount++;
				String line = confFile.readLine().trim();

				if (!line.startsWith("#") && line.length() > 0)
				{
					String words[] = line.split("=");

					if (words.length != 2)
						throw new ConfigurationParserException("Wrong format in configuration file, should be 'entry = value'");

					words[0] = words[0].trim();
					words[1] = words[1].trim();

					int entryPos = findEntry(assignments, words[0]);

					if (entryPos == -1)
						throw new ConfigurationParserException("Entry '" + words[0] + "' does not exist!\nFollowing entries are available:\n" + getAllEntries(assignments));

					int varFieldPos = getVariableFieldPosition(assignments, entryPos);

					if (getDatatype(assignments, entryPos).compareTo("byte") == 0)
					{
						fields[varFieldPos].setByte(conf, Byte.parseByte(words[1]));
					} 
					else if (getDatatype(assignments, entryPos).compareTo("short") == 0)
					{
						fields[varFieldPos].setShort(conf, Short.parseShort(words[1]));
					} 
					else if (getDatatype(assignments, entryPos).compareTo("int") == 0)
					{
						fields[varFieldPos].setInt(conf, Integer.parseInt(words[1]));
					} 
					else if (getDatatype(assignments, entryPos).compareTo("long") == 0)
					{
						fields[varFieldPos].setLong(conf, Long.parseLong(words[1]));
					} 
					else if (getDatatype(assignments, entryPos).compareTo("boolean") == 0)
					{
						fields[varFieldPos].setBoolean(conf, Boolean.parseBoolean(words[1]));
					} 
					else if (getDatatype(assignments, entryPos).compareTo("float") == 0)
					{
						if (words[1].toLowerCase().compareTo("nan") == 0)
							fields[varFieldPos].setFloat(conf, Float.NaN);
						else
							fields[varFieldPos].setFloat(conf, Float.parseFloat(words[1]));
					} 
					else if (getDatatype(assignments, entryPos).compareTo("double") == 0)
					{
						if (words[1].toLowerCase().compareTo("nan") == 0)
							fields[varFieldPos].setDouble(conf, Double.NaN);
						else
							fields[varFieldPos].setDouble(conf, Double.parseDouble(words[1]));
					} 
					else if (getDatatype(assignments, entryPos).compareTo("String") == 0)
					{
						if (words[1].equals("null"))
							fields[varFieldPos].set(conf, null);
						else
						{
							if (words[1].startsWith("\"") && words[1].endsWith("\""))
								fields[varFieldPos].set(conf, words[1].substring(1, words[1].length() - 1));
							else
								throw new ConfigurationParserException("Strings have to be surrounded by  \"\" or be null");
						}
					} 
					else if (getDatatype(assignments, entryPos).compareTo("ContainerFactory") == 0)
					{
						if (words[1].startsWith("ArrayContainerFactory"))
						{							
							ArrayContainerFactory factory = new ArrayContainerFactory();
							factory.setParameters(words[1].substring(words[1].indexOf("("), words[1].length()));
							fields[varFieldPos].set(conf, factory);
						}
						else if (words[1].startsWith("CellContainerFactory"))
						{
							CellContainerFactory factory = new CellContainerFactory();
							factory.setParameters(words[1].substring(words[1].indexOf("("), words[1].length()));
							fields[varFieldPos].set(conf, factory);
						}
						else
						{
							throw new ConfigurationParserException("Unknown implementation of ContainerFactory '" + words[1] + "'");
						}
					}
					else if (getDatatype(assignments, entryPos).compareTo("InterpolatorFactory") == 0)
					{
						if (words[1].startsWith("LinearInterpolatorFactory"))
						{
							LinearInterpolatorFactory<FloatType> factory = new LinearInterpolatorFactory<FloatType>( null );
							factory.setParameters(words[1].substring(words[1].indexOf("("), words[1].length()));
							fields[varFieldPos].set(conf, factory);
						}
						else if (words[1].startsWith("NearestNeighborInterpolatorFactory"))
						{
							NearestNeighborInterpolatorFactory<FloatType> factory = new NearestNeighborInterpolatorFactory<FloatType>( null );
							factory.setParameters(words[1].substring(words[1].indexOf("("), words[1].length()));
							fields[varFieldPos].set(conf, factory);
						}
						else
						{
							throw new ConfigurationParserException("Unknown implementation of FloatInterpolatorFactory '" + words[1] + "'");
						}						
					}
					else if (getDatatype(assignments, entryPos).compareTo("OutsideStrategyFactory") == 0)
					{
						if (words[1].startsWith("OutsideStrategyMirrorFactory"))
						{
							OutOfBoundsStrategyFactory<FloatType> factory = new OutOfBoundsStrategyMirrorFactory<FloatType>();
							factory.setParameters(words[1].substring(words[1].indexOf("("), words[1].length()));
							fields[varFieldPos].set(conf, factory);
						}
						else if (words[1].startsWith("OutsideStrategyValueFactory"))
						{
							OutOfBoundsStrategyFactory<FloatType> factory = new OutOfBoundsStrategyValueFactory<FloatType>();
							factory.setParameters(words[1].substring(words[1].indexOf("("), words[1].length()));
							fields[varFieldPos].set(conf, factory);
						}
						else
						{
							throw new ConfigurationParserException("Unknown implementation of OutsideStrategyFactory '" + words[1] + "'");
						}											
					}
					else
					{
						throw new ConfigurationParserException("Unknown datatype '" + getDatatype(assignments, entryPos) + "'");
					}

				}
			}
		} 
		catch (Exception e)
		{
			throw new ConfigurationParserException("Error reading/parsing configuration file at line " + lineCount + ":\n" + e.getMessage());
		}

		// variable specific verification
		if (conf.numberOfThreads < 1)
			conf.numberOfThreads = Runtime.getRuntime().availableProcessors();				
		
		if (conf.scaleSpaceNumberOfThreads < 1)
			conf.scaleSpaceNumberOfThreads = Runtime.getRuntime().availableProcessors();				

		
		// check the directory string
		conf.inputdirectory = conf.inputdirectory.replace('\\', '/');
		conf.inputdirectory = conf.inputdirectory.trim();
		if (conf.inputdirectory.length() > 0 && !conf.inputdirectory.endsWith("/"))
			conf.inputdirectory = conf.inputdirectory + "/";
		
		if (conf.outputdirectory == null || conf.outputdirectory.length() == 0)
		{
			conf.outputdirectory = conf.inputdirectory;
		}
		else
		{
			conf.outputdirectory = conf.outputdirectory.replace('\\', '/');
			conf.outputdirectory = conf.outputdirectory.trim();
			if (conf.outputdirectory.length() > 0 && !conf.outputdirectory.endsWith("/"))
				conf.outputdirectory = conf.outputdirectory + "/";			
		}

		if (conf.registrationFiledirectory == null || conf.registrationFiledirectory.length() == 0)
		{
			conf.registrationFiledirectory = conf.inputdirectory;
		}
		else
		{
			conf.registrationFiledirectory = conf.registrationFiledirectory.replace('\\', '/');
			conf.registrationFiledirectory = conf.registrationFiledirectory.trim();
			if (conf.registrationFiledirectory.length() > 0 && !conf.registrationFiledirectory.endsWith("/"))
				conf.registrationFiledirectory = conf.registrationFiledirectory + "/";			
		}

		// check if directories exist
		File dir = new File(conf.outputdirectory, "");
		if (!dir.exists())
		{
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Creating directory '" + conf.outputdirectory + "'.");
			boolean success = dir.mkdirs();
			if (!success)
			{
				if (!dir.exists())
				{
					IOFunctions.printErr("(" + new Date(System.currentTimeMillis()) + "): Cannot create directory '" + conf.outputdirectory + "', quitting.");
					System.exit(0);
				}				
			}
		}
		
		dir = new File(conf.registrationFiledirectory, "");
		if (!dir.exists())
		{
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Creating directory '" + conf.registrationFiledirectory + "'.");
			boolean success = dir.mkdirs();
			if (!success)
			{
				if (!dir.exists())
				{
					IOFunctions.printErr("(" + new Date(System.currentTimeMillis()) + "): Cannot create directory '" + conf.registrationFiledirectory + "', quitting.");
					System.exit(0);
				}
			}
		}	
		
		int countTrue = 0;
		if (conf.paralellFusion) countTrue++;
		if (conf.sequentialFusion) countTrue++;
		if (conf.multipleImageFusion) countTrue++;
		
		if (countTrue != 1)
			throw new ConfigurationParserException("Error reading/parsing configuration file: Only one fusion method must be true!");
		
		if ( conf.debugLevel.toUpperCase().equals("DEBUG_ERRORONLY"))
			conf.debugLevelInt = ViewStructure.DEBUG_ERRORONLY;

		if ( conf.debugLevel.toUpperCase().equals("DEBUG_MAIN"))
			conf.debugLevelInt = ViewStructure.DEBUG_MAIN;
		
		if ( conf.debugLevel.toUpperCase().equals("DEBUG_ALL"))
			conf.debugLevelInt = ViewStructure.DEBUG_ALL;
		
		conf.getFileNames();
		
		// set interpolator stuff
		conf.interpolatorFactorOutput.setOutOfBoundsStrategyFactory( conf.strategyFactoryOutput );

		
		
		// close files
		try
		{
			assignFile.close();
			confFile.close();
		} catch (Exception e)
		{};

		return conf;
	}

	private static int findEntry( ArrayList list, String entry )
	{
		int pos = -1;

		for (int i = 0; i < list.size() && pos == -1; i++)
		{
			if ((getEntry(list, i).toLowerCase()).compareTo(entry.toLowerCase()) == 0)
				pos = i;
		}

		return pos;
	}

	private static String getAllEntries( ArrayList list )
	{
		String entries = "";
		for (int i = 0; i < list.size(); i++)
		{
			entries += getEntry(list, i) + "\n";
		}

		return entries;
	}

	private static String getAllDatatypes( ArrayList list )
	{
		String entries = "";
		for (int i = 0; i < list.size(); i++)
		{
			entries += getDatatype(list, i) + "\n";
		}

		return entries;
	}

	private static String getAllVariableNames( ArrayList list )
	{
		String entries = "";
		for (int i = 0; i < list.size(); i++)
		{
			entries += getVariableName(list, i) + "\n";
		}

		return entries;
	}

	private static String getEntry( ArrayList list, int pos )
	{
		return (String) (((ArrayList) list.get(pos)).get(0));
	}

	private static String getDatatype( ArrayList list, int pos )
	{
		return (String) (((ArrayList) list.get(pos)).get(1));
	}

	private static String getVariableName( ArrayList list, int pos )
	{
		return (String) (((ArrayList) list.get(pos)).get(2));
	}

	private static int getVariableFieldPosition( ArrayList list, int pos )
	{
		return ((Integer) (((ArrayList) list.get(pos)).get(3))).intValue();
	}

}
