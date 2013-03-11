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
package com.google.dart.engine.constant;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.internal.constant.ConstantVisitor;
import com.google.dart.engine.internal.constant.ErrorResult;
import com.google.dart.engine.internal.constant.EvaluationResultImpl;
import com.google.dart.engine.internal.constant.ValidResult;
import com.google.dart.engine.source.Source;

import java.util.ArrayList;

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
 * </blockquote> The values returned by instances of this class are therefore {@code null} and
 * instances of the classes {@code Boolean}, {@code BigInteger}, {@code Double}, {@code String}, and
 * {@code DartObject}.
 * <p>
 * In addition, this class defines several values that can be returned to indicate various
 * conditions encountered during evaluation. These are documented with the static field that define
 * those values.
 */
public class ConstantEvaluator {
  /**
   * The source containing the expression(s) that will be evaluated.
   */
  private Source source;

  /**
   * Initialize a newly created evaluator to evaluate expressions in the given source.
   * 
   * @param source the source containing the expression(s) that will be evaluated
   */
  public ConstantEvaluator(Source source) {
    this.source = source;
  }

  public EvaluationResult evaluate(Expression expression) {
    EvaluationResultImpl result = expression.accept(new ConstantVisitor());
    if (result instanceof ValidResult) {
      return EvaluationResult.forValue(((ValidResult) result).getValue());
    }
    ArrayList<AnalysisError> errors = new ArrayList<AnalysisError>();
    for (ErrorResult.ErrorData data : ((ErrorResult) result).getErrorData()) {
      ASTNode node = data.getNode();
      errors.add(new AnalysisError(source, node.getOffset(), node.getLength(), data.getErrorCode()));
    }
    return EvaluationResult.forErrors(errors.toArray(new AnalysisError[errors.size()]));
  }
}
