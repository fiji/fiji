package ij3d;

import javax.media.j3d.View;

public interface UniverseListener {

	public void transformationStarted(View view);
	public void transformationUpdated(View view);
	public void transformationFinished(View view);

	public void contentAdded(Content c);
	public void contentRemoved(Content c);
	public void contentChanged(Content c);
	public void contentSelected(Content c);
	public void canvasResized();
	public void universeClosed();
}
