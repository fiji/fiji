package fiji.updater.util;

/**
 * This class helps with updating Fiji's JRE/JDK on Windows and Linux
 */

import fiji.updater.ui.IJProgress;

import ij.IJ;
import ij.Macro;

import ij.plugin.PlugIn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.MutableAttributeSet;

import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

import javax.swing.text.html.parser.ParserDelegator;

public class UpdateJava implements PlugIn {
	public final static String mainURL = "http://java.sun.com/javase/downloads/index.jsp";
	protected String cookie;
	protected int totalBytes, currentBytes;
	protected Progress progress;

	public UpdateJava() {
		this(new IJProgress());
		progress.setTitle("");
	}

	public UpdateJava(Progress progress) {
		this.progress = progress;
	}

	public void run(String  arg) {
		String what = Util.isDeveloper ? "JDK" : "JRE";
		try {
			downloadAndInstall(!Util.isDeveloper);
		} catch (IOException e) {
			progress.done();
			e.printStackTrace();
			IJ.error("Error downloading " + what + ": "
				+ (e instanceof UnknownHostException ? "unknown host " : "")
				+ e.getMessage());
		} catch (RuntimeException e) {
			progress.done();
			if (e.getMessage() == Macro.MACRO_CANCELED && !IJ.macroRunning())
				return;
			throw e;
		}
	}

	public void downloadAndInstall(boolean isJRE) throws IOException {
		progress.setCount(0, 1);
		String platform = (IJ.isLinux() ? "Linux" : IJ.isWindows() ? "Windows" : "MacOSX haha")
			+ (IJ.is64Bit() ? " x64" : "");
		String ext = IJ.isLinux() ? "bin" : "exe";

		String url = getLink("Download " + (isJRE ? "JRE" : "JDK"), mainURL);
		Form form = getForm("post", url);
		if (isJRE) {
			form.variables.put(form.ids.get("dnld_platform"), platform);
			form.variables.put(form.ids.get("dnld_license"), "true");
			// avoid matching *-rpm.bin
			url = getLink(Pattern.compile(" *jre-.*[^m]\\." + ext + " *"), form.submit(), form.url);
		}
		else {
			form = getForm("post", form.url);
			form.variables.put(form.ids.get("dnld_platform"), platform);
			// avoid matching *-rpm.bin
			url = getLink(Pattern.compile(" *jdk-.*[^m]\\." + ext + " *"), form.submit(), form.url);
		}
		File outputDirectory = getJavaDirectory(baseName(url), isJRE);
		if (outputDirectory.exists())
			abort("Already up-to-date? " + outputDirectory + " exists!");
		File exe = copyToTemp(url);
		progress.addItem("Installing new Java...");
		KnightRider eyeCandy = new KnightRider();
		eyeCandy.start();
		silentInstall(exe, outputDirectory);
		progress.addItem("Installing Java 3D...");
		launchProgram(new String[] {
			System.getProperty("fiji.executable"),
			"--java-home", outputDirectory.getAbsolutePath(),
			"--main-class", "ij3d.Install_J3D"
		}, null, null);
		eyeCandy.cancel();
		exe.delete();
		IJ.showMessage("Successfully installed new Java "
			+ (isJRE ? "Runtime" : "Development Kit")
			+ " to " + outputDirectory);
		progress.done();
	}

	public static void abort(String message) {
		IJ.error(message);
		throw new RuntimeException(Macro.MACRO_CANCELED);
	}

	public File copyToTemp(String url) throws IOException {
		File tmp = File.createTempFile("java-update", "");
		InputStream in = openURL(url);
		copyTo(in, tmp.getPath());
		File result = new File(tmp.getParentFile(), baseName(url));
		tmp.renameTo(result);
		return result;
	}

	public String baseName(String url) {
		int questionMark = url.lastIndexOf('?');
		if (questionMark > 0)
			url = url.substring(0, questionMark);
		return url.substring(url.lastIndexOf('/') + 1);
	}

	public void copyTo(InputStream in, String path) throws FileNotFoundException, IOException {
		FileOutputStream out = new FileOutputStream(path);
		byte[] buf = new byte[16384];
		for (;;) {
			int count = in.read(buf);
			if (count < 0)
				break;
			out.write(buf, 0, count);
			if (totalBytes > 0) {
				currentBytes += count;
				progress.setItemCount(currentBytes, totalBytes);
				if (currentBytes >= totalBytes)
					totalBytes = currentBytes = 0;
			}
		}
		in.close();
		out.close();
	}

	protected<T> List<T> getList(ParserCallback<T> callback, String url) throws IOException {
		return getList(openURL(url), callback);
	}

	protected InputStream openURL(String url) throws IOException {
		Util.useSystemProxies();
		if (url.startsWith("http://") || url.startsWith("https://")) try {
			HttpURLConnection http = (HttpURLConnection)new URL(url).openConnection();
			progress.addItem("Downloading " + url);
			getOrSetCookies(http);
			totalBytes = http.getContentLength();
			currentBytes = 0;
			return http.getInputStream();
		} catch (MalformedURLException e) {
			IJ.handleException(e);
		}
		cookie = null;
		return new FileInputStream(url);
	}

	public<T> List<T> getList(InputStream in, ParserCallback<T> callback) throws IOException {
		return getList(new InputStreamReader(in), callback);
	}

	public<T> List<T> getList(Reader reader, ParserCallback<T> callback) throws IOException {
		HTMLEditorKit.Parser parser;
		parser = new ParserDelegator();
		parser.parse(reader, callback, true);
		reader.close();
		return callback.result;
	}

	protected void getOrSetCookies(HttpURLConnection http) {
		if (cookie != null)
			http.setRequestProperty("Cookie", cookie);
		else {
			List<String> cookies = http.getHeaderFields().get("Set-Cookie");
			if (cookies != null && cookies.size() > 0) {
				cookie = "";
				for (String key : cookies) {
					if (cookie.length() > 0)
						cookie += "; ";
					cookie += key;
				}
			}
		}
	}

	protected interface Filter<T> {
		boolean accept(T t);
	}

	/**
	 * Parser callback base class
	 */
	protected abstract class ParserCallback<T> extends HTMLEditorKit.ParserCallback {
		String baseURL, baseURLSameDir, baseURLRoot;
		Filter<T> filter;
		List<T> result;
		T current;
		HTML.Tag tag;
		Class elementClass;

		public ParserCallback(String baseURL, Filter<T> filter, HTML.Tag tag, List<T> result) {
			this.baseURL = baseURL;
			if (baseURL.startsWith("http://") || baseURL.startsWith("https://")) {
				int slash = baseURL.indexOf('/', 8);
				baseURLRoot = baseURL.substring(0, slash);
			}
			else
				baseURLRoot = "";
			int slash = baseURL.lastIndexOf('/');
			baseURLSameDir = baseURL.substring(0, slash);

			this.tag = tag;
			this.filter = filter;
			this.result = result;
		}

		public String makeFullURL(String url) {
			if (url.startsWith("#"))
				return baseURL + url;
			else if (url.startsWith("/"))
				return baseURLRoot + url;
			else if (url.indexOf("://") < 0)
				return baseURLSameDir + url;
			return url;
		}

		public abstract T createElement(HTML.Tag t, MutableAttributeSet a, int pos);

		public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
			if (t == tag)
				current = createElement(t, a, pos);
		}

		public void handleEndTag(HTML.Tag t, int pos) {
			if (t == tag && current != null) {
				if (filter == null || filter.accept(current))
					result.add(current);
				current = null;
			}
		}

		public void handleError(String errorMsg, int pos) {
			//IJ.log(errorMsg);
		}
	}

	public String getLink(final String title, String url) throws IOException {
		return getLink(openURL(url), getLinkParser(url, title), url);
	}

	public String getLink(final Pattern titlePattern, InputStream in, String url) {
		return getLink(in, getLinkParser(url, titlePattern), url);
	}

	public String getLink(InputStream in, ParserCallback callback, String url) {
		try {
			List<Link> list = getList(in, callback);
			if (list.size() > 1)
				for (int i = 1; i < list.size(); i++)
					if (!list.get(i).url.equals(list.get(0).url)) {
						IJ.log("Ambiguous link:\n" + list.get(i).url + "\n" + list.get(0).url);
						abort("Ambiguous link in " + url);
					}
			if (list.size() == 0)
				abort("Could not find link in " + url);
			return list.get(0).url;
		} catch (IOException e) {
			abort("Could not fetch " + url);
			return null; // shut up javac
		}
	}

	public class Link {
		protected String title, altTitle, url;
	}

	public ParseLinks getLinkParser(String baseURL, final String title) {
		return new ParseLinks(baseURL, new Filter<Link>() {
			public boolean accept(Link link) {
				return link.title.equals(title);
			}
		}, new ArrayList<Link>());
	}

	public ParseLinks getLinkParser(String baseURL, final Pattern titlePattern) {
		return new ParseLinks(baseURL, new Filter<Link>() {
			public boolean accept(Link link) {
				return titlePattern.matcher(link.title).matches();
			}
		}, new ArrayList<Link>());
	}

	/**
	 * Parser callback to accumulate all the links in a list
	 */
	protected class ParseLinks extends ParserCallback<Link> {

		public ParseLinks(String baseURL, Filter<Link> filter, List<Link> result) {
			super(baseURL, filter, HTML.Tag.A, result);
		}

		public Link createElement(HTML.Tag tag, MutableAttributeSet a, int pos) {
			String url = (String)a.getAttribute(HTML.Attribute.HREF);
			if (url == null)
				return null; // skip <a name=...> tags

			Link link = new Link();
			link.title = "";
			link.altTitle = (String)a.getAttribute(HTML.Attribute.ALT);
			link.url = makeFullURL(url);
			return link;
		}

		public void handleText(char[] data, int pos) {
			if (current != null)
				current.title += new String(data);
		}
	}

	public Form getForm(final String method, String url) {
		try {
			List<Form> list = getList(getFormParser(url, method), url);
			if (list.size() > 1)
				for (int i = 1; i < list.size(); i++)
					if (!list.get(i).url.equals(list.get(0).url))
						abort("Ambiguous form of method '" + method + "' in " + url);
			if (list.size() == 0)
				abort("Could not find form of method '" + method + "' in " + url);
			return list.get(0);
		} catch (IOException e) {
			abort("Could not fetch " + url);
			return null; // shut up javac
		}
	}

	protected class Form {
		protected Map<String, String> variables = new HashMap<String, String>();
		protected Map<String, String> ids = new HashMap<String, String>();
		protected String name, method, url;

		public String toString() {
			String result = "FORM(" + name + "," + method + "," + url + ")";
			for (String key : variables.keySet())
				result += "\n\t" + key + "=" + variables.get(key);
			for (String id : ids.keySet())
				result += "\n\tID " + id + " -> " + ids.get(id);
			return result + "\n";
		}

		public InputStream submit() throws IOException, MalformedURLException {
			String parameters = "";
			for (String key : variables.keySet()) try {
				if (parameters.length() > 0)
					parameters += "&";
				parameters += URLEncoder.encode(key, "UTF-8") + "="
					+ URLEncoder.encode(variables.get(key), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				IJ.handleException(e);
			}
			byte[] bytes = parameters.getBytes();

			URLConnection connection = new URL(url).openConnection();
			if (!method.equalsIgnoreCase("get") &&
					!(connection instanceof HttpURLConnection))
				throw new IOException("Tried to " + method + ", but is not HTTP: " + url);
			HttpURLConnection http = (HttpURLConnection)connection;
			getOrSetCookies(http);
			http.setRequestMethod(method.toUpperCase());
			http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			http.setUseCaches(false);
			http.setDoInput(true);
			http.setDoOutput(true);
			http.setRequestProperty("Content-Length", "" + bytes.length);
			OutputStream out = http.getOutputStream();
			out.write(bytes);
			out.close();

			progress.addItem("Downloading " + url);
			totalBytes = http.getContentLength();
			currentBytes = 0;

			return connection.getInputStream();
		}
	}

	public ParseForms getFormParser(String baseURL, final String method) {
		return new ParseForms(baseURL, new Filter<Form>() {
			public boolean accept(Form form) {
				return form.method.equalsIgnoreCase(method);
			}
		}, new ArrayList<Form>());
	}

	/**
	 * Simple callback to collect the forms
	 */
	protected class ParseForms extends ParserCallback<Form> {

		public ParseForms(String baseURL, Filter<Form> filter, List<Form> result) {
			super(baseURL, filter, HTML.Tag.FORM, result);
		}

		public Form createElement(HTML.Tag tag, MutableAttributeSet a, int pos) {
			Form form = new Form();
			form.name = (String)a.getAttribute(HTML.Attribute.NAME);
			form.method = (String)a.getAttribute(HTML.Attribute.METHOD);
			form.url = (String)a.getAttribute(HTML.Attribute.ACTION);
			return form;
		}

		public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
			if (!handleInput(t, a))
				super.handleStartTag(t, a, pos);
		}

		public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
			handleInput(t, a);
		}

		protected boolean handleInput(HTML.Tag t, MutableAttributeSet a) {
			if (t != HTML.Tag.INPUT && t != HTML.Tag.SELECT)
				return false;
			if (current != null) {
				String id = (String)a.getAttribute(HTML.Attribute.ID);
				String name = (String)a.getAttribute(HTML.Attribute.NAME);
				String value = (String)a.getAttribute(HTML.Attribute.VALUE);
				if (name == null)
					name = id;
				if (value == null)
					value = "";
				else try {
					value = URLDecoder.decode(value, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					IJ.handleException(e);
				}
				current.variables.put(name, value);
				if (id != null)
					current.ids.put(id, name);
			}
			return true;
		}
	}

	protected File getJavaDirectory(String exeName, boolean isJRE) {
		String pattern = (isJRE ? "jre" : "jdk") + "-([0-9][0-9]*)u([0-9][0-9]*)-.*";
		Matcher matcher = Pattern.compile(pattern).matcher(exeName);
		if (!matcher.matches())
			abort("Could not determine Java version");
		String subDirectory = "java/"
			+ (IJ.isWindows() ? (IJ.is64Bit() ? "win64" : "win32") : (IJ.is64Bit() ? "linux-amd64" : "linux"))
			+ "/jdk1." + matcher.group(1) + ".0_" + matcher.group(2);
		if (isJRE)
			subDirectory += "/jre";
		return new File(System.getProperty("fiji.dir"), subDirectory);
	}

	protected void silentInstall(File exe, File targetDirectory) {
		if (targetDirectory.exists())
			abort("The directory " + targetDirectory + " already exists! Already up-to-date?");

		File parentDirectory = IJ.isWindows() ? targetDirectory : targetDirectory.getParentFile();
		if (!parentDirectory.exists() && !parentDirectory.mkdirs())
			abort("Could not make the directory " + parentDirectory + "!");
		Set<String> oldItems = new HashSet<String>();
		for (String item : parentDirectory.list())
			oldItems.add(item);

		String[] cmdarray = IJ.isWindows() ?
			// TODO: use 8.3 path on Windows to avoid quoting
			new String[] { exe.getPath(), "/s", "INSTALLDIR=" + parentDirectory.getAbsolutePath(), "STATIC=1" } :
			new String[] { "sh", exe.getPath() };
		launchProgram(cmdarray, parentDirectory, IJ.isWindows() ? null : "yes\n");

		if (!targetDirectory.exists()) {
			String[] list = parentDirectory.list();
			int index = -1;
			for (int i = 0; i < list.length; i++)
				if (!oldItems.contains(list[i])) {
					if (index < 0)
						index = i;
					else
						abort("Installation error: too many directories in " + parentDirectory
							+ " (" + list[index] + ", " + list[i]
							+ (i + 1 < list.length ? ", ..." : "") + ")");
				}
			if (index < 0)
				abort("Installation error: no directory created in " + parentDirectory);
			new File(parentDirectory, list[index]).renameTo(targetDirectory);
		}
	}

	protected static void launchProgram(String[] cmdarray, File directory, final String stdin) {
		try {
			final Process process = Runtime.getRuntime().exec(cmdarray, null, directory);
			if (stdin == null)
				process.getOutputStream().close();
			else
				new Thread() {
					public void run() {
						OutputStream out = process.getOutputStream();
						try {
							out.write(stdin.getBytes());
							out.close();
						} catch (IOException e) {
							IJ.handleException(e);
						}
					}
				}.start();
			StreamDumper stderr = new StreamDumper(process.getErrorStream());
			StreamDumper stdout = new StreamDumper(process.getInputStream());
			int exitCode = -1;
			for (;;) try {
				exitCode = process.waitFor();
				break;
			} catch (InterruptedException e) { /* ignore */ }
			for (;;) try {
				stdout.join();
				break;
			} catch (InterruptedException e) { /* ignore */ }
			for (;;) try {
				stderr.join();
				break;
			} catch (InterruptedException e) { /* ignore */ }
			if (exitCode != 0) {
				String cmd = joinArgs(cmdarray);
				IJ.log("Command '" + cmd + "' failed.");
				IJ.log("Error output:\n" + stderr.out);
				IJ.log("Other output:\n" + stdout.out);
				abort("Failed command:\n" + cmd);
			}
		} catch (IOException e) {
			abort("I/O problem launching the command:\n" + joinArgs(cmdarray));
		}
	}

	protected static class StreamDumper extends Thread {
		protected InputStream in;
		public StringBuffer out;

		public StreamDumper(InputStream in) {
			this.in = in;
			out = new StringBuffer();
			start();
		}

		public void run() {
			byte[] buffer = new byte[16384];
			try {
				for (;;) {
					int count = in.read(buffer);
					if (count < 0)
						break;
					out.append(new String(buffer, 0, count));
				}
				in.close();
			} catch (IOException e) {
				IJ.handleException(e);
			}
		}
	}

	protected static String joinArgs(String[] cmdarray) {
		String cmd = cmdarray[0];
		for (int i = 1; i < cmdarray.length; i++)
			cmd += " " + cmdarray[i];
		return cmd;
	}

	protected class KnightRider extends Thread {
		protected int total = 20, current, increment;
		protected long frameMillis = 50;
		protected boolean canceled = false;

		public void run() {
			canceled = false;
			current = 1;
			increment = +1;

			while (!canceled) {
				progress.setItemCount(current, total);
				if (current + increment == 0 || current + increment == total)
					increment = -increment;
				current += increment;
				long now = System.currentTimeMillis();
				for (;;) try {
					long millis = frameMillis - (System.currentTimeMillis() - now);
					if (millis > 0)
						Thread.sleep(millis);
					break;
				} catch (InterruptedException e) {
					/* ignore */
				}
			}
			progress.itemDone("");
		}

		public void cancel() {
			canceled = true;
		}
	}
}
