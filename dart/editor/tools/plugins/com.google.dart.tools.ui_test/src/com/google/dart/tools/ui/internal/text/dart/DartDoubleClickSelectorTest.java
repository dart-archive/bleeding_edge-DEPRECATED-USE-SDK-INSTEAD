package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.parser.ParserTestCase;
import com.google.dart.tools.ui.internal.text.editor.CompilationUnitEditor;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Point;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DartDoubleClickSelectorTest extends ParserTestCase {
  private static void assertDoubleClickSelection(String content, String clickPattern,
      String resultContent, int offset) throws Exception {
    CompilationUnitEditor.AdaptedSourceViewer textViewer = mock(CompilationUnitEditor.AdaptedSourceViewer.class);
    // mock document
    IDocument document = new Document(content);
    when(textViewer.getDocument()).thenReturn(document);
    // mock editor
    CompilationUnitEditor editor = mock(CompilationUnitEditor.class);
    when(textViewer.getEditor()).thenReturn(editor);
    CompilationUnit unit = parseCompilationUnit(content);
    when(editor.getInputUnit()).thenReturn(unit);
    // mock double click position
    int clickOffset = content.indexOf(clickPattern);
    when(textViewer.getSelectedRange()).thenReturn(new Point(clickOffset, 0));
    // ask for double click range
    DartDoubleClickSelector_OLD selector = new DartDoubleClickSelector_OLD();
    selector.doubleClicked(textViewer);
    // validate
    int resultOffset = offset;
    if (offset == 0) {
      resultOffset = content.indexOf(resultContent);
    }
    int resultLength = resultContent.length();
    verify(textViewer).setSelectedRange(resultOffset, resultLength);
  }

  public void test_dollarIdentifier() throws Exception {
    String content = "main() { var first$second; }";
    String clickPattern = "first";
    String resultContent = "first$second";
    assertDoubleClickSelection(content, clickPattern, resultContent, 0);
    clickPattern = "$";
    assertDoubleClickSelection(content, clickPattern, resultContent, 0);
    clickPattern = "second";
    assertDoubleClickSelection(content, clickPattern, resultContent, 0);
  }

  public void test_interpolation() throws Exception {
    String content = createSource(//
        "main() {",
        "  String first = 'a';",
        "  String second = 'b';",
        "  String first$second = 'c';",
        "  String s = 'xxx$first$second${first$second.length}yyy';",
        "  print(s); ",
        "}");
    int indexOfxxx = content.indexOf("xxx");
    int indexOfThirdFirst = content.indexOf("first", indexOfxxx);
    assertDoubleClickSelection(content, "$first", "xxx", 0);
    assertDoubleClickSelection(content, "first$second$", "first", indexOfThirdFirst);
    assertDoubleClickSelection(content, "first$second.", "first$second.length", 0);
    assertDoubleClickSelection(content, "{first$", "${first$second.length}", 0);
    assertDoubleClickSelection(content, "yyy", "first$second.length", 0);
  }

  public void test_typeArgument() throws Exception {
    String content = createSource(//
        "import 'dart:math';",
        "import 'dart:math' as m;",
        "main() {",
        "  List<Random> a;",
        "  List<m.Random> a;",
        "}");
    assertDoubleClickSelection(content, "Rand", "Random", 0);
    assertDoubleClickSelection(content, "m.Rand", "m.Random", 0);
  }
}
