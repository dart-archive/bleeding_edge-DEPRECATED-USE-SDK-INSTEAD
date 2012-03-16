package com.google.dart.tools.core.utilities.ast;

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartDeclaration;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartFunction;
import com.google.dart.compiler.ast.DartFunctionTypeAlias;
import com.google.dart.compiler.ast.DartInvocation;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartParameter;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.resolver.VariableElement;

import java.util.ArrayList;
import java.util.List;

public class NameOccurrencesFinder extends ASTVisitor<Void> {

  private DartNode ast;
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
    this.ast = ast;
    DartNode container = findTargetContainer();
    container.accept(this);
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
    if (node.getElement() == target) {
      matches.add(node);
    }
    return super.visitNode(node);
  }

  private DartNode findAncestor(DartNode node, Class<?>... classes) {
    if (node == null) {
      return null;
    }
    Class<? extends DartNode> nodeClass = node.getClass();
    for (Class<?> ancestorClass : classes) {
      if (ancestorClass.isAssignableFrom(nodeClass)) {
        return node;
      }
    }
    return findAncestor(node.getParent(), classes);
  }

  private DartNode findTargetContainer() {
    if (target instanceof VariableElement) {
      return findAncestor(target.getNode(), DartMethodDefinition.class);
    } else if (target instanceof DartParameter) {
      // TODO determine all decls that can have parameters and include here
      return findAncestor(target.getNode(), DartMethodDefinition.class, DartFunction.class,
          DartFunctionTypeAlias.class);
    } else {
      return ast;
    }
  }
}
