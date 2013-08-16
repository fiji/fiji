package oldsegmenters;

import ij.ImagePlus;

import amira.AmiraParameters;
import vib.SegmentationViewerCanvas;

/**
 * Wrapper clas for an ImagePlus, stored vairous objects in the properties of the Image plus
 * OBjects used for segmentating
 * User: Tom Larkworthy
 * Date: 21-Jun-2006
 * Time: 00:08:57
 */

public class SegmentatorModel {
    final ImagePlus data;

    public static final String LABEL_IMAGE_PLUS = "SegmentatorModel.labels";
    public static final String LABEL_CANVAS = "SegmentatorModel.labels.canvas";
    public static final String CURRENT_MATERIAL = "SegmentatorModel.currentMaterial";

    public SegmentatorModel(ImagePlus data) {
        this.data = data;
    }

    public ImagePlus getLabelImagePlus(){
        return (ImagePlus) data.getProperty(LABEL_IMAGE_PLUS);
    }
    public void setLabelImagePlus(ImagePlus ip){
        data.setProperty(LABEL_IMAGE_PLUS, ip);
    }

    public ImagePlus getOriginalImage() {
        return data;
    }

    public SegmentationViewerCanvas getLabelCanvas(){
        return (SegmentationViewerCanvas) data.getProperty(LABEL_CANVAS);
    }
    public void setLabelCanvas(SegmentationViewerCanvas canvas){
        data.setProperty(LABEL_CANVAS, canvas);
    }

    public AmiraParameters getMaterialParams() {
        if(getLabelImagePlus() == null) return null;
        return new AmiraParameters(getLabelImagePlus());
    }

    public void setCurrentMaterial(AmiraParameters.Material currentMaterial) {
        data.setProperty(CURRENT_MATERIAL, currentMaterial);
    }

    public AmiraParameters.Material getCurrentMaterial() {
        return (AmiraParameters.Material) data.getProperty(CURRENT_MATERIAL);
    }

    public void updateSlice(int z) {
        getLabelCanvas().updateSlice(z);
        data.updateAndDraw();
    }

    public void updateSliceNoRedraw(int z) {
        getLabelCanvas().updateSlice(z);              
    }
}
