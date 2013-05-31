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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.resolver.ResolverTestCase;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.ui.internal.text.editor.OverrideIndicatorManager.OverrideIndicator;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This runs as a junit test. It does not need to be run as a junit plugin test.
 */
public class OverrideIndicatorManagerTest extends ResolverTestCase {

  public void test01() throws Exception {
    String source = createSource(//
        "class Top {",
        "  void top(){}",
        "}",
        "class Mid extends Top {",
        "  void mid(){}",
        "}",
        "class Side {",
        "  void side(){}",
        "}",
        "class M1 {",
        "  void m1(){}",
        "}",
        "class M2 {",
        "  void m2(){}",
        "}",
        "class M extends Mid with M1, M2 implements Side {",
        "  void top(){}",
        "  void mid(){}",
        "  void m1(){}",
        "  void m2(){}",
        "  void side(){}", // not annotated
        "}");
    int[] offsets = {227, 242, 256, 212};
    int[] lengths = {3, 2, 2, 3};
    verify(source, offsets, lengths);
  }

  protected void printPositions(ProjectionAnnotationModel model) {
    List<Position> positions = new ArrayList<Position>();
    @SuppressWarnings("rawtypes")
    Iterator iter = model.getAnnotationIterator();
    while (iter.hasNext()) {
      OverrideIndicator proj = (OverrideIndicator) iter.next();
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
    ProjectionAnnotationModel model = annotate(source);
//    printPositions(model);
    @SuppressWarnings("rawtypes")
    Iterator iter = model.getAnnotationIterator();
    int count = 0;
    while (iter.hasNext()) {
      OverrideIndicator proj = (OverrideIndicator) iter.next();
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

  private ProjectionAnnotationModel annotate(String source) throws Exception {
    ProjectionAnnotationModel model = new ProjectionAnnotationModel();
    Source src = addSource(source);
    LibraryElement library = resolve(src);
    CompilationUnit libraryUnit = getAnalysisContext().resolveCompilationUnit(src, library);
    OverrideIndicatorManager folder = new OverrideIndicatorManager(model, libraryUnit);
    folder.uninstall();
    return model;
  }
}
