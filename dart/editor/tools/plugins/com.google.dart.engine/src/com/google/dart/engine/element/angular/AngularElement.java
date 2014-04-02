/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.element.angular;

import com.google.dart.engine.element.ToolkitObjectElement;
import com.google.dart.engine.internal.element.angular.AngularApplication;

/**
 * The interface {@code AngularElement} defines the behavior of objects representing information
 * about an Angular specific element.
 * 
 * @coverage dart.engine.element
 */
public interface AngularElement extends ToolkitObjectElement {
  /**
   * An empty array of Angular elements.
   */
  AngularElement[] EMPTY_ARRAY = new AngularElement[0];

  /**
   * Returns the {@link AngularApplication} this element is used in.
   * 
   * @return the {@link AngularApplication} this element is used in
   */
  AngularApplication getApplication();
}
