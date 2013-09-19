package edu.utexas.clm.reconstructreader;

import edu.utexas.clm.reconstructreader.reconstruct.ContourSet;
import org.w3c.dom.*;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public final class Utils {

    public final static String DELIM = " \t\n\r\f";

    private Utils(){}


    public static int sectionIndex(final Node n)
    {
        Document d = n.getOwnerDocument();
        Element e = d.getDocumentElement();
        return Integer.valueOf(e.getAttribute("index"));
    }

    public static double[] getReconstructStackSize(final List<Document> sections) {
        double maxImageHeight = 0;
        double maxImageWidth = 0;
        for (Document doc : sections)
        {
            NodeList images = doc.getElementsByTagName("Image");
            for (int i = 0; i < images.getLength(); ++i)
            {
                Node image = images.item(i);
                double[] wh = getReconstructImageWH(image);
                if (wh[0] > maxImageWidth)
                {
                    maxImageWidth = wh[0];
                }
                if (wh[1] > maxImageHeight)
                {
                    maxImageHeight = wh[1];
                }
            }
        }
        return new double[]{maxImageWidth, maxImageHeight};
    }


    public static class ReconstructSectionIndexComparator implements Comparator<Document>
    {
        public int compare(final Document o1, final Document o2) {
            Integer index1 = Integer.valueOf(o1.getDocumentElement().getAttribute("index"));
            Integer index2 = Integer.valueOf(o2.getDocumentElement().getAttribute("index"));
            return index1.compareTo(index2);
        }
    }

    public static AffineTransform reconstructTransform(final Element trans, double mag, double k)
    {
        return reconstructTransform(trans, mag, k, 1, true);
    }

    public static AffineTransform reconstructTransform(final Element trans, double mag, double k, double zoom)
    {
        return reconstructTransform(trans, mag, k, zoom, false);
    }

    public static AffineTransform reconstructTransform(final Element trans, double mag, double k, double zoom, boolean doFlip)
    {
        int dim = Integer.valueOf(trans.getAttribute("dim"));
        double[] matrix = new double[6];
        double[] unFuckedMatrix;
        double[] xcoef = createNodeValueVector(trans.getAttribute("xcoef"));
        double[] ycoef = createNodeValueVector(trans.getAttribute("ycoef"));
        AffineTransform at;

        Utils.nodeValueToVector(trans.getAttribute("xcoef"), xcoef);
        Utils.nodeValueToVector(trans.getAttribute("ycoef"), ycoef);

        for (int i = 0; i < 6; ++i)
        {
            matrix[i] = 0.0f;
        }

        switch (dim)
        {
            case 0:
                matrix[0] = 1;
                matrix[3] = 1;
            case 1:
                matrix[0] = 1;
                matrix[3] = 1;
                matrix[4] = xcoef[0] / mag;
                matrix[5] = ycoef[0] / mag;
                break;
            case 2:
                matrix[0] = xcoef[1];
                matrix[3] = ycoef[1];
                matrix[4] = xcoef[0] / mag;
                matrix[5] = ycoef[0] / mag;
                break;
            case 3:
                matrix[0] = xcoef[1];
                matrix[1] = ycoef[1];
                matrix[2] = xcoef[2];
                matrix[3] = ycoef[2];
                matrix[4] = xcoef[0] / mag;
                matrix[5] = ycoef[0] / mag;
                break;
            default:
                int index = Integer.valueOf(
                        trans.getOwnerDocument().getDocumentElement().getAttribute("index"));
                boolean weird = false;
                matrix[0] = xcoef[1];
                matrix[1] = ycoef[1];
                matrix[2] = xcoef[2];
                matrix[3] = ycoef[2];
                matrix[4] = xcoef[0] / mag;
                matrix[5] = ycoef[0] / mag;
                for (int i = 3; i < 6; ++i)
                {
                    weird |= xcoef[i] != 0 || ycoef[i] != 0;
                }
                if (weird && !trans.hasAttribute("weird"))
                {
                    System.err.println("Non affine tranforms are unsupported." +
                            " Expect weirdness at index " + index);
                    trans.setAttribute("weird", "true");
                }
                break;
        }

        at = new AffineTransform(matrix);

        /*
        Reconstruct uses an image coordinate system in which the origin is at the
        bottom-left, whereas TrakEM2 uses one from the top-left. Reconstruct, in addition,
        stores the transform inverse, in other words, the transform that would convert the
        registered image into the raw on-disk image. Finally, Images in Reconstruct are
        stored with different scaling from (some) contours. (except the domain contours,
        which are scaled the same as images).

        The following bit of code fixes all of this.
        */

        try
        {
            at.invert();
        }
        catch (NoninvertibleTransformException nite)
        {
            System.err.println("Found noninvertible matrix, leaving it the way it is.");
        }


        // x' = m00 x + m01 y + m02
        // y' = m10 x + m11 y + m12
        at.getMatrix(matrix);
        unFuckedMatrix = matrix.clone();

        if (doFlip)
        {
            unFuckedMatrix[1] = -matrix[1]; // m10 = -m10
            unFuckedMatrix[2] = -matrix[2]; // m11 = -m11
            unFuckedMatrix[4] = matrix[4] + k * matrix[2]; // m02 = m02 + k m01
            unFuckedMatrix[5] = k - matrix[5] - k * matrix[3]; //m12 = k - m12 - k m11
            //was 2 * k - matrix[5] ...
        }

        unFuckedMatrix[3] = unFuckedMatrix[3] * zoom;
        unFuckedMatrix[0] = unFuckedMatrix[0] * zoom;

        return new AffineTransform(unFuckedMatrix);
    }

    public static String transformToString(final AffineTransform trans)
    {
        double[] mat = new double[6];
        trans.getMatrix(mat);
        return transformToString(mat);
    }

    public static String transformToString(final double[] matrix)
    {
        StringBuilder transSB = new StringBuilder();
        Formatter f = new Formatter(transSB, Locale.US);
        transSB.append("matrix(");
        for (int i = 0; i < matrix.length - 1; ++i)
        {
            f.format("%f,", matrix[i]);
            //transSB.append(matrix[i]);
            //transSB.append(",");
        }
        //transSB.append(matrix[matrix.length - 1]);
        //transSB.append(")");
        f.format("%f)", matrix[matrix.length - 1]);
        return transSB.toString();
    }

    public static double[] getReconstructBoundingBox(final double[] wh,
                                                     final AffineTransform trans) {
        double x = wh[0], y = wh[1];
        double[] xy = new double[]{0, 0, x, 0, x, y, 0, y};
        trans.transform(xy, 0, xy, 0, 4);
        return xy;
    }

    public static Element findElementByAttributeRegex(final NodeList list,
                                                      final String name, final String regex)
    {
        for (int i = 0; i < list.getLength(); ++i)
        {
            if (list.item(i).getNodeType() == Node.ELEMENT_NODE)
            {
                final Element e = (Element) list.item(i);

                if (e.hasAttribute(name))
                {
                    if(e.getAttribute(name).matches(regex))
                    {
                        return e;
                    }
                }
            }
        }

        return null;
    }

    public static Element findElementByAttribute(final NodeList list,
                                                 final String name, final String value)
    {
        for (int i = 0; i < list.getLength(); ++i)
        {
            if (list.item(i).getNodeType() == Node.ELEMENT_NODE)
            {
                final Element e = (Element) list.item(i);
                if (e.hasAttribute(name) && e.getAttribute(name).equals(value))
                {
                    return e;
                }
            }
        }
        return null;
    }

    public static double[] createNodeValueVector(final String val)
    {
        final StringTokenizer tokr = new StringTokenizer(val, DELIM + ",\"");
        return new double[tokr.countTokens()];
    }


    public static int nodeValueToVector(String val, final double[] matrix)
    {
        StringTokenizer t;
        int rCount = 0, i = 0;

        if (val.startsWith("\""))
        {
            val = val.substring(1);
        }
        if (val.endsWith("\""))
        {
            val = val.substring(0, val.length());
        }

        t = new StringTokenizer(val, DELIM);

        String tok = "";
        while (t.hasMoreElements() && !(tok = t.nextToken()).contains(","))
        {
            ++rCount;
        }

        if (tok.contains(","))
        {
            ++rCount;
        }

        t = new StringTokenizer(val, DELIM + ",\"");

        while (t.hasMoreElements())
        {
            matrix[i++] = Float.valueOf(t.nextToken());
        }
        return rCount;
    }


    /**
     * Finds the first Transform Element with an Image child node.
     * @param doc the section XML Document
     * @return the first Transform Element with an Image child node.
     */
    public static Element getFirstImageTransformElement(final Document doc)
    {
        final NodeList transforms = doc.getElementsByTagName("Transform");
        for (int i = 0; i < transforms.getLength(); ++i)
        {
            final Element transform = (Element)transforms.item(i);
            final NodeList children = transform.getChildNodes();
            for (int j = 0; j < children.getLength(); ++j)
            {
                if (children.item(j).getNodeName().equals("Image"))
                {

                    return transform;
                }
            }
        }
        return null;
    }

    /**
     * Finds all Transform Elements with Image children.
     * @param doc the section XML Document
     * @return an ArrayList containing all Transforms with Image children
     */
    public static ArrayList<Element> getImageTransformElements(final Document doc)
    {
        final ArrayList<Element> transformList = new ArrayList<Element>(3);
        final NodeList transforms = doc.getElementsByTagName("Transform");

        // Iterate through all transform elements
        for (int i = 0; i < transforms.getLength(); ++i)
        {
            final Element transform = (Element)transforms.item(i);
            final NodeList children = transform.getChildNodes();
            boolean found = false;

            // Iterate through all children of the transforms
            for (int j = 0; j < children.getLength() && !found; ++j)
            {
                // If a child is named "Image," add the transform to the list and break out of the
                // for loop
                if (children.item(j).getNodeName().equals("Image"))
                {
                    transformList.add(transform);
                    found = true;
                }
            }
        }

        return transformList;
    }

    public static double[] getReconstructImageWH(final Node image)
    {
        return getReconstructImageWH(image, null);
    }
    
    public static Element getImageDomainContour(final Node image)
    {
        NodeList imageContourList =
                ((Element)image.getParentNode()).getElementsByTagName("Contour");
        return (Element)imageContourList.item(0);
    }

    public static double[] getReconstructImageWH(final Node image,
                                                 double[] wh)
    {
        if (null == image)
        {
            return new double[]{Double.NaN, Double.NaN};
        }
        else
        {
            Element imageDomainContour = getImageDomainContour(image);
            String pointsString;
            double[] points;

            if (null == wh)
            {
                wh = new double[2];
            }

            if (null != imageDomainContour)
            {
                pointsString = imageDomainContour.getAttribute("points");
                points = Utils.createNodeValueVector(pointsString);
                Utils.nodeValueToVector(pointsString, points);

                wh[0] = points[2] + 1;
                wh[1] = points[5] + 1;
            }
            else
            {
                wh[0] = Double.NaN;
                wh[1] = Double.NaN;
            }
            return wh;
        }
    }

    public static <T extends ContourSet> T findContourByName(final List<T> contours,
                                    final String name)
    {
        if (name != null)
        {
            for (T t : contours)
            {
                if (t.getName().equals(name))
                {
                    return t;
                }
            }
        }
        return null;
    }

    public static void selectElementsByIndex(final List<Element> elementList,
                                             final List<Integer> indexList,
                                             final List<Element> outputList,
                                             final int index)
    {
        for (int i = 0; i < indexList.size(); ++i)
        {
            if (indexList.get(i).equals(index))
            {
                outputList.add(elementList.get(i));
            }
        }
    }

    public static void appendOpenPathXML(final StringBuilder sb, final double[] dpts)
    {
        float[] pts = doubleToFloat(dpts);
        sb.append("M ").append(pts[0]).append(" ").append(pts[1]).append(" ");

        for (int i = 2; i < pts.length ; i+=2)
        {
            sb.append("L ").append(pts[i]).append(" ").append(pts[i + 1]).append(" ");
        }
    }

    public static void appendClosedPathXML(final StringBuilder sb, final double[] pts)
    {
        appendOpenPathXML(sb, pts);
        sb.append("z");
    }

    public static void appendBezierPathXML(final StringBuilder sb, final double[] dpts)
    {
        float[] pts = doubleToFloat(dpts);
        if (pts.length > 0)
        {
            sb.append("M ").append(pts[0]).append(",").append(pts[1]).append(" ");

            for (int i = 2; i < pts.length ; i+=2)
            {
                sb.append("C ").append(pts[i-2]).append(",").append(pts[i - 1]).append(" ")
                        .append(pts[i]).append(",").append(pts[i + 1]).append(" ")
                        .append(pts[i]).append(",").append(pts[i + 1]);
            }
        }
    }

    public static String hexColor(String inColor)
    {
        String hex = "";
        double[] colorTriad = new double[3];
        nodeValueToVector(inColor, colorTriad);
        for (int i = 0; i < 3; ++i)
        {
            String simplexHex = Integer.toHexString((int)(colorTriad[i]) * 255);
            if (simplexHex.length() < 2)
            {
                simplexHex = "0" + simplexHex;
            }
            hex = hex + simplexHex;
        }
        return hex;
    }

    public static double getMag(final Document d)
    {
        NodeList nl = d.getElementsByTagName("Image");
        if (nl.getLength() == 0)
        {
            return Double.NaN;
        }
        else
        {
            return Double.valueOf(((Element) nl.item(0)).getAttribute("mag"));
        }
    }

    public static double getMag(final Node n)
    {
        Document d = n.getOwnerDocument();
        return getMag(d);
    }

    public static double[] getTransformedPoints(Element contour, double stackHeight, double mag)
    {
        //I admit that this code is really really ugly.

        //So-called "domain" contours are treated differently, wrt mag and flipping....
        boolean isDomainContour = contour.getAttribute("name").startsWith("domain");
        //Mag and zoom are different. Sigh.
        double zoom = isDomainContour ? 1.0 : 1.0 / mag;
        double useMag = isDomainContour ? mag : 1.0;
        //Create the affine transform to take care of the transform.
        AffineTransform trans = Utils.reconstructTransform(
                (Element) contour.getParentNode(),
                useMag, stackHeight, zoom, isDomainContour);
        //Now, we grab the points from the XML.
        final String contourValue = contour.getAttribute("points");
        if (contourValue.trim().length() < 1)
        {
            return new double[0];
        }
        else
        {
            double[] pts = Utils.createNodeValueVector(contour.getAttribute("points"));
            int nrows = Utils.nodeValueToVector(contour.getAttribute("points"), pts);

            //If we got a different number of rows than expected, yell about it, but don't die.
            if (nrows != 2)
            {
                System.err.println("Nrows should have been 2, instead it was " + nrows
                        + ", therefore, we're boned");
                System.err.println("Points text: " + contour.getAttribute("points"));
                System.err.println("Problem encountered while processing " + contour.getAttribute("name"));
            }

            //Apply the transform (I hope I hope I hope this worked out right).
            trans.transform(pts, 0, pts, 0, pts.length / 2);

            //Flip it vertically, as long as it isn't a domain contour.
            if (!isDomainContour)
            {
                for (int i = 1; i < pts.length; i+=2)
                {
                    pts[i] = stackHeight - pts[i];
                }
            }
            return pts;
        }
    }

    public static double[] getPathExtent(double[] pts)
    {
        //assume path is interleaved in 2D
        double[] wh = new double[2];
        wh[0] = 0;
        wh[1] = 0;
        for (int i = 0; i < pts.length; i+=2)
        {
            if (pts[i] > wh[0])
            {
                wh[0] = pts[i];
            }
            if (pts[i + 1] > wh[1])
            {
                wh[1] = pts[i + 1];
            }
        }
        return wh;
    }

    public static double getMedianMag(final List<Document> sectionList)
    {
        final ArrayList<Double> magList = new ArrayList<Double>(sectionList.size());

        for (final Document d : sectionList)
        {
            double m = getMag(d);
            if (!Double.isNaN(m))
            {
                magList.add(m);
            }
        }

        Collections.sort(magList);

        return magList.get(magList.size() / 2);
    }

    public static String stackTraceToString(final Throwable t)
    {
        //adapted from code found on javapractices.com
        final StringWriter result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        t.printStackTrace(printWriter);
        return result.toString();

    }
    
    // Temporary kluge function
    public static float[] doubleToFloat(final double[] doubles)
    {
        float[] floats = new float[doubles.length];
        for (int i = 0; i < doubles.length; ++i)
        {
            floats[i] = (float)doubles[i];
        }
        return floats;
    }
    
    public static ArrayList<String> getSeriesKeys()
    {
        ArrayList<String> keys = new ArrayList<String>(128);

        keys.add("viewport");
        keys.add("units");
        keys.add("autoSaveSeries");
        keys.add("autoSaveSection");
        keys.add("warnSaveSection");
        keys.add("beepDeleting");
        keys.add("beepPaging");
        keys.add("hideTraces");
        keys.add("unhideTraces");
        keys.add("hideDomains");
        keys.add("unhideDomains");
        keys.add("useAbsolutePaths");
        keys.add("defaultThickness");
        keys.add("zMidSection");
        keys.add("thumbWidth");
        keys.add("thumbHeight");
        keys.add("fitThumbSections");
        keys.add("firstThumbSection");
        keys.add("lastThumbSection");
        keys.add("skipSections");
        keys.add("displayThumbContours");
        keys.add("useFlipbookStyle");
        keys.add("flipRate");
        keys.add("useProxies");
        keys.add("widthUseProxies");
        keys.add("heightUseProxies");
        keys.add("scaleProxies");
        keys.add("significantDigits");
        keys.add("defaultBorder");
        keys.add("defaultFill");
        keys.add("defaultMode");
        keys.add("defaultName");
        keys.add("defaultComment");
        keys.add("listSectionThickness");
        keys.add("listDomainSource");
        keys.add("listDomainPixelsize");
        keys.add("listDomainLength");
        keys.add("listDomainArea");
        keys.add("listDomainMidpoint");
        keys.add("listTraceComment");
        keys.add("listTraceLength");
        keys.add("listTraceArea");
        keys.add("listTraceCentroid");
        keys.add("listTraceExtent");
        keys.add("listTraceZ");
        keys.add("listTraceThickness");
        keys.add("listObjectRange");
        keys.add("listObjectCount");
        keys.add("listObjectSurfarea");
        keys.add("listObjectFlatarea");
        keys.add("listObjectVolume");
        keys.add("listZTraceNote");
        keys.add("listZTraceRange");
        keys.add("listZTraceLength");
        keys.add("borderColors");
        keys.add("fillColors");
        keys.add("offset3D");
        keys.add("type3Dobject");
        keys.add("first3Dsection");
        keys.add("last3Dsection");
        keys.add("max3Dconnection");
        keys.add("upper3Dfaces");
        keys.add("lower3Dfaces");
        keys.add("faceNormals");
        keys.add("vertexNormals");
        keys.add("facets3D");
        keys.add("dim3D");
        keys.add("gridType");
        keys.add("gridSize");
        keys.add("gridDistance");
        keys.add("gridNumber");
        keys.add("hueStopWhen");
        keys.add("hueStopValue");
        keys.add("satStopWhen");
        keys.add("satStopValue");
        keys.add("brightStopWhen");
        keys.add("brightStopValue");
        keys.add("tracesStopWhen");
        keys.add("areaStopPercent");
        keys.add("areaStopSize");
        keys.add("ContourMaskWidth");
        keys.add("smoothingLength");
        keys.add("mvmtIncrement");
        keys.add("ctrlIncrement");
        keys.add("shiftIncrement");

        return keys;
    }
}

