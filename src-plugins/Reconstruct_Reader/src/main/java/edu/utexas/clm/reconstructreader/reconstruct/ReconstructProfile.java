package edu.utexas.clm.reconstructreader.reconstruct;

import edu.utexas.clm.reconstructreader.Utils;
import org.w3c.dom.Element;

public class ReconstructProfile {
    private final Element profile;
    private final int id, oid;
    private final ReconstructTranslator translator;
    private final double mag;
    private final ReconstructSection section;

    public ReconstructProfile(final Element e, final ReconstructTranslator t, ReconstructSection sec)
    {
        double m;

        translator = t;
        profile = e;
        section = sec;
        id = translator.nextOID();
        oid = translator.nextOID();

        m = Utils.getMag(e);
        mag = Double.isNaN(m) ? t.getMag() : m;
    }

    public int getOID()
    {
        return oid;
    }

    public int getID()
    {
        return id;
    }

    public void appendXML(final StringBuilder sb)
    {
        String colorHex = Utils.hexColor(profile.getAttribute("fill"));
        double[] pts = Utils.getTransformedPoints(profile, section.getHeight(), mag);
        double[] wh = Utils.getPathExtent(pts);
        double width = wh[0];
        double height = wh[1];

        sb.append("<t2_profile\n" +
                "oid=\"").append(getOID()).append("\"\n" +
                "width=\"").append(width).append("\"\n" +
                "height=\"").append(height).append("\"\n" +
                "transform=\"matrix(1.0,0.0,0.0,1.0,0,0)\"\n" +
                "title=\"").append(profile.getAttribute("name")).append("\"\n" +
                "links=\"\"\n" +
                "style=\"fill:none;stroke-opacity:1.0;stroke:#").append(colorHex)
                .append(";stroke-width:1.0px;\"\n");

        sb.append("d=\"");
        Utils.appendBezierPathXML(sb, pts);
        sb.append("\"\n>\n" +
            "</t2_profile>\n");
    }

}
