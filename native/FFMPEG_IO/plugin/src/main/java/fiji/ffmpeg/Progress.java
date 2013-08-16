package fiji.ffmpeg;

public interface Progress {
	public void start(String message);
	public void step(String message, double progress);
	public void done(String message);
	public void log(String message);
}
