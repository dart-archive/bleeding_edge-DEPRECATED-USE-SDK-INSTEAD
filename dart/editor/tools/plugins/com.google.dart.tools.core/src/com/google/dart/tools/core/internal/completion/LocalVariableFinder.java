/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.completion;

import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartCatchBlock;
import com.google.dart.compiler.ast.DartForInStatement;
import com.google.dart.compiler.ast.DartForStatement;
import com.google.dart.compiler.ast.DartFunction;
import com.google.dart.compiler.ast.DartFunctionTypeAlias;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartNodeTraverser;
import com.google.dart.compiler.ast.DartParameter;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.compiler.ast.DartSwitchMember;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.ast.DartVariableStatement;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.resolver.VariableElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Traverse the AST from initial child node to successive parents, building a collection of local
 * variable and parameter names visible to the initial child node. In case of name shadowing, the
 * first name seen is the most specific one so names are not redefined.
 */
public class LocalVariableFinder extends DartNodeTraverser<Void> {

  public abstract static class LocalName {

    public abstract String getName();

    public abstract DartNode getNode();

    public abstract Element getSymbol();
  }

  private static class Param extends LocalName {
    private DartParameter param;

    Param(DartParameter param) {
      this.param = param;
    }

    @Override
    public String getName() {
      return param.getParameterName();
    }

    @Override
    public DartNode getNode() {
      return param;
    }

    @Override
    public VariableElement getSymbol() {
      return param.getSymbol();
    }
  }

  private static class Var extends LocalName {
    private DartVariable var;

    Var(DartVariable var) {
      this.var = var;
    }

    @Override
    public String getName() {
      return var.getVariableName();
    }

    @Override
    public DartNode getNode() {
      return var;
    }

    @Override
    public Element getSymbol() {
      return var.getSymbol();
    }
  }

  private DartNode immediateChild;
  private Map<String, LocalName> locals = new HashMap<String, LocalName>();

  public Map<String, LocalName> getLocals() {
    return locals;
  }

  @Override
  public Void visitBlock(DartBlock node) {
    checkStatements(node.getStatements());
    return visitStatement(node);
  }

  @Override
  public Void visitCatchBlock(DartCatchBlock node) {
    addToScope(node.getException());
    return visitStatement(node);
  }

  @Override
  public Void visitForInStatement(DartForInStatement node) {
    addVariables(node.getVariableStatement());
    return visitStatement(node);
  }

  @Override
  public Void visitForStatement(DartForStatement node) {
    if (immediateChild != node.getInit()) {
      if (node.getInit() instanceof DartVariableStatement) {
        addVariables((DartVariableStatement) node.getInit());
      }
    }
    return visitStatement(node);
  }

  @Override
  public Void visitFunction(DartFunction node) {
    for (DartParameter param : node.getParams()) {
      addToScope(param);
    }
    return visitNode(node);
  }

  @Override
  public Void visitFunctionTypeAlias(DartFunctionTypeAlias node) {
    for (DartParameter param : node.getParameters()) {
      addToScope(param);
    }
    return visitDeclaration(node);
  }

  @Override
  public Void visitNode(DartNode node) {
    immediateChild = node;
    if (node.getParent() != null) {
      node.getParent().accept(this);
    }
    return null;
  }

  @Override
  public Void visitSwitchMember(DartSwitchMember node) {
    checkStatements(node.getStatements());
    return visitNode(node);
  }

  @Override
  public Void visitVariableStatement(DartVariableStatement node) {
    addVariables(node);
    return visitNode(node);
  }

  private void addToScope(DartParameter var) {
    String name = var.getParameterName();
    if (locals.get(name) != null) {
      return;
    }
    locals.put(name, new Param(var));
  }

  private void addToScope(DartVariable var) {
    String name = var.getVariableName();
    if (locals.get(name) != null) {
      return;
    }
    locals.put(name, new Var(var));
  }

  private void addVariables(DartVariableStatement node) {
    for (DartVariable var : node.getVariables()) {
      addToScope(var);
    }
  }

  private void checkStatements(List<DartStatement> statements) {
    if (statements == null) {
      return;
    }
    for (DartStatement stmt : statements) {
      if (stmt == immediateChild) {
        return;
      }
      if (stmt instanceof DartVariableStatement) {
        addVariables((DartVariableStatement) stmt);
      }
    }
  }
}
