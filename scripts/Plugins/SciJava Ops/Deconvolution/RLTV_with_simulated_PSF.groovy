#@ OpEnvironment ops
#@ UIService ui
#@ Img (label = "Input image:", autofill = false) img
#@ Integer (label = "Iterations", value = 15) iterations
#@ Float (label = "Numerical Aperture", style = "format:0.00", min = 0.00, value = 1.45) numericalAperture
#@ Integer (label = "Emission Wavelength (nm)", value = 457) wavelength
#@ Float (label = "Refractive Index (immersion)", style = "format:0.00", min = 0.00, value = 1.5) riImmersion
#@ Float (label = "Refractive Index (sample)", style = "format:0.00", min = 0.00, value = 1.4) riSample
#@ Float (label = "XY spacing (um/pixel)", style = "format:0.0000", min = 0.0000, value = 0.065) xySpacing
#@ Float (label = "Z spacing (um/pixel)", style = "format:0.0000", min = 0.0000, value = 0.1) zSpacing
#@ Float (label="Particle/sample Position (um)", style = "format:0.0000", min = 0.0000, value = 0) zPos
#@ Float (label = "Regularization factor", style = "format:0.00000", min = 0.00000, value = 0.002) regFactor
#@ Boolean (label = "Show PSF", value = false) showPSF
#@output Img result

// Richardson-Lucy Total Variation deconvolution with a simulated point spread function.
//
// This script utilizes a SciJava Ops implementation of Richardson-Lucy Total Variation (RLTV)
// deconvolution as described by Dey et al. 2006 and a simulated point spread function (PSF)
// using the Gibson-Lanni model.
//
// Arguments
//    * `img`: A 3-dimensional image with known lateral (x and y) and axial (z) spacing.
//    * `iterations`: The number of iterations to perform (default = 15).
//    * `numerical_aperture`: The numerical aperature (NA) of the objective used.
//    * `wavelength`: The emission wavelength in nanometers (nm) of the image.
//    * `ri_immersion`: The refractive index of immersion medium (air, oil, etc...).
//    * `ri_sample`: The refractive index of the sample, a measured value (default = 1.4)
//    * `lateral_spacing`: The X and Y pixel spacing in μm/pixel of the image.
//    * `axial_spacing`: The Z spacing in μm/pixel of the image.
//    * `p_z`: The position of the sample from the coverslip.
//    * `reg_factor`: The regularization factor.
//    * `show_psf`: Optionally display the simulated PSF.
//
// Returns
//    * `result`: The deconvolved data.
//    * `PSF`: The simulated PSF (optional).
//
// Reference
//
// https://doi.org/10.1002/jemt.20294

import net.imglib2.type.numeric.complex.ComplexFloatType
import net.imglib2.type.numeric.real.FloatType

wavelength = (wavelength * 1E-9) as double
xySpacing = xySpacing * 1E-6
zSpacing = zSpacing * 1E-6
zPos = zPos * 1E-6
psf = ops.op("create.kernelDiffraction").input(img,
                                               numericalAperture,
                                               wavelength,
                                               riSample,
                                               riImmersion,
                                               xySpacing,
                                               zSpacing,
                                               zPos,
                                               new FloatType()).apply()
img_f32 = ops.op("create.img").input(img, new FloatType()).apply()
ops.op("convert.float32").input(img).output(img_f32).compute()
result = ops.op("deconvolve.richardsonLucyTV").input(img_f32,
                                                     psf,
                                                     new FloatType(),
                                                     new ComplexFloatType(),
                                                     iterations,
                                                     false,
                                                     false,
                                                     regFactor).apply()
if (showPSF) {
  ui.show("Simulated PSF", psf)
}                                                    
