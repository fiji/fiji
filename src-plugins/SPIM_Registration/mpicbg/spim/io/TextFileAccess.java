package mpicbg.spim.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class TextFileAccess 
{
	public static BufferedReader openFileRead(String fileName)
	{
	  BufferedReader inputFile;
	  try
	  {
		inputFile = new BufferedReader(new FileReader(fileName));
	  }
	  catch (IOException e)
	  {
		IOFunctions.printErr("mpi.fruitfly.general.openFileRead(): " + e);
		inputFile = null;
	  }
	  return(inputFile);
	}

	public static PrintWriter openFileWrite(String fileName)
	{
	  PrintWriter outputFile;
	  try
	  {
		outputFile = new PrintWriter(new FileWriter(fileName));
	  }
	  catch (IOException e)
	  {
		IOFunctions.printErr("mpi.fruitfly.general.openFileWrite(): " + e);
		outputFile = null;
	  }
	  return(outputFile);
	}

	public static PrintWriter openFileWriteEx( final String fileName ) throws IOException { return new PrintWriter( new FileWriter( fileName ) ); }
	public static BufferedReader openFileReadEx( final String fileName ) throws IOException{ return new BufferedReader( new FileReader( fileName ) ); }	
}
