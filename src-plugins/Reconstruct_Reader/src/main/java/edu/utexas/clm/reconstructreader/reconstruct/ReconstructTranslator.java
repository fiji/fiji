package edu.utexas.clm.reconstructreader.reconstruct;

import edu.utexas.clm.reconstructreader.Utils;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.io.*;
import java.util.*;

public class ReconstructTranslator {
    
    public static interface TranslationMessenger
    {
        public void sendMessage(String message);
    }
    

    private static class MalformedFileInputStream extends FilterInputStream
    {
        /**
         * A private class to handle malformed XML that sometimes crops up in
         * Reconstruct files, when the projects get very large, typically in
         * the comments.
         */
        int fixCnt;
        int[] callCnt;
        byte[] lastByte;

        public MalformedFileInputStream(File f) throws FileNotFoundException
        {
            super(new FileInputStream(f));
            fixCnt = 0;
            callCnt = new int[3];
            Arrays.fill(callCnt, 0);
            lastByte  = new byte[0];
        }

        private byte fixByte(byte b)
        {
            if (b<0x20 && !(b==0x9 || b==0xA) || b == 0x26)
            {
                ++fixCnt;
                b = 0x20;
            }
            return b;
        }

        public int read(byte[] bs, int off, int len) throws IOException
        {
            int ret = super.read(bs, off, len);

            ++callCnt[0];

            for (int i = 0; i < bs.length; ++i)
            {
                //Replace control chars with spaces
                bs[i] = fixByte(bs[i]);
            }

            lastByte = bs.clone();

            return ret;
        }

        public int read(byte[] bs) throws IOException
        {
            int ret = super.read(bs);

            ++callCnt[1];

            for (int i = 0; i < bs.length; ++i)
            {
                bs[i] = fixByte(bs[i]);
            }

            lastByte = bs.clone();

            return ret;
        }

        public int read() throws IOException
        {
            int read = super.read();

            ++callCnt[2];

            if (read != -1)
            {
                read = fixByte((byte)read);
            }

            return read;
        }

        public String toString()
        {
            String s = "Called " + callCnt[0] + " " + callCnt[1] + " " + callCnt[2] + ", and fixed " + fixCnt + '\n';
            s += "[";
            for (byte b : lastByte)
            {
                s += b + " ";
            }

            s += "];";

            return s;
        }
    }

    private static final int STRING_CHUNK_SIZE = 8192;

    private StringBuilder xmlBuilder;

    private Document serDoc;
    private final ArrayList<Document> sectionDocuments;
    private final ArrayList<ReconstructSection> sections;
    private final ArrayList<ReconstructAreaList> closedContours;
    private final ArrayList<ReconstructProfileList> openContours;
    private final ArrayList<ReconstructZTrace> zTraces;

    private final String projectName;
    private final String unuid;
    private final String nuid;
    private final File inputFile;

    private String postTranslationMessage;
    private String errorMessage;
    private File lastFile;

    private int currentOID;

    private final int layerSetOID;

    private double[] preTransPatchSize;
    private double defaultMag;

    private boolean ready;
    
    private TranslationMessenger messenger;


    public static InputSource getISO8559Source(final File f) throws FileNotFoundException, UnsupportedEncodingException
    {
        MalformedFileInputStream fileStream = new MalformedFileInputStream(f);
        //Reader charStream = new InputStreamReader(fileStream, "ISO-8859-1");
        Reader charStream = new InputStreamReader(fileStream);
        return new InputSource(charStream);
    }

    public static void addSection(List<Document> sectionDocuments, DocumentBuilder builder, File f) throws IOException, SAXException
    {
        MalformedFileInputStream fileStream = new MalformedFileInputStream(f);
        Reader charStream = new InputStreamReader(fileStream);
        Document d = builder.parse(new InputSource(charStream));
        sectionDocuments.add(d);
    }

    public ReconstructTranslator(String f)
    {
        inputFile = new File(f);
        // Parse out the path
        final String localFile = inputFile.getName();

        postTranslationMessage = "";
        errorMessage = "";
        lastFile = null;

        xmlBuilder = null;
        sectionDocuments = new ArrayList<Document>();
        sections = new ArrayList<ReconstructSection>();
        zTraces = new ArrayList<ReconstructZTrace>();
        closedContours = new ArrayList<ReconstructAreaList>();
        openContours = new ArrayList<ReconstructProfileList>();

        projectName = localFile.toLowerCase().endsWith(".ser") ?
                localFile.substring(0, localFile.length() - 4) : localFile;
        currentOID = -1;

        nuid = Integer.toString(projectName.hashCode());
        unuid = Long.toString(System.currentTimeMillis()) + "." + nuid;

        layerSetOID = nextOID();

        ready = true;

        messenger = new TranslationMessenger()
        {
            public void sendMessage(String message)
            {
                System.err.println(message);
            }
        };
    }

    public int getLayerSetOID()
    {
        return layerSetOID;
    }

    public void setMessenger(TranslationMessenger inMessenger)
    {
        messenger = inMessenger;
    }
    
    public void log(String logString)
    {
        messenger.sendMessage(logString);
    }

    public boolean process()
    {
        if (ready)
        {
            File[] sectionFiles;
            xmlBuilder = new StringBuilder();
            sectionDocuments.clear();

            /*
            Collect all Reconstruct section files.
            For instance, we might have a series file Reconstruct.ser
            In this case, projectName should be "Reconstruct" and we want to collect files like
            Reconstruct.1
            Reconstruct.2 ...
            Reconstruct.199
            Reconstruct.200
            */
            sectionFiles = inputFile.getParentFile().listFiles(
                    new FilenameFilter()
                    {
                        public boolean accept(File dir, String name)
                        {
                            return name.matches(projectName + ".[0-9]*$");
                        }
                    }
            );

            try
            {
                //Read and parse all of the files (series and sections)
                final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                final DocumentBuilder builder;

                // DTDs aren't commonly included with Reconstruct projects, so ignore them.
                builderFactory.setValidating(false);
                builderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                builder = builderFactory.newDocumentBuilder();

                serDoc = builder.parse(inputFile);

                for (File f : sectionFiles)
                {
                    log("Opening file " + f.getAbsolutePath());
                    lastFile = f;
                    addSection(sectionDocuments, builder, f);
                }

                //Fix the XML before translation
                postTranslationMessage = fixXML(sectionDocuments);

                //Sort section files by index.
                Collections.sort(sectionDocuments, new Utils.ReconstructSectionIndexComparator());

                //Need the pre-transform stack height in order to translate the Reconstruct
                //image transforms into TrakEM2 transforms. The coordinate systems are almost
                //as different as possible, for a 2D euclidean space.
                preTransPatchSize = Utils.getReconstructStackSize(sectionDocuments);
                defaultMag = Utils.getMedianMag(sectionDocuments);

                /*
                Reconstruct : TrakEM2
                Contour (closed) : area_list
                Contour (open) : profile_list
                Z-Trace : polyline

                Contours are stores in section files, whereas Z-Traces are stored in the
                series file.

                area_lists and polylines go in the trakem layer set definition, whereas
                profiles are stored directly in the layers.

                All of this is to say, that we need to collect all of the contours and ztraces
                ahead of time, and link them to their respective sections/layers.
                */

                collectContoursAndSections(sectionDocuments);
                collectZTraces(serDoc);

                xmlBuilder.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
                appendDTD(xmlBuilder);
                xmlBuilder.append("<trakem2>\n");
                appendProject(xmlBuilder);
                appendLayerSet(xmlBuilder);
                appendDisplay(xmlBuilder);
                xmlBuilder.append("</trakem2>\n");

                clearMemory();

                return true;
            }
            catch (SAXParseException spe)
            {
                String fid = lastFile == null ? "" : " in " + lastFile.getName();
                errorMessage = "Error while parsing XML" + fid + " at line " + spe.getLineNumber() +
                        ", column " + spe.getColumnNumber() + "\n";
                errorMessage += Utils.stackTraceToString(spe);

                clearMemory();
                
                return false;
            }
            catch (Exception e)
            {
                clearMemory();
                e.printStackTrace();
                errorMessage = Utils.stackTraceToString(e);
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    public String getPostTranslationMessage()
    {
        return postTranslationMessage;
    }

    public String getLastErrorMessage()
    {
        return errorMessage;
    }

    protected void clearMemory()
    {
        sectionDocuments.clear();
        sections.clear();
        closedContours.clear();
        openContours.clear();
        zTraces.clear();
    }

    protected void collectZTraces(Document serFile)
    {
        NodeList zContours = serFile.getElementsByTagName("ZContour");

        for (int i = 0; i < zContours.getLength(); ++i)
        {
            Element e = (Element)zContours.item(i);
            ReconstructZTrace zTrace = Utils.findContourByName(zTraces, e.getAttribute("name"));
            if (zTrace == null)
            {
                zTraces.add(new ReconstructZTrace(e, this));
            }
            else
            {
                zTrace.addContour(e, null);
            }
        }
    }

    protected void collectContoursAndSections(List<Document> sectionDocs)
    {
        //double[] sectionThickness;

        //Collect contours
        for (Document doc : sectionDocs)
        {
            NodeList contours = doc.getElementsByTagName("Contour");
            ReconstructSection currSection = new ReconstructSection(this, doc);
            ArrayList<Element> domains = new ArrayList<Element>();
            NodeList images = doc.getElementsByTagName("Image");

            sections.add(currSection);

            //Find the domain contours. domains is used later to make sure we ignore those
            //contours correctly
            for (int i = 0; i < images.getLength(); ++i)
            {
                domains.add(Utils.getImageDomainContour(images.item(i)));
            }

            for (int i = 0; i < contours.getLength(); ++i)
            {
                Element e = (Element)contours.item(i);

                //Open contours and closed contours are treated differently
                if (e.getAttribute("closed").equals("true") && !domains.contains(e))
                {
                    ReconstructAreaList areaList = Utils.findContourByName(closedContours,
                            e.getAttribute("name"));
                    if (areaList == null)
                    {
                        closedContours.add(new ReconstructAreaList(e, this, currSection));
                    }
                    else
                    {
                        areaList.addContour(e, currSection);
                    }
                }
                else if (!domains.contains(e))
                {
                    ReconstructProfileList profileList = Utils.findContourByName(openContours,
                            e.getAttribute("name"));
                    if (profileList == null)
                    {
                        openContours.add(new ReconstructProfileList(e, this, currSection));
                    }
                    else
                    {
                        profileList.addContour(e, currSection);
                    }
                }
            }
        }

        fixupSections();
    }
    
    private void fixupSections()
    {
        double[] sectionThickness = new double[sections.size()];
        //List of indices with zero thickness
        Vector<Integer> zeroIndices = new Vector<Integer>();
        for (int i = 0; i < sections.size(); ++i)
        {
            sectionThickness[i] = sections.get(i).getThickness();
            if (sectionThickness[i] <= 0)
            {
                zeroIndices.add(i);
            }
        }
        
        if (zeroIndices.size() > 0)
        {
            ArrayList<Double> nbd = new ArrayList<Double>(5);

            for (int i : zeroIndices)
            {
                int offset = 1;
                int condition = 0;
                
                nbd.clear();

                //Find up to five nearest neighbors. Take the correct thickness as the neighbors'
                //median.
                while (condition < 2 && nbd.size() < 5)
                {
                    //condition == 2 indicates that we ran out of bounds in high and low indices
                    int checkIndex = i + offset;
                    if (checkIndex < 0)
                    {
                        ++condition; //low oob
                    }
                    else if (checkIndex >= sectionThickness.length)
                    {
                        ++condition; //high oob
                    }
                    else if(sectionThickness[checkIndex] > 0)
                    {
                        nbd.add(sectionThickness[checkIndex]);
                    }
                    // 1 -> -1 -> 2 -> -2 ...
                    offset = offset > 0 ? -offset : -offset + 1;
                }
                Collections.sort(nbd);
                sections.get(i).setThickness(nbd.get(nbd.size()/2));
            }

            log("There are " + zeroIndices.size() + " sections out of " + sections.size() +
                    " with zero section thickness.\nThey will be given a best-guess thickness for use with " +
                    "TrakEM2.");
        }

        sections.get(0).setZ(((double)sections.get(0).getIndex()) * sections.get(0).getThickness());
        
        for (int i = 1; i < sections.size(); ++i)
        {
            sections.get(i).setZFromPrevious(sections.get(i-1));
        }
    }


    protected String getUNUID()
    {
        return unuid;
    }

    protected String getMipMapFolder()
    {
        return "trakem2." + nuid + "/trakem2.mipmaps";
    }

    protected void appendDTD(final StringBuilder sb)
    {
        //TODO: something smarter than this.
        sb.append("<!DOCTYPE trakem2_reconstruct [\n" +
                "\t<!ELEMENT trakem2 (project,t2_layer_set,t2_display)>\n" +
                "\t<!ELEMENT project (reconstruct)>\n" +
                "\t<!ATTLIST project id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST project unuid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST project title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST project preprocessor NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST project mipmaps_folder NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST project storage_folder NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT reconstruct (reconstruct_contour, reconstruct_ztrace, reconstruct_open_trace)>\n" +
                "\t<!ATTLIST reconstruct id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST reconstruct expanded NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT reconstruct_contour (area_list)>\n" +
                "\t<!ATTLIST reconstruct_contour id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST reconstruct_contour expanded NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT area_list EMPTY>\n" +
                "\t<!ATTLIST area_list id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST area_list oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST area_list expanded NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT reconstruct_ztrace (polyline)>\n" +
                "\t<!ATTLIST reconstruct_ztrace id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST reconstruct_ztrace expanded NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT polyline EMPTY>\n" +
                "\t<!ATTLIST polyline id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST polyline oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST polyline expanded NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT reconstruct_open_trace (profile_list)>\n" +
                "\t<!ATTLIST reconstruct_open_trace id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST reconstruct_open_trace expanded NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT profile_list (profile)>\n" +
                "\t<!ATTLIST profile_list id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST profile_list oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST profile_list expanded NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT profile EMPTY>\n" +
                "\t<!ATTLIST profile id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST profile oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST profile expanded NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_layer (t2_patch,t2_label,t2_layer_set,t2_profile)>\n" +
                "\t<!ATTLIST t2_layer oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer thickness NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer z NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_layer_set (t2_prop,t2_linked_prop,t2_annot,t2_layer,t2_pipe,t2_ball,t2_area_list,t2_calibration,t2_stack,t2_treeline)>\n" +
                "\t<!ATTLIST t2_layer_set oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set composite NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set layer_width NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set layer_height NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set rot_x NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set rot_y NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set rot_z NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set snapshots_quality NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set color_cues NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set area_color_cues NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set avoid_color_cue_colors NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set n_layers_color_cue NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set paint_arrows NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set paint_edge_confidence_boxes NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_calibration EMPTY>\n" +
                "\t<!ATTLIST t2_calibration pixelWidth NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_calibration pixelHeight NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_calibration pixelDepth NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_calibration xOrigin NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_calibration yOrigin NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_calibration zOrigin NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_calibration info NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_calibration valueUnit NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_calibration timeUnit NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_calibration unit NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_ball (t2_prop,t2_linked_prop,t2_annot,t2_ball_ob)>\n" +
                "\t<!ATTLIST t2_ball oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball composite NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball fill NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_ball_ob EMPTY>\n" +
                "\t<!ATTLIST t2_ball_ob x NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball_ob y NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball_ob r NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball_ob layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_label (t2_prop,t2_linked_prop,t2_annot)>\n" +
                "\t<!ATTLIST t2_label oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_label layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_label transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_label style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_label locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_label visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_label title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_label links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_label composite NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_patch (t2_prop,t2_linked_prop,t2_annot,ict_transform,ict_transform_list)>\n" +
                "\t<!ATTLIST t2_patch oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch composite NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch file_path NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch original_path NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch type NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch ct NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch o_width NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch o_height NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch pps NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_pipe (t2_prop,t2_linked_prop,t2_annot)>\n" +
                "\t<!ATTLIST t2_pipe oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe composite NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe d NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe p_width NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe layer_ids NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_polyline (t2_prop,t2_linked_prop,t2_annot)>\n" +
                "\t<!ATTLIST t2_polyline oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_polyline layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_polyline transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_polyline style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_polyline locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_polyline visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_polyline title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_polyline links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_polyline composite NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_polyline d NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_profile (t2_prop,t2_linked_prop,t2_annot)>\n" +
                "\t<!ATTLIST t2_profile oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_profile layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_profile transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_profile style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_profile locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_profile visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_profile title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_profile links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_profile composite NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_profile d NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_area_list (t2_prop,t2_linked_prop,t2_annot,t2_area)>\n" +
                "\t<!ATTLIST t2_area_list oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_area_list layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_area_list transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_area_list style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_area_list locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_area_list visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_area_list title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_area_list links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_area_list composite NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_area_list fill_paint NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_area (t2_path)>\n" +
                "\t<!ATTLIST t2_area layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_path EMPTY>\n" +
                "\t<!ATTLIST t2_path d NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_dissector (t2_prop,t2_linked_prop,t2_annot,t2_dd_item)>\n" +
                "\t<!ATTLIST t2_dissector oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_dissector layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_dissector transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_dissector style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_dissector locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_dissector visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_dissector title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_dissector links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_dissector composite NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_dd_item EMPTY>\n" +
                "\t<!ATTLIST t2_dd_item radius NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_dd_item tag NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_dd_item points NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_stack (t2_prop,t2_linked_prop,t2_annot,(iict_transform|iict_transform_list)?)>\n" +
                "\t<!ATTLIST t2_stack oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_stack layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_stack transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_stack style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_stack locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_stack visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_stack title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_stack links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_stack composite NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_stack file_path CDATA #REQUIRED>\n" +
                "\t<!ATTLIST t2_stack depth CDATA #REQUIRED>\n" +
                "\t<!ELEMENT t2_tag EMPTY>\n" +
                "\t<!ATTLIST t2_tag name NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_tag key NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_node (t2_area*,t2_tag*)>\n" +
                "\t<!ATTLIST t2_node x NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_node y NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_node lid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_node c NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_node r NMTOKEN #IMPLIED>\n" +
                "\t<!ELEMENT t2_treeline (t2_node*,t2_prop,t2_linked_prop,t2_annot)>\n" +
                "\t<!ATTLIST t2_treeline oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_treeline layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_treeline transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_treeline style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_treeline locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_treeline visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_treeline title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_treeline links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_treeline composite NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_areatree (t2_node*,t2_prop,t2_linked_prop,t2_annot)>\n" +
                "\t<!ATTLIST t2_areatree oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_areatree layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_areatree transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_areatree style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_areatree locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_areatree visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_areatree title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_areatree links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_areatree composite NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_connector (t2_node*,t2_prop,t2_linked_prop,t2_annot)>\n" +
                "\t<!ATTLIST t2_connector oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_connector layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_connector transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_connector style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_connector locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_connector visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_connector title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_connector links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_connector composite NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_prop EMPTY>\n" +
                "\t<!ATTLIST t2_prop key NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_prop value NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_linked_prop EMPTY>\n" +
                "\t<!ATTLIST t2_linked_prop target_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_linked_prop key NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_linked_prop value NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_annot EMPTY>\n" +
                "\t<!ELEMENT t2_display EMPTY>\n" +
                "\t<!ATTLIST t2_display id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display x NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display y NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display magnification NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display srcrect_x NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display srcrect_y NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display srcrect_width NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display srcrect_height NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display scroll_step NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display c_alphas NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display c_alphas_state NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display filter_enabled NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display filter_min_max_enabled NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display filter_min NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display filter_max NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display filter_invert NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display filter_clahe_enabled NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display filter_clahe_block_size NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display filter_clahe_histogram_bins NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display filter_clahe_max_slope NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT ict_transform EMPTY>\n" +
                "\t<!ATTLIST ict_transform class CDATA #REQUIRED>\n" +
                "\t<!ATTLIST ict_transform data CDATA #REQUIRED>\n" +
                "\t<!ELEMENT iict_transform EMPTY>\n" +
                "\t<!ATTLIST iict_transform class CDATA #REQUIRED>\n" +
                "\t<!ATTLIST iict_transform data CDATA #REQUIRED>\n" +
                "\t<!ELEMENT ict_transform_list (ict_transform|iict_transform)*>\n" +
                "\t<!ELEMENT iict_transform_list (iict_transform*)>\n" +
                "] >\n");
    }

    protected void appendProject(final StringBuilder sb)
    {
        sb.append("<project\n");
        sb.append("id=\"0\"\n");
        sb.append("title=\"").append(projectName).append("\"\n");
        sb.append("unuid=\"").append(getUNUID()).append("\"\n");
        sb.append("mipmaps_folder=\"").append(getMipMapFolder()).append("\"\n");
        sb.append("storage_folder=\"\"\n");
        sb.append("mipmaps_format=\"0\"\n");
        sb.append(">\n");

        sb.append("<reconstruct id=\"").append(nextOID()).append("\" expanded=\"true\">\n");

        for (ReconstructProfileList rpl : openContours)
        {
            rpl.appendProjectXML(sb);
        }

        for (ReconstructAreaList ral : closedContours)
        {
            ral.appendProjectXML(sb);
        }

        for (ReconstructZTrace rzt : zTraces)
        {
            rzt.appendProjectXML(sb);
        }

        sb.append("</reconstruct>\n");

        sb.append("</project>\n");
    }

    protected void appendLayerSet(final StringBuilder sb)
    {
        Node image = sectionDocuments.get(0).getElementsByTagName("Image").item(0);

        sb.append("<t2_layer_set\n");
        sb.append("oid=\"").append(layerSetOID).append("\"\n");
        sb.append("width=\"20.0\"\n");
        sb.append("height=\"20.0\"\n");
        sb.append("transform=\"matrix(1.0,0.0,0.0,1.0,0.0,0.0)\"\n");
        sb.append("title=\"Top Level\"\n");
        sb.append("links=\"\"\n");
        sb.append("layer_width=\"").append(getStackWidth()).append("\"\n");
        sb.append("layer_height=\"").append(getStackHeight()).append("\"\n");
        sb.append("rot_x=\"0.0\"\n");
        sb.append("rot_y=\"0.0\"\n");
        sb.append("rot_z=\"0.0\"\n");
        sb.append("snapshots_quality=\"true\"\n");
        sb.append("snapshots_mode=\"Full\"\n");
        sb.append("color_cues=\"false\"\n");
        sb.append("area_color_cues=\"false\"\n");
        sb.append("avoid_color_cue_colors=\"false\"\n");
        sb.append("n_layers_color_cue=\"-1\"\n");
        sb.append("paint_arrows=\"true\"\n");
        sb.append("paint_edge_confidence_boxes=\"true\"\n");
        sb.append("prepaint=\"true\"\n");
        sb.append(">\n");

        appendCalibration(sb);

        for (ReconstructAreaList ral : closedContours)
        {
            ral.appendLayerSetXML(sb, sections);
        }

        for (ReconstructZTrace rzt : zTraces)
        {
            rzt.appendXML(sb);
        }

        for (ReconstructSection rs : sections)
        {
            rs.appendXML(sb);
        }

        sb.append("</t2_layer_set>\n");

    }

    protected void appendCalibration(final StringBuilder sb)
    {
        Element image = (Element) sectionDocuments.get(0).getElementsByTagName("Image").item(0);
        String mag = image.hasAttribute("t2mag") ?
                image.getAttribute("t2mag") : image.getAttribute("mag");
        String thickness = sectionDocuments.get(0).getDocumentElement().getAttribute("thickness");

        sb.append("<t2_calibration\n" +
                "pixelWidth=\"").append(mag).append("\"\n" +
                "pixelHeight=\"").append(mag).append("\"\n" +
                "pixelDepth=\"").append(thickness).append("\"\n" +
                "xOrigin=\"0.0\"\n" +
                "yOrigin=\"0.0\"\n" +
                "tzOrigin=\"0.0\"\n" +
                "info=\"null\"\n" +
                "valueUnit=\"Gray Value\"\n" +
                "timeUnit=\"sec\"\n" +
                "unit=\"pixel\"\n" +
                "/>\n");
    }

    protected void appendDisplay(final StringBuilder sb)
    {
        sb.append("<t2_display id=\"7\"\n" +
                "layer_id=\"").append(sections.get(0).getOID()).append("\"\n" +
                "c_alphas=\"-1\"\n" +
                "c_alphas_state=\"-1\"\n" +
                "x=\"1276\"\n" +
                "y=\"-3\"\n" +
                "magnification=\"0.39615630662788986\"\n" +
                "srcrect_x=\"0\"\n" +
                "srcrect_y=\"0\"\n" +
                "srcrect_width=\"4096\"\n" +
                "srcrect_height=\"2974\"\n" +
                "scroll_step=\"1\"\n" +
                "/>\n");
    }

    public int nextOID()
    {
        return ++currentOID;
    }

    public int layerIndexToOID(final int index)
    {
        for (ReconstructSection sec : sections)
        {
            if (sec.getIndex() == index)
            {
                return sec.getOID();
            }
        }
        return -1;
    }

    public double getStackHeight()
    {
        return preTransPatchSize[1];
    }

    public double getStackWidth()
    {
        return preTransPatchSize[0];
    }


    public String writeTrakEM2()
    {
        final String trakEMFile = inputFile.getParentFile().getAbsolutePath() + "/" + projectName + ".xml";

        xmlBuilder.append('\n');

        try
        {
            FileWriter fw = new FileWriter(new File(trakEMFile));
            String xmlString = xmlBuilder.toString();
            int n = xmlString.length();
            int t = n - STRING_CHUNK_SIZE;
            int i;
            xmlBuilder = null;
            System.gc();

            for (i = 0; i < t; i += STRING_CHUNK_SIZE)
            {
                fw.write(xmlString, i, STRING_CHUNK_SIZE);
            }
            fw.write(xmlString, i, n - i - 1);
            fw.close();
            return trakEMFile;
        }
        catch (IOException ioe)
        {
            return null;
        }
    }

    public double getMag()
    {
        return defaultMag;
    }


    /**
     * Reconstruct XML Section Documents may be broken in several ways, with respect to
     * TrakEM2 translation. This function fixes that.
     * @param secDocs Reconstruct XML section documents to be fixed.
     * @return a message to display to the user if the XML fix requires some attention.
     */
    private String fixXML(final Collection<Document> secDocs)
    {
        String message;
        message = fixNonlinearTransforms(secDocs);
        message += fixTransformScale(secDocs);
        return message;
    }

    private String fixTransformScale(final Collection<Document> secDocs)
    {
        double[] sectionScale = new double[secDocs.size()];
        double minScale = calculateTransformScale(secDocs, sectionScale);
        double maxScale = minScale;

        if (minScale != 1.0 && !Double.isInfinite(minScale))
        {
            for (final Document d : secDocs)
            {
                if (!nonlinearDocument(d))
                {
                    final NodeList nl = d.getElementsByTagName("Transform");
                    for (int i = 0; i < nl.getLength(); ++i)
                    {
                        unscaleTransform((Element)nl.item(i), minScale);
                    }
                }
            }
        }

        for (double scale : sectionScale)
        {
            if (scale != 0 && !Double.isInfinite(scale))
            {
                if (Math.abs(scale - 1) > Math.abs(maxScale - 1))
                {
                    maxScale = scale;
                }
            }
        }

        if (Double.isInfinite(minScale))
        {
            messenger.sendMessage("Reconstruct project has no golden section." +
                    " This may or may not be problematic");
            return "";
        }
        else if (maxScale != minScale)
        {
            return "This Reconstruct project has been re-calibrated using the scale method.\n" +
                    "The detected scale was " + minScale + ", but multiple valid scales were" +
                    " detected,\nthe extremum of which was " + maxScale + ".\nPlease verify that " +
                    " the areas of your traces have not changed.";

        }
        else if (minScale != 1)
        {
            return "This Reconstruct project has been re-calibrated using the scale method.\n" +
                    "The detected scale was " + minScale + ".\nPlease verify that " +
                    " the areas of your traces have not changed.";
        }
        else
        {
            return "";
        }
    }

    private double calculateTransformScale(final Collection<Document> secDocs,
                                           double[] sectionScale)
    {
        double scale = Double.POSITIVE_INFINITY;
        double scaleDistance = scale - 1.0;
        int sIndex = 0;

        for (final Document doc : secDocs)
        {
            final Element trans = Utils.getFirstImageTransformElement(doc);

            sectionScale[sIndex] = 0;

            // If there actually *is* an image transform (sometimes there isn't), and if that
            // image transform hasn't been explicity set to the identity...
            if (trans != null && !nonlinearDocument(doc))
            {
                double[] xcoefs = Utils.createNodeValueVector(trans.getAttribute("xcoef"));
                double[] ycoefs = Utils.createNodeValueVector(trans.getAttribute("ycoef"));
                Utils.nodeValueToVector(trans.getAttribute("xcoef"), xcoefs);
                Utils.nodeValueToVector(trans.getAttribute("ycoef"), ycoefs);

                boolean ok = true;

                // We consider only transforms that consist of at most a scale and translation
                for (int i : new int[]{2, 3, 4, 5})
                {
                    ok &= xcoefs[i] == 0;
                }
                for (int i : new int[]{1, 3, 4, 5})
                {
                    ok &= ycoefs[i] == 0;
                }
                ok &= xcoefs[1] == ycoefs[2];

                if (ok)
                {
                    double transScale = xcoefs[1];
                    double transScaleDistance = Math.abs(transScale - 1.0);

                    sectionScale[sIndex] = scale;

                    if (transScaleDistance < scaleDistance)
                    {
                        scale = transScale;
                        scaleDistance = transScaleDistance;
                    }
                }
            }

            ++sIndex;

        }

        return scale;
    }

    private void unscaleTransform(final Element trans, final double scale)
    {
        final int dim = Integer.valueOf(trans.getAttribute("dim"));
        final double unscale = 1 / scale;
        final double[] xcoefs = Utils.createNodeValueVector(trans.getAttribute("xcoef"));
        final double[] ycoefs = Utils.createNodeValueVector(trans.getAttribute("ycoef"));
        final NodeList nl = trans.getChildNodes();

        Utils.nodeValueToVector(trans.getAttribute("xcoef"), xcoefs);
        Utils.nodeValueToVector(trans.getAttribute("ycoef"), ycoefs);

        switch (dim)
        {
            case 0:
                trans.setAttribute("dim", "2");
                trans.setAttribute("xcoef", "0 " + unscale + " 0 0 0 0");
                trans.setAttribute("ycoef", "0 " + unscale + " 0 0 0 0");
                break;
            case 1:
                trans.setAttribute("dim", "2");
                trans.setAttribute("xcoef", "" + xcoefs[0] + " " + unscale + " 0 0 0 0");
                trans.setAttribute("ycoef", "" + ycoefs[0] + " " + unscale + " 0 0 0 0");
                break;
            case 2:
                trans.setAttribute("xcoef", "" + xcoefs[0] + " " + (unscale * xcoefs[1]) +
                        " 0 0 0 0");
                trans.setAttribute("ycoef", "" + ycoefs[0] + " " + (unscale * ycoefs[1]) +
                        " 0 0 0 0");
                break;
            default:
                //fixNonlinearTransforms should precede this call, so we don't treat dim > 3
                //any differently
                trans.setAttribute("xcoef", "" + xcoefs[0] + " " + (unscale * xcoefs[1]) +
                        " " + (unscale * xcoefs[2]) + " 0 0 0");
                trans.setAttribute("ycoef", "" + ycoefs[0] + " " + (unscale * ycoefs[1]) +
                        " " + (unscale * ycoefs[2]) + " 0 0 0");
                break;
        }

        for (int i = 0; i < nl.getLength(); ++i)
        {
            if (nl.item(i).getNodeName().equals("Image"))
            {
                final Element im = (Element)nl.item(i);
                final double mag = Double.parseDouble(im.getAttribute("mag"));
                messenger.sendMessage("Image mag changed from " + mag + " to " + (mag * unscale));
                im.setAttribute("t2mag", "" + (mag * unscale));
            }

        }
    }

    /**
     * This function fixes the XML so that we present TrakEM2 with only affine transforms. TrakEM2
     * can't handle Reconstruct's nonliner transforms, and mucking around with the translation code
     * is rather hairy, so we fix it in XML first. Note: This is a quick fix. Given that this is
     * easier to do than fixing the translation code, I think it might be best to do a re-write, but
     * ain't nobody got time for that.
     *
     * 1) If we encounter a nonlinearly-transformed image, we set it to the identity transform and
     * convert all of the traces so that they will lay nicely over the image as if they were
     * originally traced there.
     * 2) In addition, if we encounter any traces with nonlinear transforms, we apply the transform
     * to the trace, then set the transform to the identity.
     *
     * @param secDocs a List of XML Documents representing the Section files
     * @return a message to display to the user, if a nonlinear image transform was fixed.
     */
    private String fixNonlinearTransforms(final Collection<Document> secDocs)
    {
        String message = "";
        String unalignedMessage = "Nonlinear Reconstruct alignments are incompatible with TrakEM2.\n" +
                "At least one was found in your project.\n" +
                "Each such section has been reset to an unaligned section.\n\n";

        for (final Document secDoc : secDocs)
        {
            // Check the image transform for non-linearity
            // If the image is nonlinear, we rewrite the whole section
            Element transform;
            if (isNonLinear(transform = Utils.getFirstImageTransformElement(secDoc)))
            {
                message = unalignedMessage;
                fixImageTransform(secDoc, imageElement(transform));
            }
            else
            {
                // If the image has a linear transforms, then we iterate through the contour
                // transforms. When we find a nonlinear transform here, we simply apply it to the
                // trace points, then set the transform to the identity.
                final NodeList transforms = secDoc.getElementsByTagName("Transform");
                for (int i = 0; i < transforms.getLength(); ++i)
                {
                    transform = (Element)transforms.item(i);

                    if (imageElement(transform) == null)
                    {
                        fixContourTransforms(transform);
                    }
                }
            }
        }
        return message;
    }

    private boolean isNonLinear(final Element transform)
    {

        if (transform == null)
        {
            return false;
        }
        else
        {
            final int dim = Integer.parseInt(transform.getAttribute("dim"));

            if (dim > 3)
            {

                final double[] xcoef = new double[6];
                final double[] ycoef = new double[6];
                boolean test = false;

                Utils.nodeValueToVector(transform.getAttribute("xcoef"), xcoef);
                Utils.nodeValueToVector(transform.getAttribute("ycoef"), ycoef);

                for (int j = 3; j < 6 && !test; ++j)
                {
                    if (xcoef[j] != 0 || ycoef[j] != 0)
                    {
                        return true;
                    }
                }

                return false;
            }
            else
            {
                return false;
            }
        }
    }
    
    private Element imageElement(final Element transform)
    {
        final NodeList children = transform.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i)
        {
            if (children.item(i).getNodeName().equals("Image"))
            {
                return (Element)children.item(i);
            }
        }
        return null;
    }

    private boolean nonlinearDocument(final Document doc)
    {
        return doc.getDocumentElement().hasAttribute("nonlinear");
    }

    private void fixImageTransform(final Document secDoc, final Element image)
    {
        final Element imageTransform = (Element)image.getParentNode();
        final double[] xcoef = new double[6];
        final double[] ycoef = new double[6];
        Utils.nodeValueToVector(imageTransform.getAttribute("xcoef"), xcoef);
        Utils.nodeValueToVector(imageTransform.getAttribute("ycoef"), ycoef);
        
        final NodeList transforms = secDoc.getElementsByTagName("Transform");
        
        for (int i = 0; i < transforms.getLength(); ++i)
        {
            final Element transform = (Element)transforms.item(i);
            if (transform != imageTransform)
            {
                final NodeList contours = fixContourTransforms(transform);
                for (int j = 0; j < contours.getLength(); ++j)
                {
                    applyTransform(contours.item(j), xcoef, ycoef, 6, true);
                }
            }
        }
        

        imageTransform.setAttribute("xcoef", "0 1 0 0 0 0");
        imageTransform.setAttribute("ycoef", "0 0 1 0 0 0");
        imageTransform.setAttribute("dim", "0");

        secDoc.getDocumentElement().setAttribute("nonlinear", "true");
    }
    
    private NodeList fixContourTransforms(final Element transform)
    {
        final int dim = Integer.parseInt(transform.getAttribute("dim"));
        final double[] xcoef = new double[6];
        final double[] ycoef = new double[6];
        final NodeList contours = transform.getElementsByTagName("Contour");

        Utils.nodeValueToVector(transform.getAttribute("xcoef"), xcoef);
        Utils.nodeValueToVector(transform.getAttribute("ycoef"), ycoef);

        for (int j = 0; j < contours.getLength(); ++j)
        {
            applyTransform(contours.item(j), xcoef, ycoef, dim, false);
        }

        transform.setAttribute("xcoef", "0 1 0 0 0 0");
        transform.setAttribute("ycoef", "0 0 1 0 0 0");
        transform.setAttribute("dim", "0");

        return contours;
    }
    
    private void applyTransform(final Node node, double[] xcoef, double[] ycoef, int dim,
                                boolean fwd)
    {
        final Element contour = (Element)node;
        final String strPoints = contour.getAttribute("points");
        final double[] pts = Utils.createNodeValueVector(strPoints);
        final StringBuilder sbTransPts = new StringBuilder(); 
        Utils.nodeValueToVector(strPoints, pts);
        
        if (fwd)
        {
            fwdTrans(xcoef, ycoef, pts, dim);
        }
        else
        {
            revTrans(xcoef, ycoef, pts, dim);
        }

        for (int i = 0; i < pts.length; i+=2)
        {
            sbTransPts.append(pts[i]).append(" ").append(pts[i+1]).append(",\n");
        }

        contour.setAttribute("points", sbTransPts.toString());
    }

    private void fwdTrans(double[] a, double[] b, double[] pts, int dim)
    {

        if (dim <= 3)
        {
            final double[] matrix = new double[6];
            AffineTransform at;
            switch (dim)
            {
                case 0:
                    matrix[0] = 1;
                    matrix[3] = 1;
                case 1:
                    matrix[0] = 1;
                    matrix[3] = 1;
                    matrix[4] = a[0];
                    matrix[5] = b[0];// ::sob::
                    break;
                case 2:
                    matrix[0] = a[1];
                    matrix[3] = b[1];
                    matrix[4] = a[0];
                    matrix[5] = a[0];// why would you do that?
                    break;
                case 3:
                    matrix[0] = a[1];
                    matrix[1] = b[1];
                    matrix[2] = a[2];
                    matrix[3] = b[2];
                    matrix[4] = a[0];
                    matrix[5] = b[0];
                    break;
            }
            at = new AffineTransform(matrix);

            at.transform(pts, 0, pts, 0, pts.length / 2);
        }
        else
        {
            for (int i = 0; i < pts.length; i+=2)
            {
                double x = pts[i];
                double y = pts[i + 1];
                pts[i] = a[0] + (a[1] + a[3] * y + a[4] * x) * x + (a[2] + a[5] * y) * y;
                pts[i + 1] = b[0] + (b[1] + b[3] * y + b[4] * x) * x + (b[2] + b[5] * y) * y;
            }
        }

    }

    private void revTrans(double[] a, double[] b, double[] pts, int dim)
    {

        if (dim <= 3)
        {
            final double[] matrix = new double[6];
            AffineTransform at;
            switch (dim)
            {
                case 0:
                    matrix[0] = 1;
                    matrix[3] = 1;
                case 1:
                    matrix[0] = 1;
                    matrix[3] = 1;
                    matrix[4] = a[0];
                    matrix[5] = b[0];
                    break;
                case 2:
                    matrix[0] = a[1];
                    matrix[3] = b[1];
                    matrix[4] = a[0];
                    matrix[5] = a[0];
                    break;
                case 3:
                    matrix[0] = a[1];
                    matrix[1] = b[1];
                    matrix[2] = a[2];
                    matrix[3] = b[2];
                    matrix[4] = a[0];
                    matrix[5] = b[0];
                    break;
            }
            at = new AffineTransform(matrix);
            try
            {
                at.inverseTransform(pts, 0, pts, 0, pts.length / 2);
            }
            catch (NoninvertibleTransformException nite)
            {
                /** do nothing **/
            }
        }
        else
        {
            // Lifted from Reconstruct source, nform.cpp::XYinverse
            int i;
            double epsilon = 5e-10;
            double e,l,m,n,o,p,x0,y0,u,v,x,y;
            double[] uv0;

            for (int j = 0; j < pts.length; j+=2)
            {
                x = pts[j];
                y = pts[j + 1];
                uv0 = new double[]{0,0};

                u = x;							// (u,v) for which we want (x,y)
                v = y;
                x0 = 0.0;						// initial guess of (x,y)
                y0 = 0.0;
                fwdTrans(a, b, uv0, dim);
                //u0 = X(x0,y0);					//	get forward tform of initial guess
                //v0 = Y(x0,y0);
                i = 0;							// allow no more than 10 iterations
                e = 1.0;						// to reduce error to this limit
                while ( (e > epsilon) && (i<10) ) {
                    i++;
                    l = a[1] + a[3]*y0 + 2.0*a[4]*x0;	// compute Jacobian
                    m = a[2] + a[3]*x0 + 2.0*a[5]*y0;
                    n = b[1] + b[3]*y0 + 2.0*b[4]*x0;
                    o = b[2] + b[3]*x0 + 2.0*b[5]*y0;
                    p = l*o - m*n;						// determinant for inverse
                    if ( Math.abs(p) > epsilon ) {
                        x0 += (o*(u-uv0[0]) - m*(v-uv0[1]))/p;	// inverse of Jacobian
                        y0 += (l*(v-uv0[1]) - n*(u-uv0[0]))/p;	// and use to increment (x0,y0)
                    }
                    else {
                        x0 += l*(u-uv0[0]) + n*(v-uv0[1]);		// try Jacobian transpose instead
                        y0 += m*(u-uv0[0]) + o*(v-uv0[1]);
                    }
                    uv0 = new double[]{x0, y0};
                    fwdTrans(a, b, uv0, dim);
                    e = Math.abs(u-uv0[0]) + Math.abs(v-uv0[1]);		// compute closeness to goal
                }

                pts[j] = x0;
                pts[j + 1] = y0;
            }
        }
    }

}
