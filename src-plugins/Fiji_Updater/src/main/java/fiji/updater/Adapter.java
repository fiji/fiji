package fiji.updater;

import java.awt.Frame;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.swing.JOptionPane;

/**
 * This class just hands off to the ImageJ Updater
 *
 * The Fiji Updater moved to a new home: ImageJ2. To this end, we will hand off to the
 * ImageJ Updater by using a URLClassLoader with a set of known-good .jar files from the
 * ImageJ Update site, unless the classes can be found locally already.
 *
 * @author Johannes Schindelin
 */
public class Adapter {
	public final static String JARS_PREFIX = "http://update.imagej.net/jars/";
	public final static String[] JARS = {
		"ij-ui-swing-updater", "ij-updater-core", "ij-core", "eventbus", "sezpoz"
	};
	public final static String[] VERSIONS = {
		"-2.0.0-beta5", "-2.0.0-beta5", "-2.0.0-beta5", "-1.4", "-1.9"
	};
	public final static String[] TIMESTAMPS = {
		"20121020192927", "20121020192927", "20121020192927", "20120404210913", "20120404210913"
	};
	public final static String UPDATER_CLASS_NAME = "imagej.updater.gui.ImageJUpdater";
	private final static String UPTODATE_CLASS_NAME = "imagej.updater.core.UpToDate";
	private final static String SWING_PROGRESS_CLASS_NAME = "imagej.updater.gui.ProgressDialog";
	private final static String STDERR_PROGRESS_CLASS_NAME = "imagej.updater.util.StderrProgress";
	private final String progressClassName;
	private final static String CHECKSUMMER_CLASS_NAME = "imagej.updater.core.Checksummer";
	private final static String COLLECTION_CLASS_NAME = "imagej.updater.core.FilesCollection";
	private static final String DOWNLOADER_CLASS_NAME = "imagej.updater.core.XMLFileDownloader";
	private final static String INSTALLER_CLASS_NAME = "imagej.updater.core.Installer";
	private static final String COMMAND_LINE_CLASS_NAME = "imagej.updater.ui.CommandLine";

	private static ClassLoader remoteClassLoader;
	private static Object progress;

	private UI ui;

	/**
	 * Construct a new Adapter object.
	 * 
	 * @param useIJ1
	 *            whether to use ImageJ 1.x' graphical user interface or an
	 *            stderr-based one instead
	 */
	public Adapter(boolean useIJ1) {
		ui = useIJ1 ? new ImageJ1UI() : new StderrUI();
		progressClassName = useIJ1 ? SWING_PROGRESS_CLASS_NAME : STDERR_PROGRESS_CLASS_NAME;
	}

	/**
	 * This implements the up-to-date check on startup, based on the ImageJ
	 * updater.
	 * 
	 * @return the status as a {@link String}
	 */
	public String checkOrShowDialog() {
		String result = check();
		if (result.toUpperCase().endsWith("AUTHENTICATION")) {
			ui.showStatus("Please run Help>Update Fiji occasionally");
			return null;
		}
		if (result.toUpperCase().equals("UPDATEABLE") && !ui.isBatchMode())
			showDialog();
		return result;
	}

	/**
	 * Show the dialog asking whether to run the ImageJ updater now when there
	 * are updates available.
	 * 
	 * Note: we do not check the latest nag here, as that is the job of the
	 * {@link #check()} method.
	 */
	public void showDialog() {
		Object[] options = {
			"Yes, please",
			"Never",
			"Remind me later"
		};
		switch (JOptionPane.showOptionDialog(null,
				"There are updates available.\nDo you want to start the ImageJ Updater now?",
				"Up-to-date check", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
				null, options, options[0])) {
		case 0:
			runUpdater();
			break;
		case 1:
			setLatestNag(Long.MAX_VALUE);
			break;
		case 2:
			setLatestNag(new Date().getTime() / 1000);
			break;
		case JOptionPane.CLOSED_OPTION:
			// do nothing
		}
	}

	/**
	 * This returns true if this seems to be the Debian packaged
	 * version of Fiji, or false otherwise.
	 */
	public static boolean isDebian() {
		String debianProperty = System.getProperty("fiji.debian");
		return debianProperty != null && debianProperty.equals("true");
	}

	/**
	 * Get the number of milliseconds after the UNIX epoch when we last asked.
	 * 
	 * @param epoch
	 *            the number of milliseconds
	 */
	protected void setLatestNag(long epoch) {
		try {
			invokeStatic(UPTODATE_CLASS_NAME, "setLatestNag", epoch);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Run the ImageJ updater (Swing).
	 */
	@SuppressWarnings("unchecked")
	public void runUpdater() {
		Class<Runnable> updaterClass = (Class<Runnable>)loadClass(UPDATER_CLASS_NAME);
		if (updaterClass != null) try {
			if (remoteClassLoader != null) try {
				firstTime();
			} catch (Throwable t) {
				t.printStackTrace();
				fallBackToRemoteUpdater(t);
				return;
			}
			Thread.currentThread().setContextClassLoader(updaterClass.getClassLoader());
			updaterClass.newInstance().run();
		} catch (Throwable t) {
			t.printStackTrace();
			fallBackToRemoteUpdater(t);
		}
	}

	// Fall back to running the updater from the remote update site
	private void fallBackToRemoteUpdater(Throwable t) {
		Class<Runnable> updaterClass = (Class<Runnable>)loadClass(UPDATER_CLASS_NAME, true);
		Thread.currentThread().setContextClassLoader(updaterClass.getClassLoader());
		try {
			updaterClass.newInstance().run();
		} catch (Throwable e) {
			ui.handleException(e);
			ui.error("Could not access the Updater: " + e.getMessage()
					+ "\nPrevious exception: " + t.getMessage());
		}
	}

	public void runCommandLineUpdater(String[] args) {
		try {
			progress = newInstance(progressClassName);
		} catch (Exception e) {
			ui.error("The Updater could not get the progress object:");
			ui.handleException(e);
			return;
		}
		if (progress != null) try {
			if (remoteClassLoader != null) try {
				firstTime();
			} catch (Throwable t) {
				t.printStackTrace();
				ui.error("Could not download the ImageJ Updater!");
				return;
			}
			Thread.currentThread().setContextClassLoader(progress.getClass().getClassLoader());
			invokeStatic(COMMAND_LINE_CLASS_NAME, "main", (Object)args);
		} catch (Throwable t) {
			ui.error("Could not access the Updater:");
			ui.handleException(t);
			return;
		}
	}

	/**
	 * If this is the first time we hand off to the ImageJ Updater, let's
	 * install the Updater.
	 * 
	 * This is the trickiest method of this class because it relies on the API
	 * of the ImageJ updater as it was current at the time of writing. So it
	 * really relies on the classes being available via a URLClassLoader
	 * accessing the precise file versions specified in the {@link #JARS},
	 * {@link #VERSIONS} and {@link #TIMESTAMPS} fields.
	 * 
	 * @throws Exception
	 */
	protected void firstTime() throws Exception {
		File ijDir = new File(System.getProperty("ij.dir"));

		File dbXmlGz = new File(ijDir, "db.xml.gz");
		if (!dbXmlGz.exists()) {
			OutputStream out = new GZIPOutputStream(new FileOutputStream(dbXmlGz));
			out.write("<pluginRecords><update-site name=\"Fiji\" url=\"http://fiji.sc/update/\" timestamp=\"0\"/></pluginRecords>".getBytes());
			out.close();
		}

		List<String> filenames = new ArrayList<String>();
		for (int i = 0; i < JARS.length; i++)
			filenames.add("jars/" + JARS[i] + VERSIONS[i] + ".jar");

		Map<String, Object> files = newInstance(COLLECTION_CLASS_NAME, ijDir);
		try {
			invoke(files, "read");
		} catch (Exception e) { /* ignore */ }
		Object downloader = newInstance(DOWNLOADER_CLASS_NAME, files);
		invoke(downloader, "start", false);
		Object checksummer = newInstance(CHECKSUMMER_CLASS_NAME, files, getProgress());
		invoke(checksummer, "updateFromLocal", filenames);
		for (String key : files.keySet())
			invoke(files.get(key), "setNoAction");
		for (String filename : filenames)
			invoke(files.get(filename), "stageForUpdate", files, false);
		Object installer = newInstance(INSTALLER_CLASS_NAME, files, getProgress());
		invoke(installer, "start");
		invoke(installer, "moveUpdatedIntoPlace");

		List<URL> classPath = new ArrayList<URL>();
		Object guiFile = invoke(files, "get", "jars/" + JARS[0] + ".jar");
		Iterable<Object> dependencies = invoke(guiFile, "getFileDependencies", files, true);
		for (Object file : dependencies)
			classPath.add(new File(ijDir, (String)invoke(file, "getLocalFilename", false)).toURI().toURL());
		remoteClassLoader = new URLClassLoader(classPath.toArray(new URL[classPath.size()]), String.class.getClassLoader());
		progress = loadClass(progress.getClass().getName());

		// Blow away ImageJ's class loader so we can pick up the newly downloaded classes
		if (progressClassName != SWING_PROGRESS_CLASS_NAME) try {
			invokeStatic("ij.IJ", "run", "Refresh Menus");
		} catch (Throwable t) {
			if (!"Could not find class: ij.IJ".equals(t.getMessage()))
				ui.handleException(t);
		}
	}

	/**
	 * Utility method for the quick up-to-date check in {@link fiji.Main}
	 *
	 * @return a tag describing whether we should run the Updater
	 */
	public String check() {
		try {
			return invokeStatic(UPTODATE_CLASS_NAME, "check").toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Utility method for the {@link fiji.packaging.Package_Maker}
	 *
	 * @return the list of files the Updater cares about
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public Collection<String> getFileList() throws Exception {
		Map<String, Object> collection = newInstance(COLLECTION_CLASS_NAME, new File(System.getProperty("ij.dir")));
		Object checksummer = newInstance(CHECKSUMMER_CLASS_NAME, collection, getProgress());

		try {
			invoke(checksummer, "updateFromLocal");
		} catch (Throwable t) {
			ui.error("Canceled");
			return null;
		}

		invoke(collection, "sort");
		List<String> result = new ArrayList<String>();
		for (Object file : (Iterable<Object>)collection) {
			result.add((String)invoke(file, "getLocalFilename", false));
		}

		return result;
	}

	/**
	 * Utility method for the {@link Bug_Submitter.Bug_Submitter}
	 *
	 * @return the list of files known to the Updater, with versions, as a String
	 */
	public String getInstalledVersions() {
		try {
			Map<String, Object> collection = newInstance(COLLECTION_CLASS_NAME, new File(System.getProperty("ij.dir")));
			Object checksummer = newInstance(CHECKSUMMER_CLASS_NAME, collection, getProgress());

			try {
				invoke(checksummer, "updateFromLocal");
			} catch (Throwable t) {
				ui.error("Canceled");
				return null;
			}

			Map<String, Object> checksums = invoke(checksummer, "getCachedChecksums");

			StringBuffer sb = new StringBuffer();

			for (Map.Entry<String, Object> entry : checksums.entrySet()) {
				    String file = entry.getKey();
				    Object version = entry.getValue();
				    sb.append("  ").append(get(version, "checksum")).append(" ");
				    sb.append(get(version, "timestamp")).append(" ");
				    sb.append(file).append("\n");
			}

			return sb.toString();
		} catch (Exception e) {
			ui.handleException(e);
			return null;
		}
	}

	/**
	 * Get the current progress object.
	 * 
	 * Make a new one if none has been instantiated yet.
	 * 
	 * @return the progress object
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws IllegalArgumentException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	protected Object getProgress() throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		if (progress == null)
			progress = progressClassName == SWING_PROGRESS_CLASS_NAME ? newInstance(progressClassName, (Frame)null) : newInstance(progressClassName);
		return progress;
	}

	/**
	 * Instantiate a new class.
	 * 
	 * This method uses the current URLClassLoader to load the specified class
	 * and find a constructor that matches the given parameters. It works
	 * completely by reflection to avoid the need to link at compile time to the
	 * ImageJ2 dependencies. The Fiji Updater was designed to update only
	 * plugins/Fiji_Updater.jar in case it was updateable, which is why we
	 * cannot simply make ImageJ2 a dependency of the Fiji Updater.
	 * 
	 * @param className
	 *            the class to load
	 * @param parameters
	 *            the parameters to pass to the constructor matching them
	 * @return the instance
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws IllegalArgumentException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	@SuppressWarnings("unchecked")
	private<T> T newInstance(String className, Object... parameters) throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		Class<?> clazz = loadClass(className);
		for (Constructor<?> constructor : clazz.getConstructors()) {
			if (doParametersMatch(constructor.getParameterTypes(), parameters))
				return (T)constructor.newInstance(parameters);
		}
		throw new NoSuchMethodException("No matching constructor found");
	}

	/**
	 * Invoke a method on an object instantiated by
	 * {@link #newInstance(String, Object...)}.
	 * 
	 * This method tries to find a method matching the given name and the
	 * parameter list. Just like {@link #newInstance(String, Object...)}, this
	 * works via reflection to avoid a compile-time dependency on ImageJ2.
	 * 
	 * @param object
	 *            the object whose method is to be called
	 * @param methodName
	 *            the name of the method to be called
	 * @param parameters
	 *            the parameters to pass to the method
	 * @return the return value of the method, if any
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	@SuppressWarnings("unchecked")
	private static<T> T invoke(Object object, String methodName, Object... parameters) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		for (Method method : object.getClass().getMethods()) {
			if (method.getName().equals(methodName) && doParametersMatch(method.getParameterTypes(), parameters))
				return (T)method.invoke(object, parameters);
		}
		throw new NoSuchMethodException("No matching method found");
	}

	/**
	 * Invoke a static method of a given class.
	 * 
	 * This method tries to find a static method matching the given name and the
	 * parameter list. Just like {@link #newInstance(String, Object...)}, this
	 * works via reflection to avoid a compile-time dependency on ImageJ2.
	 * 
	 * @param className
	 *            the name of the class whose static method is to be called
	 * @param methodName
	 *            the name of the static method to be called
	 * @param parameters
	 *            the parameters to pass to the static method
	 * @return the return value of the static method, if any
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	@SuppressWarnings("unchecked")
	private<T> T invokeStatic(String className, String methodName, Object... parameters) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Class<?> clazz = loadClass(className);
		for (Method method : clazz.getMethods()) {
			if (method.getName().equals(methodName) && doParametersMatch(method.getParameterTypes(), parameters))
				return (T)method.invoke(null, parameters);
		}
		throw new NoSuchMethodException("No matching method found");
	}

	/**
	 * Access a field of a given object.
	 * 
	 * This method finds the field of a given name in the object. It does not
	 * look recursively into the super-class to find the field, because these
	 * methods really are only meant to be used in the context of this class and
	 * not be of generic value.
	 * 
	 * Note that no attempt is made to call {@link Field#setAccessible(boolean)}
	 * , i.e. you will only be able to access public fields defined in the
	 * top-level class of the object.
	 * 
	 * @param object
	 *            the object whose field needs to be accessed
	 * @param fieldName
	 *            the name of the field
	 * @return the value of the field
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	@SuppressWarnings("unchecked")
	private static<T> T get(Object object, String fieldName) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Field field = object.getClass().getField(fieldName);
		return (T) field.get(object);
	}

	/**
	 * Set the value of a field of a given object.
	 * 
	 * This method finds the field of a given name in the object and sets its
	 * value. It does not look recursively into the super-class to find the
	 * field, because these methods really are only meant to be used in the
	 * context of this class and not be of generic value.
	 * 
	 * Note that no attempt is made to call {@link Field#setAccessible(boolean)}
	 * , i.e. you will only be able to access public fields defined in the
	 * top-level class of the object.
	 * 
	 * @param object
	 *            the object whose field needs to be accessed
	 * @param fieldName
	 *            the name of the field
	 * @param value
	 *            the value of the field
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	@SuppressWarnings("unused")
	private static void set(Object object, String fieldName, Object value) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Field field = object.getClass().getField(fieldName);
		field.set(object, value);
	}

	/**
	 * Check whether a list of parameters matches a list of parameter types.
	 * 
	 * This is used to find matching constructors and (possibly static) methods.
	 * 
	 * @param types
	 *            the parameter types
	 * @param parameters
	 *            the parameters
	 * @return whether the parameters match the types
	 */
	private static boolean doParametersMatch(Class<?>[] types, Object[] parameters) {
		if (types.length != parameters.length)
			return false;
		for (int i = 0; i < types.length; i++)
			if (parameters[i] != null) {
				Class<?> clazz = parameters[i].getClass();
				if (types[i].isPrimitive()) {
					if (types[i] != Long.TYPE && types[i] != Integer.TYPE && types[i] != Boolean.TYPE)
						throw new RuntimeException("unsupported primitive type " + clazz);
					if (types[i] == Long.TYPE && clazz != Long.class)
						return false;
					else if (types[i] == Integer.TYPE && clazz != Integer.class)
						return false;
					else if (types[i] == Boolean.TYPE && clazz != Boolean.class)
						return false;
				}
				else if (!types[i].isAssignableFrom(clazz))
					return false;
				}
		return true;
	}

	/**
	 * Load an ImageJ updater class, possibly from the ImageJ update site.
	 * 
	 * This method tries to find the class locally first. If the ImageJ updater
	 * has been installed at some stage, the current class loader -- which is
	 * the {@code FijiClassLoader} available via {@code IJ.getClassLoader()} --
	 * will find it.
	 * 
	 * Otherwise we fall back to instantiating a URLClassLoader with known deep
	 * links into the ImageJ update site.
	 * 
	 * @param name
	 *            the name of the class to load
	 * @return the class object
	 */
	protected Class<?> loadClass(String name) {
		return loadClass(name, false);
	}

	protected Class<?> loadClass(String name, boolean forceRemote) {
		ClassLoader currentLoader = forceRemote ? null : (remoteClassLoader != null ? remoteClassLoader : Adapter.class.getClassLoader());
		Class<?> result = null;
		try {
			result = currentLoader.loadClass(name);
		} catch (Throwable t) {
			if (remoteClassLoader == null) {
				ui.showStatus("Loading the remote ImageJ updater");
				// fall back to instantiating a URLClassLoader
				final URL[] urls = new URL[JARS.length];
				for (int i = 0; i < urls.length; i++) try {
					urls[i] = new URL(JARS_PREFIX + JARS[i] + VERSIONS[i] + ".jar-" + TIMESTAMPS[i]);
				} catch (MalformedURLException e) {
					ui.error("Invalid Updater URL: " + e.getMessage());
					return null;
				}
				remoteClassLoader = new URLClassLoader(urls, String.class.getClassLoader());
				// now we need to make sure that ij.dir is set properly because
				// FileUtils.getBaseDirectory() will be quite lost
				ensureIJDirIsSet();
			}
			try {
				result = remoteClassLoader.loadClass(name);
			} catch (ClassNotFoundException e) {
				ui.error("Could not find the class: " + e.getMessage());
				return null;
			}
		}
		return result;
	}

	/**
	 * Make sure that the property <i>ij.dir</i> is set.
	 * 
	 * When launching the ImageJ updater, we do not want it to guess from its
	 * <i>.jar</i> location (which might be a {@link http://...} URL) what the
	 * current ImageJ root directory might be.
	 * 
	 * Happily, ImageJ2 respects the property <i>ij.dir</i>, as long as that
	 * directory exists.
	 * 
	 * @throws RuntimeException
	 */
	public static void ensureIJDirIsSet() {
		String ijDir = System.getProperty("ij.dir");
		if (ijDir != null && new File(ijDir).isDirectory())
			return;
		ijDir = Adapter.class.getResource("Adapter.class").toString();
		for (String prefix : new String[] { "jar:", "file:" })
			if (ijDir.startsWith(prefix))
				ijDir = ijDir.substring(prefix.length());
		int bang = ijDir.indexOf("!/");
		if (bang >= 0) {
			ijDir = ijDir.substring(0, bang);
			if (ijDir.endsWith(".jar"))
				ijDir = new File(ijDir).getParent();
		}
		else {
			String suffix = "/" + Adapter.class.getName().replace('.', '/') + ".class";
			if (!ijDir.endsWith(suffix))
				throw new RuntimeException("Funny ?-) " + ijDir);
			ijDir = ijDir.substring(0, ijDir.length() - suffix.length());
		}
		for (String suffix : new String[] { "classes", "/", "target", "/", "Fiji_Updater", "/", "src-plugins", "jars", "plugins", "/", "build", "/" })
			if (ijDir.endsWith(suffix))
				ijDir = ijDir.substring(0, ijDir.length() - suffix.length());
		if (!new File(ijDir).isDirectory())
			throw new RuntimeException("Could not infer ImageJ root directory; please set the ij.dir property accordingly!");
		System.setProperty("ij.dir", ijDir);
	}
}
