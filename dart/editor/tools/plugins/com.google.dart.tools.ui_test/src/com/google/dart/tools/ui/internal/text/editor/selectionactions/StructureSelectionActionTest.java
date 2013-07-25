package com.google.dart.tools.ui.internal.text.editor.selectionactions;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.parser.ParserTestCase;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.tools.ui.internal.text.editor.CompilationUnitEditor;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

abstract public class StructureSelectionActionTest extends ParserTestCase {

  protected int offset, length;

  abstract StructureSelectionAction makeAction(CompilationUnitEditor editor,
      SelectionHistory history);

  SourceRange runTest(String source, int offset, int len) throws Exception {
    CompilationUnitEditor editor = mockEditor(source, offset, len);
    SelectionHistory history = new SelectionHistory(editor);
    history.setHistoryAction(new StructureSelectHistoryAction() {
      @Override
      public void update() {
      }
    });
    StructureSelectionAction action = makeAction(editor, history);
    action.run();
    return selectedRange();
  }

  String sampleCode() {
    String source = createSource(
        "class R {",
        "  static R _m;",
        "  static R m;",
        "  f() {",
        "    var a = m;",
        "    var b = _m;",
        "    var c = g();",
        "  }",
        "  static g() {",
        "    var a = m;",
        "    var b = _m;",
        "    var c = g();",
        "  }",
        "}",
        "class T {",
        "  f() {",
        "    R x;",
        "    x.g();",
        "    x.m;",
        "    x._m;",
        "  }",
        "  static g() {",
        "    var q = R._m;",
        "    var g = R.m;",
        "    var h = R.g();",
        "  }",
        "  h() {",
        "    var q = R._m;",
        "    var g = R.m;",
        "    var h = R.g();",
        "  }",
        "}");
    source = source.replace("\r\n", "\n");
    return source;
  }

  void saveSelection(int offset, int length) {
    this.offset = offset;
    this.length = length;
  }

  void verifySelection(String initial, String next) throws Exception {
    String source = sampleCode();
    int start = source.indexOf(initial);
    SourceRange range = runTest(source, start, initial.length());
    String result = source.substring(range.getOffset(), range.getEnd());
    assertEquals(next, result);
  }

  private CompilationUnitEditor mockEditor(String content, int start, int len) throws Exception {
    CompilationUnitEditor editor = mock(CompilationUnitEditor.class);
    IDocumentProvider docProvider = mock(IDocumentProvider.class);
    IEditorInput editorInput = mock(IEditorInput.class);
    ISelectionProvider selectionProvider = mock(ISelectionProvider.class);
    IDocument doc = new Document(content);
    CompilationUnit unit = parseCompilationUnit(content);
    when(editor.getEditorInput()).thenReturn(editorInput);
    when(editor.getInputUnit()).thenReturn(unit);
    when(docProvider.getDocument(editorInput)).thenReturn(doc);
    when(editor.getDocumentProvider()).thenReturn(docProvider);
    when(editor.getSelectionProvider()).thenReturn(selectionProvider);
    when(selectionProvider.getSelection()).thenReturn(new TextSelection(start, len));
    return editor;
  }

  private SourceRange selectedRange() {
    return new SourceRange(offset, length);
  }

}
