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

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.AdjacentStrings;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.BooleanLiteral;
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
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.internal.element.VariableElementImpl;

/**
 * Instances of the class {@code ConstantVisitor} evaluate constant expressions to produce their
 * compile-time value. According to the Dart Language Specification: <blockquote> A constant
 * expression is one of the following:
 * <ul>
 * <li>A literal number.</li>
 * <li>A literal boolean.</li>
 * <li>A literal string where any interpolated expression is a compile-time constant that evaluates
 * to a numeric, string or boolean value or to {@code null}.</li>
 * <li>{@code null}.</li>
 * <li>A reference to a static constant variable.</li>
 * <li>An identifier expression that denotes a constant variable, a class or a type variable.</li>
 * <li>A constant constructor invocation.</li>
 * <li>A constant list literal.</li>
 * <li>A constant map literal.</li>
 * <li>A simple or qualified identifier denoting a top-level function or a static method.</li>
 * <li>A parenthesized expression {@code (e)} where {@code e} is a constant expression.</li>
 * <li>An expression of one of the forms {@code identical(e1, e2)}, {@code e1 == e2},
 * {@code e1 != e2} where {@code e1} and {@code e2} are constant expressions that evaluate to a
 * numeric, string or boolean value or to {@code null}.</li>
 * <li>An expression of one of the forms {@code !e}, {@code e1 && e2} or {@code e1 || e2}, where
 * {@code e}, {@code e1} and {@code e2} are constant expressions that evaluate to a boolean value or
 * to {@code null}.</li>
 * <li>An expression of one of the forms {@code ~e}, {@code e1 ^ e2}, {@code e1 & e2},
 * {@code e1 | e2}, {@code e1 >> e2} or {@code e1 << e2}, where {@code e}, {@code e1} and {@code e2}
 * are constant expressions that evaluate to an integer value or to {@code null}.</li>
 * <li>An expression of one of the forms {@code -e}, {@code e1 + e2}, {@code e1 - e2},
 * {@code e1 * e2}, {@code e1 / e2}, {@code e1 ~/ e2}, {@code e1 > e2}, {@code e1 < e2},
 * {@code e1 >= e2}, {@code e1 <= e2} or {@code e1 % e2}, where {@code e}, {@code e1} and {@code e2}
 * are constant expressions that evaluate to a numeric value or to {@code null}.</li>
 * </ul>
 * </blockquote>
 */
public class ConstantVisitor extends GeneralizingASTVisitor<EvaluationResultImpl> {
  /**
   * Initialize a newly created constant visitor.
   */
  public ConstantVisitor() {
    super();
  }

  @Override
  public EvaluationResultImpl visitAdjacentStrings(AdjacentStrings node) {
    EvaluationResultImpl result = null;
    for (StringLiteral string : node.getStrings()) {
      if (result == null) {
        result = string.accept(this);
      } else {
        result = result.concatenate(node, string.accept(this));
      }
    }
    return result;
  }

  @Override
  public EvaluationResultImpl visitBinaryExpression(BinaryExpression node) {
    EvaluationResultImpl leftResult = node.getLeftOperand().accept(this);
    EvaluationResultImpl rightResult = node.getRightOperand().accept(this);
    switch (node.getOperator().getType()) {
      case AMPERSAND:
        return leftResult.bitAnd(node, rightResult);
      case AMPERSAND_AMPERSAND:
        return leftResult.logicalAnd(node, rightResult);
      case BANG_EQ:
        return leftResult.notEqual(node, rightResult);
      case BAR:
        return leftResult.bitOr(node, rightResult);
      case BAR_BAR:
        return leftResult.logicalOr(node, rightResult);
      case CARET:
        return leftResult.bitXor(node, rightResult);
      case EQ_EQ:
        return leftResult.equalEqual(node, rightResult);
      case GT:
        return leftResult.greaterThan(node, rightResult);
      case GT_EQ:
        return leftResult.greaterThanOrEqual(node, rightResult);
      case GT_GT:
        return leftResult.shiftRight(node, rightResult);
      case LT:
        return leftResult.lessThan(node, rightResult);
      case LT_EQ:
        return leftResult.lessThanOrEqual(node, rightResult);
      case LT_LT:
        return leftResult.shiftLeft(node, rightResult);
      case MINUS:
        return leftResult.minus(node, rightResult);
      case PERCENT:
        return leftResult.remainder(node, rightResult);
      case PLUS:
        return leftResult.add(node, rightResult);
      case STAR:
        return leftResult.times(node, rightResult);
      case SLASH:
        return leftResult.divide(node, rightResult);
      case TILDE_SLASH:
        return leftResult.integerDivide(node, rightResult);
    }
    // TODO(brianwilkerson) Figure out which error to report.
    return error(node, null);
  }

  @Override
  public EvaluationResultImpl visitBooleanLiteral(BooleanLiteral node) {
    return node.getValue() ? ValidResult.RESULT_TRUE : ValidResult.RESULT_FALSE;
  }

  @Override
  public EvaluationResultImpl visitDoubleLiteral(DoubleLiteral node) {
    return new ValidResult(Double.valueOf(node.getValue()));
  }

  @Override
  public EvaluationResultImpl visitInstanceCreationExpression(InstanceCreationExpression node) {
    ConstructorElement constructor = node.getElement();
    if (constructor != null && constructor.isConst()) {
      node.getArgumentList().accept(this);
      return ValidResult.RESULT_OBJECT;
    }
    // TODO(brianwilkerson) Figure out which error to report.
    return error(node, null);
  }

  @Override
  public EvaluationResultImpl visitIntegerLiteral(IntegerLiteral node) {
    return new ValidResult(node.getValue());
  }

  @Override
  public EvaluationResultImpl visitInterpolationExpression(InterpolationExpression node) {
    EvaluationResultImpl result = node.getExpression().accept(this);
    return result.performToString(node);
  }

  @Override
  public EvaluationResultImpl visitInterpolationString(InterpolationString node) {
    return new ValidResult(node.getValue());
  }

  @Override
  public EvaluationResultImpl visitListLiteral(ListLiteral node) {
    ErrorResult result = null;
    for (Expression element : node.getElements()) {
      result = union(result, element.accept(this));
    }
    if (result != null) {
      return result;
    }
    return ValidResult.RESULT_OBJECT;
  }

  @Override
  public EvaluationResultImpl visitMapLiteral(MapLiteral node) {
    ErrorResult result = null;
    for (MapLiteralEntry entry : node.getEntries()) {
      result = union(result, entry.getKey().accept(this));
      result = union(result, entry.getValue().accept(this));
    }
    if (result != null) {
      return result;
    }
    return ValidResult.RESULT_OBJECT;
  }

  @Override
  public EvaluationResultImpl visitMethodInvocation(MethodInvocation node) {
    Element element = node.getMethodName().getElement();
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
              return leftArgument.equalEqual(node, rightArgument);
            }
          }
        }
      }
    }
    // TODO(brianwilkerson) Figure out which error to report.
    return error(node, null);
  }

  @Override
  public EvaluationResultImpl visitNode(ASTNode node) {
    // TODO(brianwilkerson) Figure out which error to report.
    return error(node, null);
  }

  @Override
  public EvaluationResultImpl visitNullLiteral(NullLiteral node) {
    return new ValidResult(null);
  }

  @Override
  public EvaluationResultImpl visitParenthesizedExpression(ParenthesizedExpression node) {
    return node.getExpression().accept(this);
  }

  @Override
  public EvaluationResultImpl visitPrefixedIdentifier(PrefixedIdentifier node) {
    return getConstantValue(node, node.getElement());
  }

  @Override
  public EvaluationResultImpl visitPrefixExpression(PrefixExpression node) {
    EvaluationResultImpl operand = node.getOperand().accept(this);
    switch (node.getOperator().getType()) {
      case BANG:
        return operand.logicalNot(node);
      case TILDE:
        return operand.bitNot(node);
      case MINUS:
        return operand.negated(node);
    }
    // TODO(brianwilkerson) Figure out which error to report.
    return error(node, null);
  }

  @Override
  public EvaluationResultImpl visitPropertyAccess(PropertyAccess node) {
    return getConstantValue(node, node.getPropertyName().getElement());
  }

  @Override
  public EvaluationResultImpl visitSimpleIdentifier(SimpleIdentifier node) {
    return getConstantValue(node, node.getElement());
  }

  @Override
  public EvaluationResultImpl visitSimpleStringLiteral(SimpleStringLiteral node) {
    return new ValidResult(node.getValue());
  }

  @Override
  public EvaluationResultImpl visitStringInterpolation(StringInterpolation node) {
    EvaluationResultImpl result = null;
    for (InterpolationElement element : node.getElements()) {
      if (result == null) {
        result = element.accept(this);
      } else {
        result = result.concatenate(node, element.accept(this));
      }
    }
    return result;
  }

  /**
   * Return a result object representing an error associated with the given node.
   * 
   * @param node the AST node associated with the error
   * @param code the error code indicating the nature of the error
   * @return a result object representing an error associated with the given node
   */
  private ErrorResult error(ASTNode node, ErrorCode code) {
    return new ErrorResult(node, code == null ? CompileTimeErrorCode.INVALID_CONSTANT : code);
  }

  /**
   * Return the constant value of the static constant represented by the given element.
   * 
   * @param node the node to be used if an error needs to be reported
   * @param element the element whose value is to be returned
   * @return the constant value of the static constant
   */
  private EvaluationResultImpl getConstantValue(ASTNode node, Element element) {
    if (element instanceof PropertyAccessorElement) {
      element = ((PropertyAccessorElement) element).getVariable();
    }
    if (element instanceof VariableElementImpl) {
      EvaluationResultImpl value = ((VariableElementImpl) element).getEvaluationResult();
      if (value != null) {
        return value;
      }
    } else if (element instanceof ExecutableElement) {
      return new ValidResult(element);
    }
    // TODO(brianwilkerson) Figure out which error to report.
    return error(node, null);
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
