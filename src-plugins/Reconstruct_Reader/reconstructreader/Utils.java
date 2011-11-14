package reconstructreader;

import org.w3c.dom.*;
import reconstructreader.reconstruct.ContourSet;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
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
                if (weird)
                {
                    System.err.println("Non affine tranforms are unsupported." +
                            " Expect weirdness at index " + index);
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

    public static int nodeValueToVector(String val,
                                        final double[] matrix)
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

    public static double[] getReconstructImageWH(final Node image)
    {
        return getReconstructImageWH(image, null);
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
            NodeList imageContourList =
                    ((Element)image.getParentNode()).getElementsByTagName("Contour");
            Element imageDomainContour =
                    Utils.findElementByAttributeRegex(imageContourList, "name", "^domain.*");
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

    public static void appendOpenPathXML(final StringBuilder sb, final double[] pts)
    {
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

    public static void appendBezierPathXML(final StringBuilder sb, final double[] pts)
    {
        sb.append("M ").append(pts[0]).append(",").append(pts[1]).append(" ");

        for (int i = 2; i < pts.length ; i+=2)
        {
            sb.append("C ").append(pts[i-2]).append(",").append(pts[i - 1]).append(" ")
                .append(pts[i]).append(",").append(pts[i + 1]).append(" ")
                .append(pts[i]).append(",").append(pts[i + 1]);
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
        double[] pts = Utils.createNodeValueVector(contour.getAttribute("points"));
        int nrows = Utils.nodeValueToVector(contour.getAttribute("points"), pts);

        //If we got a different number of rows than expected, yell about it, but don't die.
        if (nrows != 2)
        {
            System.err.println("Nrows should have been 2, instead it was " + nrows
                    + ", therefore, we're boned");
            System.err.println("Points text: " + contour.getAttribute("points"));
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
}

