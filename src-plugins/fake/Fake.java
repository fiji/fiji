/* backwards compatibility */
public class Fake extends fiji.build.Fake {
	public Fake() throws fiji.build.Fake.FakeException {}
	public static void main(String[] args) {
		fiji.build.Fake.main(args);
	}
}
