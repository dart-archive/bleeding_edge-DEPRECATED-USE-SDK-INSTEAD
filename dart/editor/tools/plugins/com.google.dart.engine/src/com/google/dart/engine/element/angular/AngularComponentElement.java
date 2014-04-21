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
 * The interface {@code AngularControllerElement} defines the Angular component described by
 * <code>Component</code> annotation.
 * 
 * @coverage dart.engine.element
 */
public interface AngularComponentElement extends AngularHasSelectorElement,
    AngularHasTemplateElement {
  /**
   * Return an array containing all of the properties declared by this component.
   */
  AngularPropertyElement[] getProperties();

  /**
   * Return an array containing all of the scope properties set in the implementation of this
   * component.
   */
  AngularScopePropertyElement[] getScopeProperties();

  /**
   * Returns the CSS file URI.
   */
  String getStyleUri();

  /**
   * Return the offset of the {@link #getStyleUri()} in the {@link #getSource()}.
   * 
   * @return the offset of the style URI
   */
  int getStyleUriOffset();
}
