/*
 * Created on 31-May-2006
 */
package events;

import ij.ImagePlus;

public class RoiEvent {
	final ImagePlus source;
	
	public RoiEvent(ImagePlus source){
		this.source = source;
	}
	
	public ImagePlus getSource(){
		return source;
	}
}
