/*====================================================================
| Version: November 27, 2007
\===================================================================*/

/*====================================================================
| Philippe Thevenaz
| EPFL/STI/IOA/LIB/BM.4.137
| Station 17
| CH-1015 Lausanne VD
| Switzerland
|
| phone (CET): +41(21)693.51.61
| fax: +41(21)693.37.01
| RFC-822: philippe.thevenaz@epfl.ch
| X-400: /C=ch/A=400net/P=switch/O=epfl/S=thevenaz/G=philippe/
| URL: http://bigwww.epfl.ch/
\===================================================================*/

package snake2D;

// snake2D
import snake2D.Snake2DNode;

//Java 1.2
import java.awt.geom.Point2D;

/*====================================================================
|	Snake2D
\===================================================================*/

/*********************************************************************
 This abstract class encapsulates the number-crunching aspect of
 snakes.
 @see Snake2DKeeper
 @see Snake2DNode
 ********************************************************************/
public interface Snake2D

{ /* begin interface Snake2D */

/*------------------------------------------------------------------*/
/*********************************************************************
 The purpose of this method is to compute the energy of the snake.
 This energy is usually made of three additive terms: 1) the image
 energy, which gives the driving force associated to the data; 2) the
 internal energy, which favors smoothness of the snake; and 3) the
 constraint energy, which incorporates a priori knowledge. This
 method is called repeatedly during the optimization of the snake. It
 is <b>imperative</b> that this function be
 <b>everywhere differentiable</b> with respect to the snake-defining
 nodes.
 @return Return a number that should attain a minimal value when the
 snake is optimal. Negative values are admissible.
 ********************************************************************/
public double energy (
);

/*------------------------------------------------------------------*/
/*********************************************************************
 The purpose of this method is to compute the gradient of the snake
 energy with respect to the snake-defining nodes. This method is
 called repeatedly during the optimization of the snake. The
 optimization takes place under the control of the method
 <code>Snake2DKeeper.interactAndOtimize()</code>.
 @return Return an array that contains the gradient values associated
 to each node. They predict the variation of the energy for a
 horizontal or vertical displacement of one pixel. The ordering of the
 nodes must follow that of <code>getNodes()</code>. If
 <code>null</code> is returned, the optimizer within the class
 <code>Snake2DKeeper</code> will attempt to estimate the gradient by
 a finite-difference approach.
 @see #getNodes
 ********************************************************************/
public Point2D.Double[] getEnergyGradient (
);

/*------------------------------------------------------------------*/
/*********************************************************************
 This method provides an accessor to the snake-defining nodes.
 @return Return an array of subpixel node locations. It is expected
 that the ordering of the nodes and the number of nodes does not
 change during the lifetime of the snake.
 @see #setNodes
 ********************************************************************/
public Snake2DNode[] getNodes (
);

/*------------------------------------------------------------------*/
/*********************************************************************
 The purpose of this method is to detemine what to draw on screen,
 given the current configuration of nodes. This method is called
 repeatedly during the user interaction provided by the method
 <code>Snake2DKeeper.interactAndOptimize()</code>. The origin of
 coordinates lies at the top-left corner of the
 <code>display</code> parameter. Collectively, the array of scales
 forms the skin of the snake.
 @return Return an array of <code>Snake2DScale</code> objects.
 Straight lines will be drawn between the apices of each polygon, in
 the specified color. It is not necessary to maintain a constant
 number of polygons in the array, or a constant number of apices in a
 given polygon.
 @see Snake2DScale
 ********************************************************************/
public Snake2DScale[] getScales (
);

/*------------------------------------------------------------------*/
/*********************************************************************
 This method provides a mutator to the snake-defining nodes. It will
 be called repeatedly by the method
 <code>Snake2DKeeper.interactAndOptimize()</code>.
 @param node Array of subpixel node locations.
 @see #getNodes
 ********************************************************************/
public void setNodes (
	Snake2DNode[] node
);

} /* end interface Snake2D */
