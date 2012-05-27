/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.utilities.ast;

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartInvocation;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNewExpression;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartPropertyAccess;
import com.google.dart.compiler.ast.DartRedirectConstructorInvocation;
import com.google.dart.compiler.ast.DartSuperConstructorInvocation;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.resolver.Element;

import java.util.ArrayList;
import java.util.List;

// TODO should continue to work work after code is edited
// TODO should not stop working for no apparent reason
public class NameOccurrencesFinder extends ASTVisitor<Void> {

  private Element target;
  private String targetName;
  private List<DartNode> matches;

  public NameOccurrencesFinder(Element target) {
    this.target = target;
    this.targetName = target.getName();
    this.matches = new ArrayList<DartNode>();
  }

  public List<DartNode> getMatches() {
    return matches;
  }

  public void searchWithin(DartNode ast) {
    ast.accept(this);
  }

  @Override
  public Void visitIdentifier(DartIdentifier node) {
    try {
      if (node.getElement() == target) {
        addMatch(node);
      }
    } catch (UnsupportedOperationException ex) {
      return null; // apparently directives do not have elements
    }
    return super.visitIdentifier(node);
  }

  @Override
  public Void visitInvocation(DartInvocation node) {
    if (node.getElement() == target) {
      DartExpression target = node.getTarget();
      if (target != null) {
        addMatch(target);
        return null;
      }
    }
    return super.visitInvocation(node);
  }

  @Override
  public Void visitMethodInvocation(DartMethodInvocation node) {
    if (node.getElement() == target) {
      DartIdentifier name = node.getFunctionName();
      if (name.getName().equals(targetName)) {
        addMatch(name);
        return null;
      }
    }
    return super.visitMethodInvocation(node);
  }

  @Override
  public Void visitNewExpression(DartNewExpression node) {
    // TODO should not highlight plain constructor invocation when type name is selected in class
    // declaration or named constructor definition
    if (node.getElement() == target) {
      DartIdentifier name = null;
      DartNode cons = node.getConstructor();
      if (cons instanceof DartTypeNode) {
        DartNode id = ((DartTypeNode) cons).getIdentifier();
        if (id instanceof DartIdentifier) {
          name = (DartIdentifier) id;
          if (targetName.isEmpty()) {
            // unnamed constructor
            addMatch(id);
            return null;
          }
        } else if (id instanceof DartPropertyAccess) {
          name = ((DartPropertyAccess) id).getName();
        }
      } else if (cons instanceof DartPropertyAccess) {
        name = ((DartPropertyAccess) cons).getName();
      }
      if (name != null && name.getName().equals(targetName)) {
        addMatch(name);
        return null;
      }
    }
    return super.visitNewExpression(node);
  }

  @Override
  public Void visitRedirectConstructorInvocation(DartRedirectConstructorInvocation node) {
    if (node.getElement() == target) {
      DartIdentifier name = node.getName();
      if (name.getName().equals(targetName)) {
        addMatch(name);
        return null;
      }
    }
    return super.visitRedirectConstructorInvocation(node);
  }

  @Override
  public Void visitSuperConstructorInvocation(DartSuperConstructorInvocation node) {
    if (node.getElement() == target) {
      DartIdentifier name = node.getName();
      if (name.getName().equals(targetName)) {
        addMatch(name);
        return null;
      }
    }
    return super.visitSuperConstructorInvocation(node);
  }

  private void addMatch(DartNode node) {
    List<DartNode> removals = new ArrayList<DartNode>();
    for (DartNode match : matches) {
      while (match != null) {
        if (match == node) {
          removals.add(match);
          break;
        }
        match = match.getParent();
      }
    }
    // some nodes visit an identifier twice (getters, setters, possibly others)
    matches.removeAll(removals);
    matches.add(node);
  }
}
