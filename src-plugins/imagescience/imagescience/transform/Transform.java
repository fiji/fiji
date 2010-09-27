package imagescience.transform;

import imagescience.image.Axes;
import imagescience.shape.Point;

/** Represents a 3D affine transformation matrix. The matrix has the following form:
<pre>
     [ axx axy axz axt ]
     [ ayx ayy ayz ayt ]
     [ azx azy azz azt ]
     [  0   0   0   1  ]
</pre>
Methods are provided for easy manipulation of the matrix.
*/
public class Transform {
	
	/** The matrix element axx. */
	public double axx;
	
	/** The matrix element axy. */
	public double axy;
	
	/** The matrix element axz. */
	public double axz;
	
	/** The matrix element axt. */
	public double axt;
	
	/** The matrix element ayx. */
	public double ayx;
	
	/** The matrix element ayy. */
	public double ayy;
	
	/** The matrix element ayz. */
	public double ayz;
	
	/** The matrix element ayt. */
	public double ayt;
	
	/** The matrix element azx. */
	public double azx;
	
	/** The matrix element azy. */
	public double azy;
	
	/** The matrix element azz. */
	public double azz;
	
	/** The matrix element azt. */
	public double azt;
	
	/** Default constructor. Sets the transformation matrix to the identity matrix. */
	public Transform() {
		
		reset();
	}
	
	/** Copy constructor. Sets the elements of the transformation matrix to those of the given transform.
		
		@exception NullPointerException if {@code transform} is {@code null}.
	*/
	public Transform(final Transform transform) {
		
		set(transform);
	}
	
	/** Array constructor. Sets the elements of the transformation matrix to those of the given array.
		
		@param array the array containing the transformation matrix. Must be a 4 x 4 array with the first index corresponding to the rows and second index corresponding to the columns of the matrix.
		
		@exception IllegalArgumentException if {@code array} is not a 4 x 4 array.
		
		@exception NullPointerException if {@code array} is {@code null}.
	*/
	public Transform(final double[][] array) {
		
		set(array);
	}
	
	/** Element constructor. Sets the elements of the transformation matrix to the given values. */
	public Transform(
		final double axx, final double axy, final double axz, final double axt,
		final double ayx, final double ayy, final double ayz, final double ayt,
		final double azx, final double azy, final double azz, final double azt
	) {
		
		set(axx, axy, axz, axt,
			ayx, ayy, ayz, ayt,
			azx, azy, azz, azt
		);
	}
	
	/** Duplicates the transform.
		
		@return a new {@code Transform} object that is an exact copy of this object. All information is copied and no memory is shared between this and the returned object.
	*/
	public Transform duplicate() {
		
		final Transform dup = new Transform();
		
		dup.axx = axx; dup.axy = axy; dup.axz = axz; dup.axt = axt;
		dup.ayx = ayx; dup.ayy = ayy; dup.ayz = ayz; dup.ayt = ayt;
		dup.azx = azx; dup.azy = azy; dup.azz = azz; dup.azt = azt;
		
		return dup;
	}
	
	/** Indicates whether the transform is equal to the given transform.
	
		@param transform the transform to compare this transform with.
		
		@return {@code true} if {@code transform} is not {@code null} and its matrix is equal to the matrix of this object; {@code false} if this is not the case.
	*/
	public boolean equals(final Transform transform) {
		
		if (transform == null) return false;
		
		if (axx == transform.axx && axy == transform.axy && axz == transform.axz && axt == transform.axt &&
			ayx == transform.ayx && ayy == transform.ayy && ayz == transform.ayz && ayt == transform.ayt &&
			azx == transform.azx && azy == transform.azy && azz == transform.azz && azt == transform.azt)
			return true;
		
		return false;
	}
	
	/** Indicates whether the transformation matrix is equal to the identity matrix.
	
		@return {@code true} if the transformation matrix is equal to the identity matrix; {@code false} if this is not the case.
	*/
	public boolean identity() {
		
		if (axx == 1 && axy == 0 && axz == 0 && axt == 0 &&
			ayx == 0 && ayy == 1 && ayz == 0 && ayt == 0 &&
			azx == 0 && azy == 0 && azz == 1 && azt == 0)
			return true;
		
		return false;
	}
	
	/** Resets the transformation matrix to the identity matrix. */
	public void reset() {
		
		axx = 1; axy = 0; axz = 0; axt = 0;
		ayx = 0; ayy = 1; ayz = 0; ayt = 0;
		azx = 0; azy = 0; azz = 1; azt = 0;
	}
	
	/** Returns the determinant of the transformation matrix.
		
		@return the determinant of the transformation matrix.
	*/
	public double determinant() {
		
		return axx*(ayy*azz - ayz*azy) + axy*(ayz*azx - ayx*azz) + axz*(ayx*azy - ayy*azx);
	}
	
	/** Inverts the transformation matrix.
		
		@exception IllegalStateException if the transformation matrix is non-invertible.
	*/
	public void invert() {
		
		final double det = determinant();
		
		if (det == 0) throw new IllegalStateException("Non-invertible transformation matrix");
		
		final double ixx = (ayy*azz - ayz*azy)/det;
		final double ixy = (axz*azy - axy*azz)/det;
		final double ixz = (axy*ayz - axz*ayy)/det;
		final double ixt = (axy*(azz*ayt - ayz*azt) + axz*(ayy*azt - azy*ayt) + axt*(ayz*azy - ayy*azz))/det;
		
		final double iyx = (ayz*azx - ayx*azz)/det;
		final double iyy = (axx*azz - axz*azx)/det;
		final double iyz = (axz*ayx - axx*ayz)/det;
		final double iyt = (axx*(ayz*azt - azz*ayt) + axz*(azx*ayt - ayx*azt) + axt*(ayx*azz - ayz*azx))/det;
		
		final double izx = (ayx*azy - ayy*azx)/det;
		final double izy = (axy*azx - axx*azy)/det;
		final double izz = (axx*ayy - axy*ayx)/det;
		final double izt = (axx*(azy*ayt - ayy*azt) + axy*(ayx*azt - azx*ayt) + axt*(ayy*azx - ayx*azy))/det;
		
		axx = ixx; axy = ixy; axz = ixz; axt = ixt;
		ayx = iyx; ayy = iyy; ayz = iyz; ayt = iyt;
		azx = izx; azy = izy; azz = izz; azt = izt;
	}
	
	/** Transforms the transformation matrix.
		
		@param transform the transform to be applied.
		
		@exception NullPointerException if {@code transform} is {@code null}.
	*/
	public void transform(final Transform transform) {
		
		final double rxx = transform.axx*axx + transform.axy*ayx + transform.axz*azx;
		final double rxy = transform.axx*axy + transform.axy*ayy + transform.axz*azy;
		final double rxz = transform.axx*axz + transform.axy*ayz + transform.axz*azz;
		final double rxt = transform.axx*axt + transform.axy*ayt + transform.axz*azt + transform.axt;
		
		final double ryx = transform.ayx*axx + transform.ayy*ayx + transform.ayz*azx;
		final double ryy = transform.ayx*axy + transform.ayy*ayy + transform.ayz*azy;
		final double ryz = transform.ayx*axz + transform.ayy*ayz + transform.ayz*azz;
		final double ryt = transform.ayx*axt + transform.ayy*ayt + transform.ayz*azt + transform.ayt;
		
		final double rzx = transform.azx*axx + transform.azy*ayx + transform.azz*azx;
		final double rzy = transform.azx*axy + transform.azy*ayy + transform.azz*azy;
		final double rzz = transform.azx*axz + transform.azy*ayz + transform.azz*azz;
		final double rzt = transform.azx*axt + transform.azy*ayt + transform.azz*azt + transform.azt;
		
		axx = rxx; axy = rxy; axz = rxz; axt = rxt;
		ayx = ryx; ayy = ryy; ayz = ryz; ayt = ryt;
		azx = rzx; azy = rzy; azz = rzz; azt = rzt;
	}
	
	/** Transforms the given point.
		
		@param point the point to be transformed. The point is treated as a 3D point. That is, only its {@code x}, {@code y}, {@code z} coordinates are used, which are replaced by their transformed coordinates.
		
		@exception NullPointerException if {@code point} is {@code null}.
	*/
	public void transform(final Point point) {
		
		final double px = axx*point.x + axy*point.y + axz*point.z + axt;
		final double py = ayx*point.x + ayy*point.y + ayz*point.z + ayt;
		final double pz = azx*point.x + azy*point.y + azz*point.z + azt;
		
		point.x = px;
		point.y = py;
		point.z = pz;
	}
	
	/** Rotates the transformation matrix.
		
		@param angle the rotation angle in degrees.
		
		@param axis the rotation axis. Must be one of {@link Axes#X}, {@link Axes#Y}, {@link Axes#Z}.
		
		@exception IllegalArgumentException if {@code axis} is invalid.
	*/
	public void rotate(final double angle, final int axis) {
		
		final double a = angle*Math.PI/180;
		final double ca = Math.cos(a);
		final double sa = Math.sin(a);
		
		switch (axis) {
			case Axes.X: {
				final double ryx = ayx*ca - azx*sa;
				final double ryy = ayy*ca - azy*sa;
				final double ryz = ayz*ca - azz*sa;
				final double ryt = ayt*ca - azt*sa;
				
				final double rzx = azx*ca + ayx*sa;
				final double rzy = azy*ca + ayy*sa;
				final double rzz = azz*ca + ayz*sa;
				final double rzt = azt*ca + ayt*sa;
				
				ayx = ryx; ayy = ryy; ayz = ryz; ayt = ryt;
				azx = rzx; azy = rzy; azz = rzz; azt = rzt;
				
				break;
			}
			case Axes.Y: {
				final double rxx = axx*ca + azx*sa;
				final double rxy = axy*ca + azy*sa;
				final double rxz = axz*ca + azz*sa;
				final double rxt = axt*ca + azt*sa;
				
				final double rzx = azx*ca - axx*sa;
				final double rzy = azy*ca - axy*sa;
				final double rzz = azz*ca - axz*sa;
				final double rzt = azt*ca - axt*sa;
				
				axx = rxx; axy = rxy; axz = rxz; axt = rxt;
				azx = rzx; azy = rzy; azz = rzz; azt = rzt;
				
				break;
			}
			case Axes.Z: {
				final double rxx = axx*ca - ayx*sa;
				final double rxy = axy*ca - ayy*sa;
				final double rxz = axz*ca - ayz*sa;
				final double rxt = axt*ca - ayt*sa;
				
				final double ryx = ayx*ca + axx*sa;
				final double ryy = ayy*ca + axy*sa;
				final double ryz = ayz*ca + axz*sa;
				final double ryt = ayt*ca + axt*sa;
				
				axx = rxx; axy = rxy; axz = rxz; axt = rxt;
				ayx = ryx; ayy = ryy; ayz = ryz; ayt = ryt;
				
				break;
			}
			default: {
				throw new IllegalArgumentException("Invalid rotation axis.");
			}
		}
	}
	
	/** Scales the transformation matrix.
		
		@param factor the scaling factor.
		
		@param axis the scaling axis. Must be one of {@link Axes#X}, {@link Axes#Y}, {@link Axes#Z}.
		
		@exception IllegalArgumentException if {@code axis} is invalid.
	*/
	public void scale(final double factor, final int axis) {
		
		switch (axis) {
			case Axes.X: {
				axx *= factor;
				axy *= factor;
				axz *= factor;
				axt *= factor;
				break;
			}
			case Axes.Y: {
				ayx *= factor;
				ayy *= factor;
				ayz *= factor;
				ayt *= factor;
				break;
			}
			case Axes.Z: {
				azx *= factor;
				azy *= factor;
				azz *= factor;
				azt *= factor;
				break;
			}
			default: {
				throw new IllegalArgumentException("Invalid scaling axis.");
			}
		}
	}
	
	/** Shears the transformation matrix.
		
		@param factor the shearing factor.
		
		@param axis the shearing axis. Must be one of {@link Axes#X}, {@link Axes#Y}, {@link Axes#Z}.
		
		@param drive the driving axis. Must be one of {@link Axes#X}, {@link Axes#Y}, {@link Axes#Z}.
		
		@exception IllegalArgumentException if any of {@code axis} or {@code drive} is invalid.
	*/
	public void shear(final double factor, final int axis, final int drive) {
		
		switch (axis) {
			case Axes.X: {
				switch (drive) {
					case Axes.X: { // Shear X by X
						scale(factor,Axes.X);
						break;
					}
					case Axes.Y: { // Shear X by Y
						axx += ayx*factor;
						axy += ayy*factor;
						axz += ayz*factor;
						axt += ayt*factor;
						break;
					}
					case Axes.Z: { // Shear X by Z
						axx += azx*factor;
						axy += azy*factor;
						axz += azz*factor;
						axt += azt*factor;
						break;
					}
					default: {
						throw new IllegalArgumentException("Invalid driving axis.");
					}
				}
				break;
			}
			case Axes.Y: {
				switch (drive) {
					case Axes.X: { // Shear Y by X
						ayx += axx*factor;
						ayy += axy*factor;
						ayz += axz*factor;
						ayt += axt*factor;
						break;
					}
					case Axes.Y: { // Shear Y by Y
						scale(factor,Axes.Y);
						break;
					}
					case Axes.Z: { // Shear Y by Z
						ayx += azx*factor;
						ayy += azy*factor;
						ayz += azz*factor;
						ayt += azt*factor;
						break;
					}
					default: {
						throw new IllegalArgumentException("Invalid driving axis.");
					}
				}
				break;
			}
			case Axes.Z: {
				switch (drive) {
					case Axes.X: { // Shear Z by X
						azx += axx*factor;
						azy += axy*factor;
						azz += axz*factor;
						azt += axt*factor;
						break;
					}
					case Axes.Y: { // Shear Z by Y
						azx += ayx*factor;
						azy += ayy*factor;
						azz += ayz*factor;
						azt += ayt*factor;
						break;
					}
					case Axes.Z: { // Shear Z by Z
						scale(factor,Axes.Z);
						break;
					}
					default: {
						throw new IllegalArgumentException("Invalid driving axis.");
					}
				}
				break;
			}
			default: {
				throw new IllegalArgumentException("Invalid shearing axis.");
			}
		}
	}
	
	/** Translates the transformation matrix.
		
		@param distance the translation distance.
		
		@param axis the translation axis. Must be one of {@link Axes#X}, {@link Axes#Y}, {@link Axes#Z}.
		
		@exception IllegalArgumentException if {@code axis} is invalid.
	*/
	public void translate(final double distance, final int axis) {
		
		switch (axis) {
			case Axes.X: {
				axt += distance;
				break;
			}
			case Axes.Y: {
				ayt += distance;
				break;
			}
			case Axes.Z: {
				azt += distance;
				break;
			}
			default: {
				throw new IllegalArgumentException("Invalid translation axis.");
			}
		}
	}
	
	/** Sets the elements of the transformation matrix to those of the given transform.
		
		@exception NullPointerException if {@code transform} is {@code null}.
	*/
	public void set(final Transform transform) {
		
		axx = transform.axx; axy = transform.axy; axz = transform.axz; axt = transform.axt;
		ayx = transform.ayx; ayy = transform.ayy; ayz = transform.ayz; ayt = transform.ayt;
		azx = transform.azx; azy = transform.azy; azz = transform.azz; azt = transform.azt;
	}
	
	/** Sets the elements of the transformation matrix to the given values. */
	public void set(
		final double axx, final double axy, final double axz, final double axt,
		final double ayx, final double ayy, final double ayz, final double ayt,
		final double azx, final double azy, final double azz, final double azt
	) {
		
		this.axx = axx; this.axy = axy; this.axz = axz; this.axt = axt;
		this.ayx = ayx; this.ayy = ayy; this.ayz = ayz; this.ayt = ayt;
		this.azx = azx; this.azy = azy; this.azz = azz; this.azt = azt;
	}
	
	/** Sets the elements of the transformation matrix to those of the given array.
		
		@param array the array containing the transformation matrix. Must be a 4 x 4 array with the first index corresponding to the rows and second index corresponding to the columns of the matrix.
		
		@exception IllegalArgumentException if {@code array} is not a 4 x 4 array.
		
		@exception NullPointerException if {@code array} is {@code null}.
	*/
	public void set(final double[][] array) {
		
		if (array.length != 4)
			throw new IllegalArgumentException("Array does not contain 4 rows");
		
		for (int r=0; r<4; ++r) {
			if (array[r].length != 4)
				throw new IllegalArgumentException("Row "+r+" of the array does not contain 4 columns");
		}
		
		axx = array[0][0]; axy = array[0][1]; axz = array[0][2]; axt = array[0][3];
		ayx = array[1][0]; ayy = array[1][1]; ayz = array[1][2]; ayt = array[1][3];
		azx = array[2][0]; azy = array[2][1]; azz = array[2][2]; azt = array[2][3];
	}
	
	/** Sets the element at the given row and column of the transformation matrix to the given value.
		
		@param row the row index. Must be {@code 0}, {@code 1}, or {@code 2} (row {@code 3} is fixed).
		
		@param column the column index. Must be {@code 0}, {@code 1}, {@code 2}, or {@code 3}.
		
		@param value the value.
		
		@exception IllegalArgumentException if {@code row} or {@code column} is out of bounds.
	*/
	public void set(final int row, final int column, final double value) {
		
		if (row < 0 || row > 2)
			throw new IllegalArgumentException("Row index out of bounds");
		if (column < 0 || column > 3)
			throw new IllegalArgumentException("Column index out of bounds");
		
		final int index = row*10 + column;
		
		switch (index) {
			case  0: axx = value; break; case  1: axy = value; break; case  2: axz = value; break; case  3: axt = value; break;
			case 10: ayx = value; break; case 11: ayy = value; break; case 12: ayz = value; break; case 13: ayt = value; break;
			case 20: azx = value; break; case 21: azy = value; break; case 22: azz = value; break; case 23: azt = value; break;
		}
	}
	
	/** Returns the transformation matrix as a 4 x 4 {@code double} array. */
	public double[][] get() {
		
		final double[][] array = new double[4][4];
		
		array[0][0] = axx; array[0][1] = axy; array[0][2] = axz; array[0][3] = axt;
		array[1][0] = ayx; array[1][1] = ayy; array[1][2] = ayz; array[1][3] = ayt;
		array[2][0] = azx; array[2][1] = azy; array[2][2] = azz; array[2][3] = azt;
		array[3][0] = 0.0; array[3][1] = 0.0; array[3][2] = 0.0; array[3][3] = 1.0;
		
		return array;
	}
	
	/** Returns the element at the given row and column of the transformation matrix.
		
		@param row the row index. Must be {@code 0}, {@code 1}, {@code 2}, or {@code 3}.
		
		@param column the column index. Must be {@code 0}, {@code 1}, {@code 2}, or {@code 3}.
		
		@exception IllegalArgumentException if {@code row} or {@code column} is out of bounds.
	*/
	public double get(final int row, final int column) {
		
		if (row < 0 || row > 3)
			throw new IllegalArgumentException("Row index out of bounds");
		if (column < 0 || column > 3)
			throw new IllegalArgumentException("Column index out of bounds");
		
		final int index = row*10 + column;
		
		switch (index) {
			case  0: return axx; case  1: return axy; case  2: return axz; case  3: return axt;
			case 10: return ayx; case 11: return ayy; case 12: return ayz; case 13: return ayt;
			case 20: return azx; case 21: return azy; case 22: return azz; case 23: return azt;
			case 30: return 0.0; case 31: return 0.0; case 32: return 0.0; case 33: return 1.0;
		}
		
		return 0;
	}
	
	/** Returns a string representation of the transformation matrix.
		
		@return a new {@code String} object containing a string representation of the transformation matrix.
	*/
	public String string() {
		
		return string("[  ","   ","  ]\n");
	}
	
	/** Returns a formatted string representation of the transformation matrix.
		
		@param prefix the string put at the beginning of each matrix row.
		
		@param delimit the string put as delimiter between the matrix row elements.
		
		@param postfix the string put at the end of each matrix row.
		
		@return a new {@code String} object containing a formatted string representation of the transformation matrix.
	*/
	public String string(final String prefix, final String delimit, final String postfix) {
		
		String pre = prefix; if (pre == null) pre = "";
		String del = delimit; if (del == null) del = "";
		String post = postfix; if (post == null) post = "";
		
		final String nul = "0.0";
		final String one = "1.0";
		
		final StringBuffer sb = new StringBuffer();
		
		sb.append(pre + axx + del + axy + del + axz + del + axt + post);
		sb.append(pre + ayx + del + ayy + del + ayz + del + ayt + post);
		sb.append(pre + azx + del + azy + del + azz + del + azt + post);
		sb.append(pre + nul + del + nul + del + nul + del + one + post);
		
		return sb.toString();
	}
	
}
