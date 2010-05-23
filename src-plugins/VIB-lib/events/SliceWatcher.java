/*
 * Created on 31-May-2006
 */
package events;

import ij.ImageListener;
import ij.ImagePlus;

import java.util.ArrayList;

/**
 * enclosing class is just an adapter,
 * the main functionality is hidden in properties of the IP
 * it allows the user a space to add SliceListeners
 * @author s0570397
 *
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SliceWatcher{
	
	public static final String MONITOR_LOC = "SliceWatcher.monitor";
	
	final ImagePlus data;
	
	public SliceWatcher(ImagePlus data){
		this.data=data;
		if(data.getProperty(MONITOR_LOC) == null){
			data.setProperty(MONITOR_LOC, new SliceMonitor(data));
		}
		
		
	}
	
	private SliceMonitor getMonitor(){
		return (SliceMonitor)data.getProperty(MONITOR_LOC);
	}
	
	public void addSliceListener(SliceListener l){
		getMonitor().addSliceListener(l);
	}
	
	public boolean removeSliceListener(SliceListener l){
		return getMonitor().removeSliceListener(l);
	}
	
	/**
	 * this class is nestled in the properties of an IP
	 * it used to work by polling until it was found that the imageUpdate() method is called
	 * when the sliceNumber is changed
	 * @author s0570397
	 *
	 * 
	 * Window - Preferences - Java - Code Style - Code Templates
	 */
	private static class SliceMonitor implements /*Runnable,*/ ImageListener{
		final ImagePlus ip;
		final ArrayList<SliceListener> listeners = new ArrayList<SliceListener>();
		
		int lastSlicenumber;
		
		public SliceMonitor(ImagePlus ip){
			this.ip = ip;
			
			ImagePlus.addImageListener(this);
		}
		
		
		public void fireSliceNumberChange(SliceEvent e){
			for(SliceListener l:listeners){
				try{
					l.sliceNumberChanged(e);
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}		
		}
		
		public void addSliceListener(SliceListener l){
			listeners.add(l);
		}
		
		public boolean removeSliceListener(SliceListener l){
			return listeners.remove(l);
		}
		

		public void imageOpened(ImagePlus arg0) {
		}


		public void imageClosed(ImagePlus image) {
			if(image == ip){
                image.getProperties().remove(MONITOR_LOC);
				listeners.clear();
				ImagePlus.removeImageListener(this);
			}
		}


		public void imageUpdated(ImagePlus arg0) {
			int newSlicenumber= ip.getCurrentSlice();
			if(newSlicenumber != lastSlicenumber){
				fireSliceNumberChange(new SliceEvent(ip));
				lastSlicenumber = newSlicenumber;
			}
		}
	}
}
