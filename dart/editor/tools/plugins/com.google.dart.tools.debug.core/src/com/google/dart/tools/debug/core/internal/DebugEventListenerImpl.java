/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.debug.core.internal;

import org.chromium.sdk.DebugContext;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.Script;

/**
 * This class listens to and acts on Chrome / V8 events.
 * 
 * @see DebugEventListener
 */
public class DebugEventListenerImpl implements DebugEventListener {
  private ChromeDebugTarget debugTarget;

  /**
   * Create a new DebugEventListenerImpl.
   * 
   * @param debugTarget
   */
  public DebugEventListenerImpl(ChromeDebugTarget debugTarget) {
    this.debugTarget = debugTarget;
  }

  @Override
  public void disconnected() {
    fireTerminateEvent();
  }

  @Override
  public VmStatusListener getVmStatusListener() {
    return null;
  }

  @Override
  public void resumed() {
    // TODO(devoncarew):

    System.out.println("resumed()");
  }

  @Override
  public void scriptCollected(Script script) {
    // TODO(devoncarew):

    System.out.println("scriptCollected()");
  }

  @Override
  public void scriptContentChanged(Script newScript) {
    // TODO(devoncarew):

    System.out.println("scriptContentChanged()");
  }

  @Override
  public void scriptLoaded(Script newScript) {
    // TODO(devoncarew):

    System.out.println("scriptLoaded()");
  }

  @Override
  public void suspended(DebugContext context) {
    // TODO(devoncarew):

    System.out.println("suspended()");
  }

  private void fireTerminateEvent() {
    // Do not report on threads -- the children are gone when terminated.

    debugTarget.fireTerminateEvent();
  }

}
