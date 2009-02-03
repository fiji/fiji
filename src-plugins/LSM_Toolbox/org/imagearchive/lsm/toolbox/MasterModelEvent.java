/*
 * Created on 16-Sep-2005
 */
package org.imagearchive.lsm.toolbox;

import java.util.EventObject;

/**
 * @author patrick
 */
public class MasterModelEvent extends EventObject {
	
	private Object model;
	private int event = 0;
	
	/**
	 * @param arg0
	 */
	
	public MasterModelEvent(Object source) {
		super(source);
		model = source;
	}
	/**
	 * @return Returns the event.
	 */
	public int getEvent() {
		return event;
	}
	/**
	 * @param event The event to set.
	 */
	public void setEvent(int event) {
		this.event = event;
	}
}
