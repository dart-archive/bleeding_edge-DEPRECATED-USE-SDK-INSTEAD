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

package com.google.dart.tools.debug.core.server;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BreakpointLatch implements IBreakpointListener {
  private CountDownLatch latch;

  public BreakpointLatch(int count) {
    latch = new CountDownLatch(count);

    DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
  }

  public void await() throws InterruptedException {
    if (!latch.await(3000, TimeUnit.MILLISECONDS)) {
      throw new InterruptedException("never received breakpoint notification");
    }

    DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);

    // We may not be the last listener notified, so we delay a bit.
    Thread.sleep(100);
  }

  @Override
  public void breakpointAdded(IBreakpoint breakpoint) {
    latch.countDown();
  }

  @Override
  public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
    latch.countDown();
  }

  @Override
  public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
    latch.countDown();
  }

}
