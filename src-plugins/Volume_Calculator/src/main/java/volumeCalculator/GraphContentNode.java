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
import ij3d.ContentNode;
import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Tuple3d;

/**
 * GraphContentNode is a 3D Viewer ContentNode designed to be the structure returned
 * by the AnalyzedGraph class as a Java 3D representation of the vascular tree
 * representation created by the AnalyzeSkeleton plugin.
 *
 * In essence it is a Java 3D scene graph (BranchGroup).
 *
 * @author pcmarks
 */
public class GraphContentNode extends ContentNode {
    private final Point3f min;
    private final Point3f max;
    private final Point3f center;

    public GraphContentNode() {
        super();
        min = new Point3f(0,0,0);
        max = new Point3f(1,1,1);
        center = new Point3f(1f,1f,1f);
    }

    @Override
    public float getVolume() {
        return 0;
    }

    @Override
    public void getMin(Tuple3d tpld) {
        tpld.set(this.min);
    }

    @Override
    public void getMax(Tuple3d tpld) {
        tpld.set(this.max);
    }

    @Override
    public void getCenter(Tuple3d tpld) {
        tpld.set(this.center);
    }

    @Override
    public void channelsUpdated(boolean[] blns) {
        ;
    }

    @Override
    public void thresholdUpdated(int i) {
        ;
    }

    @Override
    public void colorUpdated(Color3f clrf) {
        ;
    }

    @Override
    public void transparencyUpdated(float f) {
        ;
    }

    @Override
    public void shadeUpdated(boolean bln) {
        ;
    }

    @Override
    public void eyePtChanged(View view) {
        ;
    }

    @Override
    public void lutUpdated(int[] ints, int[] ints1, int[] ints2, int[] ints3) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void swapDisplayedData(String string, String string1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clearDisplayedData() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void restoreDisplayedData(String string, String string1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
