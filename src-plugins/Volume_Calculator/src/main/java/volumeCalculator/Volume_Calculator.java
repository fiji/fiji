package volumeCalculator;
/*
Copyright (c) 2012, Peter C Marks and Maine Medical Center Research Institute
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.Duplicator;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij3d.Content;
import ij3d.Image3DUniverse;
import java.awt.BorderLayout;
import java.awt.Color;
import javax.media.j3d.Transform3D;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3d;

/**
 * <p>This is the Volume_Calculator ImageJ/Fiji plugin. It is a plugin that is
 * capable of measuring the volume of a selected path in the 3D skeletonized
 * representation of (typically) vascalatures.
 * The volume is measured in voxels or other units,
 * e.g. cm^3, depending on the meta information associated with the source image
 * file.
 * </p>
 * <p>
 * Internally it first runs the Skeletonize and then the AnalyzeSkeleton plugins. The
 * output of the analysis is used to draw a Java 3D image. It is displayed. 
 * The user can select lines that correspond to the original image and get their 
 * volumes.
 * </p>
 *
 * @author Peter C Marks - Maine Medical Center Research Institute (MMCRI.org)
 */
public class Volume_Calculator implements PlugInFilter {

    public static final Vector3d INITIAL_SCALING = new Vector3d(.05d,.05d,.05d);
    public static final float   TRANSPARENCY = 0.4f;
    public static final Color3f TRANSPARENCY_COLOR = new Color3f(Color.gray);


    private AnalyzedGraph vasculature;
    private Image3DUniverse universe;
    private ImagePlus imagePlus;
    private ImageProcessor ip;
    private Content content;
    private VolumesPanel volumesPanel;
    private ImagePlus originalImage;

    /////
    // Implementation of the PlugInFilter interface
    /////
    @Override
    public int setup(String string, ImagePlus imagePlus) {
        this.imagePlus = imagePlus;
        if (null != imagePlus) {
            this.originalImage = (new Duplicator()).run(imagePlus);
        }
        return DOES_ALL + STACK_REQUIRED;           // Gotta have a Stack!
    }

    /**
     * Start the Volume_Calculator plugin. This means invoking the
     * Fiji 3D Viewer in which the Java 3D image will be displayed. A 3D Viewer
     * Content object is created and used to encapsulate the Java 3D scene graph.
     * 
     * @param ip ImageProcessor
     */
    @Override
    public void run(ImageProcessor ip) {

        this.ip = ip;
        vasculature = new AnalyzedGraph();
        vasculature.init(imagePlus);
        vasculature.getSceneGraph().compile();

        // Create a universe and show it. Don't remember why, but this must
        // occur before the contents are added to the universe.
        universe = new Image3DUniverse();
        universe.show();

        // Ask AnalyzedGraph for a Java 3D version of the network -
        // Package it all up for ij3d use.
        GraphContentNode contentNode = vasculature.getSceneGraph();
        content = new Content("VoCal Network");
        content.setUserData(vasculature);
        content.display(contentNode);
        universe.addContent(content);
        
        // We need this image's Calibration info from the image in order to
        // calculate the volumes accurately. Given to Volumes.
        Calibration calibration = imagePlus.getCalibration();


//        Content subjectContent = universe.addContent(originalImage, Content.VOLUME);
////        Content subjectContent = universe.addContent(originalImage, Content.SURFACE);
//
//        Transform3D t3d = new Transform3D();
//        t3d.setScale(INITIAL_SCALING);
//        subjectContent.applyTransform(t3d);
//        subjectContent.setColor(TRANSPARENCY_COLOR);
//        subjectContent.setTransparency(TRANSPARENCY);       // Higher = more transparent
//
//        universe.centerSelected(content);
//        universe.centerSelected(subjectContent);
//        content.setLocked(true);
//        subjectContent.setLocked(true);

        // Create the volumes data structure. Its gui is: VolumesPanel
        // VolumePanel is placed to the SOUTH of what's in 3D Viewer
        Volumes volumes = new Volumes(calibration);
        volumesPanel = new VolumesPanel(volumes, vasculature, universe);
        // Create the picking behavior (Controller) for the graphic view of
        // the vasculature. This controller also needs a Volumes instance in which to store
        // the selected volumes.
        universe.setInteractiveBehavior(
                new CustomVolumeBehavior2(universe, content, volumes, volumesPanel, imagePlus, originalImage));
//        universe.addContent(content);
        // Make sure that the bounding box is not displayed upon selection; user
        // can reset this.
        universe.setShowBoundingBoxUponSelection(false);

        universe.getWindow().add(volumesPanel,BorderLayout.SOUTH);
        universe.getWindow().pack();


    }

    /**
     * main() is available in case you want to test this plugin directly.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ImageJ imageJ = null;
        try {
            imageJ = new ij.ImageJ();
            IJ.run("Open...");
            Volume_Calculator volume_calculator = new Volume_Calculator();
            volume_calculator.setup("", IJ.getImage());
            volume_calculator.run(volume_calculator.imagePlus.getProcessor());
        } catch (Exception e) {
            e.printStackTrace(System.out);
            IJ.showMessage(e.getLocalizedMessage());
            System.out.println(""+e.getLocalizedMessage());
            imageJ.quit();
        }
    }
}
