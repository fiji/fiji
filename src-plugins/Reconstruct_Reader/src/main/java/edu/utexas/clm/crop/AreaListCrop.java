package edu.utexas.clm.crop;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ShapeRoi;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ini.trakem2.Project;
import ini.trakem2.display.AreaList;
import ini.trakem2.display.Layer;
import ini.trakem2.display.LayerSet;
import ini.trakem2.display.Patch;
import mpicbg.models.IllDefinedDataPointsException;


import java.awt.Rectangle;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class AreaListCrop
{

/*
    private static final Comparator<Layer> layerComparator = new Comparator<Layer>()
    {

        public int compare(Layer o1, Layer o2)
        {
            return Double.compare(o1.getZ(), o2.getZ());
        }
    };
*/

    private final ArrayList<AreaList> areaLists;

    public AreaListCrop()
    {
        areaLists = new ArrayList<AreaList>();
    }


    public void addAreaList(final AreaList areaList)
    {
        if (!areaLists.contains(areaList))
        {
            areaLists.add(areaList);
        }
    }

    public ArrayList<AreaList> getAreaLists()
    {
        return areaLists;
    }

    protected void progress(double p)
    {

    }

    public ImagePlus getCropImage(int defVal)
    {
        final ImageProcessor refIp = new ByteProcessor(2,2);

        if (!areaLists.isEmpty())
        {
            final Rectangle bound = areaLists.get(0).getBoundingBox();
            final ImageStack stack;
            final Project project = areaLists.get(0).getProject();
            final List<Layer> layerRange;
            final ImagePlus imp;
            Layer minLayer = areaLists.get(0).getLayerRange().get(0);
            Layer maxLayer = areaLists.get(0).getLastLayer();
            int idxLayer = 0;

            for (final AreaList areaList : areaLists)
            {
                bound.add(areaList.getBoundingBox());

                if (areaList.getLayerRange().get(0).getZ() < minLayer.getZ())
                {
                    minLayer = areaList.getLayerRange().get(0);
                }
                if (areaList.getLastLayer().getZ() > maxLayer.getZ())
                {
                    maxLayer = areaList.getLastLayer();
                }
            }

            stack = new ImageStack(bound.width, bound.height);
            layerRange = minLayer.getParent().getLayers(minLayer, maxLayer);

            progress(0);

            for (Layer layer : layerRange)
            {
                final ImageProcessor ip;
                final ImageProcessor flatCrop;

                IJ.log("Releasing memory...");

                project.getLoader().releaseToFit(bound.width * bound.height * 10);
                ip = refIp.createProcessor(bound.width, bound.height);
                ip.add(defVal);

                IJ.log("Loading image data...");
                flatCrop = Patch.makeFlatImage(ImagePlus.GRAY8, layer, bound, 1.0,
                        layer.getAll(Patch.class), new Color(defVal, defVal, defVal)).crop();
                IJ.log("Got flat image for " + layer.toString());

                for (final AreaList areaList : areaLists)
                {
                    Area area = areaList.getArea(layer);
                    if (area != null)
                    {
                        AffineTransform aff = areaList.getAffineTransformCopy();
                        ShapeRoi roi;
                        Rectangle rb;

                        aff.translate(-bound.x, -bound.y);
                        roi = new ShapeRoi(areaList.getArea(layer).createTransformedArea(aff));
                        rb = roi.getBounds();

                        for (int x = rb.x; x - rb.x < rb.width; ++x)
                        {
                            for (int y = rb.y; y - rb.y < rb.height; ++y)
                            {
                                if (roi.contains(x, y))
                                {
                                    if (check(ip, x, y) && check(flatCrop, x, y))
                                    {
                                        ip.set(x, y, flatCrop.get(x, y));
                                    }
                                }
                            }
                        }

                    }
                }

                stack.addSlice("", ip);

                progress(((double)idxLayer++) / ((double)layerRange.size()));
                IJ.log("Done for " + idxLayer);
            }

            imp = new ImagePlus("Cropped AreaList Stack", stack);
            imp.setCalibration(minLayer.getParent().getCalibrationCopy());
            return imp;
        }
        else
        {
            return null;
        }
    }


    private boolean check(final ImageProcessor ip, final int x, final int y)
    {
        return x > 0 && y > 0 && x <= ip.getWidth() && y <= ip.getHeight();
    }

}

