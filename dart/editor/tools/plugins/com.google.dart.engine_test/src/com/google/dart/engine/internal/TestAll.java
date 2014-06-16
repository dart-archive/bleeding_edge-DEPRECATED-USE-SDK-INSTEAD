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
package com.google.dart.engine.internal;

import com.google.dart.engine.ExtendedTestSuite;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TestAll {
  public static Test suite() {
    TestSuite suite = new ExtendedTestSuite("Tests in " + TestAll.class.getPackage().getName());
    suite.addTest(com.google.dart.engine.internal.builder.TestAll.suite());
    suite.addTest(com.google.dart.engine.internal.cache.TestAll.suite());
    suite.addTest(com.google.dart.engine.internal.constant.TestAll.suite());
    suite.addTest(com.google.dart.engine.internal.context.TestAll.suite());
    suite.addTest(com.google.dart.engine.internal.element.TestAll.suite());
    suite.addTest(com.google.dart.engine.internal.error.TestAll.suite());
    suite.addTest(com.google.dart.engine.internal.hint.TestAll.suite());
    suite.addTest(com.google.dart.engine.internal.html.TestAll.suite());
    suite.addTest(com.google.dart.engine.internal.index.TestAll.suite());
    suite.addTest(com.google.dart.engine.internal.object.TestAll.suite());
    suite.addTest(com.google.dart.engine.internal.resolver.TestAll.suite());
    suite.addTest(com.google.dart.engine.internal.scope.TestAll.suite());
    suite.addTest(com.google.dart.engine.internal.sdk.TestAll.suite());
    suite.addTest(com.google.dart.engine.internal.search.TestAll.suite());
    suite.addTest(com.google.dart.engine.internal.task.TestAll.suite());
    suite.addTest(com.google.dart.engine.internal.type.TestAll.suite());
    return suite;
  }
}
