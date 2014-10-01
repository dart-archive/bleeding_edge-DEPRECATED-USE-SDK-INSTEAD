/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.constant;

import com.google.dart.engine.ast.AdjacentStrings;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.BooleanLiteral;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.DoubleLiteral;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.InterpolationElement;
import com.google.dart.engine.ast.InterpolationExpression;
import com.google.dart.engine.ast.InterpolationString;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.MapLiteral;
import com.google.dart.engine.ast.MapLiteralEntry;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.NullLiteral;
import com.google.dart.engine.ast.ParenthesizedExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.StringInterpolation;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.SymbolLiteral;
import com.google.dart.engine.ast.visitor.UnifyingAstVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.internal.object.BoolState;
import com.google.dart.engine.internal.object.DartObjectComputer;
import com.google.dart.engine.internal.object.DartObjectImpl;
import com.google.dart.engine.internal.object.DoubleState;
import com.google.dart.engine.internal.object.FunctionState;
import com.google.dart.engine.internal.object.GenericState;
import com.google.dart.engine.internal.object.IntState;
import com.google.dart.engine.internal.object.ListState;
import com.google.dart.engine.internal.object.MapState;
import com.google.dart.engine.internal.object.NullState;
import com.google.dart.engine.internal.object.StringState;
import com.google.dart.engine.internal.object.SymbolState;
import com.google.dart.engine.internal.object.TypeState;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.type.InterfaceType;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Instances of the class {@code ConstantVisitor} evaluate constant expressions to produce their
 * compile-time value. According to the Dart Language Specification: <blockquote> A constant
 * expression is one of the following:
 * <ul>
 * <li>A literal number.</li>
 * <li>A literal boolean.</li>
 * <li>A literal string where any interpolated expression is a compile-time constant that evaluates
 * to a numeric, string or boolean value or to <b>null</b>.</li>
 * <li>A literal symbol.</li>
 * <li><b>null</b>.</li>
 * <li>A qualified reference to a static constant variable.</li>
 * <li>An identifier expression that denotes a constant variable, class or type alias.</li>
 * <li>A constant constructor invocation.</li>
 * <li>A constant list literal.</li>
 * <li>A constant map literal.</li>
 * <li>A simple or qualified identifier denoting a top-level function or a static method.</li>
 * <li>A parenthesized expression <i>(e)</i> where <i>e</i> is a constant expression.</li>
 * <li>An expression of the form <i>identical(e<sub>1</sub>, e<sub>2</sub>)</i> where
 * <i>e<sub>1</sub></i> and <i>e<sub>2</sub></i> are constant expressions and <i>identical()</i> is
 * statically bound to the predefined dart function <i>identical()</i> discussed above.</li>
 * <li>An expression of one of the forms <i>e<sub>1</sub> == e<sub>2</sub></i> or <i>e<sub>1</sub>
 * != e<sub>2</sub></i> where <i>e<sub>1</sub></i> and <i>e<sub>2</sub></i> are constant expressions
 * that evaluate to a numeric, string or boolean value.</li>
 * <li>An expression of one of the forms <i>!e</i>, <i>e<sub>1</sub> &amp;&amp; e<sub>2</sub></i> or
 * <i>e<sub>1</sub> || e<sub>2</sub></i>, where <i>e</i>, <i>e1</sub></i> and <i>e2</sub></i> are
 * constant expressions that evaluate to a boolean value.</li>
 * <li>An expression of one of the forms <i>~e</i>, <i>e<sub>1</sub> ^ e<sub>2</sub></i>,
 * <i>e<sub>1</sub> &amp; e<sub>2</sub></i>, <i>e<sub>1</sub> | e<sub>2</sub></i>, <i>e<sub>1</sub>
 * &gt;&gt; e<sub>2</sub></i> or <i>e<sub>1</sub> &lt;&lt; e<sub>2</sub></i>, where <i>e</i>,
 * <i>e<sub>1</sub></i> and <i>e<sub>2</sub></i> are constant expressions that evaluate to an
 * integer value or to <b>null</b>.</li>
 * <li>An expression of one of the forms <i>-e</i>, <i>e<sub>1</sub> + e<sub>2</sub></i>,
 * <i>e<sub>1</sub> - e<sub>2</sub></i>, <i>e<sub>1</sub> * e<sub>2</sub></i>, <i>e<sub>1</sub> /
 * e<sub>2</sub></i>, <i>e<sub>1</sub> ~/ e<sub>2</sub></i>, <i>e<sub>1</sub> &gt;
 * e<sub>2</sub></i>, <i>e<sub>1</sub> &lt; e<sub>2</sub></i>, <i>e<sub>1</sub> &gt;=
 * e<sub>2</sub></i>, <i>e<sub>1</sub> &lt;= e<sub>2</sub></i> or <i>e<sub>1</sub> %
 * e<sub>2</sub></i>, where <i>e</i>, <i>e<sub>1</sub></i> and <i>e<sub>2</sub></i> are constant
 * expressions that evaluate to a numeric value or to <b>null</b>.</li>
 * <li>An expression of the form <i>e<sub>1</sub> ? e<sub>2</sub> : e<sub>3</sub></i> where
 * <i>e<sub>1</sub></i>, <i>e<sub>2</sub></i> and <i>e<sub>3</sub></i> are constant expressions, and
 * <i>e<sub>1</sub></i> evaluates to a boolean value.</li>
 * </ul>
 * </blockquote>
 */
public class ConstantVisitor extends UnifyingAstVisitor<DartObjectImpl> {
  /**
   * The type provider used to access the known types.
   */
  private TypeProvider typeProvider;

  /**
   * An shared object representing the value 'null'.
   */
  private DartObjectImpl nullObject;

  private final HashMap<String, DartObjectImpl> lexicalEnvironment;

  /**
   * Error reporter that we use to report errors accumulated while computing the constant.
   */
  private final ErrorReporter errorReporter;

  /**
   * Helper class used to compute constant values.
   */
  private final DartObjectComputer dartObjectComputer;

  /**
   * Initialize a newly created constant visitor.
   * 
   * @param typeProvider the type provider used to access known types
   * @param lexicalEnvironment values which should override simpleIdentifiers, or null if no
   *          overriding is necessary.
   */
  public ConstantVisitor(TypeProvider typeProvider, ErrorReporter errorReporter) {
    this.typeProvider = typeProvider;
    this.lexicalEnvironment = null;
    this.errorReporter = errorReporter;
    this.dartObjectComputer = new DartObjectComputer(errorReporter, typeProvider);
  }

  /**
   * Initialize a newly created constant visitor.
   * 
   * @param typeProvider the type provider used to access known types
   * @param lexicalEnvironment values which should override simpleIdentifiers, or null if no
   *          overriding is necessary.
   */
  public ConstantVisitor(TypeProvider typeProvider,
      HashMap<String, DartObjectImpl> lexicalEnvironment, ErrorReporter errorReporter) {
    this.typeProvider = typeProvider;
    this.lexicalEnvironment = lexicalEnvironment;
    this.errorReporter = errorReporter;
    this.dartObjectComputer = new DartObjectComputer(errorReporter, typeProvider);
  }

  @Override
  public DartObjectImpl visitAdjacentStrings(AdjacentStrings node) {
    DartObjectImpl result = null;
    for (StringLiteral string : node.getStrings()) {
      if (result == null) {
        result = string.accept(this);
      } else {
        result = dartObjectComputer.concatenate(node, result, string.accept(this));
      }
    }
    return result;
  }

  @Override
  public DartObjectImpl visitBinaryExpression(BinaryExpression node) {
    DartObjectImpl leftResult = node.getLeftOperand().accept(this);
    DartObjectImpl rightResult = node.getRightOperand().accept(this);
    TokenType operatorType = node.getOperator().getType();
    // 'null' is almost never good operand
    if (operatorType != TokenType.BANG_EQ && operatorType != TokenType.EQ_EQ) {
      if (leftResult != null && leftResult.isNull() || rightResult != null && rightResult.isNull()) {
        error(node, CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
        return null;
      }
    }
    // evaluate operator
    switch (operatorType) {
      case AMPERSAND:
        return dartObjectComputer.bitAnd(node, leftResult, rightResult);
      case AMPERSAND_AMPERSAND:
        return dartObjectComputer.logicalAnd(node, leftResult, rightResult);
      case BANG_EQ:
        return dartObjectComputer.notEqual(node, leftResult, rightResult);
      case BAR:
        return dartObjectComputer.bitOr(node, leftResult, rightResult);
      case BAR_BAR:
        return dartObjectComputer.logicalOr(node, leftResult, rightResult);
      case CARET:
        return dartObjectComputer.bitXor(node, leftResult, rightResult);
      case EQ_EQ:
        return dartObjectComputer.equalEqual(node, leftResult, rightResult);
      case GT:
        return dartObjectComputer.greaterThan(node, leftResult, rightResult);
      case GT_EQ:
        return dartObjectComputer.greaterThanOrEqual(node, leftResult, rightResult);
      case GT_GT:
        return dartObjectComputer.shiftRight(node, leftResult, rightResult);
      case LT:
        return dartObjectComputer.lessThan(node, leftResult, rightResult);
      case LT_EQ:
        return dartObjectComputer.lessThanOrEqual(node, leftResult, rightResult);
      case LT_LT:
        return dartObjectComputer.shiftLeft(node, leftResult, rightResult);
      case MINUS:
        return dartObjectComputer.minus(node, leftResult, rightResult);
      case PERCENT:
        return dartObjectComputer.remainder(node, leftResult, rightResult);
      case PLUS:
        return dartObjectComputer.add(node, leftResult, rightResult);
      case STAR:
        return dartObjectComputer.times(node, leftResult, rightResult);
      case SLASH:
        return dartObjectComputer.divide(node, leftResult, rightResult);
      case TILDE_SLASH:
        return dartObjectComputer.integerDivide(node, leftResult, rightResult);
      default:
        // TODO(brianwilkerson) Figure out which error to report.
        error(node, null);
        return null;
    }
  }

  @Override
  public DartObjectImpl visitBooleanLiteral(BooleanLiteral node) {
    return new DartObjectImpl(typeProvider.getBoolType(), BoolState.from(node.getValue()));
  }

  @Override
  public DartObjectImpl visitConditionalExpression(ConditionalExpression node) {
    Expression condition = node.getCondition();
    DartObjectImpl conditionResult = condition.accept(this);
    DartObjectImpl thenResult = node.getThenExpression().accept(this);
    DartObjectImpl elseResult = node.getElseExpression().accept(this);
    if (conditionResult == null) {
      return conditionResult;
    } else if (!conditionResult.isBool()) {
      errorReporter.reportErrorForNode(CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL, condition);
      return null;
    } else if (thenResult == null) {
      return thenResult;
    } else if (elseResult == null) {
      return elseResult;
    }
    conditionResult = dartObjectComputer.applyBooleanConversion(condition, conditionResult);
    if (conditionResult == null) {
      return conditionResult;
    }
    if (conditionResult.isTrue()) {
      return thenResult;
    } else if (conditionResult.isFalse()) {
      return elseResult;
    }
    InterfaceType thenType = thenResult.getType();
    InterfaceType elseType = elseResult.getType();
    return validWithUnknownValue((InterfaceType) thenType.getLeastUpperBound(elseType));
  }

  @Override
  public DartObjectImpl visitDoubleLiteral(DoubleLiteral node) {
    return new DartObjectImpl(typeProvider.getDoubleType(), new DoubleState(node.getValue()));
  }

  @Override
  public DartObjectImpl visitInstanceCreationExpression(InstanceCreationExpression node) {
    if (!node.isConst()) {
      // TODO(brianwilkerson) Figure out which error to report.
      error(node, null);
      return null;
    }
    beforeGetEvaluationResult(node);
    EvaluationResultImpl result = node.getEvaluationResult();
    if (result != null) {
      return result.getValue();
    }
    // TODO(brianwilkerson) Figure out which error to report.
    error(node, null);
    return null;
  }

  @Override
  public DartObjectImpl visitIntegerLiteral(IntegerLiteral node) {
    return new DartObjectImpl(typeProvider.getIntType(), new IntState(node.getValue()));
  }

  @Override
  public DartObjectImpl visitInterpolationExpression(InterpolationExpression node) {
    DartObjectImpl result = node.getExpression().accept(this);
    if (result != null && !result.isBoolNumStringOrNull()) {
      error(node, CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL_NUM_STRING);
      return null;
    }
    return dartObjectComputer.performToString(node, result);
  }

  @Override
  public DartObjectImpl visitInterpolationString(InterpolationString node) {
    return new DartObjectImpl(typeProvider.getStringType(), new StringState(node.getValue()));
  }

  @Override
  public DartObjectImpl visitListLiteral(ListLiteral node) {
    if (node.getConstKeyword() == null) {
      errorReporter.reportErrorForNode(CompileTimeErrorCode.MISSING_CONST_IN_LIST_LITERAL, node);
      return null;
    }
    boolean errorOccurred = false;
    ArrayList<DartObjectImpl> elements = new ArrayList<DartObjectImpl>();
    for (Expression element : node.getElements()) {
      DartObjectImpl elementResult = element.accept(this);
      if (elementResult == null) {
        errorOccurred = true;
      } else {
        elements.add(elementResult);
      }
    }
    if (errorOccurred) {
      return null;
    }
    return new DartObjectImpl(typeProvider.getListType(), new ListState(
        elements.toArray(new DartObjectImpl[elements.size()])));
  }

  @Override
  public DartObjectImpl visitMapLiteral(MapLiteral node) {
    if (node.getConstKeyword() == null) {
      errorReporter.reportErrorForNode(CompileTimeErrorCode.MISSING_CONST_IN_MAP_LITERAL, node);
      return null;
    }
    boolean errorOccurred = false;
    HashMap<DartObjectImpl, DartObjectImpl> map = new HashMap<DartObjectImpl, DartObjectImpl>();
    for (MapLiteralEntry entry : node.getEntries()) {
      DartObjectImpl keyResult = entry.getKey().accept(this);
      DartObjectImpl valueResult = entry.getValue().accept(this);
      if (keyResult == null || valueResult == null) {
        errorOccurred = true;
      } else {
        map.put(keyResult, valueResult);
      }
    }
    if (errorOccurred) {
      return null;
    }
    return new DartObjectImpl(typeProvider.getMapType(), new MapState(map));
  }

  @Override
  public DartObjectImpl visitMethodInvocation(MethodInvocation node) {
    Element element = node.getMethodName().getStaticElement();
    if (element instanceof FunctionElement) {
      FunctionElement function = (FunctionElement) element;
      if (function.getName().equals("identical")) {
        NodeList<Expression> arguments = node.getArgumentList().getArguments();
        if (arguments.size() == 2) {
          Element enclosingElement = function.getEnclosingElement();
          if (enclosingElement instanceof CompilationUnitElement) {
            LibraryElement library = ((CompilationUnitElement) enclosingElement).getLibrary();
            if (library.isDartCore()) {
              DartObjectImpl leftArgument = arguments.get(0).accept(this);
              DartObjectImpl rightArgument = arguments.get(1).accept(this);
              return dartObjectComputer.equalEqual(node, leftArgument, rightArgument);
            }
          }
        }
      }
    }
    // TODO(brianwilkerson) Figure out which error to report.
    error(node, null);
    return null;
  }

  @Override
  public DartObjectImpl visitNamedExpression(NamedExpression node) {
    return node.getExpression().accept(this);
  }

  @Override
  public DartObjectImpl visitNode(AstNode node) {
    // TODO(brianwilkerson) Figure out which error to report.
    error(node, null);
    return null;
  }

  @Override
  public DartObjectImpl visitNullLiteral(NullLiteral node) {
    return getNull();
  }

  @Override
  public DartObjectImpl visitParenthesizedExpression(ParenthesizedExpression node) {
    return node.getExpression().accept(this);
  }

  @Override
  public DartObjectImpl visitPrefixedIdentifier(PrefixedIdentifier node) {
    // TODO(brianwilkerson) Uncomment the lines below when the new constant support can be added.
//    Element element = node.getStaticElement();
//    if (isStringLength(element)) {
//      EvaluationResultImpl target = node.getPrefix().accept(this);
//      return target.stringLength(typeProvider, node);
//    }
    SimpleIdentifier prefixNode = node.getPrefix();
    Element prefixElement = prefixNode.getStaticElement();
    if (!(prefixElement instanceof PrefixElement)) {
      DartObjectImpl prefixResult = prefixNode.accept(this);
      if (prefixResult == null) {
        // The error has already been reported.
        return null;
      }
    }
    // validate prefixed identifier
    return getConstantValue(node, node.getStaticElement());
  }

  @Override
  public DartObjectImpl visitPrefixExpression(PrefixExpression node) {
    DartObjectImpl operand = node.getOperand().accept(this);
    if (operand != null && operand.isNull()) {
      error(node, CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
      return null;
    }
    switch (node.getOperator().getType()) {
      case BANG:
        return dartObjectComputer.logicalNot(node, operand);
      case TILDE:
        return dartObjectComputer.bitNot(node, operand);
      case MINUS:
        return dartObjectComputer.negated(node, operand);
      default:
        // TODO(brianwilkerson) Figure out which error to report.
        error(node, null);
        return null;
    }
  }

  @Override
  public DartObjectImpl visitPropertyAccess(PropertyAccess node) {
    Element element = node.getPropertyName().getStaticElement();
    // TODO(brianwilkerson) Uncomment the lines below when the new constant support can be added.
//    if (isStringLength(element)) {
//      EvaluationResultImpl target = node.getRealTarget().accept(this);
//      return target.stringLength(typeProvider, node);
//    }
    return getConstantValue(node, element);
  }

  @Override
  public DartObjectImpl visitSimpleIdentifier(SimpleIdentifier node) {
    if (lexicalEnvironment != null && lexicalEnvironment.containsKey(node.getName())) {
      return lexicalEnvironment.get(node.getName());
    }
    return getConstantValue(node, node.getStaticElement());
  }

  @Override
  public DartObjectImpl visitSimpleStringLiteral(SimpleStringLiteral node) {
    return new DartObjectImpl(typeProvider.getStringType(), new StringState(node.getValue()));
  }

  @Override
  public DartObjectImpl visitStringInterpolation(StringInterpolation node) {
    DartObjectImpl result = null;
    boolean first = true;
    for (InterpolationElement element : node.getElements()) {
      if (first) {
        result = element.accept(this);
        first = false;
      } else {
        result = dartObjectComputer.concatenate(node, result, element.accept(this));
      }
    }
    return result;
  }

  @Override
  public DartObjectImpl visitSymbolLiteral(SymbolLiteral node) {
    StringBuilder builder = new StringBuilder();
    Token[] components = node.getComponents();
    for (int i = 0; i < components.length; i++) {
      if (i > 0) {
        builder.append('.');
      }
      builder.append(components[i].getLexeme());
    }
    return new DartObjectImpl(typeProvider.getSymbolType(), new SymbolState(builder.toString()));
  }

  /**
   * This method is called just before retrieving an evaluation result from an AST node. Unit tests
   * will override it to introduce additional error checking.
   */
  protected void beforeGetEvaluationResult(AstNode node) {
  }

  /**
   * Return an object representing the value 'null'.
   * 
   * @return an object representing the value 'null'
   */
  DartObjectImpl getNull() {
    if (nullObject == null) {
      nullObject = new DartObjectImpl(typeProvider.getNullType(), NullState.NULL_STATE);
    }
    return nullObject;
  }

  DartObjectImpl validWithUnknownValue(InterfaceType type) {
    if (type.getElement().getLibrary().isDartCore()) {
      String typeName = type.getName();
      if (typeName.equals("bool")) {
        return new DartObjectImpl(type, BoolState.UNKNOWN_VALUE);
      } else if (typeName.equals("double")) {
        return new DartObjectImpl(type, DoubleState.UNKNOWN_VALUE);
      } else if (typeName.equals("int")) {
        return new DartObjectImpl(type, IntState.UNKNOWN_VALUE);
      } else if (typeName.equals("String")) {
        return new DartObjectImpl(type, StringState.UNKNOWN_VALUE);
      }
    }
    return new DartObjectImpl(type, GenericState.UNKNOWN_VALUE);
  }

  /**
   * Return the value of the given expression, or a representation of 'null' if the expression
   * cannot be evaluated.
   * 
   * @param expression the expression whose value is to be returned
   * @return the value of the given expression
   */
  DartObjectImpl valueOf(Expression expression) {
    DartObjectImpl expressionValue = expression.accept(this);
    if (expressionValue != null) {
      return expressionValue;
    }
    return getNull();
  }

  /**
   * Create an error associated with the given node.
   * 
   * @param node the AST node associated with the error
   * @param code the error code indicating the nature of the error
   */
  private void error(AstNode node, ErrorCode code) {
    errorReporter.reportErrorForNode(
        code == null ? CompileTimeErrorCode.INVALID_CONSTANT : code,
        node);
  }

  /**
   * Return the constant value of the static constant represented by the given element.
   * 
   * @param node the node to be used if an error needs to be reported
   * @param element the element whose value is to be returned
   * @return the constant value of the static constant
   */
  private DartObjectImpl getConstantValue(AstNode node, Element element) {
    if (element instanceof PropertyAccessorElement) {
      element = ((PropertyAccessorElement) element).getVariable();
    }
    if (element instanceof VariableElementImpl) {
      VariableElementImpl variableElementImpl = (VariableElementImpl) element;
      beforeGetEvaluationResult(node);
      EvaluationResultImpl value = variableElementImpl.getEvaluationResult();
      if (variableElementImpl.isConst() && value != null) {
        return value.getValue();
      }
    } else if (element instanceof ExecutableElement) {
      ExecutableElement function = (ExecutableElement) element;
      if (function.isStatic()) {
        return new DartObjectImpl(typeProvider.getFunctionType(), new FunctionState(function));
      }
    } else if (element instanceof ClassElement || element instanceof FunctionTypeAliasElement) {
      return new DartObjectImpl(typeProvider.getTypeType(), new TypeState(element));
    }
    // TODO(brianwilkerson) Figure out which error to report.
    error(node, null);
    return null;
  }

  /**
   * Return {@code true} if the given element represents the 'length' getter in class 'String'.
   * 
   * @param element the element being tested.
   * @return
   */
  private boolean isStringLength(Element element) {
    if (!(element instanceof PropertyAccessorElement)) {
      return false;
    }
    PropertyAccessorElement accessor = (PropertyAccessorElement) element;
    if (!accessor.isGetter() || !accessor.getName().equals("length")) {
      return false;
    }
    Element parent = accessor.getEnclosingElement();
    return parent.equals(typeProvider.getStringType().getElement());
  }
}
