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
package com.google.dart.tools.core.internal.analysis.model;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.tools.core.AbstractDartCoreTest;
import com.google.dart.tools.core.analysis.model.ContextManager;
import com.google.dart.tools.core.internal.builder.AnalysisWorker;
import com.google.dart.tools.core.internal.builder.MockContext;

public abstract class ContextManagerImplTest extends AbstractDartCoreTest {

  private class MockWorker extends AnalysisWorker {
    private boolean stopped = false;

    public MockWorker(ContextManager contextManager, AnalysisContext context) {
      super(contextManager, context);
    }

    public void assertStopped(boolean expected) {
      assertEquals(expected, stopped);
    }

    @Override
    public void stop() {
      stopped = true;
      super.stop();
    }
  }

  protected DartSdk sdk;

  public void test_getSdk() throws Exception {
    ContextManager manager = newTarget();
    DartSdk actual = manager.getSdk();
    assertNotNull(actual);
    assertSame(sdk, actual);
  }

  public void test_getSdkContext() throws Exception {
    ContextManager manager = newTarget();
    AnalysisContext actual = manager.getSdkContext();
    assertNotNull(actual);
    assertSame(sdk.getContext(), actual);
  }

  public void test_stopWorkers() throws Exception {
    ContextManager manager = newTarget();
    MockContext context1 = new MockContext();
    MockContext context2 = new MockContext();
    assertEquals(0, manager.getWorkers().length);
    MockWorker worker1 = new MockWorker(manager, context1);
    MockWorker worker2 = new MockWorker(manager, context2);
    MockWorker worker3 = new MockWorker(manager, context1);
    assertEquals(3, manager.getWorkers().length);
    manager.removeWorker(worker3);
    assertEquals(2, manager.getWorkers().length);
    worker1.assertStopped(false);
    worker2.assertStopped(false);
    worker3.assertStopped(false);
    manager.stopWorkers(context1);
    worker1.assertStopped(true);
    worker2.assertStopped(false);
    worker3.assertStopped(false);
    assertEquals(1, manager.getWorkers().length);
  }

  protected abstract ContextManager newTarget();

  @Override
  protected void setUp() throws Exception {
    sdk = DirectoryBasedDartSdk.getDefaultSdk();
  }
}
