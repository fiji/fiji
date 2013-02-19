package fiji.build;

// our very own exception

public class FakeException extends Exception {
	public static final long serialVersionUID = 1;
	public FakeException(String message) {
		super(message);
	}

	public FakeException(String message, Throwable cause) {
		super(message, cause);
	}

	public String toString() {
		return getMessage();
	}
}