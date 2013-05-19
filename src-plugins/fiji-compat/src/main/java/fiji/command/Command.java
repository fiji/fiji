package fiji.command;

import fiji.FijiTools;
import fiji.User_Plugins;

import ij.CommandListener;
import ij.IJ;
import ij.Menus;

import ij.io.OpenDialog;

import ij.plugin.MacroInstaller;
import ij.plugin.PlugIn;

import java.awt.Menu;

import java.awt.event.ActionEvent;

import java.io.File;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Hashtable;
import java.util.Vector;

public class Command {
	private long time = System.currentTimeMillis();
	private boolean consumed = false;
	protected String command;
	protected String className;
	protected String arg;
	protected Object plugin;
	protected int modifiers;

	public Command(String command) {
		this.command = command;
		setModifiers();
	}

	public void consume() { this.consumed = true; }
	public boolean isConsumed() { return consumed; }

	public String getClassName() { return className; }
	public String getCommand() { return command; }
	public String getArg() { return arg; }
	public int getModifiers() { return modifiers; }

	/** May return null if the plugin is never spawned. */
	public Object getPlugIn() { return plugin; }

	public void setModifiers() {
		modifiers = (IJ.altKeyDown()?ActionEvent.ALT_MASK:0)|(IJ.shiftKeyDown()?ActionEvent.SHIFT_MASK:0);
	}

	public String toString() {
		return new StringBuffer("command=").append(command).append(", className=").append(className)
			.append(", arg=").append(arg).append(", modifiers=").append(modifiers).append(", consumed=")
			.append(consumed).append(", time=").append(time).toString();
	}

	public void notify(Vector listeners, int action) {
                if (listeners.size() > 0) synchronized (listeners) {
                        for (int i = 0; i < listeners.size(); i++) {
                                CommandListener listener = (CommandListener)listeners.elementAt(i);
                                if (listener instanceof CommandListenerPlus)
                                        notify((CommandListenerPlus)listener, action);
                        }
                }
        }

	public void notify(CommandListenerPlus listener, int action) {
		listener.stateChanged(this, action);
	}

	public void runCommand(Vector listeners) {
		Hashtable table = Menus.getCommands();
		className = (String)table.get(command);
		if (className != null) {
			arg = "";
			if (className.endsWith("\")")) {
				// extract string argument (e.g. className("arg"))
				int argStart = className.lastIndexOf("(\"");
				if (argStart > 0) {
					arg = className.substring(argStart + 2, className.length() - 2);
					className = className.substring(0, argStart);
				}
			}
			notify(listeners, CommandListenerPlus.CMD_READY);
			if (isConsumed())
				return; // last chance to interrupt
			if (IJ.shiftKeyDown() &&
					className.startsWith("ij.plugin.Macro_Runner") &&
					!Menus.getShortcuts().contains("*" + command))
				IJ.open(IJ.getDirectory("plugins") + arg);
			else
				plugin = runPlugIn(command, className, arg);
			notify(listeners, CommandListenerPlus.CMD_STARTED);
		} else {
			notify(listeners, CommandListenerPlus.CMD_READY);
			// Is this command in Plugins>Macros?
			if (MacroInstaller.runMacroCommand(command)) {
				notify(listeners, CommandListenerPlus.CMD_MACRO);
				return;
			}
			// Is this command a LUT name?
			String path = IJ.getDirectory("luts") + command + ".lut";
			File f = new File(path);
			if (f.exists()) {
				String dir = OpenDialog.getLastDirectory();
				IJ.open(path);
				notify(listeners, CommandListenerPlus.CMD_LUT);
				OpenDialog.setLastDirectory(dir);
			} else if (!openRecent())
				IJ.error("Unrecognized command: " + command);
		}
		notify(listeners, CommandListenerPlus.CMD_FINISHED);
	}

	protected boolean openRecent() {
		Menu menu = User_Plugins.getMenu("File>Open Recent");
		if (menu == null)
			return false;
		for (int i = 0; i < menu.getItemCount(); i++) {
			if (menu.getItem(i).getLabel().equals(command)) {
				IJ.open(command);
				return true;
			}
		}
		return false;
	}

	protected static Method runPlugInMethod;

	/* For some unfathomable reason, this method is package local in IJ1 */
	protected static Object runPlugIn(String command, String className, String arg) {
		if (runPlugInMethod == null) {
			try {
				runPlugInMethod = IJ.class.getDeclaredMethod("runPlugIn",
					new Class[] { String.class, String.class, String.class });
				runPlugInMethod.setAccessible(true);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		try {
			return runPlugInMethod.invoke(null, new Object[] { command, className, arg });
		} catch (Exception e) {
			if (e instanceof InvocationTargetException) {
				Throwable cause = e.getCause();
				if (cause != null &&
						cause instanceof NoSuchMethodError &&
						FijiTools.handleNoSuchMethodError((NoSuchMethodError)cause))
					return null;
			}
			e.printStackTrace();
			IJ.handleException(e);
			return null;
		}
	}
}
