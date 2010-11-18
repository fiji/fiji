/**
 * A small structure to keep decimal places information
 * with numbers along with a name.
 */
public class ValueResult {
	public String name;
	public double number;
	public int decimals;

	public ValueResult( String name, double number, int decimals ) {
		this.name = name;
		this.number = number;
		this.decimals = decimals;
	}
}
