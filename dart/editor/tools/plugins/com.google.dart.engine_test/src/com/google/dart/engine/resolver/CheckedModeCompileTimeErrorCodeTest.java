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
package com.google.dart.engine.resolver;

import com.google.dart.engine.error.CheckedModeCompileTimeErrorCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.source.Source;

public class CheckedModeCompileTimeErrorCodeTest extends ResolverTestCase {
  public void test_fieldInitializerNotAssignable() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final int x;",
        "  const A() : x = '';",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CheckedModeCompileTimeErrorCode.CONST_FIELD_INITIALIZER_NOT_ASSIGNABLE,
        StaticWarningCode.FIELD_INITIALIZER_NOT_ASSIGNABLE);
    verify(source);
  }
}
