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
import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;
import com.sun.j3d.utils.picking.PickTool;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij3d.Content;
import ij3d.Image3DUniverse;
import ij3d.behaviors.InteractiveBehavior;
import ij3d.behaviors.InteractiveViewPlatformTransformer;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Group;
import javax.media.j3d.LineArray;
import javax.media.j3d.Node;
import javax.media.j3d.PickSegment;
import javax.media.j3d.SceneGraphPath;
import javax.media.j3d.Shape3D;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3i;
import skeleton_analysis.Edge;
import skeleton_analysis.Graph;
import skeleton_analysis.Point;

/**
 * <p>
 * CustomVolumeBehavior2 implements the special Java 3d Picking behavior needed by the
 * Volume_Calculator. Here we are only concerned with the MOUSE_CLICKED event, any
 * others are passed up unless the user has chosen to use keyboard navigation
 * only. In this case, keyboard events are processed.
 * </p>
 * <p>
 * The special behavior consists of waiting for two clicks of the mouse button
 * followed by the identification of the Java 3D nodes that are in a path
 * connected between the two click points. This path is traversed calculating
 * the volume. If there is no path, the user is warned and nothing is done.
 * </p>
 * <p>For an explanation of how the voxels are counted, please see the
 * <pre>reconstructEdge</pre> method comments.
 * </p>
 * <p>
 * The InteractiveBehavior of the 3D Viewer is extended so as to catch these
 * events first.
 *</p>
 * @author pcmarks - marksp at mmc.org
 */
public class CustomVolumeBehavior2 extends InteractiveBehavior {

    /**
     * Where the branch node of a tree for a scene graph path is located.
     * NB: This could change!!
     */
    private static int BRANCH_NODE_INDEX = 3;
    private static final String NO_PATH_MSG = "No path between those two points.";

    private final PickCanvas pickCanvas;
    private PickSegment pickSegment = new PickSegment();
    private boolean firstPickPicked;
    /**
     * The Java 3d paths corresponding to a pair of mouse clicks. The
     * path is from the top down. Used to see if these two paths are
     * connected.
     */
    private SceneGraphPath firstClickSGP;
    private SceneGraphPath secondClickSGP;

    private Volumes volumes;                // Model
    private VolumesPanel volumesPanel;      // View
    private InteractiveViewPlatformTransformer viewTransformer;
    private final ImagePlus originalImage;
    /*
     * Properties of the original image
     */
    private final int imageHeight, imageWidth, imageDepth;

    /**
     * <p>Build an instance of CustomVolumeBehavior. It is a subclass of InteractiveBehavior
     * which means that its basic role is to intercept key and mouse events.
     * </p><p>
     * Mouse events are those that select an edge from a display graph.
     * </p><p>
     * Key events can be used for navigational control (eg, left, right, etc)
     * </p><p>
     * The bulk of the logic determines whether one can traverse between the clicks
     * on an edge or edges. If so, for every edge - inclusive - between the two
     * selected edges, the coordinates of the edge are used to extract the pixels
     * (present and eroded) from the original image.
     * </p><p>
     * The so-called blob lists are built in this constructor for late use.
     * 
     * @param universe The J3D Universe
     * @param content   A wrapper around the J3D group representing the graph
     * @param volumes A set of accumulaters for each selected color
     * @param volumesPanel Where the volumes values appear
     * @param imagePlus ImageJ structure
     * @param originalImage The original image as an ImajeJ structure
     */
    public CustomVolumeBehavior2(
            Image3DUniverse universe,
            Content content,
            Volumes volumes,
            VolumesPanel volumesPanel,
            ImagePlus imagePlus,
            ImagePlus originalImage) {

        super(universe); // The InteractiveBehavior
        this.viewTransformer = universe.getViewPlatformTransformer();
        this.volumes = volumes;
        this.volumesPanel = volumesPanel;
        this.originalImage = originalImage;
        this.imageHeight = originalImage.getHeight();
        this.imageWidth = originalImage.getWidth();
        this.imageDepth = originalImage.getNSlices();
        /*
         * Java 3d tools to deal with picking a 3d element.
         */
        pickCanvas = new PickCanvas(universe.getCanvas(), content);
        pickCanvas.setMode(PickTool.GEOMETRY_INTERSECT_INFO);
        pickCanvas.setShape(pickSegment, new Point3d(2, 2, 2));

        // create slice and edge blob lists
        createBlobLists((AnalyzedGraph) content.getUserData());

    }

    /**
     * Intercept the 3D Viewer's mouse event processing so that we can focus on
     * the clicking of network paths first.
     *
     * The user can opt to not process other mouse events. Otherwise the mouse
     * events not handled here are passed up to the 3D Viewer.
     *
     * Special consideration must be made for the fact that the AnalyzedGraph
     * BranchGroup is now a child of the ij3D viewers BranchGraphs.
     *
     * @param e the mouse event (click, etc.)
     *
     */
    @Override
    public void doProcess(MouseEvent e) {
        int iD = e.getID();
        if (iD == MouseEvent.MOUSE_CLICKED) {
            // Get the point on the geometry where the mouse
            // press occurred
            pickCanvas.setShapeLocation(e.getX(), e.getY());
            PickResult pickResult = pickCanvas.pickClosest();
            if (pickResult != null) {
                if (firstPickPicked) {
                    volumesPanel.showStatus("Second Click");
                    secondClickSGP = pickResult.getSceneGraphPath();

                    // Determine if the two paths share *any* nodes. Because each
                    // tree is a different branch graph, the branch nodes
                    // for each node must be equal for there to be a connection.
                    // NOTA BENE: BRANCH_NODE_INDEX value is hardwired.
                    if (firstClickSGP.getNode(BRANCH_NODE_INDEX)
                            != secondClickSGP.getNode(BRANCH_NODE_INDEX)) {
                        // Nothing in common - let the user know this, reset and leave.
                        IJ.showMessage(NO_PATH_MSG);
                        firstPickPicked = false;
                        volumesPanel.showStatus("");
                        return;
                    }
                    int lenSGP1 = firstClickSGP.nodeCount();
                    int lenSGP2 = secondClickSGP.nodeCount();

                    int commonCount = 0;
                    while ((commonCount < lenSGP1) && (commonCount < lenSGP2)
                            && (firstClickSGP.getNode(commonCount)
                            == secondClickSGP.getNode(commonCount))) {
                        commonCount++;
                    }

                    if (commonCount > 0) {
                        // First check and see if the two paths are exactly the
                        // same. If so, just process one path (not the second path).
                        boolean oneBranchOnly = firstClickSGP.equals(secondClickSGP);

                        if (lenSGP1 > commonCount) {
                            for (int c = commonCount; c < lenSGP1; c++) {
                                Node node = firstClickSGP.getNode(c);
                                if (node instanceof Group) {
                                    for (int n = 0; n < ((Group) node).numChildren(); n++) {
                                        Node childNode = ((Group) node).getChild(n);
                                        if (childNode instanceof Shape3D) {
                                            computeAndDisplayEdgeVolume((Shape3D) childNode);
                                            highlightEdge((Shape3D) childNode);
                                        }
                                    }
                                }
                            }
                        } else {
                            int c = commonCount - 1;
                            Node node = firstClickSGP.getNode(c);
                            if (node instanceof Group) {
                                for (int n = 0; n < ((Group) node).numChildren(); n++) {
                                    Node childNode = ((Group) node).getChild(n);
                                    if (childNode instanceof Shape3D) {
                                        computeAndDisplayEdgeVolume((Shape3D) childNode);
                                        highlightEdge((Shape3D) childNode);
                                    }
                                }
                            }
                        }
                        if (!oneBranchOnly) {
                            if (lenSGP2 > commonCount) {
                                for (int c = commonCount; c < lenSGP2; c++) {
                                    Node node = secondClickSGP.getNode(c);
                                    if (node instanceof Group) {
                                        for (int n = 0; n < ((Group) node).numChildren(); n++) {
                                            Node childNode = ((Group) node).getChild(n);
                                            if (childNode instanceof Shape3D) {
                                                computeAndDisplayEdgeVolume((Shape3D) childNode);
                                                highlightEdge((Shape3D) childNode);
                                            }
                                        }
                                    }
                                }
                            } else {
                                int c = commonCount - 1;
                                Node node = secondClickSGP.getNode(c);
                                if (node instanceof Group) {
                                    for (int n = 0; n < ((Group) node).numChildren(); n++) {
                                        Node childNode = ((Group) node).getChild(n);
                                        if (childNode instanceof Shape3D) {
                                            computeAndDisplayEdgeVolume((Shape3D) childNode);
                                            highlightEdge((Shape3D) childNode);
                                        }
                                    }
                                }
                            }
                        }

                    }
                    firstPickPicked = false;
                    volumesPanel.showStatus("           ");
                } else {
                    firstPickPicked = true;
                    firstClickSGP = pickResult.getSceneGraphPath();
                    volumesPanel.showStatus("First Click");
                }
            } else {
                firstPickPicked = false;        // Turn off - bail out
            }
        } else {
            // If the user only wants to use the keyboard for navigation then
            // don't process the other mouse events.
            // ?? Should any other events be flushed??
            if (!volumesPanel.getKeyNavigationCheckBoxState()) {
                super.doProcess(e);
            }
        }
        return;
    }

    /**
     * Handle the alt arrow mouse events for zooming here because the
     * superclass does not do anything: zoom(1)
     *
     * @param ke the key event, e.g., arrow up.
     */
    @Override
    protected void doProcess(KeyEvent ke) {
        if (ke.isAltDown()) {
            int code = ke.getKeyCode();
            switch (code) {
                case KeyEvent.VK_UP:
                    viewTransformer.zoomTo(.9);
                    return;
                case KeyEvent.VK_DOWN:
                    viewTransformer.zoomTo(1.1);
                    return;
            }
        } else {
            super.doProcess(ke);
        }
    }

    /**
     * Use the currently selected color to paint an edge.
     *
     * @param shape a Java3D shape - must be a LineArray
     *
     */
    void highlightEdge(Shape3D shape) {

        Color3f highlightColor = volumes.getSelectedColor();
        if (shape.getGeometry() instanceof LineArray) {
            Appearance appearance = shape.getAppearance();
            ColoringAttributes ca = appearance.getColoringAttributes();
            Color3f before = new Color3f();
            appearance.setColoringAttributes(ca);
            ca.getColor(before);
            ca.setColor(highlightColor);
        }
    }

    /**
     * Compute the volume (number of voxels) that this Java3D shape intercepts
     * in the original image (prior to skeletonization)
     * 
     * @param shape
     */
    private void computeAndDisplayEdgeVolume(Shape3D shape) {
        // Has user clicked on a J3D line?
        if (!(shape.getGeometry() instanceof LineArray)) {
            return;         // NOPE
        }
        // Does this line correspond to an Edge?
        UserData userData = (UserData) (shape.getUserData());
        Object graphData = userData.getGraphInfo();
        if (!(graphData instanceof Edge)) {
            return;          // NOPE
        }

        int voxelCount = reconstructEdge((Edge) graphData);

        // Initialize
        int oldColorIndex = userData.getColorIndex();
        int currentColorIndex = volumes.getCurrentColorIndex();

        volumes.updateVoxelCount(oldColorIndex, -voxelCount);
        volumes.updateVoxelCount(currentColorIndex, +voxelCount);
        // Make corresponding changes to the VolumePanel
        volumesPanel.updateVoxelCount(oldColorIndex);
        volumesPanel.updateVoxelCount(currentColorIndex);
        userData.setColorIndex(currentColorIndex);
    }

    /**
     * This method is <b>not</b> part of the plugin's advertised public interface. It can
     * be used to automatically select all the edges in the graph and calculate
     * their total volume in voxels. It is as if the user clicked on all the
     * edges in the graphic.
     * 
     * @return number of voxels in the entire image.
     */
    public int computeAllEdgesVolume() {

        int voxelCount = 0;
        int edgeCount = 0;
        for (Edge edge : edgeBlobs.keySet()) {
            voxelCount += reconstructEdge(edge);
            edgeCount++;
        }
//        System.out.println("Total voxelCount = "+voxelCount+" edgeCount = "+edgeCount);
        return voxelCount;
    }

    /**
     * If there are  any adjacent pixels at this checkPoint then there could also be
     * a blob there as well. Get the biggest contiguous blob possible.
     * 
     * @param checkPoint
     * @param edge
     * @param eroded
     * @return a new Blob or null
     */
    private Blob checkForBlob(Point checkPoint, Edge edge, boolean eroded) {
        Blob newBlob = null;
        Point3i point3i = new Point3i(checkPoint.x, checkPoint.y, checkPoint.z);
        // Get this points adjacent pixels. There can be a maximum of eight.
        Set<Point3i> neighbors = neighbors(point3i, 255);
        if (!neighbors.isEmpty()) {
            for (Point3i nPoint3i : neighbors) {
                Point nPoint = new Point(nPoint3i.x, nPoint3i.y, nPoint3i.z);
                newBlob = getBlobAtPoint(nPoint, eroded);
                if (newBlob == null) {
                    return null;
                }
                if (!newBlob.eroded) {
                    return null;
                }
                newBlob.edges.add(edge);
                return newBlob;
            }
        }
        return newBlob;
    }

    /**
     * A structure used to hold the points of a blob (contiguous pixels in a
     * slice) as well as the edges that pass through it.
     *
     */
    private class Blob {

        Set<Point3i> points = new HashSet<Point3i>();
        Set<Edge> edges = new HashSet<Edge>();  // that pass through
        boolean eroded = false;             // true if blob was eroded away

        Blob() {
            this.points = new HashSet<Point3i>();
            this.edges = new HashSet<Edge>();
        }

        Blob(Set<Point3i> points) {
            this.points = points;
            this.edges = new HashSet<Edge>();
        }

        int getDivisor() {
            return edges.size();
        }

        @Override
        public String toString() {
            return "(" + points.size() + " / " + edges.size() + "/" + eroded + ")";
        }
    }
    /**
     * edgeBlobs represents all the blobs that make up this edge, sometimes in the
     * same slice - usually through several slices.
     *
     * A map with a key of Edge - the primary structure in the AnalyzeSkeleton's
     * graph. The value is a list of Blobs.
     */
    HashMap<Edge, CopyOnWriteArrayList<Blob>> edgeBlobs = new HashMap<Edge, CopyOnWriteArrayList<Blob>>();
    /**
     * nSliceBlobs represents a list of Blobs that can be found in each slice.
     * 
     * A list organized by slice number. Each slice element in this list is a
     * list of Blobs. This list is created and updated during the construction of
     * the edgeBlobs map. See getBlobAtPoint().
     */
    List<List<Blob>>nSliceBlobs;

    /**
     * Given the graph (really a forest of trees) created by the AnalyzeSkeleton
     * plugin, a List and a Map are constructed. See above for a description of
     * these two structures.
     *
     * @param graph
     */
    private void createBlobLists(AnalyzedGraph graph) {
        Graph[] forest;
        Blob v1Blob = new Blob();
        Blob v2Blob = new Blob();
        Blob slabBlob;
        nSliceBlobs = new ArrayList(Collections.nCopies(imageDepth, null));
        forest = graph.getSkeletonResult().getGraph();
//        System.out.println("Trees in the forest: "+forest.length);
        int treecount = 0;
        for (Graph tree : forest) {
            // Skip those trees with no (zero) edges
            if (tree.getEdges().size() < 1) {
//                if (tree.getVertices().size() > 0) {
//                    for (Vertex v : tree.getVertices()) {
//                        for (Point p : v.getPoints()) {
//                            Blob pBlob = getBlobAtPoint(p, false);  // Called for the side effect
//                        }
//                    }
//                }
                continue;   // Skip this tree
            }
            treecount++;
//            System.out.println("T#: "+treecount+" Edges: "+tree.getEdges().size());
            int edgecount = 0;
            for (Edge edge : tree.getEdges()) {
                if (edge.getType() == -1) continue;
//                System.out.println("     E#: "+(++edgecount));
                for (Point v1Point : edge.getV1().getPoints()) {
                    v1Blob = getBlobAtPoint(v1Point, false);
                    if (v1Blob != null) {
                        v1Blob.edges.add(edge);
                        putBlobAtEdge(v1Blob, edge, false);
                    }
                }
                for (Point v2Point : edge.getV2().getPoints()) {
                    v2Blob = getBlobAtPoint(v2Point, false);
                    if (v2Blob != null) {
                        v2Blob.edges.add(edge);
                        putBlobAtEdge(v2Blob, edge, false);
                    }
                }

                for (Point slabPoint : edge.getSlabs()) {
                    slabBlob = getBlobAtPoint(slabPoint, false);
                    if (slabBlob != null) {
                        slabBlob.edges.add(edge);
                        putBlobAtEdge(slabBlob, edge, false);
                    }
                }
            }
        }
//        System.out.println("Trees used: "+treecount);
    }



    /**
     * 
     * @param edge
     */
    void recoverErodedBlobs(Edge edge) {
        Point checkPoint;
        Blob checkBlob;
        List<Blob> newBlobList = new LinkedList<Blob>();

        List<Blob> blobList = edgeBlobs.get(edge);
        if (null == blobList) return;
        do {
            newBlobList.clear();
            ListIterator<Blob> listIter = blobList.listIterator();
            while (listIter.hasNext()) {
                Blob vBlob = listIter.next();
                for (Point3i point3i : vBlob.points) {
                    checkPoint = new Point(point3i.x, point3i.y, point3i.z);
                    do {
                        checkPoint.z++;
                        checkBlob = checkForBlob(checkPoint, edge, true);
                        if (checkBlob != null && checkBlob.eroded) {
                            checkBlob.eroded = false;
                            checkBlob.edges.add(edge);
                            putBlobAtEdge(checkBlob, edge, false);
                            newBlobList.add(checkBlob);
                        }
                    } while (checkBlob != null);
                    checkPoint = new Point(point3i.x, point3i.y, point3i.z);
                    do {
                        checkPoint.z--;
                        checkBlob = checkForBlob(checkPoint, edge, true);
                        if (checkBlob != null && checkBlob.eroded) {
                            checkBlob.eroded = false;
                            checkBlob.edges.add(edge);
                            putBlobAtEdge(checkBlob, edge, false);
                            newBlobList.add(checkBlob);
                        }
                    } while (checkBlob != null);
                }
            }
        } while (!newBlobList.isEmpty());

    }

    /**
     * 
     * @param point
     * @param eroded
     * @return
     */
    Blob getBlobAtPoint(Point point, boolean eroded) {
        Point3i point3i;

        point3i = new Point3i(point.x, point.y, point.z);
        // Check the list of slice blobs first
        List<Blob> blobList = nSliceBlobs.get(point.z);
        if (blobList == null) {
            // New slice encountered - add it with an empty BlobList
            blobList = new CopyOnWriteArrayList<Blob>();
            nSliceBlobs.set(point.z, blobList);
        } 
        // Run through this slice's blobs to see if one of them
        // contains this point. If so, return that blob.
        for (Blob blob : blobList) {
            if (blob.points.contains(point3i)) {
                return blob;
            }
        }
        // Need to create a new blob that will contain this point,
        // add it to this slice's blob list and return it
        Set<Point3i> points = slabNeighbors(point3i);
        if (points.isEmpty()) {
            return null;
        }
        Blob newBlob = new Blob(points);
        newBlob.eroded = eroded;
        blobList.add(newBlob);
        return newBlob;
    }

    /**
     * 
     * @param blob
     * @param edge
     * @param eroded
     */
    void putBlobAtEdge(Blob blob, Edge edge, boolean eroded) {
        // See if this edge has an entry already
        CopyOnWriteArrayList<Blob> blobList = edgeBlobs.get(edge);

        if (blobList == null) {
            // No, create a new entry with a empty blob list
            blobList = new CopyOnWriteArrayList<Blob>();
            edgeBlobs.put(edge, blobList);
        }
        // search through the list of blobs to see if it
        // exists
        for (Blob blob2 : blobList) {
            if (blob2.points.equals(blob.points) && blob2.eroded == eroded) {
                blob2.edges.add(edge);
                return;
            }
        }
        blob.edges.add(edge);
        blobList.add(blob);
    }

    int grandVoxelCount = 0;
    double grandVoxelCountD = 0.0;

    /**
     * This method attempts to identify all the voxels from the original image
     * that this skeletonized edge passes through.
     *
     * Steps:
     * 1. Voxels may have been "eaten" away from the sides of the edge. Recover
     * these voxels.
     *
     * 2. Voxels may have been "eaten" away from the ends of the vertices. Recover
     * these voxels.
     *
     * 3. For this edge, retrieve all the blobs that it passes through.
     *
     * 4. For each blob, compute the number of voxels by dividing the size of the
     * blob (number of Points) by the number of edges that share (pass through)
     * this blob. Points are allocated evenly amongst the edges.
     *
     * @param edge
     * @return the number of voxels in the original image that this edge represents.
     */
    private int reconstructEdge(Edge edge) {
        int voxelCount = 0;
        double voxelCountD = 0.0;

        recoverErodedBlobs(edge);

        for (Edge branch : edge.getV1().getBranches()) {
            recoverErodedBlobs(branch);
        }
        for (Edge branch : edge.getV2().getBranches()) {
            recoverErodedBlobs(branch);
        }

        List<Blob> blobList = edgeBlobs.get(edge);

        if (blobList == null) {
            return voxelCount;
        }
        for (Blob blob : blobList) {
            double size = blob.points.size();
            voxelCountD += size / blob.edges.size();
            voxelCount += blob.points.size() / blob.edges.size();
        }
        int v = 0;

//        for (List<Blob> bList : nSliceBlobs) {
//            if (null != bList) {
//                for (Blob blob : bList) {
//                    v += blob.points.size();
//                }
//            }
//        }
        grandVoxelCount += voxelCount;
        grandVoxelCountD += voxelCountD;

        return voxelCount;

    }


    /**
     * neighbors() returns the eight surrounding points only if they are set to
     * the target pixel value. Slice limits are checked.
     *
     * @param homePoint The place from which to look around
     * @param targetPixel What the value of a pixel must be for acceptance
     * @return Points that were found or an empty set
     */
    private Set<Point3i> neighbors(Point3i homePoint, int targetPixel) {
        Set<Point3i> neighborPointSet = new HashSet<Point3i>();
        if ((homePoint.z >= originalImage.getStackSize()) ||
                (homePoint.z < 0)) {
            return neighborPointSet;    // Run out of boundary
        }
        
        ImageProcessor sliceProcessor =
                originalImage.getStack().getProcessor(homePoint.z + 1);
        
        if (sliceProcessor.getPixel(homePoint.x, homePoint.y) == targetPixel) {
            neighborPointSet.add(homePoint);
        }

        // Neighbors above (in the plane)
        int x = homePoint.x;
        int y = homePoint.y;

        if (y - 1 >= 0) {
            if (x + 1 < imageWidth) {
                if (sliceProcessor.getPixel(x + 1, y - 1) == targetPixel) {
                    neighborPointSet.add(new Point3i(x + 1, y - 1, homePoint.z));
                }
            }
            if (x - 1 >= 0) {
                if (sliceProcessor.getPixel(x - 1, y - 1) == targetPixel) {
                    neighborPointSet.add(new Point3i(x - 1, y - 1, homePoint.z));
                }
            }
            if (sliceProcessor.getPixel(x, y - 1) == targetPixel) {
                neighborPointSet.add(new Point3i(x, y - 1, homePoint.z));
            }
        }
        // Neighbors left and right (in this plane)
        if (x > 0) {
            if (sliceProcessor.getPixel(x - 1, y) == targetPixel) {
                neighborPointSet.add(new Point3i(x - 1, y, homePoint.z));
            }
        }
        if (x + 1 < imageWidth) {
            if (sliceProcessor.getPixel(x + 1, y) == targetPixel) {
                neighborPointSet.add(new Point3i(x + 1, y, homePoint.z));
            }
        }
        // Neighbors below (in this plane)
        if (y + 1 < imageHeight) {
            if (x + 1 < imageWidth) {
                if (sliceProcessor.getPixel(x + 1, y + 1) == targetPixel) {
                    neighborPointSet.add(new Point3i(x + 1, y + 1, homePoint.z));
                }
            }
            if (x - 1 >= 0) {
                if (sliceProcessor.getPixel(x - 1, y + 1) == targetPixel) {
                    neighborPointSet.add(new Point3i(x - 1, y + 1, homePoint.z));
                }
            }
            if (sliceProcessor.getPixel(x, y + 1) == targetPixel) {
                neighborPointSet.add(new Point3i(x, y + 1, homePoint.z));
            }
        }
        return neighborPointSet;
    }


    /**
     * Beginning at the startPoint, slabNeighbors will return *all* contiguous
     * pixels - not just the immediate neighbors.
     * 
     * @param startPoint
     * @return All contiguous neighboring points or empty set.
     */
    Set<Point3i> slabNeighbors(Point3i startPoint) {
        Set<Point3i> slabPoints = new HashSet<Point3i>();
        Queue<Point3i> pointQueue = new LinkedList<Point3i>();
        Point3i checkPoint = new Point3i(startPoint.x, startPoint.y, startPoint.z);

        while (null != checkPoint) {
            Set<Point3i> slabNeighbors = neighbors(checkPoint, 255);
            for (Point3i slabPoint : slabNeighbors) {
                if (slabPoints.add(slabPoint)) {
                    pointQueue.add(slabPoint);
                }
            }
            checkPoint = pointQueue.poll();
        }
        return slabPoints;
    }
}
