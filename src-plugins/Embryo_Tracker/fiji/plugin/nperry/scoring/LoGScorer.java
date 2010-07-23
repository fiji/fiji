package fiji.plugin.nperry.scoring;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.nperry.Spot;

public class LoGScorer <T extends RealType<T>> extends IndependentScorer {

	private static final String SCORING_METHOD_NAME = "Quality";
	private Image<T> img;
	private LocalizableByDimCursor<T> cursor;

	public LoGScorer(Image<T> filteredImage) {
		this.img = filteredImage;
		this.cursor = img.createLocalizableByDimCursor();
	}
	
	@Override
	public String getName() {
		return SCORING_METHOD_NAME;
	}

	@Override
	public void score(Spot spot) {
		final double[] coords = spot.getCoordinates();
		final int[] intCoords = new int[coords.length];
		for (int i = 0; i < intCoords.length; i++) {
			intCoords[i] = (int) Math.round(coords[i]);
		}
		cursor.setPosition(intCoords);
		spot.addScore(SCORING_METHOD_NAME, cursor.getType().getRealDouble());
		//System.out.println("Scoring, coordinate " + MathLib.printCoordinates(intCoords) + "should have " + cursor.getType().getRealDouble()); //debug
		//System.out.println("Spot contains this score for LoGScorer: " + spot.getScores().get("Quality"));//debug
	}
	
	@Override
	public boolean isNormalized() {
		return false;
	}

}
