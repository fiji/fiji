# Robert Bryson-Richardson and Albert Cardona 2010-10-08 at Estoril, Portugal
# EMBO Developmental Imaging course by Gabriel Martins
#
# Register time frames (stacks) to each other using Stitching_3D library
# to compute translations only, in all 3 spatial axes.
# Operates on a virtual stack.


from ij import VirtualStack, IJ, CompositeImage
from ij.process import ColorProcessor
from ij.io import DirectoryChooser
from ij.gui import YesNoCancelDialog
from mpicbg.imglib.image import ImagePlusAdapter
from mpicbg.imglib.algorithm.fft import PhaseCorrelation
from javax.vecmath import Point3i
from java.io import File, FilenameFilter

# imp stands for ij.ImagePlus instance

def compute_stitch(imp1, imp2):
  """ Compute a Point3i that expressed the translation of imp2 relative to imp1."""
  phc = PhaseCorrelation(ImagePlusAdapter.wrap(imp1), ImagePlusAdapter.wrap(imp2), 5, True)
  phc.process()
  return Point3i(phc.getShift().getPosition())


def extract_frame(imp, frame, channel):
  """ From a VirtualStack that is a hyperstack, contained in imp,
  extract the timepoint frame as an ImageStack, and return it.
  It will do so only for the given channel. """
  stack = imp.getStack() # multi-time point virtual stack
  vs = ImageStack(imp.width, imp.height, None)
  for s in range(1, imp.getNSlices()+1):
    i = imp.getStackIndex(channel, s, frame)
    vs.addSlice(str(s), stack.getProcessor(i))
  return vs

def compute_frame_translations(imp, channel):
  """ imp contains a hyper virtual stack, and we want to compute
  the X,Y,Z translation between every time point in it
  using the given preferred channel. """
  t1_vs = extract_frame(imp, 1, channel)
  shifts = []
  # store the first shift: between t1 and t2
  shifts.append(Point3i(0, 0, 0))
  # append the rest:
  IJ.showProgress(0)
  i = 1
  for t in range(2, imp.getNFrames()+1):
    t2_vs = extract_frame(imp, t, channel)
    shift = compute_stitch(ImagePlus("1", t1_vs), ImagePlus("2", t2_vs))
    shifts.append(shift)
    t1_vs = t2_vs
    IJ.showProgress(i / float(imp.getNFrames()))
    i += 1
  IJ.showProgress(1)
  return shifts

def concatenate_shifts(shifts):
  """ Take the shifts, which are relative to the previous shift,
  and sum them up so that all of them are relative to the first."""
  # the first shift is 0,0,0
  for i in range(2, len(shifts)): # we start at the third
    s0 = shifts[i-1]
    s1 = shifts[i]
    s1.x += s0.x
    s1.y += s0.y
    s1.z += s0.z
  return shifts

def compute_min_max(shifts):
  """ Find out the top left up corner, and the right bottom down corner,
  namely the bounds of the new virtual stack to create.
  Expects absolute shifts. """
  minx = Integer.MAX_VALUE
  miny = Integer.MAX_VALUE
  minz = Integer.MAX_VALUE
  maxx = -Integer.MAX_VALUE
  maxy = -Integer.MAX_VALUE
  maxz = -Integer.MAX_VALUE
  for shift in shifts:
    minx = min(minx, shift.x)
    miny = min(miny, shift.y)
    minz = min(minz, shift.z)
    maxx = max(maxx, shift.x)
    maxy = max(maxy, shift.y)
    maxz = max(maxz, shift.z)
  
  return minx, miny, minz, maxx, maxy, maxz

def zero_pad(num, digits):
  """ for 34, 4 --> '0034' """
  str_num = str(num)
  while (len(str_num) < digits):
    str_num = '0' + str_num
  return str_num


def create_registered_hyperstack(imp, target_folder, channel):
  """ Takes the imp, which contains a virtual hyper stack,
  and determines the x,y,z drift for each pair of time points,
  using the preferred given channel,
  and output one image for each slide into the target folder."""
  shifts = compute_frame_translations(imp, channel)
  # Make shifts relative to 0,0,0 of the original imp:
  shifts = concatenate_shifts(shifts)
  print "shifts concatenated:"
  for s in shifts:
    print s.x, s.y, s.z
  # Compute bounds of the new volume,
  # which accounts for all translations:
  minx, miny, minz, maxx, maxy, maxz = compute_min_max(shifts)
  # Make shifts relative to new canvas dimensions
  # so that the min values become 0,0,0
  for shift in shifts:
    shift.x -= minx
    shift.y -= miny
    shift.z -= minz
  print "shifts relative to new dimensions:"
  for s in shifts:
    print s.x, s.y, s.z
  # new canvas dimensions:
  width = imp.width + maxx - minx
  height = maxy - miny + imp.height
  slices = maxz - minz + imp.getNSlices()

  print "New dimensions:", width, height, slices
  # Count number of digits of each dimension, to output zero-padded numbers:
  slice_digits = len(str(slices))
  frame_digits = len(str(imp.getNFrames()))
  channel_digits = len(str(imp.getNChannels()))
  # List to accumulate all created names:
  names = []
  # Prepare empty slice to pad in Z when necessary
  empty = imp.getProcessor().createProcessor(width, height)
  # if it's RGB, fill the empty slice with blackness
  if isinstance(empty, ColorProcessor):
    empty.setValue(0)
    empty.fill()
  # Write all slices to files:
  stack = imp.getStack()
  for frame in range(1, imp.getNFrames()+1):
    shift = shifts[frame-1]
    fr = "t" + zero_pad(frame, frame_digits)
    # Pad with mpty slices before reaching the first slice
    for s in range(shift.z):
      ss = "_z" + zero_pad(s + 1, slice_digits) # slices start at 1
      for ch in range(1, imp.getNChannels()+1):
        name = fr + ss + "_c" + zero_pad(ch, channel_digits) +".tif"
        names.append(name)
        FileSaver(ImagePlus("", empty)).saveAsTiff(target_folder + "/" + name)
    # Add all proper slices
    for s in range(1, imp.getNSlices()+1):
      ss = "_z" + zero_pad(s + shift.z, slice_digits)
      for ch in range(1, imp.getNChannels()+1):
         ip = stack.getProcessor(imp.getStackIndex(ch, s, frame))
         ip2 = ip.createProcessor(width, height) # potentially larger
         ip2.insert(ip, shift.x, shift.y)
         name = fr + ss + "_c" + zero_pad(ch, channel_digits) +".tif"
         names.append(name)
         FileSaver(ImagePlus("", ip2)).saveAsTiff(target_folder + "/" + name)
    # Pad the end
    for s in range(shift.z + imp.getNSlices(), slices):
      ss = "_z" + zero_pad(s + 1, slice_digits)
      for ch in range(1, imp.getNChannels()+1):
        name = fr + ss + "_c" + zero_pad(ch, channel_digits) +".tif"
        names.append(name)
        FileSaver(ImagePlus("", empty)).saveAsTiff(target_folder + "/" + name)
  
  # Create virtual hyper stack with the result
  vs = VirtualStack(width, height, None, target_folder)
  for name in names:
    vs.addSlice(name)
  vs_imp = ImagePlus("registered time points", vs)
  vs_imp.setDimensions(imp.getNChannels(), len(names) / (imp.getNChannels() * imp.getNFrames()), imp.getNFrames())
  vs_imp.setOpenAsHyperStack(True)
  IJ.log("\nHyperstack dimensions: time frames:" + str(vs_imp.getNFrames()) + ", slices: " + str(vs_imp.getNSlices()) + ", channels: " + str(vs_imp.getNChannels()))
  if 1 == vs_imp.getNSlices():
    return vs_imp
  # Else, as composite
  mode = CompositeImage.COLOR;
  if isinstance(imp, CompositeImage):
    mode = imp.getMode()
  else:
    return vs_imp
  return CompositeImage(vs_imp, mode)

class Filter(FilenameFilter):
  def accept(self, folder, name):
    return not File(folder.getAbsolutePath() + "/" + name).isHidden()

def validate(target_folder):
  f = File(target_folder)
  if len(File(target_folder).list(Filter())) > 0:
    yn = YesNoCancelDialog(IJ.getInstance(), "Warning!", "Target folder is not empty! May overwrite files! Continue?")
    if yn.yesPressed():
      return True
    else:
      return False
  return True

def run():
  imp = IJ.getImage()
  if imp is None:
    return
  if not imp.isHyperStack():
    print "Not a hyper stack!"
    return
  if 1 == imp.getNFrames():
    print "There is only one time frame!"
    return
  if 1 == imp.getNSlices():
    print "To register slices of a stack, use 'Register Virtual Stack Slices'"
    return
  dc = DirectoryChooser("Choose target folder")
  target_folder = dc.getDirectory()
  if target_folder is None:
    return # user canceled the dialog
  if not validate(target_folder):
    return
  gd = GenericDialog("Options")
  channels = []
  for ch in range(1, imp.getNChannels()+1 ):
    channels.append(str(ch))
  gd.addChoice("channel:", channels, channels[0])
  gd.showDialog()
  if gd.wasCanceled():
    return
  channel = gd.getNextChoiceIndex() + 1  # zero-based
  vs_imp = create_registered_hyperstack(imp, target_folder, channel)
  vs_imp.show()

run()

