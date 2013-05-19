package imagescience.utility;

import ij.IJ;
import ij.ImageJ;
import ij.gui.ProgressBar;
import java.awt.Graphics;

/** Wrapper around ImageJ's progress bar. This class offers several advantages over ImageJ's native progress displaying methods. First, it allows one to make sure that the progress bar is <em>always</em> updated (repainting can be enforced) if displaying is enabled, even when it is used from within the event dispatching thread (in which case the bar - within ImageJ drawn using that same thread - would otherwise not be updated until the process using the bar is finished). Furthermore, it relieves the user of explicitly computing the progress percentage (all this class requires is the total number of steps before the start of a process and the same number of step calls during the process). Also, it allows specifying the number of progress updates (by default 20 from start to end), thereby limiting the number of repaint calls (which are relatively expensive), and thus reducing execution time for progress displaying. Finally, it allows specifying the progress range (the minimum / maximum displayed progress value for the start / end of the corresponding process), which facilitates progress displaying for subprocesses. */
public class Progressor {
	
	private int step = 0;
	private int update = 0;
	
	private double steps = 0;
	private double updates = 20;
	
	private double min = 0, max = 1;
	private double percent = 0;
	
	private boolean display = false;
	private boolean enforce = false;
	
	private Progressor parent = null;
	
	/** Default constructor. */
	public Progressor() { }
	
	/** Specifies the number of progress updates displayed by the progress bar. The default number of updates is {@code 20}.
		
		@param n the number of progress updates displayed by the progress bar.
		
		@exception IllegalArgumentException if {@code n} is less than {@code 0}.
	*/
	public void updates(final int n) {
		
		if (n < 0) throw new IllegalArgumentException("Number of updates less than 0");
		updates = n;
	}
	
	/** Returns the number of progress updates displayed by the progress bar. */
	public double updates() { return updates; }
	
	/** Specifies the number of steps in the process.
		
		@param n the number of steps in the process.
		
		@exception IllegalArgumentException if {@code n} is less than {@code 0}.
	*/
	public void steps(final int n) {
		
		if (n < 0) throw new IllegalArgumentException("Number of steps less than 0");
		steps = n;
	}
	
	/** Returns the number of steps in the process. */
	public double steps() { return steps; }
	
	/** Increases the internal step counter. */
	public void step() {
		
		if (steps > 0) {
			++step;
			percent = step/steps;
			final int pup = (int)(percent*updates);
			if (pup > update) {
				update = pup;
				progress();
			}
		}
	}
	
	/** Increases the internal step counter by the given amount.
		
		@param n the number of steps to be added to the internal step counter.
		
		@exception IllegalArgumentException if {@code n} is less than {@code 0}.
	*/
	public void step(final int n) {
		
		if (n < 0)
			throw new IllegalArgumentException("Number of steps less than 0");
		if (steps > 0) {
			step += n;
			percent = step/steps;
			final int pup = (int)(percent*updates);
			if (pup > update) {
				update = pup;
				progress();
			}
		}
	}
	
	/** Specifies the minimum displayed progress value. The default minimum is {@code 0}. If a parent object has been set using {@link #parent(Progressor)}, the actual minimum displayed progress value as returned by {@link #min()} is the specified value, mapped to the progress range of the parent.
		
		@param min the minimum displayed progress value.
		
		@exception IllegalArgumentException if {@code min} is less than {@code 0} or larger than the maximum displayed progress value.
	*/
	public void min(final double min) {
		
		if (min < 0) throw new IllegalArgumentException("Minimum progress value less than 0");
		else if (min > max) throw new IllegalArgumentException("Minimum larger than maximum progress value");
		this.min = min;
	}
	
	/** Returns the minimum displayed progress value. */
	public double min() {
		
		if (parent != null) {
			final double pmin = parent.min();
			final double pmax = parent.max();
			return pmin + min*(pmax - pmin);
		} else return min;
	}
	
	/** Specifies the maximum displayed progress value. The default maximum is {@code 1}. If a parent object has been set using {@link #parent(Progressor)}, the actual maximum displayed progress value as returned by {@link #max()} is the specified value, mapped to the progress range of the parent.
		
		@param max the maximum displayed progress value.
		
		@exception IllegalArgumentException if {@code max} is larger than {@code 1} or less than the minimum displayed progress value.
	*/
	public void max(final double max) {
		
		if (max > 1) throw new IllegalArgumentException("Maximum progress value larger than 1");
		else if (max < min) throw new IllegalArgumentException("Maximum less than minimum progress value");
		this.max = max;
	}
	
	/** Returns the maximum displayed progress value. */
	public double max() {
		
		if (parent != null) {
			final double pmin = parent.min();
			final double pmax = parent.max();
			return pmin + max*(pmax - pmin);
		} else return max;
	}
	
	/** Specifies the minimum and maximum displayed progress value. The default minimum is {@code 0} and the default maximum is {@code 1}. If a parent object has been set using {@link #parent(Progressor)}, the actual minimum and maximum displayed progress values as returned by {@link #min()} and {@link #max()} are the specified values, mapped to the progress range of the parent.
		
		@param min the minimum displayed progress value.
		
		@param max the maximum displayed progress value.
		
		@exception IllegalArgumentException if {@code min} is less than {@code 0} or larger than {@code max}, or if {@code max} is larger than {@code 1}.
	*/
	public void range(final double min, final double max) {
		
		if (min < 0) throw new IllegalArgumentException("Minimum progress value less than 0");
		else if (max > 1) throw new IllegalArgumentException("Maximum progress value larger than 1");
		else if (min > max) throw new IllegalArgumentException("Minimum larger than maximum progress value");
		this.min = min;
		this.max = max;
	}
	
	/** Starts the progress displaying. Should be called at the start of the process whose progress is to be displayed. */
	public void start() {
		
		step = 0;
		update = 0;
		percent = 0;
		progress();
	}
	
	/** Stops the progress displaying. Should be called at the end of the process whose progress was displayed. */
	public void stop() {
		
		percent = 1;
		progress();
	}
	
	private void progress() {
		
		if (display()) {
			final ImageJ ij = IJ.getInstance();
			if (ij != null) { // To avoid exceptions in ImageJ batch mode
				final ProgressBar pb = ij.getProgressBar();
				if (pb !=null) {
					final double lo = min();
					final double hi = max();
					pb.show(lo + percent*(hi - lo));
					if (enforce()) {
						final Graphics gx = pb.getGraphics();
						if (gx != null) pb.paint(gx); // Enforces a refresh also when called from event stack
					}
				}
			}
		}
	}
	
	/** Indicates whether progress is displayed. */
	public boolean display() {
		
		if (parent != null) return parent.display();
		else return display;
	}
	
	/** Determines whether progress is displayed. By default progress is not displayed. If a parent object has been set using {@link #parent(Progressor)}, that object instead determines whether progress is displayed, as indicated by {@link #display()}. */
	public void display(final boolean enable) {
		
		display = enable;
	}
	
	/** Indicates whether repainting of the progress bar is enforced. */
	public boolean enforce() {
		
		if (parent != null) return parent.enforce();
		else return enforce;
	}
	
	/** Determines whether repainting of the progress bar is enforced. By default it is not enforced. Under normal circumstances the progress bar gets repainted in the event dispatching thread. This precludes progress displaying for processes running in that same thread. By enabling enforced repainting, the progress bar gets repainted explicitly, outside of that thread. If a parent object has been set using {@link #parent(Progressor)}, that object instead determines whether repainting is enforced, as indicated by {@link #enforce()}. */
	public void enforce(final boolean enable) {
		
		enforce = enable;
	}
	
	/** Sets the parent object for progress displaying. The default parent is {@code null} (no parent). If a parent object is specified, that object determines whether progress is displayed, overruling the value set using {@link #display(boolean)}, and the minimum and maximum displayed progress values set using {@link #min(double)} and {@link #max(double)} are mapped to the progress range of the parent. */
	public void parent(final Progressor parent) {
		
		this.parent = parent;
	}
	
	/** Returns the parent object for progress displaying. The returned value is either {@code null} (indicating no parent has been set) or a valid {@code Progressor} object. */
	public Progressor parent() {
		
		return parent;
	}
	
}
