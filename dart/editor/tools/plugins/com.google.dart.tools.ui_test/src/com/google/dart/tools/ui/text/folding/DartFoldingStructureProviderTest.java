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
package com.google.dart.tools.ui.text.folding;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.parser.ParserTestCase;
import com.google.dart.tools.ui.internal.text.editor.CompilationUnitEditor;
import com.google.dart.tools.ui.text.folding.DartFoldingStructureProvider.DartProjectionAnnotation;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DartFoldingStructureProviderTest extends ParserTestCase {

  public void test01() throws Exception {
    String source = createSource(//
        "/*",
        " * comment",
        " */",
        "",
        "typedef void arg(num k);",
        "///",
        "///",
        "int fun(arg a) =>",
        "    a(3);",
        "",
        "///",
        "///",
        "//",
        "",
        "//",
        "//",
        "//",
        "",
        "/**",
        " * method comment",
        " * on four lines",
        " */",
        "void main() {",
        "  arg s(l) => (l)=>l;",
        "  print(fun(s(1)));",
        "}",
        "",
        "// two line",
        "// class comment",
        "/// doc",
        "class T {",
        "  f() {",
        "    //",
        "    A() {",
        "      return;",
        "    }",
        "    return;",
        "  }",
        "  q() {",
        "    B() {",
        "      return;",
        "    }",
        "    return;",
        "  }",
        "}");
    source = source.replace("\r\n", "\n");
    int[] offsets = {242, 267, 205, 81, 44, 0, 146, 52, 103, 252, 321, 313};
    int[] lengths = {127, 30, 47, 22, 26, 18, 58, 28, 43, 61, 30, 54};
    verify(source, offsets, lengths);
  }

  protected void printPositions(ProjectionAnnotationModel model) {
    List<Position> positions = new ArrayList<Position>();
    @SuppressWarnings("rawtypes")
    Iterator iter = model.getAnnotationIterator();
    while (iter.hasNext()) {
      DartProjectionAnnotation proj = (DartProjectionAnnotation) iter.next();
      positions.add(model.getPosition(proj));
    }
    System.out.println();
    for (Position pos : positions) {
      System.out.print(pos.offset);
      System.out.print(", ");
    }
    System.out.println();
    for (Position pos : positions) {
      System.out.print(pos.length);
      System.out.print(", ");
    }
  }

  protected void verify(String source, int[] offsets, int[] lengths) throws Exception {
    boolean[] found = new boolean[offsets.length];
    ProjectionAnnotationModel model = fold(source);
//    printPositions(model);
    @SuppressWarnings("rawtypes")
    Iterator iter = model.getAnnotationIterator();
    int count = 0;
    while (iter.hasNext()) {
      DartProjectionAnnotation proj = (DartProjectionAnnotation) iter.next();
      count += 1;
      Position pos = model.getPosition(proj);
      boolean foundThisOne = false;
      for (int i = 0; i < offsets.length; i++) {
        if (found[i]) {
          continue;
        }
        if (offsets[i] == pos.offset && lengths[i] == pos.length) {
          found[i] = true;
          foundThisOne = true;
          break;
        }
      }
      if (!foundThisOne) {
        fail("No annotation found for " + pos);
      }
    }
    if (count != offsets.length) {
      fail("Expected " + offsets.length + " but found " + count + " annotations.");
    }
  }

  private ProjectionAnnotationModel fold(String source) throws Exception {
    ProjectionAnnotationModel model = new ProjectionAnnotationModel();
    CompilationUnitEditor editor = mockEditor(source, model);
    ProjectionViewer viewer = mock(ProjectionViewer.class);
    DartFoldingStructureProvider folder = new DartFoldingStructureProvider();
    folder.install(editor, viewer);
    folder.initialize();
    return model;
  }

  private CompilationUnitEditor mockEditor(String content, ProjectionAnnotationModel model)
      throws Exception {
    CompilationUnitEditor editor = mock(CompilationUnitEditor.class);
    IDocumentProvider docProvider = mock(IDocumentProvider.class);
    IEditorInput editorInput = mock(IEditorInput.class);
    IDocument doc = new Document(content);
    CompilationUnit unit = parseCompilationUnit(content);
    when(editor.getEditorInput()).thenReturn(editorInput);
    when(editor.getInputUnit()).thenReturn(unit);
    when(docProvider.getDocument(editorInput)).thenReturn(doc);
    when(editor.getDocumentProvider()).thenReturn(docProvider);
    when(editor.getAdapter(ProjectionAnnotationModel.class)).thenReturn(model);
    return editor;
  }
}
