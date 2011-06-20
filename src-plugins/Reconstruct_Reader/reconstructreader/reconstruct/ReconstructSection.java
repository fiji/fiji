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

    public ReconstructSection(Translator t, final Document d)
    {
        translator = t;
        //index = t.nextOID();
        index = Integer.valueOf(d.getDocumentElement().getAttribute("index"));
        oid = t.nextOID();
        doc = d;
        profiles = new ArrayList<ReconstructProfile>();
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

    public void addProfile(ReconstructProfile rp)
    {
        profiles.add(rp);
    }

    public void appendXML(StringBuilder sb)
    {
        String thickness = doc.getDocumentElement().getAttribute("thickness");
        int index = Integer.valueOf(doc.getDocumentElement().getAttribute("index"));
        float thicknessFloat = Float.valueOf(thickness);
        NodeList imageList = doc.getElementsByTagName("Image");

        sb.append("<t2_layer oid=\"")
                .append(oid).append("\"\n" +
                "thickness=\"").append(thickness).append("\"\n" +
                "z=\"").append(thicknessFloat * (float)index).append("\"\n" +
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


    protected void appendPatch(final StringBuilder sb,final Element image)
    {
        Element rTransform = (Element)image.getParentNode();

        AffineTransform trans = Utils.reconstructTransform(rTransform,
                Double.valueOf(image.getAttribute("mag")), translator.getStackHeight());
        String src = image.getAttribute("src");
        String transString = Utils.transformToString(trans);
        double[] wh = Utils.getReconstructImageWH(image);

        sb.append("<t2_patch\n" +
                "oid=\"").append(translator.nextOID()).append("\"\n" +
                "width=\"").append(wh[0]).append("\"\n" +
                "height=\"").append(wh[1]).append("\"\n" +
                "transform=\"").append(transString).append("\"\n" +
                "title=\"").append(src).append("\"\n" +
                "links=\"\"\n" +
                "type=\"0\"\n" +
                "file_path=\"").append(src).append("\"\n" +
                "style=\"fill-opacity:1.0;stroke:#ffff00;\"\n" +
                "o_width=\"").append((int)wh[0]).append("\"\n" +
                "o_height=\"").append((int)wh[1]).append("\"\n" +
                "min=\"0.0\"\n" +
                "max=\"255.0\"\n" +
                ">\n" +
                "</t2_patch>\n");
    }

}
