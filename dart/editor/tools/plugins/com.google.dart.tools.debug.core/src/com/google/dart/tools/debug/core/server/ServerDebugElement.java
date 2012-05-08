/*
 * Copyright (c) 2012, the Dart project authors.
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

import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugTarget;

/**
 * The abstract parent of all Dart VM debug elements.
 */
public abstract class ServerDebugElement extends DebugElement {

  /**
   * Create a new server debug element.
   * 
   * @param target
   */
  public ServerDebugElement(IDebugTarget target) {
    super(target);
  }

  @Override
  public String getModelIdentifier() {
    return DartDebugCorePlugin.DEBUG_MODEL_ID;
  }

  protected VmConnection getConnection() {
    return getTarget().getVmConnection();
  }

  /**
   * @return the ServerDebugTarget for this element
   */
  protected ServerDebugTarget getTarget() {
    return (ServerDebugTarget) getDebugTarget();
  }

}
