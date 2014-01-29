
package tests;

import fiji.Debug;

public class TestInteractively {

	public static void main(String[] args) {
		// dummy call: initialize debug session
		Debug.run("Close All", "");
		final String image1 = "colocsample1b-green.tif";
		final String image2 = "colocsample1b-red.tif";
		String testsDataDir = System.getProperty("plugins.dir") + "/../tests/Data/";
		Debug.run("Open...", "open=" + testsDataDir + image1);
		Debug.run("Open...", "open=" + testsDataDir + image2);
		Debug.run("Coloc 2",
			"channel_1=" + image1 + " channel_2=" + image2 + " roi_or_mask=<None> "
			+ "li_histogram_channel_1 "
			+ "li_histogram_channel_2 "
			+ "li_icq "
			+ "spearman's_rank_correlation "
			+ "manders'_correlation "
			+ "kendall's_tau_rank_correlation "
			+ "2d_instensity_histogram "
			+ "costes'_significance_test "
			+ "psf=3 "
			+ "costes_randomisations=10"
			);
		}
}
