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
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionBody;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.utilities.general.StringUtilities;
import com.google.dart.server.Outline;
import com.google.dart.server.OutlineKind;
import com.google.dart.server.SourceRegion;

import java.util.List;

/**
 * A computer for {@link Outline}s in a Dart {@link CompilationUnit}.
 * 
 * @coverage dart.server.local
 */
public class DartUnitOutlineComputer {
  private static final String UNITTEST_LIBRARY = "unittest";

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
                newField(
                    classOutline,
                    classChildren,
                    fieldTypeName,
                    field,
                    fieldDeclaration.isStatic());
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

  private void addLocalFunctionOutlines(final OutlineImpl parent, FunctionBody body) {
    final List<Outline> localOutlines = Lists.newArrayList();
    body.accept(new RecursiveAstVisitor<Void>() {
      @Override
      public Void visitFunctionDeclaration(FunctionDeclaration node) {
        newFunctionOutline(parent, localOutlines, node);
        return null;
      }

      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        boolean handled = addUnitTestOutlines(parent, localOutlines, node);
        if (handled) {
          return null;
        }
        return super.visitMethodInvocation(node);
      }
    });
    parent.setChildren(localOutlines.toArray(new Outline[localOutlines.size()]));
  }

  private boolean addUnitTestOutlines(OutlineImpl parent, List<Outline> children,
      MethodInvocation node) {
    OutlineKind unitTestKind = null;
    if (isUnitTestFunctionInvocation(node, "group")) {
      unitTestKind = OutlineKind.UNIT_TEST_GROUP;
    } else if (isUnitTestFunctionInvocation(node, "test")) {
      unitTestKind = OutlineKind.UNIT_TEST_CASE;
    } else {
      return false;
    }
    ArgumentList argumentList = node.getArgumentList();
    if (argumentList != null) {
      List<Expression> arguments = argumentList.getArguments();
      if (arguments.size() == 2 && arguments.get(1) instanceof FunctionExpression) {
        // prepare name
        String name;
        int nameOffset;
        int nameLength;
        {
          Expression nameNode = arguments.get(0);
          if (nameNode instanceof SimpleStringLiteral) {
            SimpleStringLiteral nameLiteral = (SimpleStringLiteral) arguments.get(0);
            name = nameLiteral.getValue();
            nameOffset = nameLiteral.getValueOffset();
            nameLength = name.length();
          } else {
            name = "??????????";
            nameOffset = nameNode.getOffset();
            nameLength = nameNode.getLength();
          }
        }
        // add a new outline
        FunctionExpression functionExpression = (FunctionExpression) arguments.get(1);
        SourceRegionImpl sourceRegion = new SourceRegionImpl(node.getOffset(), node.getLength());
        OutlineImpl outline = new OutlineImpl(
            parent,
            sourceRegion,
            unitTestKind,
            name,
            nameOffset,
            nameLength,
            null,
            null,
            false,
            false,
            false);
        children.add(outline);
        addLocalFunctionOutlines(outline, functionExpression.getBody());
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the {@link AstNode}'s source region.
   */
  private SourceRegion getSourceRegion(AstNode node) {
    int endOffset = node.getEnd();
    // prepare position of the node among its siblings
    int firstOffset;
    List<? extends AstNode> siblings;
    AstNode parent = node.getParent();
    // field
    if (parent instanceof VariableDeclarationList) {
      VariableDeclarationList variableList = (VariableDeclarationList) parent;
      List<VariableDeclaration> variables = variableList.getVariables();
      int variableIndex = variables.indexOf(node);
      if (variableIndex == variables.size() - 1) {
        endOffset = variableList.getParent().getEnd();
      }
      if (variableIndex == 0) {
        node = parent.getParent();
        parent = node.getParent();
      } else if (variableIndex >= 1) {
        firstOffset = variables.get(variableIndex - 1).getEnd();
        return new SourceRegionImpl(firstOffset, endOffset - firstOffset);
      }
    }
    // unit or class member
    if (parent instanceof CompilationUnit) {
      firstOffset = 0;
      siblings = ((CompilationUnit) parent).getDeclarations();
    } else if (parent instanceof ClassDeclaration) {
      ClassDeclaration classDeclaration = (ClassDeclaration) parent;
      firstOffset = classDeclaration.getLeftBracket().getEnd();
      siblings = classDeclaration.getMembers();
    } else {
      int offset = node.getOffset();
      return new SourceRegionImpl(offset, endOffset - offset);
    }
    // first child: [endOfParent, endOfNode]
    int index = siblings.indexOf(node);
    if (index == 0) {
      return new SourceRegionImpl(firstOffset, endOffset - firstOffset);
    }
    // not first child: [endOfPreviousSibling, endOfNode]
    int prevSiblingEnd = siblings.get(index - 1).getEnd();
    return new SourceRegionImpl(prevSiblingEnd, endOffset - prevSiblingEnd);
  }

  /**
   * Returns {@code true} if the given {@link MethodInvocation} is invocation of the function with
   * the given name from the "unittest" library.
   */
  private boolean isUnitTestFunctionInvocation(MethodInvocation node, String name) {
    SimpleIdentifier methodName = node.getMethodName();
    if (methodName != null) {
      Element element = methodName.getStaticElement();
      if (element instanceof FunctionElement) {
        FunctionElement functionElement = (FunctionElement) element;
        if (name.equals(functionElement.getName())) {
          LibraryElement libraryElement = functionElement.getAncestor(LibraryElement.class);
          return libraryElement != null && UNITTEST_LIBRARY.equals(libraryElement.getName());
        }
      }
    }
    return false;
  }

  private OutlineImpl newClassOutline(Outline unitOutline, List<Outline> unitChildren,
      ClassDeclaration classDeclaration) {
    SimpleIdentifier nameNode = classDeclaration.getName();
    String name = nameNode.getName();
    OutlineImpl outline = new OutlineImpl(
        unitOutline,
        getSourceRegion(classDeclaration),
        OutlineKind.CLASS,
        name,
        nameNode.getOffset(),
        name.length(),
        null,
        null,
        classDeclaration.isAbstract(),
        StringUtilities.startsWithChar(name, '_'),
        false);
    unitChildren.add(outline);
    return outline;
  }

  private void newClassTypeAlias(Outline unitOutline, List<Outline> unitChildren,
      ClassTypeAlias alias) {
    SimpleIdentifier nameNode = alias.getName();
    String name = nameNode.getName();
    unitChildren.add(new OutlineImpl(
        unitOutline,
        getSourceRegion(alias),
        OutlineKind.CLASS_TYPE_ALIAS,
        name,
        nameNode.getOffset(),
        nameNode.getLength(),
        null,
        null,
        alias.isAbstract(),
        StringUtilities.startsWithChar(name, '_'),
        false));
  }

  private void newConstructorOutline(OutlineImpl classOutline, List<Outline> children,
      ConstructorDeclaration constructorDeclaration) {
    Identifier returnType = constructorDeclaration.getReturnType();
    String name = returnType.getName();
    int offset = returnType.getOffset();
    int length = returnType.getLength();
    boolean isPrivate = false;
    SimpleIdentifier constructorNameNode = constructorDeclaration.getName();
    if (constructorNameNode != null) {
      String constructorName = constructorNameNode.getName();
      isPrivate = StringUtilities.startsWithChar(constructorName, '_');
      name += "." + constructorName;
      offset = constructorNameNode.getOffset();
      length = constructorNameNode.getLength();
    }
    FormalParameterList parameters = constructorDeclaration.getParameters();
    OutlineImpl outline = new OutlineImpl(
        classOutline,
        getSourceRegion(constructorDeclaration),
        OutlineKind.CONSTRUCTOR,
        name,
        offset,
        length,
        parameters != null ? parameters.toSource() : "",
        null,
        false,
        isPrivate,
        false);
    children.add(outline);
    addLocalFunctionOutlines(outline, constructorDeclaration.getBody());
  }

  private void newField(OutlineImpl classOutline, List<Outline> children, String fieldTypeName,
      VariableDeclaration field, boolean isStatic) {
    SimpleIdentifier nameNode = field.getName();
    String name = nameNode.getName();
    children.add(new OutlineImpl(
        classOutline,
        getSourceRegion(field),
        OutlineKind.FIELD,
        name,
        nameNode.getOffset(),
        nameNode.getLength(),
        null,
        fieldTypeName,
        false,
        StringUtilities.startsWithChar(name, '_'),
        isStatic));
  }

  private void newFunctionOutline(Outline parent, List<Outline> children,
      FunctionDeclaration functionDeclaration) {
    TypeName returnType = functionDeclaration.getReturnType();
    SimpleIdentifier nameNode = functionDeclaration.getName();
    String name = nameNode.getName();
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
        parent,
        getSourceRegion(functionDeclaration),
        kind,
        name,
        nameNode.getOffset(),
        nameNode.getLength(),
        parameters != null ? parameters.toSource() : "",
        returnType != null ? returnType.toSource() : "",
        false,
        StringUtilities.startsWithChar(name, '_'),
        false);
    children.add(outline);
    addLocalFunctionOutlines(outline, functionExpression.getBody());
  }

  private void newFunctionTypeAliasOutline(Outline unitOutline, List<Outline> unitChildren,
      FunctionTypeAlias alias) {
    TypeName returnType = alias.getReturnType();
    SimpleIdentifier nameNode = alias.getName();
    String name = nameNode.getName();
    FormalParameterList parameters = alias.getParameters();
    unitChildren.add(new OutlineImpl(
        unitOutline,
        getSourceRegion(alias),
        OutlineKind.FUNCTION_TYPE_ALIAS,
        name,
        nameNode.getOffset(),
        nameNode.getLength(),
        parameters != null ? parameters.toSource() : "",
        returnType != null ? returnType.toSource() : "",
        false,
        StringUtilities.startsWithChar(name, '_'),
        false));
  }

  private void newMethodOutline(OutlineImpl classOutline, List<Outline> children,
      MethodDeclaration methodDeclaration) {
    TypeName returnType = methodDeclaration.getReturnType();
    SimpleIdentifier nameNode = methodDeclaration.getName();
    String name = nameNode.getName();
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
        getSourceRegion(methodDeclaration),
        kind,
        name,
        nameNode.getOffset(),
        nameNode.getLength(),
        parameters != null ? parameters.toSource() : "",
        returnType != null ? returnType.toSource() : "",
        methodDeclaration.isAbstract(),
        StringUtilities.startsWithChar(name, '_'),
        methodDeclaration.isStatic());
    children.add(outline);
    addLocalFunctionOutlines(outline, methodDeclaration.getBody());
  }

  private OutlineImpl newUnitOutline() {
    return new OutlineImpl(
        null,
        new SourceRegionImpl(unit.getOffset(), unit.getLength()),
        OutlineKind.COMPILATION_UNIT,
        null,
        0,
        0,
        null,
        null,
        false,
        false,
        false);
  }
}
