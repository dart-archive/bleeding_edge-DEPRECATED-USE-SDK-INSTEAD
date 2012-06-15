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

/**
 * The representation of a VM breakpoint.
 */
public class VmBreakpoint {
  private VmLocation location;
  private int breakpointId;

  VmBreakpoint(String url, int line, int breakpointId) {
    this.location = new VmLocation(url, line);
    this.breakpointId = breakpointId;
  }

  public int getBreakpointId() {
    return breakpointId;
  }

  public VmLocation getLocation() {
    return location;
  }

  @Override
  public String toString() {
    return "[breakpoint " + getBreakpointId() + "," + getLocation() + "]";
  }

  void updateInfo(String url, int line) {
    location.updateInfo(url, line);
  }

}
