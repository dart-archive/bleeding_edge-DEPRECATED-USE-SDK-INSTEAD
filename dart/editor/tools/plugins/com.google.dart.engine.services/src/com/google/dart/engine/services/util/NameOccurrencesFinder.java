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
package com.google.dart.engine.services.util;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldFormalParameterElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.internal.element.member.Member;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class NameOccurrencesFinder extends RecursiveAstVisitor<Void> {

  public static Collection<AstNode> findIn(SimpleIdentifier ident, AstNode root) {
    if (ident == null || ident.getBestElement() == null) {
      return new HashSet<AstNode>(0);
    }
    NameOccurrencesFinder finder = new NameOccurrencesFinder(ident.getBestElement());
    root.accept(finder);
    return finder.getMatches();
  }

  private Element target;
  private Element target2;
  private Element target3;
  private Element target4;

  private Set<AstNode> matches;

  private NameOccurrencesFinder(Element source) {
    this.target = source;
    switch (source.getKind()) {
      case GETTER:
      case SETTER:
        PropertyAccessorElement accessorElem = (PropertyAccessorElement) source;
        this.target2 = accessorElem.getVariable();
        if (source instanceof Member) {
          Member member = (Member) source;
          this.target4 = member.getBaseElement();
        }
        if (this.target2 instanceof Member) {
          Member member = (Member) source;
          this.target3 = member.getBaseElement();
        }
        break;
      case FIELD:
      case TOP_LEVEL_VARIABLE:
        PropertyInducingElement propertyElem = (PropertyInducingElement) source;
        this.target2 = propertyElem.getGetter();
        this.target3 = propertyElem.getSetter();
        break;
      case METHOD:
        if (source instanceof Member) {
          Member member = (Member) source;
          this.target4 = member.getBaseElement();
        }
        break;
      case PARAMETER:
        ParameterElement param = (ParameterElement) source;
        if (param.isInitializingFormal()) {
          FieldFormalParameterElement fieldInit = (FieldFormalParameterElement) param;
          this.target2 = fieldInit.getField();
        }
      default:
        break;
    }
    if (target2 == null) {
      target2 = target;
    }
    if (target3 == null) {
      target3 = target;
    }
    if (target4 == null) {
      target4 = target;
    }
    this.matches = new HashSet<AstNode>();
  }

  public Collection<AstNode> getMatches() {
    return matches;
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    Element element = node.getBestElement();
    if (element == null) {
      return null;
    }
    match(element, node);
    if (element instanceof Member) {
      Member member = (Member) element;
      match(member.getBaseElement(), node);
    }
    switch (element.getKind()) {
      case GETTER:
      case SETTER:
        PropertyAccessorElement accessorElem = (PropertyAccessorElement) element;
        match(accessorElem.getVariable(), node);
        break;
      case FIELD:
      case TOP_LEVEL_VARIABLE:
        PropertyInducingElement propertyElem = (PropertyInducingElement) element;
        match(propertyElem.getGetter(), node);
        match(propertyElem.getSetter(), node);
        break;
      case PARAMETER:
        ParameterElement param = (ParameterElement) element;
        if (param.isInitializingFormal()) {
          FieldFormalParameterElement fieldInit = (FieldFormalParameterElement) param;
          match(fieldInit.getField(), node);
        }
        break;
      default:
        break;
    }
    return null;
  }

  private void match(Element element, AstNode node) {
    if (target == element || target2 == element || target3 == element || target4 == element) {
      matches.add(node);
    }
  }
}
