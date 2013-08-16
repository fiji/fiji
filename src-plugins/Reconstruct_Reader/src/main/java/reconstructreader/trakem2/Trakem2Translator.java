package reconstructreader.trakem2;

import ini.trakem2.Project;
import ini.trakem2.display.*;
import ini.trakem2.tree.ProjectThing;
import ini.trakem2.tree.Thing;
import reconstructreader.Utils;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Trakem2Translator implements Runnable
{
    private final String name;
    private final File dir;
    private final Project project;
    private final double mag;
    private final LayerSet rootLayerSet;
    private final SeriesTemplate template;
    private final Display display;
    private boolean success;
    private final List<ProjectThing> reconstructThings;
    
    
    private class ProfileData
    {
        public final Profile profile;
        public final ProjectThing parent;
        
        public ProfileData(Profile profileIn, ProjectThing parentIn)                
        {
            profile = profileIn;
            parent = parentIn;
        }
    }
    
    
    public Trakem2Translator(final Project p, final File outdir)
    {
        this (p, outdir, p.getTitle().replace(".xml", "") + " export");
    }

    public Trakem2Translator(final Project p, final File outdir, final String projectName)
    {
        project = p;
        dir = outdir;
        name = projectName;
        rootLayerSet = p.getRootLayerSet(); 
        mag = (rootLayerSet.getCalibration().pixelHeight + rootLayerSet.getCalibration().pixelWidth) / 2.0;
        template = DefaultTemplate.getTemplate();
        display = Display.getFront(project);
        success = false;

        ProjectThing rootPT = project.getRootProjectThing();
        reconstructThings = rootPT.findChildrenOfType("reconstruct");

        

    }

    public void run()
    {
        try
        {
            writeSeriesDoc();
            writeSectionDocs();
            success = true;
        }
        catch (IOException ioe)
        {
            System.err.println("IOException while writing files");
            success = false;
        }
    }

    public boolean getSuccess()
    {
        return success;
    }
    
    private void writeSeriesDoc() throws IOException
    {
        File serFile = new File(dir, name + ".ser");
        StringBuilder sb = new StringBuilder();
        Layer frontLayer = Display.getFrontLayer(project);
        //Rectangle srcRect = display.getCanvas().getSrcRect();
        FileOutputStream fos;
        OutputStreamWriter writer;

        if (frontLayer == null)
        {
            frontLayer = project.getRootLayerSet().getLayer(0);
        }

        sb.append("<?xml version = \"1.0\"?>\n");
        sb.append("<!DOCTYPE Series SYSTEM \"series.dtd\">\n");
        sb.append("<Series index=\"").append(project.getRootLayerSet().indexOf(frontLayer)).append("\"\n");
        sb.append("");
        
        for (String key : Utils.getSeriesKeys())
        {
            sb.append(key).append("=\"").append(template.getValue(key)).append("\"\n");
        }
        
        sb.append(">\n");
        
        template.setContourText(sb);

        sb.append("</Series>\n");

        fos = new FileOutputStream(serFile);
        writer = new OutputStreamWriter(new BufferedOutputStream(fos), "8859_1");

        writer.write(sb.toString());
        writer.flush();
        writer.close();
    }
    
    private HashMap<Long, ArrayList<ProfileData>> sortProfileLists()
    {
        final HashMap<Long, ArrayList<ProfileData>> map = new HashMap<Long, ArrayList<ProfileData>>();
        
        for (Layer layer : rootLayerSet.getLayers())
        {
            map.put(layer.getId(), new ArrayList<ProfileData>());
        }
        
        for (final ProjectThing rt : reconstructThings)
        {
            List<ProjectThing> profileLists = rt.findChildren("profile_list", null, false);

            for (ProjectThing pList : profileLists)
            {
                List<Profile> profiles = pList.findChildrenOfType(Profile.class);

                for (Profile p : profiles)
                {
                    long id = p.getLayer().getId(); 
                    map.get(id).add(new ProfileData(p, pList));

                }
            }
        }
        
        return map;
    }
    
    private void writeSectionDocs() throws IOException
    {
        final List<AreaList> areaLists = rootLayerSet.getAll(AreaList.class);
        final int numLayers = rootLayerSet.size();
        final Rectangle r = project.getRootLayerSet().get2DBounds();
        final float w = r.width;
        final float h = r.height;
        final HashMap<Long, ArrayList<ProfileData>> profileMap = sortProfileLists();
        
        for (int l = 0; l < numLayers; ++l)
        {
            String im = rootLayerSet.getLayer(l).getPrintableTitle() + ".tif";
            final File sectionFile = new File(dir, name + "." + l);
            final FileOutputStream fos = new FileOutputStream(sectionFile);
            final OutputStreamWriter writer = new OutputStreamWriter(
                    new BufferedOutputStream(fos), "8859_1");

            //Write the header out
            writer.write("<?xml version=\"1.0\"?>\n");
            writer.write("<!DOCTYPE Series SYSTEM \"series.dtd\">\n\n");
            writer.write("<Section index=\"" + l + "\" thickness=\"" +
                    rootLayerSet.getLayer(l).getCalibratedThickness() + " \" " +
                    "alignLocked=\"true\" >\n");
            //Append Image Transform
            writer.write("<Transform dim=\"0\"\n" +
                    "xcoef=\"0 1 0 0 0 0\"\n" +
                    "ycoef=\"0 0 1 0 0 0\">\n");
            writer.write("<Image mag=\"" + mag + "\" contrast=\"1.14434\" " +
                    "brightness=\"-0.15\" red=\"true\" green=\"true\" blue=\"true\" " +
                    "src=\"" + im + "\" />\n");
            writer.write("<Contour name=\"domain1\" hidden=\"false\" closed=\"true\" " +
                    "simplified=\"false\" border=\"1 0 1\" fill=\"1 0 1\" mode=\"11\" " +
                    "points=\" 0 0,\n" +
                    "\t " + w + " 0,\n" +
                    "\t " + w + " " + h + ",\n" +
                    "\t 0 " + h + ",\n" +
                    "\t\" />\n");
            writer.write("</Transform>\n");
            
            //write out a unity Transform
            writer.write("<Transform dim=\"0\"\n" +
                    "xcoef=\"0 1 0 0 0 0\"\n" +
                    "ycoef=\"0 0 1 0 0 0\">\n");
            
            //Push all of the closed Contours
            for (final AreaList al : areaLists)
            {
                final Area area = al.getArea(rootLayerSet.getLayer(l));
                if (area != null && !area.isEmpty())
                {
                    final Thing thing = project.findProjectThing(al).getParent();
                    final PathIterator pathIter = area.getPathIterator(al.getAffineTransform());
                    final float[] rgb = new float[3];                    

                    al.getColor().getColorComponents(rgb);

                    while (!pathIter.isDone())
                    {                        
                        ArrayList<float[]> path = getNextPath(pathIter);
                        
                        writePathXML(writer, al, thing, path, true, h);
                    }
                }
            }

            //Push all of the open Contours
            
            for (ProfileData profileData : profileMap.get(rootLayerSet.getLayer(l).getId()))
            {
                ArrayList<float[]> path = getPathFromProfile(profileData.profile);
                writePathXML(writer, profileData.profile, profileData.parent.getParent(), path, false, h);
            }
            
            
            writer.write("</Transform>");
            writer.write("</Section>");
            
            writer.flush();
            writer.close();
        }
        
    }
    
    private void writePathXML(final Writer writer, final Displayable d, final Thing t,
                              final List<float[]> path, final boolean isClosed, final float h)
            throws IOException
    {
        String alname = t.getTitle();
        float[] rgb = new float[3];
        boolean isVisible = d.isVisible();
        
        d.getColor().getColorComponents(rgb);        
        
        if (!path.isEmpty())
        {
            String tab = "";
            writer.write("<Contour name=\"" + alname + "\" hidden=\"" + !isVisible);
            writer.write("\" closed=\"" + isClosed + "\" simplified=\"true\" ");
            writer.write("border=\"" + rgb[0] + " " + rgb[1] + " " + rgb[2] + "\" ");
            writer.write("fill=\"" + rgb[0] + " " + rgb[1] + " " + rgb[2] + "\" ");
            writer.write("mode=\"11\" points=\"");
            for (float[] pt : path)
            {
                writer.write(tab + " " + pt[0] * mag + " " + (h - pt[1]) * mag + ",\n");
                tab = "\t";
            }
            writer.write(tab + "\" />\n");
        }
    }
    
    private ArrayList<float[]> getPathFromProfile(final Profile profile)
    {
        final ArrayList<float[]> path = new ArrayList<float[]>();
        final AffineTransform at = profile.getAffineTransform();
        final double[][][] ba = profile.getBezierArrays();

        if (profile.getPointCount() > 0)
        {
            double[][] pts = profile.transformPoints(ba[1]);
            double[] xpts = pts[0];
            double[] ypts = pts[1];
            for (int i = 0; i < xpts.length; ++i)
            {
                path.add(new float[]{(float)xpts[i], (float)ypts[i]});
            }
        }

        return path;
    }
    
    private ArrayList<float[]> getNextPath(final PathIterator pi)
    {
        final ArrayList<float[]> path = new ArrayList<float[]>();
        boolean cont = !pi.isDone();
        int type;
        boolean firstPt = true;

        while (cont)
        {
            float[] pt = new float[2];
            pt[0] = Float.NaN;
            pt[1] = Float.NaN;
            type = pi.currentSegment(pt);
            if (type == PathIterator.SEG_MOVETO && !firstPt)
            {
                cont = false;
            }
            else
            {
                if (!Float.isNaN(pt[0]))
                {
                    path.add(pt);
                }
                pi.next();
                cont = !pi.isDone();
                firstPt = false;
            }
        }

        float[] start = path.get(0);
        float[] end = path.get(path.size() - 1);
        
        if (start[0] == end[0] && start[1] == end[1])
        {
            // This is probably true for all paths, but it's good to be careful
            path.remove(path.size() - 1);
        }
        
        if (isCCW(path))
        {
            Collections.reverse(path);
        }

        return path;
    }
    
    private boolean isCCW(List<float[]> path)
    {
        // Test by signed area.
        float twiceA = 0;
        for (int i = 0; i + 1< path.size(); ++i)
        {
            float[] pt0 = path.get(i);
            float[] pt1 = path.get(i + 1);
            twiceA += pt0[0] * pt1[1] - pt0[1] * pt1[0];
        }

        float[] pt0 = path.get(path.size() - 1);
        float[] pt1 = path.get(0);
        twiceA += pt0[0] * pt1[1] - pt0[1] * pt1[0];
        
        return twiceA >= 0;
    }
    
}
