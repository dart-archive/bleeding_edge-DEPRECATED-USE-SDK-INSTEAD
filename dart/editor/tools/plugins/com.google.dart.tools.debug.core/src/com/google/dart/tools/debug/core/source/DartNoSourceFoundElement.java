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

package com.google.dart.tools.debug.core.source;

import org.eclipse.debug.core.ILaunch;

/**
 * A placeholder for when we can't locate source for a debugger element (generally a stack frame).
 */
public class DartNoSourceFoundElement {
  private ILaunch launch;
  private String description;

  protected DartNoSourceFoundElement(ILaunch launch, String description) {
    this.launch = launch;
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public ILaunch getLaunch() {
    return launch;
  }

}
