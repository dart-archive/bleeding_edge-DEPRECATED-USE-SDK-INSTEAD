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
package com.google.dart.tools.core.dom;

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartArrayAccess;
import com.google.dart.compiler.ast.DartArrayLiteral;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartBooleanLiteral;
import com.google.dart.compiler.ast.DartBreakStatement;
import com.google.dart.compiler.ast.DartCase;
import com.google.dart.compiler.ast.DartCatchBlock;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartClassMember;
import com.google.dart.compiler.ast.DartConditional;
import com.google.dart.compiler.ast.DartContinueStatement;
import com.google.dart.compiler.ast.DartDefault;
import com.google.dart.compiler.ast.DartDirective;
import com.google.dart.compiler.ast.DartDoWhileStatement;
import com.google.dart.compiler.ast.DartDoubleLiteral;
import com.google.dart.compiler.ast.DartEmptyStatement;
import com.google.dart.compiler.ast.DartExprStmt;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFieldDefinition;
import com.google.dart.compiler.ast.DartForInStatement;
import com.google.dart.compiler.ast.DartForStatement;
import com.google.dart.compiler.ast.DartFunction;
import com.google.dart.compiler.ast.DartFunctionExpression;
import com.google.dart.compiler.ast.DartFunctionObjectInvocation;
import com.google.dart.compiler.ast.DartFunctionTypeAlias;
import com.google.dart.compiler.ast.DartGotoStatement;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartIfStatement;
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartInitializer;
import com.google.dart.compiler.ast.DartIntegerLiteral;
import com.google.dart.compiler.ast.DartInvocation;
import com.google.dart.compiler.ast.DartLabel;
import com.google.dart.compiler.ast.DartLibraryDirective;
import com.google.dart.compiler.ast.DartLiteral;
import com.google.dart.compiler.ast.DartMapLiteral;
import com.google.dart.compiler.ast.DartMapLiteralEntry;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNamedExpression;
import com.google.dart.compiler.ast.DartNativeBlock;
import com.google.dart.compiler.ast.DartNativeDirective;
import com.google.dart.compiler.ast.DartNewExpression;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartNullLiteral;
import com.google.dart.compiler.ast.DartParameter;
import com.google.dart.compiler.ast.DartParameterizedTypeNode;
import com.google.dart.compiler.ast.DartParenthesizedExpression;
import com.google.dart.compiler.ast.DartPropertyAccess;
import com.google.dart.compiler.ast.DartRedirectConstructorInvocation;
import com.google.dart.compiler.ast.DartReturnStatement;
import com.google.dart.compiler.ast.DartSourceDirective;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.compiler.ast.DartStringInterpolation;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.ast.DartSuperConstructorInvocation;
import com.google.dart.compiler.ast.DartSuperExpression;
import com.google.dart.compiler.ast.DartSwitchMember;
import com.google.dart.compiler.ast.DartSwitchStatement;
import com.google.dart.compiler.ast.DartSyntheticErrorExpression;
import com.google.dart.compiler.ast.DartSyntheticErrorStatement;
import com.google.dart.compiler.ast.DartThisExpression;
import com.google.dart.compiler.ast.DartThrowExpression;
import com.google.dart.compiler.ast.DartTryStatement;
import com.google.dart.compiler.ast.DartTypeExpression;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.ast.DartTypeParameter;
import com.google.dart.compiler.ast.DartUnaryExpression;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.DartUnqualifiedInvocation;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.ast.DartVariableStatement;
import com.google.dart.compiler.ast.DartWhileStatement;
import com.google.dart.tools.core.DartCore;

import java.util.List;

/**
 * Instances of the class <code>PropertyLocator</code> implement a visitor that returns a description of the
 * property whose value is (or contains) a given child node.
 */
public class PropertyLocator extends ASTVisitor<StructuralPropertyDescriptor> {
  /**
   * The child node whose location is to be returned.
   */
  private DartNode childNode;

  /**
   * Initialize a newly created visitor to return the location of the given child node in the node
   * being visited.
   * 
   * @param childNode the child node whose location is to be returned
   */
  public PropertyLocator(DartNode childNode) {
    this.childNode = childNode;
  }

  @Override
  public void visit(List<? extends DartNode> nodes) {
    if (nodes != null) {
      for (DartNode node : nodes) {
        node.accept(this);
      }
    }
  }

  @Override
  public StructuralPropertyDescriptor visitArrayAccess(DartArrayAccess node) {
    if (childNode == node.getKey()) {
      return PropertyDescriptorHelper.DART_ARRAY_ACCESS_KEY;
    } else if (childNode == node.getTarget()) {
      return PropertyDescriptorHelper.DART_ARRAY_ACCESS_TARGET;
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitArrayLiteral(DartArrayLiteral node) {
    if (childContainedIn(node.getExpressions())) {
      return PropertyDescriptorHelper.DART_ARRAY_LITERAL_EXPRESSIONS;
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitBinaryExpression(DartBinaryExpression node) {
    if (childNode == node.getArg1()) {
      return PropertyDescriptorHelper.DART_BINARY_EXPRESSION_LEFT_OPERAND;
      // } else if (childNode == node.getOperator()) {
      // return PropertyDescriptorHelper.DART_BINARY_EXPRESSION_OPERATOR;
    } else if (childNode == node.getArg2()) {
      return PropertyDescriptorHelper.DART_BINARY_EXPRESSION_RIGHT_OPERAND;
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitBlock(DartBlock node) {
    if (childContainedIn(node.getStatements())) {
      return PropertyDescriptorHelper.DART_BLOCK_STATEMENTS;
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitBooleanLiteral(DartBooleanLiteral node) {
    // if (childNode == PropertyDescriptorHelper.DART_BOOLEAN_LITERAL_VALUE) {
    // return node.getValue();
    // } else {
    return visitLiteral(node);
    // }
  }

  @Override
  public StructuralPropertyDescriptor visitBreakStatement(DartBreakStatement node) {
    return visitGotoStatement(node);
  }

  @Override
  public StructuralPropertyDescriptor visitCase(DartCase node) {
    if (childNode == node.getExpr()) {
      return PropertyDescriptorHelper.DART_CASE_EXPRESSION;
    } else if (childContainedIn(node.getLabels())) {
      return PropertyDescriptorHelper.DART_CASE_LABELS;
    } else if (childContainedIn(node.getStatements())) {
      return PropertyDescriptorHelper.DART_CASE_STATEMENTS;
    } else {
      return visitSwitchMember(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitCatchBlock(DartCatchBlock node) {
    if (childNode == node.getException()) {
      return PropertyDescriptorHelper.DART_CATCH_BLOCK_EXCEPTION;
    } else if (childNode == node.getStackTrace()) {
      return PropertyDescriptorHelper.DART_CATCH_BLOCK_STACK_TRACE;
    } else if (childNode == node.getBlock()) {
      return PropertyDescriptorHelper.DART_CATCH_BLOCK_BODY;
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitClass(DartClass node) {
    if (childNode == node.getDefaultClass()) {
      return PropertyDescriptorHelper.DART_CLASS_DEFAULT_CLASS;
    } else if (childContainedIn(node.getInterfaces())) {
      return PropertyDescriptorHelper.DART_CLASS_INTERFACES;
      // } else if (childNode ==
      // PropertyDescriptorHelper.DART_CLASS_IS_INTERFACE) {
      // return Boolean.valueOf(node.isInterface());
    } else if (childContainedIn(node.getMembers())) {
      return PropertyDescriptorHelper.DART_CLASS_MEMBERS;
    } else if (childNode == node.getName()) {
      return PropertyDescriptorHelper.DART_CLASS_NAME;
    } else if (childNode == node.getSuperclass()) {
      return PropertyDescriptorHelper.DART_CLASS_SUPERCLASS;
    } else if (childContainedIn(node.getTypeParameters())) {
      return PropertyDescriptorHelper.DART_CLASS_TYPE_PARAMETERS;
    } else {
      return visitNode(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitClassMember(DartClassMember<?> node) {
    // if (childNode == node.getModifiers()) {
    // return PropertyDescriptorHelper.DART_CLASS_MEMBER_MODIFIERS;
    // } else
    if (childNode == node.getName()) {
      return PropertyDescriptorHelper.DART_CLASS_MEMBER_NAME;
    } else {
      return visitNode(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitConditional(DartConditional node) {
    if (childNode == node.getCondition()) {
      return PropertyDescriptorHelper.DART_CONDITIONAL_CONDITION;
    } else if (childNode == node.getElseExpression()) {
      return PropertyDescriptorHelper.DART_CONDITIONAL_ELSE;
    } else if (childNode == node.getThenExpression()) {
      return PropertyDescriptorHelper.DART_CONDITIONAL_THEN;
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitContinueStatement(DartContinueStatement node) {
    return visitGotoStatement(node);
  }

  @Override
  public StructuralPropertyDescriptor visitDefault(DartDefault node) {
    return visitSwitchMember(node);
  }

  @Override
  public StructuralPropertyDescriptor visitDirective(DartDirective node) {
    return visitNode(node);
  }

  @Override
  public StructuralPropertyDescriptor visitDoubleLiteral(DartDoubleLiteral node) {
    // if (childNode == PropertyDescriptorHelper.DART_DOUBLE_LITERAL_VALUE) {
    // return Double.valueOf(node.getValue());
    // } else {
    return visitLiteral(node);
    // }
  }

  @Override
  public StructuralPropertyDescriptor visitDoWhileStatement(DartDoWhileStatement node) {
    if (childNode == node.getBody()) {
      return PropertyDescriptorHelper.DART_DO_WHILE_STATEMENT_BODY;
    } else if (childNode == node.getCondition()) {
      return PropertyDescriptorHelper.DART_DO_WHILE_STATEMENT_CONDITION;
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitEmptyStatement(DartEmptyStatement node) {
    return visitStatement(node);
  }

  @Override
  public StructuralPropertyDescriptor visitExpression(DartExpression node) {
    return visitNode(node);
  }

  @Override
  public StructuralPropertyDescriptor visitExprStmt(DartExprStmt node) {
    if (childNode == node.getExpression()) {
      return PropertyDescriptorHelper.DART_EXPRESSION_STATEMENT_EXPRESSION;
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitField(DartField node) {
    if (childNode == node.getValue()) {
      return PropertyDescriptorHelper.DART_FIELD_VALUE;
    } else {
      return visitClassMember(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitFieldDefinition(DartFieldDefinition node) {
    if (childNode == node.getType()) {
      return PropertyDescriptorHelper.DART_FIELD_DEFINITION_TYPE;
    } else if (childContainedIn(node.getFields())) {
      return PropertyDescriptorHelper.DART_FIELD_DEFINITION_FIELDS;
    } else {
      return visitNode(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitForInStatement(DartForInStatement node) {
    if (childNode == node.getBody()) {
      return PropertyDescriptorHelper.DART_FOR_IN_STATEMENT_BODY;
    } else if (childNode == node.getIterable()) {
      return PropertyDescriptorHelper.DART_FOR_IN_STATEMENT_ITERABLE;
    } else if (childNode == node.getVariableStatement()) {
      return PropertyDescriptorHelper.DART_FOR_IN_STATEMENT_VARIABLE;
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitForStatement(DartForStatement node) {
    if (childNode == node.getBody()) {
      return PropertyDescriptorHelper.DART_FOR_STATEMENT_BODY;
    } else if (childNode == node.getCondition()) {
      return PropertyDescriptorHelper.DART_FOR_STATEMENT_CONDITION;
    } else if (childNode == node.getIncrement()) {
      return PropertyDescriptorHelper.DART_FOR_STATEMENT_INCREMENT;
    } else if (childNode == node.getInit()) {
      return PropertyDescriptorHelper.DART_FOR_STATEMENT_INIT;
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitFunction(DartFunction node) {
    if (childNode == node.getBody()) {
      return PropertyDescriptorHelper.DART_FUNCTION_BODY;
    } else if (childContainedIn(node.getParameters())) {
      return PropertyDescriptorHelper.DART_FUNCTION_PARAMETERS;
    } else if (childNode == node.getReturnTypeNode()) {
      return PropertyDescriptorHelper.DART_FUNCTION_RETURN_TYPE;
    } else {
      return visitNode(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitFunctionExpression(DartFunctionExpression node) {
    if (childNode == node.getFunction()) {
      return PropertyDescriptorHelper.DART_FUNCTION_EXPRESSION_FUNCTION;
      // } else if (childNode ==
      // PropertyDescriptorHelper.DART_FUNCTION_EXPRESSION_IS_STATEMENT) {
      // return Boolean.valueOf(node.isStatement());
    } else if (childNode == node.getName()) {
      return PropertyDescriptorHelper.DART_FUNCTION_EXPRESSION_NAME;
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitFunctionObjectInvocation(
      DartFunctionObjectInvocation node) {
    if (childNode == node.getTarget()) {
      return PropertyDescriptorHelper.DART_FUNCTION_OBJECT_INVOCATION_TARGET;
    } else {
      return visitInvocation(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitFunctionTypeAlias(DartFunctionTypeAlias node) {
    if (childNode == node.getName()) {
      return PropertyDescriptorHelper.DART_FUNCTION_TYPE_ALIAS_NAME;
      // } else if (childNode == node.getReturnType()) {
      // return PropertyDescriptorHelper.DART_FUNCTION_TYPE_ALIAS_RETURN_TYPE;
      // } else if (childContainedIn(node.getParameters())) {
      // return PropertyDescriptorHelper.DART_FUNCTION_TYPE_ALIAS_PARAMETERS;
    } else {
      return visitNode(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitGotoStatement(DartGotoStatement node) {
    if (childNode == node.getLabel()) {
      return PropertyDescriptorHelper.DART_GOTO_STATEMENT_LABEL;
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitIdentifier(DartIdentifier node) {
    // if (childNode == node.getTargetName()) {
    // return PropertyDescriptorHelper.DART_IDENTIFIER_TARGET_NAME;
    // } else {
    return visitExpression(node);
    // }
  }

  @Override
  public StructuralPropertyDescriptor visitIfStatement(DartIfStatement node) {
    if (childNode == node.getCondition()) {
      return PropertyDescriptorHelper.DART_IF_STATEMENT_CONDITION;
    } else if (childNode == node.getElseStatement()) {
      return PropertyDescriptorHelper.DART_IF_STATEMENT_ELSE;
    } else if (childNode == node.getThenStatement()) {
      return PropertyDescriptorHelper.DART_IF_STATEMENT_THEN;
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitImportDirective(DartImportDirective node) {
    if (childNode == node.getLibraryUri()) {
      return PropertyDescriptorHelper.DART_IMPORT_DIRECTIVE_URI;
    } else if (childNode == node.getPrefix()) {
      return PropertyDescriptorHelper.DART_IMPORT_DIRECTIVE_PREFIX;
    } else {
      return visitDirective(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitInitializer(DartInitializer node) {
    if (childNode == node.getName()) {
      return PropertyDescriptorHelper.DART_INITIALIZER_NAME;
    } else if (childNode == node.getValue()) {
      return PropertyDescriptorHelper.DART_INITIALIZER_VALUE;
    } else {
      return visitNode(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitIntegerLiteral(DartIntegerLiteral node) {
    // if (childNode == node.getValue()) {
    // return PropertyDescriptorHelper.DART_INTEGER_LITERAL_VALUE;
    // } else {
    return visitLiteral(node);
    // }
  }

  @Override
  public StructuralPropertyDescriptor visitInvocation(DartInvocation node) {
    if (childContainedIn(node.getArguments())) {
      return PropertyDescriptorHelper.DART_INVOCATION_ARGS;
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitLabel(DartLabel node) {
    if (childNode == node.getLabel()) {
      return PropertyDescriptorHelper.DART_LABELED_STATEMENT_LABEL;
    } else if (childNode == node.getStatement()) {
      return PropertyDescriptorHelper.DART_LABELED_STATEMENT_STATEMENT;
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitLibraryDirective(DartLibraryDirective node) {
    if (childNode == node.getName()) {
      return PropertyDescriptorHelper.DART_LIBRARY_DIRECTIVE_NAME;
    } else {
      return visitDirective(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitLiteral(DartLiteral node) {
    return visitExpression(node);
  }

  @Override
  public StructuralPropertyDescriptor visitMapLiteral(DartMapLiteral node) {
    if (childContainedIn(node.getEntries())) {
      return PropertyDescriptorHelper.DART_MAP_LITERAL_ENTRIES;
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitMapLiteralEntry(DartMapLiteralEntry node) {
    if (childNode == node.getKey()) {
      return PropertyDescriptorHelper.DART_MAP_LITERAL_ENTRY_KEY;
    } else if (childNode == node.getValue()) {
      return PropertyDescriptorHelper.DART_MAP_LITERAL_ENTRY_VALUE;
    } else {
      return visitNode(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitMethodDefinition(DartMethodDefinition node) {
    if (childNode == node.getFunction()) {
      return PropertyDescriptorHelper.DART_METHOD_DEFINITION_FUNCTION;
    } else if (childContainedIn(node.getInitializers())) {
      return PropertyDescriptorHelper.DART_METHOD_DEFINITION_INITIALIZERS;
    } else {
      return visitClassMember(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitMethodInvocation(DartMethodInvocation node) {
    if (childNode == node.getTarget()) {
      return PropertyDescriptorHelper.DART_METHOD_INVOCATION_TARGET;
    } else if (childNode == node.getFunctionName()) {
      return PropertyDescriptorHelper.DART_METHOD_INVOCATION_FUNCTION_NAME;
    } else {
      return visitInvocation(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitNamedExpression(DartNamedExpression node) {
//    if (childNode == node.getName()) {
//      return PropertyDescriptorHelper.DART_NAMED_EXPRESSION_NAME;
//    } else
    DartCore.notYetImplemented();
    if (childNode == node.getExpression()) {
      return PropertyDescriptorHelper.DART_NAMED_EXPRESSION_EXPRESSION;
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitNativeBlock(DartNativeBlock node) {
    return visitBlock(node);
  }

  @Override
  public StructuralPropertyDescriptor visitNativeDirective(DartNativeDirective node) {
    if (childNode == node.getNativeUri()) {
      return PropertyDescriptorHelper.DART_NATIVE_DIRECTIVE_URI;
    } else {
      return visitDirective(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitNewExpression(DartNewExpression node) {
    if (childNode == node.getConstructor()) {
      return PropertyDescriptorHelper.DART_NEW_EXPRESSION_CONSTRUCTOR_NAME;
    } else if (childContainedIn(node.getArguments())) {
      return PropertyDescriptorHelper.DART_NEW_EXPRESSION_ARGUMENTS;
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitNode(DartNode node) {
    // noSuchProperty(node.getClass().getName());
    return null;
  }

  @Override
  public StructuralPropertyDescriptor visitNullLiteral(DartNullLiteral node) {
    return visitLiteral(node);
  }

  @Override
  public StructuralPropertyDescriptor visitParameter(DartParameter node) {
    if (childNode == node.getDefaultExpr()) {
      return PropertyDescriptorHelper.DART_PARAMETER_DEFAULT_EXPRESSION;
    } else if (childContainedIn(node.getFunctionParameters())) {
      return PropertyDescriptorHelper.DART_PARAMETER_FUNCTION_PARAMETERS;
      // } else if (childNode ==
      // PropertyDescriptorHelper.DART_PARAMETER_IS_CONST) {
      // return Boolean.valueOf(node.isConst());
      // } else if (childNode ==
      // PropertyDescriptorHelper.DART_PARAMETER_IS_VARIADIC) {
      // return Boolean.valueOf(node.isVariadic());
    } else if (childNode == node.getName()) {
      return PropertyDescriptorHelper.DART_PARAMETER_NAME;
      // } else if (childNode == node.getType()) {
      // return PropertyDescriptorHelper.DART_PARAMETER_TYPE_NAME;
    } else {
      return visitNode(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitParameterizedTypeNode(DartParameterizedTypeNode node) {
    if (childNode == node.getExpression()) {
      return PropertyDescriptorHelper.DART_PARAMETERIZED_NODE_EXPRESSION;
    } else if (childContainedIn(node.getTypeParameters())) {
      return PropertyDescriptorHelper.DART_PARAMETERIZED_NODE_TYPE_PARAMETERS;
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitParenthesizedExpression(
      DartParenthesizedExpression node) {
    if (childNode == node.getExpression()) {
      return PropertyDescriptorHelper.DART_PARENTHESIZED_EXPRESSION_EXPRESSION;
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitPropertyAccess(DartPropertyAccess node) {
    if (childNode == node.getQualifier()) {
      return PropertyDescriptorHelper.DART_PROPERTY_ACCESS_QUALIFIER;
    } else if (childNode == node.getName()) {
      return PropertyDescriptorHelper.DART_PROPERTY_ACCESS_NAME;
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitRedirectConstructorInvocation(
      DartRedirectConstructorInvocation node) {
    if (childNode == node.getName()) {
      return PropertyDescriptorHelper.DART_REDIRECT_CONSTRUCTOR_INVOCATION_NAME;
    } else {
      return visitInvocation(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitReturnStatement(DartReturnStatement node) {
    if (childNode == node.getValue()) {
      return PropertyDescriptorHelper.DART_RETURN_STATEMENT_VALUE;
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitSourceDirective(DartSourceDirective node) {
    if (childNode == node.getSourceUri()) {
      return PropertyDescriptorHelper.DART_SOURCE_DIRECTIVE_URI;
    } else {
      return visitDirective(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitStatement(DartStatement node) {
    return visitNode(node);
  }

  @Override
  public StructuralPropertyDescriptor visitStringInterpolation(DartStringInterpolation node) {
    if (childContainedIn(node.getExpressions())) {
      return PropertyDescriptorHelper.DART_STRING_INTERPOLATION_EXPRESSIONS;
    } else if (childContainedIn(node.getStrings())) {
      return PropertyDescriptorHelper.DART_STRING_INTERPOLATION_STRINGS;
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitStringLiteral(DartStringLiteral node) {
    // if (childNode == node.getValue()) {
    // return PropertyDescriptorHelper.DART_STRING_LITERAL_VALUE;
    // } else {
    return visitLiteral(node);
    // }
  }

  @Override
  public StructuralPropertyDescriptor visitSuperConstructorInvocation(
      DartSuperConstructorInvocation node) {
    // if (childNode == node.getConstructorName()) {
    // return PropertyDescriptorHelper.DART_CALL_SUPER_CONSTRUCTOR_NAME;
    // } else {
    return visitInvocation(node);
    // }
  }

  @Override
  public StructuralPropertyDescriptor visitSuperExpression(DartSuperExpression node) {
    return visitExpression(node);
  }

  @Override
  public StructuralPropertyDescriptor visitSwitchMember(DartSwitchMember node) {
    if (childContainedIn(node.getLabels())) {
      return PropertyDescriptorHelper.DART_SWITCH_MEMBER_LABELS;
    } else if (childContainedIn(node.getStatements())) {
      return PropertyDescriptorHelper.DART_SWITCH_MEMBER_STATEMENTS;
    } else {
      return visitNode(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitSwitchStatement(DartSwitchStatement node) {
    if (childNode == node.getExpression()) {
      return PropertyDescriptorHelper.DART_SWITCH_STATEMENT_EXPRESSION;
    } else if (childContainedIn(node.getMembers())) {
      return PropertyDescriptorHelper.DART_SWITCH_STATEMENT_MEMBERS;
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitSyntheticErrorExpression(
      DartSyntheticErrorExpression node) {
//     if (childNode == node.getTokenString()) {
//     return PropertyDescriptorHelper.DART_SYNTHETIC_ERROR_EXPRESSION_TOKEN_STRING;
//     } else {
    return visitExpression(node);
    // }
  }

  @Override
  public StructuralPropertyDescriptor visitSyntheticErrorStatement(
      DartSyntheticErrorStatement node) {
    // if (childNode == node.getTokenString()) {
    // return PropertyDescriptorHelper.DART_SYNTHETIC_ERROR_STATEMENT_TOKEN_STRING;
    // } else {
    return visitStatement(node);
    // }
  }

  @Override
  public StructuralPropertyDescriptor visitThisExpression(DartThisExpression node) {
    return visitExpression(node);
  }

  @Override
  public StructuralPropertyDescriptor visitThrowExpression(DartThrowExpression node) {
    if (childNode == node.getException()) {
      return PropertyDescriptorHelper.DART_THROW_STATEMENT_EXCEPTION;
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitTryStatement(DartTryStatement node) {
    if (childNode == node.getCatchBlocks()) {
      return PropertyDescriptorHelper.DART_TRY_STATEMENT_CATCH_BLOCKS;
    } else if (childNode == node.getFinallyBlock()) {
      return PropertyDescriptorHelper.DART_TRY_STATEMENT_FINALY_BLOCK;
    } else if (childNode == node.getTryBlock()) {
      return PropertyDescriptorHelper.DART_TRY_STATEMENT_TRY_BLOCK;
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitTypeExpression(DartTypeExpression node) {
    // if (childNode == node.getType()) {
    // return PropertyDescriptorHelper.DART_TYPE_EXPRESSION_TYPE;
    // } else {
    return visitExpression(node);
    // }
  }

  @Override
  public StructuralPropertyDescriptor visitTypeNode(DartTypeNode node) {
    if (childNode == node.getTypeArguments()) {
      return PropertyDescriptorHelper.DART_TYPE_TYPE_ARGUMENTS;
    } else if (childNode == node.getIdentifier()) {
      return PropertyDescriptorHelper.DART_TYPE_IDENTIFIER;
    } else {
      return visitNode(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitTypeParameter(DartTypeParameter node) {
    if (childNode == node.getBound()) {
      return PropertyDescriptorHelper.DART_TYPE_PARAMETER_BOUND;
    } else if (childNode == node.getName()) {
      return PropertyDescriptorHelper.DART_TYPE_PARAMETER_NAME;
    } else {
      return visitNode(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitUnaryExpression(DartUnaryExpression node) {
    // if (childNode == Boolean.valueOf(node.isPrefix())) {
    // return PropertyDescriptorHelper.DART_UNARY_EXPRESSION_IS_PREFIX;
    // } else
    if (childNode == node.getArg()) {
      return PropertyDescriptorHelper.DART_UNARY_EXPRESSION_OPERAND;
      // } else if (childNode == node.getOperator()) {
      // return PropertyDescriptorHelper.DART_UNARY_EXPRESSION_OPERATOR;
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitUnit(DartUnit node) {
    if (childContainedIn(node.getComments())) {
      return PropertyDescriptorHelper.DART_UNIT_COMMENTS;
    } else if (childContainedIn(node.getTopLevelNodes())) {
      return PropertyDescriptorHelper.DART_UNIT_MEMBERS;
    } else {
      return visitNode(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitUnqualifiedInvocation(DartUnqualifiedInvocation node) {
    if (childNode == node.getTarget()) {
      return PropertyDescriptorHelper.DART_METHOD_INVOCATION_TARGET;
    } else {
      return visitInvocation(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitVariable(DartVariable node) {
    if (childNode == node.getName()) {
      return PropertyDescriptorHelper.DART_VARIABLE_NAME;
    } else if (childNode == node.getValue()) {
      return PropertyDescriptorHelper.DART_VARIABLE_VALUE;
    } else {
      return visitNode(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitVariableStatement(DartVariableStatement node) {
    // if (childNode == node.getType()) {
    // return PropertyDescriptorHelper.DART_VARIABLE_STATEMENT_TYPE;
    // } else
    if (childNode == node.getVariables()) {
      return PropertyDescriptorHelper.DART_VARIABLE_STATEMENT_VARIABLES;
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public StructuralPropertyDescriptor visitWhileStatement(DartWhileStatement node) {
    if (childNode == node.getBody()) {
      return PropertyDescriptorHelper.DART_WHILE_STATEMENT_BODY;
    } else if (childNode == node.getCondition()) {
      return PropertyDescriptorHelper.DART_WHILE_STATEMENT_CONDITION;
    } else {
      return visitStatement(node);
    }
  }

  private boolean childContainedIn(List<? extends DartNode> nodeList) {
    return nodeList != null && nodeList.contains(childNode);
  }
}
