package edu.utexas.clm.reconstructreader.reconstruct;

import edu.utexas.clm.reconstructreader.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;

public class ReconstructSection {

    private final int index, oid;
    private final Document doc;
    private final ReconstructTranslator translator;
    private final ArrayList<ReconstructProfile> profiles;
    private final double mag;
    private double z;
    private double thickness;

    public ReconstructSection(final ReconstructTranslator t, final Document d)
    {
        double m;
        translator = t;
        //index = t.nextOID();
        index = Integer.valueOf(d.getDocumentElement().getAttribute("index"));
        oid = t.nextOID();
        doc = d;
        profiles = new ArrayList<ReconstructProfile>();

        m = Utils.getMag(d);
        mag = Double.isNaN(m) ? t.getMag() : m;
        z = -1;
        thickness = -1;
    }

    public int getOID()
    {
        return oid;
    }

    public int getIndex()
    {
        return index;
    }

    public Document getDocument()
    {
        return doc;
    }

    public double getMag()
    {
        return mag;
    }

    public void addProfile(final ReconstructProfile rp)
    {
        profiles.add(rp);
    }
    
    public void setZ(double inZ)
    {
        z = inZ;
    }
    
    public double getThickness()
    {
        if (thickness < 0)
        {
            String thickStr = doc.getDocumentElement().getAttribute("thickness");
            thickness = Double.valueOf(thickStr);
        }

        return thickness;
    }
    
    public double getPixelThickness()
    {
        return thickness / getMag();
    }

    public void setThickness(double inThickness)
    {
        thickness = inThickness;
    }
    
    public void setZFromPrevious(ReconstructSection prev)
    {
        if (prev.getIndex() > getIndex())
        {
            translator.log("Whoa! Sections not sorted!");
        }
        setZ(prev.getZ() + getThickness() * (this.getIndex() - prev.getIndex()));
    }
    
    public double getZ()
    {
        return z;
    }

    public double getHeight()
    {
        NodeList imageList = getDocument().getElementsByTagName("Image");
        double wh[] = Utils.getReconstructImageWH(imageList.item(0));
        return Double.isNaN(wh[1]) ? translator.getStackHeight() : wh[1];
    }

    public void appendXML(final StringBuilder sb)
    {
        //double mag = translator.getMag();
        //int index = Integer.valueOf(doc.getDocumentElement().getAttribute("index"));
        //double aniso = th / mag;
        NodeList imageList = doc.getElementsByTagName("Image");

        sb.append("<t2_layer oid=\"")
                .append(oid).append("\"\n" +
                "thickness=\"").append(getPixelThickness()).append("\"\n" +
                "z=\"").append(getZ()).append("\"\n" +
                "title=\"\"\n" +
                ">\n");

        for (int i = 0; i < imageList.getLength(); ++i)
        {
            appendPatch(sb, (Element)imageList.item(i));
        }

        for (ReconstructProfile profile : profiles)
        {
            profile.appendXML(sb);
        }

        sb.append("</t2_layer>\n");
    }


    protected void appendPatch(final StringBuilder sb, final Element image)
    {
        Element rTransform = (Element)image.getParentNode();
        double[] wh = Utils.getReconstructImageWH(image);
        double h = Double.isNaN(wh[1]) ? translator.getStackHeight() : wh[1];
        double w = Double.isNaN(wh[0]) ? translator.getStackWidth() : wh[0];
        AffineTransform trans;
        String src = image.getAttribute("src");
        String transString;

        trans = Utils.reconstructTransform(rTransform,
                Double.valueOf(image.getAttribute("mag")), h);
        transString = Utils.transformToString(trans);

        sb.append("<t2_patch\n" +
                "oid=\"").append(translator.nextOID()).append("\"\n" +
                "width=\"").append(w).append("\"\n" +
                "height=\"").append(h).append("\"\n" +
                "transform=\"").append(transString).append("\"\n" +
                "title=\"").append(src).append("\"\n" +
                "links=\"\"\n" +
                "type=\"0\"\n" +
                "file_path=\"").append(src).append("\"\n" +
                "style=\"fill-opacity:1.0;stroke:#ffff00;\"\n" +
                "o_width=\"").append((int)w).append("\"\n" +
                "o_height=\"").append((int)h).append("\"\n" +
                "min=\"0.0\"\n" +
                "max=\"255.0\"\n" +
                ">\n" +
                "</t2_patch>\n");
    }

}
