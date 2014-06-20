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

/**
 * The interface {@code NavigationTarget} defines the object that some {@link NavigationRegion} is
 * associated.
 * 
 * @coverage dart.server
 */
public interface NavigationTarget extends SourceRegion {
  /**
   * An empty array of navigation targets.
   */
  NavigationTarget[] EMPTY_ARRAY = new NavigationTarget[0];

  /**
   * Return the element to which this target should navigate.
   * 
   * @return the element to which this target should navigate
   */
  public Element getElement();

  /**
   * Return the file containing the element to which this target will navigate.
   * 
   * @return the file containing the element to which this target will navigate
   */
  public String getFile();

}
