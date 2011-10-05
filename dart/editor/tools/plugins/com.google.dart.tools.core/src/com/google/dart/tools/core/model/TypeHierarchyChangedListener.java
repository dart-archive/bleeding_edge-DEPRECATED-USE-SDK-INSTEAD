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
package com.google.dart.tools.core.model;

/**
 * A listener which gets notified when a particular type hierarchy object changes.
 * <p>
 * This interface may be implemented by clients.
 * </p>
 */
public interface TypeHierarchyChangedListener {
  /**
   * Notifies that the given type hierarchy has changed in some way and should be refreshed at some
   * point to make it consistent with the current state of the Java model.
   * 
   * @param typeHierarchy the given type hierarchy
   */
  void typeHierarchyChanged(TypeHierarchy typeHierarchy);
}
