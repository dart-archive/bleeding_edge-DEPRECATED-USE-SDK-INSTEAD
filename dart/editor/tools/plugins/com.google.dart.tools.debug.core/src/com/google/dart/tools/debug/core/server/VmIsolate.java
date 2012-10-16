/*
 * Copyright 2012 Dart project authors.
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

import java.util.HashMap;
import java.util.Map;

/**
 * The representation of a VM isolate.
 */
public class VmIsolate {
  private int id;

  private Map<Integer, String> classNameMap = new HashMap<Integer, String>();

  protected VmIsolate(int isolateId) {
    this.id = isolateId;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof VmIsolate) {
      VmIsolate isolate = (VmIsolate) other;

      return getId() == isolate.getId();
    }

    return false;
  }

  public String getClassName(int classId) {
    return classNameMap.get(classId);
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return "isolate-" + getId();
  }

  public boolean hasClassName(int classId) {
    return classNameMap.containsKey(classId);
  }

  @Override
  public int hashCode() {
    return getId();
  }

  @Override
  public String toString() {
    return "VmIsolate " + getId();
  }

  protected void clearClassNameMap() {
    classNameMap.clear();
  }

  protected void setClassName(int classId, String className) {
    classNameMap.put(classId, className);
  }

}
