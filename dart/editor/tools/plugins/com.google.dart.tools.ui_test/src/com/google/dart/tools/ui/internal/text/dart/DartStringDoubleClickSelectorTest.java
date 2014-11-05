/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
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

public class DartStringDoubleClickSelectorTest extends ParserTestCase {
  private static void assertDoubleClickSelection(String content, String clickPattern,
      String resultContent) throws Exception {
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
    DartStringDoubleClickSelector_OLD selector = new DartStringDoubleClickSelector_OLD();
    selector.doubleClicked(textViewer);
    // validate
    int resultOffset = content.indexOf(resultContent);
    int resultLength = resultContent.length();
    verify(textViewer).setSelectedRange(resultOffset, resultLength);
  }

  public void test_afterDoubleQuote() throws Exception {
    String content = "main() { print(\"aaa bbb\"); }";
    String clickPattern = "aaa bbb";
    String resultContent = "aaa bbb";
    assertDoubleClickSelection(content, clickPattern, resultContent);
  }

  public void test_afterMultilineQuote() throws Exception {
    String content = "main() { print('''aaa \n bbb'''); }";
    String clickPattern = "aaa ";
    String resultContent = "aaa \n bbb";
    assertDoubleClickSelection(content, clickPattern, resultContent);
  }

  public void test_afterSingleQuote() throws Exception {
    String content = "main() { print('aaa bbb'); }";
    String clickPattern = "aaa bbb";
    String resultContent = "aaa bbb";
    assertDoubleClickSelection(content, clickPattern, resultContent);
  }

  public void test_beforeDoubleQuote() throws Exception {
    String content = "main() { print(\"aaa bbb\"); }";
    String clickPattern = "\");";
    String resultContent = "aaa bbb";
    assertDoubleClickSelection(content, clickPattern, resultContent);
  }

  public void test_beforeMultilineQuote() throws Exception {
    String content = "main() { print('''aaa \n bbb'''); }";
    String clickPattern = "''');";
    String resultContent = "aaa \n bbb";
    assertDoubleClickSelection(content, clickPattern, resultContent);
  }

  public void test_beforeSingleQuote() throws Exception {
    String content = "main() { print('aaa bbb'); }";
    String clickPattern = "');";
    String resultContent = "aaa bbb";
    assertDoubleClickSelection(content, clickPattern, resultContent);
  }

  public void test_interpolation() throws Exception {
    String content = createSource(//
        "main() {",
        "  String first = 'a';",
        "  String second = 'b';",
        "  String first$second = 'c';",
        "  String s = 'xxx$first$second${first$second.length}yyy';//",
        "  print(s); ",
        "}");
    assertDoubleClickSelection(content, "xxx", "xxx$first$second${first$second.length}yyy");
    assertDoubleClickSelection(content, "xx$", "xxx");
    assertDoubleClickSelection(content, "yy\'", "yyy");
    assertDoubleClickSelection(content, "';/", "xxx$first$second${first$second.length}yyy");
  }

  public void test_onWord() throws Exception {
    String content = "main() { print('aaa bbb'); }";
    assertDoubleClickSelection(content, "aa bbb", "aaa");
    assertDoubleClickSelection(content, "bbb'", "bbb");
  }
}
