#@ OpEnvironment ops
#@ Img (label = "Input binary image:", autofill = false) img
#@ String (label = "Structuring Element type:", choices = {"FOUR", "EIGHT"}, style = "listBox") se_type
#@output Img label_image

import net.imglib2.algorithm.labeling.ConnectedComponents.StructuringElement

se = StructuringElement.FOUR_CONNECTED
if (se_type == "EIGHT") {
  se = StructuringElement.EIGHT_CONNECTED
}
labeling = ops.op("labeling.cca").input(img, se).apply()
label_image = labeling.getIndexImg()
