package fiji.scripting;

import BSH.Refresh_BSH_Scripts;
import Clojure.Refresh_Clojure_Scripts;
import JRuby.Refresh_JRuby_Scripts;
import Jython.Refresh_Jython_Scripts;
import Javascript.Refresh_Javascript_Scripts;
import common.RefreshScripts;
import fiji.scripting.java.Refresh_Javas;
import javax.swing.JRadioButtonMenuItem;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

public class Languages {
	protected Language[] languages;
	protected Map<String, Language> map;

	protected Languages() {
		languages = new Language[] {
		        new Language(".java", SyntaxConstants.SYNTAX_STYLE_JAVA, "Java", KeyEvent.VK_J, new Refresh_Javas()),
		        new Language(".js", SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT, "Javascript", KeyEvent.VK_S, new Refresh_Javascript_Scripts()),
		        new Language(".py", SyntaxConstants.SYNTAX_STYLE_PYTHON, "Python", KeyEvent.VK_P, new Refresh_Jython_Scripts()),
		        new Language(".rb", SyntaxConstants.SYNTAX_STYLE_RUBY, "Ruby", KeyEvent.VK_R, new Refresh_JRuby_Scripts()),
		        new Language(".clj", null, "Clojure", KeyEvent.VK_C, new Refresh_Clojure_Scripts()),
		        new Language(".m", null, "Matlab", KeyEvent.VK_M, null),
		        new Language(".bsh", SyntaxConstants.SYNTAX_STYLE_JAVA, "BeanShell", KeyEvent.VK_B, new Refresh_BSH_Scripts()),
		        new Language("", SyntaxConstants.SYNTAX_STYLE_NONE, "None", KeyEvent.VK_N, null)
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
		return get(extension).interpreter;
	}

	/* The class keeps information about particular language */
	public class Language {
		String extension;
		String syntaxStyle;
		String menuLabel;
		int shortCut;
		RefreshScripts interpreter;

		JRadioButtonMenuItem item;

		Language(String extension, String style, String label, int shortCut, RefreshScripts interpreter) {
			this.extension = extension;
			syntaxStyle = style;
			menuLabel = label;
			this.shortCut = shortCut;
			this.interpreter = interpreter;
		}
	}
}
