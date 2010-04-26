/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

package org.nfunk.jepexamples;

import java.awt.*;
import java.util.*;

import org.nfunk.jep.*;
import org.nfunk.jep.type.*;
import org.nfunk.jep.evaluation.*;

/**
* This class performs the drawing of the fractal.
*/
public class FractalCanvas extends Canvas {
	private static final long serialVersionUID = -593341831485283712L;

	private int scaleX, scaleY;

	private Dimension dimensions;
	
	private int iterations, nEvals;

	private boolean hasError;

	private JEP myParser;
	private ExpressionCompiler expressionCompiler;
	private CommandEvaluator          evaluator;
	private CommandElement[]          commands;
	private SymbolTable        symTab;
	
	private java.awt.TextField exprField;

	/**
	* Constructor. 
	*/
	public FractalCanvas(String initialExpression, java.awt.TextField exprField_in) {
		iterations = 20;
		nEvals = 0;
		scaleX = 100;
		scaleY = 100;
		dimensions = getSize();
		hasError = true;
		exprField = exprField_in;
		initParser(initialExpression);
		expressionCompiler = new ExpressionCompiler();
		evaluator = new CommandEvaluator();
		symTab = myParser.getSymbolTable();
	}

	/**
	* Initializes the parser
	*/
	private void initParser(String initialExpression) {
		//Init Parser
		myParser = new JEP();

		//Load the standard functions
		myParser.addStandardFunctions();

		//Load the standard
		myParser.addStandardConstants();
		myParser.addComplex();	

		//Add and initialize z to (0,0)
		myParser.addVariable("z",0,0);
		myParser.addVariable("c",0,0);

		setExpressionString(initialExpression);
	}

	/**
	* Parses a new expression
	*/
	public void setExpressionString(String newString) {
		nEvals = 0;

		//Parse the new expression
		myParser.parseExpression(newString);

		//Find out whether there was an error in the expression
		hasError = myParser.hasError();
		if (hasError)
		  exprField.setForeground(Color.red);
		else
		  exprField.setForeground(Color.black);

	}

	public void setIterations(int iterations_in) {
		iterations = iterations_in;
	}

	private void paintWhite(Graphics g) {
		g.setColor(Color.white);
		g.fillRect(0,0,dimensions.width,dimensions.height);
	}


	private void paintFractalSansJEP(Graphics g) {
		System.out.println("paintFractalSansJEP()");

		//paintRegion(g, 0,0,256,256,0,8);

		System.out.println("done.");
	}
	
	private void paintRegion(Graphics g, int x, int y,
							int width, int height, int depth,
							int depth_max) {
		double re, im, p, q, resq, imsq, imtemp;
		int count;

		if (depth == depth_max) {
			p = (double)(x+width/2-230)/scaleX;
			q = (double)(y+height/2-150)/scaleY;
			count = 0;
			re = 0;
			im = 0;
			resq = 0;
			imsq = 0;

			while ((count < iterations) && ((resq + imsq) < 4.0)) {
				imtemp = 2 * re * im;
				re = resq - imsq + p;
				im = imtemp + q;
				count++;
				resq = re * re;
				imsq = im * im;
				nEvals++;
			}
			//System.out.println("At: " + x + ", " + y + ": " + count + " "+ result);
			if (count != iterations) {
				g.setColor(new Color(0, 0, (int)(255.0*(Math.sqrt(count)/Math.sqrt(iterations)))));
				g.fillRect(x, y, width, height);
			}

		} else {
			paintRegion(g,           x,            y, width/2, height - height/2, depth+1, depth_max);
			paintRegion(g, x + width/2,            y, width - width/2, height/2, depth+1, depth_max);
			paintRegion(g,           x, y + height/2, width/2, height - height/2, depth+1, depth_max);
			paintRegion(g, x + width/2, y + height/2, width - width/2, height - height/2, depth+1, depth_max);
		}
	}

	private void paintFractal(Graphics g) {
		Complex z,c,temp;
		int count;
		
		c = myParser.addVariable("c", 0, 0);
		z = myParser.addVariable("z", 0, 0);

		for (int x = 0; x <= (dimensions.width-1); x++) {
			for (int y = 0; y <= (dimensions.height-1); y++) {
				count = 0;
				c.set((double)(x-230)/scaleX,
				      (double)(y-150)/scaleY);
				z.set(0,0);
				
				while ((count < iterations) && (z.abs2() < 4.0)) {
					z.set(myParser.getComplexValue());
					count++;
					nEvals++;
				}

				if (count != iterations) {
					g.setColor(new Color(0, 0, (int)(255.0*(Math.sqrt(count)/Math.sqrt(iterations)))));
					g.fillRect(x, y, 1, 1);
				}
			}
		}
	}

	private void paintFractalWithCompiler(Graphics g) {
		Complex z,c,temp;
		int count;
		
		c = myParser.addVariable("c", 0, 0);
		z = myParser.addVariable("z", 0, 0);
		try {
			commands = expressionCompiler.compile(myParser.getTopNode());
		} catch (ParseException e) {
			System.out.println("Failed to compile expression");
			e.printStackTrace();
		}

		for (int x = 0; x <= (dimensions.width-1); x++) {
			for (int y = 0; y <= (dimensions.height-1); y++) {
				count = 0;
				c.set((double)(x-230)/scaleX,
					  (double)(y-150)/scaleY);
				z.set(0,0);
				
				while ((count < iterations) && (z.abs2() < 4.0)) {
					try {
						temp = (Complex)evaluator.evaluate(commands, symTab);
						z.set(temp);
					} catch (Exception e) {
						//System.out.println(e.toString());
						e.printStackTrace();
					}
					count++;
					nEvals++;
				}

				if (count != iterations) {
					g.setColor(new Color(0, 0, (int)(255.0*(Math.sqrt(count)/Math.sqrt(iterations)))));
					g.fillRect(x, y, 1, 1);
				}
			}
		}
	}

	private void paintNonJEPFractal(Graphics g) {
		double re, im, p, q, resq, imsq, imtemp;
		int count;

		for (int x = 0; x <= (dimensions.width-1); x++) {
			for (int y = 0; y <= (dimensions.height-1); y++) {
				p = (double)(x-230)/scaleX;
				q = (double)(y-150)/scaleY;
				count = 0;
				re = 0;
				im = 0;
				resq = 0;
				imsq = 0;
	
				while ( (count < iterations) && ((resq + imsq) < 4.0) ) {
					imtemp = 2 * re * im;
					re = resq - imsq + p;
					im = imtemp + q;
					resq = re * re;
					imsq = im * im;
					count++;
					nEvals++;
				}
				//System.out.println("At: " + x + ", " + y + ": " + count + " "+ result);
				if (count != iterations) {
					g.setColor(new Color(0, 0, (int)(255.0*(Math.sqrt(count)/Math.sqrt(iterations)))));
					g.fillRect(x, y, 1, 1);
				}
			}
		}
	}

	public void paint(Graphics g) {
		Date start, finish;

		dimensions = getSize();
		paintWhite(g);
		if (!hasError) {
			System.out.println("Painting... ");
			start = new Date();
			nEvals = 0;
			//paintFractal(g);
			paintFractalWithCompiler(g);
			//paintNonJEPFractal(g);
			finish = new Date();
			System.out.print("done. sec/eval: ");
			double seconds =  ( finish.getTime() - start.getTime() ) / 1000.0;
			System.out.println(seconds/nEvals);
		}
/*
		if (!initializedBuffer)
		{
			buffer = createImage(dimensions.width, dimensions.height);
			initializedBuffer = true;
		}

		Graphics buffergc = buffer.getGraphics();

		if (changedFunction)
		{
			paintWhite(buffergc);
			if (!hasError) paintFractal(buffergc);
			g.drawImage(buffer, 0, 0, null);
			changedFunction = false;
		}
		else
		{
			g.drawImage(buffer, 0, 0, null);
		}*/
	}
}
