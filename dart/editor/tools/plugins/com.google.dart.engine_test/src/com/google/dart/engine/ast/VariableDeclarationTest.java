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

package com.google.dart.engine.ast;

import com.google.dart.engine.parser.ParserTestCase;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.Token;

import static com.google.dart.engine.ast.AstFactory.topLevelVariableDeclaration;
import static com.google.dart.engine.ast.AstFactory.variableDeclaration;

public class VariableDeclarationTest extends ParserTestCase {

  public void test_getDocumentationComment_onGrandParent() {
    VariableDeclaration varDecl = variableDeclaration("a");
    TopLevelVariableDeclaration decl = topLevelVariableDeclaration(Keyword.VAR, varDecl);
    Comment comment = Comment.createDocumentationComment(new Token[0]);

    assertNull(varDecl.getDocumentationComment());
    decl.setDocumentationComment(comment);
    assertNotNull(varDecl.getDocumentationComment());
    assertNotNull(decl.getDocumentationComment());
  }

  public void test_getDocumentationComment_onNode() {
    VariableDeclaration decl = variableDeclaration("a");
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    decl.setDocumentationComment(comment);

    assertNotNull(decl.getDocumentationComment());
  }

}
