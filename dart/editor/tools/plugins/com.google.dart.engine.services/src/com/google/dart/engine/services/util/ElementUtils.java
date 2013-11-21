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

package com.google.dart.engine.services.util;

import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;

public class ElementUtils {
  /**
   * Check if "what" is visible in "where".
   * 
   * @param what some {@link Element}
   * @param where some {@link Element} such as class, method or function where "what" could
   *          potentially be referenced
   */
  public static boolean isAccessible(Element what, Element where) {
    LibraryElement whereLibrary = where.getLibrary();
    return what.isAccessibleIn(whereLibrary);
  }

  /**
   * @return {@code true} if given {@link Element} is library private.
   */
  public static boolean isPrivate(Element element) {
    String name = element.getDisplayName();
    if (name == null) {
      return false;
    }
    return Identifier.isPrivateName(name);
  }

  /**
   * @return {@code true} if given {@link Element} is public.
   */
  public static boolean isPublic(Element element) {
    return !isPrivate(element);
  }
}
