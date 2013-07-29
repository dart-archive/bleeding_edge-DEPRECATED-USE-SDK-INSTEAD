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

import com.google.dart.tools.debug.core.breakpoints.DartBreakpoint;
import com.google.dart.tools.debug.core.server.ServerTestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IThread;

public class DartServerLaunchConfigurationDelegateTest extends ServerTestCase {

  public void testPerformRemoteConnection() throws Exception {
    vm.connect(testFile.getLocation().toFile().getAbsolutePath());

    assertNotNull(vm.getDebugTarget());

    assertEquals("1", vm.readLine());
    assertEquals("2", vm.readLine());
    assertEquals("3", vm.readLine());

    vm.waitForExit(3000);
  }

  @Override
  protected void assertPaused() throws DebugException {
    long start = System.currentTimeMillis();

    while (System.currentTimeMillis() - start < 1000) {
      IThread thread = vm.getDebugTarget().getThreads()[0];

      if (thread.isSuspended()) {
        return;
      }

      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {

      }
    }

    assertTrue(vm.getDebugTarget() + " not paused", false);
  }

  @Override
  protected void createBreakpoint(final IFile file, final int line) throws CoreException,
      InterruptedException {
    DartBreakpoint bp = new DartBreakpoint(file, line);

    DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(bp);
  }

}
