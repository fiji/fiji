import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import ij.gui.GenericDialog;

import ij.plugin.PlugIn;

public class Thread_Killer implements PlugIn {
	public void run(String arg) {
		ThreadGroup group = Thread.currentThread().getThreadGroup();
		int activeCount = group.activeCount();
		Thread[] threads = new Thread[activeCount];
		group.enumerate(threads);
		int j = 0;
		for (int i = 0; i < activeCount; i++) {
			String name = threads[i].getName();
			if (threads[i] == Thread.currentThread() ||
					name.startsWith("AWT-") ||
					name.equals("zSelector") ||
					name.equals("Java2D Disposer") ||
					name.equals("SocketListener") ||
					name.equals("DestroyJavaVM") ||
					name.equals("TimerQueue"))
				continue;
			if (j < i)
				threads[j] = threads[i];
			System.err.println("nr " + i + ": " + threads[i].getName());
			j++;
		}
		activeCount = j;
		if (activeCount == 0) {
			IJ.showMessage("No threads to kill.");
			return;
		}

		String[] names = new String[activeCount];
		for (int i = 0; i < activeCount; i++)
			names[i] = threads[i].getName();
		GenericDialog gd = new GenericDialog("Thread to kill");
		gd.addChoice("thread", names, names[0]);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		int threadIndex = gd.getNextChoiceIndex();
		threads[threadIndex].stop();

		for (int id : WindowManager.getIDList()) {
			ImagePlus image = WindowManager.getImage(id);
			if (image != null)
				image.unlock();
		}
	}
}
