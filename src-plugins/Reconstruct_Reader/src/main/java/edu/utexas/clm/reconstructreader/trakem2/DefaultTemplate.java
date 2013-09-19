package edu.utexas.clm.reconstructreader.trakem2;

import java.util.HashMap;

public class DefaultTemplate implements SeriesTemplate
{
    private static final DefaultTemplate template = new DefaultTemplate(); 
    
    public static DefaultTemplate getTemplate()
    {
        return template;
    }
    
    private final HashMap<String, String> dictionary;

    public DefaultTemplate()
    {
        dictionary = new HashMap<String, String>();
        dictionary.put("viewport","0 0 0.00254");
        dictionary.put("units","microns");
        dictionary.put("autoSaveSeries","true");
        dictionary.put("autoSaveSection","true");
        dictionary.put("warnSaveSection","true");
        dictionary.put("beepDeleting","true");
        dictionary.put("beepPaging","true");
        dictionary.put("hideTraces","false");
        dictionary.put("unhideTraces","false");
        dictionary.put("hideDomains","false");
        dictionary.put("unhideDomains","false");
        dictionary.put("useAbsolutePaths","false");
        dictionary.put("defaultThickness","0.05");
        dictionary.put("zMidSection","false");
        dictionary.put("thumbWidth","128");
        dictionary.put("thumbHeight","96");
        dictionary.put("fitThumbSections","false");
        dictionary.put("firstThumbSection","1");
        dictionary.put("lastThumbSection","2147483647");
        dictionary.put("skipSections","1");
        dictionary.put("displayThumbContours","true");
        dictionary.put("useFlipbookStyle","false");
        dictionary.put("flipRate","5");
        dictionary.put("useProxies","true");
        dictionary.put("widthUseProxies","2048");
        dictionary.put("heightUseProxies","1536");
        dictionary.put("scaleProxies","0.25");
        dictionary.put("significantDigits","6");
        dictionary.put("defaultBorder","1.000 0.000 1.000");
        dictionary.put("defaultFill","1.000 0.000 1.000");
        dictionary.put("defaultMode","9");
        dictionary.put("defaultName","domain$+");
        dictionary.put("defaultComment","");
        dictionary.put("listSectionThickness","true");
        dictionary.put("listDomainSource","true");
        dictionary.put("listDomainPixelsize","true");
        dictionary.put("listDomainLength","false");
        dictionary.put("listDomainArea","false");
        dictionary.put("listDomainMidpoint","false");
        dictionary.put("listTraceComment","true");
        dictionary.put("listTraceLength","false");
        dictionary.put("listTraceArea","true");
        dictionary.put("listTraceCentroid","false");
        dictionary.put("listTraceExtent","false");
        dictionary.put("listTraceZ","false");
        dictionary.put("listTraceThickness","false");
        dictionary.put("listObjectRange","true");
        dictionary.put("listObjectCount","true");
        dictionary.put("listObjectSurfarea","false");
        dictionary.put("listObjectFlatarea","false");
        dictionary.put("listObjectVolume","false");
        dictionary.put("listZTraceNote","true");
        dictionary.put("listZTraceRange","true");
        dictionary.put("listZTraceLength","true");
    }
    
    public String getValue(final String key) {
        return dictionary.get(key);
    }

    public void setContourText(StringBuilder sb)
    {
        sb.append("<Contour name=\"a$+\" closed=\"true\" border=\"1.000 0.500 0.000\" " +
                "fill=\"1.000 0.500 0.000\" mode=\"13\"\n" +
                " points=\"-3 1,\n" +
                "\t-3 -1,\n" +
                "\t-1 -3,\n" +
                "\t1 -3,\n" +
                "\t3 -1,\n" +
                "\t3 1,\n" +
                "\t1 3,\n" +
                "\t-1 3,\n" +
                "\t\"/>\n" +
                "<Contour name=\"b$+\" closed=\"true\" border=\"0.500 0.000 1.000\" " +
                "fill=\"0.500 0.000 1.000\" mode=\"13\"\n" +
                " points=\"-2 1,\n" +
                "\t-5 0,\n" +
                "\t-2 -1,\n" +
                "\t-4 -4,\n" +
                "\t-1 -2,\n" +
                "\t0 -5,\n" +
                "\t1 -2,\n" +
                "\t4 -4,\n" +
                "\t2 -1,\n" +
                "\t5 0,\n" +
                "\t2 1,\n" +
                "\t4 4,\n" +
                "\t1 2,\n" +
                "\t0 5,\n" +
                "\t-1 2,\n" +
                "\t-4 4,\n" +
                "\t\"/>\n" +
                "<Contour name=\"pink$+\" closed=\"true\" border=\"1.000 0.000 0.500\" " +
                "fill=\"1.000 0.000 0.500\" mode=\"-13\"\n" +
                " points=\"-6 -6,\n" +
                "\t6 -6,\n" +
                "\t0 5,\n" +
                "\t\"/>\n" +
                "<Contour name=\"X$+\" closed=\"true\" border=\"1.000 0.000 0.000\" " +
                "fill=\"1.000 0.000 0.000\" mode=\"-13\"\n" +
                " points=\"-7 7,\n" +
                "\t-2 0,\n" +
                "\t-7 -7,\n" +
                "\t-4 -7,\n" +
                "\t0 -1,\n" +
                "\t4 -7,\n" +
                "\t7 -7,\n" +
                "\t2 0,\n" +
                "\t7 7,\n" +
                "\t4 7,\n" +
                "\t0 1,\n" +
                "\t-4 7,\n" +
                "\t\"/>\n" +
                "<Contour name=\"yellow$+\" closed=\"true\" border=\"1.000 1.000 0.000\" " +
                "fill=\"1.000 1.000 0.000\" mode=\"-13\"\n" +
                " points=\"8 8,\n" +
                "\t8 -8,\n" +
                "\t-8 -8,\n" +
                "\t-8 6,\n" +
                "\t-10 8,\n" +
                "\t-10 -10,\n" +
                "\t10 -10,\n" +
                "\t10 10,\n" +
                "\t-10 10,\n" +
                "\t-8 8,\n" +
                "\t\"/>\n" +
                "<Contour name=\"blue$+\" closed=\"true\" border=\"0.000 0.000 1.000\" " +
                "fill=\"0.000 0.000 1.000\" mode=\"9\"\n" +
                " points=\"0 7,\n" +
                "\t-7 0,\n" +
                "\t0 -7,\n" +
                "\t7 0,\n" +
                "\t\"/>\n" +
                "<Contour name=\"magenta$+\" closed=\"true\" border=\"1.000 0.000 1.000\" " +
                "fill=\"1.000 0.000 1.000\" mode=\"9\"\n" +
                " points=\"-6 2,\n" +
                "\t-6 -2,\n" +
                "\t-2 -6,\n" +
                "\t2 -6,\n" +
                "\t6 -2,\n" +
                "\t6 2,\n" +
                "\t2 6,\n" +
                "\t-2 6,\n" +
                "\t\"/>\n" +
                "<Contour name=\"red$+\" closed=\"true\" border=\"1.000 0.000 0.000\" " +
                "fill=\"1.000 0.000 0.000\" mode=\"9\"\n" +
                " points=\"6 -6,\n" +
                "\t0 -6,\n" +
                "\t0 -3,\n" +
                "\t3 0,\n" +
                "\t12 3,\n" +
                "\t6 6,\n" +
                "\t3 12,\n" +
                "\t-3 6,\n" +
                "\t-6 0,\n" +
                "\t-6 -6,\n" +
                "\t-12 -6,\n" +
                "\t-3 -12,\n" +
                "\t\"/>\n" +
                "<Contour name=\"green$+\" closed=\"true\" border=\"0.000 1.000 0.000\" " +
                "fill=\"0.000 1.000 0.000\" mode=\"9\"\n" +
                " points=\"-12 4,\n" +
                "\t-12 -4,\n" +
                "\t-4 -4,\n" +
                "\t-4 -12,\n" +
                "\t4 -12,\n" +
                "\t4 -4,\n" +
                "\t12 -4,\n" +
                "\t12 4,\n" +
                "\t4 4,\n" +
                "\t4 12,\n" +
                "\t-4 12,\n" +
                "\t-4 4,\n" +
                "\t\"/>\n" +
                "<Contour name=\"cyan$+\" closed=\"true\" border=\"0.000 1.000 1.000\" " +
                "fill=\"0.000 1.000 1.000\" mode=\"9\"\n" +
                " points=\"0 12,\n" +
                "\t4 8,\n" +
                "\t-12 -8,\n" +
                "\t-8 -12,\n" +
                "\t8 4,\n" +
                "\t12 0,\n" +
                "\t12 12,\n" +
                "\t\"/>\n" +
                "<Contour name=\"a$+\" closed=\"true\" border=\"1.000 0.500 0.000\" " +
                "fill=\"1.000 0.500 0.000\" mode=\"13\"\n" +
                " points=\"-3 1,\n" +
                "\t-3 -1,\n" +
                "\t-1 -3,\n" +
                "\t1 -3,\n" +
                "\t3 -1,\n" +
                "\t3 1,\n" +
                "\t1 3,\n" +
                "\t-1 3,\n" +
                "\t\"/>\n" +
                "<Contour name=\"b$+\" closed=\"true\" border=\"0.500 0.000 1.000\" " +
                "fill=\"0.500 0.000 1.000\" mode=\"13\"\n" +
                " points=\"-2 1,\n" +
                "\t-5 0,\n" +
                "\t-2 -1,\n" +
                "\t-4 -4,\n" +
                "\t-1 -2,\n" +
                "\t0 -5,\n" +
                "\t1 -2,\n" +
                "\t4 -4,\n" +
                "\t2 -1,\n" +
                "\t5 0,\n" +
                "\t2 1,\n" +
                "\t4 4,\n" +
                "\t1 2,\n" +
                "\t0 5,\n" +
                "\t-1 2,\n" +
                "\t-4 4,\n" +
                "\t\"/>\n" +
                "<Contour name=\"pink$+\" closed=\"true\" border=\"1.000 0.000 0.500\" " +
                "fill=\"1.000 0.000 0.500\" mode=\"-13\"\n" +
                " points=\"-6 -6,\n" +
                "\t6 -6,\n" +
                "\t0 5,\n" +
                "\t\"/>\n" +
                "<Contour name=\"X$+\" closed=\"true\" border=\"1.000 0.000 0.000\" " +
                "fill=\"1.000 0.000 0.000\" mode=\"-13\"\n" +
                " points=\"-7 7,\n" +
                "\t-2 0,\n" +
                "\t-7 -7,\n" +
                "\t-4 -7,\n" +
                "\t0 -1,\n" +
                "\t4 -7,\n" +
                "\t7 -7,\n" +
                "\t2 0,\n" +
                "\t7 7,\n" +
                "\t4 7,\n" +
                "\t0 1,\n" +
                "\t-4 7,\n" +
                "\t\"/>\n" +
                "<Contour name=\"yellow$+\" closed=\"true\" border=\"1.000 1.000 0.000\" " +
                "fill=\"1.000 1.000 0.000\" mode=\"-13\"\n" +
                " points=\"8 8,\n" +
                "\t8 -8,\n" +
                "\t-8 -8,\n" +
                "\t-8 6,\n" +
                "\t-10 8,\n" +
                "\t-10 -10,\n" +
                "\t10 -10,\n" +
                "\t10 10,\n" +
                "\t-10 10,\n" +
                "\t-8 8,\n" +
                "\t\"/>\n" +
                "<Contour name=\"blue$+\" closed=\"true\" border=\"0.000 0.000 1.000\" " +
                "fill=\"0.000 0.000 1.000\" mode=\"9\"\n" +
                " points=\"0 7,\n" +
                "\t-7 0,\n" +
                "\t0 -7,\n" +
                "\t7 0,\n" +
                "\t\"/>\n" +
                "<Contour name=\"magenta$+\" closed=\"true\" border=\"1.000 0.000 1.000\" " +
                "fill=\"1.000 0.000 1.000\" mode=\"9\"\n" +
                " points=\"-6 2,\n" +
                "\t-6 -2,\n" +
                "\t-2 -6,\n" +
                "\t2 -6,\n" +
                "\t6 -2,\n" +
                "\t6 2,\n" +
                "\t2 6,\n" +
                "\t-2 6,\n" +
                "\t\"/>\n" +
                "<Contour name=\"red$+\" closed=\"true\" border=\"1.000 0.000 0.000\" " +
                "fill=\"1.000 0.000 0.000\" mode=\"9\"\n" +
                " points=\"6 -6,\n" +
                "\t0 -6,\n" +
                "\t0 -3,\n" +
                "\t3 0,\n" +
                "\t12 3,\n" +
                "\t6 6,\n" +
                "\t3 12,\n" +
                "\t-3 6,\n" +
                "\t-6 0,\n" +
                "\t-6 -6,\n" +
                "\t-12 -6,\n" +
                "\t-3 -12,\n" +
                "\t\"/>\n" +
                "<Contour name=\"green$+\" closed=\"true\" border=\"0.000 1.000 0.000\" " +
                "fill=\"0.000 1.000 0.000\" mode=\"9\"\n" +
                " points=\"-12 4,\n" +
                "\t-12 -4,\n" +
                "\t-4 -4,\n" +
                "\t-4 -12,\n" +
                "\t4 -12,\n" +
                "\t4 -4,\n" +
                "\t12 -4,\n" +
                "\t12 4,\n" +
                "\t4 4,\n" +
                "\t4 12,\n" +
                "\t-4 12,\n" +
                "\t-4 4,\n" +
                "\t\"/>\n" +
                "<Contour name=\"cyan$+\" closed=\"true\" border=\"0.000 1.000 1.000\" " +
                "fill=\"0.000 1.000 1.000\" mode=\"9\"\n" +
                " points=\"0 12,\n" +
                "\t4 8,\n" +
                "\t-12 -8,\n" +
                "\t-8 -12,\n" +
                "\t8 4,\n" +
                "\t12 0,\n" +
                "\t12 12,\n" +
                "\t\"/>\n");
    }
}
