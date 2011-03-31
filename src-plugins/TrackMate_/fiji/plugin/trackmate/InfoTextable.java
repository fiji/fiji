package fiji.plugin.trackmate;

public interface InfoTextable {

	/**
	 * Return a String containing a descriptive information about this instance. 
	 * <p>
	 * The string should be an html string, that starts with <<code>html</code>> tag and
	 * ends with its counterpart.
	 */
	public String getInfoText();

}
