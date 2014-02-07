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
import com.google.dart.engine.internal.element.ToolkitObjectElementImpl;

/**
 * Implementation of {@code AngularElement}.
 * 
 * @coverage dart.engine.element
 */
public abstract class AngularElementImpl extends ToolkitObjectElementImpl implements AngularElement {
  /**
   * The {@link AngularApplication} this element is used in.
   */
  private AngularApplication application;

  /**
   * Initialize a newly created Angular element to have the given name.
   * 
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  public AngularElementImpl(String name, int nameOffset) {
    super(name, nameOffset);
  }

  @Override
  public AngularApplication getApplication() {
    return application;
  }

  /**
   * Set the {@link AngularApplication} this element is used in.
   */
  public void setApplication(AngularApplication application) {
    this.application = application;
  }
}
