/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.server;

import com.google.dart.engine.source.Source;

/**
 * The interface {@code NavigationTarget} defines the behavior of objects that provide information
 * about the target of a navigation region.
 * 
 * @coverage dart.server
 */
public interface NavigationTarget {
  /**
   * Return the id of the element to which this target will navigate.
   * 
   * @return the id of the element to which this target will navigate
   */
  public String getElementId();

  /**
   * Return the length of the region to which the target will navigate.
   * 
   * @return the length of the region to which the target will navigate
   */
  public int getLength();

  /**
   * Return the offset to the region to which the target will navigate.
   * 
   * @return the offset to the region to which the target will navigate
   */
  public int getOffset();

  /**
   * Return the source containing the element to which this target will navigate.
   * 
   * @return the source containing the element to which this target will navigate
   */
  public Source getSource();
}
