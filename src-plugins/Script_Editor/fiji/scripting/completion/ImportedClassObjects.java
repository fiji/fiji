package fiji.scripting.completion;

// TODO: what does this class do?  Better name?  Or unnecessary?
public class ImportedClassObjects implements Comparable {

	String name;
	String className;
	String fullClassName;
	boolean isImported;

	public ImportedClassObjects(String itsname, String classname, String fullclassname, boolean imported) {
		name = itsname;
		className = classname;
		fullClassName = fullclassname;
		isImported = imported;
	}

	public ImportedClassObjects(String itsname, String classname) {
		this(itsname, classname,  null, false);
	}

	public ImportedClassObjects(String itsname) {
		this(itsname, null);
	}

	public void setClassName(String classname) {
		this.className = classname;
	}

	public void setFullClassName(String fullclassname) {
		this.fullClassName = fullclassname;
	}

	public void setIsImported(boolean set) {
		this.isImported = set;
	}

	public String getCompleteClassName() {
		return fullClassName;
	}
	public int compareTo(Object o) {
		ImportedClassObjects other = (ImportedClassObjects)o;
		int i = name.compareTo(other.name);
		return i != 0 ? i : className.compareTo(other.className);
	}
}
