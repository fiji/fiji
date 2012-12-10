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
import ij.measure.Calibration;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Color3f;


/**
 * <p>An instance of Volumes is used to keep track of the volume values accumulated by
 * the Volume_Calculator plugin. There is one set of volume values for each
 * edge "color". There can be an arbitrary number of "colors";  only one color is
 * active at one time. When the user selects an edge the display color of the
 * edge is changed to the current color and the number of voxels is calculated
 * for this edge.
 * </p>
 * <p>
 * MVC: An instance of Volumes (the Model) is shared by CustomValueBehavior (the
 * Controller) and VolumePanel (the View).
 * </p>
 * 
 * @author pcmarks
 */
public class Volumes {

    private final String calibrationUnits;
    private final double volumeMultiplier;

    private int                     currentColorIndex = 0;

    private List<Double>             volumes = new ArrayList<Double>();
    private List<Integer>           voxelCounts = new ArrayList<Integer>();
    private List<Color3f>           colors = new ArrayList<Color3f>();

    public Volumes (Calibration calibration) {
        this.calibrationUnits = calibration.getUnits();
        this.volumeMultiplier = calibration.pixelDepth *
                                calibration.pixelHeight *
                                calibration.pixelWidth;
//        System.out.println("VOLUME MULT: "+this.volumeMultiplier);
    }

    /**
     * The user has created another Volume Color. Make a place for its color and
     * its volume. Don't allow the use of an existing color.
     * 
     * @param chosenColor
     * @return
     */
    public boolean addVolumeColor(Color chosenColor) {
        volumes.add(new Double(0));
        voxelCounts.add(new Integer(0));
        Color3f newColor = new Color3f(chosenColor);
        for (Color3f color : colors) {
            if (newColor.equals(color)) return false;   // Already used
        }
        colors.add(new Color3f(chosenColor));
        currentColorIndex = volumes.size()-1;
        return true;
    }

    Color3f getSelectedColor() {
        return colors.get(currentColorIndex);
    }

    int getCurrentColorIndex() {
        return currentColorIndex;
    }

    void setCurrentColorIndex(int index) {
        currentColorIndex = index;
    }

    void updateVoxelCount(int colorIndex, int count) {
        if (colorIndex == UserData.INITIAL_COLOR_INDEX) return;
        int voxelCount = voxelCounts.get(colorIndex) + count;
        voxelCounts.set(colorIndex, voxelCount);
        Double volume = volumes.get(colorIndex);
        volumes.set(colorIndex, voxelCount * volumeMultiplier);
    }

    Double getVolumeAt(int colorIndex) {
        return volumes.get(colorIndex);
    }

    public String getCalibrationUnits() {
        return calibrationUnits;
    }

    Color3f getColorAt(int colorIndex) {
        return colors.get(colorIndex);
    }

    void clearVoxelCount(int colorIndex) {
        updateVoxelCount(colorIndex, -voxelCounts.get(colorIndex));
    }
    
}
