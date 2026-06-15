#@ OpEnvironment ops
#@ UIService ui
#@ Img (label = "Input image:", autofill = false) img
#@ Integer (label = "Iterations", value = 15) iterations
#@ Float (label = "Numerical Aperture", style = "format:0.00", min = 0.00, value = 1.45) numericalAperture
#@ Integer (label = "Emission Wavelength (nm)", value = 457) wavelength
#@ Float (label = "Refractive Index (immersion)", style = "format:0.00", min = 0.00, value = 1.5) riImmersion
#@ Float (label = "Refractive Index (sample)", style = "format:0.00", min = 0.00, value = 1.4) riSample
#@ Float (label = "XY spacing (um/pixel)", style = "format:0.0000", min = 0.0000, value = 0.065) lateral_res
#@ Float (label = "Z spacing (um/pixel)", style = "format:0.0000", min = 0.0000, value = 0.1) axial_res
#@ Float (label="Particle/sample Position (um)", style = "format:0.0000", min = 0.0000, value = 0) pZ
#@ Float (label = "Regularization factor", style = "format:0.00000", min = 0.00000, value = 0.002) regularizationFactor
#@ Boolean (label = "Show PSF", value = false) show_psf
#@output Img result

# Richardson-Lucy Total Variation deconvolution with a simulated point spread function.
#
# This script utilizes a SciJava Ops implementation of Richardson-Lucy Total Variation (RLTV)
# deconvolution as described by Dey et al. 2006 and a simulated point spread function (PSF)
# using the Gibson-Lanni model.
#
# Arguments
#    * `img`: A 3-dimensional image with known lateral (x and y) and axial (z) spacing.
#    * `iterations`: The number of iterations to perform (default = 15).
#    * `numerical_aperture`: The numerical aperature (NA) of the objective used.
#    * `wavelength`: The emission wavelength in nanometers (nm) of the image.
#    * `ri_immersion`: The refractive index of immersion medium (air, oil, etc...).
#    * `ri_sample`: The refractive index of the sample, a measured value (default = 1.4)
#    * `lateral_spacing`: The X and Y pixel spacing in μm/pixel of the image.
#    * `axial_spacing`: The Z spacing in μm/pixel of the image.
#    * `p_z`: The position of the sample from the coverslip.
#    * `reg_factor`: The regularization factor.
#    * `show_psf`: Optionally display the simulated PSF.
#
# Returns
#    * `result`: The deconvolved data.
#    * `PSF`: The simulated PSF (optional).
#
# Reference
#
# https://doi.org/10.1002/jemt.20294

from net.imglib2 import FinalDimensions
from net.imglib2.type.numeric.real import FloatType
from net.imglib2.type.numeric.complex import ComplexFloatType
from java.lang import Float

# convert input image to float
img_float = ops.op("create.img").input(img, FloatType()).apply()
ops.op("convert.float32").input(img).output(img_float).compute()

# use image dimensions for PSF size
psf_size = FinalDimensions(img.dimensionsAsLongArray())

# convert the input parameters to meters (m)
wavelength = float(wavelength) * 1E-9
lateral_res = lateral_res * 1E-6
axial_res = axial_res * 1E-6
pZ = pZ * 1E-6

# create the synthetic PSF
psf = ops.op("create.kernelDiffraction").input(psf_size,
                                                        numericalAperture,
                                                        wavelength,
                                                        riSample,
                                                        riImmersion,
                                                        lateral_res,
                                                        axial_res,
                                                        pZ,
                                                        FloatType()).apply()

# deconvolve image
result = ops.op("deconvolve.richardsonLucyTV").input(img_float, psf, FloatType(), ComplexFloatType(), iterations, False, False, Float(regularizationFactor)).apply()

# optionally show PSF
if show_psf:
    ui.show("PSF", psf)
