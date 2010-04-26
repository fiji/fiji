//	Persistence of Vision Raytracer Version 3.5 Scene Description File
//	File:        $file_name$.pov
//	Author:      Stephan Saalfeld <saalfeld@mpi-cbg.de>
//	Description: Renders an image stack as media density.
//
//*******************************************

#declare Clock = clock;

camera
{
	location < 0.0, 0.0, -1.0 >
	look_at < 0.0, 0.0, 0.0 >
	angle 55
	right x * image_width / image_height
}


light_source
{
	500 * < -1.0, 1.0, -1.0 >
	rgb 1.0 
}
light_source
{
	500 * < 1.0, 1.0, 0.0 >
	rgb < 1.0, 0, 0 >
}
light_source
{
	500 * < 0, -1.0, 0.0 >
	rgb < 0, 0, 0.25 >
}

box
{
	0.0, 1.0
	texture
	{
		pigment
		{
			rgbt 1.0
		}
	}
	interior
	{
		media
		{
			emission 0.0
			scattering
			{
				1, 20		// make smaller for large bright structures e.g. 1, 4
			}
			absorption 10
			intervals 1
			samples 50		// make larger for very detailed structures e.g. 100
			
			density
			{
				density_file df3 "$file_name$.df3" interpolate 1
				color_map
				{
					[ 0.0	rgb 0.0 ]
					[ 1.0	rgb 1.0 ]
				}
			}
		}
	}
	hollow
	translate -0.5
    scale 1 / $max_length$ * < $width$, -$height$, $depth$ >
    rotate y * ( Clock * 360 )
    rotate x * -20
}

