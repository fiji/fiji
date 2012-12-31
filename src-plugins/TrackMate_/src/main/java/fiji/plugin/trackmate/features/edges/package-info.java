/**
 * Package for the classes that compute scalar features on links between
 * spots (edges), such as instantaneous velocities, etc....
 * <p>
 * All analyzers should implement {@link fiji.plugin.trackmate.features.edges.EdgeAnalyzer},
 * which is limited to the independent analysis of a single edge.
 * <p>
 * Note that this class design might change heavily in the future since we might meet 
 * more demanding tasks.
 *  
 * @author Jean-Yves Tinevez
 *
 */
package fiji.plugin.trackmate.features.edges;