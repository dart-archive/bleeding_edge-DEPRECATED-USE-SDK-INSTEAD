import junit.framework.Test;
import junit.framework.TestSuite;

public class TestAll {
  public static Test suite() {
    TestSuite suite = new TestSuite("All UI tests");
    suite.addTest(editor.TestAll.suite());
    //suite.addTest(views.TestAll.suite());
    return suite;
  }
}
