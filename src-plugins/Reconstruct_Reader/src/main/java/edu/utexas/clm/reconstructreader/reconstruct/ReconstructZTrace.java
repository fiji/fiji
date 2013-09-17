package edu.utexas.clm.reconstructreader.reconstruct;

import edu.utexas.clm.reconstructreader.Utils;
import org.w3c.dom.Element;

import java.util.ArrayList;

public class ReconstructZTrace implements ContourSet
{
    private final int oid;
    private final String name;
    private final ArrayList<Element> polyLineList;
    private final ArrayList<Integer> polyLineOIDList;
    private final ArrayList<Integer> polyLineIDList;
    private final ReconstructTranslator translator;

    public ReconstructZTrace(final Element e, final ReconstructTranslator t)
    {
        translator = t;
        oid = translator.nextOID();

        name = e.getAttribute("name");
        polyLineList = new ArrayList<Element>();
        polyLineOIDList = new ArrayList<Integer>();
        polyLineIDList = new ArrayList<Integer>();

        addContour(e, null);
    }

    public void appendProjectXML(final StringBuilder sb)
    {
        sb.append("<reconstruct_ztrace id=\"").append(oid).append("\" title=\"")
                .append(name).append("\" expanded=\"true\">\n");

        for (int i = 0; i < polyLineList.size(); ++i)
        {
            sb.append("<polyline id=\"").append(polyLineIDList.get(i)).append("\" oid=\"")
                    .append(polyLineOIDList.get(i)).append("\"/>\n");
        }

        sb.append("</reconstruct_ztrace>\n");
    }

    public void appendXML(final StringBuilder sb)
    {
        for (int i = 0; i < polyLineList.size(); ++i)
        {
            Element e = polyLineList.get(i);
            double[] pts3D = Utils.createNodeValueVector(e.getAttribute("points"));
            double[] pts2D, wh;
            double mag = translator.getMag();
            int[] layerID;
            int n = Utils.nodeValueToVector(e.getAttribute("points"), pts3D);
            String hexColor = Utils.hexColor(e.getAttribute("border"));


            if (n != 3)
            {
                translator.log("While processing Z-Traces, expected n = 3, but n = " + n);
            }

            layerID = new int[pts3D.length / n];
            pts2D = new double[layerID.length * 2];

            for (int j = 0; j < layerID.length; ++j)
            {
                pts2D[j*2] = pts3D[j*n];
                pts2D[j*2 + 1] = pts3D[j*n + 1];
                layerID[j] = (int)pts3D[j*n + 2];
            }

            for (int j = 0; j < pts2D.length; ++j)
            {
                pts2D[j] /= mag;
            }

            wh = Utils.getPathExtent(pts2D);

            sb.append("<t2_polyline\n" +
                    "oid=\"").append(polyLineOIDList.get(i)).append("\"\n" +
                    "width=\"").append(wh[0]).append("\"\n" +
                    "height=\"").append(wh[1]).append("\"\n" +
                    "transform=\"matrix(1.0,0.0,0.0,1.0,0.0,0.0)\"\n" +
                    "title=\"").append(name).append("\"\n" +
                    "links=\"\"\n" +
                    "layer_set_id=\"").append(translator.getLayerSetOID()).append("\"\n" +
                    "style=\"fill:none;stroke-opacity:1.0;stroke:#").append(hexColor)
                    .append(";stroke-width:1.0px;stroke-opacity:1.0\"\n" +
                    "d=\"");

            Utils.appendOpenPathXML(sb, pts2D);

            sb.append("\"\n" +
                    "layer_ids=\"");

            for (int id : layerID)
            {
                sb.append(translator.layerIndexToOID(id)).append(",");
            }

            sb.append("\"\n>\n</t2_polyline>\n");
        }
    }

    public void addContour(final Element e, final ReconstructSection sec) {
        polyLineList.add(e);
        polyLineOIDList.add(translator.nextOID());
        polyLineIDList.add(translator.nextOID());
    }

    public String getName() {
        return name;
    }
}
