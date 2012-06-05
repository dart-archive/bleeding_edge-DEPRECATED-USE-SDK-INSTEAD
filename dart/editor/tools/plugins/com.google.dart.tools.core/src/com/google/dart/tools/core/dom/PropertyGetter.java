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
import com.google.dart.compiler.ast.DartComment;
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
import com.google.dart.compiler.ast.DartResourceDirective;
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
import com.google.dart.compiler.ast.DartThrowStatement;
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

/**
 * Instances of the class <code>PropertyGetter</code> implement a visitor that captures the value of
 * the specified property for the node being visited.
 */
public class PropertyGetter extends PropertyVisitor {
  /**
   * Initialize a newly created visitor to get the value of the given property.
   * 
   * @param property the specification of the property to be returned
   */
  public PropertyGetter(StructuralPropertyDescriptor property) {
    super(property);
  }

  @Override
  public Object visitArrayAccess(DartArrayAccess node) {
    if (property == PropertyDescriptorHelper.DART_ARRAY_ACCESS_KEY) {
      return node.getKey();
    } else if (property == PropertyDescriptorHelper.DART_ARRAY_ACCESS_TARGET) {
      return node.getTarget();
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public Object visitArrayLiteral(DartArrayLiteral node) {
    if (property == PropertyDescriptorHelper.DART_ARRAY_LITERAL_EXPRESSIONS) {
      return node.getExpressions();
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public Object visitBinaryExpression(DartBinaryExpression node) {
    if (property == PropertyDescriptorHelper.DART_BINARY_EXPRESSION_LEFT_OPERAND) {
      return node.getArg1();
    } else if (property == PropertyDescriptorHelper.DART_BINARY_EXPRESSION_OPERATOR) {
      return node.getOperator();
    } else if (property == PropertyDescriptorHelper.DART_BINARY_EXPRESSION_RIGHT_OPERAND) {
      return node.getArg2();
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public Object visitBlock(DartBlock node) {
    if (property == PropertyDescriptorHelper.DART_BLOCK_STATEMENTS) {
      return node.getStatements();
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public Object visitBooleanLiteral(DartBooleanLiteral node) {
    if (property == PropertyDescriptorHelper.DART_BOOLEAN_LITERAL_VALUE) {
      return Boolean.valueOf(node.getValue());
    } else {
      return visitLiteral(node);
    }
  }

  @Override
  public Object visitBreakStatement(DartBreakStatement node) {
    return visitGotoStatement(node);
  }

  @Override
  public Object visitCase(DartCase node) {
    if (property == PropertyDescriptorHelper.DART_CASE_EXPRESSION) {
      return node.getExpr();
    } else if (property == PropertyDescriptorHelper.DART_CASE_LABEL) {
      return node.getLabel();
    } else if (property == PropertyDescriptorHelper.DART_CASE_STATEMENTS) {
      return node.getStatements();
    } else {
      return visitSwitchMember(node);
    }
  }

  @Override
  public Object visitCatchBlock(DartCatchBlock node) {
    if (property == PropertyDescriptorHelper.DART_CATCH_BLOCK_EXCEPTION) {
      return node.getException();
    } else if (property == PropertyDescriptorHelper.DART_CATCH_BLOCK_STACK_TRACE) {
      return node.getStackTrace();
    } else if (property == PropertyDescriptorHelper.DART_CATCH_BLOCK_BODY) {
      return node.getBlock();
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public Object visitClass(DartClass node) {
    if (property == PropertyDescriptorHelper.DART_CLASS_DEFAULT_CLASS) {
      return node.getDefaultClass();
    } else if (property == PropertyDescriptorHelper.DART_CLASS_INTERFACES) {
      return node.getInterfaces();
    } else if (property == PropertyDescriptorHelper.DART_CLASS_IS_INTERFACE) {
      return Boolean.valueOf(node.isInterface());
    } else if (property == PropertyDescriptorHelper.DART_CLASS_MEMBERS) {
      return node.getMembers();
    } else if (property == PropertyDescriptorHelper.DART_CLASS_NAME) {
      return node.getName();
    } else if (property == PropertyDescriptorHelper.DART_CLASS_SUPERCLASS) {
      return node.getSuperclass();
    } else if (property == PropertyDescriptorHelper.DART_CLASS_TYPE_PARAMETERS) {
      return node.getTypeParameters();
    } else {
      return visitNode(node);
    }
  }

  @Override
  public Object visitClassMember(DartClassMember<?> node) {
    if (property == PropertyDescriptorHelper.DART_CLASS_MEMBER_MODIFIERS) {
      return node.getModifiers();
    } else if (property == PropertyDescriptorHelper.DART_CLASS_MEMBER_NAME) {
      return node.getName();
    } else {
      return visitNode(node);
    }
  }

  @Override
  public Object visitComment(DartComment node) {
    return visitNode(node);
  }

  @Override
  public Object visitConditional(DartConditional node) {
    if (property == PropertyDescriptorHelper.DART_CONDITIONAL_CONDITION) {
      return node.getCondition();
    } else if (property == PropertyDescriptorHelper.DART_CONDITIONAL_ELSE) {
      return node.getElseExpression();
    } else if (property == PropertyDescriptorHelper.DART_CONDITIONAL_THEN) {
      return node.getThenExpression();
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public Object visitContinueStatement(DartContinueStatement node) {
    return visitGotoStatement(node);
  }

  @Override
  public Object visitDefault(DartDefault node) {
    return visitSwitchMember(node);
  }

  @Override
  public Object visitDirective(DartDirective node) {
    return visitNode(node);
  }

  @Override
  public Object visitDoubleLiteral(DartDoubleLiteral node) {
    if (property == PropertyDescriptorHelper.DART_DOUBLE_LITERAL_VALUE) {
      return Double.valueOf(node.getValue());
    } else {
      return visitLiteral(node);
    }
  }

  @Override
  public Object visitDoWhileStatement(DartDoWhileStatement node) {
    if (property == PropertyDescriptorHelper.DART_DO_WHILE_STATEMENT_BODY) {
      return node.getBody();
    } else if (property == PropertyDescriptorHelper.DART_DO_WHILE_STATEMENT_CONDITION) {
      return node.getCondition();
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public Object visitEmptyStatement(DartEmptyStatement node) {
    return visitStatement(node);
  }

  @Override
  public Object visitExpression(DartExpression node) {
    return visitNode(node);
  }

  @Override
  public Object visitExprStmt(DartExprStmt node) {
    if (property == PropertyDescriptorHelper.DART_EXPRESSION_STATEMENT_EXPRESSION) {
      return node.getExpression();
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public Object visitField(DartField node) {
    if (property == PropertyDescriptorHelper.DART_FIELD_VALUE) {
      return node.getValue();
    } else {
      return visitClassMember(node);
    }
  }

  @Override
  public Object visitFieldDefinition(DartFieldDefinition node) {
    if (property == PropertyDescriptorHelper.DART_FIELD_DEFINITION_TYPE) {
      return node.getType();
    } else if (property == PropertyDescriptorHelper.DART_FIELD_DEFINITION_FIELDS) {
      return node.getFields();
    } else {
      return visitNode(node);
    }
  }

  @Override
  public Object visitForInStatement(DartForInStatement node) {
    if (property == PropertyDescriptorHelper.DART_FOR_IN_STATEMENT_BODY) {
      return node.getBody();
    } else if (property == PropertyDescriptorHelper.DART_FOR_IN_STATEMENT_ITERABLE) {
      return node.getIterable();
    } else if (property == PropertyDescriptorHelper.DART_FOR_IN_STATEMENT_VARIABLE) {
      return node.getVariableStatement();
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public Object visitForStatement(DartForStatement node) {
    if (property == PropertyDescriptorHelper.DART_FOR_STATEMENT_BODY) {
      return node.getBody();
    } else if (property == PropertyDescriptorHelper.DART_FOR_STATEMENT_CONDITION) {
      return node.getCondition();
    } else if (property == PropertyDescriptorHelper.DART_FOR_STATEMENT_INCREMENT) {
      return node.getIncrement();
    } else if (property == PropertyDescriptorHelper.DART_FOR_STATEMENT_INIT) {
      return node.getInit();
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public Object visitFunction(DartFunction node) {
    if (property == PropertyDescriptorHelper.DART_FUNCTION_BODY) {
      return node.getBody();
    } else if (property == PropertyDescriptorHelper.DART_FUNCTION_PARAMETERS) {
      return node.getParameters();
    } else if (property == PropertyDescriptorHelper.DART_FUNCTION_RETURN_TYPE) {
      return node.getReturnTypeNode();
    } else {
      return visitNode(node);
    }
  }

  @Override
  public Object visitFunctionExpression(DartFunctionExpression node) {
    if (property == PropertyDescriptorHelper.DART_FUNCTION_EXPRESSION_FUNCTION) {
      return node.getFunction();
    } else if (property == PropertyDescriptorHelper.DART_FUNCTION_EXPRESSION_IS_STATEMENT) {
      return Boolean.valueOf(node.isStatement());
    } else if (property == PropertyDescriptorHelper.DART_FUNCTION_EXPRESSION_NAME) {
      return node.getName();
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public Object visitFunctionObjectInvocation(DartFunctionObjectInvocation node) {
    if (property == PropertyDescriptorHelper.DART_FUNCTION_OBJECT_INVOCATION_TARGET) {
      return node.getTarget();
    } else {
      return visitInvocation(node);
    }
  }

  @Override
  public Object visitFunctionTypeAlias(DartFunctionTypeAlias node) {
    if (property == PropertyDescriptorHelper.DART_FUNCTION_TYPE_ALIAS_NAME) {
      return node.getName();
    } else if (property == PropertyDescriptorHelper.DART_FUNCTION_TYPE_ALIAS_RETURN_TYPE) {
      return node.getReturnTypeNode();
    } else if (property == PropertyDescriptorHelper.DART_FUNCTION_TYPE_ALIAS_PARAMETERS) {
      return node.getParameters();
    } else {
      return visitNode(node);
    }
  }

  @Override
  public Object visitGotoStatement(DartGotoStatement node) {
    if (property == PropertyDescriptorHelper.DART_GOTO_STATEMENT_LABEL) {
      return node.getLabel();
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public Object visitIdentifier(DartIdentifier node) {
    if (property == PropertyDescriptorHelper.DART_IDENTIFIER_TARGET_NAME) {
      return node.getName();
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public Object visitIfStatement(DartIfStatement node) {
    if (property == PropertyDescriptorHelper.DART_IF_STATEMENT_CONDITION) {
      return node.getCondition();
    } else if (property == PropertyDescriptorHelper.DART_IF_STATEMENT_ELSE) {
      return node.getElseStatement();
    } else if (property == PropertyDescriptorHelper.DART_IF_STATEMENT_THEN) {
      return node.getThenStatement();
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public Object visitImportDirective(DartImportDirective node) {
    if (property == PropertyDescriptorHelper.DART_IMPORT_DIRECTIVE_URI) {
      return node.getLibraryUri();
    } else if (property == PropertyDescriptorHelper.DART_IMPORT_DIRECTIVE_PREFIX) {
      return node.getPrefix();
    } else {
      return visitDirective(node);
    }
  }

  @Override
  public Object visitInitializer(DartInitializer node) {
    if (property == PropertyDescriptorHelper.DART_INITIALIZER_NAME) {
      return node.getName();
    } else if (property == PropertyDescriptorHelper.DART_INITIALIZER_VALUE) {
      return node.getValue();
    } else {
      return visitNode(node);
    }
  }

  @Override
  public Object visitIntegerLiteral(DartIntegerLiteral node) {
    if (property == PropertyDescriptorHelper.DART_INTEGER_LITERAL_VALUE) {
      return node.getValue();
    } else {
      return visitLiteral(node);
    }
  }

  @Override
  public Object visitInvocation(DartInvocation node) {
    if (property == PropertyDescriptorHelper.DART_INVOCATION_ARGS) {
      return node.getArguments();
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public Object visitLabel(DartLabel node) {
    if (property == PropertyDescriptorHelper.DART_LABELED_STATEMENT_LABEL) {
      return node.getLabel();
    } else if (property == PropertyDescriptorHelper.DART_LABELED_STATEMENT_STATEMENT) {
      return node.getStatement();
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public Object visitLibraryDirective(DartLibraryDirective node) {
    if (property == PropertyDescriptorHelper.DART_LIBRARY_DIRECTIVE_NAME) {
      return node.getName();
    } else {
      return visitDirective(node);
    }
  }

  @Override
  public Object visitLiteral(DartLiteral node) {
    return visitExpression(node);
  }

  @Override
  public Object visitMapLiteral(DartMapLiteral node) {
    if (property == PropertyDescriptorHelper.DART_MAP_LITERAL_ENTRIES) {
      return node.getEntries();
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public Object visitMapLiteralEntry(DartMapLiteralEntry node) {
    if (property == PropertyDescriptorHelper.DART_MAP_LITERAL_ENTRY_KEY) {
      return node.getKey();
    } else if (property == PropertyDescriptorHelper.DART_MAP_LITERAL_ENTRY_VALUE) {
      return node.getValue();
    } else {
      return visitNode(node);
    }
  }

  @Override
  public Object visitMethodDefinition(DartMethodDefinition node) {
    if (property == PropertyDescriptorHelper.DART_METHOD_DEFINITION_FUNCTION) {
      return node.getFunction();
    } else if (property == PropertyDescriptorHelper.DART_METHOD_DEFINITION_INITIALIZERS) {
      return node.getInitializers();
    } else {
      return visitClassMember(node);
    }
  }

  @Override
  public Object visitMethodInvocation(DartMethodInvocation node) {
    if (property == PropertyDescriptorHelper.DART_METHOD_INVOCATION_TARGET) {
      return node.getTarget();
    } else if (property == PropertyDescriptorHelper.DART_METHOD_INVOCATION_FUNCTION_NAME) {
      return node.getFunctionName();
    } else {
      return visitInvocation(node);
    }
  }

  @Override
  public Object visitNamedExpression(DartNamedExpression node) {
    if (property == PropertyDescriptorHelper.DART_NAMED_EXPRESSION_NAME) {
      return node.getName();
    } else if (property == PropertyDescriptorHelper.DART_NAMED_EXPRESSION_EXPRESSION) {
      return node.getExpression();
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public Object visitNativeBlock(DartNativeBlock node) {
    return visitBlock(node);
  }

  @Override
  public Object visitNativeDirective(DartNativeDirective node) {
    if (property == PropertyDescriptorHelper.DART_NATIVE_DIRECTIVE_URI) {
      return node.getNativeUri();
    } else {
      return visitDirective(node);
    }
  }

  @Override
  public Object visitNewExpression(DartNewExpression node) {
    if (property == PropertyDescriptorHelper.DART_NEW_EXPRESSION_CONSTRUCTOR_NAME) {
      return node.getConstructor();
    } else if (property == PropertyDescriptorHelper.DART_NEW_EXPRESSION_ARGUMENTS) {
      return node.getArguments();
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public Object visitNode(DartNode node) {
    noSuchProperty(node.getClass().getName());
    return null;
  }

  @Override
  public Object visitNullLiteral(DartNullLiteral node) {
    return visitLiteral(node);
  }

  @Override
  public Object visitParameter(DartParameter node) {
    if (property == PropertyDescriptorHelper.DART_PARAMETER_DEFAULT_EXPRESSION) {
      return node.getDefaultExpr();
    } else if (property == PropertyDescriptorHelper.DART_PARAMETER_FUNCTION_PARAMETERS) {
      return node.getFunctionParameters();
    } else if (property == PropertyDescriptorHelper.DART_PARAMETER_IS_CONST) {
      return Boolean.valueOf(node.getModifiers().isConstant());
    } else if (property == PropertyDescriptorHelper.DART_PARAMETER_NAME) {
      return node.getName();
    } else if (property == PropertyDescriptorHelper.DART_PARAMETER_TYPE_NAME) {
      return node.getType();
    } else {
      return visitNode(node);
    }
  }

  @Override
  public Object visitParameterizedTypeNode(DartParameterizedTypeNode node) {
    if (property == PropertyDescriptorHelper.DART_PARAMETERIZED_NODE_EXPRESSION) {
      return node.getExpression();
    } else if (property == PropertyDescriptorHelper.DART_PARAMETERIZED_NODE_TYPE_PARAMETERS) {
      return node.getTypeParameters();
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public Object visitParenthesizedExpression(DartParenthesizedExpression node) {
    if (property == PropertyDescriptorHelper.DART_PARENTHESIZED_EXPRESSION_EXPRESSION) {
      return node.getExpression();
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public Object visitPropertyAccess(DartPropertyAccess node) {
    if (property == PropertyDescriptorHelper.DART_PROPERTY_ACCESS_QUALIFIER) {
      return node.getQualifier();
    } else if (property == PropertyDescriptorHelper.DART_PROPERTY_ACCESS_NAME) {
      return node.getName();
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public Object visitRedirectConstructorInvocation(DartRedirectConstructorInvocation node) {
    if (property == PropertyDescriptorHelper.DART_REDIRECT_CONSTRUCTOR_INVOCATION_NAME) {
      return node.getName();
    } else {
      return visitInvocation(node);
    }
  }

  @Override
  public Object visitResourceDirective(DartResourceDirective node) {
    if (property == PropertyDescriptorHelper.DART_RESOURCE_DIRECTIVE_URI) {
      return node.getResourceUri();
    } else {
      return visitDirective(node);
    }
  }

  @Override
  public Object visitReturnStatement(DartReturnStatement node) {
    if (property == PropertyDescriptorHelper.DART_RETURN_STATEMENT_VALUE) {
      return node.getValue();
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public Object visitSourceDirective(DartSourceDirective node) {
    if (property == PropertyDescriptorHelper.DART_SOURCE_DIRECTIVE_URI) {
      return node.getSourceUri();
    } else {
      return visitDirective(node);
    }
  }

  @Override
  public Object visitStatement(DartStatement node) {
    return visitNode(node);
  }

  @Override
  public Object visitStringInterpolation(DartStringInterpolation node) {
    if (property == PropertyDescriptorHelper.DART_STRING_INTERPOLATION_EXPRESSIONS) {
      return node.getExpressions();
    } else if (property == PropertyDescriptorHelper.DART_STRING_INTERPOLATION_STRINGS) {
      return node.getStrings();
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public Object visitStringLiteral(DartStringLiteral node) {
    if (property == PropertyDescriptorHelper.DART_STRING_LITERAL_VALUE) {
      return node.getValue();
    } else {
      return visitLiteral(node);
    }
  }

  @Override
  public Object visitSuperConstructorInvocation(DartSuperConstructorInvocation node) {
    if (property == PropertyDescriptorHelper.DART_SUPER_CONSTRUCTOR_INVOCATION_NAME) {
      return node.getConstructorName();
    } else {
      return visitInvocation(node);
    }
  }

  @Override
  public Object visitSuperExpression(DartSuperExpression node) {
    return visitExpression(node);
  }

  @Override
  public Object visitSwitchMember(DartSwitchMember node) {
    if (property == PropertyDescriptorHelper.DART_SWITCH_MEMBER_LABEL) {
      return node.getLabel();
    } else if (property == PropertyDescriptorHelper.DART_SWITCH_MEMBER_STATEMENTS) {
      return node.getStatements();
    } else {
      return visitNode(node);
    }
  }

  @Override
  public Object visitSwitchStatement(DartSwitchStatement node) {
    if (property == PropertyDescriptorHelper.DART_SWITCH_STATEMENT_EXPRESSION) {
      return node.getExpression();
    } else if (property == PropertyDescriptorHelper.DART_SWITCH_STATEMENT_MEMBERS) {
      return node.getMembers();
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public Object visitSyntheticErrorExpression(DartSyntheticErrorExpression node) {
    if (property == PropertyDescriptorHelper.DART_SYNTHETIC_ERROR_EXPRESSION_TOKEN_STRING) {
      return node.getTokenString();
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public Object visitSyntheticErrorStatement(DartSyntheticErrorStatement node) {
    if (property == PropertyDescriptorHelper.DART_SYNTHETIC_ERROR_STATEMENT_TOKEN_STRING) {
      return node.getTokenString();
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public Object visitThisExpression(DartThisExpression node) {
    return visitExpression(node);
  }

  @Override
  public Object visitThrowStatement(DartThrowStatement node) {
    if (property == PropertyDescriptorHelper.DART_THROW_STATEMENT_EXCEPTION) {
      return node.getException();
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public Object visitTryStatement(DartTryStatement node) {
    if (property == PropertyDescriptorHelper.DART_TRY_STATEMENT_CATCH_BLOCKS) {
      return node.getCatchBlocks();
    } else if (property == PropertyDescriptorHelper.DART_TRY_STATEMENT_FINALY_BLOCK) {
      return node.getFinallyBlock();
    } else if (property == PropertyDescriptorHelper.DART_TRY_STATEMENT_TRY_BLOCK) {
      return node.getTryBlock();
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public Object visitTypeExpression(DartTypeExpression node) {
    if (property == PropertyDescriptorHelper.DART_TYPE_EXPRESSION_TYPE) {
      return node.getType();
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public Object visitTypeNode(DartTypeNode node) {
    if (property == PropertyDescriptorHelper.DART_TYPE_TYPE_ARGUMENTS) {
      return node.getTypeArguments();
    } else if (property == PropertyDescriptorHelper.DART_TYPE_IDENTIFIER) {
      return node.getIdentifier();
    } else {
      return visitNode(node);
    }
  }

  @Override
  public Object visitTypeParameter(DartTypeParameter node) {
    if (property == PropertyDescriptorHelper.DART_TYPE_PARAMETER_BOUND) {
      return node.getBound();
    } else if (property == PropertyDescriptorHelper.DART_TYPE_PARAMETER_NAME) {
      return node.getName();
    } else {
      return visitNode(node);
    }
  }

  @Override
  public Object visitUnaryExpression(DartUnaryExpression node) {
    if (property == PropertyDescriptorHelper.DART_UNARY_EXPRESSION_IS_PREFIX) {
      return Boolean.valueOf(node.isPrefix());
    } else if (property == PropertyDescriptorHelper.DART_UNARY_EXPRESSION_OPERAND) {
      return node.getArg();
    } else if (property == PropertyDescriptorHelper.DART_UNARY_EXPRESSION_OPERATOR) {
      return node.getOperator();
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public Object visitUnit(DartUnit node) {
    if (property == PropertyDescriptorHelper.DART_UNIT_COMMENTS) {
      return node.getComments();
    } else if (property == PropertyDescriptorHelper.DART_UNIT_MEMBERS) {
      return node.getTopLevelNodes();
    } else {
      return visitNode(node);
    }
  }

  @Override
  public Object visitUnqualifiedInvocation(DartUnqualifiedInvocation node) {
    if (property == PropertyDescriptorHelper.DART_METHOD_INVOCATION_TARGET) {
      return node.getTarget();
    } else {
      return visitInvocation(node);
    }
  }

  @Override
  public Object visitVariable(DartVariable node) {
    if (property == PropertyDescriptorHelper.DART_VARIABLE_NAME) {
      return node.getName();
    } else if (property == PropertyDescriptorHelper.DART_VARIABLE_VALUE) {
      return node.getValue();
    } else {
      return visitNode(node);
    }
  }

  @Override
  public Object visitVariableStatement(DartVariableStatement node) {
    if (property == PropertyDescriptorHelper.DART_VARIABLE_STATEMENT_TYPE) {
      return node.getType();
    } else if (property == PropertyDescriptorHelper.DART_VARIABLE_STATEMENT_VARIABLES) {
      return node.getVariables();
    } else {
      return visitStatement(node);
    }
  }

  @Override
  public Object visitWhileStatement(DartWhileStatement node) {
    if (property == PropertyDescriptorHelper.DART_WHILE_STATEMENT_BODY) {
      return node.getBody();
    } else if (property == PropertyDescriptorHelper.DART_WHILE_STATEMENT_CONDITION) {
      return node.getCondition();
    } else {
      return visitStatement(node);
    }
  }
}
