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
package com.google.dart.engine.internal.error;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.source.TestSource;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.compilationUnit;

public class ErrorReporterTest extends EngineTestCase {
  /**
   * Create a type with the given name in a compilation unit with the given name.
   * 
   * @param fileName the name of the compilation unit containing the class
   * @param typeName the name of the type to be created
   * @return the type that was created
   */
  public InterfaceType createType(String fileName, String typeName) {
    CompilationUnitElementImpl unit = compilationUnit(fileName);
    ClassElementImpl element = classElement(typeName);
    unit.setTypes(new ClassElement[] {element});
    return element.getType();
  }

  public void test_creation() {
    GatheringErrorListener listener = new GatheringErrorListener();
    TestSource source = new TestSource();
    assertNotNull(new ErrorReporter(listener, source));
  }

  public void test_reportTypeErrorForNode_differentNames() {
    Type firstType = createType("/test1.dart", "A");
    Type secondType = createType("/test2.dart", "B");
    GatheringErrorListener listener = new GatheringErrorListener();
    ErrorReporter reporter = new ErrorReporter(listener, firstType.getElement().getSource());
    reporter.reportTypeErrorForNode(
        StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE,
        identifier("x"),
        firstType,
        secondType);
    AnalysisError error = listener.getErrors().get(0);
    assertTrue(error.getMessage().indexOf("(") < 0);
  }

  public void test_reportTypeErrorForNode_sameName() {
    String typeName = "A";
    Type firstType = createType("/test1.dart", typeName);
    Type secondType = createType("/test2.dart", typeName);
    GatheringErrorListener listener = new GatheringErrorListener();
    ErrorReporter reporter = new ErrorReporter(listener, firstType.getElement().getSource());
    reporter.reportTypeErrorForNode(
        StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE,
        identifier("x"),
        firstType,
        secondType);
    AnalysisError error = listener.getErrors().get(0);
    assertTrue(error.getMessage().indexOf("(") >= 0);
  }
}
