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

package com.google.dart.tools.core.test.util;

import org.eclipse.core.runtime.IProgressMonitor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This progress monitor implementation will block until the done() method is called. If the task
 * takes longer then a fixed amount if time, the await() method will terminate with a
 * RuntimeException.
 * <p>
 * This class is intended to be used by the testing framework.
 */
public class ProgressMonitorLatch implements IProgressMonitor {
  CountDownLatch latch;

  public ProgressMonitorLatch() {
    latch = new CountDownLatch(1);
  }

  public void await() {
    try {
      // Wait 10 seconds.
      latch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void beginTask(String name, int totalWork) {

  }

  @Override
  public void done() {
    latch.countDown();
  }

  @Override
  public void internalWorked(double work) {

  }

  @Override
  public boolean isCanceled() {
    return latch.getCount() == 0;
  }

  @Override
  public void setCanceled(boolean value) {
    latch.countDown();
  }

  @Override
  public void setTaskName(String name) {

  }

  @Override
  public void subTask(String name) {

  }

  @Override
  public void worked(int work) {

  }

}
