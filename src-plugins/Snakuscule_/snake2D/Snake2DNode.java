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

// Java 1.2
import java.awt.geom.Point2D;

/*====================================================================
|	Snake2DNode
\===================================================================*/

/*------------------------------------------------------------------*/
/*********************************************************************
 This class is used to store the snake-defining parameters. It extends
 the capabilities of the class <code>Point2D.Double</code> by
 additional state variables.
 @see Snake2D
 ********************************************************************/
public class Snake2DNode
	extends
		Point2D.Double

{ /* begin class Snake2DNode */

/*....................................................................
	public variables
....................................................................*/

/*********************************************************************
 Methods of the class <code>Snake2DKeeper</code> are allowed to
 modify <code>Snake2DNode</code> objects only if their
 <code>frozen</code> flag is set to <code>false</code>. 
 ********************************************************************/
public boolean frozen = false;

/*********************************************************************
 Methods of the class <code>Snake2DKeeper</code> are allowed to
 interactively display (as a cross) the nodes stored in
 <code>Snake2DNode</code> objects only if their <code>hidden</code>
 flag is set to <code>false</code>. 
 ********************************************************************/
public boolean hidden = false;

/*....................................................................
	constructor methods
....................................................................*/

/*------------------------------------------------------------------*/
/*********************************************************************
 This constructor builds a point that is initially neither frozen nor
 hidden.
 @param x The horizontal coordinate.
 @param y The vertical coordinate.
 ********************************************************************/
public Snake2DNode (
	final double x,
	final double y
) {
	super(x, y);
} /* end Snake2DNode */

/*------------------------------------------------------------------*/
/*********************************************************************
 This constructor builds a point with the given initial values.
 @param x The horizontal coordinate.
 @param y The vertical coordinate.
 @param frozen Set to <code>false</code> to allow methods of the class
 <code>Snake2DKeeper</code> to modify this point. Set to
 <code>true</code> otherwise.
 @param hidden Set to <code>false</code> to allow methods of the class
 <code>Snake2DKeeper</code> to display this point. Set to
 <code>true</code> otherwise.
 ********************************************************************/
public Snake2DNode (
	final double x,
	final double y,
	final boolean frozen,
	final boolean hidden
) {
	super(x, y);
	this.frozen = frozen;
	this.hidden = hidden;
} /* end Snake2DNode */

/*....................................................................
	Object methods
....................................................................*/

/*------------------------------------------------------------------*/
/*********************************************************************
 This method returns text-based information about this object.
 ********************************************************************/
public String toString (
) {
	return("[" + super.toString()
		+ ", frozen: " + frozen
		+ ", hidden: " + hidden
		+ "]"
	);
} /* end toString */

} /* end class Snake2DNode */
