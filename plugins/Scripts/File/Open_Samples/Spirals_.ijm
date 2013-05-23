/*
 * Pretty cool optical illusion involving color spirals
 *
 * The green and the blue are actually the very same color;
 * Just call Analyze>Color Inspector 3D, select
 * Segmentation>Segmented: Original & White and move the
 * "Depth" slider up and down...
 */
w = h = 512;
cx = w / 2;
cy = h / 2;
count1 = 8;
factor = 2 * PI / w;

useOnlyRedAndGreen = !isKeyDown("alt");
if (useOnlyRedAndGreen) {
	count2 = 8 * 16;

	yellow = (150 << 16) | (250 << 8) | 0;
	red = (240 << 16) | (20 << 8) | 0;
	green = (70 << 16) | (190 << 8) | 0;
} else {
	count2 = 8 * 8;

	yellow = 0x0096be;
	red = 0xfa14a0;
	green = 0xc8c864;
}

function modulo(x, base) {
	return x - base * floor(x / base);
}

function getBand(angle, count) {
	return modulo(floor(count + angle * count / 2 / PI), count / 2);
}

newImage("Untitled", "RGB Black", w, h, 1);
for (j = 0; j < h; j++)
	for (i = 0; i < w; i++) {
		dx = i - cx;
		dy = j - cy;
		r = sqrt(dx * dx + dy * dy);
		angle = atan2(dy, dx);
		band = getBand(r * factor - angle, count1);
		band2 = getBand(r * factor + angle, count2);
		if ((band2 % 2) == 0) {
			if (band == 0)
				color = yellow;
			else
				color = red;
		} else {
			if (band == 2)
				color = yellow;
			else
				color = green;
		}
		setPixel(i, j, color);
	}
