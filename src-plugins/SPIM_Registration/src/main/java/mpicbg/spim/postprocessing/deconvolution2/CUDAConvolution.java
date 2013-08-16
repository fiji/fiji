package mpicbg.spim.postprocessing.deconvolution2;

import com.sun.jna.Library;

public interface CUDAConvolution extends Library 
{
	/*
	__declspec(dllexport) imageType* convolution3DfftCUDA(imageType* im,int* imDim,imageType* kernel,int* kernelDim,int devCUDA);
	__declspec(dllexport) int getCUDAcomputeCapabilityMinorVersion(int devCUDA);
	__declspec(dllexport) int getCUDAcomputeCapabilityMajorVersion(int devCUDA);
	__declspec(dllexport) int getNumDevicesCUDA();
	__declspec(dllexport) char* getNameDeviceCUDA(int devCUDA);
	__declspec(dllexport) long long int getMemDeviceCUDA(int devCUDA);
	 */
	
	public float[] convolution3DfftCUDA( float[] im, int[] imDim, float[] kernel, int[] kernelDim, int devCUDA );
	public void convolution3DfftCUDAInPlace( float[] im, int[] imDim, float[] kernel, int[] kernelDim, int devCUDA );
	public int getCUDAcomputeCapabilityMinorVersion(int devCUDA);
	public int getCUDAcomputeCapabilityMajorVersion(int devCUDA);
	public int getNumDevicesCUDA();
	public void getNameDeviceCUDA(int devCUDA, byte[] name);
	public long getMemDeviceCUDA(int devCUDA);
}
