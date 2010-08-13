package QuickPALM;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

/** This plugin halts the Analyse Particles threads. */
public class Stop_processing implements PlugIn {

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
		int foundThread=-1;
		//for (int i = 0; i < activeCount; i++)
		//	names[i] = threads[i].getName();
		for (int i = 0; i < activeCount; i++)
			if (threads[i].getName().equals("Run$_Analyse Particles"))
				foundThread=i;
				
		//GenericDialog gd = new GenericDialog("Thread Killer");
		//gd.addChoice("Thread:", names, names[0]);
		//gd.showDialog();
		//if (gd.wasCanceled())
		//	return;
		//int threadIndex = gd.getNextChoiceIndex();
		//threads[threadIndex].stop();
		if (foundThread!=-1) threads[foundThread].stop();
	}
}