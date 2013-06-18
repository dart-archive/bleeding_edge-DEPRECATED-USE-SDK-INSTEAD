package com.google.dart.tools.ui.internal.text.editor.selectionactions;

import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.tools.ui.internal.text.editor.CompilationUnitEditor;

public class StructureSelectEnclosingActionTest extends StructureSelectionActionTest {

  public void test01() throws Exception {
    SourceRange range = runTest(sampleCode(), 276, 1);
    assertEquals(range.getOffset(), 274);
    assertEquals(range.getLength(), 3);
  }

  public void test02() throws Exception {
    String initial = "g = R.m";
    String next = "var g = R.m";
    verifySelection(initial, next);
  }

  public void test03() throws Exception {
    String initial = "var g = R.m";
    String next = "var g = R.m;";
    verifySelection(initial, next);
  }

  public void test04() throws Exception {
    String initial = "var g = R.m;";
    String next = "{\n" + //
        "    var q = R._m;\n" + //
        "    var g = R.m;\n" + //
        "    var h = R.g();\n" + //
        "  }";
    verifySelection(initial, next);
  }

  @Override
  StructureSelectionAction makeAction(CompilationUnitEditor editor, SelectionHistory history) {
    return new StructureSelectEnclosingAction(editor, history) {
      @Override
      protected void changeSelection(int offset, int len) {
        saveSelection(offset, len);
      }
    };
  }
}
