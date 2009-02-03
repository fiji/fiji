
def draw_triangle(x1,y1,x2,y2,x3,y3,n)
  reset
  moveTo x1, y1
  lineTo x2, y2
  lineTo x3, y3
  lineTo x1, y1
  update_display
  delay = 25000 / (n*n)
  if delay > 250
    delay = 250
  end
  if delay < 1
    delay = 1;
  end
  wait delay
end

def find_first_point(xs,ys)
  n = xs.length
  smallest_y = ys.min
  smallest_x = get_width
  p1 = nil
  (0...n).each do |i|
    x = xs[i]
    y = ys[i]
    if y == smallest_y && x < smallest_x
      p1 = i
    end
  end
  p1
end

def draw_convex_hull animate
  requires "1.30l"
  x_coordinates, y_coordinates = get_selection_coordinates
  n = x_coordinates.length
  run "Line Width...", "line=1"
  set_foreground_color 0, 0, 0
  snapshot if animate
  p1 = find_first_point x_coordinates, y_coordinates
  pstart = p1
  auto_update false
  loop do
    x1 = x_coordinates[p1];
    y1 = y_coordinates[p1];
    p2 = p1 + 1
    p2 = 0 if (p2 == n)   
    x2 = x_coordinates[p2]
    y2 = y_coordinates[p2]
    p3 = p2 + 1
    p3 = 0 if (p3 == n)
    loop do
      x3 = x_coordinates[p3]
      y3 = y_coordinates[p3]
      if animate
        draw_triangle x1, y1, x2, y2, x3, y3, n
      end
      determinate = x1 * (y2 - y3) - y1 * (x2 - x3) + ( y3 * x2 - y2 * x3)
      if determinate > 0
        x2=x3
        y2=y3
        p2=p3
      end
      p3 += 1
      p3 = 0 if (p3 == n)
      break if p3 == p1
    end
    if animate    
      reset
      drawLine x1, y1, x2, y2
      snapshot
    else 
      drawLine x1, y1, x2, y2
    end
    p1 = p2;
    break if p1 == pstart
  end
  update_display
end

def draw_all_convex_hulls
  (0...n_results).each do |i|
    x = get_result 'XStart', i
    y = get_result 'YStart', i
    do_wand x, y
    draw_convex_hull false
    show_progress( i / Float(n_results) ) if (i % 5) == 0
  end
  show_progress 1
  run"Select None"
end

def blobs_demo
  run "Blobs (25K)"
  # open "/home/mark/blobs.gif"
  set_threshold 125, 248
  run "Analyze Particles...", "show=Nothing exclude clear record"
  draw_all_convex_hulls
end

blobs_demo

