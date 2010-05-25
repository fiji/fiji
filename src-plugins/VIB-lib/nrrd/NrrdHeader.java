package nrrd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * Class to read and represent the header of a NRRD file.
 * This is for a generic nrrd reader.  The only special processing that it 
 * does is:
 * - change field names to lower case
 * - turn nrrd field names into a standard form 
 *   (e.g. "axismin"->"axis min")
 * - doesn't attempt to white space separate content field
 * - records content-type tag if present  
 * @author jefferis
 *
 */
public class NrrdHeader {
	String filename,directory;
	LinkedHashMap tags;
	LinkedHashMap fields;
	ArrayList header,comments; 
	String magic;
	// A special field and a special tag respectively
	String content,contentType;
	String encoding;
	
	public int lineSkip;

	boolean detachedHeader=false;
	// If the data file spec looks like: data file: LIST [<subdim>]
	// and the filenames are on multiple additional lines 
	boolean multiLineDataFile=false;
	ArrayList dataFiles;
	
	// For handling the basic magic-less text format 
	StringBuffer textData;
	boolean textNrrd=false;
	public String getTextData(){
		if(!textNrrd || textData.length()==0) return null;
		else return textData.toString();
	}
	
	public NrrdHeader(){ this("UTF-8"); }
	public NrrdHeader(String encoding){ this.encoding=encoding; }
	
	public String toString(){
		StringBuffer sb=new StringBuffer();
		if(header==null || header.size()<1) return null;
		
		Iterator it = header.iterator();
		while (it.hasNext()){
			sb.append(it.next()+"\n");
		}
		return sb.toString();
	}
	
	void appendLine(String l) {
		header.add(l);
	}
	
	void init(){
		tags=new LinkedHashMap();
		fields=new LinkedHashMap();
		header=new ArrayList();
		comments=new ArrayList();
		textData=new StringBuffer();
		dataFiles=new ArrayList();
		detachedHeader=false;
		multiLineDataFile=false;
	}
	
	public String getCommentStrings() {return comments.toString();}
	void appendCommment(String comment) throws Exception {
		//tag=tag.trim();
		if(!comment.startsWith("#")) throw new Exception("nrrd invalid comment: "+comment);
		appendLine(comment);
		comment=comment.split("#\\s*", 1)[0];
	}
	
	public String getTagStrings() {return tags.toString();}
	void appendTag(String tag) throws Exception {
		appendLine(tag);
		//tag=tag.trim();
		int sepPos=tag.indexOf(":=");
		if(sepPos<1 || tag.length()<3) throw new Exception("nrrd: invalid tag: "+tag);
		String tagKey=tag.substring(0, sepPos);		
		String tagValue=tag.substring(sepPos+2,tag.length());
		tags.put(tagKey,tagValue);
	}
	
	public String getFieldStrings() {
		// This expands the field value arrays
		if(fields==null || fields.size()<1) return null;
		StringBuffer sb=new StringBuffer("{");
		Iterator it = fields.keySet().iterator();
		String fieldname;
		while (it.hasNext()){
			fieldname=(String) it.next();
			String sa=Arrays.toString((String[]) fields.get(fieldname));
			if(sa!=null) sb.append(fieldname+"="+sa);
			if(it.hasNext()) sb.append(", ");
		}
		sb.append("}");
		return sb.toString();
	}
	
	void processDataFile(String allFieldVals) throws Exception {
		String[] fieldVals=null;	
		detachedHeader=true;
		if(allFieldVals.startsWith("LIST")){
			// ie: data file: LIST [<subdim>]
			multiLineDataFile=true;
			fieldVals=allFieldVals.trim().split("\\s+");
		} else {
			// check for format specifier %[0-9]d
			// TODO - cleverer check in case of escaped %?
			if(allFieldVals.matches(".*%[0-9]*d.*")){
				// looks like data file: <format> <min> <max> <step> [<subdim>]
				if(allFieldVals.startsWith("\"")){
					// NB actually reference NRRD library cannot cope with
					// spaces in names because this format specifier cannot be quoted
					// now look for last quote (since the other items should not be quoted)
					int lastQuotePos=allFieldVals.lastIndexOf("\"", 1);
					if(lastQuotePos<2) throw new Exception
						("Unable to read quoted format string in data file line");	
					String[] sa=allFieldVals.substring(lastQuotePos+1).split("\\s+");
					if(sa.length<4 || sa.length>5) throw new Exception 
						("Incorrect number of field specifications in data file field");
					fieldVals=new String[1+sa.length];
					fieldVals[0]=allFieldVals.substring(1, lastQuotePos);
					System.arraycopy(sa, 0, fieldVals, 0, sa.length);
				} else{
					// format string not quoted, so just split
					fieldVals=allFieldVals.split("\\s+");
				}
			} else {
				// looks like data file: <filename>
				fieldVals=new String[1]; fieldVals[0]=allFieldVals;
			}
		}
		// Store the data file specification and return
		fields.put("data file", fieldVals);
	}
	
	void appendField(String fieldspec) throws Exception {
		appendLine(fieldspec);
		// Separate the field spec
		int sepIndex=fieldspec.indexOf(": ");
		String fieldName=standardFieldName(fieldspec.substring(0, sepIndex));
		String allFieldVals=fieldspec.substring(sepIndex+2,fieldspec.length());
		String[] fieldVals=null;
		
		if(fieldName.equals("content")){
			// special case: this field contains a string that should not be split
			this.content=allFieldVals;
			fieldVals=new String[1]; fieldVals[0]=allFieldVals;
			fields.put(fieldName, fieldVals);
			return;
		}
		if(fieldName.equals("data file")){
			// data file needs special handling
			processDataFile(allFieldVals);
			return;			
		}

		// Now trim and split field vals if required
		allFieldVals=allFieldVals.trim();		
		if(allFieldVals.startsWith("\"") && allFieldVals.endsWith("\"")){
			// Remove the first and last quote
			allFieldVals=allFieldVals.substring(1, allFieldVals.length()-1);
			fieldVals=allFieldVals.split("\"\\s+\"");
			if(fieldVals.length<1) throw 
				new Exception("nrrd: trouble parsing quoted field values: "+fieldspec);
		} else fieldVals=allFieldVals.toLowerCase().split("\\s+");
		fields.put(fieldName, fieldVals);
	}
	
	String standardFieldName(String fieldName){
		fieldName=fieldName.toLowerCase();
		if(fieldName.equals("centerings")) return "centers";		
		if(fieldName.equals("axismaxs")) return "axis maxs";
		if(fieldName.equals("axismins")) return "axis mins";
		if(fieldName.equals("lineskip")) return "line skip";
		if(fieldName.equals("byteskip")) return "byte skip";
		if(fieldName.equals("datafile")) return "data file";
		if(fieldName.equals("oldmax")) return "old max";
		if(fieldName.equals("oldmin")) return "old min";
		return fieldName;
	}
	
	public void readHeader(String path) throws IOException {
		FileInputStream fis;
		try {
			File f=new File (path);
			filename=f.getName();
			directory=f.getParent();
			fis=new FileInputStream(f);
		} catch (Exception e){
			throw new IOException("Unable to access nrrd file: "+path);
		}
		readHeader(fis);
		fis.close();
	}
	
	public void readHeader(InputStream is) throws IOException {
		try {
			LineNumberReader in=new LineNumberReader(new InputStreamReader(is,encoding));
			
			// Clear hashes etc
			init();
			magic = in.readLine();
			appendLine(magic);
			
			String s;
			while((s = in.readLine()) != null){
				if(s.startsWith("#")) {
					appendCommment(s);
					continue;
				} else if(s.indexOf(":=")>-1) {
					appendTag(s);
					continue;
				} else if(s.indexOf(": ")>-1){
					appendField(s);
					continue;
				} else if(s.length()==0){
					// First blank line marks the end of the header
					lineSkip=in.getLineNumber();
					break;
				} else if (multiLineDataFile) {
					// if this is true then we are at the end of the header and 
					// each line contains a filename without any spaces
					appendLine(s); dataFiles.add(s);					
					continue;
				}
				if(textNrrd){
					// If data line from a text nrrd then just append
					textData.append(s);
				} else if(magic.startsWith("NRRD")){
					// If we got here there's a problem
					throw new Exception ("Invalid nrrd header at line:"+in.getLineNumber());					
				} else {
					// This could still be a valid text nrrd is if there
					// is some float data in this line
					// figuring out how to get this data generically may be a problem though
					// the point of having an inputstream method would be to leave the
					// stream ready to read the data and if we've already read the first line ...
					textData.append(s);
					String[] floatStrings = s.split("\\s+");
					try {
						for(int i=0;i<floatStrings.length;i++){
							float f = new Float(floatStrings[i]).floatValue();
						}
					} catch (NumberFormatException e) {
						throw new IOException("Invalid nrrd text file - first uncommented line must contain floats");
					}
				}
			}
		} catch (Exception e){
			throw new IOException("Trouble reading nrrd header: "+e);
		}
	}
	
	public static void main(String [] args)
	{
		if(args.length<1) System.err.println("Must specify a file name!");
		else {
			NrrdHeader nh=null; NrrdInfo ni=null;
			for(int i=0;i<args.length;i++){
				try{
					nh=new NrrdHeader();
					nh.readHeader(args[i]);					
					ni = new NrrdInfo(nh);
					ni.parseHeader();
					System.out.println(""+nh);
					System.out.println("fields:\n"+nh.getFieldStrings()+"\n");
				} catch (IOException e){
					System.err.println("Problem reading file name: "+args[i]+"\n"+e);
				} catch (Exception e){
					System.err.println("Problem parsing file name: "+args[i]+"\n"+e);				
					e.printStackTrace();				
				}
			}	
		}
	}
}
