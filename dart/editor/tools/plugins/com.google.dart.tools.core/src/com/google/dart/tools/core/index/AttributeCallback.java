/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.index;

/**
 * The interface <code>AttributeCallback</code> defines the behavior of objects that are invoked
 * with the result of a query about a given attribute.
 */
public interface AttributeCallback {
  /**
   * This method is invoked when the value of the given attribute on the given element is available.
   * 
   * @param element the element with which the attribute is associated
   * @param attribute the attribute whose value was requested
   * @param value the value of the attribute
   */
  public void hasValue(Element element, Attribute attribute, String value);
}
