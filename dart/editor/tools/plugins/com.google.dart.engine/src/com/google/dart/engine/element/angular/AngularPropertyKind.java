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
 * The enumeration {@code AngularPropertyKind} defines the different kinds of property bindings.
 * 
 * @coverage dart.engine.element
 */
public enum AngularPropertyKind {
  /**
   * `@` - Map the DOM attribute string. The attribute string will be taken literally or
   * interpolated if it contains binding {{}} syntax and assigned to the expression. (cost: 0
   * watches)
   */
  ATTR,
  /**
   * `&` - Treat the DOM attribute value as an expression. Assign a closure function into the field.
   * This allows the component to control the invocation of the closure. This is useful for passing
   * expressions into controllers which act like callbacks. (cost: 0 watches)
   */
  CALLBACK,
  /**
   * `=>` - Treat the DOM attribute value as an expression. Set up a watch, which will read the
   * expression in the attribute and assign the value to destination expression. (cost: 1 watch)
   */
  ONE_WAY,
  /**
   * `=>!` - Treat the DOM attribute value as an expression. Set up a one time watch on expression.
   * Once the expression turns not null it will no longer update. (cost: 1 watches until not null,
   * then 0 watches)
   */
  ONE_WAY_ONE_TIME,
  /**
   * `<=>` - Treat the DOM attribute value as an expression. Set up a watch on both outside as well
   * as component scope to keep the source and destination in sync. (cost: 2 watches)
   */
  TWO_WAY {
    @Override
    public boolean callsGetter() {
      return true;
    }
  };

  /**
   * Returns {@code true} if property of this kind calls field getter.
   */
  public boolean callsGetter() {
    return false;
  }

  /**
   * Returns {@code true} if property of this kind calls field setter.
   */
  public boolean callsSetter() {
    return true;
  }
}
