package com.google.dart.tools.core.utilities.ast;

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartDeclaration;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartInvocation;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.resolver.Element;

import java.util.ArrayList;
import java.util.List;

public class NameOccurrencesFinder extends ASTVisitor<Void> {

  private Element target;
  private List<DartNode> matches;

  public NameOccurrencesFinder(Element target) {
    this.target = target;
    this.matches = new ArrayList<DartNode>();
  }

  public List<DartNode> getMatches() {
    return matches;
  }

  public void searchWithin(DartNode ast) {
    ast.accept(this);
  }

  @Override
  public Void visitDeclaration(DartDeclaration<?> node) {
    if (node.getElement() == target) {
      DartNode name = node.getName();
      if (name != null) {
        matches.add(name);
        return null;
      }
    }
    return super.visitDeclaration(node);
  }

  @Override
  public Void visitInvocation(DartInvocation node) {
    if (node.getElement() == target) {
      DartExpression target = node.getTarget();
      if (target != null) {
        matches.add(target);
        return null;
      }
    }
    return super.visitInvocation(node);
  }

  @Override
  public Void visitNode(DartNode node) {
    try {
      if (node.getElement() == target) {
        matches.add(node);
      }
    } catch (UnsupportedOperationException ex) {
      return null; // apparently directives do not have elements
    }
    return super.visitNode(node);
  }
}
