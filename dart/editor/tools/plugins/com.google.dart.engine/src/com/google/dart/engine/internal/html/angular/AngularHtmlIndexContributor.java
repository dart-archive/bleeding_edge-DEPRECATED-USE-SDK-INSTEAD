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
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.angular.AngularElement;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.scanner.Token;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.internal.index.IndexConstants;
import com.google.dart.engine.internal.index.IndexContributor;

/**
 * Visits resolved {@link HtmlUnit} and adds relationships into {@link IndexStore}.
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
    indexContributor = new IndexContributor(store) {
      @Override
      public Element peekElement() {
        return htmlUnitElement;
      }

      @Override
      protected void recordRelationship(Element element, Relationship relationship,
          Location location) {
        AngularElement angularElement = AngularHtmlUnitResolver.getAngularElement(element);
        if (angularElement != null) {
          element = angularElement;
          relationship = IndexConstants.ANGULAR_REFERENCE;
        }
        super.recordRelationship(element, relationship, location);
      }
    };
  }

  @Override
  public void visitExpression(Expression expression) {
    // Formatter
    if (expression instanceof SimpleIdentifier) {
      SimpleIdentifier identifier = (SimpleIdentifier) expression;
      Element element = identifier.getBestElement();
      if (element instanceof AngularElement) {
        store.recordRelationship(
            element,
            IndexConstants.ANGULAR_REFERENCE,
            createLocationForIdentifier(identifier));
        return;
      }
    }
    // index as a normal Dart expression
    expression.accept(indexContributor);
  }

  @Override
  public Void visitHtmlUnit(HtmlUnit node) {
    htmlUnitElement = node.getElement();
    CompilationUnitElement dartUnitElement = htmlUnitElement.getAngularCompilationUnit();
    indexContributor.enterScope(dartUnitElement);
    return super.visitHtmlUnit(node);
  }

  @Override
  public Void visitXmlAttributeNode(XmlAttributeNode node) {
    Element element = node.getElement();
    if (element != null) {
      Token nameToken = node.getNameToken();
      Location location = createLocationForToken(nameToken);
      store.recordRelationship(element, IndexConstants.ANGULAR_REFERENCE, location);
    }
    return super.visitXmlAttributeNode(node);
  }

  @Override
  public Void visitXmlTagNode(XmlTagNode node) {
    Element element = node.getElement();
    if (element != null) {
      // tag
      {
        Token tagToken = node.getTagToken();
        Location location = createLocationForToken(tagToken);
        store.recordRelationship(element, IndexConstants.ANGULAR_REFERENCE, location);
      }
      // maybe add closing tag range
      Token closingTag = node.getClosingTag();
      if (closingTag != null) {
        Location location = createLocationForToken(closingTag);
        store.recordRelationship(element, IndexConstants.ANGULAR_CLOSING_TAG_REFERENCE, location);
      }
    }
    return super.visitXmlTagNode(node);
  }

  private Location createLocationForIdentifier(SimpleIdentifier identifier) {
    return new Location(htmlUnitElement, identifier.getOffset(), identifier.getLength());
  }

  private Location createLocationForToken(Token token) {
    return new Location(htmlUnitElement, token.getOffset(), token.getLength());
  }
}
