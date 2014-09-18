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

import java.util.List;

/**
 * A default, abstract implementation of VmListener.
 */
public abstract class VMListenerAdapter implements VmListener {

  public VMListenerAdapter() {

  }

  @Override
  public void breakpointResolved(VmIsolate isolate, VmBreakpoint breakpoint) {

  }

  @Override
  public void connectionClosed(VmConnection connection) {

  }

  @Override
  public void connectionOpened(VmConnection connection) {

  }

  @Override
  public void debuggerPaused(PausedReason reason, VmIsolate isolate, List<VmCallFrame> frames,
      VmValue exception) {

  }

  @Override
  public void debuggerResumed(VmIsolate isolate) {

  }

  @Override
  public void isolateCreated(VmIsolate isolate) {

  }

  @Override
  public void isolateShutdown(VmIsolate isolate) {

  }
}
