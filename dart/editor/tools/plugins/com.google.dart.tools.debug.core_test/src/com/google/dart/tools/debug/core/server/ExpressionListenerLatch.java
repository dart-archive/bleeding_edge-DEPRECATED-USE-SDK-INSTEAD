/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.tools.debug.core.server;

import org.eclipse.debug.core.model.IWatchExpressionListener;
import org.eclipse.debug.core.model.IWatchExpressionResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ExpressionListenerLatch implements IWatchExpressionListener {
  private CountDownLatch latch;
  private IWatchExpressionResult result;

  public ExpressionListenerLatch() {
    latch = new CountDownLatch(1);
  }

  public void await() throws InterruptedException {
    if (!latch.await(3000, TimeUnit.MILLISECONDS)) {
      throw new InterruptedException("never received breakpoint notification");
    }
  }

  public IWatchExpressionResult getResult() {
    return result;
  }

  @Override
  public void watchEvaluationFinished(IWatchExpressionResult result) {
    this.result = result;

    latch.countDown();
  }
}
