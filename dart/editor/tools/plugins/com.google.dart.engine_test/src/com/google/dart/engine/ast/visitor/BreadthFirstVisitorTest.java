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
package com.google.dart.engine.ast.visitor;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionDeclarationStatement;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.parser.ParserTestCase;

import java.util.ArrayList;

public class BreadthFirstVisitorTest extends ParserTestCase {
  public void testIt() throws Exception {
    String source = createSource(//
        "class A {",
        "  bool get g => true;",
        "}",
        "class B {",
        "  int f() {",
        "    num q() {",
        "      return 3;",
        "    }",
        "  return q() + 4;",
        "  }",
        "}",
        "A f(var p) {",
        "  if ((p as A).g) {",
        "    return p;",
        "  } else {",
        "    return null;",
        "  }",
        "}");
    CompilationUnit unit = parseCompilationUnit(source);
    final ArrayList<AstNode> nodes = new ArrayList<AstNode>();
    BreadthFirstVisitor<Void> visitor = new BreadthFirstVisitor<Void>() {
      @Override
      public Void visitNode(AstNode node) {
        nodes.add(node);
        return super.visitNode(node);
      }
    };
    visitor.visitAllNodes(unit);
    assertSizeOfList(59, nodes);
    assertInstanceOf(CompilationUnit.class, nodes.get(0));
    assertInstanceOf(ClassDeclaration.class, nodes.get(2)); //class B {...}
    assertInstanceOf(FunctionDeclaration.class, nodes.get(3)); //A f(var p) {...}
    assertInstanceOf(FunctionDeclarationStatement.class, nodes.get(27)); //num q() {return 3;};
    assertInstanceOf(IntegerLiteral.class, nodes.get(58)); //3
  }

}
