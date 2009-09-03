package fiji.pluginManager.logic;

public class Dependency {
        public enum Relation { AT_LEAST, AT_MOST, EXACT };

        public static Relation getRelation(String label) {
                return Relation.valueOf(label.toUpperCase().replace('-', '_'));
        }

	public String filename;
	public long timestamp;
	public Relation relation;

	public Dependency(String filename, long timestamp, String relation) {
		this(filename, timestamp, getRelation(relation));
	}

	public Dependency(String filename, long timestamp, Relation relation) {
		this.filename = filename;
		this.timestamp = timestamp;
		this.relation = relation;
	}
}
