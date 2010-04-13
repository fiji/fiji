/*
 * Created on 31-May-2006
 */
package events;

import ij.ImageListener;
import ij.ImagePlus;
import ij.gui.Roi;

import java.util.ArrayList;


/**
 * an adapter class that allows RoiEventListeners on an ImagePlus to be added (and removed)
 * Sadly the Roi has to be polled for changes, so it cannot really be gauranteed to be up to date.
 */
public class RoiWatcher {
public static final String MONITOR_LOC = "RoiWatcher.monitor";

public static final long POLL_TIME = 10;
	
	final ImagePlus data;
	
	public RoiWatcher(ImagePlus data){
		this.data=data;
		if(data.getProperty(MONITOR_LOC) == null){
			data.setProperty(MONITOR_LOC, new RoiMonitor(data));
		}
		
		
	}
	
	private RoiMonitor getMonitor(){
		return (RoiMonitor)data.getProperty(MONITOR_LOC);
	}
	
	public void addRoiListener(RoiListener l){
		getMonitor().addRoiListener(l);
	}
	
	public boolean removeRoiListener(RoiListener l){
		return getMonitor().removeRoiListener(l);
	}
	
	/**
	 * this class is nestled in the properties of an IP
	 * it polls the IP to tast if the Roi has changed
	 * dirty but there is no way of listening directly in ImageJ
	 * @author s0570397
	 * 
	 * Window - Preferences - Java - Code Style - Code Templates
	 */
	private static class RoiMonitor implements Runnable, ImageListener{
		final ImagePlus ip;
		final ArrayList<RoiListener> listeners = new ArrayList<RoiListener>();
		
		Roi oldRoi;
		
		boolean run = true;
		Thread thread;
		
		public RoiMonitor(ImagePlus ip){
			this.ip = ip;
			
			ImagePlus.addImageListener(this);
			
			thread = new Thread(this);
			thread.start();
			
		}
		
		
		public void fireRoiChange(RoiEvent e){
			System.out.println("roi change from " + this);
			for(RoiListener l:listeners){
				try{
					l.roiChanged(e);
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}		
		}
		
		public void addRoiListener(RoiListener l){
			listeners.add(l);
		}
		
		public boolean removeRoiListener(RoiListener l){
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


		public void imageUpdated(ImagePlus image) {
			if(image == ip){
                System.out.println("Image update, loading new ROI");
				oldRoi = ip.getRoi();
			}
		}


		public void run() {
			while(run){
				try {
					Thread.sleep(POLL_TIME);
				} catch (InterruptedException e) {
					
					e.printStackTrace();
				}
				Roi newRoi = ip.getRoi();
				
				if(newRoi == null && newRoi == null) continue;
				
				if((newRoi == null && newRoi != null) ||  !newRoi.equals(oldRoi)){
                    System.out.println("newRoi = " + newRoi);
                    System.out.println("oldRoi = " + oldRoi);
					fireRoiChange(new RoiEvent(ip));
					
					oldRoi = (Roi) newRoi;
				}
			}
		}
	}
}
