/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.indexer.tests.internal;

public class TestingUtils {
  public static StackTraceElement callerOutside(Class<?> klass) {
    boolean thisClassMet = false;
    StackTraceElement[] stackTrace = new Exception().getStackTrace();
    for (int i = 0; i < stackTrace.length; i++) {
      StackTraceElement element = stackTrace[i];
      boolean inThisClass = klass.getName().equals(element.getClassName());
      if (!thisClassMet) {
        if (inThisClass)
          thisClassMet = true;
      } else {
        if (!inThisClass)
          return element;
      }
    }
    throw new AssertionError("Unreachable");
  }
}
