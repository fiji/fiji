# This is an example that find all the image.bin.gz files from channel
# 01 under a directory and makes an image stack of all the slices
# numbered 23.  If this might be useful to you, copy the file and
# customize the bits preceded with "CHANGEME":

include_class 'util.BatchOpener'

# CHANGEME:
directory = '/Volumes/LaCie/corpus/central-complex/biorad/reformatted'

# CHANGEME:
match  =  /01_warp/

# CHANGEME:
def filename_to_slicename(f)
  # Take the name of the parent directory:
  f.gsub( /\/([^\/]+)\/[^\/]+$/, '\1' )
end

unless FileTest.directory? directory
  error = "Couldn't find directory '#{directory}" +
    "You probably need to customize the script."
  ij.IJ.error error
  exit(-1)  
end

images = `find #{directory} -name 'image.bin.gz' -print0`.split("\0")
images = images.grep

if images.empty?
  ij.IJ.error "No images found.  You probably need to customize the script."
  exit(-1)
end

slice_to_get = 23

width  = -1
height = -1
depth  = -1

stack = nil

images.each do |f|

  i = BatchOpener.openFirstChannel f
  
  if width < 0
    width  = i.getWidth
    height = i.getHeight
    depth  = i.getStackSize
    stack = ij.ImageStack.new width, height
  else
    unless
        width = i.getWidth && 
        height == i.getHeight &&
        depth == i.getStackSize
      ij.IJ.error "The image #{i.getTitle} didn't have matching dimensions"
      exit(-1)
    end
  end

  slice_name = filename_to_slicename f
  processor = i.getStack.getProcessor( slice_to_get )
  stack.addSlice slice_name, processor
            
	i.close

end

result = ImagePlus.new "All slices numbered #{slice_to_get}", stack
result.show
