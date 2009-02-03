// $Revision$, $Date$, $Author$

package levelsets.algorithm;

import ij.IJ;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import levelsets.ij.ImageContainer;
import levelsets.ij.ImageProgressContainer;
import levelsets.ij.StateContainer;

/**
 * Implementation of the Sparse Field Levelset algorithm.
 */
public class SparseFieldLevelSet implements StagedAlgorithm
{
   // Values of the Levelset equation per voxel
   private DeferredDoubleArray3D phi = null;
   // Coordinate->element lookup table for BandElements to avoid linear searches
   private DeferredObjectArray3D<BandElement> elementLUT = null;
   
   /* Holds mean curvature per slice to be used in the next step, useless if
    * topology is not detected in every step?
    */ 
   //private double [] global_curvatures = null;
   
   /* Holds the state of the voxel
    * 0 for zero layer,  +1 for next outside layer, -1 for next inside layer
    * and so on
    */
   private int [][][] state = null;
   
   // constant for areas inside the zero level set contour */
   public static int INSIDE = -1;
   // constant for areas inside the zero level set contour */
   public static int OUTSIDE = 1;
   
   // Tag for zero level voxel state
   public static int STATE_ZERO = 0;
   // Tag for voxels far (not part of a layer) inside the contour
   public static int INSIDE_FAR = Integer.MIN_VALUE;
   // Tag for voxels far (not part of a layer) inside the contour
   public static int OUTSIDE_FAR = Integer.MAX_VALUE;
   
   // Holds actions that will be performed on voxels after when updating
   private DeferredIntArray3D action = null;
   // No action scheduled yet
   private static int NO_ACTION = 0;
   // Scheduled for layer change
   private static int CHANGE_LAYER = 2;
   // Active voxel scheduled for inside movement
   private static int ACTIVE_OUTSIDE = OUTSIDE;
   // Active voxel scheduled for inde
   private static int ACTIVE_INSIDE = INSIDE;
   // Tag to signal, that a voxel should NOT be moved to another layer
   private static int NO_DRAG = Integer.MAX_VALUE - 1;
   
   // Threshold around zero crossing for the levelset function for active set 
   private static double PHI_THRESHOLD = 0.5d;
   
   private double [][][] gradients = null;
   
   // The source image
   private ImageContainer source = null;
   // Working copy of the source that will be filtered   
   private ImageContainer img = null;
   // Progress image
   private ImageProgressContainer progress = null;
      
   private static int NUM_LAYERS = 2;
   /* as zero layer is used to address the array it is assigned numlayers here!
    * not num_layers + 1 which would be more logical
    */
   private static int ZERO_LAYER = NUM_LAYERS;
   
   // Initial size of the layer lists
   private static int INITIAL_LISTSIZE = 500;
   
   // List that holds the layer lists
   private ArrayList[] layers = new ArrayList[2 * NUM_LAYERS + 1];
   
   // Lists for iterative layer change procedure
   private ArrayList<BandElement> outside_list = new ArrayList<BandElement>(INITIAL_LISTSIZE);
   private ArrayList<BandElement> inside_list = new ArrayList<BandElement>(INITIAL_LISTSIZE);
   private ArrayList<BandElement> result_outside_list = new ArrayList<BandElement>(INITIAL_LISTSIZE);
   private ArrayList<BandElement> result_inside_list = new ArrayList<BandElement>(INITIAL_LISTSIZE);
   
   // List for voxel that are scheduled for Phi value update
   private ArrayList<BandElement> update_list = new ArrayList<BandElement>(INITIAL_LISTSIZE);
   
   // Cache for BandElement objects to avoid continuous reallocation
   private BandElementCache elem_cache = null;
   // Size of that cache (number of elements)
   private static int ELEMENT_CACHE_SIZE = 10000;
   
   // Reference to the Fast Marching stage that was run before to get the start contour
   // private FastMarching fm = null;
   // No link to previous stage. Instead: The initial contour as independent container
   private StateContainer init_state = null;
   // Tag to signal wheter initialization is needed
   private boolean needInit = true;
   // Tag to signal whether the level set function has converged
   private boolean convergence = false;
   // Tag to signal if a problem was encountered which prevents more iterations
   private boolean invalid = false;
   
   // Mean grey value around seed points - taken from Fast Marching
   private int seed_greyvalue = 0;
   // If the  grey value wasn't provided, where do we take it from - zero set or inside set
   boolean seed_grey_zero = true;
   
   // Absolute sum of phi value changes in updated voxels
   private double total_change = 0;
   // Number of updated voxels
   private int num_updated = 0;
   
   // preallocate
   int [] pixel = new int[4];
   
   // Control constants for level set evolution equation
   // Power of the advection force - expands contour along surface normals
   private static double ADVECTION_FORCE = 2.2;
   // Power of regulatory curvature term
   private static double CURVATURE_EPSILON = 1;
   // Time step for numerical solution
   private static double DELTA_T = 1d/6d * 1d/(CURVATURE_EPSILON * ADVECTION_FORCE);
   // Mean change per updated pixel threshold for convergence
   private static double CONVERGENCE_WEIGHT = 0.005;
   private static double CONVERGENCE_FACTOR;
   
   // Scaling factor for the slice spacing to pixel spacing ratio  
   private double zScale = 0;
   
   // verbosity of log output
   int verbose = 0;
   
   /**
    * Creates a new instance of LevelSet
    * @param image The input image
    * @param fm The Fast Marching stage that was run before
    * @param gradients Image gradients?
    */
   public SparseFieldLevelSet(ImageContainer image, ImageProgressContainer img_progress, StateContainer init_state)
   {
      // this.fm = fm;
	  this.init_state = init_state;
      this.source = image;
      this.progress = img_progress;
      zScale = this.source.getzScale();
      needInit = true;
   }
   
   /**
    * Returns the state map
    * @return The state map
    */
   public int[][][] getStateMap()
   {
      return state;
   }
   
   /**
    * Returns the state map as StateContainer
    * @return The StateContainer
    */
   public StateContainer getStateContainer()
   {
	  if (invalid) {
		  return null;
	  }
	   
	  StateContainer sc = new StateContainer();
	  sc.setSparseField(state);
      return sc;
   }

   /**
    * Sets the Advection weight.
    * Works only before the initialization (i.e. the first iteration)
    */
   public void setAdvectionWeight(double w) {
	   if ( needInit )
		   ADVECTION_FORCE = w;
   }
   
   /**
    * Returns the Advection weight.
    * @return The Advection weight
    */
   public static double getAdvectionWeight() {
	   return ADVECTION_FORCE;
   }

   /**
    * Sets the Curvature weight.
    * Works only before the initialization (i.e. the first iteration)
    */
   public void setCurvatureWeight(double w) {
	   if ( needInit )
		   CURVATURE_EPSILON = w;
   }
   
   /**
    * Returns the Curvature weight.
    * @return The Curvature weight
    */
   public static double getCurvatureWeight() {
	   return CURVATURE_EPSILON;
   }

   /**
    * Sets the Convergence factor
    * Careful with changing that. Convergence is set relative to the time change constant.
    * Works only before the initialization (i.e. the first iteration)
    */
   public void setConvergenceFactor(double f) {
	   if ( needInit )
		   CONVERGENCE_WEIGHT = f;
   }
   
   /**
    * Returns the Convergence factor.
    * @return The Convergence factor
    */
   public static double getConvergenceFactor() {
	   return CONVERGENCE_WEIGHT;
   }
  
   
 
   
   private void init()
   {
      phi = new DeferredDoubleArray3D(source.getWidth(), source.getHeight(), source.getImageCount(), 5 , 0);
      state = new int[source.getWidth()][source.getHeight()][source.getImageCount()];
      action = new DeferredIntArray3D(source.getWidth(), source.getHeight(), source.getImageCount(), 5, 0);
      elementLUT = new DeferredObjectArray3D<BandElement>(source.getWidth(), source.getHeight(), source.getImageCount(), 5, null);
      
      //        global_curvatures = new double[img.getImageCount()];
      
      elem_cache = new BandElementCache(ELEMENT_CACHE_SIZE);
      
      // the convergence factor may have changed over the ui
      CONVERGENCE_FACTOR = (CONVERGENCE_WEIGHT * DELTA_T);
      
      // Create layer lists
      for (int i = 0; i < (NUM_LAYERS * 2 + 1); i++)
      {
         layers[i] = new ArrayList(INITIAL_LISTSIZE);
      }
      
      // this.seed_greyvalue = fm.getSeedGreyValue();
      this.seed_greyvalue = init_state.getZeroGreyValue();
      
      this.img = source.deepCopy();
      
      // this.img.applyFilter(new MedianFilter(2));
      // this.img.applyFilter(new GreyValueErosion(MorphologicalOperator.getTrueMask(3, 3)));
      gradients = this.img.calculateGradients();
      
      createActiveLayer();
      // this.fm = null;
      this.init_state = null;
      
      // This may be using a lot of memory - lets make sure to clean up before we get serious
      System.gc();
      
      createInactiveLayers();
      checkConsistency();
      visualize(true); // update visualization only when we start
      
      needInit = false;
      
      IJ.log("Sparse field done init");
      IJ.log("Delta t = " + DELTA_T);
      //      System.exit(0);
   }
   
   /**
    * // See interface defintion for javadoc
    * granularity is the max number of iterations that are executed before it returns
    * returns true if no convergence reached (makes sense to continue), false if convergence has been reached
    */
   public boolean step(int granularity)
   {
	  if (invalid) {
		  return false;
	  }
	   
      if (needInit)
      {
         init();
      }
      
      for (int i = 0; i < granularity; i++)
      {
         assert(outside_list.size() == 0 && inside_list.size() == 0 && result_inside_list.size() == 0 && result_outside_list.size() == 0 );
         assert (layers[0].size() != 0  && layers[1].size() != 0  && layers[2].size() != 0 && layers[3].size() != 0 && layers[4].size() != 0);
         
         iterate();
         //convergence = true;
         if (convergence)
         {
            IJ.log("Converged!");
            break;
         }
         
//         checkConsistency();
      }
      
      visualize(true);
      if (convergence)
      {
    	 // TODO: create mask
         // dumpStateMap("d:\\temp\\ls_output.txt");
         cleanup();
      }
      
      // If the contour goes haywire, total_change (sum of all phi) gets NaN values
      // No point to continue, abort and tell the user about the fact
      if ( Double.isNaN(total_change) ) {
    	  invalid = true;
    	  IJ.error("Level Sets encountered numerical instability (i.e. the contour probably expanded to infinity) - Aborted");
    	  return(false);
      }
      
      return (!convergence);
   }
   
   private void visualize(boolean set_output )
   {
	   
	  if ( progress == null ) {
		  return; 
	  }
	   
      IJ.log("Change was " + (total_change / (layers[ZERO_LAYER].size())));
      // Just for debugging: find out details so that we can catch numerical instability
      // IJ.log("Change splits into total change=" + total_change + ", ZERO level layers" + layers[ZERO_LAYER].size());
      if ( verbose > 0 ) {
    	  IJ.log("Stats: ");
    	  checkConsistency();
    	  //        inImg.copyData(img.getRaster());
    	  //            gradient_image.copyData(img.getRaster());
    	  //            Graphics2D g2d = (Graphics2D)img.getGraphics();
    	  //            g2d.clearRect(0, 0, img.getWidth() - 1, img.getHeight() - 1);
      }
      ImageContainer output = null;
      if (convergence)
      {
         // output = source.deepCopy();
         progress.duplicateImages(source);
      }
      else if ( set_output == true )
      {
         // output = img.deepCopy();
         progress.duplicateImages(img);
      }
      
      drawLayers(ZERO_LAYER, ZERO_LAYER + 1);
      
      progress.showProgressStep();
   }
   
   private void iterate()
   {
      // Update voxels in the active layer
      convergence = updateActiveLayer();
      
      // Process voxels that move out of the active layer
      processLayerChangeList(inside_list, INSIDE, result_inside_list, OUTSIDE);
      processLayerChangeList(outside_list, OUTSIDE, result_outside_list, INSIDE);
      
      // Process voxels that move into/towards the active layer (except voxels in the outermost layers)   
      for (int i = 1; i < NUM_LAYERS; i++)
      {
         swapLists();
         
         processLayerChangeList(inside_list, (i * OUTSIDE) + INSIDE, result_inside_list, (i * OUTSIDE) + OUTSIDE);
         processLayerChangeList(outside_list, (i * INSIDE) + OUTSIDE, result_outside_list, (i * INSIDE) + INSIDE);
      }
      
      swapLists();
      
      // Process voxels in the outermost layers that move towards the active layer
      processLayerChangeList(inside_list, (NUM_LAYERS * OUTSIDE) + INSIDE, result_inside_list, OUTSIDE_FAR);
      processLayerChangeList(outside_list, (NUM_LAYERS * INSIDE) + OUTSIDE, result_outside_list, INSIDE_FAR);
      
      swapLists();
      
      // Finally move voxels that are far into the outermost layers if necessary
      processLayerChangeList(inside_list, NUM_LAYERS * OUTSIDE, result_inside_list, NO_DRAG);
      processLayerChangeList(outside_list, NUM_LAYERS * INSIDE, result_outside_list, NO_DRAG);
      
      // Update the inactive layers around the active set
      for (int i = 1; i <= NUM_LAYERS; i++)
      {
         updateInactiveLayer(ZERO_LAYER - i);
         updateInactiveLayer(ZERO_LAYER + i);
      }
      
   }
   
   /* Swaps the list references between input and output lists. This is done
    * because updating the inactive layers is an iterative process where a 
    * processed layer`s output (the "drag list") is used as input for the next 
    * outer layer. See method processLayerChangeList()
    */
   private void swapLists()
   {
      ArrayList<BandElement> swap = null;
      swap = outside_list;
      outside_list = result_outside_list;
      result_outside_list = swap;
      
      swap = inside_list;
      inside_list = result_inside_list;
      result_inside_list = swap;
   }
   
   /* Calculates delta Phi for all voxels in the active layer. Then updates
    * all voxels that remain in the active layer and the voxels that will move 
    * into it (some neighbours of voxels moving out). The actual updates are 
    * delayed until all voxels have been calculated to avoid influencing the
    * result by using neighbour voxels with already updated values
    */
   private boolean updateActiveLayer()
   {
      assert (update_list.size() == 0);
      
      //double total_change = 0;
      total_change = 0;
      num_updated = 0;
      
      Iterator<BandElement> it = layers[ZERO_LAYER].iterator();
      while (it.hasNext())
      {
         BandElement elem = it.next();
         int x = elem.getX();
         int y = elem.getY();
         int z = elem.getZ();
         
         double image_term = getImageTerm(x, y, z);
         
         //         if (image_term < (CONVERGENCE_FACTOR / 2)) continue;
         
         double curvature = getCurvatureTerm(x, y, z);
         double advection = getAdvectionTerm(x, y, z);
         
         //image_term = 0;           else num_updated++;
         
         // calculate net change
         double delta_phi = - DELTA_T * image_term *
                 (advection * ADVECTION_FORCE + curvature * CURVATURE_EPSILON);
         
         // add absolute value of the net change of this voxel to the total change
         total_change += Math.abs(delta_phi);
         num_updated++;
         
         // Calculate Phi value of the voxel after an executed update
         double temp_phi = phi.get(x, y, z) + delta_phi;
         
         if (temp_phi < INSIDE * PHI_THRESHOLD)
         {
            // Will move to the inside of the active set
            
            if (zeroLayerNeighbourMovement(x, y, z, ACTIVE_OUTSIDE))
            {
               //System.out.println("Called - zero layer neighbour movement");
               continue;
            }
            
            /* this node will move inside, so update outside neigbours as they will move
             into the zero set
             */
            updateZeroLayerNeighbours(x, y, z, ZERO_LAYER + OUTSIDE, temp_phi, update_list);
            
            it.remove();
            inside_list.add(elem);
            action.set(x, y, z, ACTIVE_INSIDE);
         }
         else if (temp_phi > OUTSIDE * PHI_THRESHOLD)
         {
            // Will move to the outside of the active set
            
            if (zeroLayerNeighbourMovement(x, y, z, ACTIVE_INSIDE))
            {
               //System.out.println("Called - zero layer neighbour movement");
               continue;
            }
            
            /* this node will move outside, so update inside neigbours as they will move
             into the zero set
             */
            updateZeroLayerNeighbours(x, y, z, ZERO_LAYER + INSIDE, temp_phi, update_list);
            
            it.remove();
            outside_list.add(elem);
            action.set(x, y, z, ACTIVE_OUTSIDE);
         }
         else
         {
            // stays in active set, schedule for update
            elem.setValue(temp_phi);
            update_list.add(elem);
         }
      }
      
      // All calculations are done, it is safe to do the updates now
      it = update_list.iterator();
      while (it.hasNext())
      {
         BandElement elem = it.next();
         // was queued more than once, only update one time so continue
         if (elem.getValue() == Double.MAX_VALUE) continue;
         
         // set the updated phi value
         phi.set(elem.getX(), elem.getY(), elem.getZ(), elem.getValue());         
         // tag the element so it is not updated again (would be expensive)
         elem.setValue(Double.MAX_VALUE);
      }
      
      // check for convergence
      if ((total_change / num_updated) < CONVERGENCE_FACTOR)
      {
         return true;
      }
      else return false; 
   }
   
   /* Updates inactives layer voxels. For the update the neighbour in the next 
    * inner layer nearest to the zero level set is located and then the voxel
    * value is updated to be that value plus distance (city block)
    */
   private void updateInactiveLayer(int layer)
   {
      int delta_phi = (layer < ZERO_LAYER) ? INSIDE * 1 : OUTSIDE * 1;
      
      Iterator<BandElement> it = layers[layer].iterator();
      while (it.hasNext())
      {
         BandElement elem = it.next();
         int x = elem.getX();
         int y = elem.getY();
         int z = elem.getZ();
         
         // Check if this is an orphaned element - remove if so
         if (state[x][y][z] != layer - NUM_LAYERS)
         {
            elem_cache.recycleBandElement(elem);
            it.remove();
            continue;
         }
         
         double value = checkNeighboursForUpdate(x, y, z, layer);
         // no neighbour found, demote the element into the next outer layer
         if (Math.abs(value) == Double.MAX_VALUE)
         {
            it.remove();
            
            // check if already in the outermost layers
            if (layer == 0)
            {
               state[x][y][z] = INSIDE_FAR;
               elementLUT.set(x, y, z, null);
               elem_cache.recycleBandElement(elem);
            }
            else if (layer == (layers.length - 1))
            {
               state[x][y][z] = OUTSIDE_FAR;
               elementLUT.set(x, y, z, null);
               elem_cache.recycleBandElement(elem);
            }
            // or push down a layer
            else
            {
               if (layer < ZERO_LAYER)
               {
                  layers[layer + INSIDE].add(elem);
                  state[x][y][z] = layer - NUM_LAYERS + INSIDE;
               }
               else
               {
                  layers[layer + OUTSIDE].add(elem);
                  state[x][y][z] = layer - NUM_LAYERS + OUTSIDE;
               }
            }
            
         }
         // neighbour in next inner layer found -> update
         else
         {
            phi.set(x, y, z, value + delta_phi);
         }
      }
   }
   
   /* Processes a "swap_list" that contains voxels that are moved into the layer
    * designated by "swap_to". Neighbours voxels of moved voxels from the layer
    * "drag_index" are accumulated in the "drag_list". Those voxels will change
    * layer in the next step. Therefore this methods output list ist the input
    * for the next call where the "drag_list" becomes the "swap_list".
    */
   private void processLayerChangeList(List<BandElement> swap_list, int swap_to, List<BandElement> drag_list, int drag_index)
   {
      // Step through the swap list
      Iterator<BandElement> it = swap_list.iterator();
      while (it.hasNext())
      {
         BandElement elem = it.next();
         it.remove();
         
         int elem_x = elem.getX();
         int elem_y = elem.getY();
         int elem_z = elem.getZ();
         
         layers[swap_to + NUM_LAYERS].add(elem);
         elementLUT.set(elem_x, elem_y, elem_z, elem);
         state[elem_x][elem_y][elem_z] = swap_to;
         action.set(elem_x, elem_y, elem_z, NO_ACTION);
         
         // If neighbours should be dragged behind the movement
         if (drag_index != NO_DRAG)
         {
            /* Step through neighbours and look for voxels with the appropriate
             * state (index) for dragging
             */
            Iterator<BandElement> neighbours = neighbourhood(elem_x, elem_y, elem_z);
            while (neighbours.hasNext())
            {
               BandElement neighbour = neighbours.next();
               int neighbour_x = neighbour.getX();
               int neighbour_y = neighbour.getY();
               int neighbour_z = neighbour.getZ();
               
               /* If this voxel is not scheduled for layer change yet, get a
                * BandElement object to represent this voxel and queue it in
                * the output list (drag_list)
                */
               if (state[neighbour_x][neighbour_y][neighbour_z] == drag_index
                       && action.get(neighbour_x, neighbour_y, neighbour_z) != CHANGE_LAYER)
               {
                  action.set(neighbour_x, neighbour_y, neighbour_z, CHANGE_LAYER);
                  BandElement dragged =
                          elem_cache.getRecycledBandElement(neighbour_x, neighbour_y, neighbour_z, Double.MAX_VALUE);
                  drag_list.add(dragged);
               }
            }
         }
      }
   }
   
   /* If during update of the active layer a voxel is found to get outside the
    * active sets Phi value threshold it will be moved out of the active layer
    * and some neighbours (opposite to the movement) will move into the
    * active layer on its stead. This method examines the neighbourhood, 
    * scheduling voxels for update that move into the active layer
    */ 
   private void updateZeroLayerNeighbours(int x, int y, int z, int layer,
           double temp_phi, List update_list)
   {
      Iterator<BandElement> neighbours = neighbourhood(x, y, z);
      while (neighbours.hasNext())
      {
         BandElement aNeighbour = neighbours.next();
         if (state[aNeighbour.getX()][aNeighbour.getY()][aNeighbour.getZ()] != (layer - NUM_LAYERS)) continue;
         
         BandElement elem = elementLUT.get(aNeighbour.getX(), aNeighbour.getY(), aNeighbour.getZ());
         int neighbour_x = elem.getX();
         int neighbour_y = elem.getY();
         int neighbour_z = elem.getZ();
         
         int side = (layer < ZERO_LAYER) ? INSIDE : OUTSIDE;
         double value = (elem.getValue() == Double.MAX_VALUE) ? (Double.MAX_VALUE * side) : elem.getValue();
         
         if (layer < ZERO_LAYER)
         {
            if (temp_phi + INSIDE > value)
            {
               elem.setValue(temp_phi + INSIDE);
               update_list.add(elem);
            }  
         }
         else
         {
            if (temp_phi + OUTSIDE < value)
            {
               elem.setValue(temp_phi + OUTSIDE);
               update_list.add(elem);
            }
         }
      }
   }
   
   private double checkNeighboursForUpdate(int x, int y, int z, int layer)
   {
      assert (state[x][y][z] == (layer - NUM_LAYERS));
      
      double value, trial_value;
      int from_layer;
      
      if (layer < ZERO_LAYER)
      {
         from_layer = layer + OUTSIDE;
         value = Double.MAX_VALUE * INSIDE;
      }
      else
      {
         from_layer = layer + INSIDE;
         value = Double.MAX_VALUE * OUTSIDE;
      }
      
      Iterator<BandElement> it = neighbourhood(x, y, z);
      while (it.hasNext())
      {
         BandElement elem = it.next();
         int elem_x = elem.getX();
         int elem_y = elem.getY();
         int elem_z = elem.getZ();
         
         // not a node in the next inner layer
         if (state[elem_x][elem_y][elem_z] != (from_layer - NUM_LAYERS))
         {
            continue;
         }
         
         trial_value = phi.get(elem_x, elem_y, elem_z);
         
         if (layer < ZERO_LAYER)
         {
            if (trial_value > value)
            {
               value = trial_value;
            }
         }
         else
         {
            if (trial_value < value)
            {
               value = trial_value;
            }
         }
      }
      
      return value;
   }
   
   private boolean zeroLayerNeighbourMovement(int x, int y, int z, int direction)
   {
      Iterator<BandElement> it = neighbourhood(x, y, z);
      while (it.hasNext())
      {
         BandElement elem = it.next();
         int elem_x = elem.getX();
         int elem_y = elem.getY();
         int elem_z = elem.getZ();
         
         // check if zero layer
         if (state[elem_x][elem_y][elem_z] != ZERO_LAYER)
         {
            continue;
         }
         
         if (action.get(elem_x, elem_y, elem_z) == direction)
         {
            return true;
         }
      }
      
      return false;
   }
   
   // upwind scheme
   private double getAdvectionTerm(int x, int y, int z)
   {
      double xB = (x > 0) ?
         phi.get(x - 1, y, z) : Double.MAX_VALUE;
      double xF = (x + 1 < phi.getXLength()) ?
         phi.get(x + 1, y, z) : Double.MAX_VALUE;
      double yB = (y > 0) ?
         phi.get(x, y - 1, z) : Double.MAX_VALUE;
      double yF = (y + 1 < phi.getYLength()) ?
         phi.get(x, y + 1, z) : Double.MAX_VALUE;
      double zB = (z > 0) ?
         phi.get(x, y, z - 1) : Double.MAX_VALUE;
      double zF = (z + 1 < phi.getZLength()) ?
         phi.get(x, y, z + 1) : Double.MAX_VALUE;
      
      double cell_phi = phi.get(x, y, z);
      
      double xBdiff = Math.max(cell_phi - xB, 0);
      double xFdiff = Math.min(xF - cell_phi, 0);
      double yBdiff = Math.max(cell_phi - yB, 0);
      double yFdiff = Math.min(yF - cell_phi, 0);
      double zBdiff = Math.max((cell_phi - zB) / zScale, 0);
      double zFdiff = Math.min((zF - cell_phi) / zScale, 0);
      
      return Math.sqrt(xBdiff * xBdiff + xFdiff * xFdiff +
              yBdiff * yBdiff + yFdiff * yFdiff +
              zBdiff * zBdiff + zFdiff * zFdiff);
   }
   
   // central differneces
   private double getCurvatureTerm(int x, int y, int z)
   {
      if (x == 0 || x >= (phi.getXLength() - 1)) return 0;
      if (y == 0 || y >= (phi.getYLength() - 1)) return 0;
      boolean curvature_3d = false; //((z > 0) && (z < phi.getZLength() - 1));
      
        /* access to the deferred array is costly, so avoid multiple queries
         for the same value and pre assign here
         */
      double cell_phi = phi.get(x, y, z);
      double phiXB = phi.get(x - 1, y, z);
      double phiXF = phi.get(x + 1, y, z);
      double phiYB = phi.get(x, y - 1, z);
      double phiYF = phi.get(x, y + 1, z);
      
      double phiX = (phiXF - phiXB) / 2;
      double phiY = (phiYF - phiYB) / 2;
      double phiXX = (phiXF + phiXB - (2 * cell_phi));
      double phiYY = (phiYF + phiYB - (2 * cell_phi));
      double phiXY = (phi.get(x + 1, y + 1, z) - phi.get(x + 1, y - 1, z) -
              phi.get(x - 1, y + 1, z) + phi.get(x - 1, y - 1, z)) / 4;
      
      double phiZ = 0, phiZZ = 0, phiXZ = 0, phiYZ = 0;
      if (curvature_3d)
      {
         double phiZB = phi.get(x, y, z - 1);
         double phiZF = phi.get(x, y, z + 1);
         phiZ = (phiZF - phiZB) / 2;
         phiZZ = (phiZF + phiZB - (2 * cell_phi));
         phiXZ = (phi.get(x + 1, y, z + 1) - phi.get(x + 1, y, z - 1) - phi.get(x - 1, y, z + 1) + phi.get(x - 1, y, z - 1)) / 4;
         phiYZ = (phi.get(x, y + 1, z + 1) - phi.get(x, y + 1, z - 1) - phi.get(x, y - 1, z + 1) + phi.get(x, y - 1, z - 1)) / 4;
      }
      
      if (phiX == 0 || phiY == 0) return 0;
      if (curvature_3d && phiZ == 0) return 0;
      
      double curvature = 0, deltaPhi = 0;
      if (curvature_3d)
      {
         deltaPhi = Math.sqrt(phiX * phiX + phiY * phiY + phiZ * phiZ);
         curvature = -1 * ((phiXX * (phiY * phiY + phiZ * phiZ)) +
                 (phiYY * (phiX * phiX + phiZ * phiZ)) +
                 (phiZZ * (phiX * phiX + phiY * phiY)) -
                 (2 * phiX * phiY * phiXY) -
                 (2 * phiX * phiZ * phiXZ) -
                 (2 * phiY * phiZ * phiYZ)) /
                 Math.pow(phiX * phiX + phiY * phiY + phiZ * phiZ, 3/2);
      }
      else
      {
         deltaPhi = Math.sqrt(phiX * phiX + phiY * phiY);
         curvature = -1 * ((phiXX * phiY * phiY) + (phiYY * phiX * phiX)
         - (2 * phiX * phiY * phiXY)) /
                 Math.pow(phiX * phiX + phiY * phiY, 3/2);
      }
      
      return curvature * deltaPhi;
   }
   
   private double getImageTerm(int x, int y, int z)
   {
      int greyval = img.getPixel(x, y, z);
      int greyval_penalty = Math.abs(greyval - this.seed_greyvalue);
      if (greyval_penalty < 30) greyval_penalty = 0;
      return (1 / (1 + ((gradients[x][y][z] + greyval_penalty) * 2)));
   }
   
   private void createInactiveLayers()
   {
      for (int i = 0; i <= (NUM_LAYERS - 1); i++)
      {
         Iterator<BandElement> it = layers[ZERO_LAYER + i * INSIDE].iterator();
         while (it.hasNext())
         {
            BandElement elem = it.next();
            
            Iterator<BandElement> neighbours = neighbourhood(elem.getX(), elem.getY(), elem.getZ());
            while (neighbours.hasNext())
            {
                addToLayerIfFar(neighbours.next(), ZERO_LAYER + i * INSIDE);
            }
         }
         
         if (i == 0) continue;
         
         it = layers[ZERO_LAYER + i * OUTSIDE].iterator();
         while (it.hasNext())
         {
            BandElement elem = it.next();
            
            Iterator<BandElement> neighbours = neighbourhood(elem.getX(), elem.getY(), elem.getZ());
            while (neighbours.hasNext())
            {
                addToLayerIfFar(neighbours.next(), ZERO_LAYER + i * OUTSIDE);
            }
         }
      }
   }
   
   private void addToLayerIfFar(BandElement element, int from_layer)
   {
      int x = element.getX(); 
      int y = element.getY(); 
      int z = element.getZ(); 
      
      if (state[x][y][z] == INSIDE_FAR)
      {
         BandElement elem = new BandElement(x, y, z, Double.MAX_VALUE);
         layers[from_layer + INSIDE].add(elem);
         elementLUT.set(x, y, z, elem);
         state[x][y][z] = from_layer - NUM_LAYERS + INSIDE;
      }
      else if (state[x][y][z] == OUTSIDE_FAR)
      {
         BandElement elem = new BandElement(x, y, z, Double.MAX_VALUE);
         layers[from_layer + OUTSIDE].add(elem);
         elementLUT.set(x, y, z, elem);
         state[x][y][z] = from_layer - NUM_LAYERS + OUTSIDE;
      }
      else return;
      
      // initialize layer values with distance transform on unity grid
      phi.set(x, y, z, state[x][y][z]);
   }
   
   private void drawLayers(int from, int to)
   {
      for (int i = from; i <= to; i++)
      {
         if (i == ZERO_LAYER)
         {
            pixel[0] = 255; pixel[1] = 0; pixel[2] = 0;
         }
         else
         {
            pixel[0] = 255;
            pixel[1] = 255 / (Math.abs(i - (ZERO_LAYER)));
            pixel[2] = 0;
         }
         
         Iterator<BandElement> it = layers[i].iterator();
         while (it.hasNext())
         {
            BandElement elem = it.next();
            progress.setPixel(elem.getX(), elem.getY(), elem.getZ(), pixel);
         }
      }
   }
   
   private void createActiveLayer()
   {
      // DeferredByteArray3D statemap = fm.getStateMap();
	  int px_zero = 0, px_inside = 0, px_outside = 0; 
	  int grey_zero = 0, grey_inside = 0;
	  
      DeferredObjectArray3D<StateContainer.States> statemap = init_state.getForSparseField();
      for (int x = 0; x < statemap.getXLength(); x++)
      {
         for (int y = 0; y < statemap.getYLength(); y++)
         {
            for (int z = 0; z < statemap.getZLength(); z++)
            {
               if (statemap.get(x, y, z) == StateContainer.States.ZERO )
               {
                  state[x][y][z] = STATE_ZERO;
                  phi.set(x, y, z, 0);
                  BandElement element = new BandElement(x, y, z, 0);
                  layers[ZERO_LAYER].add(element);
                  elementLUT.set(x, y, z, element);
                  px_zero++;
                  grey_zero += source.getPixel(x, y, z);
               }
               else if ( statemap.get(x, y, z) == StateContainer.States.INSIDE )
               {
                  state[x][y][z] = INSIDE_FAR;
                  px_inside++;
                  grey_inside += source.getPixel(x, y, z);
               }
               else
               {
                  state[x][y][z] = OUTSIDE_FAR;
                  px_outside++;
               }
            }
         }
      }
      IJ.log("From FastMarching: Found pixels " +px_zero+" ZERO, " +px_inside + " INSIDE,"+px_outside + " OUTSIDE");
      if ( px_inside == 0 && px_zero == 0 ) {
       	  invalid = true;
       	  IJ.error("Level Sets didn't get any starting shape - Aborting");
       }
      
      // TODO median would be better
      if ( this.seed_greyvalue < 0 ) {
    	  if (this.seed_grey_zero) {
    		  this.seed_greyvalue = grey_zero / px_zero;
    	  } else {
    		  this.seed_greyvalue = grey_inside / px_zero;
    	  }
    	  IJ.log("Grey seed not set - setting to " + this.seed_greyvalue);
      }
   }
   
   private boolean outOfRange(int x, int y, int z)
   {
      if (x < 0 || x > state.length - 1) return true;
      else if (y < 0 || y > state[0].length - 1) return true;
      else if (z < 0 || z > state[0][0].length - 1) return true;
      else return false;
   }
   
   private Iterator<BandElement> neighbourhood(int x, int y, int z)
   {
      // 2 neighbours per dimension, 3 dimensions
      ArrayList<BandElement> neighbourList = new ArrayList<BandElement>(6);
      if (!outOfRange(x - 1, y, z))
      {
         neighbourList.add(elem_cache.getRecycledBandElement(x - 1, y, z, Double.MAX_VALUE));
      }
      if (!outOfRange(x + 1, y, z))
      {
         neighbourList.add(elem_cache.getRecycledBandElement(x + 1, y, z, Double.MAX_VALUE));
      }
      if (!outOfRange(x, y - 1, z))
      {
         neighbourList.add(elem_cache.getRecycledBandElement(x, y - 1, z, Double.MAX_VALUE));
      }
      if (!outOfRange(x, y + 1, z))
      {
         neighbourList.add(elem_cache.getRecycledBandElement(x, y + 1, z, Double.MAX_VALUE));
      }
      if (!outOfRange(x, y, z - 1))
      {
         neighbourList.add(elem_cache.getRecycledBandElement(x, y, z - 1, Double.MAX_VALUE));
      }
      if (!outOfRange(x, y, z + 1))
      {
         neighbourList.add(elem_cache.getRecycledBandElement(x, y, z + 1, Double.MAX_VALUE));
      }
      
      return neighbourList.iterator();
   }
   
   private void checkConsistency()
   {
      for (int i = 0; i < (2 * NUM_LAYERS + 1); i++)
      {
         IJ.log("Layer " + (i - NUM_LAYERS) + " : " + layers[i].size() +" elements");
         Iterator<BandElement> it = layers[i].iterator();
         while (it.hasNext())
         {
            BandElement elem = it.next();
            if (state[elem.getX()][elem.getY()][elem.getZ()] != (i - NUM_LAYERS))
            {
            	IJ.log("*** Layer index mismatch!!! ***");
            	IJ.log("Layer = " + i);
            }
            double val = phi.get(elem.getX(), elem.getY(), elem.getZ());
            if ((val > 0 && i < ZERO_LAYER) || (val < 0 && i > ZERO_LAYER))
            {
            	IJ.log("*** Illegal PHI value !!! ***");
            	IJ.log("Layer = " + i);
            }
            if ((val > 0.55 && i == ZERO_LAYER) || (val < -0.55 && i == ZERO_LAYER))
            {
            	IJ.log("*** Illegal PHI value in ZERO Layer !!! ***");
            	IJ.log("Value = " + val);
            }
         }
      }
      
      IJ.log("-----------------------------------------------------");
   }
   
   private void cleanup()
   {
      this.elem_cache = null;
      this.phi = null;
      //      this.state = null;
      this.gradients = null;
      this.action = null;
      this.img = source = null;
      this.gradients = null;
      System.gc();
   }
   
   /**
    * Dums the statemap into a file
    * @param path Fully qualified filename of the output file
    */
   public void dumpStateMap(String path)
   {
      try
      {
         BufferedWriter out = new BufferedWriter(new FileWriter(new File(path)));
         
         out.write(state.length + " " + state[0].length + " " + state[0][0].length);
         out.newLine(); out.newLine();
         
         int val = 0;
         for (int z = 0; z < state[0][0].length; z++)
         {
            for (int y = 0; y < state[0].length; y++)
            {
               for (int x = 0; x < state.length; x++)
               {
                  val = state[x][y][z];
                  if (val == Integer.MAX_VALUE) val = 9;
                  else if (val == Integer.MIN_VALUE) val = -9;
                  out.write(val + " ");
               }
               out.newLine();
            }
            out.newLine();
         }
         out.close();
      }
      catch (IOException ioe)
      {
         ioe.printStackTrace();
      }
   }
}
