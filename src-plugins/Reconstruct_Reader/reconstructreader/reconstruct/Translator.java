package reconstructreader.reconstruct;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import reconstructreader.Utils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Translator {

    private static class MalformedFileInputStream extends FilterInputStream
    {

        int fixCnt;
        int[] callCnt;
        //int lastInt;
        byte[] lastByte;

        public MalformedFileInputStream(File f) throws FileNotFoundException
        {
            super(new FileInputStream(f));
            fixCnt = 0;
            callCnt = new int[3];
            Arrays.fill(callCnt, 0);
            //lastInt = 0;
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

//                //Handle ampersand
//                if (bs[i] == 0x26)
//                {
//                    if (bs.length - i < 5)
//                    {
//                        bs[i] = 0x20;
//                    }
//                    else
//                    {
//                        byte[] amp = {0x61, 0x6d, 0x70, 0x3b};
//                        byte[] lt = {0x6c, 0x74, 0x3b};
//                        byte[] gt = {0x67, 0x74, 0x3b};
//                        byte[] bs4 = new byte[4];
//                        byte[] bs3 = new byte[3];
//                        System.arraycopy(bs, i + 1, bs4, 0, 4);
//                        System.arraycopy(bs, i + 1, bs3, 0, 3);
//
//                        if (!(Arrays.equals(amp, bs4) || Arrays.equals(lt, bs3) || Arrays.equals(gt, bs3)))
//                        {
//                            bs[i] = 0x20;
//                        }
//                    }
//                }
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

    //private final String fileName;
    private final String projectName;
    private final String unuid;
    private final String nuid;
    private final File inputFile;

    private int currentOID;

    private final int layerSetOID;

    private double[] preTransPatchSize;
    private double defaultMag;

    private boolean ready;

    private static String lastok = "";

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
        try
        {
            Document d = builder.parse(new InputSource(charStream));
            sectionDocuments.add(d);
            lastok = fileStream.toString();
        }
        catch (SAXParseException spe)
        {
            System.err.println(lastok);
            System.err.println(fileStream);
            throw spe;
        }
    }

    public Translator(String f)
    {
        inputFile = new File(f);
        // Parse out the path
        final String localFile = inputFile.getName();
        final File seriesDTD = new File(inputFile.getParent() + "/series.dtd");
        final File sectionDTD = new File(inputFile.getParent() + "/section.dtd");

        xmlBuilder = null;
        sectionDocuments = new ArrayList<Document>();
        sections = new ArrayList<ReconstructSection>();
        zTraces = new ArrayList<ReconstructZTrace>();
        closedContours = new ArrayList<ReconstructAreaList>();
        openContours = new ArrayList<ReconstructProfileList>();

        projectName = (localFile.endsWith(".ser") || localFile.endsWith(".SER")) ?
                localFile.substring(0, localFile.length() - 4) : localFile;
        //fileName = f;
        currentOID = -1;

        nuid = Integer.toString(projectName.hashCode());
        unuid = Long.toString(System.currentTimeMillis()) + "." + nuid;

        layerSetOID = nextOID();

        // Make sure that the DTD files exist. Apparently it doesn't matter a
        // whole lot if they actually contain anything.
        try
        {
            if (!seriesDTD.exists())
            {
                FileWriter fw = new FileWriter(seriesDTD);
                fw.write("");
                fw.close();
                seriesDTD.deleteOnExit();
            }

            if (!sectionDTD.exists())
            {
                FileWriter fw = new FileWriter(sectionDTD);
                fw.write("");
                fw.close();
                sectionDTD.deleteOnExit();
            }

            ready = true;
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
            xmlBuilder.append(ioe.getStackTrace());

            ready = false;
        }
    }

    public int getLayerSetOID()
    {
        return layerSetOID;
    }


    public boolean process()
    {
        if (ready)
        {
            File[] list;

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
            list = inputFile.getParentFile().listFiles(
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
                DocumentBuilder builder =
                        DocumentBuilderFactory.newInstance().newDocumentBuilder();

                serDoc = builder.parse(inputFile);

                for (File f : list)
                {
                    System.out.println("Opening file " + f.getName());
                    addSection(sectionDocuments, builder, f);
                }

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

                collectContours(sectionDocuments);
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
            catch (Exception e)
            {
                clearMemory();
                e.printStackTrace();
                return false;
            }
        }
        else
        {
            return false;
        }
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

    protected void collectContours(List<Document> sectionDocs)
    {
        for (Document doc : sectionDocs)
        {
            NodeList contours = doc.getElementsByTagName("Contour");
            ReconstructSection currSection = new ReconstructSection(this, doc);
            sections.add(currSection);

            for (int i = 0; i < contours.getLength(); ++i)
            {
                Element e = (Element)contours.item(i);
                if (e.getAttribute("closed").equals("true"))
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
                else
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
        String mag = image.getAttribute("mag");
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
}
