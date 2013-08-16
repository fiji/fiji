/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

public class PathResult {

	protected float [] pathPoints;
	protected float [] numberOfPoints;
	protected String errorMessage;
	protected boolean succeeded;

	public float [] getPath() {
		return pathPoints;
	}

	public int getNumberOfPoints() {
		return pathPoints.length / 4;
	}

	public void setPath(float [] pathPoints) {
		this.pathPoints = pathPoints;
	}

	public void setErrorMessage(String message) {
		this.errorMessage = message;
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}

	public void setSuccess(boolean succeeded) {
		this.succeeded = succeeded;
	}

	public boolean getSuccess( ) {
		return this.succeeded;
	}

}
