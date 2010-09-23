package fiji.scripting;

import BSH.Refresh_BSH_Scripts;

import CLI.Refresh_Macros;

import Clojure.Refresh_Clojure_Scripts;

import JRuby.Refresh_JRuby_Scripts;

import Javascript.Refresh_Javascript_Scripts;

import Jython.Refresh_Jython_Scripts;

import common.RefreshScripts;

import fiji.scripting.java.Refresh_Javas;

import java.awt.event.KeyEvent;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JRadioButtonMenuItem;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

public class Languages {
	protected Language[] languages;
	protected Map<String, Language> map;

	protected Languages() {
		languages = new Language[] {
		        new Language(".java", SyntaxConstants.SYNTAX_STYLE_JAVA, "Java", KeyEvent.VK_J, Refresh_Javas.class, true, true),
		        new Language(".js", SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT, "Javascript", KeyEvent.VK_S, Refresh_Javascript_Scripts.class, false, false),
		        new Language(".py", SyntaxConstants.SYNTAX_STYLE_PYTHON, "Python", KeyEvent.VK_P, Refresh_Jython_Scripts.class, false, false),
		        new Language(".rb", SyntaxConstants.SYNTAX_STYLE_RUBY, "Ruby", KeyEvent.VK_R, Refresh_JRuby_Scripts.class, false, false),
		        new Language(".clj", null, "Clojure", KeyEvent.VK_C, Refresh_Clojure_Scripts.class, false, false),
		        /* new Language(".m", null, "Matlab", KeyEvent.VK_M, null, false, false), */
		        new Language(".bsh", SyntaxConstants.SYNTAX_STYLE_JAVA, "BeanShell", KeyEvent.VK_B, Refresh_BSH_Scripts.class, false, false),
		        new Language(".ijm", null, "ImageJ Macro", KeyEvent.VK_I, Refresh_Macros.class, false, false),
		        new Language("", SyntaxConstants.SYNTAX_STYLE_NONE, "None", KeyEvent.VK_N, null, false, false)
		};

		map = new HashMap<String, Language>();
		for (Language language : languages)
			map.put(language.extension, language);
	}

	protected static Languages instance;

	public static Languages getInstance() {
		if (instance == null)
			instance = new Languages();
		return instance;
	}

	public Language getLanguage(String label) {
		for (Language language : languages)
			if (label.equals(language.menuLabel))
				return language;
		return get("");
	}

	public static Language get(String extension) {
		Languages languages = getInstance();
		return languages.map.get(languages.map.containsKey(extension) ?
			extension : "");
	}

	public String getSyntaxStyle(String extension) {
		return get(extension).syntaxStyle;
	}

	public String getMenuEntry(String extension) {
		return get(extension).menuLabel;
	}

	public RefreshScripts getInterpreter(String extension) {
		return get(extension).newInterpreter();
	}

	/* The class keeps information about particular language */
	public class Language {
		String extension;
		String syntaxStyle;
		String menuLabel;
		int shortCut;
		Class<? extends RefreshScripts> interpreter_class;
		boolean debuggable, compileable;

		JRadioButtonMenuItem item;

		Language(String extension, String style, String label,
				int shortCut, Class<? extends RefreshScripts> interpreter_class,
				boolean isDebuggable, boolean isCompileable) {
			this.extension = extension;
			syntaxStyle = style;
			menuLabel = label;
			this.shortCut = shortCut;
			this.interpreter_class = interpreter_class;
			debuggable = isDebuggable;
			compileable = isCompileable;
		}

		boolean isRunnable() {
			return interpreter_class != null;
		}

		// TODO: add a proper interface so we can add other debuggers
		boolean isDebuggable() {
			return debuggable;
		}

		boolean isCompileable() {
			return compileable;
		}

		RefreshScripts newInterpreter() {
			if (null == interpreter_class)
				return null;
			try {
				return interpreter_class.newInstance();
			} catch (InstantiationException ie) {
				ie.printStackTrace();
			} catch (IllegalAccessException iae) {
				iae.printStackTrace();
			}
			return null;
		}

		public String toString() {
			return "(" + extension + "; interpreter: "
				+ (interpreter_class == null ? "<none>" :
				   interpreter_class.getSimpleName()) + "; "
				+ (isCompileable() ? "" : "not ")
				+ "compileable; "
				+ (isDebuggable() ? "" : "not ")
				+ "debuggable)";
		}
	}
}
