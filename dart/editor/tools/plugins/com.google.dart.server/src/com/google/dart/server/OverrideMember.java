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

/**
 * The interface {@code OverrideMember} defines an override
 * 
 * @coverage dart.server
 */
public interface OverrideMember {
  /**
   * An empty array of overrides.
   */
  OverrideMember[] EMPTY_ARRAY = new OverrideMember[0];

  /**
   * Return the length of the name of the overriding member.
   * 
   * @return the length of the name of the overriding member
   */
  public int getLength();

  /**
   * Return the offset of the name of the overriding member.
   * 
   * @return the offset of the name of the overriding member
   */
  public int getOffset();

  /**
   * Return the element that was overridden from a superclass. The value is omitted if there is no
   * superclass method.
   * 
   * @return the element that was overridden from a superclass
   */
  public Element getSuperclassElement();

}
