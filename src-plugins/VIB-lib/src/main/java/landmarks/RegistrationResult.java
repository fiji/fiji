/*  -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package landmarks;

public class RegistrationResult implements Comparable {

	int overlay_width;
	int overlay_height;
	int overlay_depth;

	/* These two sets of coordinates are indexes into the cropped
	 * images. */

	int fixed_point_x, fixed_point_y, fixed_point_z;
	int transformed_point_x, transformed_point_y, transformed_point_z;
	
	byte [][] transformed_bytes;
	byte [][] fixed_bytes;
	
	double score;
	double[] parameters;

	double pointMoved;
	
	/* These should now be world co-ordinates */
	double point_would_be_moved_to_x;
	double point_would_be_moved_to_y;
	double point_would_be_moved_to_z;
	
	public int compareTo(Object otherRegistrationResult) {
		RegistrationResult other = (RegistrationResult) otherRegistrationResult;
		return Double.compare(score, other.score);
	}
	
	@Override
	public String toString() {
		return "score: " + score + " for parameters: " + parameters;
	}

}
