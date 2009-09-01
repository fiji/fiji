package fiji.pluginManager.logic;

public class Dependency {
	private String filename;
	private String timestamp;
	private String relation = null;
	public static final String RELATION_AT_LEAST = "at-least";
	public static final String RELATION_AT_MOST = "at-most";
	public static final String RELATION_EXACT = "exact";

	public Dependency(String filename, String timestamp, String relation) {
		this.filename = filename;
		this.timestamp = timestamp;
		if (relation != null && !relation.trim().equals("")) {
			String parsed = relation.trim().toLowerCase();
			if (parsed.equals(RELATION_AT_LEAST) || parsed.equals(RELATION_AT_MOST)
					|| parsed.equals(RELATION_EXACT))
				this.relation = parsed;
		}
		//a "null" relation is the default ("at-least")
	}

	public String getFilename() {
		return filename;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public String getRelation() {
		return relation;
	}
}