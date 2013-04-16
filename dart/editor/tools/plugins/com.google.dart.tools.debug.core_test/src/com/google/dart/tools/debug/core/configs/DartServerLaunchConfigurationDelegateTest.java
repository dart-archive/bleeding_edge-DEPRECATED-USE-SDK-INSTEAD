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

package com.google.dart.tools.debug.core.configs;

import junit.framework.TestCase;

import org.eclipse.debug.core.model.IDebugTarget;

public class DartServerLaunchConfigurationDelegateTest extends TestCase {

  public void testPerformRemoteConnection1() throws Exception {

  }

  // TODO(devoncarew): this times out waiting for the VM to finish execution on the linux and mac
  // bots. I suspect that it's hitting an issue where the VM doesn't resume properly after the
  // initial pause.
  public void xxx_testPerformRemoteConnection2() throws Exception {
    VMDebugger vm = new VMDebugger();

    vm.start();

    try {
      DartServerLaunchConfigurationDelegate delegate = new DartServerLaunchConfigurationDelegate();

      IDebugTarget debugTarget = delegate.performRemoteConnection(
          null,
          vm.getConnectionPort(),
          null);

      assertNotNull(debugTarget);
      waitUntilFinished(debugTarget, 6000);
      String output = vm.getOutput();
      output = output.replaceAll("\r\n", "\n");
      assertEquals("1\n2\n3\n", output);
    } finally {
      vm.dispose();
    }
  }

  private void waitUntilFinished(IDebugTarget debugTarget, long maxWait)
      throws InterruptedException {
    long startTime = System.currentTimeMillis();

    while ((System.currentTimeMillis() - startTime) < maxWait) {
      if (debugTarget.isTerminated()) {
        return;
      }

      Thread.sleep(100);
    }

    if (!debugTarget.isTerminated()) {
      throw new InterruptedException("timeout waiting for vm to exit");
    }
  }

}
