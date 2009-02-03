# The functions in this file are reimplementations of the functions
# built into the classic ImageJ macro language.  These are provided
# to help in porting exisiting ImageJ macros to JRuby scripts.
# Please help to fill in the unimplemented functions!  There is a
# large list of these (commented out) at the end of this file.

# ------------------------------------------------------------------------

# There are advantages to both being able to write idiomatic Ruby
# ("example_method_name") and the "lowerCamelCase" form used in the macro
# language's definitions ("exampleMethodName"), so use this helper method
# to alias each of the replacement macro definition functions to one
# with the macro language's name for it, so the programmer has the option
# to use either.

def alias_to_lower_camel_case(method_name)
  new_method_name = method_name.gsub( /(_(.))/ ) do |m|
    $2.upcase
  end
  eval "alias #{new_method_name} #{method_name}"
end

# ------------------------------------------------------------------------
# FIXME: The error handling here is a bit inconsistent.
#   ImageJ macros seem to do one of the following:
#     - throw a RuntimeException(Macro.MACRO_CANCELED)
#     - call interp.exit()
#   At the moment the Ruby equivalents do raise "Error message"
# This needs a bit more work, obviously...
# ........................................................................

# The macro language uses a number of implicit global variables to
# track the current image, etc.:

$macro_auto_update = true

def reset_image
  $macro_default_image = nil
  $macro_default_processor = nil

  $macro_color_set = false
  $macro_font_set = false
end

alias_to_lower_camel_case "reset_image"

# ........................................................................

def select_window(title)
  ij.IJ.selectWindow title
end

alias_to_lower_camel_case "select_window"

# ........................................................................

def get_image
  unless $macro_default_image
    $macro_default_image = ij.IJ.getImage
    unless $macro_default_image
      raise "No image"
      nil
    end
    #		if (defaultImp.getWindow()==null && IJ.getInstance()!=null && !interp.isBatchMode() && WindowManager.getTempCurrentImage()==null)
    #			throw new RuntimeException(Macro.MACRO_CANCELED);
  end
  return $macro_default_image  
end

alias_to_lower_camel_case "get_image"

# ........................................................................

def get_processor
  if $macro_default_processor
    $macro_default_processor
  else
    $macro_default_image = get_image
    $macro_default_processor = $macro_default_image.getProcessor
  end
end

alias_to_lower_camel_case "get_processor"

# ........................................................................

def requires(version)
  if ij.IJ.versionLessThan version
    raise "Version mismatch"
  end
end

# ........................................................................

def run( command, argument = nil )
  if argument
    ij.IJ.run command, argument
  else
    ij.IJ.run command
  end
end

# ........................................................................

def open( path = nil )
  if path
    ij.IJ.open(path)
  else
    ij.IJ.open
  end
  reset_image
end

# ........................................................................

def set_threshold( minimum, maximum )
  ij.IJ.setThreshold minimum, maximum
end

alias_to_lower_camel_case "set_threshold"

# ........................................................................

def get_results_count
  ij.plugin.filter.Analyzer.getResultsTable.getCounter
end

alias_to_lower_camel_case "get_results_count"

def n_results
  get_results_count
end

alias_to_lower_camel_case "n_results"

# ........................................................................

def get_result( label, row = -1 )
  rt = ij.plugin.filter.Analyzer.getResultsTable
  c = rt.getCounter
  if c == 0
    raise "\"Results\" table empty"
  end
  if row == -1
    row = c - 1
  end
  if row < 0 || row >= c
    raise "Row (#{row}) out of range"
  end
  column = rt.getColumnIndex label
  if rt.columnExists column
    rt.getValueAsDouble column, row
  else
    # java.lang.Double.NaN
    nil
  end
end

alias_to_lower_camel_case "get_result"

# ........................................................................

def do_wand( x, y )
  ij.IJ.doWand x, y
  reset_image
end

alias_to_lower_camel_case "do_wand"

# ........................................................................

def show_progress( a, b = nil )
  imagej = ij.IJ.getInstance
  progress_bar = nil
  if imagej
    progress_bar = imagej.getProgressBar
  end
  if progress_bar
    if b
      progress_bar.show( (a + 1.0) / b, true )
    else      
      progress_bar.show a, true
    end
    $macro_showing_progress = true 
  end
end

alias_to_lower_camel_case "show_progress"

# ........................................................................

def get_selection_coordinates
  reset_image
  imp = ij.IJ.getImage();
	roi = imp.getRoi
  unless roi
    raise "Selection required"
  end
  p = roi.getPolygon
  x_coordinates = p.xpoints.dup
  y_coordinates = p.ypoints.dup
  return x_coordinates, y_coordinates
end

alias_to_lower_camel_case "get_selection_coordinates"

# ........................................................................

def set_foreground_color_processor(p)
  if $macro_default_color
    p.setColor $macro_default_color
  elsif $macro_default_value
    p.setValue $macro_default_value
  else
    p.setColor ij.gui.Toolbar.getForegroundColor()
    $macro_color_set = true
  end
end

alias_to_lower_camel_case "set_foreground_color_processor"

def set_foreground_color(r,g,b)
  ij.IJ.setForegroundColor Integer(r), Integer(g), Integer(b)
  reset_image
  $macro_default_color = nil
  # $macro_default_value = java.lang.Double.NaN
  $macro_default_value = nil
end

alias_to_lower_camel_case "set_foreground_color"

def set_background_color(r,g,b)
  ij.IJ.setBackgroundColor Integer(r), Integer(g), Integer(b)
  reset_image
end

alias_to_lower_camel_case "set_background_color"

# ........................................................................

def snapshot
  ip = get_processor
  ip.snapshot
end

# ........................................................................

def get_width
  get_image.get_width  
end

alias_to_lower_camel_case "get_width"

# ........................................................................

def get_height
  get_image.get_height  
end

alias_to_lower_camel_case "get_height"

# ........................................................................

def auto_update(b)
  $macro_auto_update = b
end

alias_to_lower_camel_case "auto_update"

# ........................................................................

def update_display
  if $macro_update_needed && ij.WindowManager.getImageCount > 0
    imp = getImage
    imp.updateAndDraw
    $macro_update_needed = false;
  end
end

alias_to_lower_camel_case "update_display"

# ........................................................................

def reset
  ip = get_processor
  ip.reset
  $macro_update_needed = true
end

# ........................................................................

def wait(delay)
  ij.IJ.wait Integer(delay)
end

# ........................................................................

def move_to(x,y)
  # The 0.5 here is copied from the source in Functions.java - I don't
  # necessarily think this is sensible...
  get_processor.moveTo Integer( x + 0.5 ), Integer( y + 0.5 )
end

alias_to_lower_camel_case "move_to"

# ........................................................................

def line_to(x,y)
  # The 0.5 here is copied from the source in Functions.java - I don't
  # necessarily think this is sensible...
  p = get_processor
  x2 = Integer( x + 0.5 )
  y2 = Integer( y + 0.5 )
  unless $macro_color_set
    set_foreground_color_processor p
  end
  p.lineTo x2, y2
  update_and_draw $macro_default_image
end

alias_to_lower_camel_case "line_to"

# ........................................................................

def draw_line(x1,y1,x2,y2)
  # The 0.5 here is copied from the source in Functions.java - I don't
  # necessarily think this is sensible...
  p = get_processor
  x1 = Integer( x1 + 0.5 )
  y1 = Integer( y1 + 0.5 )
  x2 = Integer( x2 + 0.5 )
  y2 = Integer( y2 + 0.5 )
  unless $macro_color_set
    set_foreground_color_processor p
  end
  p.drawLine(x1,y1,x2,y2)
  update_and_draw $macro_default_image
end

alias_to_lower_camel_case "draw_line"

# ........................................................................

def update_and_draw(image)
  if $macro_auto_update
    image.updateChannelAndDraw
  else
    $macro_update_needed = true
  end
end

alias_to_lower_camel_case "update_and_draw"

# ........................................................................

def beep
  ij.IJ.beep
end

# ........................................................................

def reset_min_and_max
  ij.IJ.resetMinAndMax
  reset_image
end

alias_to_lower_camel_case "reset_min_and_max"

# ........................................................................

def reset_threshold
  ij.IJ.resetThreshold
  reset_image
end

alias_to_lower_camel_case "reset_threshold"

# ........................................................................

def set_min_max(min,max)
  ij.IJ.setMinAndMax min, max
  reset_image
end

alias_to_lower_camel_case "set_min_max"

# ........................................................................
# I think these are all the ImageJ macro functions we would have to
# implement to be completely equivalent.  I have prefixed with "+"
# those which I have already written.
# 
# To get a quick summary of how we're doing, run:
# 
# : echo Implemented: `egrep '^#  .*:.*:' imagej.rb | wc -l`
# : echo Missing: `egrep '^# \+.*:.*:' imagej.rb | wc -l`
# 
# -----------------------------------------------------------------------
# +RUN=300:"run":RUN
#  INVERT=301:"invert":INVERT
# +SELECT=302:"selectWindow":SELECT
# +WAIT=303:"wait":WAIT
# +BEEP=304:"beep":BEEP
# +RESET_MIN_MAX=305:"resetMinAndMax":RESET_MIN_MAX
# +RESET_THRESHOLD=306:"resetThreshold":RESET_THRESHOLD
#  PRINT=307:"print":PRINT
#  WRITE=308:"write":WRITE
# +DO_WAND=309:"doWand":DO_WAND
# +SET_MIN_MAX=310:"setMinAndMax":SET_MIN_MAX
# +SET_THRESHOLD=311:"setThreshold":SET_THRESHOLD
#  SET_TOOL=312:"setTool":SET_TOOL
# +SET_FOREGROUND=313:"setForegroundColor":SET_FOREGROUND
# +SET_BACKGROUND=314:"setBackgroundColor":SET_BACKGROUND
#  MAKE_LINE=315:"makeLine":MAKE_LINE
#  MAKE_OVAL=316:"makeOval":MAKE_OVAL
#  MAKE_RECTANGLE=317:"makeRectangle":MAKE_RECTANGLE
#  DUMP=318:"dump":DUMP
# +MOVE_TO=319:"moveTo":MOVE_TO
# +LINE_TO=320:"lineTo":LINE_TO
# +DRAW_LINE=321:"drawLine":DRAW_LINE
# +REQUIRES=322:"requires":REQUIRES
# +AUTO_UPDATE=323:"autoUpdate":AUTO_UPDATE
# +UPDATE_DISPLAY=324:"updateDisplay":UPDATE_DISPLAY
#  DRAW_STRING=325:"drawString":DRAW_STRING
#  SET_PASTE_MODE=326:"setPasteMode":SET_PASTE_MODE
#  DO_COMMAND=327:"doCommand":DO_COMMAND
#  SHOW_STATUS=328:"showStatus":SHOW_STATUS
# +SHOW_PROGRESS=329:"showProgress":SHOW_PROGRESS
#  SHOW_MESSAGE=330:"showMessage":SHOW_MESSAGE
#  PUT_PIXEL=331:"putPixel":PUT_PIXEL
#  SET_PIXEL=332:"setPixel":SET_PIXEL
# +SNAPSHOT=333:"snapshot":SNAPSHOT
# +RESET=334:"reset":RESET
#  FILL=335:"fill":FILL
#  SET_COLOR=336:"setColor":SET_COLOR
#  SET_LINE_WIDTH=337:"setLineWidth":SET_LINE_WIDTH
#  CHANGE_VALUES=338:"changeValues":CHANGE_VALUES
#  SELECT_IMAGE=339:"selectImage":SELECT_IMAGE
#  EXIT=340:"exit":EXIT
#  SET_LOCATION=341:"setLocation":SET_LOCATION
#  GET_CURSOR_LOC=342:"getCursorLoc":GET_CURSOR_LOC
#  GET_LINE=343:"getLine":GET_LINE
#  GET_VOXEL_SIZE=344:"getVoxelSize":GET_VOXEL_SIZE
#  GET_HISTOGRAM=345:"getHistogram":GET_HISTOGRAM
#  GET_STATISTICS=346:"getStatistics":GET_STATISTICS
#  GET_BOUNDING_RECT=347:"getBoundingRect":GET_BOUNDING_RECT
#  GET_LUT=348:"getLut":GET_LUT
#  SET_LUT=349:"setLut":SET_LUT
# +GET_COORDINATES=350:"getSelectionCoordinates":GET_COORDINATES
#  SHOW_MESSAGE_WITH_CANCEL=351:"showMessageWithCancel":SHOW_MESSAGE_WITH_CANCEL
#  MAKE_SELECTION=352:"makeSelection":MAKE_SELECTION
#  SET_RESULT=353:"setResult":SET_RESULT
#  UPDATE_RESULTS=354:"updateResults":UPDATE_RESULTS
#  SET_BATCH_MODE=355:"setBatchMode":SET_BATCH_MODE
#  PLOT=356:"Plot":PLOT
#  SET_JUSTIFICATION=357:"setJustification":SET_JUSTIFICATION
#  SET_Z_COORDINATE=358:"setZCoordinate":SET_Z_COORDINATE
#  GET_THRESHOLD=359:"getThreshold":GET_THRESHOLD
#  GET_PIXEL_SIZE=360:"getPixelSize":GET_PIXEL_SIZE
#  SETUP_UNDO=361:"setupUndo":SETUP_UNDO
#  SAVE_SETTINGS=362:"saveSettings":SAVE_SETTINGS
#  RESTORE_SETTINGS=363:"restoreSettings":RESTORE_SETTINGS
#  SET_KEY_DOWN=364:"setKeyDown":SET_KEY_DOWN
# +OPEN=365:"open":OPEN
#  SET_FONT=366:"setFont":SET_FONT
#  GET_MIN_AND_MAX=367:"getMinAndMax":GET_MIN_AND_MAX
#  CLOSE=368:"close":CLOSE
#  SET_SLICE=369:"setSlice":SET_SLICE
#  NEW_IMAGE=370:"newImage":NEW_IMAGE
#  SAVE_AS=371:"saveAs":SAVE_AS
#  SAVE=372:"save":SAVE
#  SET_AUTO_THRESHOLD=373:"setAutoThreshold":SET_AUTO_THRESHOLD
#  RENAME=374:"rename":RENAME
#  GET_BOUNDS=375:"getSelectionBounds":GET_BOUNDS
#  FILL_RECT=376:"fillRect":FILL_RECT
#  GET_RAW_STATISTICS=377:"getRawStatistics":GET_RAW_STATISTICS
#  FLOOD_FILL=378:"floodFill":FLOOD_FILL
#  RESTORE_PREVIOUS_TOOL=379:"restorePreviousTool":RESTORE_PREVIOUS_TOOL
#  SET_VOXEL_SIZE=380:"setVoxelSize":SET_VOXEL_SIZE
#  GET_LOCATION_AND_SIZE=381:"getLocationAndSize":GET_LOCATION_AND_SIZE
#  GET_DATE_AND_TIME=382:"getDateAndTime":GET_DATE_AND_TIME
#  SET_METADATA=383:"setMetadata":SET_METADATA
#  CALCULATOR=384:"imageCalculator":CALCULATOR
#  SET_RGB_WEIGHTS=385:"setRGBWeights":SET_RGB_WEIGHTS
#  MAKE_POLYGON=386:"makePolygon":MAKE_POLYGON
#  SET_SELECTION_NAME=387:"setSelectionName":SET_SELECTION_NAME
#  DRAW_RECT=388:"drawRect":DRAW_RECT
#  DRAW_OVAL=389:"drawOval":DRAW_OVAL
#  FILL_OVAL=390:"fillOval":FILL_OVAL
#  SET_OPTION=391:"setOption":SET_OPTION
#  SHOW_TEXT=392:"showText":SHOW_TEXT
#  SET_SELECTION_LOC=393:"setSelectionLocation":SET_SELECTION_LOC
#  GET_DIMENSIONS=394:"getDimensions":GET_DIMENSIONS
#  WAIT_FOR_USER=395:"waitForUser:WAIT_FOR_USE
#  ------------------------------------------------------------------------
#  GET_PIXEL=1000:"getPixel":GET_PIXEL
#  ABS=1001:"abs":ABS
#  COS=1002:"cos":COS
#  EXP=1003:"exp":EXP
#  FLOOR=1004:"floor":FLOOR
#  LOG=1005:"log":LOG
#  MAX_OF=1006:"maxOf":MAX_OF
#  MIN_OF=1007:"minOf":MIN_OF
#  POW=1008:"pow":POW
#  ROUND=1009:"round":ROUND
#  SIN=1010:"sin":SIN
#  SQRT=1011:"sqrt":SQRT
#  TAN=1012:"tan":TAN
#  GET_TIME=1013:"getTime":GET_TIME
# +GET_WIDTH=1014:"getWidth":GET_WIDTH
# +GET_HEIGHT=1015:"getHeight":GET_HEIGHT
#  RANDOM=1016:"random":RANDOM
#  GET_RESULT=1017:"getResult":GET_RESULT
# +GET_COUNT=1018:"getResultsCount":GET_COUNT
#  GET_NUMBER=1019:"getNumber":GET_NUMBER
#  NIMAGES=1020:"nImages":NIMAGES
#  NSLICES=1021:"nSlices":NSLICES
#  LENGTH_OF=1022:"lengthOf":LENGTH_OF
#  NRESULTS=1023:"nResults":NRESULTS
#  GET_ID=1024:"getImageID":GET_ID
#  BIT_DEPTH=1025:"bitDepth":BIT_DEPTH
#  SELECTION_TYPE=1026:"selectionType":SELECTION_TYPE
#  ATAN=1027:"atan":ATAN
#  IS_OPEN=1028:"isOpen":IS_OPEN
#  IS_ACTIVE=1029:"isActive":IS_ACTIVE
#  INDEX_OF=1030:"indexOf":INDEX_OF
#  LAST_INDEX_OF=1031:"lastIndexOf":LAST_INDEX_OF
#  CHAR_CODE_AT=1032:"charCodeAt":CHAR_CODE_AT
#  GET_BOOLEAN=1033:"getBoolean":GET_BOOLEAN
#  STARTS_WITH=1034:"startsWith":STARTS_WITH
#  ENDS_WITH=1035:"endsWith":ENDS_WITH
#  ATAN2=1036:"atan2":ATAN2
#  IS_NAN=1037:"isNaN":IS_NAN
#  GET_ZOOM=1038:"getZoom":GET_ZOOM
#  PARSE_INT=1039:"parseInt":PARSE_INT
#  PARSE_FLOAT=1040:"parseFloat":PARSE_FLOAT
#  IS_KEY_DOWN=1041:"isKeyDown":IS_KEY_DOWN
#  GET_SLICE_NUMBER=1042:"getSliceNumber":GET_SLICE_NUMBER
#  SCREEN_WIDTH=1043:"screenWidth":SCREEN_WIDTH
#  SCREEN_HEIGHT=1044:"screenHeight":SCREEN_HEIGHT
#  CALIBRATE=1045:"calibrate":CALIBRATE
#  ASIN=1046:"asin":ASIN
#  ACOS=1047:"acos":ACOS
#  ROI_MANAGER=1048:"roiManager":ROI_MANAGER
#  TOOL_ID=1049:"toolID":TOOL_ID
#  IS=1050:"is":IS
#  GET_VALUE=1051:"getValue":GET_VALUE
#  STACK=1052:"Stack":STACK
#  MATCHES=1053:"matches":MATCHES
#  ------------------------------------------------------------------------
#  D2S=2000:"d2s":D2S
#  TO_HEX=2001:"toHex":TO_HEX
#  TO_BINARY=2002:"toBinary":TO_BINARY
#  GET_TITLE=2003:"getTitle":GET_TITLE
#  GET_STRING=2004:"getString":GET_STRING
#  SUBSTRING=2005:"substring":SUBSTRING
#  FROM_CHAR_CODE=2006:"fromCharCode":FROM_CHAR_CODE
#  GET_INFO=2007:"getInfo":GET_INFO
#  GET_DIRECTORY=2008:"getDirectory":GET_DIRECTORY
#  GET_ARGUMENT=2009:"getArgument":GET_ARGUMENT
#  GET_IMAGE_INFO=2010:"getImageInfo":GET_IMAGE_INFO
#  TO_LOWER_CASE=2011:"toLowerCase":TO_LOWER_CASE
#  TO_UPPER_CASE=2012:"toUpperCase":TO_UPPER_CASE
#  RUN_MACRO=2013:"runMacro":RUN_MACRO
#  EVAL=2014:"eval":EVAL
#  TO_STRING=2015:"toString":TO_STRING
#  REPLACE=2016:"replace":REPLACE
#  DIALOG=2017:"Dialog":DIALOG
#  GET_METADATA=2018:"getMetadata":GET_METADATA
#  FILE=2019:"File":FILE
#  SELECTION_NAME=2020:"selectionName":SELECTION_NAME
#  GET_VERSION=2021:"getVersion":GET_VERSION
#  GET_RESULT_LABEL=2022:"getResultLabel":GET_RESULT_LABEL
#  CALL=2023:"call":CALL
#  STRING=2024:"String":STRING
#  EXT=2025:"Ext":EXT
#  EXEC=2026:"exec:EXEC
#  ------------------------------------------------------------------------
#  GET_PROFILE=3000:"getProfile":GET_PROFILE
#  NEW_ARRAY=3001:"newArray":NEW_ARRAY
#  SPLIT=3002:"split":SPLIT
#  GET_FILE_LIST=3003:"getFileList":GET_FILE_LIST
#  GET_FONT_LIST=3004:"getFontList":GET_FONT_LIST
#  NEW_MENU=3005:"newMenu":NEW_MENU
#  GET_LIST=3006:"getList":GET_LIST
 
