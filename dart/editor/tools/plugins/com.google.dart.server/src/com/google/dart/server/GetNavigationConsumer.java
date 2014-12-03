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

import com.google.dart.server.generated.types.NavigationRegion;
import com.google.dart.server.generated.types.NavigationTarget;

/**
 * The interface {@code GetNavigationConsumer} defines the behavior of objects that consume
 * navigation information from an {@code analysis.getNavigation} request.
 * 
 * @coverage dart.server
 */
public interface GetNavigationConsumer {
  /**
   * The navigation information that has been computed.
   * 
   * @param files the paths of files that are referenced by the navigation targets
   * @param targets the navigation targets that are referenced by the navigation regions
   * @param regions the navigation regions within the requested region of the file
   */
  public void computedNavigation(String[] files, NavigationTarget[] targets,
      NavigationRegion[] regions);
}
