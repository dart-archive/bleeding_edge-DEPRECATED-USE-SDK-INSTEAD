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
package com.google.dart.engine.ast.visitor;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.AdjacentStrings;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.BooleanLiteral;
import com.google.dart.engine.ast.DoubleLiteral;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.InterpolationElement;
import com.google.dart.engine.ast.InterpolationExpression;
import com.google.dart.engine.ast.InterpolationString;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.MapLiteral;
import com.google.dart.engine.ast.MapLiteralEntry;
import com.google.dart.engine.ast.MethodInvocation;
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
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.scanner.Token;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Instances of the class {@code ConstantEvaluator} evaluate constant expressions to produce their
 * compile-time value. According to the Dart Language Specification: <blockquote> A constant
 * expression is one of the following:
 * <ul>
 * <li>A literal number.</li>
 * <li>A literal boolean.</li>
 * <li>A literal string where any interpolated expression is a compile-time constant that evaluates
 * to a numeric, string or boolean value or to {@code null}.</li>
 * <li>{@code null}.</li>
 * <li>A reference to a static constant variable.</li>
 * <li>An identifier expression that denotes a constant variable, a class or a type parameter.</li>
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
 * </blockquote> The values returned by instances of this class are therefore {@code null} and
 * instances of the classes {@code Boolean}, {@code BigInteger}, {@code Double}, {@code String}, and
 * {@code DartObject}.
 * <p>
 * In addition, this class defines several values that can be returned to indicate various
 * conditions encountered during evaluation. These are documented with the static field that define
 * those values.
 * 
 * @coverage dart.engine.ast
 */
public class ConstantEvaluator extends GeneralizingAstVisitor<Object> {
  /**
   * The value returned for expressions (or non-expression nodes) that are not compile-time constant
   * expressions.
   */
  public static final Object NOT_A_CONSTANT = new Object();

  /**
   * Initialize a newly created constant evaluator.
   */
  public ConstantEvaluator() {
  }

  @Override
  public Object visitAdjacentStrings(AdjacentStrings node) {
    StringBuilder builder = new StringBuilder();
    for (StringLiteral string : node.getStrings()) {
      Object value = string.accept(this);
      if (value == NOT_A_CONSTANT) {
        return value;
      }
      builder.append(value);
    }
    return builder.toString();
  }

  @Override
  public Object visitBinaryExpression(BinaryExpression node) {
    Object leftOperand = node.getLeftOperand().accept(this);
    if (leftOperand == NOT_A_CONSTANT) {
      return leftOperand;
    }
    Object rightOperand = node.getRightOperand().accept(this);
    if (rightOperand == NOT_A_CONSTANT) {
      return rightOperand;
    }
    switch (node.getOperator().getType()) {
      case AMPERSAND:
        // integer or {@code null}
        if (leftOperand instanceof BigInteger && rightOperand instanceof BigInteger) {
          return ((BigInteger) leftOperand).and((BigInteger) rightOperand);
        }
        break;
      case AMPERSAND_AMPERSAND:
        // boolean or {@code null}
        if (leftOperand instanceof Boolean && rightOperand instanceof Boolean) {
          return ((Boolean) leftOperand).booleanValue() && ((Boolean) rightOperand).booleanValue();
        }
        break;
      case BANG_EQ:
        // numeric, string, boolean, or {@code null}
        if (leftOperand instanceof Boolean && rightOperand instanceof Boolean) {
          return ((Boolean) leftOperand).booleanValue() != ((Boolean) rightOperand).booleanValue();
        } else if (leftOperand instanceof BigInteger && rightOperand instanceof BigInteger) {
          return !((BigInteger) leftOperand).equals(rightOperand);
        } else if (leftOperand instanceof Double && rightOperand instanceof Double) {
          return !((Double) leftOperand).equals(rightOperand);
        } else if (leftOperand instanceof String && rightOperand instanceof String) {
          return !((String) leftOperand).equals(rightOperand);
        }
        break;
      case BAR:
        // integer or {@code null}
        if (leftOperand instanceof BigInteger && rightOperand instanceof BigInteger) {
          return ((BigInteger) leftOperand).or((BigInteger) rightOperand);
        }
        break;
      case BAR_BAR:
        // boolean or {@code null}
        if (leftOperand instanceof Boolean && rightOperand instanceof Boolean) {
          return ((Boolean) leftOperand).booleanValue() || ((Boolean) rightOperand).booleanValue();
        }
        break;
      case CARET:
        // integer or {@code null}
        if (leftOperand instanceof BigInteger && rightOperand instanceof BigInteger) {
          return ((BigInteger) leftOperand).xor((BigInteger) rightOperand);
        }
        break;
      case EQ_EQ:
        // numeric, string, boolean, or {@code null}
        if (leftOperand instanceof Boolean && rightOperand instanceof Boolean) {
          return ((Boolean) leftOperand).booleanValue() == ((Boolean) rightOperand).booleanValue();
        } else if (leftOperand instanceof BigInteger && rightOperand instanceof BigInteger) {
          return ((BigInteger) leftOperand).equals(rightOperand);
        } else if (leftOperand instanceof Double && rightOperand instanceof Double) {
          return ((Double) leftOperand).equals(rightOperand);
        } else if (leftOperand instanceof String && rightOperand instanceof String) {
          return ((String) leftOperand).equals(rightOperand);
        }
        break;
      case GT:
        // numeric or {@code null}
        if (leftOperand instanceof BigInteger && rightOperand instanceof BigInteger) {
          return ((BigInteger) leftOperand).compareTo((BigInteger) rightOperand) > 0;
        } else if (leftOperand instanceof Double && rightOperand instanceof Double) {
          return ((Double) leftOperand).compareTo((Double) rightOperand) > 0;
        }
        break;
      case GT_EQ:
        // numeric or {@code null}
        if (leftOperand instanceof BigInteger && rightOperand instanceof BigInteger) {
          return ((BigInteger) leftOperand).compareTo((BigInteger) rightOperand) >= 0;
        } else if (leftOperand instanceof Double && rightOperand instanceof Double) {
          return ((Double) leftOperand).compareTo((Double) rightOperand) >= 0;
        }
        break;
      case GT_GT:
        // integer or {@code null}
        if (leftOperand instanceof BigInteger && rightOperand instanceof BigInteger) {
          return ((BigInteger) leftOperand).shiftRight(((BigInteger) rightOperand).intValue());
        }
        break;
      case LT:
        // numeric or {@code null}
        if (leftOperand instanceof BigInteger && rightOperand instanceof BigInteger) {
          return ((BigInteger) leftOperand).compareTo((BigInteger) rightOperand) < 0;
        } else if (leftOperand instanceof Double && rightOperand instanceof Double) {
          return ((Double) leftOperand).compareTo((Double) rightOperand) < 0;
        }
        break;
      case LT_EQ:
        // numeric or {@code null}
        if (leftOperand instanceof BigInteger && rightOperand instanceof BigInteger) {
          return ((BigInteger) leftOperand).compareTo((BigInteger) rightOperand) <= 0;
        } else if (leftOperand instanceof Double && rightOperand instanceof Double) {
          return ((Double) leftOperand).compareTo((Double) rightOperand) <= 0;
        }
        break;
      case LT_LT:
        // integer or {@code null}
        if (leftOperand instanceof BigInteger && rightOperand instanceof BigInteger) {
          return ((BigInteger) leftOperand).shiftLeft(((BigInteger) rightOperand).intValue());
        }
        break;
      case MINUS:
        // numeric or {@code null}
        if (leftOperand instanceof BigInteger && rightOperand instanceof BigInteger) {
          return ((BigInteger) leftOperand).subtract((BigInteger) rightOperand);
        } else if (leftOperand instanceof Double && rightOperand instanceof Double) {
          return ((Double) leftOperand).doubleValue() - ((Double) rightOperand).doubleValue();
        }
        break;
      case PERCENT:
        // numeric or {@code null}
        if (leftOperand instanceof BigInteger && rightOperand instanceof BigInteger) {
          return ((BigInteger) leftOperand).remainder((BigInteger) rightOperand);
        } else if (leftOperand instanceof Double && rightOperand instanceof Double) {
          return ((Double) leftOperand).doubleValue() % ((Double) rightOperand).doubleValue();
        }
        break;
      case PLUS:
        // numeric or {@code null}
        if (leftOperand instanceof BigInteger && rightOperand instanceof BigInteger) {
          return ((BigInteger) leftOperand).add((BigInteger) rightOperand);
        } else if (leftOperand instanceof Double && rightOperand instanceof Double) {
          return ((Double) leftOperand).doubleValue() + ((Double) rightOperand).doubleValue();
        }
        break;
      case STAR:
        // numeric or {@code null}
        if (leftOperand instanceof BigInteger && rightOperand instanceof BigInteger) {
          return ((BigInteger) leftOperand).multiply((BigInteger) rightOperand);
        } else if (leftOperand instanceof Double && rightOperand instanceof Double) {
          return ((Double) leftOperand).doubleValue() * ((Double) rightOperand).doubleValue();
        }
        break;
      case SLASH:
        // numeric or {@code null}
        if (leftOperand instanceof BigInteger && rightOperand instanceof BigInteger) {
          if (!rightOperand.equals(BigInteger.ZERO)) {
            return ((BigInteger) leftOperand).divide((BigInteger) rightOperand);
          } else {
            return Double.valueOf(((BigInteger) leftOperand).doubleValue()
                / ((BigInteger) rightOperand).doubleValue());
          }
        } else if (leftOperand instanceof Double && rightOperand instanceof Double) {
          return ((Double) leftOperand).doubleValue() / ((Double) rightOperand).doubleValue();
        }
        break;
      case TILDE_SLASH:
        // numeric or {@code null}
        if (leftOperand instanceof BigInteger && rightOperand instanceof BigInteger) {
          if (!rightOperand.equals(BigInteger.ZERO)) {
            return ((BigInteger) leftOperand).divide((BigInteger) rightOperand);
          } else {
            return BigInteger.ZERO;
          }
        } else if (leftOperand instanceof Double && rightOperand instanceof Double) {
          return BigInteger.valueOf(Double.valueOf(
              Math.floor(((Double) leftOperand).doubleValue()
                  / ((Double) rightOperand).doubleValue())).longValue());
        }
        break;
      default:
        // Fall through to return the default value.
        break;
    }
    // TODO(brianwilkerson) This doesn't handle numeric conversions.
    return visitExpression(node);
  }

  @Override
  public Object visitBooleanLiteral(BooleanLiteral node) {
    return node.getValue() ? Boolean.TRUE : Boolean.FALSE;
  }

  @Override
  public Object visitDoubleLiteral(DoubleLiteral node) {
    return Double.valueOf(node.getValue());
  }

  @Override
  public Object visitIntegerLiteral(IntegerLiteral node) {
    return node.getValue();
  }

  @Override
  public Object visitInterpolationExpression(InterpolationExpression node) {
    Object value = node.getExpression().accept(this);
    if (value == null || value instanceof Boolean || value instanceof String
        || value instanceof BigInteger || value instanceof Double) {
      return value;
    }
    return NOT_A_CONSTANT;
  }

  @Override
  public Object visitInterpolationString(InterpolationString node) {
    return node.getValue();
  }

  @Override
  public Object visitListLiteral(ListLiteral node) {
    ArrayList<Object> list = new ArrayList<Object>();
    for (Expression element : node.getElements()) {
      Object value = element.accept(this);
      if (value == NOT_A_CONSTANT) {
        return value;
      }
      list.add(value);
    }
    return list;
  }

  @Override
  public Object visitMapLiteral(MapLiteral node) {
    HashMap<String, Object> map = new HashMap<String, Object>();
    for (MapLiteralEntry entry : node.getEntries()) {
      Object key = entry.getKey().accept(this);
      Object value = entry.getValue().accept(this);
      if (!(key instanceof String) || value == NOT_A_CONSTANT) {
        return NOT_A_CONSTANT;
      }
      map.put((String) key, value);
    }
    return map;
  }

  @Override
  public Object visitMethodInvocation(MethodInvocation node) {
    // TODO(brianwilkerson) Need to look for invocation of "identical".
    return visitNode(node);
  }

  @Override
  public Object visitNode(AstNode node) {
    return NOT_A_CONSTANT;
  }

  @Override
  public Object visitNullLiteral(NullLiteral node) {
    return null;
  }

  @Override
  public Object visitParenthesizedExpression(ParenthesizedExpression node) {
    return node.getExpression().accept(this);
  }

  @Override
  public Object visitPrefixedIdentifier(PrefixedIdentifier node) {
    // TODO(brianwilkerson) Resolve the identifier.
    return getConstantValue(null);
  }

  @Override
  public Object visitPrefixExpression(PrefixExpression node) {
    Object operand = node.getOperand().accept(this);
    if (operand == NOT_A_CONSTANT) {
      return operand;
    }
    switch (node.getOperator().getType()) {
      case BANG:
        if (operand == Boolean.TRUE) {
          return Boolean.FALSE;
        } else if (operand == Boolean.FALSE) {
          return Boolean.TRUE;
        }
        // TODO(brianwilkerson) We might need to support !null, but I don't know yet what value to return.
        break;
      case TILDE:
        if (operand instanceof BigInteger) {
          return ((BigInteger) operand).not();
        }
        break;
      case MINUS:
        if (operand == null) {
          return null;
        } else if (operand instanceof BigInteger) {
          return ((BigInteger) operand).negate();
        } else if (operand instanceof Double) {
          return Double.valueOf(-((Double) operand).doubleValue());
        }
        break;
      default:
        // Fall through to return the default value.
        break;
    }
    return NOT_A_CONSTANT;
  }

  @Override
  public Object visitPropertyAccess(PropertyAccess node) {
    // TODO(brianwilkerson) Resolve the property.
    return getConstantValue(null);
  }

  @Override
  public Object visitSimpleIdentifier(SimpleIdentifier node) {
    // TODO(brianwilkerson) Resolve the identifier.
    return getConstantValue(null);
  }

  @Override
  public Object visitSimpleStringLiteral(SimpleStringLiteral node) {
    return node.getValue();
  }

  @Override
  public Object visitStringInterpolation(StringInterpolation node) {
    StringBuilder builder = new StringBuilder();
    for (InterpolationElement element : node.getElements()) {
      Object value = element.accept(this);
      if (value == NOT_A_CONSTANT) {
        return value;
      }
      builder.append(value);
    }
    return builder.toString();
  }

  @Override
  public Object visitSymbolLiteral(SymbolLiteral node) {
    // TODO(brianwilkerson) This isn't optimal because a Symbol is not a String.
    StringBuilder builder = new StringBuilder();
    for (Token component : node.getComponents()) {
      if (builder.length() > 0) {
        builder.append('.');
      }
      builder.append(component.getLexeme());
    }
    return builder.toString();
  }

  /**
   * Return the constant value of the static constant represented by the given element.
   * 
   * @param element the element whose value is to be returned
   * @return the constant value of the static constant
   */
  private Object getConstantValue(Element element) {
    // TODO(brianwilkerson) Implement this
    if (element instanceof FieldElement) {
      FieldElement field = (FieldElement) element;
      if (field.isStatic() && field.isConst()) {
        //field.getConstantValue();
      }
//    } else if (element instanceof VariableElement) {
//      VariableElement variable = (VariableElement) element;
//      if (variable.isStatic() && variable.isConst()) {
//        //variable.getConstantValue();
//      }
    }
    return NOT_A_CONSTANT;
  }

}
