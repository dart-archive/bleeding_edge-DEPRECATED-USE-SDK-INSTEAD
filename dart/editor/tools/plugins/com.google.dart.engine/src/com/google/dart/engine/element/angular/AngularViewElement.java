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
package com.google.dart.engine.element.angular;

/**
 * The interface {@code AngularViewElement} defines the Angular view defined using invocation like
 * <code>view('views/create.html')</code>.
 * 
 * @coverage dart.engine.element
 */
public interface AngularViewElement extends AngularHasTemplateElement {
  /**
   * An empty array of view elements.
   */
  public static final AngularViewElement[] EMPTY_ARRAY = new AngularViewElement[0];
}
