package reconstructreader.reconstruct;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import reconstructreader.Utils;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;

public class ReconstructSection {

    private final int index, oid;
    private final Document doc;
    private final Translator translator;
    private final ArrayList<ReconstructProfile> profiles;
    private final double mag;

    public ReconstructSection(final Translator t, final Document d)
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

    public void appendXML(final StringBuilder sb)
    {
        String thickness = doc.getDocumentElement().getAttribute("thickness");
        double mag = translator.getMag();
        int index = Integer.valueOf(doc.getDocumentElement().getAttribute("index"));
        double aniso = Double.valueOf(thickness) / mag;
        NodeList imageList = doc.getElementsByTagName("Image");

        sb.append("<t2_layer oid=\"")
                .append(oid).append("\"\n" +
                "thickness=\"").append(thickness).append("\"\n" +
                "z=\"").append(aniso * (double)index).append("\"\n" +
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
