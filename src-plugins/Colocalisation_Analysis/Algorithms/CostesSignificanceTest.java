import mpicbg.imglib.type.numeric.RealType;

public class CostesSignificanceTest<T extends RealType<T>> extends Algorithm<T> {
	protected double psfRadiusInPixels[] = new double[3];

	public CostesSignificanceTest(int psfRadiusInPixels) {
		this.psfRadiusInPixels[0] = psfRadiusInPixels;
	}

	@Override
	public void execute(DataContainer<T> container)
			throws MissingPreconditionException {

	}
}