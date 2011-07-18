package mpicbg.spim.mpicbg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import mpicbg.spim.io.TextFileAccess;


public class SketchTikZFileObject
{
	BufferedReader in;
	PrintWriter out;		

	public SketchTikZFileObject( BufferedReader in, PrintWriter out )
	{
		this.in = in;
		this.out = out;
	}
	
	public BufferedReader getTemplate() { return in; }
	public PrintWriter getOutput() { return out; }
	
	public boolean finishFiles()
	{
		try
		{
			// copy the rest
			while ( in.ready() )
			{
				String line = in.readLine();
				out.println( line );
			}

			in.close();
			out.close();		
		}
		catch ( IOException e )
		{
			System.err.println(" Cannot close file: " + e );
			return false;
		}
		
		return true;
	}
	
	public static SketchTikZFileObject initOutputFile( String template, String output )
	{
		String fillOpacity = "0.5";
		String dotSize = "0.03";
		
		boolean reachedInsertPosition = false;
		boolean setupColors = false;
		
		BufferedReader in = null;
		PrintWriter out = null;

		try
		{
			in = TextFileAccess.openFileRead( template );
			out = TextFileAccess.openFileWrite( output );
			
			// set up the colors
			while ( in.ready() && !setupColors )
			{
				String line = in.readLine();
				out.println( line );
				
				if ( line.contains("% color definitions"))
				{
					out.println("");
					out.println("  def beadStyle [dotsize=0.03,draw=none,fill=black,fill opacity=0.1] ");
					out.println("  def Bead{ dots[beadStyle](0,0,0) } ");
					out.println("");

					for ( int color = 0; color < 512; color++ )
					{
						int red, green;
						
						if ( color >= 256 )
						{
							red = 255;
							green = 512 - color;
						}
						else
						{
							green = 255;
							red = color;
						}
						
						String colorDef = "defColor" + color;
						String colorName = "color" + color;
						
						out.println("  special |\\definecolor{" + colorDef + "}{rgb}{ " + red/255f +", " + green/255f + ", 0}");
						out.println("           \\tikzstyle{" + colorName + "} = [" + colorDef + "]|[lay=under]");
						out.println("  def ransacBead" + color + "Style [dotsize=" + dotSize + ",draw=none,fill=defColor" + color + ",fill opacity=" + fillOpacity + "] ");
						out.println("  def RansacBead" + color + "{ dots[ransacBead" + color + "Style](0,0,0) } ");
						out.println();
					}
										
					out.println();
					setupColors = true;
				}							
			}

			// find the insert site and copy the beginning
			while ( in.ready() && !reachedInsertPosition )
			{
				String line = in.readLine();
				if ( line.contains("%<--for Java-->"))
					reachedInsertPosition = true;
				else
					out.println( line );
			}
		}
		catch (IOException e )
		{
			System.err.println("Error reading/writing template or output file: " + e);
			return null;
		}
		
		return new SketchTikZFileObject( in, out );
	}
	
	public void putTextEntry( final float x, final float y, final String text )
	{
		// % some text if wanted
		// def y (10,-1.5,0)
	  	// special|\path #1 node[left=5pt, color=black] {$\textbf{Iteration 15 (82.54 px)}$};|(y)

		boolean reachedInsertPosition = false;
		
		try
		{
			// copy the rest
			while ( in.ready() && !reachedInsertPosition )
			{
				String line = in.readLine();
				
				if ( line.contains("%<--Text for Java-->"))
				{
					reachedInsertPosition = true;
					
					out.println( "def y (" + x + "," + y + ",0)" );
					out.println( "special|\\path #1 node[left=5pt, color=black] {$\\textbf{" + text + "}$};|(y)" );
				}
				else
				{
					out.println( line );
				}
			}
		}
		catch ( IOException e )
		{
			System.err.println(" Error: " + e );
			return;
		}
		
	}
}

