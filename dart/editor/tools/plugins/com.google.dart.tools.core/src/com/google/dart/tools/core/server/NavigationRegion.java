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
package com.google.dart.tools.core.server;

/**
 * The interface {@code NavigationRegion} defines the behavior of objects representing a list of
 * elements with which a source region is associated.
 */
public interface NavigationRegion extends SourceRegion {
  /**
   * Return the identifiers of the elements associated with the region.
   * 
   * @return the identifiers of the elements associated with the region
   */
  public NavigationTarget[] getTargets();
}
