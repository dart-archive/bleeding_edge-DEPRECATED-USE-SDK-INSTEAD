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
package com.google.dart.tools.core;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TestAll {
  public static Test suite() {
    TestSuite suite = new TestSuite("Tests in " + TestAll.class.getPackage().getName());

    suite.addTestSuite(CmdLineOptionsTest.class);
    suite.addTestSuite(DartCoreTest_New.class);

    suite.addTestSuite(PluginXMLTest.class);

    suite.addTest(com.google.dart.tools.core.builder.TestAll.suite());
    suite.addTest(com.google.dart.tools.core.completion.TestAll.suite());
    suite.addTest(com.google.dart.tools.core.dart2js.TestAll.suite());
    //suite.addTest(com.google.dart.tools.core.formatter.TestAll.suite());
    suite.addTest(com.google.dart.tools.core.generator.TestAll.suite());
    // The code under test is no longer used, and causes dartbug.com/17587.
    //suite.addTest(com.google.dart.tools.core.snapshot.TestAll.suite());
    suite.addTest(com.google.dart.tools.core.html.TestAll.suite());
    suite.addTest(com.google.dart.tools.core.internal.TestAll.suite());
    suite.addTest(com.google.dart.tools.core.model.TestAll.suite());
    suite.addTest(com.google.dart.tools.core.pub.TestAll.suite());
    suite.addTest(com.google.dart.tools.core.utilities.TestAll.suite());
    return suite;
  }
}
