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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.source.Source;
import com.google.dart.server.Outline;

/**
 * A computer for {@link Outline}s in a Dart {@link CompilationUnit}.
 * 
 * @coverage dart.server.local
 */
public class DartUnitOutlineComputer {
//  private static final String UNITTEST_LIBRARY = "unittest";

//  private final CompilationUnit unit;

  public DartUnitOutlineComputer(String contextId, Source source, CompilationUnit unit) {
//    this.unit = unit;
  }

  /**
   * Returns the computed {@link Outline}s, not {@code null}.
   */
  public Outline compute() {
    return null;
//    OutlineImpl unitOutline = newUnitOutline();
//    List<Outline> unitChildren = Lists.newArrayList();
//    for (CompilationUnitMember unitMember : unit.getDeclarations()) {
//      if (unitMember instanceof ClassDeclaration) {
//        ClassDeclaration classDeclartion = (ClassDeclaration) unitMember;
//        OutlineImpl classOutline = newClassOutline(unitOutline, unitChildren, classDeclartion);
//        List<Outline> classChildren = Lists.newArrayList();
//        for (ClassMember classMember : classDeclartion.getMembers()) {
//          if (classMember instanceof ConstructorDeclaration) {
//            ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) classMember;
//            newConstructorOutline(classOutline, classChildren, constructorDeclaration);
//          }
//          if (classMember instanceof FieldDeclaration) {
//            FieldDeclaration fieldDeclaration = (FieldDeclaration) classMember;
//            VariableDeclarationList fields = fieldDeclaration.getFields();
//            if (fields != null) {
//              TypeName fieldType = fields.getType();
//              String fieldTypeName = fieldType != null ? fieldType.toSource() : "";
//              for (VariableDeclaration field : fields.getVariables()) {
//                newVariableOutline(
//                    classOutline,
//                    classChildren,
//                    fieldTypeName,
//                    ElementKind.FIELD,
//                    field,
//                    fieldDeclaration.isStatic());
//              }
//            }
//          }
//          if (classMember instanceof MethodDeclaration) {
//            MethodDeclaration methodDeclaration = (MethodDeclaration) classMember;
//            newMethodOutline(classOutline, classChildren, methodDeclaration);
//          }
//        }
//        classOutline.setChildren(classChildren.toArray(new Outline[classChildren.size()]));
//      }
//      if (unitMember instanceof TopLevelVariableDeclaration) {
//        TopLevelVariableDeclaration fieldDeclaration = (TopLevelVariableDeclaration) unitMember;
//        VariableDeclarationList fields = fieldDeclaration.getVariables();
//        if (fields != null) {
//          TypeName fieldType = fields.getType();
//          String fieldTypeName = fieldType != null ? fieldType.toSource() : "";
//          for (VariableDeclaration field : fields.getVariables()) {
//            newVariableOutline(
//                unitOutline,
//                unitChildren,
//                fieldTypeName,
//                ElementKind.TOP_LEVEL_VARIABLE,
//                field,
//                false);
//          }
//        }
//      }
//      if (unitMember instanceof FunctionDeclaration) {
//        FunctionDeclaration functionDeclaration = (FunctionDeclaration) unitMember;
//        newFunctionOutline(unitOutline, unitChildren, functionDeclaration);
//      }
//      if (unitMember instanceof ClassTypeAlias) {
//        ClassTypeAlias alias = (ClassTypeAlias) unitMember;
//        newClassTypeAlias(unitOutline, unitChildren, alias);
//      }
//      if (unitMember instanceof FunctionTypeAlias) {
//        FunctionTypeAlias alias = (FunctionTypeAlias) unitMember;
//        newFunctionTypeAliasOutline(unitOutline, unitChildren, alias);
//      }
//    }
//    unitOutline.setChildren(unitChildren.toArray(new Outline[unitChildren.size()]));
//    return unitOutline;
  }

//  private void addLocalFunctionOutlines(final OutlineImpl parent, FunctionBody body) {
//    final List<Outline> localOutlines = Lists.newArrayList();
//    body.accept(new RecursiveAstVisitor<Void>() {
//      @Override
//      public Void visitFunctionDeclaration(FunctionDeclaration node) {
//        newFunctionOutline(parent, localOutlines, node);
//        return null;
//      }
//
//      @Override
//      public Void visitMethodInvocation(MethodInvocation node) {
//        boolean handled = addUnitTestOutlines(parent, localOutlines, node);
//        if (handled) {
//          return null;
//        }
//        return super.visitMethodInvocation(node);
//      }
//    });
//    parent.setChildren(localOutlines.toArray(new Outline[localOutlines.size()]));
//  }
//
//  private boolean addUnitTestOutlines(OutlineImpl parent, List<Outline> children,
//      MethodInvocation node) {
//    ElementKind unitTestKind = null;
//    if (isUnitTestFunctionInvocation(node, "group")) {
//      unitTestKind = ElementKind.UNIT_TEST_GROUP;
//    } else if (isUnitTestFunctionInvocation(node, "test")) {
//      unitTestKind = ElementKind.UNIT_TEST_CASE;
//    } else {
//      return false;
//    }
//    ArgumentList argumentList = node.getArgumentList();
//    if (argumentList != null) {
//      List<Expression> arguments = argumentList.getArguments();
//      if (arguments.size() == 2 && arguments.get(1) instanceof FunctionExpression) {
//        // prepare name
//        String name;
//        int nameOffset;
//        int nameLength;
//        {
//          Expression nameNode = arguments.get(0);
//          if (nameNode instanceof SimpleStringLiteral) {
//            SimpleStringLiteral nameLiteral = (SimpleStringLiteral) arguments.get(0);
//            name = nameLiteral.getValue();
//            nameOffset = nameLiteral.getValueOffset();
//            nameLength = name.length();
//          } else {
//            name = "??????????";
//            nameOffset = nameNode.getOffset();
//            nameLength = nameNode.getLength();
//          }
//        }
//        // add a new outline
//        FunctionExpression functionExpression = (FunctionExpression) arguments.get(1);
//        SourceRegionImpl sourceRegion = new SourceRegionImpl(node.getOffset(), node.getLength());
//        ElementImpl element = new ElementImpl(
//            contextId,
//            null,
//            source,
//            unitTestKind,
//            name,
//            nameOffset,
//            nameLength,
//            null,
//            null,
//            false,
//            false,
//            false);
//        OutlineImpl outline = new OutlineImpl(parent, element, sourceRegion);
//        children.add(outline);
//        addLocalFunctionOutlines(outline, functionExpression.getBody());
//        return true;
//      }
//    }
//    return false;
//  }
//
//  /**
//   * Returns the {@link AstNode}'s source region.
//   */
//  private SourceRegion getSourceRegion(AstNode node) {
//    int endOffset = node.getEnd();
//    // prepare position of the node among its siblings
//    int firstOffset;
//    List<? extends AstNode> siblings;
//    AstNode parent = node.getParent();
//    // field
//    if (parent instanceof VariableDeclarationList) {
//      VariableDeclarationList variableList = (VariableDeclarationList) parent;
//      List<VariableDeclaration> variables = variableList.getVariables();
//      int variableIndex = variables.indexOf(node);
//      if (variableIndex == variables.size() - 1) {
//        endOffset = variableList.getParent().getEnd();
//      }
//      if (variableIndex == 0) {
//        node = parent.getParent();
//        parent = node.getParent();
//      } else if (variableIndex >= 1) {
//        firstOffset = variables.get(variableIndex - 1).getEnd();
//        return new SourceRegionImpl(firstOffset, endOffset - firstOffset);
//      }
//    }
//    // unit or class member
//    if (parent instanceof CompilationUnit) {
//      firstOffset = 0;
//      siblings = ((CompilationUnit) parent).getDeclarations();
//    } else if (parent instanceof ClassDeclaration) {
//      ClassDeclaration classDeclaration = (ClassDeclaration) parent;
//      firstOffset = classDeclaration.getLeftBracket().getEnd();
//      siblings = classDeclaration.getMembers();
//    } else {
//      int offset = node.getOffset();
//      return new SourceRegionImpl(offset, endOffset - offset);
//    }
//    // first child: [endOfParent, endOfNode]
//    int index = siblings.indexOf(node);
//    if (index == 0) {
//      return new SourceRegionImpl(firstOffset, endOffset - firstOffset);
//    }
//    // not first child: [endOfPreviousSibling, endOfNode]
//    int prevSiblingEnd = siblings.get(index - 1).getEnd();
//    return new SourceRegionImpl(prevSiblingEnd, endOffset - prevSiblingEnd);
//  }
//
//  /**
//   * Returns {@code true} if the given {@link MethodInvocation} is invocation of the function with
//   * the given name from the "unittest" library.
//   */
//  private boolean isUnitTestFunctionInvocation(MethodInvocation node, String name) {
//    SimpleIdentifier methodName = node.getMethodName();
//    if (methodName != null) {
//      Element element = methodName.getStaticElement();
//      if (element instanceof FunctionElement) {
//        FunctionElement functionElement = (FunctionElement) element;
//        if (name.equals(functionElement.getName())) {
//          LibraryElement libraryElement = functionElement.getLibrary();
//          return libraryElement != null && UNITTEST_LIBRARY.equals(libraryElement.getName());
//        }
//      }
//    }
//    return false;
//  }
//
//  private OutlineImpl newClassOutline(Outline unitOutline, List<Outline> unitChildren,
//      ClassDeclaration classDeclaration) {
//    SimpleIdentifier nameNode = classDeclaration.getName();
//    String name = nameNode.getName();
//    ElementImpl element = new ElementImpl(
//        contextId,
//        ElementImpl.createId(classDeclaration.getElement()),
//        source,
//        ElementKind.CLASS,
//        name,
//        nameNode.getOffset(),
//        name.length(),
//        null,
//        null,
//        classDeclaration.isAbstract(),
//        false,
//        StringUtilities.startsWithChar(name, '_'));
//    SourceRegion sourceRegion = getSourceRegion(classDeclaration);
//    OutlineImpl outline = new OutlineImpl(unitOutline, element, sourceRegion);
//    unitChildren.add(outline);
//    return outline;
//  }
//
//  private void newClassTypeAlias(Outline unitOutline, List<Outline> unitChildren,
//      ClassTypeAlias alias) {
//    SimpleIdentifier nameNode = alias.getName();
//    String name = nameNode.getName();
//    ElementImpl element = new ElementImpl(
//        contextId,
//        ElementImpl.createId(alias.getElement()),
//        source,
//        ElementKind.CLASS_TYPE_ALIAS,
//        name,
//        nameNode.getOffset(),
//        nameNode.getLength(),
//        null,
//        null,
//        alias.isAbstract(),
//        false,
//        StringUtilities.startsWithChar(name, '_'));
//    SourceRegion sourceRegion = getSourceRegion(alias);
//    OutlineImpl outline = new OutlineImpl(unitOutline, element, sourceRegion);
//    unitChildren.add(outline);
//  }
//
//  private void newConstructorOutline(OutlineImpl classOutline, List<Outline> children,
//      ConstructorDeclaration constructorDeclaration) {
//    Identifier returnType = constructorDeclaration.getReturnType();
//    String name = returnType.getName();
//    int offset = returnType.getOffset();
//    int length = returnType.getLength();
//    boolean isPrivate = false;
//    SimpleIdentifier constructorNameNode = constructorDeclaration.getName();
//    if (constructorNameNode != null) {
//      String constructorName = constructorNameNode.getName();
//      isPrivate = StringUtilities.startsWithChar(constructorName, '_');
//      name += "." + constructorName;
//      offset = constructorNameNode.getOffset();
//      length = constructorNameNode.getLength();
//    }
//    FormalParameterList parameters = constructorDeclaration.getParameters();
//    ElementImpl element = new ElementImpl(
//        contextId,
//        ElementImpl.createId(constructorDeclaration.getElement()),
//        source,
//        ElementKind.CONSTRUCTOR,
//        name,
//        offset,
//        length,
//        parameters != null ? parameters.toSource() : "",
//        null,
//        false,
//        false,
//        isPrivate);
//    SourceRegion sourceRegion = getSourceRegion(constructorDeclaration);
//    OutlineImpl outline = new OutlineImpl(classOutline, element, sourceRegion);
//    children.add(outline);
//    addLocalFunctionOutlines(outline, constructorDeclaration.getBody());
//  }
//
//  private void newFunctionOutline(Outline parent, List<Outline> children,
//      FunctionDeclaration functionDeclaration) {
//    TypeName returnType = functionDeclaration.getReturnType();
//    SimpleIdentifier nameNode = functionDeclaration.getName();
//    String name = nameNode.getName();
//    FunctionExpression functionExpression = functionDeclaration.getFunctionExpression();
//    FormalParameterList parameters = functionExpression.getParameters();
//    ElementKind kind;
//    if (functionDeclaration.isGetter()) {
//      kind = ElementKind.GETTER;
//    } else if (functionDeclaration.isSetter()) {
//      kind = ElementKind.SETTER;
//    } else {
//      kind = ElementKind.FUNCTION;
//    }
//    ElementImpl element = new ElementImpl(
//        contextId,
//        ElementImpl.createId(functionDeclaration.getElement()),
//        source,
//        kind,
//        name,
//        nameNode.getOffset(),
//        nameNode.getLength(),
//        parameters != null ? parameters.toSource() : "",
//        returnType != null ? returnType.toSource() : "",
//        false,
//        false,
//        StringUtilities.startsWithChar(name, '_'));
//    SourceRegion sourceRegion = getSourceRegion(functionDeclaration);
//    OutlineImpl outline = new OutlineImpl(parent, element, sourceRegion);
//    children.add(outline);
//    addLocalFunctionOutlines(outline, functionExpression.getBody());
//  }
//
//  private void newFunctionTypeAliasOutline(Outline unitOutline, List<Outline> unitChildren,
//      FunctionTypeAlias alias) {
//    TypeName returnType = alias.getReturnType();
//    SimpleIdentifier nameNode = alias.getName();
//    String name = nameNode.getName();
//    FormalParameterList parameters = alias.getParameters();
//    ElementImpl element = new ElementImpl(
//        contextId,
//        ElementImpl.createId(alias.getElement()),
//        source,
//        ElementKind.FUNCTION_TYPE_ALIAS,
//        name,
//        nameNode.getOffset(),
//        nameNode.getLength(),
//        parameters != null ? parameters.toSource() : "",
//        returnType != null ? returnType.toSource() : "",
//        false,
//        false,
//        StringUtilities.startsWithChar(name, '_'));
//    SourceRegion sourceRegion = getSourceRegion(alias);
//    OutlineImpl outline = new OutlineImpl(unitOutline, element, sourceRegion);
//    unitChildren.add(outline);
//  }
//
//  private void newMethodOutline(OutlineImpl classOutline, List<Outline> children,
//      MethodDeclaration methodDeclaration) {
//    TypeName returnType = methodDeclaration.getReturnType();
//    SimpleIdentifier nameNode = methodDeclaration.getName();
//    String name = nameNode.getName();
//    FormalParameterList parameters = methodDeclaration.getParameters();
//    ElementKind kind;
//    if (methodDeclaration.isGetter()) {
//      kind = ElementKind.GETTER;
//    } else if (methodDeclaration.isSetter()) {
//      kind = ElementKind.SETTER;
//    } else {
//      kind = ElementKind.METHOD;
//    }
//    ElementImpl element = new ElementImpl(
//        contextId,
//        ElementImpl.createId(methodDeclaration.getElement()),
//        source,
//        kind,
//        name,
//        nameNode.getOffset(),
//        nameNode.getLength(),
//        parameters != null ? parameters.toSource() : "",
//        returnType != null ? returnType.toSource() : "",
//        methodDeclaration.isAbstract(),
//        methodDeclaration.isStatic(),
//        StringUtilities.startsWithChar(name, '_'));
//    SourceRegion sourceRegion = getSourceRegion(methodDeclaration);
//    OutlineImpl outline = new OutlineImpl(classOutline, element, sourceRegion);
//    children.add(outline);
//    addLocalFunctionOutlines(outline, methodDeclaration.getBody());
//  }
//
//  private OutlineImpl newUnitOutline() {
//    return new OutlineImpl(
//        null,
//        ElementKind.COMPILATION_UNIT,
//        unit.getElement().getDisplayName(),
//        unit.getOffset(),
//        unit.getLength(),
//        0,
//        0,
//        false,
//        false);
//  }
//
//  private void newVariableOutline(OutlineImpl classOutline, List<Outline> children,
//      String typeName, ElementKind kind, VariableDeclaration variable, boolean isStatic) {
//    SimpleIdentifier nameNode = variable.getName();
//    String name = nameNode.getName();
//    ElementImpl element = new ElementImpl(
//        contextId,
//        ElementImpl.createId(variable.getElement()),
//        source,
//        kind,
//        name,
//        nameNode.getOffset(),
//        nameNode.getLength(),
//        null,
//        typeName,
//        false,
//        isStatic,
//        StringUtilities.startsWithChar(name, '_'));
//    SourceRegion sourceRegion = getSourceRegion(variable);
//    OutlineImpl outline = new OutlineImpl(classOutline, element, sourceRegion);
//    children.add(outline);
//  }
}
