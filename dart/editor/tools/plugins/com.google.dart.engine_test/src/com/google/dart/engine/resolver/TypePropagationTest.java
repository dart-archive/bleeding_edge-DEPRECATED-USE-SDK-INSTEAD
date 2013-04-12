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
package com.google.dart.engine.resolver;

import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.source.Source;

public class TypePropagationTest extends ResolverTestCase {
  public void test_assignment() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        " var v;",
        " v = 0;",
        " return v;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors();
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(0);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(2);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(getTypeProvider().getIntType(), variableName.getStaticType());
  }

  public void test_initializer() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        " var v = 0;",
        " return v;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors();
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(0);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(1);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(getTypeProvider().getIntType(), variableName.getStaticType());
  }
}
