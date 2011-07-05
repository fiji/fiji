# This is a simple script showing how to create a "plasma cloud" in
# JRuby.  This is *very* slow way of implementing this, but hopefully
# it's an instructive example.  See this page for more details:
#
#   http://fiji.sc/wiki/index.php/JRuby_Scripting

# ------------------------------------------------------------------------

# The width and the height; these are global variables so that
# "to_index" can use them:
$w = 400
$h = 300

# Create a single slice RGB image and get the pixels array:
cp = ij.process.ColorProcessor.new($w,$h)
$i = ij.ImagePlus.new "Plasma Cloud", cp
pixels = cp.getPixels

# A helper function to find the index into the pixel array:
def to_index( x, y )
  x + y * $w
end

# Take a list of integer values and return a list with the midpoint
# between each inserted into the list:
def subdivide( points )
  new_points = []
  points.each_index do |index|
    next if index == (points.length - 1)
    min = points[index]
    max = points[index+1]
    new_points.push min
    mid = (min + max) / 2
    new_points.push mid
  end
  new_points.push points.last
  new_points
end

# Seed the random number generator so we get a different cloud each time:
$rng = java.util.Random.new( Time.now.to_i )

# Keep track of the last time we updated the display:
$last_time_displayed = java.lang.System.currentTimeMillis
$update_every = 1000

def set_interpolated_points( pixels, x_min, x_mid, x_max, y_min, y_mid, y_max )
  # Don't redraw all the time, only every $update_every milliseconds
  now = java.lang.System.currentTimeMillis
  if (now - $last_time_displayed) > $update_every
    $last_time_displayed = now
    $i.updateAndDraw
  end
  # Do nothing if there are no pixels to fill in:
  return if (((x_max - x_min) <= 1) && ((y_max - y_min) <= 1))
  # Set the points in the middle of the top row and the bottom row:
  if x_mid != x_min
    pixels[ to_index( x_mid, y_min ) ] =
      color_between( x_max - x_mid,
                     [ pixels[ to_index( x_min, y_min ) ],
                       pixels[ to_index( x_max, y_min ) ] ] )
    pixels[ to_index( x_mid, y_max ) ] =
      color_between( x_max - x_mid,
                     [ pixels[ to_index( x_min, y_max ) ],
                       pixels[ to_index( x_max, y_max ) ] ] )
  end
  # Set the points in the middle of the left colum and the right column:
  if y_mid != y_min
    pixels[ to_index( x_min, y_mid ) ] =
      color_between( y_max - y_mid,
                     [ pixels[ to_index( x_min, y_min ) ],
                       pixels[ to_index( x_min, y_max ) ] ] )      
    pixels[ to_index( x_max, y_mid ) ] =
      color_between( y_max - y_mid,
                     [ pixels[ to_index( x_max, y_min ) ],
                       pixels[ to_index( x_max, y_max ) ] ] )
  end
  # Now the middle point:
  xdiff = (x_max - x_min) / 2.0
  ydiff = (y_max - y_min) / 2.0
  separation = Math.sqrt( xdiff*xdiff + ydiff*ydiff )
  pixels[ to_index( x_mid, y_mid ) ] =
    color_between( separation,
                   [ pixels[ to_index( x_min, y_min ) ],
                     pixels[ to_index( x_max, y_min ) ],
                     pixels[ to_index( x_min, y_max ) ],
                     pixels[ to_index( x_max, y_max ) ] ] )
end

# Get a random RGB value for the initial corner points:
def random_color
  r = $rng.nextInt 256
  g = $rng.nextInt 256
  b = $rng.nextInt 256
  b + (g << 8) + (r << 16)
end

# Return 'old_value' plus some noise up to 'greatest_difference',
# making sure we don't return a value > 255 or < 0:
def add_noise( old_value, greatest_difference )
  new_value = old_value + $rng.nextInt( 2 * greatest_difference ) - greatest_difference
  if new_value > 255
    255
  elsif new_value < 0
    0
  else
    new_value
  end
end

# 'colors' is a list of the colors at 'separation' distance form some
# point; return a color which is an average of those plus some random
# noise linearly related to the separation:
def color_between( separation, colors )
  separation = 1 if separation < 1
  sum_red = sum_green = sum_blue = n = 0
  colors.each do |c|
    n += 1
    sum_blue  += c & 0xFF
    sum_green += (c >> 8)  & 0xFF;
    sum_red   += (c >> 16) & 0xFF;
  end
  # The basic value is the mean of the surrounding colors:
  new_r = sum_red / n
  new_g = sum_green / n
  new_b = sum_blue / n
  # Let's say we can add a random value between -256 and 256 when the
  # separation is half the maximum of $w and $h, and we can only add 0
  # if they're adjacent:
  greatest_difference = Integer( ( 256.0 * separation ) / ([ $w, $h ].max / 2) )
  new_r = add_noise( new_r, greatest_difference )
  new_g = add_noise( new_g, greatest_difference )
  new_b = add_noise( new_b, greatest_difference )
  # Now return the result:
  new_b + (new_g << 8) + (new_r << 16)
end

# Set random colors in the corners:
pixels[ 0 ] = random_color
pixels[ to_index( 0, $h - 1 ) ] = random_color
pixels[ to_index( $w - 1, 0 ) ] = random_color
pixels[ to_index( $w - 1, $h - 1 ) ] = random_color

x_points = [ 0, $w - 1 ]
y_points = [ 0, $h - 1 ]

did_something_last_time = true

$i.show

while true

  did_something_last_time = false

  # Divide up the x_points and y_points to find the midpoints to add:

  new_x_points = subdivide x_points
  new_y_points = subdivide y_points

  # Now for each sub-rectangle we should have set the colours of the
  # corners, so set the interpolated midpoints based on those:

  new_y_points.each_index do |y_min_index|
    next unless (y_min_index % 2) == 0
    next unless y_min_index < (new_y_points.length - 2)
    y_min = new_y_points[y_min_index]
    y_mid = new_y_points[y_min_index+1]
    y_max = new_y_points[y_min_index+2]

    new_x_points.each_index do |x_min_index|
      next unless (x_min_index % 2) == 0
      next unless x_min_index < (new_x_points.length - 2)
      x_min = new_x_points[x_min_index]
      x_mid = new_x_points[x_min_index+1]
      x_max = new_x_points[x_min_index+2]      

      set_interpolated_points( pixels, x_min, x_mid, x_max, y_min, y_mid, y_max )
      
    end
  end

  x_points = new_x_points.uniq
  y_points = new_y_points.uniq

  # We can break when the list of edge points in x and y is the same
  # as the width and the height:
  break if (x_points.length == $w) && (y_points.length == $h)

end

$i.updateAndDraw
