/*
 * Created on 31-May-2006
 */
package events;

import ij.ImagePlus;

public class SliceEvent{
	final ImagePlus source;
	public SliceEvent(ImagePlus source) {
		this.source = source;
	}
	
	public ImagePlus getSource(){
		return source;
	}
	
}
