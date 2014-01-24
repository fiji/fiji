
package tests;

import fiji.Debug;

public class TestInteractively {

	public static void main(String[] args) {
		// dummy call: initialize debug session
		Debug.run("Close All", "");
		String testsDataDir = System.getProperty("plugins.dir") + "/../tests/Data";
		Debug.run("Open...", "open=" + testsDataDir + "/colocsample1b-green.tif");
		Debug.run("Open...", "open=" + testsDataDir + "/colocsample1b-red.tif");
		Debug.run("Coloc 2",
			"channel_1=colocsample1b-green.tif channel_2=colocsample1b-red.tif roi_or_mask=<None> "
			+ "li_histogram_channel_1 "
			+ "li_histogram_channel_2 "
			+ "li_icq "
			+ "spearman's_rank_correlation "
			+ "manders'_correlation "
			+ "2d_instensity_histogram "
			+ "costes'_significance_test "
			+ "psf=3 "
			+ "costes_randomisations=10"
			);
		}
}
