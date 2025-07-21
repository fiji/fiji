/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2007 - 2025 Fiji developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package sc.fiji.compat;

import java.awt.EventQueue;

/**
 * This class helps with invoking tasks much, much later.
 *
 * {@link EventQueue#invokeLater(Runnable)} works fine if something needs to
 * run after all currently queued tasks have been run. However, sometimes
 * these tasks run other tasks which run yet other tasks and we might want
 * to have them run first, before invoking our task.
 *
 * By queuing the same task a predetermined number of times, we achieve this.
 *
 * @author Johannes Schindelin
 */
public class InvokeLater implements Runnable {
	private long waitMillis;
	private int count;
	private final Runnable runnable;

	/**
	 * The constructor.
	 *
	 * @param count the number of times to queue the task before
	 *        actually invoking it.
	 * @param runnable the task.
	 */
	public InvokeLater(final int count, final Runnable runnable) {
		this.count = count;
		this.runnable = runnable;
	}

	public void later(long millis) {
		waitMillis = millis;
		new Thread(this).start();
	}

	@Override
	public void run() {
		if (waitMillis > 0) try {
			Thread.sleep(waitMillis);
			waitMillis = 0;
		} catch (InterruptedException e) {
			// ignore and return
			return;
		}
		if (--count <= 0) {
			runnable.run();
		} else {
			EventQueue.invokeLater(this);
		}
	}
}
