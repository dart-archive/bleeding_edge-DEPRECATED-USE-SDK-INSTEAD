/*
 * Copyright (c) 2015, the Dart project authors.
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
package com.google.dart.engine.internal.hint;

import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.resolver.ResolverTestCase;
import com.google.dart.engine.source.Source;

/**
 * Tests for the {@link ExitDetector} that require that the AST be resolved. See
 * {@link ExitDetectorTest} for tests that do not require the AST to be resolved.
 */
public class ExitDetectorTest2 extends ResolverTestCase {
  public void test_switch_withEnum_false_noDefault() throws Exception {
    Source source = addSource(createSource(//
        "enum E { A, B }",
        "String f(E e) {",
        "  var x;",
        "  switch (e) {",
        "    case A:",
        "      x = 'A';",
        "    case B:",
        "      x = 'B';",
        "  }",
        "  return x;",
        "}"));
    LibraryElement element = resolve(source);
    CompilationUnit unit = resolveCompilationUnit(source, element);
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    Statement statement = body.getBlock().getStatements().get(1);
    assertFalse(ExitDetector.exits(statement));
  }

  public void test_switch_withEnum_false_withDefault() throws Exception {
    Source source = addSource(createSource(//
        "enum E { A, B }",
        "String f(E e) {",
        "  var x;",
        "  switch (e) {",
        "    case A:",
        "      x = 'A';",
        "    default:",
        "      x = '?';",
        "  }",
        "  return x;",
        "}"));
    LibraryElement element = resolve(source);
    CompilationUnit unit = resolveCompilationUnit(source, element);
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    Statement statement = body.getBlock().getStatements().get(1);
    assertFalse(ExitDetector.exits(statement));
  }

  public void test_switch_withEnum_true_noDefault() throws Exception {
    Source source = addSource(createSource(//
        "enum E { A, B }",
        "String f(E e) {",
        "  var x;",
        "  switch (e) {",
        "    case A:",
        "      return 'A';",
        "    case B:",
        "      return 'B';",
        "  }",
        "}"));
    LibraryElement element = resolve(source);
    CompilationUnit unit = resolveCompilationUnit(source, element);
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    Statement statement = body.getBlock().getStatements().get(1);
    assertTrue(ExitDetector.exits(statement));
  }

  public void test_switch_withEnum_true_withDefault() throws Exception {
    Source source = addSource(createSource(//
        "enum E { A, B }",
        "String f(E e) {",
        "  var x;",
        "  switch (e) {",
        "    case A:",
        "      return 'A';",
        "    default:",
        "      return '?';",
        "  }",
        "}"));
    LibraryElement element = resolve(source);
    CompilationUnit unit = resolveCompilationUnit(source, element);
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    Statement statement = body.getBlock().getStatements().get(1);
    assertTrue(ExitDetector.exits(statement));
  }
}
