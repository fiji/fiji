package color;

public class CIELAB {
	/* White point D65, CIE 1964 */
	final static float luminance = 0.54f;
	final static float xn = 0.31382f, yn = 0.33100f;
	final static float Yn = luminance, Xn = Yn * xn / yn,
		     Zn = Yn * (1 - xn - yn) / yn;
	static float[] tmp = new float[3];

	public final static float power3(float t) {
		if (t > 0.008856f)
			return (float)Math.exp(Math.log(t) * 3);
		return (t - 16f / 116) / 7.787f;
	}

	public final static float root3(float t) {
		if (t > power3(0.008856f))
			return (float)Math.exp(Math.log(t) / 3);
		return 7.787f * t + 16.0f / 116;
	}

	public final static void XYZ2CIELAB(float[] xyz, float[] lab) {
		lab[0] = 116 * root3(xyz[1] / Yn) - 16;
		lab[1] = 500 * (root3(xyz[0] / Xn) - root3(xyz[1] / Yn));
		lab[2] = 200 * (root3(xyz[1] / Yn) - root3(xyz[2] / Zn));
	}

	public final static void CIELAB2XYZ(float[] lab, float[] xyz) {
		float fy = (lab[0] + 16) / 116;
		xyz[0] = Xn * power3(lab[1] / 500 + fy);
		xyz[1] = Yn * power3(fy);
		xyz[2] = Zn * power3(fy - lab[2] / 200);
	}

	public final static float linear2gamma(float t) {
		return t <= 0.0031308 ? 12.92f * t :
			(float)(1.055 * Math.exp(Math.log(t) / 2.4) - 0.055);
	}

	public final static float gamma2linear(float t) {
		return t <= 0.04045 ? t / 12.92f : (float)
			Math.exp(Math.log((t + 0.055) / 1.055) * 2.4);
	}

	public final static void sRGB2XYZ(float[] rgb, float[] xyz) {
		rgb[0] = gamma2linear(rgb[0]);
		rgb[1] = gamma2linear(rgb[1]);
		rgb[2] = gamma2linear(rgb[2]);
		xyz[0] = 0.4124f * rgb[0] + 0.3576f * rgb[1] + 0.1805f * rgb[2];
		xyz[1] = 0.2126f * rgb[0] + 0.7152f * rgb[1] + 0.0722f * rgb[2];
		xyz[2] = 0.0193f * rgb[0] + 0.1192f * rgb[1] + 0.9505f * rgb[2];
	}

	public final static void XYZ2sRGB(float[] xyz, float[] rgb) {
		rgb[0] = 3.2410f * xyz[0] - 1.5374f * xyz[1] - 0.4986f * xyz[2];
		rgb[1] =-0.9692f * xyz[0] + 1.8760f * xyz[1] + 0.0416f * xyz[2];
		rgb[2] = 0.0556f * xyz[0] - 0.2040f * xyz[1] + 1.0570f * xyz[2];
		rgb[0] = linear2gamma(rgb[0]);
		rgb[1] = linear2gamma(rgb[1]);
		rgb[2] = linear2gamma(rgb[2]);
	}

	public final static void CIELAB2sRGB(float[] lab, float[] rgb) {
		float[] xyz = new float[3];
		CIELAB2XYZ(lab, xyz);
		XYZ2sRGB(xyz, rgb);
	}

	public final static void sRGB2CIELAB(float[] rgb, float[] lab) {
		float[] xyz = new float[3];
		sRGB2XYZ(rgb, xyz);
		XYZ2CIELAB(xyz, lab);
	}

	public final static float norm(float f) {
		return f / 255f;
	}

	public final static int unnorm(float f) {
		return f < 0 ? 0 : f > 1 ? 255 : (int)Math.round(f * 255);
	}

	public final static void int2sRGB(int v, float[] rgb) {
		rgb[0] = norm((v >> 16) & 0xff);
		rgb[1] = norm((v >> 8) & 0xff);
		rgb[2] = norm(v & 0xff);
	}

	public final static int sRGB2int(float[] rgb) {
		return (unnorm(rgb[0]) << 16) | (unnorm(rgb[1]) << 8) |
			unnorm(rgb[2]);
	}
}
