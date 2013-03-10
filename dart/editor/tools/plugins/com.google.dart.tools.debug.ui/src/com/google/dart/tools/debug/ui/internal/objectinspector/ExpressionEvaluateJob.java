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

package com.google.dart.tools.debug.ui.internal.objectinspector;

import com.google.dart.tools.debug.core.util.IDartDebugValue;
import com.google.dart.tools.debug.ui.internal.presentation.DartDebugModelPresentation;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpressionListener;
import org.eclipse.debug.core.model.IWatchExpressionResult;
import org.eclipse.debug.ui.IValueDetailListener;

import java.util.concurrent.CountDownLatch;

/**
 * Evaluate the expression in an object context and return the result asynchronously.
 */
class ExpressionEvaluateJob extends Job {

  static interface ExpressionListener {
    public void watchEvaluationFinished(IWatchExpressionResult result, String stringValue);
  }

  private static DartDebugModelPresentation presentation = new DartDebugModelPresentation();

  private IDartDebugValue value;
  private String expression;
  private ExpressionListener callback;

  public ExpressionEvaluateJob(IDartDebugValue value, String expression, ExpressionListener callback) {
    super("Evaluating...");

    this.value = value;
    this.expression = expression;
    this.callback = callback;

    setUser(false);
  }

  @Override
  public boolean belongsTo(Object family) {
    return family == ObjectInspectorView.EXPRESSION_EVAL_JOB_FAMILY;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    final CountDownLatch latch = new CountDownLatch(1);
    final IWatchExpressionResult[] expResult = new IWatchExpressionResult[1];
    final String[] stringValue = new String[1];

    value.evaluateExpression(expression, new IWatchExpressionListener() {
      @Override
      public void watchEvaluationFinished(IWatchExpressionResult result) {
        expResult[0] = result;

        latch.countDown();
      }
    });

    try {
      latch.await();
    } catch (InterruptedException e) {

    }

    if (callback != null) {
      IWatchExpressionResult result = expResult[0];

      if (result.getValue() != null) {
        final CountDownLatch latch2 = new CountDownLatch(1);

        presentation.computeDetail(result.getValue(), new IValueDetailListener() {
          @Override
          public void detailComputed(IValue value, String strResult) {
            stringValue[0] = strResult;

            latch2.countDown();
          }
        });

        try {
          latch2.await();
        } catch (InterruptedException e) {

        }
      }

      callback.watchEvaluationFinished(result, stringValue[0]);
    }

    return Status.OK_STATUS;
  }

}
