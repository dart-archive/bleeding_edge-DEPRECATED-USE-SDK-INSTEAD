/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui;

public class Flags {

  public static final int AccPublic = 1;
  public static final int AccStatic = 2;

  /** There is no abstract modifier */
  public static boolean isAbstract(int f) {
    DartX.todo("modifiers");
    return false;
  }

  public static boolean isConst(int f) {
    DartX.todo("modifiers");
    return false;
  }

  /** There is no deprecated flag */
  public static boolean isDeprecated(int f) {
    DartX.todo("modifiers");
    return false;
  }

  /** There is no private modifier */
  public static boolean isPrivate(int f) {
    DartX.todo("modifiers");
    return false;
  }

  /** There is no protected modifier */
  public static boolean isProtected(int f) {
    DartX.todo("modifiers");
    return true;
  }

  /** There is no public modifier */
  public static boolean isPublic(int f) {
    DartX.todo("modifiers");
    return true;
  }

  public static boolean isStatic(int f) {
    DartX.todo("modifiers");
    return false;
  }

  public static boolean isVarargs(int f) {
    DartX.todo("modifiers");
    return false;
  }
}
