/* backwards compatibility */
public class Fake extends fiji.build.Fake {
	protected Fake() throws fiji.build.Fake.FakeException {}
	public static void main(String[] args) {
		fiji.build.Fake.main(args);
	}
}
