package results;

/**
 * A class representing a warning, combining a short and
 * a long message. Typically Algorithms can produce such
 * warnings if they find problems with the input data.
 */
public class Warning
{
	private String shortMessage;
	private String longMessage;

	public Warning(String shortMessage, String longMessage)
	{
		this.shortMessage = shortMessage;
		this.longMessage = longMessage;
	}

	public String getShortMessage() {
		return shortMessage;
	}

	public String getLongMessage() {
		return longMessage;
	}

}