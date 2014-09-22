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
import com.google.dart.engine.internal.object.BoolState;
import com.google.dart.engine.internal.object.DartObjectImpl;
import com.google.dart.engine.internal.object.DoubleState;
import com.google.dart.engine.internal.object.FunctionState;
import com.google.dart.engine.internal.object.GenericState;
import com.google.dart.engine.internal.object.InstanceState;
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
public class ConstantVisitor extends UnifyingAstVisitor<EvaluationResultImpl> {
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
   * Initialize a newly created constant visitor.
   * 
   * @param typeProvider the type provider used to access known types
   * @param lexicalEnvironment values which should override simpleIdentifiers, or null if no
   *          overriding is necessary.
   */
  public ConstantVisitor(TypeProvider typeProvider) {
    this.typeProvider = typeProvider;
    this.lexicalEnvironment = null;
  }

  /**
   * Initialize a newly created constant visitor.
   * 
   * @param typeProvider the type provider used to access known types
   * @param lexicalEnvironment values which should override simpleIdentifiers, or null if no
   *          overriding is necessary.
   */
  public ConstantVisitor(TypeProvider typeProvider,
      HashMap<String, DartObjectImpl> lexicalEnvironment) {
    this.typeProvider = typeProvider;
    this.lexicalEnvironment = lexicalEnvironment;
  }

  @Override
  public EvaluationResultImpl visitAdjacentStrings(AdjacentStrings node) {
    EvaluationResultImpl result = null;
    for (StringLiteral string : node.getStrings()) {
      if (result == null) {
        result = string.accept(this);
      } else {
        result = result.concatenate(typeProvider, node, string.accept(this));
      }
    }
    return result;
  }

  @Override
  public EvaluationResultImpl visitBinaryExpression(BinaryExpression node) {
    EvaluationResultImpl leftResult = node.getLeftOperand().accept(this);
    EvaluationResultImpl rightResult = node.getRightOperand().accept(this);
    TokenType operatorType = node.getOperator().getType();
    // 'null' is almost never good operand
    if (operatorType != TokenType.BANG_EQ && operatorType != TokenType.EQ_EQ) {
      if (leftResult instanceof ValidResult && ((ValidResult) leftResult).isNull()
          || rightResult instanceof ValidResult && ((ValidResult) rightResult).isNull()) {
        return error(node, CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
      }
    }
    // evaluate operator
    switch (operatorType) {
      case AMPERSAND:
        return leftResult.bitAnd(typeProvider, node, rightResult);
      case AMPERSAND_AMPERSAND:
        return leftResult.logicalAnd(typeProvider, node, rightResult);
      case BANG_EQ:
        return leftResult.notEqual(typeProvider, node, rightResult);
      case BAR:
        return leftResult.bitOr(typeProvider, node, rightResult);
      case BAR_BAR:
        return leftResult.logicalOr(typeProvider, node, rightResult);
      case CARET:
        return leftResult.bitXor(typeProvider, node, rightResult);
      case EQ_EQ:
        return leftResult.equalEqual(typeProvider, node, rightResult);
      case GT:
        return leftResult.greaterThan(typeProvider, node, rightResult);
      case GT_EQ:
        return leftResult.greaterThanOrEqual(typeProvider, node, rightResult);
      case GT_GT:
        return leftResult.shiftRight(typeProvider, node, rightResult);
      case LT:
        return leftResult.lessThan(typeProvider, node, rightResult);
      case LT_EQ:
        return leftResult.lessThanOrEqual(typeProvider, node, rightResult);
      case LT_LT:
        return leftResult.shiftLeft(typeProvider, node, rightResult);
      case MINUS:
        return leftResult.minus(typeProvider, node, rightResult);
      case PERCENT:
        return leftResult.remainder(typeProvider, node, rightResult);
      case PLUS:
        return leftResult.add(typeProvider, node, rightResult);
      case STAR:
        return leftResult.times(typeProvider, node, rightResult);
      case SLASH:
        return leftResult.divide(typeProvider, node, rightResult);
      case TILDE_SLASH:
        return leftResult.integerDivide(typeProvider, node, rightResult);
      default:
        // TODO(brianwilkerson) Figure out which error to report.
        return error(node, null);
    }
  }

  @Override
  public EvaluationResultImpl visitBooleanLiteral(BooleanLiteral node) {
    return valid(typeProvider.getBoolType(), BoolState.from(node.getValue()));
  }

  @Override
  public EvaluationResultImpl visitConditionalExpression(ConditionalExpression node) {
    Expression condition = node.getCondition();
    EvaluationResultImpl conditionResult = condition.accept(this);
    EvaluationResultImpl thenResult = node.getThenExpression().accept(this);
    EvaluationResultImpl elseResult = node.getElseExpression().accept(this);
    if (conditionResult instanceof ErrorResult) {
      return union(union((ErrorResult) conditionResult, thenResult), elseResult);
    } else if (!((ValidResult) conditionResult).isBool()) {
      return new ErrorResult(condition, CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL);
    } else if (thenResult instanceof ErrorResult) {
      return union((ErrorResult) thenResult, elseResult);
    } else if (elseResult instanceof ErrorResult) {
      return elseResult;
    }
    conditionResult = conditionResult.applyBooleanConversion(typeProvider, condition);
    if (conditionResult instanceof ErrorResult) {
      return conditionResult;
    }
    ValidResult validResult = (ValidResult) conditionResult;
    if (validResult.isTrue()) {
      return thenResult;
    } else if (validResult.isFalse()) {
      return elseResult;
    }
    InterfaceType thenType = ((ValidResult) thenResult).getValue().getType();
    InterfaceType elseType = ((ValidResult) elseResult).getValue().getType();
    return validWithUnknownValue((InterfaceType) thenType.getLeastUpperBound(elseType));
  }

  @Override
  public EvaluationResultImpl visitDoubleLiteral(DoubleLiteral node) {
    return valid(typeProvider.getDoubleType(), new DoubleState(node.getValue()));
  }

  @Override
  public EvaluationResultImpl visitInstanceCreationExpression(InstanceCreationExpression node) {
    if (!node.isConst()) {
      // TODO(brianwilkerson) Figure out which error to report.
      return error(node, null);
    }
    beforeGetEvaluationResult(node);
    EvaluationResultImpl result = node.getEvaluationResult();
    if (result != null) {
      return result;
    }
    // TODO(brianwilkerson) Figure out which error to report.
    return error(node, null);
  }

  @Override
  public EvaluationResultImpl visitIntegerLiteral(IntegerLiteral node) {
    return valid(typeProvider.getIntType(), new IntState(node.getValue()));
  }

  @Override
  public EvaluationResultImpl visitInterpolationExpression(InterpolationExpression node) {
    EvaluationResultImpl result = node.getExpression().accept(this);
    if (result instanceof ValidResult && !((ValidResult) result).isBoolNumStringOrNull()) {
      return error(node, CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL_NUM_STRING);
    }
    return result.performToString(typeProvider, node);
  }

  @Override
  public EvaluationResultImpl visitInterpolationString(InterpolationString node) {
    return valid(typeProvider.getStringType(), new StringState(node.getValue()));
  }

  @Override
  public EvaluationResultImpl visitListLiteral(ListLiteral node) {
    if (node.getConstKeyword() == null) {
      return new ErrorResult(node, CompileTimeErrorCode.MISSING_CONST_IN_LIST_LITERAL);
    }
    ErrorResult result = null;
    ArrayList<DartObjectImpl> elements = new ArrayList<DartObjectImpl>();
    for (Expression element : node.getElements()) {
      EvaluationResultImpl elementResult = element.accept(this);
      result = union(result, elementResult);
      if (elementResult instanceof ValidResult) {
        elements.add(((ValidResult) elementResult).getValue());
      }
    }
    if (result != null) {
      return result;
    }
    return valid(
        typeProvider.getListType(),
        new ListState(elements.toArray(new DartObjectImpl[elements.size()])));
  }

  @Override
  public EvaluationResultImpl visitMapLiteral(MapLiteral node) {
    if (node.getConstKeyword() == null) {
      return new ErrorResult(node, CompileTimeErrorCode.MISSING_CONST_IN_MAP_LITERAL);
    }
    ErrorResult result = null;
    HashMap<DartObjectImpl, DartObjectImpl> map = new HashMap<DartObjectImpl, DartObjectImpl>();
    for (MapLiteralEntry entry : node.getEntries()) {
      EvaluationResultImpl keyResult = entry.getKey().accept(this);
      EvaluationResultImpl valueResult = entry.getValue().accept(this);
      result = union(result, keyResult);
      result = union(result, valueResult);
      if (keyResult instanceof ValidResult && valueResult instanceof ValidResult) {
        map.put(((ValidResult) keyResult).getValue(), ((ValidResult) valueResult).getValue());
      }
    }
    if (result != null) {
      return result;
    }
    return valid(typeProvider.getMapType(), new MapState(map));
  }

  @Override
  public EvaluationResultImpl visitMethodInvocation(MethodInvocation node) {
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
              EvaluationResultImpl leftArgument = arguments.get(0).accept(this);
              EvaluationResultImpl rightArgument = arguments.get(1).accept(this);
              return leftArgument.equalEqual(typeProvider, node, rightArgument);
            }
          }
        }
      }
    }
    // TODO(brianwilkerson) Figure out which error to report.
    return error(node, null);
  }

  @Override
  public EvaluationResultImpl visitNamedExpression(NamedExpression node) {
    return node.getExpression().accept(this);
  }

  @Override
  public EvaluationResultImpl visitNode(AstNode node) {
    // TODO(brianwilkerson) Figure out which error to report.
    return error(node, null);
  }

  @Override
  public EvaluationResultImpl visitNullLiteral(NullLiteral node) {
    return new ValidResult(getNull());
  }

  @Override
  public EvaluationResultImpl visitParenthesizedExpression(ParenthesizedExpression node) {
    return node.getExpression().accept(this);
  }

  @Override
  public EvaluationResultImpl visitPrefixedIdentifier(PrefixedIdentifier node) {
    // TODO(brianwilkerson) Uncomment the lines below when the new constant support can be added.
//    Element element = node.getStaticElement();
//    if (isStringLength(element)) {
//      EvaluationResultImpl target = node.getPrefix().accept(this);
//      return target.stringLength(typeProvider, node);
//    }
    SimpleIdentifier prefixNode = node.getPrefix();
    Element prefixElement = prefixNode.getStaticElement();
    if (!(prefixElement instanceof PrefixElement)) {
      EvaluationResultImpl prefixResult = prefixNode.accept(this);
      if (!(prefixResult instanceof ValidResult)) {
        return error(node, null);
      }
    }
    // validate prefixed identifier
    return getConstantValue(node, node.getStaticElement());
  }

  @Override
  public EvaluationResultImpl visitPrefixExpression(PrefixExpression node) {
    EvaluationResultImpl operand = node.getOperand().accept(this);
    if (operand instanceof ValidResult && ((ValidResult) operand).isNull()) {
      return error(node, CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
    }
    switch (node.getOperator().getType()) {
      case BANG:
        return operand.logicalNot(typeProvider, node);
      case TILDE:
        return operand.bitNot(typeProvider, node);
      case MINUS:
        return operand.negated(typeProvider, node);
      default:
        // TODO(brianwilkerson) Figure out which error to report.
        return error(node, null);
    }
  }

  @Override
  public EvaluationResultImpl visitPropertyAccess(PropertyAccess node) {
    Element element = node.getPropertyName().getStaticElement();
    // TODO(brianwilkerson) Uncomment the lines below when the new constant support can be added.
//    if (isStringLength(element)) {
//      EvaluationResultImpl target = node.getRealTarget().accept(this);
//      return target.stringLength(typeProvider, node);
//    }
    return getConstantValue(node, element);
  }

  @Override
  public EvaluationResultImpl visitSimpleIdentifier(SimpleIdentifier node) {
    if (lexicalEnvironment != null && lexicalEnvironment.containsKey(node.getName())) {
      return new ValidResult(lexicalEnvironment.get(node.getName()));
    }
    return getConstantValue(node, node.getStaticElement());
  }

  @Override
  public EvaluationResultImpl visitSimpleStringLiteral(SimpleStringLiteral node) {
    return valid(typeProvider.getStringType(), new StringState(node.getValue()));
  }

  @Override
  public EvaluationResultImpl visitStringInterpolation(StringInterpolation node) {
    EvaluationResultImpl result = null;
    for (InterpolationElement element : node.getElements()) {
      if (result == null) {
        result = element.accept(this);
      } else {
        result = result.concatenate(typeProvider, node, element.accept(this));
      }
    }
    return result;
  }

  @Override
  public EvaluationResultImpl visitSymbolLiteral(SymbolLiteral node) {
    StringBuilder builder = new StringBuilder();
    Token[] components = node.getComponents();
    for (int i = 0; i < components.length; i++) {
      if (i > 0) {
        builder.append('.');
      }
      builder.append(components[i].getLexeme());
    }
    return valid(typeProvider.getSymbolType(), new SymbolState(builder.toString()));
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

  ValidResult valid(InterfaceType type, InstanceState state) {
    return new ValidResult(new DartObjectImpl(type, state));
  }

  ValidResult validWithUnknownValue(InterfaceType type) {
    if (type.getElement().getLibrary().isDartCore()) {
      String typeName = type.getName();
      if (typeName.equals("bool")) {
        return valid(type, BoolState.UNKNOWN_VALUE);
      } else if (typeName.equals("double")) {
        return valid(type, DoubleState.UNKNOWN_VALUE);
      } else if (typeName.equals("int")) {
        return valid(type, IntState.UNKNOWN_VALUE);
      } else if (typeName.equals("String")) {
        return valid(type, StringState.UNKNOWN_VALUE);
      }
    }
    return valid(type, GenericState.UNKNOWN_VALUE);
  }

  /**
   * Return the value of the given expression, or a representation of 'null' if the expression
   * cannot be evaluated.
   * 
   * @param expression the expression whose value is to be returned
   * @return the value of the given expression
   */
  DartObjectImpl valueOf(Expression expression) {
    EvaluationResultImpl expressionValue = expression.accept(this);
    if (expressionValue instanceof ValidResult) {
      return ((ValidResult) expressionValue).getValue();
    }
    return getNull();
  }

  /**
   * Return a result object representing an error associated with the given node.
   * 
   * @param node the AST node associated with the error
   * @param code the error code indicating the nature of the error
   * @return a result object representing an error associated with the given node
   */
  private ErrorResult error(AstNode node, ErrorCode code) {
    return new ErrorResult(node, code == null ? CompileTimeErrorCode.INVALID_CONSTANT : code);
  }

  /**
   * Return the constant value of the static constant represented by the given element.
   * 
   * @param node the node to be used if an error needs to be reported
   * @param element the element whose value is to be returned
   * @return the constant value of the static constant
   */
  private EvaluationResultImpl getConstantValue(AstNode node, Element element) {
    if (element instanceof PropertyAccessorElement) {
      element = ((PropertyAccessorElement) element).getVariable();
    }
    if (element instanceof VariableElementImpl) {
      VariableElementImpl variableElementImpl = (VariableElementImpl) element;
      beforeGetEvaluationResult(node);
      EvaluationResultImpl value = variableElementImpl.getEvaluationResult();
      if (variableElementImpl.isConst() && value != null) {
        return value;
      }
    } else if (element instanceof ExecutableElement) {
      ExecutableElement function = (ExecutableElement) element;
      if (function.isStatic()) {
        return valid(typeProvider.getFunctionType(), new FunctionState(function));
      }
    } else if (element instanceof ClassElement || element instanceof FunctionTypeAliasElement) {
      return valid(typeProvider.getTypeType(), new TypeState(element));
    }
    // TODO(brianwilkerson) Figure out which error to report.
    return error(node, null);
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

  /**
   * Return the union of the errors encoded in the given results.
   * 
   * @param leftResult the first set of errors, or {@code null} if there was no previous collection
   *          of errors
   * @param rightResult the errors to be added to the collection, or a valid result if there are no
   *          errors to be added
   * @return the union of the errors encoded in the given results
   */
  private ErrorResult union(ErrorResult leftResult, EvaluationResultImpl rightResult) {
    if (rightResult instanceof ErrorResult) {
      if (leftResult != null) {
        return new ErrorResult(leftResult, (ErrorResult) rightResult);
      } else {
        return (ErrorResult) rightResult;
      }
    }
    return leftResult;
  }
}
