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

import com.google.dart.tools.ui.internal.util.NotYetImplementedException;

public class DartX {

  /**
   * A marker that disallows execution of unfinished code.
   */
  public static void notYet() {
    throw new NotYetImplementedException();
  }

  /**
   * A marker that disallows execution of unfinished code.
   */
  public static void notYet(String note) {
    throw new NotYetImplementedException(note);
  }

  /**
   * A benign marker to identify unfinished code.
   */
  public static void todo() {
  }

  /**
   * A benign marker to identify unfinished code.
   */
  public static void todo(String note) {
  }
}
