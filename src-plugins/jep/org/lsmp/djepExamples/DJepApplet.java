/*
 * Created on 16-Jun-2003 by Rich webmaster@pfaf.org
 * www.comp.leeds.ac.uk/pfaf/lsmp
 *
 * Adapted from :
 */

/*****************************************************************************

JEP - Java Math Expression Parser 2.24
	  December 30 2002
	  (c) Copyright 2002, Nathan Funk
	  See LICENSE.txt for license information.

*****************************************************************************/

/**
 * Console - JEP Example Applet
 * Copyright (c) 2000 Nathan Funk
 *
 * @author Nathan Funk 
 */

package org.lsmp.djepExamples;
import org.lsmp.djep.djep.*;
import org.nfunk.jep.*;
import java.applet.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
* This class implements a simple command line utility for evaluating
* mathematical expressions.
*
*   Usage: java org.lsmp.djep.DJepConsole [expression]
*
* If an argument is passed, it is interpreted as an expression
* and evaluated. Otherwise, a prompt is printed, and the user can enter
* expressions to be evaluated. To exit from the command prompt a 'q' must
* be entered.
* typing 
* <pre>diff(x^2,x)</pre>
* will differentiate x^2 wrt 2. And
* <pre>eval(x^2,x,3)</pre> 
* will calculate x^2 at x=3.
* Expresions like
* <pre>eval(diff(diff(x^2+y^3,x),y),x,3,y,4)</pre>
* are also allowed.
*/
public class DJepApplet extends Applet implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2026659445630529741L;
	/**
	 * 
	 */
	/** Main JEP object */
	DJep j;	
	/** Input equation. */
	TextField inputTF;
	/** variable to differentiate wrt respect to. */
	TextField varTF;
	/** Output equation. */
	TextField outputTF;
	/** Button to perform differentiation. */
	Button but;
	
	/** Applet initialisation */
		
	public void init() 
	{
		initialise();
		setLayout(new GridLayout(3,2));
		inputTF = new TextField("sin(x^2)",50);
		outputTF = new TextField(50);
		outputTF.setEditable(false);
		varTF = new TextField("x",5);
		but = new Button("Calculate");
		but.addActionListener(this);
		inputTF.addActionListener(this);

		Panel p1 = new Panel();
		p1.add(new Label("Differentiate:"));
		p1.add(inputTF);
		add(p1);
		
		Panel p2 = new Panel();
		p2.add(new Label("with respect to:"));
		p2.add(varTF);
		p2.add(but);
		add(p2);
		
		Panel p3 = new Panel();
		p3.add(new Label("Result:"));
		p3.add(outputTF);
		add(p3);
	}
	
	/** Called when the Calculate button is pressed.
	 * Firsts differentiates the expresion in inputTF wrt variable in
	 * varTF, then simplifies it and puts results into outputTF.
	 */ 
	public void actionPerformed(ActionEvent e)
	{
			String command = inputTF.getText();
			j.parseExpression(command);
			if (j.hasError())
			{
				outputTF.setText(j.getErrorInfo());
			}
			else
			{
				// expression is OK, get the value
				try
				{
					Node diff = j.differentiate(j.getTopNode(),varTF.getText());
					Node simp = j.simplify(diff);
					if (j.hasError()) 
					{
						outputTF.setText(j.getErrorInfo());
					}
					else
						outputTF.setText(j.toString(simp));
				}
				catch(ParseException e1) { outputTF.setText("Parse Error: "+e1.getMessage()); }
				catch(IllegalArgumentException e2) { outputTF.setText(e2.getMessage()); }
				catch(Exception e3) { outputTF.setText(e3.getMessage()); }

				// did error occur during evaluation?
			}
		
	}
	
	/** Creates a new Console object and calls run() */
	public static void main(String args[]) {
		DJepApplet app = new DJepApplet();
		app.init();

		Frame mainFrame = new Frame("Wallpaper patterns");
		mainFrame.setBounds(0,0,200,200);
		mainFrame.add(app);
		mainFrame.show();
	}
	
	/** sets up all the needed objects. */
	public void initialise()
	{
		j = new DJep();
		j.addStandardConstants();
		j.addStandardFunctions();
		j.addComplex();
		j.setAllowUndeclared(true);
		j.setAllowAssignment(true);
		j.setImplicitMul(true);
		j.addStandardDiffRules();
		//j.setTraverse(true);
	}
	
	
	
}
