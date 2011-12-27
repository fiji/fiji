/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2008 Mark Longair */

package util;

import ij.*;

import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Hashtable;

public class ModelessQuestions extends Dialog implements ActionListener {

	protected final static int LABEL     = 1;
	protected final static int CHECKBOX  = 2;
	protected final static int RADIO     = 3;
	protected final static int TEXTFIELD = 4;
		
	public class QuestionComponent {		
		int type;
		String group;
		String key;
		Component component;
		public QuestionComponent( int type, String group, String key, Component component ) {
			this.type = type;
			this.group = group;
			this.key = key;			
			this.component = component;
		}
	}

	ArrayList<QuestionComponent> components;

	ImagePlus imagePlus;

	public ModelessQuestions( String title, ImagePlus imagePlus ) {
		super( imagePlus.getWindow(), title, false /* i.e. modeless */ );
		completingButtons = new ArrayList<Button>();
		checkboxGroups = new Hashtable<String,CheckboxGroup>();
		components = new ArrayList<QuestionComponent>();
		this.imagePlus = imagePlus;
	}

	/*
	    You call this method to ask the user for some input about
	    the image, having set up a dialog previously with the
	    various add() methods.
	 */       
	public void waitForAnswers( ) {
		if( completingButtons.size() == 0 ) {
			IJ.error("You must add some buttons to the ModelessQuestions object before calling waitForAnswers");
			return;
		}
		layOutDialog();
		try {
			System.out.println("About to synchronize");
			synchronized (this) {
				System.out.println("About to show");
				// show();
				setVisible(true);
				System.out.println("About to wait");
				wait();
				System.out.println("Finished waiting");
			}
			System.out.println("After sychronized.");
		} catch( InterruptedException e ) {
		}
	}

	public void layOutDialog() {
		setLayout( new BorderLayout() );
		Panel questionPanel=new Panel();
		questionPanel.setLayout( new GridBagLayout() );
		GridBagConstraints c=new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.LINE_START;
		for( QuestionComponent q : components ) {
			switch (q.type) {
			case TEXTFIELD:
				c.gridx = 0;
				c.gridwidth = 1;
				c.anchor = GridBagConstraints.LINE_END;
				questionPanel.add( new Label(q.key), c );
				c.gridx = 1;
				c.anchor = GridBagConstraints.LINE_START;
				questionPanel.add( q.component, c );
				++ c.gridy;
				break;
			case RADIO:
			case LABEL:
			case CHECKBOX:
				c.gridx = 0;
				c.gridwidth = 2;	
				questionPanel.add( q.component, c);
				++ c.gridy;
				break;
			default:
				IJ.error("BUG: Unkown component type.");
			}
		}
		add( questionPanel, BorderLayout.CENTER );
		Panel buttonPanel = new Panel();
		buttonPanel.setLayout(new GridBagLayout());
		c = new GridBagConstraints();		
		c.gridx = 0;
		c.gridy = 0;
		for( Button b : completingButtons ) {			
			buttonPanel.add(b,c);
			c.gridx += 1;
		}
		add( buttonPanel, BorderLayout.SOUTH );
		pack();
		setVisible(true);
	}

	public void close() {
		System.out.println("In close(), about to synchronize");
		synchronized (this) {
			System.out.println("  In close(), about to notify()");
			notify();
		}
		System.out.println("In close(), after synchronized");
		setVisible(false);
		dispose();
	}

	public void addLabel( String text ) {
		Label l = new Label( text );
		QuestionComponent q = new QuestionComponent( LABEL,
							     null,
							     text,
							     l );
		components.add(q);
	}

	public void addTextField( String defaultText, int columns, String label ) {
		TextField tf = new TextField( defaultText,columns );
		QuestionComponent q = new QuestionComponent( TEXTFIELD,
							     null,
							     label,
							     tf );
		components.add(q);
	}

	Hashtable<String,CheckboxGroup> checkboxGroups;

	public void addRadio( String groupName, String label ) {
		if( ! checkboxGroups.containsKey(groupName) ) {
			checkboxGroups.put(groupName,new CheckboxGroup());
		}
		CheckboxGroup cbg = checkboxGroups.get( groupName );
		Checkbox cb = new Checkbox( label, false, cbg );
		QuestionComponent q = new QuestionComponent( RADIO,
							     groupName,
							     label,
							     cb );
		components.add(q);
	}

	public void addCheckbox( String label ) {
		Checkbox cb = new Checkbox( label, false );
		QuestionComponent q = new QuestionComponent( CHECKBOX,
							     null,
							     label,
							     cb );
		components.add(q);
	}

	ArrayList<Button> completingButtons;

	public void addCompletingButton( String key, String text ) {
		Button newButton = new Button(text);
		newButton.addActionListener(this);
		completingButtons.add(newButton);
	}
	
	int buttonPressed = -1;

	public void actionPerformed( ActionEvent e ) {
		/* We should only get actions from the completing
		   buttons. */
		Object source = e.getSource();
		for( int i = 0; i < completingButtons.size(); ++i ) {
			if( source == completingButtons.get(i) )
				buttonPressed = i;
		}
		close();
	}

	public int getCompletingButtonIndex() {
		return buttonPressed;
	}

}
