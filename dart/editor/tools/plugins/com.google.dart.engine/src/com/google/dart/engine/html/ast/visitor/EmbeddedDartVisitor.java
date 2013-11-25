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
package com.google.dart.engine.html.ast.visitor;

import com.google.dart.engine.ast.ASTVisitor;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.html.ast.EmbeddedExpression;
import com.google.dart.engine.html.ast.HtmlScriptTagNode;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;

/**
 * Instances of the class {@code EmbeddedDartVisitor} implement a recursive visitor for HTML files
 * that will invoke another visitor on all embedded dart scripts and expressions.
 */
public class EmbeddedDartVisitor<R> implements XmlVisitor<R> {
  /**
   * The visitor used to visit embedded Dart code.
   */
  private ASTVisitor<R> dartVisitor;

  /**
   * Initialize a newly created visitor to visit all of the nodes in an HTML structure and to use
   * the given visitor to visit all of the nodes representing any embedded scripts or expressions.
   * 
   * @param dartVisitor the visitor used to visit embedded Dart code
   */
  public EmbeddedDartVisitor(ASTVisitor<R> dartVisitor) {
    this.dartVisitor = dartVisitor;
  }

  @Override
  public R visitHtmlScriptTagNode(HtmlScriptTagNode node) {
    node.visitChildren(this);
    CompilationUnit script = node.getScript();
    if (script != null) {
      script.accept(dartVisitor);
    }
    return null;
  }

  @Override
  public R visitHtmlUnit(HtmlUnit node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitXmlAttributeNode(XmlAttributeNode node) {
    node.visitChildren(this);
    for (EmbeddedExpression expression : node.getExpressions()) {
      expression.getExpression().accept(dartVisitor);
    }
    return null;
  }

  @Override
  public R visitXmlTagNode(XmlTagNode node) {
    node.visitChildren(this);
    for (EmbeddedExpression expression : node.getExpressions()) {
      expression.getExpression().accept(dartVisitor);
    }
    return null;
  }
}
