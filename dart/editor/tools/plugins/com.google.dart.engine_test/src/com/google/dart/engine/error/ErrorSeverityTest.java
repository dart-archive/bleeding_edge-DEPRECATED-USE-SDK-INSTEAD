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
package com.google.dart.engine.error;

import com.google.dart.engine.EngineTestCase;

import static com.google.dart.engine.error.ErrorSeverity.ERROR;
import static com.google.dart.engine.error.ErrorSeverity.NONE;
import static com.google.dart.engine.error.ErrorSeverity.WARNING;

public class ErrorSeverityTest extends EngineTestCase {
  public void test_max_error_error() {
    assertSame(ERROR, ERROR.max(ERROR));
  }

  public void test_max_error_none() {
    assertSame(ERROR, ERROR.max(NONE));
  }

  public void test_max_error_warning() {
    assertSame(ERROR, ERROR.max(WARNING));
  }

  public void test_max_none_error() {
    assertSame(ERROR, NONE.max(ERROR));
  }

  public void test_max_none_none() {
    assertSame(NONE, NONE.max(NONE));
  }

  public void test_max_none_warning() {
    assertSame(WARNING, NONE.max(WARNING));
  }

  public void test_max_warning_error() {
    assertSame(ERROR, WARNING.max(ERROR));
  }

  public void test_max_warning_none() {
    assertSame(WARNING, WARNING.max(NONE));
  }

  public void test_max_warning_warning() {
    assertSame(WARNING, WARNING.max(WARNING));
  }
}
