package edu.utexas.clm.crop;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ini.trakem2.display.AreaList;
import ini.trakem2.display.LayerSet;
import ini.trakem2.plugin.TPlugIn;

import java.util.ArrayList;

/**
 *
 */
public class AreaList_Crop implements TPlugIn
{
    public boolean setup(final Object... params)
    {
        return false;
    }

    public Object invoke(final Object... params)
    {
        final ArrayList<AreaList> areaLists = new ArrayList<AreaList>(params.length);
        for (final Object ob : params)
        {
            if (ob instanceof AreaList)
            {
                areaLists.add((AreaList)ob);
            }
        }

        if (areaLists.size() > 0)
        {
//            areaListCropGUI(areaLists);
            for (final AreaList al : areaLists)
            {
                al.getStack(ImagePlus.GRAY8, 1.0).show();
            }
        }
        else
        {
            IJ.showMessage("No area lists were selected");
        }

        return null;
    }

    public boolean applies(final Object ob)
    {
        return ob != null && ob instanceof AreaList;
    }

//    private static GenericDialog createDialog(final ArrayList<AreaList> areaLists)
//    {
//        final GenericDialog gd = new GenericDialog("Select Layers");
//        final LayerSet layerSet = areaLists.get(0).getLayerSet();
//        final String[] names = new String[layerSet.size()];
//
//        for (int i = 0; i < layerSet.size(); ++i)
//        {
//            names[i] = layerSet.getLayer(i).toString();
//        }
//
//        gd.addChoice("Begin", names, names[0]);
//        gd.addChoice("End", names, names[names.length - 1]);
//
//        return gd;
//    }

//    public static void areaListCropGUI(final ArrayList<AreaList> areaLists)
//    {
//        for (final AreaList al : areaLists)
//        {
//            al.getStack(ImagePlus.GRAY8, 1.0).show();
//        }
//    }

}
