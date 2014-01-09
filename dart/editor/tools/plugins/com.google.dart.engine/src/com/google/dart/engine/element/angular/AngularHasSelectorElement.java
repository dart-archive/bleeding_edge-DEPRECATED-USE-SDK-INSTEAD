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

/**
 * The interface {@code AngularElement} defines the behavior of objects representing information
 * about an Angular element which is applied conditionally using some {@link AngularSelectorElement}.
 * 
 * @coverage dart.engine.element
 */
public interface AngularHasSelectorElement extends AngularElement {
  /**
   * Returns the selector specified for this element.
   * 
   * @return the {@link AngularSelectorElement} specified for this element
   */
  AngularSelectorElement getSelector();
}
