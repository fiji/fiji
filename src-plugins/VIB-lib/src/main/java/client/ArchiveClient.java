/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* This class is for interacting with the Amstrong group confocal
 * archive.  ImageJ is launched as an applet with the following
 * parameters:
 * 
 *   url1  -       the URL of the image to download from the server
 *   eval1 -       a macro expression to run, typically just:
 *                     run('Some Plugin or Other','macro options string...');
 * 
 * The macro options string should be a semi-colon separated list of
 * parameters for the plugin, such as:
 * 
 *     user=mark;
 *     cookie-value=mark:88285a52fbc6345345cdc16ca60859e1;
 *     cgi-root=http://localhost/confocal/;
 *     md5sum=37aef5074b9c5918e3351555b434d61a;
 *     date=2007-24-05
 * 
 * (I've split that across multiple lines for clarity. If there is no
 * "=" between the semi-colons, then everything between the semi-colons
 * is regarded as the key and the value is the empty string.)
 * 
 * The meaning oof these parameters is:
 *
 *       user <-- the username that's logged into the website
 *       cookie-value <-- the cookie value that corresponds to that user's session
 *       cgi-root <-- a partial URL where the website scripts can be found
 *       md5sum <-- the md5sum of the image referred to by url1
 * 
 * There are some convenience methods for talking to the API for
 * the archive in this class.
 */

// FIXME: not actually threaded at the moment
// FIXME: odd bug where sometimes the cookie isn't actually sent?

package client;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;

import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.URLEncoder;

import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Enumeration;
import java.util.ArrayList;

import java.applet.Applet;

import java.io.*;

class APIRequestThread extends Thread {

	boolean stopRequested = false;
	
	public void requestStop() {
		stopRequested = true;
	}

	public ArrayList< String [] > doUploadRequest( String apiURL,
						       Hashtable parametersForPost,
						       byte [] data,
						       String cookie_value ) {

		ArrayList< String [] > result = new ArrayList< String [] >();

		HttpURLConnection connection = null;

		DataOutputStream dos = null;

		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary =  "sillymimeboundary";

		byte [] buffer;
		int bufferSize = 1024 * 1024;
		int bytesRead;

		try {

			URL url = new URL(apiURL);
			
			URLConnection urlConnection = url.openConnection();
			if( urlConnection instanceof HttpURLConnection ) {
				connection = (HttpURLConnection)urlConnection;
			} else {
				return null;
			}

			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);

			connection.setRequestMethod("POST");

			if( cookie_value != null ) {
				System.err.println("sending cookie: " + cookie_value );
				connection.setRequestProperty("Cookie", cookie_value );
			}

			connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
			
			dos = new DataOutputStream( connection.getOutputStream() );
			
			System.out.println("Writing data to output (upload) stream...");
			
			for (Enumeration e = parametersForPost.keys() ; e.hasMoreElements() ; ) {

				String key = (String)e.nextElement();
				String value = (String)parametersForPost.get(key);

				System.err.println(""+key+" => "+value);

				dos.writeBytes( twoHyphens + boundary + lineEnd );
				dos.writeBytes( "Content-Disposition: form-data; name=\"" + key + "\"" + lineEnd );
				dos.writeBytes( lineEnd );
				dos.writeBytes( value );
				dos.writeBytes( lineEnd );
			}

			if( data != null ) {

				dos.writeBytes( twoHyphens + boundary + lineEnd );
				dos.writeBytes( "Content-Disposition: form-data; name=\"new-file\"" + lineEnd );
				dos.writeBytes( "Content-Type: application/octet-stream" + lineEnd );
				dos.writeBytes( "Content-TransferEncoding: binary" + lineEnd );
				dos.writeBytes( lineEnd );
				
				int bytes_written = 0;
			
				int bytes_to_write = data.length;

				while( bytes_to_write > 0 ) {
					
					dos.write( data,
						   bytes_written,
						   bytes_to_write );
					bytes_written = dos.size();
					bytes_to_write = data.length - bytes_written;
				}
			
				dos.writeBytes( lineEnd );

			}

			dos.writeBytes( twoHyphens + boundary + twoHyphens + lineEnd );

			dos.flush();
			dos.close();

			System.out.println("Finished writing data to the output (upload) stream.");

		} catch( Exception e ) {
			System.out.println( "Got an exception while uploading the file: " + e );
			e.printStackTrace();
			result = new ArrayList< String [] >();
			String [] error_array = { "error", "Got an exception while uploading the file: " + e };
			result.add( error_array );
			return result;
		}
		
		try {

			System.out.println("Reading data from input (download) stream...");

			DataInputStream dis = new DataInputStream ( connection.getInputStream() );
			String s;
			do {
				s = dis.readLine(); // <-- FIXME, broken of course
				if( s != null ) {

					ArrayList< String > tokens = new ArrayList< String >();

					// FIXME: should unquote
					
					StringTokenizer tokenizer=new StringTokenizer(s,"\t");

					while( tokenizer.hasMoreTokens() ) {
						String token = tokenizer.nextToken();
						tokens.add(token);
					}

					String [] tokens_array = new String[tokens.size()];
					
					for( int i=0; i < tokens.size(); ++i )
						tokens_array[i] = (String)tokens.get(i);

					result.add(tokens_array);
				}

			} while( s != null );
			
			dis.close();
			
			System.out.println("Finished reading data from input (download) stream.");
			
		} catch( Exception e ) {
			IJ.error( "Got an exception while making the request to "+apiURL+": " + e );
			System.err.println( "Got an exception while making the request to "+apiURL+": " + e );
			e.printStackTrace();
			result = new ArrayList< String [] >();
			String [] error_array = { "error", "Got an exception while uploading the file: " + e };
			result.add( error_array );
			return result;
		}
		
		System.out.println("Upload finished.");

		return result;
		
	}

}

public class ArchiveClient {
	
	Applet applet;

	Hashtable<String,String> parameterHash;

	String cgiRoot;

	public ArchiveClient( Applet applet ) {
		this(applet,Macro.getOptions());
	}
	
	public ArchiveClient( Applet applet, String arguments ) {
		this.applet = applet;
		extractArchiveParameters(arguments);
		cgiRoot=getValue("cgi-root");
	}

	static byte [] justGetFile( String string_url ) {

		HttpURLConnection connection = null;

		DataOutputStream dos = null;

		byte [] buffer;
		int initialBufferSize = 1024 * 1024;
		buffer = new byte[initialBufferSize];
		int bytesRead = 0;
		int bytesJustRead;

		try {

			URL url = new URL(string_url);
			
			URLConnection urlConnection = url.openConnection();
			if( urlConnection instanceof HttpURLConnection ) {
				connection = (HttpURLConnection)urlConnection;
			} else {
				return null;
			}

			connection.setDoInput(true);
			connection.setDoOutput(false);
			connection.setUseCaches(false);

			connection.setRequestMethod("GET");

			DataInputStream dis = new DataInputStream ( connection.getInputStream() );
			String s;
			do {

				if( bytesRead == buffer.length ) {
					// Expand the buffer...

					byte [] newBuffer = new byte[(int)(buffer.length * 1.5)];
					System.arraycopy( buffer, 0, newBuffer, 0, bytesRead );
					buffer = newBuffer;

				}
				
				bytesJustRead = dis.read( buffer, bytesRead, buffer.length - bytesRead );
				bytesRead += bytesJustRead;
				
			} while( bytesJustRead >= 0 );
			
			dis.close();
			
			System.out.println("Finished reading data from input (download) stream.");
			
		} catch( Exception e ) {
			
			IJ.error( "Fetching "+string_url+" failed with:\n" + e );
			return null;
		}
		
		System.out.println("Upload finished.");

		byte [] result = new byte[bytesRead];
		System.arraycopy(buffer,0,result,0,bytesRead);

		return result;
	}

	public static String justGetFileAsString( String url ) {

		byte [] data = justGetFile( url );
		if( data == null )
			return null;
		else
			try {
				String result = new String( data, "UTF-8" );
				return result;
			} catch( UnsupportedEncodingException e ) {
				return null;
			}
		
	}

	

	public ArrayList< String [] > synchronousRequest( Hashtable parametersForPost,
				   byte [] data ) {

		String cookie_value = parameterHash.get("cookie-value");

		APIRequestThread requestThread = new APIRequestThread();

		System.err.println("cgiRoot is "+cgiRoot);

		// FIXME: make this actually threaded...

		ArrayList< String [] > returned_data =
			requestThread.doUploadRequest( cgiRoot + "api",
						       parametersForPost,
						       data,
						       cookie_value );

		for( int i = 0; i < returned_data.size(); ++i ) {
			String [] line = (String [])returned_data.get(i);
			System.err.print("Got line: ");
			for( int j = 0; j < line.length; ++j ) {
				System.err.print( line[j] + ";\t" );
			}
			System.err.println( "" );
		}

		return returned_data;
	}

	private void extractArchiveParameters( String arguments ) {

		StringTokenizer tokenizer=new StringTokenizer(arguments,";");
		parameterHash=new Hashtable<String,String>();

		while( tokenizer.hasMoreTokens() ) {
			String token = tokenizer.nextToken();
			System.err.println( "got token: "+token );
			int indexOfFirstEquals = token.indexOf('=');
			if( indexOfFirstEquals < 0 ) {
				parameterHash.put(token,"");
			} else {
				String key=token.substring(0,indexOfFirstEquals);
				String value=token.substring(indexOfFirstEquals+1);
				parameterHash.put(key,value);
			}
		}		
	}

	public String getValue(String key) {
		return (String)parameterHash.get(key);
	}

	public boolean hasKey(String key) {
		return parameterHash.containsKey(key);
	}

	public Enumeration keys() {
		return parameterHash.keys();
	}

			
	public void closeChannelsWithTag( String tag ) {

		// We close any channel that's marked nc82

		Hashtable<String,String> parameters = new Hashtable<String,String>();
		parameters.put("method","channel-tags");
		parameters.put("md5sum",getValue("md5sum"));
			
		ArrayList< String [] > tsv_results = synchronousRequest(parameters,null);
		int tags = Integer.parseInt(((String [])tsv_results.get(0))[1]); // FIXME error checking
		int matching_channel = -1;
		for( int i = 0; i < tags; ++i ) {
			String [] row = (String [])tsv_results.get(i);
			if( tag.equalsIgnoreCase(row[1]) ) {
				matching_channel = Integer.parseInt(row[0]);
				break;
			}
		}
		if( matching_channel >= 0 ) {
			
			String lookFor = "Ch"+(matching_channel+1);
			
			int[] wList = WindowManager.getIDList();
			if (wList==null) {
				IJ.error("Neurite Tracer: no images have been loaded");
				return;
			}
			
			for (int i=0; i<wList.length; i++) {
				ImagePlus imp = WindowManager.getImage(wList[i]);
				String title = imp!=null?imp.getTitle():"";
				int indexOfChannel = title.indexOf(lookFor);
				if( indexOfChannel >= 0 ) {
					imp.close();
					break;
				}
			}

		}
	}
}
