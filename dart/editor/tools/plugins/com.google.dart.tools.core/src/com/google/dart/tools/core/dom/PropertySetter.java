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
import com.google.dart.tools.core.DartCore;

/**
 * Instances of the class <code>PropertySetter</code> implement a visitor that sets the value of the specified
 * property for the node being visited.
 */
public class PropertySetter extends PropertyVisitor {
  /**
   * The value to which the property is to be set.
   */
  private Object propertyValue;

  /**
   * Initialize a newly created visitor to set the value of the given property to the given value.
   * 
   * @param property the specification of the property to be set
   * @param propertyValue the value to which the property is to be set
   */
  public PropertySetter(StructuralPropertyDescriptor property, Object propertyValue) {
    super(property);
    this.propertyValue = propertyValue;
    checkValueType();
  }

  @Override
  public Object visitArrayAccess(DartArrayAccess node) {
    if (property == PropertyDescriptorHelper.DART_ARRAY_ACCESS_KEY) {
      // node.setKey((DartExpression) propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_ARRAY_ACCESS_TARGET) {
      // node.setTarget((DartExpression) propertyValue);
    } else {
      visitExpression(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitArrayLiteral(DartArrayLiteral node) {
    if (property == PropertyDescriptorHelper.DART_ARRAY_LITERAL_EXPRESSIONS) {
      // node.setExpressions((DartExpression) propertyValue);
    } else {
      visitExpression(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitBinaryExpression(DartBinaryExpression node) {
    if (property == PropertyDescriptorHelper.DART_BINARY_EXPRESSION_LEFT_OPERAND) {
      // node.setArg1(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_BINARY_EXPRESSION_OPERATOR) {
      // node.setOperator(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_BINARY_EXPRESSION_RIGHT_OPERAND) {
      // node.setArg2(propertyValue);
    } else {
      visitExpression(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitBlock(DartBlock node) {
    if (property == PropertyDescriptorHelper.DART_BLOCK_STATEMENTS) {
      // node.setStatements(propertyValue);
    } else {
      visitStatement(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitBooleanLiteral(DartBooleanLiteral node) {
    if (property == PropertyDescriptorHelper.DART_BOOLEAN_LITERAL_VALUE) {
      // node.setValue(propertyValue);
    } else {
      visitLiteral(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitBreakStatement(DartBreakStatement node) {
    visitGotoStatement(node);
    return null;
  }

  @Override
  public Object visitCase(DartCase node) {
    if (property == PropertyDescriptorHelper.DART_CASE_EXPRESSION) {
      // node.setExpr(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_CASE_LABELS) {
      // node.setLabel(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_CASE_STATEMENTS) {
      // node.setStatements(propertyValue);
    } else {
      visitSwitchMember(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitCatchBlock(DartCatchBlock node) {
    if (property == PropertyDescriptorHelper.DART_CATCH_BLOCK_EXCEPTION) {
      // node.setException(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_CATCH_BLOCK_STACK_TRACE) {
      // node.setStackTrace(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_CATCH_BLOCK_BODY) {
      // node.setBlock(propertyValue);
    } else {
      visitStatement(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitClass(DartClass node) {
    if (property == PropertyDescriptorHelper.DART_CLASS_DEFAULT_CLASS) {
      // node.setDefaultClass(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_CLASS_INTERFACES) {
      // node.setInterfaces(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_CLASS_IS_INTERFACE) {
      // node.setIsInterface(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_CLASS_MEMBERS) {
      // node.setMethods(propertyValue);
      // node.setFields(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_CLASS_NAME) {
      // node.setName(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_CLASS_SUPERCLASS) {
      // node.setSuperclass(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_CLASS_TYPE_PARAMETERS) {
      // node.setTypeParameters(propertyValue);
    } else {
      visitNode(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitClassMember(DartClassMember<?> node) {
    if (property == PropertyDescriptorHelper.DART_CLASS_MEMBER_MODIFIERS) {
      // node.setModifiers(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_CLASS_MEMBER_NAME) {
      // node.setName(propertyValue);
    } else {
      visitNode(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitComment(DartComment node) {
    visitNode(node);
    return null;
  }

  @Override
  public Object visitConditional(DartConditional node) {
    if (property == PropertyDescriptorHelper.DART_CONDITIONAL_CONDITION) {
      // node.setCondition(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_CONDITIONAL_ELSE) {
      // node.setElseExpression(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_CONDITIONAL_THEN) {
      // node.setThenExpression(propertyValue);
    } else {
      visitExpression(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitContinueStatement(DartContinueStatement node) {
    visitGotoStatement(node);
    return null;
  }

  @Override
  public Object visitDefault(DartDefault node) {
    visitSwitchMember(node);
    return null;
  }

  @Override
  public Object visitDirective(DartDirective node) {
    return visitNode(node);
  }

  @Override
  public Object visitDoubleLiteral(DartDoubleLiteral node) {
    if (property == PropertyDescriptorHelper.DART_DOUBLE_LITERAL_VALUE) {
      // node.setValue(propertyValue);
    } else {
      visitLiteral(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitDoWhileStatement(DartDoWhileStatement node) {
    if (property == PropertyDescriptorHelper.DART_DO_WHILE_STATEMENT_BODY) {
      // node.setBody(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_DO_WHILE_STATEMENT_CONDITION) {
      // node.setCondition(propertyValue);
    } else {
      visitStatement(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitEmptyStatement(DartEmptyStatement node) {
    visitStatement(node);
    return null;
  }

  @Override
  public Object visitExpression(DartExpression node) {
    visitNode(node);
    return null;
  }

  @Override
  public Object visitExprStmt(DartExprStmt node) {
    if (property == PropertyDescriptorHelper.DART_EXPRESSION_STATEMENT_EXPRESSION) {
      // node.setExpression(propertyValue);
    } else {
      visitStatement(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitField(DartField node) {
    if (property == PropertyDescriptorHelper.DART_FIELD_VALUE) {
      // node.setValue(propertyValue);
    } else {
      visitClassMember(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitFieldDefinition(DartFieldDefinition node) {
    if (property == PropertyDescriptorHelper.DART_FIELD_DEFINITION_TYPE) {
      // node.setType(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_FIELD_DEFINITION_FIELDS) {
      // node.addField(...)
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitForInStatement(DartForInStatement node) {
    if (property == PropertyDescriptorHelper.DART_FOR_IN_STATEMENT_BODY) {
      //node.setBody((DartStatement) propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_FOR_IN_STATEMENT_ITERABLE) {
      //node.setIterable((DartVariableStatement) propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_FOR_IN_STATEMENT_VARIABLE) {
      //node.setVariable((DartExpression) propertyValue);
    } else {
      visitStatement(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitForStatement(DartForStatement node) {
    if (property == PropertyDescriptorHelper.DART_FOR_STATEMENT_BODY) {
      // node.setBody(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_FOR_STATEMENT_CONDITION) {
      // node.setCondition(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_FOR_STATEMENT_INCREMENT) {
      // node.setIncrement(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_FOR_STATEMENT_INIT) {
      // node.setInit(propertyValue);
    } else {
      visitStatement(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitFunction(DartFunction node) {
    if (property == PropertyDescriptorHelper.DART_FUNCTION_BODY) {
      // node.setBody(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_FUNCTION_PARAMETERS) {
      // node.setParams(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_FUNCTION_RETURN_TYPE) {
      // node.setReturnType(propertyValue);
    } else {
      visitNode(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitFunctionExpression(DartFunctionExpression node) {
    if (property == PropertyDescriptorHelper.DART_FUNCTION_EXPRESSION_FUNCTION) {
      // node.setFunction(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_FUNCTION_EXPRESSION_IS_STATEMENT) {
      // node.setIsStatement(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_FUNCTION_EXPRESSION_NAME) {
      // node.setName(propertyValue);
    } else {
      visitExpression(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitFunctionObjectInvocation(DartFunctionObjectInvocation node) {
    if (property == PropertyDescriptorHelper.DART_FUNCTION_OBJECT_INVOCATION_TARGET) {
      // node.setTarget(propertyValue);
    } else {
      visitInvocation(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitFunctionTypeAlias(DartFunctionTypeAlias node) {
    if (property == PropertyDescriptorHelper.DART_FUNCTION_TYPE_ALIAS_NAME) {
      // return node.setName(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_FUNCTION_TYPE_ALIAS_RETURN_TYPE) {
      // return node.setReturnType(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_FUNCTION_TYPE_ALIAS_PARAMETERS) {
      // return node.setParameters(propertyValue);
    } else {
      return visitNode(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitGotoStatement(DartGotoStatement node) {
    if (property == PropertyDescriptorHelper.DART_GOTO_STATEMENT_LABEL) {
      // node.setLabel(propertyValue);
    } else {
      visitStatement(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitIdentifier(DartIdentifier node) {
    if (property == PropertyDescriptorHelper.DART_IDENTIFIER_TARGET_NAME) {
      // node.setTargetName(propertyValue);
    } else {
      visitExpression(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitIfStatement(DartIfStatement node) {
    if (property == PropertyDescriptorHelper.DART_IF_STATEMENT_CONDITION) {
      // node.setCondition(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_IF_STATEMENT_ELSE) {
      // node.setElseStatement(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_IF_STATEMENT_THEN) {
      // node.setThenStatement(propertyValue);
    } else {
      visitStatement(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitImportDirective(DartImportDirective node) {
    if (property == PropertyDescriptorHelper.DART_IMPORT_DIRECTIVE_URI) {
      // node.setLibraryUri(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_IMPORT_DIRECTIVE_PREFIX) {
      // node.setPrefix(propertyValue);
    } else {
      visitDirective(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitInitializer(DartInitializer node) {
    if (property == PropertyDescriptorHelper.DART_INITIALIZER_NAME) {
      // node.setName(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_INITIALIZER_VALUE) {
      // node.setValue(propertyValue);
    } else {
      visitNode(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitIntegerLiteral(DartIntegerLiteral node) {
    if (property == PropertyDescriptorHelper.DART_INTEGER_LITERAL_VALUE) {
      // node.setValue(propertyValue);
    } else {
      visitLiteral(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitInvocation(DartInvocation node) {
    if (property == PropertyDescriptorHelper.DART_INVOCATION_ARGS) {
      // node.setArgs(propertyValue);
    } else {
      visitExpression(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitLabel(DartLabel node) {
    if (property == PropertyDescriptorHelper.DART_LABELED_STATEMENT_LABEL) {
      // node.setLabel(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_LABELED_STATEMENT_STATEMENT) {
      // node.setStatement(propertyValue);
    } else {
      visitStatement(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitLibraryDirective(DartLibraryDirective node) {
    if (property == PropertyDescriptorHelper.DART_LIBRARY_DIRECTIVE_NAME) {
      // node.setName(propertyValue);
    } else {
      visitDirective(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitLiteral(DartLiteral node) {
    visitExpression(node);
    return null;
  }

  @Override
  public Object visitMapLiteral(DartMapLiteral node) {
    if (property == PropertyDescriptorHelper.DART_MAP_LITERAL_ENTRIES) {
      // node.setEntries(propertyValue);
    } else {
      visitExpression(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitMapLiteralEntry(DartMapLiteralEntry node) {
    if (property == PropertyDescriptorHelper.DART_MAP_LITERAL_ENTRY_KEY) {
      // node.setKeyNode(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_MAP_LITERAL_ENTRY_VALUE) {
      // node.setValue(propertyValue);
    } else {
      visitNode(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitMethodDefinition(DartMethodDefinition node) {
    if (property == PropertyDescriptorHelper.DART_METHOD_DEFINITION_FUNCTION) {
      // node.setFunction(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_METHOD_DEFINITION_INITIALIZERS) {
      // node.setInitializers(propertyValue);
    } else {
      visitClassMember(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitMethodInvocation(DartMethodInvocation node) {
    if (property == PropertyDescriptorHelper.DART_METHOD_INVOCATION_TARGET) {
      // node.setTarget(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_METHOD_INVOCATION_FUNCTION_NAME) {
      // node.setFunctionName(propertyValue);
    } else {
      visitInvocation(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitNamedExpression(DartNamedExpression node) {
    if (property == PropertyDescriptorHelper.DART_NAMED_EXPRESSION_NAME) {
      // node.setName((DartIdentifier) propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_NAMED_EXPRESSION_EXPRESSION) {
      // node.setExpression((DartExpression) propertyValue);
    } else {
      visitExpression(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitNativeBlock(DartNativeBlock node) {
    visitBlock(node);
    return null;
  }

  @Override
  public Object visitNativeDirective(DartNativeDirective node) {
    if (property == PropertyDescriptorHelper.DART_NATIVE_DIRECTIVE_URI) {
      // node.setNativeUri(propertyValue);
    } else {
      visitDirective(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitNewExpression(DartNewExpression node) {
    if (property == PropertyDescriptorHelper.DART_NEW_EXPRESSION_CONSTRUCTOR_NAME) {
      // node.setConstructor(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_NEW_EXPRESSION_ARGUMENTS) {
      // node.setArgs(propertyValue);
    } else {
      visitExpression(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitNode(DartNode node) {
    noSuchProperty(node.getClass().getName());
    return null;
  }

  @Override
  public Object visitNullLiteral(DartNullLiteral node) {
    visitLiteral(node);
    return null;
  }

  @Override
  public Object visitParameter(DartParameter node) {
    if (property == PropertyDescriptorHelper.DART_PARAMETER_DEFAULT_EXPRESSION) {
      // node.setDefaultExpr(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_PARAMETER_FUNCTION_PARAMETERS) {
      // node.setFunctionParameters(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_PARAMETER_IS_CONST) {
      // node.setIsConst(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_PARAMETER_NAME) {
      // node.setName(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_PARAMETER_TYPE_NAME) {
      // node.setType(propertyValue);
    } else {
      visitNode(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitParameterizedTypeNode(DartParameterizedTypeNode node) {
    if (property == PropertyDescriptorHelper.DART_PARAMETERIZED_NODE_EXPRESSION) {
      // node.setExpression(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_PARAMETERIZED_NODE_TYPE_PARAMETERS) {
      // node.setTypeParameters(propertyValue);
    } else {
      visitExpression(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitParenthesizedExpression(DartParenthesizedExpression node) {
    if (property == PropertyDescriptorHelper.DART_PARENTHESIZED_EXPRESSION_EXPRESSION) {
      // node.setExpression(propertyValue);
    } else {
      visitExpression(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitPropertyAccess(DartPropertyAccess node) {
    if (property == PropertyDescriptorHelper.DART_PROPERTY_ACCESS_QUALIFIER) {
      // node.setQualifier(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_PROPERTY_ACCESS_NAME) {
      // node.setName(propertyValue);
    } else {
      visitExpression(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitRedirectConstructorInvocation(DartRedirectConstructorInvocation node) {
    if (property == PropertyDescriptorHelper.DART_REDIRECT_CONSTRUCTOR_INVOCATION_NAME) {
      // node.setName((DartIdentifier) propertyValue);
    } else {
      visitInvocation(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitReturnStatement(DartReturnStatement node) {
    if (property == PropertyDescriptorHelper.DART_RETURN_STATEMENT_VALUE) {
      // node.setValue(propertyValue);
    } else {
      visitStatement(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitSourceDirective(DartSourceDirective node) {
    if (property == PropertyDescriptorHelper.DART_SOURCE_DIRECTIVE_URI) {
      // node.setSourceUri(propertyValue);
    } else {
      visitDirective(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitStatement(DartStatement node) {
    visitNode(node);
    return null;
  }

  @Override
  public Object visitStringInterpolation(DartStringInterpolation node) {
    if (property == PropertyDescriptorHelper.DART_STRING_INTERPOLATION_EXPRESSIONS) {
      // node.setExpressions(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_STRING_INTERPOLATION_STRINGS) {
      // node.setStrings(propertyValue);
    } else {
      visitExpression(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitStringLiteral(DartStringLiteral node) {
    if (property == PropertyDescriptorHelper.DART_STRING_LITERAL_VALUE) {
      // node.setValue(propertyValue);
    } else {
      visitLiteral(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitSuperConstructorInvocation(DartSuperConstructorInvocation node) {
    if (property == PropertyDescriptorHelper.DART_SUPER_CONSTRUCTOR_INVOCATION_NAME) {
      // node.setConstructorName(propertyValue);
    } else {
      visitInvocation(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitSuperExpression(DartSuperExpression node) {
    visitExpression(node);
    return null;
  }

  @Override
  public Object visitSwitchMember(DartSwitchMember node) {
    if (property == PropertyDescriptorHelper.DART_SWITCH_MEMBER_LABELS) {
      // node.setLabel(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_SWITCH_MEMBER_STATEMENTS) {
      // node.setStatements(propertyValue);
    } else {
      visitNode(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitSwitchStatement(DartSwitchStatement node) {
    if (property == PropertyDescriptorHelper.DART_SWITCH_STATEMENT_EXPRESSION) {
      // node.setExpression(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_SWITCH_STATEMENT_MEMBERS) {
      // node.setMembers(propertyValue);
    } else {
      visitStatement(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitSyntheticErrorExpression(DartSyntheticErrorExpression node) {
    if (property == PropertyDescriptorHelper.DART_SYNTHETIC_ERROR_EXPRESSION_TOKEN_STRING) {
      // node.setTargetName(propertyValue);
    } else {
      visitExpression(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitSyntheticErrorStatement(DartSyntheticErrorStatement node) {
    if (property == PropertyDescriptorHelper.DART_SYNTHETIC_ERROR_STATEMENT_TOKEN_STRING) {
      // node.setTokenString(propertyValue);
    } else {
      visitStatement(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitThisExpression(DartThisExpression node) {
    visitExpression(node);
    return null;
  }

  @Override
  public Object visitThrowStatement(DartThrowStatement node) {
    if (property == PropertyDescriptorHelper.DART_THROW_STATEMENT_EXCEPTION) {
      // node.setException(propertyValue);
    } else {
      visitStatement(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitTryStatement(DartTryStatement node) {
    if (property == PropertyDescriptorHelper.DART_TRY_STATEMENT_CATCH_BLOCKS) {
      // node.setCatchBlocks(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_TRY_STATEMENT_FINALY_BLOCK) {
      // node.setFinallyBlock(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_TRY_STATEMENT_TRY_BLOCK) {
      // node.setTryBlock(propertyValue);
    } else {
      visitStatement(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitTypeExpression(DartTypeExpression node) {
    if (property == PropertyDescriptorHelper.DART_TYPE_EXPRESSION_TYPE) {
      // node.setType(propertyValue);
    } else {
      visitExpression(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitTypeNode(DartTypeNode node) {
    if (property == PropertyDescriptorHelper.DART_TYPE_TYPE_ARGUMENTS) {
      // node.setTypeArguments(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_TYPE_IDENTIFIER) {
      // node.setIdentifier(propertyValue);
    } else {
      visitNode(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitTypeParameter(DartTypeParameter node) {
    if (property == PropertyDescriptorHelper.DART_TYPE_PARAMETER_BOUND) {
      // node.setBound(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_TYPE_PARAMETER_NAME) {
      // node.setName(propertyValue);
    } else {
      visitNode(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitUnaryExpression(DartUnaryExpression node) {
    if (property == PropertyDescriptorHelper.DART_UNARY_EXPRESSION_IS_PREFIX) {
      // node.setIsPrefix(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_UNARY_EXPRESSION_OPERAND) {
      // node.setArg(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_UNARY_EXPRESSION_OPERATOR) {
      // node.setOperator(propertyValue);
    } else {
      visitExpression(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitUnit(DartUnit node) {
    if (property == PropertyDescriptorHelper.DART_UNIT_COMMENTS) {
      // node.setComments(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_UNIT_MEMBERS) {
      // node.setTopLevelMembers(propertyValue);
    } else {
      visitNode(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitUnqualifiedInvocation(DartUnqualifiedInvocation node) {
    if (property == PropertyDescriptorHelper.DART_UNQUALIFIED_INVOCATION_TARGET) {
      // node.setTarget(propertyValue);
    } else {
      visitInvocation(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitVariable(DartVariable node) {
    if (property == PropertyDescriptorHelper.DART_VARIABLE_NAME) {
      // node.setName(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_VARIABLE_VALUE) {
      // node.setValue(propertyValue);
    } else {
      visitNode(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitVariableStatement(DartVariableStatement node) {
    if (property == PropertyDescriptorHelper.DART_VARIABLE_STATEMENT_TYPE) {
      // node.setType(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_VARIABLE_STATEMENT_VARIABLES) {
      // node.setVariables(propertyValue);
    } else {
      visitStatement(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public Object visitWhileStatement(DartWhileStatement node) {
    if (property == PropertyDescriptorHelper.DART_WHILE_STATEMENT_BODY) {
      // node.setBody(propertyValue);
    } else if (property == PropertyDescriptorHelper.DART_WHILE_STATEMENT_CONDITION) {
      // node.setCondition(propertyValue);
    } else {
      visitStatement(node);
    }
    DartCore.notYetImplemented();
    return null;
  }

  private void checkValueType() {
    Class<?> propertyClass;
    if (property instanceof ChildListPropertyDescriptor) {
      propertyClass = ((ChildListPropertyDescriptor) property).getElementType();
    } else if (property instanceof ChildPropertyDescriptor) {
      if (propertyValue == null) {
        return;
      }
      propertyClass = ((ChildPropertyDescriptor) property).getChildType();
    } else {
      propertyClass = ((SimplePropertyDescriptor) property).getValueType();
    }
    if (!propertyClass.isInstance(propertyValue)) {
      throw new RuntimeException(
          "Value of class " + propertyValue.getClass().getName()
              + " cannot be assigned to a property of type " + propertyClass.getName());
    }
  }
}
