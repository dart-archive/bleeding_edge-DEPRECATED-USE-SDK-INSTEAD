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
    DartDoubleClickSelector_NEW selector = new DartDoubleClickSelector_NEW();
    selector.doubleClicked(textViewer);
    // validate
    int resultOffset = offset;
    if (resultOffset == 0) {
      resultOffset = content.indexOf(resultContent);
    }
    int resultLength = resultContent.length();
    verify(textViewer).setSelectedRange(resultOffset, resultLength);
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

  public void test_word_identifier() throws Exception {
    String content = createSource(//
        "main() {",
        "  var myVar;",
        "  var my_var;",
        "}");
    assertDoubleClickSelection(content, "mai", "main", 0);
    assertDoubleClickSelection(content, "yV", "myVar", 0);
    assertDoubleClickSelection(content, "var;", "my_var", 0);
  }

  public void test_word_identifier_withDollar() throws Exception {
    String content = "main() { var first$second; }";
    assertDoubleClickSelection(content, "first", "first$second", 0);
    assertDoubleClickSelection(content, "irst", "first$second", 0);
    assertDoubleClickSelection(content, "econd", "first$second", 0);
  }

  public void test_word_interpolation() throws Exception {
    assertDoubleClickSelection("main() { print('$ident'); }", "dent", "ident", 0);
  }
}
