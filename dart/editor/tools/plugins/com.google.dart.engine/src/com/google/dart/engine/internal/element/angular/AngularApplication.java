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

package com.google.dart.engine.internal.element.angular;

import com.google.dart.engine.element.angular.AngularElement;
import com.google.dart.engine.source.Source;

/**
 * Information about Angular application.
 */
public class AngularApplication {
  private final Source entryPoint;
  private final AngularElement[] elements;

  public AngularApplication(Source entryPoint, AngularElement[] elements) {
    this.entryPoint = entryPoint;
    this.elements = elements;
  }

  /**
   * Returns {@link AngularElement}s that are accessible in this Angular application.
   */
  public AngularElement[] getElements() {
    return elements;
  }

  /**
   * Returns the entry point {@link Source}, which should be used to resolve all Web URIs.
   */
  public Source getEntryPoint() {
    return entryPoint;
  }
}
