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
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.HtmlUnitUtils;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.scanner.Token;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.internal.index.IndexConstants;
import com.google.dart.engine.internal.index.IndexContributor;

/**
 * Visits resolved {@link HtmlUnitUtils} and adds relationships into {@link IndexStore}.
 * 
 * @coverage dart.engine.index
 */
public class AngularHtmlIndexContributor extends ExpressionVisitor {
  /**
   * The {@link IndexStore} to record relations into.
   */
  private final IndexStore store;

  /**
   * The index contributor used to index Dart {@link Expression}s.
   */
  private final IndexContributor indexContributor;

  private HtmlElement htmlUnitElement;

  /**
   * Initialize a newly created Angular HTML index contributor.
   * 
   * @param store the {@link IndexStore} to record relations into.
   */
  public AngularHtmlIndexContributor(IndexStore store) {
    this.store = store;
    indexContributor = new IndexContributor(store);
  }

  @Override
  public void visitExpression(Expression expression) {
    expression.accept(indexContributor);
  }

  @Override
  public Void visitHtmlUnit(HtmlUnit node) {
    htmlUnitElement = node.getElement();
    CompilationUnitElement dartUnitElement = node.getCompilationUnitElement();
    indexContributor.enterScope(dartUnitElement);
    return super.visitHtmlUnit(node);
  }

  @Override
  public Void visitXmlAttributeNode(XmlAttributeNode node) {
    Element element = node.getElement();
    if (element != null) {
      Token nameToken = node.getNameToken();
      Location location = createLocation(nameToken);
      store.recordRelationship(element, IndexConstants.IS_REFERENCED_BY, location);
    }
    return super.visitXmlAttributeNode(node);
  }

  @Override
  public Void visitXmlTagNode(XmlTagNode node) {
    Element element = node.getElement();
    if (element != null) {
      Token tagToken = node.getTagToken();
      Location location = createLocation(tagToken);
      store.recordRelationship(element, IndexConstants.IS_REFERENCED_BY, location);
    }
    return super.visitXmlTagNode(node);
  }

  private Location createLocation(Token token) {
    return new Location(htmlUnitElement, token.getOffset(), token.getLength());
  }
}
