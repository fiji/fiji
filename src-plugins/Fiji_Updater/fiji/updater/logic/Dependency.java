package fiji.updater.logic;

public class Dependency {
	public String filename;
	public long timestamp;
	public boolean overrides;

	public Dependency(String filename, long timestamp, boolean overrides) {
		this.filename = filename;
		this.timestamp = timestamp;
		this.overrides = overrides;
	}

	public String toString() {
		return filename;
	}
}
