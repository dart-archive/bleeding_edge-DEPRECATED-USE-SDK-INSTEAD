/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.model.info;

import com.google.dart.tools.core.internal.model.DartElementImpl;
import com.google.dart.tools.core.model.DartElement;

/**
 * Instances of the class <code>DartImportContainerInfo</code> represent a container for the imports
 * within a library or application file.
 */
public class DartImportContainerInfo extends DartElementInfo {
  /**
   * Collection of handles of immediate children of this object. This is an empty array if this
   * element has no children.
   */
  private DartElement[] children = DartElementImpl.EMPTY_ARRAY;

  @Override
  public DartElement[] getChildren() {
    return children;
  }

  public void setChildren(DartElement[] newChildren) {
    children = newChildren;
  }
}
