package siox;

import java.io.Serializable;

public class SegmentationInfo implements Serializable 
{

	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = -87458916146804009L;
	/** background color signature */
	private float[][] bgSignature = null;
	/** foreground color signature */
	private float[][] fgSignature = null;
	/** number of smoothing steps in the post processing */
	private int smoothness = 0;
	/** Segmentation retains the largest connected
	 *  foreground component plus any component with size at least
	 *  <CODE>sizeOfLargestComponent/sizeFactorToKeep</CODE>*/
	private double sizeFactorToKeep = 0; 
	
	/**
	 * Basic constructor 
	 * 
	 * @param bgSignature background color signature
	 * @param fgSignature foreground color signature
	 * @param smoothness smoothing steps in the post processing
	 * @param sizeFactorToKeep size factor for keeping objects
	 */
	public SegmentationInfo(
			float[][] bgSignature,
			float[][] fgSignature,
			int smoothness,
			double sizeFactorToKeep)
	{
		this.bgSignature = bgSignature;
		this.fgSignature = fgSignature;
		this.smoothness = smoothness;
		this.sizeFactorToKeep = sizeFactorToKeep;
	}
	
	public void setBgSignature(float[][] bgSignature) {
		this.bgSignature = bgSignature;
	}
	public float[][] getBgSignature() {
		return bgSignature;
	}
	public void setFgSignature(float[][] fgSignature) {
		this.fgSignature = fgSignature;
	}
	public float[][] getFgSignature() {
		return fgSignature;
	}
	public void setSmoothness(int smoothness) {
		this.smoothness = smoothness;
	}
	public int getSmoothness() {
		return smoothness;
	}
	public void setSizeFactorToKeep(double sizeFactorToKeep) {
		this.sizeFactorToKeep = sizeFactorToKeep;
	}
	public double getSizeFactorToKeep() {
		return sizeFactorToKeep;
	}
}
