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
package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.context.ResolutionEraser;
import com.google.dart.engine.resolver.ResolverTestCase;
import com.google.dart.engine.source.Source;

public class IncrementalResolverTest extends ResolverTestCase {
  public void test_resolve() throws Exception {
    MethodDeclaration method = resolveMethod(createSource(
        "class C {",
        "  int m(int a) {",
        "    return a + a;",
        "  }",
        "}"));
    BlockFunctionBody body = (BlockFunctionBody) method.getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(0);
    BinaryExpression expression = (BinaryExpression) statement.getExpression();
    SimpleIdentifier left = (SimpleIdentifier) expression.getLeftOperand();
    Element leftElement = left.getStaticElement();
    SimpleIdentifier right = (SimpleIdentifier) expression.getRightOperand();
    Element rightElement = right.getStaticElement();
    assertNotNull(leftElement);
    assertSame(leftElement, rightElement);
  }

  private MethodDeclaration resolveMethod(String content) throws Exception {
    Source source = addSource(content);
    LibraryElement library = resolve(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classNode = (ClassDeclaration) unit.getDeclarations().get(0);
    MethodDeclaration method = (MethodDeclaration) classNode.getMembers().get(0);
    method.getBody().accept(new ResolutionEraser());
    GatheringErrorListener errorListener = new GatheringErrorListener();
    IncrementalResolver resolver = new IncrementalResolver(
        library,
        source,
        getTypeProvider(),
        errorListener);
    resolver.resolve(method.getBody());
    return method;
  }
}
