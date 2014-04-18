/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server.internal.local.computer;

import com.google.common.collect.Lists;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionBody;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.server.Outline;
import com.google.dart.server.OutlineKind;

import java.util.List;

/**
 * A computer for {@link Outline}s in a Dart {@link CompilationUnit}.
 * 
 * @coverage dart.server.local
 */
public class DartUnitOutlineComputer {
  private final CompilationUnit unit;

  public DartUnitOutlineComputer(CompilationUnit unit) {
    this.unit = unit;
  }

  /**
   * Returns the computed {@link Outline}s, not {@code null}.
   */
  public Outline compute() {
    OutlineImpl unitOutline = newUnitOutline();
    List<Outline> unitChildren = Lists.newArrayList();
    for (CompilationUnitMember unitMember : unit.getDeclarations()) {
      if (unitMember instanceof ClassDeclaration) {
        ClassDeclaration classDeclartion = (ClassDeclaration) unitMember;
        OutlineImpl classOutline = newClassOutline(unitOutline, unitChildren, classDeclartion);
        List<Outline> classChildren = Lists.newArrayList();
        for (ClassMember classMember : classDeclartion.getMembers()) {
          if (classMember instanceof ConstructorDeclaration) {
            ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) classMember;
            newConstructorOutline(classOutline, classChildren, constructorDeclaration);
          }
          if (classMember instanceof FieldDeclaration) {
            FieldDeclaration fieldDeclaration = (FieldDeclaration) classMember;
            VariableDeclarationList fields = fieldDeclaration.getFields();
            if (fields != null) {
              TypeName fieldType = fields.getType();
              String fieldTypeName = fieldType != null ? fieldType.toSource() : "";
              for (VariableDeclaration field : fields.getVariables()) {
                newField(classOutline, classChildren, fieldTypeName, field);
              }
            }
          }
          if (classMember instanceof MethodDeclaration) {
            MethodDeclaration methodDeclaration = (MethodDeclaration) classMember;
            newMethodOutline(classOutline, classChildren, methodDeclaration);
          }
        }
        classOutline.setChildren(classChildren.toArray(new Outline[classChildren.size()]));
      }
      if (unitMember instanceof FunctionDeclaration) {
        FunctionDeclaration functionDeclaration = (FunctionDeclaration) unitMember;
        newFunctionOutline(unitOutline, unitChildren, functionDeclaration);
      }
      if (unitMember instanceof ClassTypeAlias) {
        ClassTypeAlias alias = (ClassTypeAlias) unitMember;
        newClassTypeAlias(unitOutline, unitChildren, alias);
      }
      if (unitMember instanceof FunctionTypeAlias) {
        FunctionTypeAlias alias = (FunctionTypeAlias) unitMember;
        newFunctionTypeAliasOutline(unitOutline, unitChildren, alias);
      }
    }
    unitOutline.setChildren(unitChildren.toArray(new Outline[unitChildren.size()]));
    return unitOutline;
  }

  private void addLocalFunctionOutlines(final OutlineImpl parenet, FunctionBody body) {
    final List<Outline> localOutlines = Lists.newArrayList();
    body.accept(new RecursiveAstVisitor<Void>() {
      @Override
      public Void visitFunctionDeclaration(FunctionDeclaration node) {
        newFunctionOutline(parenet, localOutlines, node);
        return null;
      }
    });
    parenet.setChildren(localOutlines.toArray(new Outline[localOutlines.size()]));
  }

  private OutlineImpl newClassOutline(Outline unitOutline, List<Outline> unitChildren,
      ClassDeclaration classDeclartion) {
    SimpleIdentifier nameNode = classDeclartion.getName();
    String name = nameNode.getName();
    OutlineImpl outline = new OutlineImpl(
        unitOutline,
        OutlineKind.CLASS,
        name,
        nameNode.getOffset(),
        name.length(),
        null,
        null);
    unitChildren.add(outline);
    return outline;
  }

  private void newClassTypeAlias(Outline unitOutline, List<Outline> unitChildren,
      ClassTypeAlias alias) {
    SimpleIdentifier nameNode = alias.getName();
    unitChildren.add(new OutlineImpl(
        unitOutline,
        OutlineKind.CLASS_TYPE_ALIAS,
        nameNode.getName(),
        nameNode.getOffset(),
        nameNode.getLength(),
        null,
        null));
  }

  private void newConstructorOutline(OutlineImpl classOutline, List<Outline> children,
      ConstructorDeclaration constructorDeclaration) {
    Identifier returnType = constructorDeclaration.getReturnType();
    String name = returnType.getName();
    int offset = returnType.getOffset();
    int length = returnType.getLength();
    SimpleIdentifier constructorNameNode = constructorDeclaration.getName();
    if (constructorNameNode != null) {
      name += "." + constructorNameNode.getName();
      offset = constructorNameNode.getOffset();
      length = constructorNameNode.getLength();
    }
    FormalParameterList parameters = constructorDeclaration.getParameters();
    OutlineImpl outline = new OutlineImpl(
        classOutline,
        OutlineKind.CONSTRUCTOR,
        name,
        offset,
        length,
        parameters != null ? parameters.toSource() : "",
        null);
    children.add(outline);
    addLocalFunctionOutlines(outline, constructorDeclaration.getBody());
  }

  private void newField(OutlineImpl classOutline, List<Outline> children, String fieldTypeName,
      VariableDeclaration field) {
    SimpleIdentifier nameNode = field.getName();
    children.add(new OutlineImpl(
        classOutline,
        OutlineKind.FIELD,
        nameNode.getName(),
        nameNode.getOffset(),
        nameNode.getLength(),
        null,
        fieldTypeName));
  }

  private void newFunctionOutline(Outline unitOutline, List<Outline> unitChildren,
      FunctionDeclaration functionDeclaration) {
    TypeName returnType = functionDeclaration.getReturnType();
    SimpleIdentifier nameNode = functionDeclaration.getName();
    FunctionExpression functionExpression = functionDeclaration.getFunctionExpression();
    FormalParameterList parameters = functionExpression.getParameters();
    OutlineKind kind;
    if (functionDeclaration.isGetter()) {
      kind = OutlineKind.GETTER;
    } else if (functionDeclaration.isSetter()) {
      kind = OutlineKind.SETTER;
    } else {
      kind = OutlineKind.FUNCTION;
    }
    OutlineImpl outline = new OutlineImpl(
        unitOutline,
        kind,
        nameNode.getName(),
        nameNode.getOffset(),
        nameNode.getLength(),
        parameters != null ? parameters.toSource() : "",
        returnType != null ? returnType.toSource() : "");
    unitChildren.add(outline);
    addLocalFunctionOutlines(outline, functionExpression.getBody());
  }

  private void newFunctionTypeAliasOutline(Outline unitOutline, List<Outline> unitChildren,
      FunctionTypeAlias alias) {
    TypeName returnType = alias.getReturnType();
    SimpleIdentifier nameNode = alias.getName();
    FormalParameterList parameters = alias.getParameters();
    unitChildren.add(new OutlineImpl(
        unitOutline,
        OutlineKind.FUNCTION_TYPE_ALIAS,
        nameNode.getName(),
        nameNode.getOffset(),
        nameNode.getLength(),
        parameters != null ? parameters.toSource() : "",
        returnType != null ? returnType.toSource() : ""));
  }

  private void newMethodOutline(OutlineImpl classOutline, List<Outline> children,
      MethodDeclaration methodDeclaration) {
    TypeName returnType = methodDeclaration.getReturnType();
    SimpleIdentifier nameNode = methodDeclaration.getName();
    FormalParameterList parameters = methodDeclaration.getParameters();
    OutlineKind kind;
    if (methodDeclaration.isGetter()) {
      kind = OutlineKind.GETTER;
    } else if (methodDeclaration.isSetter()) {
      kind = OutlineKind.SETTER;
    } else {
      kind = OutlineKind.METHOD;
    }
    OutlineImpl outline = new OutlineImpl(
        classOutline,
        kind,
        nameNode.getName(),
        nameNode.getOffset(),
        nameNode.getLength(),
        parameters != null ? parameters.toSource() : "",
        returnType != null ? returnType.toSource() : "");
    children.add(outline);
    addLocalFunctionOutlines(outline, methodDeclaration.getBody());
  }

  private OutlineImpl newUnitOutline() {
    return new OutlineImpl(null, OutlineKind.COMPILATION_UNIT, null, 0, 0, null, null);
  }
}
