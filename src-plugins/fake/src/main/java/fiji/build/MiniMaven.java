package fiji.build;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

import java.lang.reflect.Method;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import java.util.zip.ZipEntry;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.xml.sax.helpers.DefaultHandler;

public class MiniMaven {
	public static void ensureIJDirIsSet() {
		String ijDir = System.getProperty("ij.dir");
		if (ijDir != null && new File(ijDir).isDirectory())
			return;
		ijDir = MiniMaven.class.getResource("MiniMaven.class").toString();
		for (String prefix : new String[] { "jar:", "file:" })
			if (ijDir.startsWith(prefix))
				ijDir = ijDir.substring(prefix.length());
		int bang = ijDir.indexOf("!/");
		if (bang >= 0)
			ijDir = ijDir.substring(0, bang);
		else {
			String suffix = "/" + MiniMaven.class.getName().replace('.', '/') + ".class";
			if (ijDir.endsWith(suffix))
				ijDir = ijDir.substring(0, ijDir.length() - suffix.length());
			else
				throw new RuntimeException("Funny ?-) " + ijDir);
		}
		for (String suffix : new String[] { "fake.jar", "fake", File.separator, "jars", File.separator, "build", File.separator })
			if (ijDir.endsWith(suffix))
				ijDir = ijDir.substring(0, ijDir.length() - suffix.length());
		System.setProperty("ij.dir", ijDir);
	}

	private final static String usage = "Usage: MiniMaven [command]\n"
		+ "\tSupported commands: compile, run, compile-and-run, clean, get-dependencies";

	public static void main(String[] args) throws Exception {
		ensureIJDirIsSet();
		BuildEnvironment miniMaven = new BuildEnvironment(System.err, "true".equals(getSystemProperty("minimaven.verbose", "false")), false);
		POM root = miniMaven.parse(new File("pom.xml"), null);
		String command = args.length == 0 ? "compile-and-run" : args[0];
		String artifactId = getSystemProperty("artifactId", root.coordinate.artifactId.equals("pom-ij-base") ? "ij-app" : root.coordinate.artifactId);

		POM pom = root.findPOM(new Coordinate(null, artifactId, null), false, miniMaven.downloadAutomatically);
		if (pom == null)
			pom = root;
		if (command.equals("compile") || command.equals("build") || command.equals("compile-and-run")) {
			pom.build();
			if (command.equals("compile-and-run"))
				command = "run";
			else
				return;
		}
		else if (command.equals("jar") || command.equals("jars")) {
			if (!pom.buildFromSource) {
				System.err.println("Cannot build " + pom + " from source");
				System.exit(1);
			}
			pom.buildJar();
			if (command.equals("jars"))
				pom.copyDependencies(new File(pom.directory, "target"), true);
			return;
		}
		if (command.equals("clean"))
			pom.clean();
		else if (command.equals("get") || command.equals("get-dependencies"))
			pom.downloadDependencies();
		else if (command.equals("run")) {
			String mainClass = getSystemProperty("mainClass", pom.mainClass);
			if (mainClass == null) {
				miniMaven.err.println("No main class specified in pom " + pom.coordinate);
				System.exit(1);
			}
			String[] paths = pom.getClassPath(false).split(File.pathSeparator);
			URL[] urls = new URL[paths.length];
			for (int i = 0; i < urls.length; i++)
				urls[i] = new URL("file:" + paths[i] + (paths[i].endsWith(".jar") ? "" : "/"));
			URLClassLoader classLoader = new URLClassLoader(urls);
			// needed for sezpoz
			Thread.currentThread().setContextClassLoader(classLoader);
			Class clazz = classLoader.loadClass(mainClass);
			Method main = clazz.getMethod("main", new Class[] { String[].class });
			main.invoke(null, new Object[] { new String[0] });
		}
		else if (command.equals("classpath"))
			miniMaven.err.println(pom.getClassPath(false));
		else if (command.equals("list")) {
			Set<POM> result = new TreeSet<POM>();
			Stack<POM> stack = new Stack<POM>();
			stack.push(pom.getRoot());
			while (!stack.empty()) {
				pom = stack.pop();
				if (result.contains(pom) || !pom.buildFromSource)
					continue;
				result.add(pom);
				for (POM child : pom.getChildren())
					stack.push(child);
			}
			for (POM pom2 : result)
				System.err.println(pom2);
		}
		else
			miniMaven.err.println("Unhandled command: " + command + "\n" + usage);
	}

	protected static String getSystemProperty(String key, String defaultValue) {
		String result = System.getProperty(key);
		return result == null ? defaultValue : result;
	}
}
