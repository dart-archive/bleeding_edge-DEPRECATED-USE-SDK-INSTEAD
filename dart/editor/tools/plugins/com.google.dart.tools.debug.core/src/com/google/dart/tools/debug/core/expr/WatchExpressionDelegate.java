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

import com.google.dart.tools.core.utilities.general.AdapterUtilities;

import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IWatchExpressionDelegate;
import org.eclipse.debug.core.model.IWatchExpressionListener;

/**
 * An implementation of a watch expression delegate. Used to asynchronously evaluate expressions in
 * certain contexts (like debugger call frames).
 * 
 * @see IWatchExpressionDelegate
 */
public class WatchExpressionDelegate implements IWatchExpressionDelegate {

  /**
   * Evaluates the given expression in the given context asynchronously and notifies the given
   * listener when the evaluation finishes.
   * 
   * @param expression the expression to evaluate
   * @param context the context for the evaluation
   * @param listener the listener to notify when the evaluation completes
   */
  @Override
  public void evaluateExpression(String expression, IDebugElement context,
      IWatchExpressionListener listener) {
    IExpressionEvaluator expressionEvaluator = AdapterUtilities.getAdapter(
        context,
        IExpressionEvaluator.class);

    if (expressionEvaluator != null) {
      expressionEvaluator.evaluateExpression(expression, listener);
    } else {
      listener.watchEvaluationFinished(WatchExpressionResult.noOp(expression));
    }
  }

}
