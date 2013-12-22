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

package com.google.dart.engine.internal.html.angular;

import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.HtmlUnitUtils;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.internal.index.IndexContributor;

/**
 * Visits resolved {@link HtmlUnitUtils} and adds relationships into {@link IndexStore}.
 * 
 * @coverage dart.engine.index
 */
public class AngularHtmlIndexContributor extends ExpressionVisitor {
  /**
   * The index contributor used to index Dart {@link Expression}s.
   */
  private final IndexContributor indexContributor;

  /**
   * Initialize a newly created Angular HTML index contributor.
   * 
   * @param store the {@link IndexStore} to record relations into.
   */
  public AngularHtmlIndexContributor(IndexStore store) {
    indexContributor = new IndexContributor(store);
  }

  @Override
  public void visitExpression(Expression expression) {
    expression.accept(indexContributor);
  }

  @Override
  public Void visitHtmlUnit(HtmlUnit node) {
    CompilationUnitElement dartUnitElement = node.getCompilationUnitElement();
    indexContributor.enterScope(dartUnitElement);
    return super.visitHtmlUnit(node);
  }
}
