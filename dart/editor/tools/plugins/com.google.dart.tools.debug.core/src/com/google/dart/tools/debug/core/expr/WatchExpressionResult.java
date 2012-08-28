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

package com.google.dart.tools.debug.core.expr;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpressionResult;

import java.util.Collections;
import java.util.List;

/**
 * A IWatchExpressionResult. Instances can be created using the given static factory methods.
 */
public class WatchExpressionResult implements IWatchExpressionResult {

  /**
   * @return a expression result with the given error text
   */
  public static IWatchExpressionResult error(String expression, String errorText) {
    WatchExpressionResult result = new WatchExpressionResult(expression);

    result.errors = Collections.singletonList(errorText);

    return result;
  }

  /**
   * @return an expression result with no value or errors
   */
  public static IWatchExpressionResult noOp(String expression) {
    return new WatchExpressionResult(expression);
  }

  /**
   * @return a expression result with the given value
   */
  public static IWatchExpressionResult value(String expression, IValue value) {
    WatchExpressionResult result = new WatchExpressionResult(expression);

    result.value = value;

    return result;
  }

  private List<String> errors = Collections.emptyList();
  private IValue value;

  private DebugException exception;

  private String expressionText;

  private WatchExpressionResult(String expression) {
    this.expressionText = expression;
  }

  @Override
  public String[] getErrorMessages() {
    return errors.toArray(new String[errors.size()]);
  }

  @Override
  public DebugException getException() {
    return exception;
  }

  @Override
  public String getExpressionText() {
    return expressionText;
  }

  @Override
  public IValue getValue() {
    return value;
  }

  @Override
  public boolean hasErrors() {
    return errors.size() > 0;
  }

}
