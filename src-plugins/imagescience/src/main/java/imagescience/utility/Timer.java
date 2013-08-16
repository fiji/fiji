package imagescience.utility;

import java.lang.System;

/** Facilitates measuring of the computation time consumed by a process. The measuring is done with millisecond precision. Note, however, that the accuracy of the measuring depends on the timing granularity of the underlying operating system and may be coarser than milliseconds. */
public class Timer {
	
	private final static long MILLISECONDS_PER_DAY = 86400000;
	private final static long MILLISECONDS_PER_HOUR = 3600000;
	private final static long MILLISECONDS_PER_MINUTE = 60000;
	private final static long MILLISECONDS_PER_SECOND = 1000;
	
	private long start = 0;
	private long elapsed = 0;
	private boolean running = false;
	
	/** Default constructor. */
	public Timer() { }
	
	/** Starts the timer. */
	public void start() {
		
		start = System.currentTimeMillis();
		elapsed = 0;
		running = true;
	}
	
	/** Pauses the timer. */
	public void pause() {
		
		if (running) {
			elapsed += System.currentTimeMillis() - start;
			running = false;
		}
	}
	
	/** Resumes the timer. */
	public void resume() {
		
		if (!running) {
			start = System.currentTimeMillis();
			running = true;
		}
	}
	
	/** Stops the timer and returns the elapsed time with millisecond precision.
		
		@return the elapsed time with millisecond precision.
	*/
	public long stop() {
		
		if (running) {
			elapsed += System.currentTimeMillis() - start;
			running = false;
		}
		
		if (messenger.log()) {
			final long days = elapsed / MILLISECONDS_PER_DAY;
			final long drest = elapsed % MILLISECONDS_PER_DAY;
			final long hours = drest / MILLISECONDS_PER_HOUR;
			final long hrest = drest % MILLISECONDS_PER_HOUR;
			final long minutes = hrest / MILLISECONDS_PER_MINUTE;
			final long mrest = hrest % MILLISECONDS_PER_MINUTE;
			final long seconds = mrest / MILLISECONDS_PER_SECOND;
			final long milliseconds = mrest % MILLISECONDS_PER_SECOND;
			boolean d = false, h = false, m = false, s = false;
			if (days != 0) { d = h = m = s = true; }
			else if (hours != 0) { h = m = s = true; }
			else if (minutes != 0) { m = s = true; }
			else if (seconds != 0) { s = true; }
			messenger.log("Finished in " +
				(d ? days + "d " : "") +
				(h ? hours + "h " : "") +
				(m ? minutes + "m " : "") +
				(s ? seconds + "s " : "") +
				milliseconds + "ms"
			);
		}
		
		return elapsed;
	}
	
	/** The object used for message displaying. */
	public final Messenger messenger = new Messenger();
	
}
