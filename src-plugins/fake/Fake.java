/* backwards compatibility */
public class Fake extends fiji.build.Fake {
	public Fake() throws fiji.build.FakeException {}
	public static void main(String[] args) {
		fiji.build.Fake.main(args);
	}
}
