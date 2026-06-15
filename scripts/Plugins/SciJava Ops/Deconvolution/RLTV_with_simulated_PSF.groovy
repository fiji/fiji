#@ OpEnvironment ops
#@ UIService ui
#@ Img (label = "Input image:", autofill = false) img
#@ String (visibility = MESSAGE, value ="<b>[ Gibson-Lanni PSF settings ]</b>", required = false) psf_msg
#@ Float (label = "Numerical Aperture", style = "format:0.00", min = 0.00, value = 1.45) numericalAperture
#@ Integer (label = "Emission Wavelength (nm)", value = 457) wavelength
#@ Float (label = "Refractive Index (immersion)", style = "format:0.00", min = 0.00, value = 1.5) riImmersion
#@ Float (label = "Refractive Index (sample)", style = "format:0.00", min = 0.00, value = 1.4) riSample
#@ Float (label = "XY spacing (um/pixel)", style = "format:0.0000", min = 0.0000, value = 0.065) xySpacing
#@ Float (label = "Z spacing (um/pixel)", style = "format:0.0000", min = 0.0000, value = 0.1) zSpacing
#@ Float (label="Particle/sample Position (um)", style = "format:0.0000", min = 0.0000, value = 0) zPos
#@ String (visibility = MESSAGE, value ="<b>[ Richardson-Lucy TV settings ]</b>", required = false) rltv_msg
#@ Integer (label = "Iterations", value = 15) iterations
#@ Float (label = "Regularization factor", style = "format:0.00000", min = 0.00000, value = 0.002) regFactor
#@ Boolean (label = "Use non-circulant edge handling", value = false) nonCirc
#@ Boolean (label = "Use acceleration", value = false) acc
#@ String (visibility = MESSAGE, value ="<b>[ Output settings ]</b>", required = false) rltv_msg
#@ Boolean (label = "Show PSF", value = false) showPSF
#@output Img result

// Richardson-Lucy Total Variation deconvolution with a simulated point spread
// function (PSF).
//
// This script utilizes a SciJava Ops implementation of Richardson-Lucy Total
// Variation (RLTV) deconvolution as described by Dey et al. 2006 and a
// simulated point spread function (PSF) using the Gibson-Lanni model.
//
// Reference:
// <https://doi.org/10.1002/jemt.20294>
// <https://doi.org/10.1364/josaa.9.000154>

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
                                                     nonCirc,
                                                     acc,
                                                     regFactor).apply()
if (showPSF) {
  ui.show("Simulated PSF", psf)
}                                                    
