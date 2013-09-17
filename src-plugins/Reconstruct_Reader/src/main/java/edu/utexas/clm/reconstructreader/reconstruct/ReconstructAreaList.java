package edu.utexas.clm.reconstructreader.reconstruct;

import edu.utexas.clm.reconstructreader.Utils;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;


public class ReconstructAreaList implements ContourSet {

    private final String name;
    private final int oid;
    private final int recContourID, areaListID;
    private final ArrayList<Element> contourList;
    private final ArrayList<Integer> indexList;
    private final ReconstructTranslator translator;

    public ReconstructAreaList(final Element e, final ReconstructTranslator t, final ReconstructSection rs)
    {
        translator = t;
        name = e.getAttribute("name");
        oid = translator.nextOID();
        recContourID = translator.nextOID();
        areaListID = translator.nextOID();
        contourList = new ArrayList<Element>();
        indexList = new ArrayList<Integer>();
        contourList.add(e);
        indexList.add(rs.getIndex());
    }

    public String getName()
    {
        return name;
    }

    public boolean equals(final Object o)
    {
        if (o instanceof ReconstructAreaList)
        {
            ReconstructAreaList ral = (ReconstructAreaList)o;
            return name.equals(ral.name);
        }
        else if (o instanceof Element)
        {
            Element e = (Element)o;
            return name.equals(e.getAttribute("name"));
        }
        else
        {
            return false;
        }
    }

    public void addContour(final Element e, ReconstructSection sec)
    {
        contourList.add(e);
        indexList.add(sec.getIndex());
    }

    public void appendProjectXML(final StringBuilder sb)
    {
        sb.append("<reconstruct_contour id=\"").append(recContourID).append("\" title=\"")
                .append(name).append("\" expanded=\"true\">\n" +
                "<area_list id=\"").append(areaListID).append("\" oid=\"").append(oid)
                .append("\"/>\n</reconstruct_contour>\n");
    }

    public void appendLayerSetXML(final StringBuilder sb, final List<ReconstructSection> sectionList)
    {
        final ArrayList<Element> selectionList = new ArrayList<Element>();
        String fillColorHex = Utils.hexColor(contourList.get(0).getAttribute("fill"));
        String strokeColorHex = Utils.hexColor(contourList.get(0).getAttribute("border"));
        boolean visible = contourList.get(0).getAttribute("hidden").equals("false");

        sb.append("<t2_area_list\n" +
                "oid=\"").append(oid).append("\"\n" +
                "width=\"").append(translator.getStackWidth()).append("\"\n" +
                "height=\"").append(translator.getStackHeight()).append("\"\n" +
                "transform=\"matrix(1.0,0.0,0.0,1.0,0,0)\"\n" +
                "visible=\"").append(visible).append("\"\n" +
                "title=\"area_list\"\n" +
                "links=\"\"\n" +
                "layer_set_id=\"0\"\n" +
                "fill_paint=\"true\"\n" +
                "style=\"stroke:#").append(strokeColorHex).append(";fill-opacity:0.4;fill:#")
                .append(fillColorHex).append(";\"\n" +
                ">\n");

        for (ReconstructSection sec : sectionList)
        {
            int index = sec.getIndex();
            int layerOID = sec.getOID();
            double h = sec.getHeight();

            Utils.selectElementsByIndex(contourList, indexList, selectionList, index);

            sb.append("<t2_area layer_id=\"").append(layerOID).append("\">\n");

            for (Element contour : selectionList)
            {
                double[] pts = Utils.getTransformedPoints(contour, h, sec.getMag());
                sb.append("<t2_path d=\"");
                Utils.appendClosedPathXML(sb, pts);
                sb.append("\" />\n");
            }

            sb.append("</t2_area>\n");

            selectionList.clear();
        }
        sb.append("</t2_area_list>\n");
    }
}
