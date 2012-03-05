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
package com.google.dart.tools.ui.internal.callhierarchy;

import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MethodCall {

  private DartElement member;
  private List<CallLocation> callLocations;

  public MethodCall(DartElement enclosingElement) {
    this.member = enclosingElement;
  }

  public void addCallLocation(CallLocation location) {
    if (callLocations == null) {
      callLocations = new ArrayList<CallLocation>();
    }
    callLocations.add(location);
  }

  public Collection<CallLocation> getCallLocations() {
    return callLocations;
  }

  public CallLocation getFirstCallLocation() {
    if ((callLocations != null) && !callLocations.isEmpty()) {
      return callLocations.get(0);
    } else {
      return null;
    }
  }

  public String getKey() {
    return ((CompilationUnitElement) getMember()).getHandleIdentifier();
  }

  public DartElement getMember() {
    return member;
  }

  public boolean hasCallLocations() {
    return callLocations != null && callLocations.size() > 0;
  }
}
