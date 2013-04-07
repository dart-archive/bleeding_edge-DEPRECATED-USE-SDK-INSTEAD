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
package com.google.dart.tools.ui.instrumentation;

import com.google.dart.tools.ui.Activator;

import junit.framework.TestCase;

public class HeartbeatTest extends TestCase {

  public void testGetInstance() {
    assertNotNull(Heartbeat.getInstance());
  }

  public void testHeartbeat() {
    assertTrue(Activator.waitForEarlyStartup(10000));

    Heartbeat target = Heartbeat.getInstance();
    MockInstrumentationBuilder instrumentation = new MockInstrumentationBuilder();
    long start = System.currentTimeMillis();
    target.heartbeat(instrumentation);
    long delta = System.currentTimeMillis() - start;

//    instrumentation.echoToStdOut(getClass().getSimpleName());
//    System.out.println("heartbeat took " + delta + " ms");

    assertTrue(delta < 10);
    assertNotNull(instrumentation.getMetric("OpenWindowsCount"));
  }
}
