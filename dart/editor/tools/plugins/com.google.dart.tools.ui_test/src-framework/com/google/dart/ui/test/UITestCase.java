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
package com.google.dart.ui.test;

import com.google.dart.ui.test.model.internal.workbench.LogWatcher;

import junit.framework.TestCase;

import org.eclipse.swt.widgets.Display;

/**
 * Base class for UI tests.
 */
public class UITestCase extends TestCase {

  private final LogWatcher watcher = new LogWatcher();

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    checkThread();
    watcher.start();
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      watcher.assertNoLoggedExceptions();
    } finally {
      watcher.stop();
    }

    super.tearDown();
  }

  private void checkThread() {
    assertTrue(
        "UI tests should not be run on the UI thread.  (Be sure 'Run in UI thread' is UN-checked "
            + "in your launch config.)",
        Display.getCurrent() == null);
  }

}
