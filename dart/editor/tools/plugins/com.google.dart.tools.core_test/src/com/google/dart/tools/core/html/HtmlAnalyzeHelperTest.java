package com.google.dart.tools.core.html;

import com.google.common.base.Joiner;
import com.google.dart.compiler.ErrorCode;
import com.google.dart.compiler.resolver.TypeErrorCode;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.test.AbstractDartCoreTest;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import static org.fest.assertions.Assertions.assertThat;

import java.util.concurrent.TimeoutException;

/**
 * Test for {@link HtmlAnalyzeHelper}.
 */
public class HtmlAnalyzeHelperTest extends AbstractDartCoreTest {
  private static final long ONE_MINUTE_MS = 1 * 60 * 1000;

  private static void assertMarker(IMarker marker, TypeErrorCode expectedErrorCode,
      int expectedLine, int expectedOffset, int expectedLength) throws CoreException {
    assertSame(expectedErrorCode, getMarkerErrorCode(marker));
    assertEquals(expectedLine, marker.getAttribute(IMarker.LINE_NUMBER));
    int offset = (Integer) marker.getAttribute(IMarker.CHAR_START);
    int length = (Integer) marker.getAttribute(IMarker.CHAR_END) - offset;
    assertEquals(expectedOffset, offset);
    assertEquals(expectedLength, length);
  }

  /**
   * @return the {@link ErrorCode} from {@link IMarker}.
   */
  private static ErrorCode getMarkerErrorCode(IMarker marker) {
    String qualifiedName = marker.getAttribute("errorCode", (String) null);
    if (qualifiedName == null) {
      return null;
    }
    assertThat(qualifiedName).isNotNull();
    return ErrorCode.Helper.forQualifiedName(qualifiedName);
  }

  private static IMarker[] waitForMarkers(IResource resource) throws Exception {
    long start = System.currentTimeMillis();
    while (true) {
      if (System.currentTimeMillis() - start > ONE_MINUTE_MS) {
        throw new TimeoutException();
      }
      IMarker[] markers = resource.findMarkers(DartCore.DART_PROBLEM_MARKER_TYPE, false, 0);
      if (markers.length > 0) {
        boolean allMarkersHasMessage = true;
        for (IMarker marker : markers) {
          allMarkersHasMessage &= marker.getAttribute(IMarker.MESSAGE) != null;
        }
        if (allMarkersHasMessage) {
          return markers;
        }
      }
      Thread.yield();
    }
  }

  /**
   * Test that when we analyze HTML file we add markers and place them at correct positions.
   */
  public void test_addMarkers() throws Exception {
    String source = Joiner.on("\n").join(
        "// filler filler filler filler filler filler filler filler filler filler",
        "<html>",
        "  <body>",
        "    <script type='application/dart'>",
        "main() {",
        "  int v = '123';",
        "}",
        "    </script>",
        "  </body>",
        "</html>",
        "");
    IFile htmlFile = testProject.setFileContent("Test.html", source);
    // notify HtmlAnalyzeHelper
    HtmlAnalyzeHelper.analyze(htmlFile);
    // prepare single marker
    IMarker marker;
    {
      IMarker[] markers = waitForMarkers(htmlFile);
      assertThat(markers).hasSize(1);
      marker = markers[0];
    }
    // validate marker
    {
      TypeErrorCode expectedErrorCode = TypeErrorCode.TYPE_NOT_ASSIGNMENT_COMPATIBLE;
      int expectedLine = 6;
      int expectedOffset = source.indexOf("'123'");
      int expectedLength = "'123'".length();
      assertMarker(marker, expectedErrorCode, expectedLine, expectedOffset, expectedLength);
    }
  }

  /**
   * Test that Dart scripts in HTML can reference other Dart code using relative path.
   */
  public void test_importLibrary() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library Test;",
        "class MyClass {}",
        "");
    String source = Joiner.on("\n").join(
        "// filler filler filler filler filler filler filler filler filler filler",
        "<html>",
        "  <body>",
        "    <script type='application/dart'>",
        "import 'Test.dart';",
        "main() {",
        "  new MyClass();",
        "  int v = '123';",
        "}",
        "    </script>",
        "  </body>",
        "</html>",
        "");
    IFile htmlFile = testProject.setFileContent("Test.html", source);
    // notify HtmlAnalyzeHelper
    HtmlAnalyzeHelper.analyze(htmlFile);
    // prepare single marker
    IMarker marker;
    {
      IMarker[] markers = waitForMarkers(htmlFile);
      assertThat(markers).hasSize(1);
      marker = markers[0];
    }
    // we need marker here just as indicator that analysis was finished
    {
      TypeErrorCode expectedErrorCode = TypeErrorCode.TYPE_NOT_ASSIGNMENT_COMPATIBLE;
      int expectedLine = 8;
      int expectedOffset = source.indexOf("'123'");
      int expectedLength = "'123'".length();
      assertMarker(marker, expectedErrorCode, expectedLine, expectedOffset, expectedLength);
    }
  }
}
