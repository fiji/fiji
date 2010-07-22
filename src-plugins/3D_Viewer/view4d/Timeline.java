package view4d;

import ij3d.Image3DUniverse;
import ij.ImagePlus;
import ij.ImageStack;


/**
 * Implements the functionality for the 4D viewer, like loading
 * and animation.
 *
 * @author Benjamin Schmid
 */
public class Timeline {

	private Image3DUniverse univ;
	private Thread playing = null;
	private boolean bounceback = true;

	/**
	 * Initialize the timeline
	 * @param univ
	 */
	public Timeline(Image3DUniverse univ) {
		this.univ = univ;
	}

	public Image3DUniverse getUniverse() {
		return univ;
	}

	public void setBounceBack(boolean bounce) {
		this.bounceback = bounce;
	}

	public boolean getBounceBack() {
		return bounceback;
	}

	/**
	 * Returns the number of time points.
	 * @return
	 */
	public int size() {
		if(univ.getContents().size() == 0)
			return 0;
		return univ.getEndTime() - univ.getStartTime();
	}

	/**
	 * Speed up the animation.
	 */
	public void faster() {
		if(delay >= 50)
			delay -= 50;
	}

	/**
	 * Slows the animation down.
	 */
	public void slower() {
		delay += 50;
	}

	public ImagePlus record() {
		pause();
		int s = univ.getStartTime();
		int e = univ.getEndTime();

		univ.showTimepoint(s);
		try {
			Thread.sleep(100);
		} catch(InterruptedException ex) {}
		ImagePlus imp = univ.takeSnapshot();
		ImageStack stack = new ImageStack(
			imp.getWidth(), imp.getHeight());
		stack.addSlice("", imp.getProcessor());

		for(int i = s + 1; i <= e; i++) {
			univ.showTimepoint(i);
			try {
				Thread.sleep(100);
			} catch(InterruptedException ex) {}
			stack.addSlice("", univ.takeSnapshot().getProcessor());
		}
		return new ImagePlus("Movie", stack);
	}

	private boolean shouldPause = false;
	private int delay = 200;

	/**
	 * Start animation.
	 */
	public synchronized void play() {
		if(size() == 0)
			return;
		if(playing != null)
			return;
		playing = new Thread(new Runnable() {
			public void run() {
				int inc = +1;
				shouldPause = false;
				while(!shouldPause) {
					int next = univ.getCurrentTimepoint() + inc;
					if(next > univ.getEndTime()) {
						if(bounceback) {
							inc = -inc;
							continue;
						} else {
							next = univ.getStartTime();
						}
					} else if(next < univ.getStartTime()) {
						inc = -inc;
						continue;
					}
					univ.showTimepoint(next);
					try {
						Thread.sleep(delay);
					} catch(Exception e) {
						shouldPause = true;
					}
				}
				shouldPause = false;
				playing = null;
			}
		});
		playing.start();
	}

	/**
	 * Stop/pause animation
	 */
	public synchronized void pause() {
		shouldPause = true;
	}

	/**
	 * Display next timepoint.
	 */
	public void next() {
		if(univ.getContents().size() == 0)
			return;
		int curr = univ.getCurrentTimepoint();
		if(curr == univ.getEndTime())
			return;
		univ.showTimepoint(curr + 1);
	}

	/**
	 * Display previous timepoint.
	 */
	public void previous() {
		if(univ.getContents().size() == 0)
			return;
		int curr = univ.getCurrentTimepoint();
		if(curr == univ.getStartTime())
			return;
		univ.showTimepoint(curr - 1);
	}

	/**
	 * Display first timepoint.
	 */
	public void first() {
		if(univ.getContents().size() == 0)
			return;
		int first = univ.getStartTime();
		if(univ.getCurrentTimepoint() == first)
			return;
		univ.showTimepoint(first);
	}

	/**
	 * Display last timepoint.
	 */
	public void last() {
		if(univ.getContents().size() == 0)
			return;
		int last = univ.getEndTime();
		if(univ.getCurrentTimepoint() == last)
			return;
		univ.showTimepoint(last);
	}
}

