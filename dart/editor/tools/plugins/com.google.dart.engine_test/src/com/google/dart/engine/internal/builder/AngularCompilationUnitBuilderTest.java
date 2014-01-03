/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.engine.internal.builder;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ToolkitObjectElement;
import com.google.dart.engine.element.angular.AngularElement;
import com.google.dart.engine.element.angular.AngularFilterElement;
import com.google.dart.engine.internal.html.angular.AngularTest;
import com.google.dart.engine.source.Source;

public class AngularCompilationUnitBuilderTest extends AngularTest {
  /**
   * Function to force formatter to put every string on separate line.
   */
  public static String[] formatLines(String... lines) {
    return lines;
  }

  private static String createAngularModuleSource(String[] lines, String[] types) {
    String source = "import 'angular.dart';\n";
    source += createSource(lines);
    source += "\n";
    source += createSource(//
        "class MyModule extends Module {",
        "  MyModule() {");
    for (String type : types) {
      source += "    type(" + type + ");";
    }
    source += createSource(//
        "  }",
        "}",
        "",
        "main() {",
        "  ngBootstrap(module: new MyModule());",
        "}");
    return source;
  }

  public void test_bad_notConstructorAnnotation() throws Exception {
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "const MY_ANNOTATION = null;",
            "@MY_ANNOTATION",
            "class MyFilter {",
            "}"),
        formatLines("MyFilter"));
    resolveMainSourceNoErrors(mainContent);
    // prepare AngularFilterElement
    ClassElement classElement = mainUnitElement.getType("MyFilter");
    AngularFilterElement filter = getAngularElement(classElement, AngularFilterElement.class);
    assertNull(filter);
  }

  public void test_NgFilter() throws Exception {
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "@NgFilter(name: 'myFilter')",
            "class MyFilter {",
            "  call(p1, p2) {}",
            "}"),
        formatLines("MyFilter"));
    resolveMainSourceNoErrors(mainContent);
    // prepare AngularFilterElement
    ClassElement classElement = mainUnitElement.getType("MyFilter");
    AngularFilterElement filter = getAngularElement(classElement, AngularFilterElement.class);
    assertNotNull(filter);
    // verify
    assertEquals("myFilter", filter.getName());
    assertEquals(findOffset(mainContent, "myFilter'"), filter.getNameOffset());
  }

  private void assertNoErrors(Source source) throws AnalysisException {
    assertErrors(source);
  }

  @SuppressWarnings("unchecked")
  private <T extends AngularElement> T getAngularElement(ClassElement classElement,
      Class<T> angularElementType) {
    ToolkitObjectElement[] toolkitObjects = classElement.getToolkitObjects();
    for (ToolkitObjectElement toolkitObject : toolkitObjects) {
      if (angularElementType.isInstance(toolkitObject)) {
        return (T) toolkitObject;
      }
    }
    return null;
  }

  private void resolveMainSource(String content) throws Exception {
    mainSource = contextHelper.addSource("/main.dart", content);
    CompilationUnit unit = contextHelper.resolveDefiningUnit(mainSource);
    mainUnitElement = unit.getElement();
  }

  private void resolveMainSourceNoErrors(String content) throws Exception {
    resolveMainSource(content);
    assertNoErrors(mainSource);
  }
}
