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
package com.google.dart.tools.core.internal.completion;

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartCatchBlock;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFieldDefinition;
import com.google.dart.compiler.ast.DartForInStatement;
import com.google.dart.compiler.ast.DartForStatement;
import com.google.dart.compiler.ast.DartFunction;
import com.google.dart.compiler.ast.DartFunctionTypeAlias;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartParameter;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.compiler.ast.DartSwitchMember;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.ast.DartVariableStatement;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.resolver.FieldElement;
import com.google.dart.compiler.resolver.VariableElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Traverse the AST from initial child node to successive parents, building a collection of local
 * variable and parameter names visible to the initial child node. In case of name shadowing, the
 * first name seen is the most specific one so names are not redefined.
 */
public class ScopedNameFinder extends ASTVisitor<Void> {

  public abstract static class ScopedName {

    public abstract String getName();

    public abstract DartNode getNode();

    public abstract Element getSymbol();

    public boolean isParameter() {
      return false;
    }
  }

  private static class Field extends ScopedName {
    private DartField field;

    Field(DartField field) {
      this.field = field;
    }

    @Override
    public String getName() {
      return field.getName().getName();
    }

    @Override
    public DartNode getNode() {
      return field;
    }

    @Override
    public FieldElement getSymbol() {
      return field.getElement();
    }

  }

  private static class Method extends ScopedName {
    private DartMethodDefinition method;

    Method(DartMethodDefinition method) {
      this.method = method;
    }

    @Override
    public String getName() {
      return method.getElement().getName();
    }

    @Override
    public DartNode getNode() {
      return method;
    }

    @Override
    public Element getSymbol() {
      return method.getElement();
    }

  }

  private static class Param extends ScopedName {
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
      return param.getElement();
    }

    @Override
    public boolean isParameter() {
      return true;
    }
  }

  private static class Var extends ScopedName {
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
      return var.getElement();
    }
  }

  private DartNode immediateChild;
  private Map<String, ScopedName> locals = new HashMap<String, ScopedName>();
  private int position;

  public ScopedNameFinder() {
    this(-1);
  }

  public ScopedNameFinder(int position) {
    this.position = position;
  }

  public Map<String, ScopedName> getLocals() {
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
    for (DartParameter param : node.getParameters()) {
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
  public Void visitUnit(DartUnit unit) {
    for (DartNode node : unit.getTopLevelNodes()) {
      if (node instanceof DartFieldDefinition) {
        DartFieldDefinition field = (DartFieldDefinition) node;
        addToScope(field);
      } else if (node instanceof DartMethodDefinition) {
        DartMethodDefinition method = (DartMethodDefinition) node;
        addToScope(method);
      }
    }
    return null;
  }

  @Override
  public Void visitVariableStatement(DartVariableStatement node) {
    addVariables(node);
    return visitNode(node);
  }

  private void addToScope(DartFieldDefinition fieldDef) {
    boolean notTopLevel = fieldDef.getParent() != null
        && !(fieldDef.getParent() instanceof DartUnit);
    for (DartField field : fieldDef.getFields()) {
      DartIdentifier name = field.getName();
      if (notTopLevel && !isInRange(name)) {
        continue;
      }
      String nameString = name.getName();
      if (locals.get(nameString) != null) {
        return;
      }
      locals.put(nameString, new Field(field));
    }
  }

  private void addToScope(DartMethodDefinition method) {
    boolean notTopLevel = method.getParent() != null && !(method.getParent() instanceof DartUnit);
    if (notTopLevel && !isInRange(method.getName())) {
      return;
    }
    String name = method.getElement().getName();
    if (locals.get(name) != null) {
      return;
    }
    locals.put(name, new Method(method));
  }

  private void addToScope(DartParameter var) {
    if (!isInRange(var)) {
      return;
    }
    String name = var.getParameterName();
    if (locals.get(name) != null) {
      return;
    }
    locals.put(name, new Param(var));
  }

  private void addToScope(DartVariable var) {
    if (!isInRange(var)) {
      return;
    }
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

  private boolean isInRange(DartNode node) {
    if (position < 0) {
      // if source position is not set then all nodes are in range
      return true;
    }
    int start = node.getSourceInfo().getOffset();
    if (start < 0) {
      // assume nodes without source position are in range
      return true;
    }
    return start <= position && node.getSourceInfo().getEnd() < position;
  }
}
